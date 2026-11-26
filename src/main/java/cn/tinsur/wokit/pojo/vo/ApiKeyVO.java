package cn.tinsur.wokit.pojo.vo;

import cn.tinsur.wokit.pojo.entity.ApiKey;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * API Key 页面视图：不返回 Secret 哈希
 *
 * @author Tinsur
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ApiKeyVO extends ApiKey {

    /**
     * 创建或重置时一次性返回的 Secret 明文，列表接口永远为 null
     */
    private String secretKey;

    /**
     * 是否仅本次展示 Secret
     */
    private Boolean secretOnce;
}
