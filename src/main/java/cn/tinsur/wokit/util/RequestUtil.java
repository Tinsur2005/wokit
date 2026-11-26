package cn.tinsur.wokit.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 获取真实客户端 IP，兼容 Nginx 反向代理。
 *
 * @author Tinsur
 */
public final class RequestUtil {

    private RequestUtil() {
    }

    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String first = forwarded.split(",")[0].trim();
            if (!first.isEmpty()) {
                return first;
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
