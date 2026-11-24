package cn.tinsur.wokit.pojo.dto;

import lombok.Data;

/**
 * 后台登录请求对象：账号 + 密码
 *
 * @author Tinsur
 */
@Data
public class UserLoginDTO {

    /**
     * 登录账号
     */
    private String name;

    /**
     * 密码
     */
    private String password;
}
