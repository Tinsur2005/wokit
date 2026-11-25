package cn.tinsur.wokit.service.impl;

import cn.tinsur.wokit.exception.ServiceException;
import cn.tinsur.wokit.mapper.PermissionMapper;
import cn.tinsur.wokit.mapper.RoleMapper;
import cn.tinsur.wokit.mapper.RolePermissionMapper;
import cn.tinsur.wokit.mapper.UserRoleMapper;
import cn.tinsur.wokit.pojo.entity.Permission;
import cn.tinsur.wokit.pojo.entity.Role;
import cn.tinsur.wokit.pojo.entity.RolePermission;
import cn.tinsur.wokit.pojo.entity.UserRole;
import cn.tinsur.wokit.pojo.query.RoleQuery;
import cn.tinsur.wokit.service.IRoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 角色服务
 *
 * @author Tinsur
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements IRoleService {

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Override
    public IPage<Role> list(RoleQuery roleQuery) {
        Integer pageNum = ObjectUtils.isEmpty(roleQuery.getPage()) ? 1 : roleQuery.getPage();
        Integer pageSize = ObjectUtils.isEmpty(roleQuery.getLimit()) ? 10 : roleQuery.getLimit();
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(!ObjectUtils.isEmpty(roleQuery.getName()), Role::getName, roleQuery.getName())
                .like(!ObjectUtils.isEmpty(roleQuery.getCode()), Role::getCode, roleQuery.getCode())
                .orderByAsc(Role::getSort)
                .orderByAsc(Role::getId);
        return roleMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<Role> listAll() {
        return list(new LambdaQueryWrapper<Role>()
                .orderByAsc(Role::getSort)
                .orderByAsc(Role::getId));
    }

    @Override
    public List<Long> selectPermissionById(Long id) {
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id));
        return rolePermissions.stream().map(RolePermission::getPermissionId).distinct().toList();
    }

    @Override
    @Transactional
    public void updatePermission(Long id, Long[] permissionIds) {
        Role role = getById(id);
        if (role == null) {
            throw new ServiceException("角色不存在或已被删除");
        }
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, id));
        List<Long> ids = new ArrayList<>();
        // 管理员角色始终拥有全部权限，避免把管理员自己锁在系统外
        if ("admin".equals(role.getCode())) {
            ids = permissionMapper.selectList(new LambdaQueryWrapper<Permission>()
                            .eq(Permission::getStatus, 1))
                    .stream()
                    .map(Permission::getId)
                    .toList();
        } else if (permissionIds != null) {
            Arrays.stream(permissionIds).filter(java.util.Objects::nonNull).distinct().forEach(ids::add);
        }
        for (Long permissionId : ids) {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRoleId(id);
            rolePermission.setPermissionId(permissionId);
            rolePermissionMapper.insert(rolePermission);
        }
    }

    @Override
    public void addRole(Role role) {
        validateRole(role, null);
        role.setId(null);
        role.setBuiltIn(0);
        if (role.getSort() == null) {
            role.setSort(10);
        }
        save(role);
    }

    @Override
    public void updateRole(Long id, Role role) {
        Role dbRole = getById(id);
        if (dbRole == null) {
            throw new ServiceException("角色不存在或已被删除");
        }
        if (dbRole.getBuiltIn() != null && dbRole.getBuiltIn() == 1
                && !ObjectUtils.isEmpty(role.getCode())
                && !role.getCode().equals(dbRole.getCode())) {
            throw new ServiceException("内置角色的编码不允许修改");
        }
        if (ObjectUtils.isEmpty(role.getCode())) {
            role.setCode(dbRole.getCode());
        }
        validateRole(role, id);
        role.setId(id);
        role.setBuiltIn(dbRole.getBuiltIn());
        updateById(role);
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        Role role = getById(id);
        if (role == null) {
            throw new ServiceException("角色不存在或已被删除");
        }
        if (role.getBuiltIn() != null && role.getBuiltIn() == 1) {
            throw new ServiceException("内置角色不允许删除");
        }
        long userCount = userRoleMapper.selectCount(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, id));
        if (userCount > 0) {
            throw new ServiceException("该角色仍被用户使用，请先调整用户角色");
        }
        removeById(id);
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, id));
    }

    @Override
    public Role getRoleByUserId(Long userId) {
        UserRole userRole = userRoleMapper.selectOne(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId).last("LIMIT 1"));
        if (userRole == null) {
            return null;
        }
        return getById(userRole.getRoleId());
    }

    private void validateRole(Role role, Long excludeId) {
        if (ObjectUtils.isEmpty(role.getName()) || role.getName().trim().length() > 50) {
            throw new ServiceException("角色名称不能为空且不能超过 50 个字符");
        }
        if (ObjectUtils.isEmpty(role.getCode()) || role.getCode().trim().length() > 50) {
            throw new ServiceException("角色编码不能为空且不能超过 50 个字符");
        }
        role.setName(role.getName().trim());
        role.setCode(role.getCode().trim());
        long sameCode = count(new LambdaQueryWrapper<Role>()
                .eq(Role::getCode, role.getCode())
                .ne(excludeId != null, Role::getId, excludeId));
        if (sameCode > 0) {
            throw new ServiceException("角色编码已存在，请换一个");
        }
    }
}
