package cn.tinsur.wokit.pojo.dto;

import lombok.Data;

import java.util.Date;

/**
 * 创建 API Key 的请求对象
 *
 * @author Tinsur
 */
@Data
public class ApiKeyCreateDTO {

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 平台：desktop/mobile/server/other
     */
    private String platform;

    /**
     * 类型：app/service
     */
    private String keyType;

    /**
     * 过期时间，空表示长期有效
     */
    private Date expiresAt;

    /**
     * IP 白名单，逗号分隔
     */
    private String ipWhitelist;

    /**
     * 备注
     */
    private String remark;
}
