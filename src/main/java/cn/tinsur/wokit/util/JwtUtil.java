package cn.tinsur.wokit.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类：登录成功后签发 Token，拦截器解析 Token
 *
 * @author Tinsur
 */
public class JwtUtil {

    // 签名密钥，生产环境应放到配置文件
    private static final String KEY = "tinsur-wokit";
    // Token 有效期：24 小时
    private static final long EXPIRE_TIME = 1000 * 60 * 60 * 24;

    /**
     * 生成 Token
     *
     * @param claims 有效载荷（用户ID、账号、角色）
     * @return Token 字符串
     */
    public static String creatToken(Map<String, Object> claims) {
        return JWT.create()
                .withClaim("claims", claims) // 有效载荷
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRE_TIME))
                .sign(Algorithm.HMAC256(KEY)); // 密钥
    }

    /**
     * 解析 Token
     *
     * @param token 前端请求头 Authorization 中的 Token
     * @return 有效载荷
     */
    public static Map<String, Object> parseToken(String token) {
        return JWT.require(Algorithm.HMAC256(KEY))
                .build()
                .verify(token)
                .getClaim("claims")
                .asMap();
    }
}
