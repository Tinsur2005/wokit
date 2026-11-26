package cn.tinsur.wokit.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * API Key / Refresh Token 随机生成与哈希工具。
 *
 * @author Tinsur
 */
public final class SecretUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private SecretUtil() {
    }

    /**
     * 生成 Access Key，公开的应用标识
     */
    public static String newAccessKey() {
        return "wk_ak_" + randomUrlSafe(24);
    }

    /**
     * 生成 Secret Key，只在创建/重置时返回一次
     */
    public static String newSecretKey() {
        return "wk_sk_" + randomUrlSafe(36);
    }

    /**
     * 生成 Refresh Token 明文
     */
    public static String newRefreshToken() {
        return "wk_rt_" + randomUrlSafe(48);
    }

    /**
     * SHA-256 十六进制哈希，用于 Secret 和 Refresh Token 落库
     */
    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            char[] chars = new char[bytes.length * 2];
            for (int i = 0; i < bytes.length; i++) {
                int v = bytes[i] & 0xFF;
                chars[i * 2] = HEX[v >>> 4];
                chars[i * 2 + 1] = HEX[v & 0x0F];
            }
            return new String(chars);
        } catch (Exception e) {
            throw new IllegalStateException("哈希计算失败", e);
        }
    }

    /**
     * 固定时间比较，避免根据比较耗时推测 Secret
     */
    public static boolean matchesHash(String raw, String hashed) {
        if (raw == null || hashed == null || hashed.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(
                sha256(raw).getBytes(StandardCharsets.UTF_8),
                hashed.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Secret 掩码，例如 wk_sk_ab****yz
     */
    public static String secretHint(String secret) {
        if (secret == null || secret.length() <= 12) {
            return "****";
        }
        return secret.substring(0, 9) + "****" + secret.substring(secret.length() - 4);
    }

    private static String randomUrlSafe(int byteLength) {
        byte[] bytes = new byte[byteLength];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
