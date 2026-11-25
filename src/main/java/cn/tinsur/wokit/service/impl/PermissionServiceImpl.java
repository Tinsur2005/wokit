package cn.tinsur.wokit.service.impl;

import cn.tinsur.wokit.exception.ServiceException;
import cn.tinsur.wokit.mapper.PermissionMapper;
import cn.tinsur.wokit.mapper.RolePermissionMapper;
import cn.tinsur.wokit.mapper.UserRoleMapper;
import cn.tinsur.wokit.pojo.entity.Permission;
import cn.tinsur.wokit.pojo.entity.RolePermission;
import cn.tinsur.wokit.pojo.entity.UserRole;
import cn.tinsur.wokit.pojo.vo.PermissionVO;
import cn.tinsur.wokit.service.IPermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 权限服务：负责权限树、角色授权查询与权限维护
 *
 * @author Tinsur
 */
@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements IPermissionService {

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Override
    public List<PermissionVO> selectPermissionTree() {
        List<Permission> permissions = list(new LambdaQueryWrapper<Permission>()
                .orderByAsc(Permission::getSort)
                .orderByAsc(Permission::getId));
        return buildTree(permissions);
    }

    @Override
    public List<PermissionVO> buildTree(List<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, PermissionVO> nodeMap = new HashMap<>();
        for (Permission permission : permissions) {
            PermissionVO vo = new PermissionVO();
            BeanUtils.copyProperties(permission, vo);
            vo.setChildren(new ArrayList<>());
            nodeMap.put(permission.getId(), vo);
        }
        List<PermissionVO> roots = new ArrayList<>();
        for (PermissionVO node : nodeMap.values()) {
            Long parentId = node.getParentId();
            PermissionVO parent = parentId == null ? null : nodeMap.get(parentId);
            if (parentId == null || parentId == 0 || parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        sortTree(roots);
        return roots;
    }

    private void sortTree(List<PermissionVO> nodes) {
        nodes.sort((a, b) -> {
            int sortA = a.getSort() == null ? 0 : a.getSort();
            int sortB = b.getSort() == null ? 0 : b.getSort();
            if (sortA != sortB) {
                return Integer.compare(sortA, sortB);
            }
            return Long.compare(a.getId(), b.getId());
        });
        nodes.forEach(node -> sortTree(node.getChildren()));
    }

    @Override
    public List<Permission> selectByUserId(Long userId) {
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (userRoles.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).distinct().toList();
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, roleIds));
        List<Long> permissionIds = rolePermissions.stream()
                .map(RolePermission::getPermissionId)
                .distinct()
                .toList();
        if (permissionIds.isEmpty()) {
            return new ArrayList<>();
        }
        return list(new LambdaQueryWrapper<Permission>()
                .in(Permission::getId, permissionIds)
                .eq(Permission::getStatus, 1)
                .orderByAsc(Permission::getSort)
                .orderByAsc(Permission::getId));
    }

    @Override
    public void addPermission(Permission permission) {
        validate(permission, null);
        permission.setId(null);
        if (permission.getStatus() == null) {
            permission.setStatus(1);
        }
        if (permission.getSort() == null) {
            permission.setSort(0);
        }
        save(permission);
    }

    @Override
    public void updatePermission(Long id, Permission permission) {
        Permission dbPermission = getById(id);
        if (dbPermission == null) {
            throw new ServiceException("权限不存在或已被删除");
        }
        validate(permission, id);
        permission.setId(id);
        updateById(permission);
    }

    @Override
    @Transactional
    public void deletePermission(Long id) {
        Permission permission = getById(id);
        if (permission == null) {
            throw new ServiceException("权限不存在或已被删除");
        }
        long childCount = count(new LambdaQueryWrapper<Permission>().eq(Permission::getParentId, id));
        if (childCount > 0) {
            throw new ServiceException("请先删除该权限下的子权限");
        }
        removeById(id);
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getPermissionId, id));
    }

    private void validate(Permission permission, Long excludeId) {
        if (ObjectUtils.isEmpty(permission.getName()) || permission.getName().trim().length() > 50) {
            throw new ServiceException("权限名称不能为空且不能超过 50 个字符");
        }
        permission.setName(permission.getName().trim());
        if (permission.getType() == null || permission.getType() < 0 || permission.getType() > 2) {
            throw new ServiceException("请选择正确的权限类型");
        }
        Long parentId = permission.getParentId() == null ? 0L : permission.getParentId();
        permission.setParentId(parentId);
        if (parentId != 0L) {
            if (excludeId != null && parentId.equals(excludeId)) {
                throw new ServiceException("上级权限不能选择自己");
            }
            Permission parent = getById(parentId);
            if (parent == null) {
                throw new ServiceException("上级权限不存在");
            }
            if (parent.getType() == 2) {
                throw new ServiceException("按钮下不能再挂子权限");
            }
            if (excludeId != null && isDescendant(parentId, excludeId)) {
                throw new ServiceException("上级权限不能选择自己的下级");
            }
        }
        if (permission.getType() == 2) {
            if (ObjectUtils.isEmpty(permission.getPermissionValue())) {
                throw new ServiceException("按钮权限必须填写权限标识");
            }
            permission.setPermissionValue(permission.getPermissionValue().trim());
            long sameValueCount = count(new LambdaQueryWrapper<Permission>()
                    .eq(Permission::getType, 2)
                    .eq(Permission::getPermissionValue, permission.getPermissionValue())
                    .ne(excludeId != null, Permission::getId, excludeId));
            if (sameValueCount > 0) {
                throw new ServiceException("权限标识已存在，请换一个");
            }
            permission.setPath(null);
            permission.setIcon(null);
        } else {
            if (ObjectUtils.isEmpty(permission.getPath())) {
                throw new ServiceException("路由和菜单必须填写路由地址");
            }
            permission.setPath(permission.getPath().trim());
            long samePathCount = count(new LambdaQueryWrapper<Permission>()
                    .ne(Permission::getType, 2)
                    .eq(Permission::getPath, permission.getPath())
                    .ne(excludeId != null, Permission::getId, excludeId));
            if (samePathCount > 0) {
                throw new ServiceException("路由地址已存在，请换一个");
            }
            permission.setPermissionValue(null);
        }
    }

    private boolean isDescendant(Long candidateParentId, Long nodeId) {
        Long cursor = candidateParentId;
        int guard = 0;
        while (cursor != null && cursor != 0 && guard++ < 20) {
            if (cursor.equals(nodeId)) {
                return true;
            }
            Permission parent = getById(cursor);
            cursor = parent == null ? 0L : parent.getParentId();
        }
        return false;
    }
}
