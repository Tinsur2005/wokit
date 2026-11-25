package cn.tinsur.wokit.controller.admin.system;

import cn.tinsur.wokit.exception.ServiceException;
import cn.tinsur.wokit.pojo.entity.Permission;
import cn.tinsur.wokit.pojo.vo.PermissionVO;
import cn.tinsur.wokit.service.IPermissionService;
import cn.tinsur.wokit.util.AuthUtil;
import cn.tinsur.wokit.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限管理接口（仅管理员）
 *
 * @author Tinsur
 */
@RestController
@RequestMapping("/admin/permissions")
public class PermissionController {

    @Autowired
    private IPermissionService permissionService;

    /**
     * 查询完整权限树
     */
    @GetMapping("/tree")
    public Result<List<PermissionVO>> tree(HttpServletRequest request) {
        checkAdmin(request);
        return Result.ok(permissionService.selectPermissionTree());
    }

    /**
     * 按ID查询权限
     */
    @GetMapping("/{id}")
    public Result<Permission> getById(@PathVariable Long id, HttpServletRequest request) {
        checkAdmin(request);
        Permission permission = permissionService.getById(id);
        if (permission == null) {
            throw new ServiceException("权限不存在或已被删除");
        }
        return Result.ok(permission);
    }

    /**
     * 新增权限
     */
    @PostMapping
    public Result add(@RequestBody Permission permission, HttpServletRequest request) {
        checkAdmin(request);
        permissionService.addPermission(permission);
        return Result.ok("新增成功");
    }

    /**
     * 修改权限
     */
    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @RequestBody Permission permission, HttpServletRequest request) {
        checkAdmin(request);
        permissionService.updatePermission(id, permission);
        return Result.ok("修改成功");
    }

    /**
     * 删除权限
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id, HttpServletRequest request) {
        checkAdmin(request);
        permissionService.deletePermission(id);
        return Result.ok("删除成功");
    }

    private void checkAdmin(HttpServletRequest request) {
        if (AuthUtil.getRole(request) != 1) {
            throw new ServiceException("只有管理员可以管理权限");
        }
    }
}
