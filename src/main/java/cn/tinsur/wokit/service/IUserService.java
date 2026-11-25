package cn.tinsur.wokit.service;

import cn.tinsur.wokit.pojo.dto.UserLoginDTO;
import cn.tinsur.wokit.pojo.dto.UserPasswordDTO;
import cn.tinsur.wokit.pojo.entity.User;
import cn.tinsur.wokit.pojo.query.UserQuery;
import cn.tinsur.wokit.pojo.vo.UserVO;
import cn.tinsur.wokit.util.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IUserService extends IService<User> {

    /**
     * 登录：校验账号、密码、账号状态，成功后签发 Token
     */
    Result login(UserLoginDTO userLoginDTO);

    /**
     * 分页查询用户列表
     */
    IPage<UserVO> list(UserQuery userQuery);

    /**
     * 新增平台账号
     */
    Result add(User user);

    /**
     * 修改用户信息（密码为空表示不修改）
     */
    Result updateUser(Long id, User user);

    /**
     * 启用 / 停用用户
     */
    Result changeStatus(Long id, Integer status);

    /**
     * 当前登录用户修改密码
     */
    Result updatePassword(Long id, UserPasswordDTO userPasswordDTO);

    /**
     * 查询当前登录用户信息（密码置空）
     */
    User currentUser(Long id);

    /**
     * 按ID查询用户详情，带角色信息
     */
    UserVO detail(Long id);

    /**
     * 当前用户信息 + 路由权限树 + 按钮权限列表
     */
    java.util.Map<String, Object> userInfo(Long id);

    /**
     * 删除用户，同时清理用户角色关联
     */
    void deleteUser(Long id);
}
