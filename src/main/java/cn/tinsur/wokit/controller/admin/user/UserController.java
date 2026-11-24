package cn.tinsur.wokit.controller.admin.user;

import cn.tinsur.wokit.exception.ServiceException;
import cn.tinsur.wokit.pojo.dto.UserLoginDTO;
import cn.tinsur.wokit.pojo.dto.UserPasswordDTO;
import cn.tinsur.wokit.pojo.entity.User;
import cn.tinsur.wokit.pojo.query.UserQuery;
import cn.tinsur.wokit.pojo.vo.UserVO;
import cn.tinsur.wokit.service.IUserService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 平台用户表 前端控制器
 * </p>
 * 登录、当前用户信息、用户管理（仅管理员）
 *
 * @author Tinsur
 */
@RestController
@RequestMapping("/admin/users")
public class UserController {

    @Autowired
    private IUserService userService;

    /**
     * 登录
     * POST /admin/users/login
     *
     * @param userLoginDTO 登录请求对象（账号+密码）
     * @return Result<String> 登录成功时 data 为 Token
     */
    @PostMapping("/login")
    public Result<String> login(@RequestBody UserLoginDTO userLoginDTO) {
        return userService.login(userLoginDTO);
    }

    /**
     * 当前登录用户信息
     * GET /admin/users/userInfo
     */
    @GetMapping("/userInfo")
    public Result userInfo(HttpServletRequest request) {
        User user = userService.currentUser(AuthUtil.getUserId(request));
        return Result.ok(user);
    }

    /**
     * 分页查询用户列表（仅管理员）
     * GET /admin/users?page=1&limit=10&name=xxx&status=1
     */
    @GetMapping
    public Result<IPage<UserVO>> list(UserQuery userQuery, HttpServletRequest request) {
        checkAdmin(request);
        return Result.ok(userService.list(userQuery));
    }

    /**
     * 根据ID查询用户（仅管理员）
     * GET /admin/users/1
     */
    @GetMapping("/{id}")
    public Result<UserVO> getById(@PathVariable Long id, HttpServletRequest request) {
        checkAdmin(request);
        User user = userService.getById(id);
        if (user == null) {
            throw new ServiceException("用户不存在或已被删除");
        }
        user.setPassword(null);
        UserVO vo = new UserVO();
        org.springframework.beans.BeanUtils.copyProperties(user, vo);
        vo.setRoleName(user.getRole() != null && user.getRole() == 1 ? "管理员" : "普通用户");
        return Result.ok(vo);
    }

    /**
     * 新增平台账号（仅管理员）
     * POST /admin/users
     */
    @PostMapping
    public Result add(@RequestBody User user, HttpServletRequest request) {
        checkAdmin(request);
        return userService.add(user);
    }

    /**
     * 修改用户（仅管理员，密码留空表示不修改）
     * PUT /admin/users/1
     */
    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @RequestBody User user, HttpServletRequest request) {
        checkAdmin(request);
        // 角色和状态走各自接口，这里不覆盖
        user.setRole(null);
        user.setStatus(null);
        return userService.updateUser(id, user);
    }

    /**
     * 启用 / 停用用户（仅管理员）
     * PUT /admin/users/status/1?status=0
     */
    @PutMapping("/status/{id}")
    public Result changeStatus(@PathVariable Long id,
                               @RequestParam Integer status,
                               HttpServletRequest request) {
        checkAdmin(request);
        // 不允许停用自己，避免把自己锁在门外
        if (status == 0 && id.equals(AuthUtil.getUserId(request))) {
            throw new ServiceException("不能停用当前登录账号");
        }
        return userService.changeStatus(id, status);
    }

    /**
     * 删除用户（仅管理员，真实删除）
     * DELETE /admin/users/1
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id, HttpServletRequest request) {
        checkAdmin(request);
        if (id.equals(AuthUtil.getUserId(request))) {
            throw new ServiceException("不能删除当前登录账号");
        }
        userService.removeById(id);
        return Result.ok("删除成功");
    }

    /**
     * 当前登录用户修改密码
     * PUT /admin/users/password
     */
    @PutMapping("/password")
    public Result updatePassword(@RequestBody UserPasswordDTO userPasswordDTO, HttpServletRequest request) {
        return userService.updatePassword(AuthUtil.getUserId(request), userPasswordDTO);
    }

    /**
     * 校验管理员权限
     */
    private void checkAdmin(HttpServletRequest request) {
        if (AuthUtil.getRole(request) != 1) {
            throw new ServiceException("只有管理员可以管理平台账号");
        }
    }
}
