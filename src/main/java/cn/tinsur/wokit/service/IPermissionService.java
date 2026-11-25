package cn.tinsur.wokit.service;

import cn.tinsur.wokit.pojo.entity.Permission;
import cn.tinsur.wokit.pojo.query.RoleQuery;
import cn.tinsur.wokit.pojo.vo.PermissionVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IPermissionService extends IService<Permission> {

    /**
     * 查询完整权限树
     */
    List<PermissionVO> selectPermissionTree();

    /**
     * 把平铺权限列表构建成树
     */
    List<PermissionVO> buildTree(List<Permission> permissions);

    /**
     * 按用户ID查询其角色下已授权的权限
     */
    List<Permission> selectByUserId(Long userId);

    /**
     * 新增权限
     */
    void addPermission(Permission permission);

    /**
     * 修改权限
     */
    void updatePermission(Long id, Permission permission);

    /**
     * 删除权限，同时清掉角色上的授权
     */
    void deletePermission(Long id);
}
