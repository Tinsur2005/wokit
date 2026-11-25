package cn.tinsur.wokit.controller.admin.system;

import cn.tinsur.wokit.exception.ServiceException;
import cn.tinsur.wokit.pojo.entity.Role;
import cn.tinsur.wokit.pojo.query.RoleQuery;
import cn.tinsur.wokit.service.IRoleService;
import cn.tinsur.wokit.util.AuthUtil;
import cn.tinsur.wokit.util.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
 * 角色管理接口（仅管理员）
 *
 * @author Tinsur
 */
@RestController
@RequestMapping("/admin/roles")
public class RoleController {

    @Autowired
    private IRoleService roleService;

    /**
     * 全部角色，供用户表单下拉使用
     */
    @GetMapping("/list")
    public Result<List<Role>> listAll(HttpServletRequest request) {
        checkAdmin(request);
        return Result.ok(roleService.listAll());
    }

    /**
     * 分页查询角色
     */
    @GetMapping
    public Result<IPage<Role>> pageList(RoleQuery roleQuery, HttpServletRequest request) {
        checkAdmin(request);
        return Result.ok(roleService.list(roleQuery));
    }

    /**
     * 按ID查询角色
     */
    @GetMapping("/{id}")
    public Result<Role> getById(@PathVariable Long id, HttpServletRequest request) {
        checkAdmin(request);
        Role role = roleService.getById(id);
        if (role == null) {
            throw new ServiceException("角色不存在或已被删除");
        }
        return Result.ok(role);
    }

    /**
     * 新增角色
     */
    @PostMapping
    public Result add(@RequestBody Role role, HttpServletRequest request) {
        checkAdmin(request);
        roleService.addRole(role);
        return Result.ok("新增成功");
    }

    /**
     * 修改角色
     */
    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @RequestBody Role role, HttpServletRequest request) {
        checkAdmin(request);
        roleService.updateRole(id, role);
        return Result.ok("修改成功");
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id, HttpServletRequest request) {
        checkAdmin(request);
        roleService.deleteRole(id);
        return Result.ok("删除成功");
    }

    /**
     * 查询角色已分配的权限ID
     */
    @GetMapping("/selectPermissionById/{id}")
    public Result<List<Long>> selectPermissionById(@PathVariable Long id, HttpServletRequest request) {
        checkAdmin(request);
        return Result.ok(roleService.selectPermissionById(id));
    }

    /**
     * 保存角色权限
     */
    @PutMapping("/updatePermission/{id}")
    public Result updatePermission(@PathVariable Long id,
                                   @RequestBody Long[] permissionIds,
                                   HttpServletRequest request) {
        checkAdmin(request);
        roleService.updatePermission(id, permissionIds);
        return Result.ok("权限分配成功");
    }

    private void checkAdmin(HttpServletRequest request) {
        if (AuthUtil.getRole(request) != 1) {
            throw new ServiceException("只有管理员可以管理角色");
        }
    }
}
