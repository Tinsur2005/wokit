package cn.tinsur.wokit.pojo.vo;

import cn.tinsur.wokit.pojo.entity.User;
import lombok.Data;

/**
 * 用户视图对象：在实体基础上补充角色名称，密码不返回前端
 *
 * @author Tinsur
 */
@Data
public class UserVO extends User {

    /**
     * 角色名称（管理员/普通用户）
     */
    private String roleName;
}
