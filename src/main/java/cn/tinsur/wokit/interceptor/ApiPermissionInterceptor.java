package cn.tinsur.wokit.interceptor;

import cn.tinsur.wokit.security.PermissionGuard;
import cn.tinsur.wokit.security.RequirePermission;
import cn.tinsur.wokit.util.ApiAuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 开放 API 按钮权限拦截器：按方法上的 @RequirePermission 校验。
 *
 * @author Tinsur
 */
@Component
public class ApiPermissionInterceptor implements HandlerInterceptor {

    @Autowired
    private PermissionGuard permissionGuard;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequirePermission annotation = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (annotation == null) {
            annotation = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
        }
        if (annotation == null) {
            return true;
        }
        permissionGuard.requireValue(ApiAuthUtil.getUserId(request), annotation.value());
        return true;
    }
}
