package cn.tinsur.wokit.pojo.dto;

import lombok.Data;

/**
 * 刷新 / 退出登录请求
 *
 * @author Tinsur
 */
@Data
public class ApiRefreshDTO {

    private String refreshToken;

    private String deviceId;

    private String deviceName;
}
