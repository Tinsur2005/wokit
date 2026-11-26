package cn.tinsur.wokit.service;

import cn.tinsur.wokit.pojo.dto.ApiLoginDTO;
import cn.tinsur.wokit.pojo.dto.ApiRefreshDTO;
import cn.tinsur.wokit.pojo.dto.UserPasswordDTO;
import cn.tinsur.wokit.pojo.vo.ApiTokenVO;
import cn.tinsur.wokit.util.Result;

import java.util.Map;

public interface IApiAuthService {

    /**
     * App 用户登录：必须携带有效 Access Key，仅校验管理员已创建的用户账号
     */
    Result<ApiTokenVO> login(String accessKey, ApiLoginDTO dto, String ip);

    /**
     * 刷新用户 Token，Refresh Token 轮换
     */
    Result<ApiTokenVO> refresh(ApiRefreshDTO dto, String ip);

    /**
     * 退出登录，撤销 Refresh Token
     */
    Result logout(ApiRefreshDTO dto);

    /**
     * 当前用户信息与权限
     */
    Map<String, Object> me(Long userId);

    /**
     * App 用户修改密码，并撤销全部 Refresh Token
     */
    Result updatePassword(Long userId, UserPasswordDTO dto);

    /**
     * 服务端 Client Credentials 换取 Service Token
     */
    Map<String, Object> serviceToken(String accessKey, String secretKey, String ip);
}
