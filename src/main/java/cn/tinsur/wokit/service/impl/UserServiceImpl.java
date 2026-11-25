package cn.tinsur.wokit.service.impl;

import cn.tinsur.wokit.exception.ServiceException;
import cn.tinsur.wokit.mapper.RoleMapper;
import cn.tinsur.wokit.mapper.UserMapper;
import cn.tinsur.wokit.mapper.UserRoleMapper;
import cn.tinsur.wokit.pojo.dto.UserLoginDTO;
import cn.tinsur.wokit.pojo.dto.UserPasswordDTO;
import cn.tinsur.wokit.pojo.entity.Permission;
import cn.tinsur.wokit.pojo.entity.Role;
import cn.tinsur.wokit.pojo.entity.User;
import cn.tinsur.wokit.pojo.entity.UserRole;
import cn.tinsur.wokit.pojo.query.UserQuery;
import cn.tinsur.wokit.pojo.vo.PermissionVO;
import cn.tinsur.wokit.pojo.vo.UserVO;
import cn.tinsur.wokit.service.IPermissionService;
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
 * <p>
 * 平台用户表 服务实现类
 * </p>
 *
 * @author Tinsur
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private IPermissionService permissionService;

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
                    return vo;
                })
                .toList();
        fillRoles(userVOList);

        // 4.返回 UserVO 类型的分页
        IPage<UserVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        voPage.setRecords(userVOList);
        return voPage;
    }

    /**
     * 批量回填用户角色，避免逐条查库
     */
    private void fillRoles(List<UserVO> userVOList) {
        if (userVOList.isEmpty()) {
            return;
        }
        List<Long> userIds = userVOList.stream().map(UserVO::getId).toList();
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().in(UserRole::getUserId, userIds));
        if (userRoles.isEmpty()) {
            userVOList.forEach(vo -> {
                vo.setRoleName("普通用户");
                vo.setRoleCode("user");
            });
            return;
        }
        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).distinct().toList();
        Map<Long, Role> roleMap = roleMapper.selectBatchIds(roleIds).stream()
                .collect(Collectors.toMap(Role::getId, role -> role));
        Map<Long, UserRole> userRoleMap = userRoles.stream()
                .collect(Collectors.toMap(UserRole::getUserId, item -> item, (a, b) -> a));
        userVOList.forEach(vo -> {
            UserRole userRole = userRoleMap.get(vo.getId());
            Role role = userRole == null ? null : roleMap.get(userRole.getRoleId());
            if (role == null) {
                vo.setRoleName("普通用户");
                vo.setRoleCode("user");
            } else {
                vo.setRoleId(role.getId());
                vo.setRoleName(role.getName());
                vo.setRoleCode(role.getCode());
            }
        });
    }

    /**
     * 新增平台账号
     */
    @Override
    @Transactional
    public Result add(User user) {
        // 1.账号、密码必填
        if (ObjectUtils.isEmpty(user.getName()) || ObjectUtils.isEmpty(user.getPassword())) {
            throw new ServiceException("账号和密码不能为空");
        }
        // 2.账号不能重复
        if (isExists(user.getName().trim(), null)) {
            throw new ServiceException("账号已存在，换一个账号试试吧");
        }
        // 3.解析角色，未选择时默认普通用户
        Role role = resolveRole(user.getRoleId());
        user.setId(null);
        user.setName(user.getName().trim());
        user.setPassword(PasswordUtil.hash(user.getPassword()));
        user.setRole("admin".equals(role.getCode()) ? 1 : 0);
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        save(user);
        saveUserRole(user.getId(), role.getId());
        return Result.ok("新增成功");
    }

    /**
     * 修改用户信息：密码留空表示不修改
     */
    @Override
    @Transactional
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
        // 4.角色变更时同步 sys_user_role 与 sys_user.role
        Role role = user.getRoleId() == null ? null : resolveRole(user.getRoleId());
        if (role != null) {
            if (id.equals(1L) && !"admin".equals(role.getCode())) {
                throw new ServiceException("不能修改当前超级管理员的角色");
            }
            user.setRole("admin".equals(role.getCode()) ? 1 : 0);
        }
        user.setId(id);
        updateById(user);
        if (role != null) {
            saveUserRole(id, role.getId());
        }
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

    @Override
    public UserVO detail(Long id) {
        User user = currentUser(id);
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        Role role = getRoleByUserId(id);
        if (role != null) {
            vo.setRoleId(role.getId());
            vo.setRoleName(role.getName());
            vo.setRoleCode(role.getCode());
        } else {
            vo.setRoleName("普通用户");
            vo.setRoleCode("user");
        }
        return vo;
    }

    @Override
    public Map<String, Object> userInfo(Long id) {        User user = currentUser(id);
        Role role = getRoleByUserId(id);
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        if (role != null) {
            vo.setRoleId(role.getId());
            vo.setRoleName(role.getName());
            vo.setRoleCode(role.getCode());
        } else {
            vo.setRoleName("普通用户");
            vo.setRoleCode("user");
        }

        List<Permission> permissionList = permissionService.selectByUserId(id);
        List<String> btnList = permissionList.stream()
                .filter(permission -> permission.getType() != null && permission.getType() == 2)
                .map(Permission::getPermissionValue)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<PermissionVO> routerList = permissionService.buildTree(permissionList.stream()
                .filter(permission -> permission.getType() == null || permission.getType() != 2)
                .toList());

        Map<String, Object> result = new HashMap<>();
        result.put("user", vo);
        result.put("routerList", routerList);
        result.put("btnList", btnList);
        return result;
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new ServiceException("用户不存在或已被删除");
        }
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id));
        removeById(id);
    }

    private Role getRoleByUserId(Long userId) {
        UserRole userRole = userRoleMapper.selectOne(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId).last("LIMIT 1"));
        if (userRole == null) {
            return null;
        }
        return roleMapper.selectById(userRole.getRoleId());
    }

    private Role resolveRole(Long roleId) {
        if (roleId == null) {
            Role defaultRole = roleMapper.selectOne(
                    new LambdaQueryWrapper<Role>().eq(Role::getCode, "user"));
            if (defaultRole == null) {
                throw new ServiceException("默认普通用户角色不存在，请先执行 RBAC 初始化脚本");
            }
            return defaultRole;
        }
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new ServiceException("所选角色不存在或已被删除");
        }
        return role;
    }

    private void saveUserRole(Long userId, Long roleId) {
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRoleMapper.insert(userRole);
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
