package cn.tinsur.wokit.pojo.query;

import lombok.Data;

@Data
public class UserQuery {

    /**
     * 账号或姓名关键字
     */
    private String name;

    /**
     * 邮箱关键字
     */
    private String email;

    /**
     * 状态（0：停用，1：正常）
     */
    private Integer status;

    /**
     * 角色（0：普通用户，1：管理员）
     */
    private Integer role;

    private Integer page;

    private Integer limit;
}
