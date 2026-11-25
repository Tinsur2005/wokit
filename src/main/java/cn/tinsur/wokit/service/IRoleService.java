package cn.tinsur.wokit.service;

import cn.tinsur.wokit.pojo.entity.Role;
import cn.tinsur.wokit.pojo.query.RoleQuery;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IRoleService extends IService<Role> {

    /**
     * 分页查询角色
     */
    IPage<Role> list(RoleQuery roleQuery);

    /**
     * 查询全部角色
     */
    List<Role> listAll();

    /**
     * 查询角色已分配的权限ID
     */
    List<Long> selectPermissionById(Long id);

    /**
     * 重新分配角色权限
     */
    void updatePermission(Long id, Long[] permissionIds);

    /**
     * 新增角色
     */
    void addRole(Role role);

    /**
     * 修改角色
     */
    void updateRole(Long id, Role role);

    /**
     * 删除角色，内置或仍被用户使用的角色不可删除
     */
    void deleteRole(Long id);

    /**
     * 按用户查询角色
     */
    Role getRoleByUserId(Long userId);
}
