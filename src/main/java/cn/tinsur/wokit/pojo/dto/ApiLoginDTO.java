package cn.tinsur.wokit.pojo.dto;

import lombok.Data;

/**
 * App 登录请求
 *
 * @author Tinsur
 */
@Data
public class ApiLoginDTO {

    private String username;

    private String password;

    /**
     * 设备标识，选填
     */
    private String deviceId;

    /**
     * 设备名称，选填
     */
    private String deviceName;
}
