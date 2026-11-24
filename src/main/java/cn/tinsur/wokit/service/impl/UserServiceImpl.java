package cn.tinsur.wokit.service.impl;

import cn.tinsur.wokit.exception.ServiceException;
import cn.tinsur.wokit.mapper.UserMapper;
import cn.tinsur.wokit.pojo.dto.UserLoginDTO;
import cn.tinsur.wokit.pojo.dto.UserPasswordDTO;
import cn.tinsur.wokit.pojo.entity.User;
import cn.tinsur.wokit.pojo.query.UserQuery;
import cn.tinsur.wokit.pojo.vo.UserVO;
import cn.tinsur.wokit.service.IUserService;
import cn.tinsur.wokit.util.JwtUtil;
import cn.tinsur.wokit.util.PasswordUtil;
import cn.tinsur.wokit.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 平台用户表 服务实现类
 * </p>
 *
 * @author Tinsur
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    /**
     * 登录：校验账号、密码、账号状态，成功后签发 Token
     */
    @Override
    public Result login(UserLoginDTO userLoginDTO) {
        // 1.账号密码不能为空
        if (ObjectUtils.isEmpty(userLoginDTO.getName()) || ObjectUtils.isEmpty(userLoginDTO.getPassword())) {
            return Result.error("请输入账号和密码");
        }
        // 2.根据账号查找用户
        User dbUser = getOne(new QueryWrapper<User>().eq("name", userLoginDTO.getName().trim()));
        if (dbUser == null) {
            return Result.error("账号不存在");
        }
        // 3.库中存的是 BCrypt 哈希，必须用 matches 比对
        if (!PasswordUtil.matches(userLoginDTO.getPassword(), dbUser.getPassword())) {
            return Result.error("密码错误，请重新输入");
        }
        // 4.账号密码正确时，判断用户状态
        if (dbUser.getStatus() != null && dbUser.getStatus() == 0) {
            return Result.error("该账号已被停用，请联系管理员");
        }
        // 5.登录校验成功，生成 Token
        Map<String, Object> map = new HashMap<>();
        map.put("id", dbUser.getId());
        map.put("name", dbUser.getName());
        map.put("role", dbUser.getRole() == null ? 0 : dbUser.getRole());
        String token = JwtUtil.creatToken(map);
        return Result.ok("登录成功", token);
    }

    /**
     * 分页查询用户列表
     */
    @Override
    public IPage<UserVO> list(UserQuery userQuery) {
        // 1.分页参数缺省处理
        Integer pageNum = ObjectUtils.isEmpty(userQuery.getPage()) ? 1 : userQuery.getPage();
        Integer pageSize = ObjectUtils.isEmpty(userQuery.getLimit()) ? 10 : userQuery.getLimit();
        IPage<User> page = new Page<>(pageNum, pageSize);

        // 2.账号或姓名模糊查询，邮箱、状态、角色精确筛选
        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper
                .and(!ObjectUtils.isEmpty(userQuery.getName()), wrapper -> wrapper
                        .like(User::getName, userQuery.getName())
                        .or()
                        .like(User::getRealName, userQuery.getName()))
                .like(!ObjectUtils.isEmpty(userQuery.getEmail()), User::getEmail, userQuery.getEmail())
                .eq(!ObjectUtils.isEmpty(userQuery.getStatus()), User::getStatus, userQuery.getStatus())
                .eq(!ObjectUtils.isEmpty(userQuery.getRole()), User::getRole, userQuery.getRole())
                .orderByDesc(User::getCreateTime);
        IPage<User> userPage = page(page, lambdaQueryWrapper);

        // 3.把当前页的 User 转成 UserVO，密码不返回前端
        List<UserVO> userVOList = userPage.getRecords().stream()
                .map(user -> {
                    UserVO vo = new UserVO();
                    BeanUtils.copyProperties(user, vo);
                    vo.setPassword(null);
                    vo.setRoleName(user.getRole() != null && user.getRole() == 1 ? "管理员" : "普通用户");
                    return vo;
                })
                .toList();

        // 4.返回 UserVO 类型的分页
        IPage<UserVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        voPage.setRecords(userVOList);
        return voPage;
    }

    /**
     * 新增平台账号
     */
    @Override
    public Result add(User user) {
        // 1.账号、密码必填
        if (ObjectUtils.isEmpty(user.getName()) || ObjectUtils.isEmpty(user.getPassword())) {
            throw new ServiceException("账号和密码不能为空");
        }
        // 2.账号不能重复
        if (isExists(user.getName().trim(), null)) {
            throw new ServiceException("账号已存在，换一个账号试试吧");
        }
        // 3.密码加密后入库，未指定角色和状态时给默认值
        user.setId(null);
        user.setName(user.getName().trim());
        user.setPassword(PasswordUtil.hash(user.getPassword()));
        if (user.getRole() == null) {
            user.setRole(0);
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        save(user);
        return Result.ok("新增成功");
    }

    /**
     * 修改用户信息：密码留空表示不修改
     */
    @Override
    public Result updateUser(Long id, User user) {
        // 1.先确认用户存在
        User dbUser = getById(id);
        if (dbUser == null) {
            throw new ServiceException("用户不存在或已被删除");
        }
        // 2.账号修改时校验是否重复
        if (!ObjectUtils.isEmpty(user.getName()) && isExists(user.getName().trim(), id)) {
            throw new ServiceException("账号已存在，换一个账号试试吧");
        }
        // 3.密码留空置 null，让 MyBatis-Plus 跳过该列；填写了则 BCrypt 加密
        if (ObjectUtils.isEmpty(user.getPassword())) {
            user.setPassword(null);
        } else {
            user.setPassword(PasswordUtil.hash(user.getPassword()));
        }
        if (!ObjectUtils.isEmpty(user.getName())) {
            user.setName(user.getName().trim());
        }
        user.setId(id);
        updateById(user);
        return Result.ok("修改成功");
    }

    /**
     * 启用 / 停用用户
     */
    @Override
    public Result changeStatus(Long id, Integer status) {
        User dbUser = getById(id);
        if (dbUser == null) {
            throw new ServiceException("用户不存在或已被删除");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new ServiceException("状态值不正确");
        }
        User updateUser = new User();
        updateUser.setId(id);
        updateUser.setStatus(status);
        updateById(updateUser);
        return Result.ok(status == 1 ? "已启用该账号" : "已停用该账号");
    }

    /**
     * 当前登录用户修改密码
     */
    @Override
    public Result updatePassword(Long id, UserPasswordDTO userPasswordDTO) {
        User user = getById(id);
        if (user == null) {
            throw new ServiceException("用户不存在或已被删除");
        }
        // 1.原密码必须正确
        if (!PasswordUtil.matches(userPasswordDTO.getOldPassword(), user.getPassword())) {
            return Result.error("原密码错误");
        }
        // 2.新密码不能与原密码相同
        if (PasswordUtil.matches(userPasswordDTO.getNewPassword(), user.getPassword())) {
            return Result.error("新密码不能与原密码相同");
        }
        if (ObjectUtils.isEmpty(userPasswordDTO.getNewPassword())
                || userPasswordDTO.getNewPassword().length() < 6) {
            return Result.error("新密码长度不能少于6位");
        }
        // 3.更新密码（BCrypt 加密后入库）
        User updateUser = new User();
        updateUser.setId(id);
        updateUser.setPassword(PasswordUtil.hash(userPasswordDTO.getNewPassword()));
        updateById(updateUser);
        return Result.ok("密码修改成功，请重新登录");
    }

    /**
     * 查询当前登录用户信息
     */
    @Override
    public User currentUser(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new ServiceException("用户不存在或已被删除");
        }
        user.setPassword(null);
        return user;
    }

    /**
     * 判断账号是否已存在，excludeId 用于修改时排除自己
     */
    public Boolean isExists(String name, Long excludeId) {
        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(User::getName, name)
                .ne(excludeId != null, User::getId, excludeId);
        return count(lambdaQueryWrapper) > 0;
    }
}
