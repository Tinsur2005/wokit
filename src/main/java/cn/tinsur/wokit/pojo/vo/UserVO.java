package cn.tinsur.wokit.pojo.vo;

import cn.tinsur.wokit.pojo.entity.User;
import lombok.Data;

/**
 * 用户视图对象：在实体基础上补充角色信息，密码不返回前端
 *
 * @author Tinsur
 */
@Data
public class UserVO extends User {

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色编码
     */
    private String roleCode;
}
