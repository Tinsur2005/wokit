package cn.tinsur.wokit.service.impl;

import cn.tinsur.wokit.exception.ApiException;
import cn.tinsur.wokit.mapper.RefreshTokenMapper;
import cn.tinsur.wokit.mapper.UserMapper;
import cn.tinsur.wokit.pojo.dto.ApiLoginDTO;
import cn.tinsur.wokit.pojo.dto.ApiRefreshDTO;
import cn.tinsur.wokit.pojo.dto.UserPasswordDTO;
import cn.tinsur.wokit.pojo.entity.ApiKey;
import cn.tinsur.wokit.pojo.entity.RefreshToken;
import cn.tinsur.wokit.pojo.entity.User;
import cn.tinsur.wokit.pojo.vo.ApiTokenVO;
import cn.tinsur.wokit.pojo.vo.UserVO;
import cn.tinsur.wokit.security.PermissionGuard;
import cn.tinsur.wokit.service.IApiAuthService;
import cn.tinsur.wokit.service.IApiKeyService;
import cn.tinsur.wokit.service.IUserService;
import cn.tinsur.wokit.util.JwtUtil;
import cn.tinsur.wokit.util.PasswordUtil;
import cn.tinsur.wokit.util.Result;
import cn.tinsur.wokit.util.SecretUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * App / 服务端认证。
 *
 * @author Tinsur
 */
@Service
public class ApiAuthServiceImpl implements IApiAuthService {

    @Autowired
    private IApiKeyService apiKeyService;

    @Autowired
    private RefreshTokenMapper refreshTokenMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private IUserService userService;

    @Autowired
    private PermissionGuard permissionGuard;

    @Value("${wokit.api.access-token-seconds:3600}")
    private long accessTokenSeconds;

    @Value("${wokit.api.refresh-token-days:30}")
    private int refreshTokenDays;

    /**
     * 简单的内存登录失败限流：同一 Access Key + IP + 账号连续失败 5 次锁定 10 分钟。
     * 单机部署足够使用，多实例部署时可以替换为 Redis。
     */
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    @Override
    public Result<ApiTokenVO> login(String accessKey, ApiLoginDTO dto, String ip) {
        if (ObjectUtils.isEmpty(accessKey)) {
            throw new ApiException(401, "缺少应用标识，请在请求头携带 X-Wokit-Client-Key");
        }
        if (dto == null || ObjectUtils.isEmpty(dto.getUsername()) || ObjectUtils.isEmpty(dto.getPassword())) {
            throw new ApiException(400, "请输入账号和密码");
        }
        String username = dto.getUsername().trim();
        String attemptKey = accessKey + "|" + username + "|" + ip;
        checkAttempt(attemptKey);

        ApiKey apiKey = apiKeyService.findValidKey(accessKey, "app", ip);
        if (apiKey == null) {
            throw new ApiException(401, "应用密钥无效、已停用或已过期");
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getName, username)
                .last("LIMIT 1"));
        if (user == null || !PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            recordFailure(attemptKey);
            throw new ApiException(401, "账号或密码错误");
        }
        if (Integer.valueOf(0).equals(user.getStatus())) {
            throw new ApiException(403, "该账号已被停用，请联系管理员");
        }

        clearAttempt(attemptKey);
        apiKeyService.touch(apiKey.getId(), ip);
        ApiTokenVO tokenVO = issueUserToken(user, apiKey, dto.getDeviceId(), dto.getDeviceName(), ip);
        return Result.ok("登录成功", tokenVO);
    }

    @Override
    @Transactional
    public Result<ApiTokenVO> refresh(ApiRefreshDTO dto, String ip) {
        if (dto == null || ObjectUtils.isEmpty(dto.getRefreshToken())) {
            throw new ApiException(400, "refreshToken 不能为空");
        }
        RefreshToken dbToken = refreshTokenMapper.selectOne(new LambdaQueryWrapper<RefreshToken>()
                .eq(RefreshToken::getTokenHash, SecretUtil.sha256(dto.getRefreshToken().trim()))
                .last("LIMIT 1"));
        if (dbToken == null || dbToken.getRevokedAt() != null
                || dbToken.getExpiresAt() == null || dbToken.getExpiresAt().before(new Date())) {
            throw new ApiException(401, "Refresh Token 无效或已过期，请重新登录");
        }
        User user = userMapper.selectById(dbToken.getUserId());
        if (user == null || Integer.valueOf(0).equals(user.getStatus())) {
            throw new ApiException(403, "账号不存在或已被停用");
        }
        ApiKey apiKey = dbToken.getApiKeyId() == null ? null : apiKeyService.getById(dbToken.getApiKeyId());
        if (apiKey == null || Integer.valueOf(0).equals(apiKey.getStatus())
                || (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().before(new Date()))) {
            throw new ApiException(401, "应用密钥已停用或已过期，请重新登录");
        }
        if (!apiKeyService.isIpAllowed(apiKey, ip)) {
            throw new ApiException(403, "当前 IP 不在应用密钥白名单内");
        }
        if (!permissionGuard.hasApiKeyAccess(apiKey.getOwnerUserId())) {
            throw new ApiException(403, "应用密钥所属账号已失去接口权限");
        }

        // 轮换：旧 Refresh Token 立即失效
        dbToken.setRevokedAt(new Date());
        dbToken.setLastUsedAt(new Date());
        refreshTokenMapper.updateById(dbToken);

        apiKeyService.touch(apiKey.getId(), ip);
        ApiTokenVO tokenVO = issueUserToken(user, apiKey, dto.getDeviceId(), dto.getDeviceName(), ip);
        return Result.ok("刷新成功", tokenVO);
    }

    @Override
    public Result logout(ApiRefreshDTO dto) {
        if (dto == null || ObjectUtils.isEmpty(dto.getRefreshToken())) {
            return Result.ok("已退出登录");
        }
        RefreshToken dbToken = refreshTokenMapper.selectOne(new LambdaQueryWrapper<RefreshToken>()
                .eq(RefreshToken::getTokenHash, SecretUtil.sha256(dto.getRefreshToken().trim()))
                .last("LIMIT 1"));
        if (dbToken != null && dbToken.getRevokedAt() == null) {
            dbToken.setRevokedAt(new Date());
            refreshTokenMapper.updateById(dbToken);
        }
        return Result.ok("已退出登录");
    }

    @Override
    public Map<String, Object> me(Long userId) {
        return userService.userInfo(userId);
    }

    @Override
    @Transactional
    public Result updatePassword(Long userId, UserPasswordDTO dto) {
        Result result = userService.updatePassword(userId, dto);
        if (Integer.valueOf(1).equals(result.getCode())) {
            refreshTokenMapper.update(null, new LambdaUpdateWrapper<RefreshToken>()
                    .eq(RefreshToken::getUserId, userId)
                    .isNull(RefreshToken::getRevokedAt)
                    .set(RefreshToken::getRevokedAt, new Date()));
        }
        return result;
    }

    @Override
    public Map<String, Object> serviceToken(String accessKey, String secretKey, String ip) {
        ApiKey apiKey = apiKeyService.validateServiceCredentials(accessKey, secretKey, ip);
        if (apiKey == null) {
            throw new ApiException(401, "client_id 或 client_secret 无效");
        }
        User owner = userMapper.selectById(apiKey.getOwnerUserId());
        if (owner == null || Integer.valueOf(0).equals(owner.getStatus())) {
            throw new ApiException(403, "密钥所属账号已被停用");
        }
        apiKeyService.touch(apiKey.getId(), ip);

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("id", owner.getId());
        claims.put("name", apiKey.getAppName());
        claims.put("role", owner.getRole() == null ? 0 : owner.getRole());
        claims.put("keyId", apiKey.getId());
        String accessToken = JwtUtil.createToken(claims, accessTokenSeconds * 1000, "api_service");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accessToken", accessToken);
        result.put("tokenType", "Bearer");
        result.put("expiresIn", accessTokenSeconds);
        result.put("scope", apiKey.getScopes());
        result.put("apiKeyId", apiKey.getId());
        return result;
    }

    private ApiTokenVO issueUserToken(User user, ApiKey apiKey, String deviceId, String deviceName, String ip) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("id", user.getId());
        claims.put("name", user.getName());
        claims.put("role", user.getRole() == null ? 0 : user.getRole());
        claims.put("keyId", apiKey.getId());
        String accessToken = JwtUtil.createToken(claims, accessTokenSeconds * 1000, "api_user");

        String refreshRaw = SecretUtil.newRefreshToken();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setApiKeyId(apiKey.getId());
        refreshToken.setTokenHash(SecretUtil.sha256(refreshRaw));
        refreshToken.setDeviceId(limit(deviceId, 100));
        refreshToken.setDeviceName(limit(deviceName, 100));
        refreshToken.setIp(limit(ip, 64));
        refreshToken.setExpiresAt(Date.from(Instant.now().plusSeconds(refreshTokenDays * 86400L)));
        refreshTokenMapper.insert(refreshToken);

        ApiTokenVO vo = new ApiTokenVO();
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshRaw);
        vo.setExpiresIn(accessTokenSeconds);
        vo.setUser(userMap(user));
        return vo;
    }

    private Map<String, Object> userMap(User user) {
        UserVO detail = userService.detail(user.getId());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", detail.getId());
        map.put("name", detail.getName());
        map.put("realName", detail.getRealName());
        map.put("avatar", detail.getAvatar());
        map.put("role", detail.getRole() == null ? 0 : detail.getRole());
        map.put("roleName", detail.getRoleName());
        return map;
    }

    private String limit(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }

    private void checkAttempt(String key) {
        Attempt attempt = attempts.get(key);
        if (attempt != null && attempt.blockedUntil > System.currentTimeMillis()) {
            throw new ApiException(429, "登录失败次数过多，请 10 分钟后再试");
        }
    }

    private void recordFailure(String key) {
        long now = System.currentTimeMillis();
        attempts.compute(key, (k, old) -> {
            if (old == null || now - old.lastFailAt > 10 * 60 * 1000L) {
                return new Attempt(now, 1, 0);
            }
            int count = old.count + 1;
            long blockedUntil = count >= 5 ? now + 10 * 60 * 1000L : 0;
            return new Attempt(now, count, blockedUntil);
        });
        if (attempts.size() > 2000) {
            attempts.entrySet().removeIf(entry ->
                    now - entry.getValue().lastFailAt > 30 * 60 * 1000L);
        }
    }

    private void clearAttempt(String key) {
        attempts.remove(key);
    }

    private static class Attempt {
        private final long lastFailAt;
        private final int count;
        private final long blockedUntil;

        private Attempt(long lastFailAt, int count, long blockedUntil) {
            this.lastFailAt = lastFailAt;
            this.count = count;
            this.blockedUntil = blockedUntil;
        }
    }
}
