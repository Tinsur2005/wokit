-- Wokit 工作助手 RBAC 与系统配置升级脚本
-- 适用于已有 wokit 数据库；可重复执行，角色与权限数据会按脚本重新初始化

USE `wokit`;

-- ============================================================
-- 1. 角色表
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `name`        VARCHAR(50)  NOT NULL COMMENT '角色名称',
    `code`        VARCHAR(50)  NOT NULL COMMENT '角色编码',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '角色描述',
    `built_in`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否内置（0：否，1：是）',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统角色表';

-- ============================================================
-- 2. 权限表（0 路由目录、1 菜单、2 按钮）
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_permission` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    `parent_id`        BIGINT       NOT NULL DEFAULT 0 COMMENT '父权限ID，0为根',
    `name`             VARCHAR(50)  NOT NULL COMMENT '权限名称',
    `type`             TINYINT      NOT NULL COMMENT '类型（0：路由/目录，1：菜单，2：按钮）',
    `path`             VARCHAR(120) DEFAULT NULL COMMENT '路由地址',
    `permission_value` VARCHAR(100) DEFAULT NULL COMMENT '按钮权限标识',
    `icon`             VARCHAR(50)  DEFAULT NULL COMMENT '菜单图标名称',
    `sort`             INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `status`           TINYINT      NOT NULL DEFAULT 1 COMMENT '状态（0：停用，1：启用）',
    `create_time`      DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_time`      DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_permission_parent` (`parent_id`),
    KEY `idx_permission_type` (`type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统权限表';

-- ============================================================
-- 3. 用户角色关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `id`      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`),
    KEY `idx_user_role_role` (`role_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户角色关联表';

-- ============================================================
-- 4. 角色权限关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_role_permission` (
    `id`            BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `role_id`       BIGINT NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT NOT NULL COMMENT '权限ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    KEY `idx_role_permission_permission` (`permission_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色权限关联表';

-- ============================================================
-- 5. 系统配置表（键值对）
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_config` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_key`   VARCHAR(60)  NOT NULL COMMENT '配置键',
    `config_value` VARCHAR(500) DEFAULT NULL COMMENT '配置值',
    `description`  VARCHAR(200) DEFAULT NULL COMMENT '配置说明',
    `update_time`  DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统配置表';

-- ============================================================
-- 6. 初始化角色
-- ============================================================
DELETE FROM `sys_role_permission`;
DELETE FROM `sys_user_role`;
DELETE FROM `sys_role`;

INSERT INTO `sys_role` (`id`, `name`, `code`, `description`, `built_in`, `sort`, `create_time`, `update_time`)
VALUES
(1, '管理员', 'admin', '拥有工作台与系统设置的全部权限', 1, 1, NOW(), NOW()),
(2, '普通用户', 'user', '可使用工作台全部业务功能，不能进入系统设置', 1, 2, NOW(), NOW());

-- ============================================================
-- 7. 初始化权限树
-- ============================================================
DELETE FROM `sys_permission`;

INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `permission_value`, `icon`, `sort`, `status`, `create_time`, `update_time`)
VALUES
-- 路由/目录
(100, 0, '工作台', 0, '/', NULL, 'HomeFilled', 10, 1, NOW(), NOW()),
(200, 0, '系统设置', 0, '/settings', NULL, 'Setting', 20, 1, NOW(), NOW()),
-- 工作台菜单
(101, 100, '首页', 1, '/dashboard', NULL, 'HomeFilled', 1, 1, NOW(), NOW()),
(102, 100, '每日任务', 1, '/tasks', NULL, 'Calendar', 2, 1, NOW(), NOW()),
(103, 100, '周报记录', 1, '/weeklies', NULL, 'DocumentChecked', 3, 1, NOW(), NOW()),
(104, 100, '备忘录', 1, '/memos', NULL, 'Memo', 4, 1, NOW(), NOW()),
(105, 100, '工时登记', 1, '/worklogs', NULL, 'Timer', 5, 1, NOW(), NOW()),
(106, 100, '计时', 1, '/timers', NULL, 'AlarmClock', 6, 1, NOW(), NOW()),
(107, 100, '项目管理', 1, '/projects', NULL, 'FolderOpened', 7, 1, NOW(), NOW()),
-- 系统设置菜单
(201, 200, '系统信息', 1, '/settings/general', NULL, 'Setting', 1, 1, NOW(), NOW()),
(202, 200, '用户管理', 1, '/settings/users', NULL, 'User', 2, 1, NOW(), NOW()),
(203, 200, '角色管理', 1, '/settings/roles', NULL, 'Avatar', 3, 1, NOW(), NOW()),
(204, 200, '权限管理', 1, '/settings/permissions', NULL, 'Key', 4, 1, NOW(), NOW()),
-- 每日任务按钮
(1101, 102, '新增任务', 2, NULL, 'task:add', NULL, 1, 1, NOW(), NOW()),
(1102, 102, '编辑任务', 2, NULL, 'task:edit', NULL, 2, 1, NOW(), NOW()),
(1103, 102, '删除任务', 2, NULL, 'task:delete', NULL, 3, 1, NOW(), NOW()),
(1104, 102, '切换完成状态', 2, NULL, 'task:toggle', NULL, 4, 1, NOW(), NOW()),
-- 周报按钮
(1201, 103, '新增周报', 2, NULL, 'weekly:add', NULL, 1, 1, NOW(), NOW()),
(1202, 103, '编辑周报', 2, NULL, 'weekly:edit', NULL, 2, 1, NOW(), NOW()),
(1203, 103, '删除周报', 2, NULL, 'weekly:delete', NULL, 3, 1, NOW(), NOW()),
(1204, 103, '导出周报', 2, NULL, 'weekly:export', NULL, 4, 1, NOW(), NOW()),
-- 备忘录按钮
(1301, 104, '新建备忘', 2, NULL, 'memo:add', NULL, 1, 1, NOW(), NOW()),
(1302, 104, '编辑备忘', 2, NULL, 'memo:edit', NULL, 2, 1, NOW(), NOW()),
(1303, 104, '删除备忘', 2, NULL, 'memo:delete', NULL, 3, 1, NOW(), NOW()),
(1304, 104, '置顶备忘', 2, NULL, 'memo:pin', NULL, 4, 1, NOW(), NOW()),
-- 工时按钮
(1401, 105, '登记工时', 2, NULL, 'worklog:add', NULL, 1, 1, NOW(), NOW()),
(1402, 105, '编辑工时', 2, NULL, 'worklog:edit', NULL, 2, 1, NOW(), NOW()),
(1403, 105, '删除工时', 2, NULL, 'worklog:delete', NULL, 3, 1, NOW(), NOW()),
(1404, 105, '查询工时', 2, NULL, 'worklog:search', NULL, 4, 1, NOW(), NOW()),
-- 计时按钮
(1501, 106, '新建计时', 2, NULL, 'timer:add', NULL, 1, 1, NOW(), NOW()),
(1502, 106, '编辑计时', 2, NULL, 'timer:edit', NULL, 2, 1, NOW(), NOW()),
(1503, 106, '删除计时', 2, NULL, 'timer:delete', NULL, 3, 1, NOW(), NOW()),
-- 项目按钮
(1601, 107, '新建项目', 2, NULL, 'project:add', NULL, 1, 1, NOW(), NOW()),
(1602, 107, '编辑项目', 2, NULL, 'project:edit', NULL, 2, 1, NOW(), NOW()),
(1603, 107, '删除项目', 2, NULL, 'project:delete', NULL, 3, 1, NOW(), NOW()),
(1604, 107, '新建分类', 2, NULL, 'project:category:add', NULL, 4, 1, NOW(), NOW()),
(1605, 107, '编辑分类', 2, NULL, 'project:category:edit', NULL, 5, 1, NOW(), NOW()),
(1606, 107, '删除分类', 2, NULL, 'project:category:delete', NULL, 6, 1, NOW(), NOW()),
-- 用户管理按钮
(2101, 202, '添加用户', 2, NULL, 'user:add', NULL, 1, 1, NOW(), NOW()),
(2102, 202, '编辑用户', 2, NULL, 'user:edit', NULL, 2, 1, NOW(), NOW()),
(2103, 202, '删除用户', 2, NULL, 'user:delete', NULL, 3, 1, NOW(), NOW()),
(2104, 202, '启用停用用户', 2, NULL, 'user:status', NULL, 4, 1, NOW(), NOW()),
-- 角色管理按钮
(2201, 203, '添加角色', 2, NULL, 'role:add', NULL, 1, 1, NOW(), NOW()),
(2202, 203, '编辑角色', 2, NULL, 'role:edit', NULL, 2, 1, NOW(), NOW()),
(2203, 203, '删除角色', 2, NULL, 'role:delete', NULL, 3, 1, NOW(), NOW()),
(2204, 203, '分配权限', 2, NULL, 'role:assign', NULL, 4, 1, NOW(), NOW()),
-- 权限管理按钮
(2301, 204, '添加权限', 2, NULL, 'permission:add', NULL, 1, 1, NOW(), NOW()),
(2302, 204, '编辑权限', 2, NULL, 'permission:edit', NULL, 2, 1, NOW(), NOW()),
(2303, 204, '删除权限', 2, NULL, 'permission:delete', NULL, 3, 1, NOW(), NOW()),
-- 系统信息按钮
(2401, 201, '保存系统信息', 2, NULL, 'config:save', NULL, 1, 1, NOW(), NOW());

-- ============================================================
-- 8. 初始化角色权限
--    管理员：全部权限
--    普通用户：工作台路由 + 7 个菜单 + 全部业务按钮，不含系统设置
-- ============================================================
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 1, `id` FROM `sys_permission`;

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 2, `id` FROM `sys_permission`
WHERE `id` = 100
   OR (`id` BETWEEN 101 AND 107)
   OR (`id` BETWEEN 1101 AND 1104)
   OR (`id` BETWEEN 1201 AND 1204)
   OR (`id` BETWEEN 1301 AND 1304)
   OR (`id` BETWEEN 1401 AND 1404)
   OR (`id` BETWEEN 1501 AND 1503)
   OR (`id` BETWEEN 1601 AND 1606);

-- ============================================================
-- 9. 迁移现有用户角色
-- ============================================================
INSERT INTO `sys_user_role` (`user_id`, `role_id`)
SELECT `id`, CASE WHEN `role` = 1 THEN 1 ELSE 2 END
FROM `sys_user`;

-- ============================================================
-- 10. 初始化系统配置
-- ============================================================
INSERT INTO `sys_config` (`config_key`, `config_value`, `description`, `update_time`)
VALUES
('system_name', 'Wokit', '系统名称，显示在工作台与系统设置左上角', NOW()),
('system_sub_name', '工作助手', '系统副标题，显示在系统名称下方', NOW()),
('browser_title', 'Wokit 工作助手', '浏览器标题后缀', NOW()),
('login_hint', '使用平台账号进入工作台', '登录页账号表单说明', NOW())
ON DUPLICATE KEY UPDATE `description` = VALUES(`description`);
