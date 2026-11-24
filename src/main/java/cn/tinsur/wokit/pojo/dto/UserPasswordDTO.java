package cn.tinsur.wokit.pojo.dto;

import lombok.Data;

/**
 * 修改密码请求对象：原密码 + 新密码
 *
 * @author Tinsur
 */
@Data
public class UserPasswordDTO {

    /**
     * 原密码
     */
    private String oldPassword;

    /**
     * 新密码
     */
    private String newPassword;
}
