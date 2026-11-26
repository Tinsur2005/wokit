package cn.tinsur.wokit.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类：网页端 Token、App 用户 Token、服务对接 Token 统一签发与解析。
 *
 * @author Tinsur
 */
public class JwtUtil {

    /**
     * 默认密钥，启动时由 JwtSecretInitializer 使用配置文件覆盖
     */
    private static volatile String key = "tinsur-wokit";

    /**
     * 网页端 Token 有效期：24 小时
     */
    private static final long WEB_EXPIRE_TIME = 1000L * 60 * 60 * 24;

    public static void setKey(String secret) {
        if (secret != null && !secret.isBlank()) {
            key = secret;
        }
    }

    /**
     * 生成网页端 Token，保留原有 24 小时有效期
     */
    public static String creatToken(Map<String, Object> claims) {
        return createToken(claims, WEB_EXPIRE_TIME, "web");
    }

    /**
     * 生成指定有效期与类型的 Token
     *
     * @param claims    业务载荷
     * @param ttlMillis 有效期（毫秒）
     * @param type      web / api_user / api_service
     */
    public static String createToken(Map<String, Object> claims, long ttlMillis, String type) {
        return JWT.create()
                .withClaim("claims", claims)
                .withClaim("typ", type)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + ttlMillis))
                .sign(Algorithm.HMAC256(key));
    }

    /**
     * 解析 Token，签名错误或过期会抛异常
     */
    public static Map<String, Object> parseToken(String token) {
        return parseDecoded(token).getClaim("claims").asMap();
    }

    /**
     * 获取 Token 类型，兼容旧版本没有 typ 的网页 Token
     */
    public static String getType(String token) {
        return parseDecoded(token).getClaim("typ").asString();
    }

    private static DecodedJWT parseDecoded(String token) {
        return JWT.require(Algorithm.HMAC256(key))
                .build()
                .verify(token);
    }
}
