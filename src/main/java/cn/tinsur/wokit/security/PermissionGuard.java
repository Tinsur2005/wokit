package cn.tinsur.wokit.security;

import cn.tinsur.wokit.exception.ApiException;
import cn.tinsur.wokit.mapper.UserMapper;
import cn.tinsur.wokit.pojo.entity.Permission;
import cn.tinsur.wokit.pojo.entity.User;
import cn.tinsur.wokit.service.IPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 统一权限判断：管理员直接放行，其他角色按 RBAC 的菜单/按钮权限判断。
 * API Key 页面、接口文档页面是否授权给某个角色，完全由管理员在角色权限中配置。
 *
 * @author Tinsur
 */
@Component
public class PermissionGuard {

    public static final String API_KEY_MENU = "/settings/api-keys";
    public static final String API_DOC_MENU = "/settings/api-docs";

    @Autowired
    private IPermissionService permissionService;

    @Autowired
    private UserMapper userMapper;

    public boolean isAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        User user = userMapper.selectById(userId);
        return user != null && Objects.equals(user.getRole(), 1);
    }

    public List<Permission> permissions(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return permissionService.selectByUserId(userId);
    }

    public boolean hasMenu(Long userId, String path) {
        if (isAdmin(userId)) {
            return true;
        }
        return permissions(userId).stream()
                .anyMatch(p -> p.getType() != null && p.getType() != 2 && path.equals(p.getPath()));
    }

    public boolean hasValue(Long userId, String value) {
        if (isAdmin(userId)) {
            return true;
        }
        return permissions(userId).stream()
                .anyMatch(p -> p.getType() != null && p.getType() == 2 && value.equals(p.getPermissionValue()));
    }

    public void requireMenu(Long userId, String path) {
        if (!hasMenu(userId, path)) {
            throw new ApiException(403, "当前账号没有访问该页面的权限");
        }
    }

    public void requireValue(Long userId, String value) {
        if (!hasValue(userId, value)) {
            throw new ApiException(403, "当前账号没有执行该操作的权限");
        }
    }

    /**
     * API Key 的所属用户是否仍有管理 API Key 的权限。
     * 一旦管理员收回了该角色的菜单/按钮权限，对应应用也不能继续登录。
     */
    public boolean hasApiKeyAccess(Long userId) {
        if (isAdmin(userId)) {
            return true;
        }
        return permissions(userId).stream().anyMatch(p ->
                API_KEY_MENU.equals(p.getPath())
                        || (p.getPermissionValue() != null && p.getPermissionValue().startsWith("api:key:")));
    }
}
