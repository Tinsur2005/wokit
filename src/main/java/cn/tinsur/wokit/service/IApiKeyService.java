package cn.tinsur.wokit.service;

import cn.tinsur.wokit.pojo.dto.ApiKeyCreateDTO;
import cn.tinsur.wokit.pojo.entity.ApiKey;
import cn.tinsur.wokit.pojo.vo.ApiKeyVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IApiKeyService extends IService<ApiKey> {

    /**
     * 分页查询：管理员看全部，其他角色只看自己创建的
     */
    IPage<ApiKeyVO> list(Integer page, Integer limit, String appName, String keyType, Integer status, Long currentUserId);

    /**
     * 创建密钥，返回本次生成的 Secret 明文
     */
    ApiKeyVO create(ApiKeyCreateDTO dto, Long currentUserId);

    /**
     * 启用 / 停用
     */
    void changeStatus(Long id, Integer status, Long currentUserId);

    /**
     * 重置 Secret，旧 Secret 与旧 Refresh Token 立即失效
     */
    ApiKeyVO resetSecret(Long id, Long currentUserId);

    /**
     * 删除密钥，同时撤销其签发的 Refresh Token
     */
    void delete(Long id, Long currentUserId);

    /**
     * 根据 Access Key 查找有效应用密钥
     */
    ApiKey findValidKey(String accessKey, String expectedType, String ip);

    /**
     * 校验服务端 Client Credentials
     */
    ApiKey validateServiceCredentials(String accessKey, String secretKey, String ip);

    /**
     * 校验密钥 IP 白名单
     */
    boolean isIpAllowed(ApiKey apiKey, String ip);

    /**
     * 更新最后使用信息
     */
    void touch(Long id, String ip);

    /**
     * 撤销某个 Key 签发的全部 Refresh Token
     */
    void revokeRefreshTokensByKey(Long apiKeyId);
}
