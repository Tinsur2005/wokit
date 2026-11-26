package cn.tinsur.wokit.interceptor;

import cn.tinsur.wokit.mapper.ApiKeyMapper;
import cn.tinsur.wokit.mapper.UserMapper;
import cn.tinsur.wokit.pojo.entity.ApiKey;
import cn.tinsur.wokit.pojo.entity.User;
import cn.tinsur.wokit.security.PermissionGuard;
import cn.tinsur.wokit.util.ApiAuthUtil;
import cn.tinsur.wokit.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 开放 API 认证拦截器：只接受 api_user / api_service 类型的 Token。
 * 每次请求校验 Key 状态和用户状态，保证管理员停用后立即生效。
 *
 * @author Tinsur
 */
@Component
public class ApiAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ApiKeyMapper apiKeyMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PermissionGuard permissionGuard;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String value = request.getHeader("Authorization");
        if (ObjectUtils.isEmpty(value)) {
            return reject(response, "请先登录");
        }
        String token = value.startsWith("Bearer ") ? value.substring(7).trim() : value.trim();
        if (token.isEmpty()) {
            return reject(response, "请先登录");
        }
        try {
            Map<String, Object> claims = JwtUtil.parseToken(token);
            String type = JwtUtil.getType(token);
            if (!"api_user".equals(type) && !"api_service".equals(type)) {
                return reject(response, "Token 类型不正确");
            }
            Object userIdValue = claims.get("id");
            if (!(userIdValue instanceof Number userIdNumber)) {
                return reject(response, "Token 缺少用户信息");
            }
            Long userId = userIdNumber.longValue();
            User user = userMapper.selectById(userId);
            if (user == null || Integer.valueOf(0).equals(user.getStatus())) {
                return reject(response, "账号不存在或已被停用");
            }

            Object keyIdValue = claims.get("keyId");
            Long keyId = keyIdValue instanceof Number number ? number.longValue() : null;
            if (keyId != null) {
                ApiKey apiKey = apiKeyMapper.selectById(keyId);
                if (apiKey == null || Integer.valueOf(0).equals(apiKey.getStatus())) {
                    return reject(response, "应用密钥已停用或不存在");
                }
                if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().before(new java.util.Date())) {
                    return reject(response, "应用密钥已过期");
                }
                if (!permissionGuard.hasApiKeyAccess(apiKey.getOwnerUserId())) {
                    return reject(response, "应用密钥所属账号已失去接口权限");
                }
            }

            request.setAttribute(ApiAuthUtil.ATTR_USER_ID, userId);
            request.setAttribute(ApiAuthUtil.ATTR_KEY_ID, keyId);
            request.setAttribute(ApiAuthUtil.ATTR_TOKEN_TYPE, type);
            request.setAttribute(ApiAuthUtil.ATTR_ROLE, claims.get("role"));
            return true;
        } catch (Exception e) {
            return reject(response, "登录状态已失效，请重新登录");
        }
    }

    private boolean reject(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 0);
        body.put("msg", message);
        body.put("data", null);
        response.getWriter().write(objectMapper.writeValueAsString(body));
        return false;
    }
}
