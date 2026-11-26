package cn.tinsur.wokit.pojo.vo;

import lombok.Data;

import java.util.Map;

/**
 * App 登录 / 刷新返回的 Token 信息
 *
 * @author Tinsur
 */
@Data
public class ApiTokenVO {

    private String accessToken;

    private String refreshToken;

    private String tokenType = "Bearer";

    private Long expiresIn;

    private Map<String, Object> user;
}
