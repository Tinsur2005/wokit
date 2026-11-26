package cn.tinsur.wokit.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * API Key：应用身份凭证。
 * Access Key 可公开用于 App 标识，Secret Key 只允许服务端保存。
 *
 * @author Tinsur
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("wokit_api_key")
public class ApiKey implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 密钥所属用户ID，创建人
     */
    @TableField("owner_user_id")
    private Long ownerUserId;

    /**
     * 应用名称
     */
    @TableField("app_name")
    private String appName;

    /**
     * 平台：desktop/mobile/server/other
     */
    private String platform;

    /**
     * 类型：app（App登录）/service（服务对接）
     */
    @TableField("key_type")
    private String keyType;

    /**
     * 公开的应用标识
     */
    @TableField("access_key")
    private String accessKey;

    /**
     * Secret Key 的 SHA-256 哈希，不保存明文
     */
    @TableField("secret_hash")
    @JsonIgnore
    private String secretHash;

    /**
     * Secret 掩码，仅用于页面展示
     */
    @TableField("secret_hint")
    private String secretHint;

    /**
     * Secret 版本，重置后递增
     */
    @TableField("secret_version")
    private Integer secretVersion;

    /**
     * 授权范围，逗号分隔
     */
    private String scopes;

    /**
     * IP 白名单，逗号分隔，为空表示不限制
     */
    @TableField("ip_whitelist")
    private String ipWhitelist;

    /**
     * 状态（0：停用，1：启用）
     */
    private Integer status;

    /**
     * 过期时间，空表示长期有效
     */
    @TableField("expires_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date expiresAt;

    /**
     * 最后使用时间
     */
    @TableField("last_used_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date lastUsedAt;

    /**
     * 最后使用IP
     */
    @TableField("last_used_ip")
    private String lastUsedIp;

    /**
     * 备注
     */
    private String remark;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}
