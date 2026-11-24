package cn.tinsur.wokit.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * 登录用户工具类：从请求头 Token 中取出当前登录用户信息
 * 拦截器已校验过 Token，这里解析失败会由全局异常处理
 *
 * @author Tinsur
 */
public class AuthUtil {

    /**
     * 获取当前登录用户ID
     */
    public static Long getUserId(HttpServletRequest request) {
        Map<String, Object> claims = JwtUtil.parseToken(request.getHeader("Authorization"));
        return ((Number) claims.get("id")).longValue();
    }

    /**
     * 获取当前登录用户角色（0：普通用户，1：管理员）
     */
    public static Integer getRole(HttpServletRequest request) {
        Map<String, Object> claims = JwtUtil.parseToken(request.getHeader("Authorization"));
        Object role = claims.get("role");
        return role == null ? 0 : ((Number) role).intValue();
    }
}
