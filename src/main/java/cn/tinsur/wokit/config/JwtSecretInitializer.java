package cn.tinsur.wokit.config;

import cn.tinsur.wokit.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * 启动时把配置中的 JWT 密钥注入 JwtUtil，避免继续依赖硬编码密钥。
 *
 * @author Tinsur
 */
@Component
public class JwtSecretInitializer {

    @Value("${wokit.jwt.secret:tinsur-wokit}")
    private String secret;

    @PostConstruct
    public void init() {
        JwtUtil.setKey(secret);
    }
}
