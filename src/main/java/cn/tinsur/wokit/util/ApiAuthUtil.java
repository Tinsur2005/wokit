package cn.tinsur.wokit.util;

import cn.tinsur.wokit.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 开放 API 当前调用者工具。
 * 用户 Token 和 Service Token 都会映射到一个用户 ID，Service Token 映射到 Key 所属用户。
 *
 * @author Tinsur
 */
public final class ApiAuthUtil {

    public static final String ATTR_USER_ID = "wokit.api.userId";
    public static final String ATTR_KEY_ID = "wokit.api.keyId";
    public static final String ATTR_TOKEN_TYPE = "wokit.api.tokenType";
    public static final String ATTR_ROLE = "wokit.api.role";

    private ApiAuthUtil() {
    }

    public static Long getUserId(HttpServletRequest request) {
        Object value = request.getAttribute(ATTR_USER_ID);
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new ApiException(401, "登录状态已失效，请重新登录");
    }

    public static Long getKeyId(HttpServletRequest request) {
        Object value = request.getAttribute(ATTR_KEY_ID);
        return value instanceof Number number ? number.longValue() : null;
    }

    public static String getTokenType(HttpServletRequest request) {
        Object value = request.getAttribute(ATTR_TOKEN_TYPE);
        return value == null ? null : value.toString();
    }

    public static boolean isServiceToken(HttpServletRequest request) {
        return "api_service".equals(getTokenType(request));
    }

    public static void requireUserToken(HttpServletRequest request) {
        if (isServiceToken(request)) {
            throw new ApiException(403, "该接口只允许用户 Token 调用");
        }
    }
}
