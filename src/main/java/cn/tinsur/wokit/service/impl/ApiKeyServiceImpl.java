package cn.tinsur.wokit.service.impl;

import cn.tinsur.wokit.exception.ServiceException;
import cn.tinsur.wokit.mapper.ApiKeyMapper;
import cn.tinsur.wokit.mapper.RefreshTokenMapper;
import cn.tinsur.wokit.pojo.dto.ApiKeyCreateDTO;
import cn.tinsur.wokit.pojo.entity.ApiKey;
import cn.tinsur.wokit.pojo.entity.RefreshToken;
import cn.tinsur.wokit.pojo.vo.ApiKeyVO;
import cn.tinsur.wokit.security.PermissionGuard;
import cn.tinsur.wokit.service.IApiKeyService;
import cn.tinsur.wokit.util.SecretUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * API Key 管理。
 *
 * @author Tinsur
 */
@Service
public class ApiKeyServiceImpl extends ServiceImpl<ApiKeyMapper, ApiKey> implements IApiKeyService {

    private static final Set<String> PLATFORMS = Set.of("desktop", "mobile", "server", "other");
    private static final Set<String> KEY_TYPES = Set.of("app", "service");

    @Autowired
    private RefreshTokenMapper refreshTokenMapper;

    @Autowired
    private PermissionGuard permissionGuard;

    @Override
    public IPage<ApiKeyVO> list(Integer page, Integer limit, String appName, String keyType, Integer status,
                                Long currentUserId) {
        int pageNum = page == null || page < 1 ? 1 : page;
        int pageSize = limit == null || limit < 1 ? 10 : Math.min(limit, 100);
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<ApiKey>()
                .like(!ObjectUtils.isEmpty(appName), ApiKey::getAppName, appName)
                .eq(!ObjectUtils.isEmpty(keyType), ApiKey::getKeyType, keyType)
                .eq(status != null, ApiKey::getStatus, status)
                .eq(!permissionGuard.isAdmin(currentUserId), ApiKey::getOwnerUserId, currentUserId)
                .orderByDesc(ApiKey::getCreateTime);
        IPage<ApiKey> source = page(new Page<>(pageNum, pageSize), wrapper);
        IPage<ApiKeyVO> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setRecords(source.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    @Override
    public ApiKeyVO create(ApiKeyCreateDTO dto, Long currentUserId) {
        if (dto == null || ObjectUtils.isEmpty(dto.getAppName())) {
            throw new ServiceException("应用名称不能为空");
        }
        String appName = dto.getAppName().trim();
        if (appName.length() > 60) {
            throw new ServiceException("应用名称不能超过 60 个字符");
        }
        String platform = ObjectUtils.isEmpty(dto.getPlatform()) ? "desktop" : dto.getPlatform().trim();
        if (!PLATFORMS.contains(platform)) {
            throw new ServiceException("平台类型不正确");
        }
        String keyType = ObjectUtils.isEmpty(dto.getKeyType()) ? "app" : dto.getKeyType().trim();
        if (!KEY_TYPES.contains(keyType)) {
            throw new ServiceException("密钥类型不正确");
        }
        if (dto.getExpiresAt() != null && dto.getExpiresAt().before(new Date())) {
            throw new ServiceException("过期时间不能早于当前时间");
        }
        validateIpWhitelist(dto.getIpWhitelist());

        String accessKey = SecretUtil.newAccessKey();
        while (count(new LambdaQueryWrapper<ApiKey>().eq(ApiKey::getAccessKey, accessKey)) > 0) {
            accessKey = SecretUtil.newAccessKey();
        }
        String secret = SecretUtil.newSecretKey();

        ApiKey apiKey = new ApiKey();
        apiKey.setOwnerUserId(currentUserId);
        apiKey.setAppName(appName);
        apiKey.setPlatform(platform);
        apiKey.setKeyType(keyType);
        apiKey.setAccessKey(accessKey);
        apiKey.setSecretHash(SecretUtil.sha256(secret));
        apiKey.setSecretHint(SecretUtil.secretHint(secret));
        apiKey.setSecretVersion(1);
        apiKey.setScopes("app".equals(keyType) ? "auth:login" : "data:read,data:write");
        apiKey.setIpWhitelist(normalizeIpWhitelist(dto.getIpWhitelist()));
        apiKey.setStatus(1);
        apiKey.setExpiresAt(dto.getExpiresAt());
        apiKey.setRemark(dto.getRemark() == null ? null : dto.getRemark().trim());
        save(apiKey);

        ApiKeyVO vo = toVO(apiKey);
        vo.setSecretKey(secret);
        vo.setSecretOnce(true);
        return vo;
    }

    @Override
    public void changeStatus(Long id, Integer status, Long currentUserId) {
        if (status == null || (status != 0 && status != 1)) {
            throw new ServiceException("状态值不正确");
        }
        ApiKey apiKey = requireManageable(id, currentUserId);
        apiKey.setStatus(status);
        updateById(apiKey);
        if (status == 0) {
            revokeRefreshTokensByKey(id);
        }
    }

    @Override
    @Transactional
    public ApiKeyVO resetSecret(Long id, Long currentUserId) {
        ApiKey apiKey = requireManageable(id, currentUserId);
        String secret = SecretUtil.newSecretKey();
        apiKey.setSecretHash(SecretUtil.sha256(secret));
        apiKey.setSecretHint(SecretUtil.secretHint(secret));
        apiKey.setSecretVersion((apiKey.getSecretVersion() == null ? 0 : apiKey.getSecretVersion()) + 1);
        updateById(apiKey);
        revokeRefreshTokensByKey(id);

        ApiKeyVO vo = toVO(apiKey);
        vo.setSecretKey(secret);
        vo.setSecretOnce(true);
        return vo;
    }

    @Override
    @Transactional
    public void delete(Long id, Long currentUserId) {
        requireManageable(id, currentUserId);
        revokeRefreshTokensByKey(id);
        removeById(id);
    }

    @Override
    public ApiKey findValidKey(String accessKey, String expectedType, String ip) {
        if (ObjectUtils.isEmpty(accessKey)) {
            return null;
        }
        ApiKey apiKey = getOne(new LambdaQueryWrapper<ApiKey>()
                .eq(ApiKey::getAccessKey, accessKey.trim())
                .last("LIMIT 1"));
        if (apiKey == null || Integer.valueOf(0).equals(apiKey.getStatus())) {
            return null;
        }
        if (expectedType != null && !expectedType.equals(apiKey.getKeyType())) {
            return null;
        }
        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().before(new Date())) {
            return null;
        }
        if (!isIpAllowed(apiKey, ip)) {
            return null;
        }
        if (!permissionGuard.hasApiKeyAccess(apiKey.getOwnerUserId())) {
            return null;
        }
        return apiKey;
    }

    @Override
    public ApiKey validateServiceCredentials(String accessKey, String secretKey, String ip) {
        ApiKey apiKey = findValidKey(accessKey, "service", ip);
        if (apiKey == null || !SecretUtil.matchesHash(secretKey, apiKey.getSecretHash())) {
            return null;
        }
        return apiKey;
    }

    @Override
    public boolean isIpAllowed(ApiKey apiKey, String ip) {
        String whitelist = apiKey.getIpWhitelist();
        if (ObjectUtils.isEmpty(whitelist)) {
            return true;
        }
        String clientIp = ip == null ? "" : ip.trim();
        for (String item : whitelist.split(",")) {
            if (clientIp.equals(item.trim())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void touch(Long id, String ip) {
        if (id == null) {
            return;
        }
        update(new LambdaUpdateWrapper<ApiKey>()
                .eq(ApiKey::getId, id)
                .set(ApiKey::getLastUsedAt, new Date())
                .set(ApiKey::getLastUsedIp, ip));
    }

    @Override
    public void revokeRefreshTokensByKey(Long apiKeyId) {
        if (apiKeyId == null) {
            return;
        }
        Date now = new Date();
        List<RefreshToken> tokens = refreshTokenMapper.selectList(
                new LambdaQueryWrapper<RefreshToken>()
                        .eq(RefreshToken::getApiKeyId, apiKeyId)
                        .isNull(RefreshToken::getRevokedAt));
        for (RefreshToken token : tokens) {
            token.setRevokedAt(now);
            refreshTokenMapper.updateById(token);
        }
    }

    private ApiKey requireManageable(Long id, Long currentUserId) {
        ApiKey apiKey = getById(id);
        if (apiKey == null) {
            throw new ServiceException("密钥不存在或已被删除");
        }
        if (!permissionGuard.isAdmin(currentUserId)
                && !apiKey.getOwnerUserId().equals(currentUserId)) {
            throw new ServiceException("只能管理自己创建的密钥");
        }
        return apiKey;
    }

    private void validateIpWhitelist(String whitelist) {
        if (ObjectUtils.isEmpty(whitelist)) {
            return;
        }
        if (whitelist.length() > 300) {
            throw new ServiceException("IP 白名单不能超过 300 个字符");
        }
        for (String item : whitelist.split(",")) {
            String value = item.trim();
            if (value.isEmpty()) {
                throw new ServiceException("IP 白名单格式不正确");
            }
            if (!value.matches("^[0-9a-fA-F:.\\-]{1,64}$")) {
                throw new ServiceException("IP 白名单只支持 IPv4 / IPv6 地址，多个地址用英文逗号分隔");
            }
        }
    }

    private String normalizeIpWhitelist(String whitelist) {
        if (ObjectUtils.isEmpty(whitelist)) {
            return null;
        }
        return String.join(",", java.util.Arrays.stream(whitelist.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList());
    }

    private ApiKeyVO toVO(ApiKey apiKey) {
        ApiKeyVO vo = new ApiKeyVO();
        BeanUtils.copyProperties(apiKey, vo);
        vo.setSecretHash(null);
        vo.setSecretKey(null);
        vo.setSecretOnce(false);
        return vo;
    }
}
