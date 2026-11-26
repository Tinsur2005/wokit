package cn.tinsur.wokit.interceptor;

import cn.tinsur.wokit.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器：除登录接口外全部请求必须携带有效 Token
 * Token 缺失或解析失败统一返回 401，前端跳转登录页
 *
 * @author Tinsur
 */
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取 Token
        String token = request.getHeader("Authorization");
        if (ObjectUtils.isEmpty(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        try {
            // 解析 token，解析失败（过期、伪造）会抛异常
            JwtUtil.parseToken(token);
            // 开放 API 的 App / 服务 Token 不能用于网页端
            String type = JwtUtil.getType(token);
            if (type != null && !"web".equals(type)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }
            return true; // 放行
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }
}
