-- Wokit 工作助手 数据库初始化脚本
-- MySQL 8.0，执行前请确认账号有建库建表权限

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `wokit`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `wokit`;

-- ============================================================
-- 1. 平台用户表
-- ============================================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `name`        VARCHAR(50)  NOT NULL COMMENT '登录账号',
    `password`    VARCHAR(60)  NOT NULL COMMENT '密码哈希（BCrypt）',
    `real_name`   VARCHAR(50)  DEFAULT NULL COMMENT '姓名',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `email`       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
    `role`        TINYINT      NOT NULL DEFAULT 0 COMMENT '角色（0：普通用户，1：管理员）',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态（0：停用，1：启用）',
    `create_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '平台用户表';

-- ============================================================
-- 2. 项目分类表
-- ============================================================
DROP TABLE IF EXISTS `work_project_category`;
CREATE TABLE `work_project_category` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `user_id`     BIGINT      NOT NULL COMMENT '所属用户ID',
    `parent_id`   BIGINT      DEFAULT NULL COMMENT '父分类ID，空表示一级分类',
    `name`        VARCHAR(20) NOT NULL COMMENT '分类名称',
    `create_time` DATETIME    DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME    DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_project_category_user_name` (`user_id`, `name`),
    KEY `idx_project_category_parent` (`user_id`, `parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '项目分类表';

-- ============================================================
-- 3. 项目表
-- ============================================================
DROP TABLE IF EXISTS `work_project`;
CREATE TABLE `work_project` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '项目ID',
    `user_id`     BIGINT      NOT NULL COMMENT '所属用户ID',
    `category_id` BIGINT      NOT NULL COMMENT '分类ID',
    `name`        VARCHAR(60) NOT NULL COMMENT '项目名称',
    `create_time` DATETIME    DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME    DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_project_user_name` (`user_id`, `name`),
    KEY `idx_project_user_category` (`user_id`, `category_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '项目表';

-- ============================================================
-- 4. 每日任务表
-- ============================================================
DROP TABLE IF EXISTS `work_task`;
CREATE TABLE `work_task` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `user_id`     BIGINT       NOT NULL COMMENT '所属用户ID',
    `title`       VARCHAR(100) NOT NULL COMMENT '任务标题',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '任务描述',
    `start_time`  DATETIME     NOT NULL COMMENT '开始时间',
    `deadline`    DATETIME     NOT NULL COMMENT '截止时间',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '任务状态（0：待完成，1：已完成）',
    `project_id`  BIGINT       DEFAULT NULL COMMENT '绑定项目ID',
    `create_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_task_user_deadline` (`user_id`, `deadline`),
    KEY `idx_task_user_status` (`user_id`, `status`),
    KEY `idx_task_user_project` (`user_id`, `project_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '每日任务表';

-- ============================================================
-- 5. 备忘录表（扩展小工具）
-- ============================================================
DROP TABLE IF EXISTS `work_memo`;
CREATE TABLE `work_memo` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '备忘录ID',
    `user_id`     BIGINT        NOT NULL COMMENT '所属用户ID',
    `content`     VARCHAR(1000) NOT NULL COMMENT '备忘内容',
    `color`       VARCHAR(20)   NOT NULL DEFAULT 'blue' COMMENT '卡片颜色标记',
    `pinned`      TINYINT       NOT NULL DEFAULT 0 COMMENT '是否置顶（0：否，1：是）',
    `create_time` DATETIME      DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME      DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_memo_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '备忘录表';

-- ============================================================
-- 6. 工时登记表（扩展小工具）
-- ============================================================
DROP TABLE IF EXISTS `work_log`;
CREATE TABLE `work_log` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '工时记录ID',
    `user_id`     BIGINT        NOT NULL COMMENT '所属用户ID',
    `work_date`   DATE          NOT NULL COMMENT '工作日期',
    `project`     VARCHAR(100)  NOT NULL COMMENT '事项名称',
    `project_id`  BIGINT        DEFAULT NULL COMMENT '绑定项目ID',
    `content`     VARCHAR(500)  DEFAULT NULL COMMENT '工作内容',
    `hours`       DECIMAL(5, 1) NOT NULL DEFAULT 0.0 COMMENT '工时（小时）',
    `create_time` DATETIME      DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME      DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_log_user_date` (`user_id`, `work_date`),
    KEY `idx_log_user_project` (`user_id`, `project_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工时登记表';

-- ============================================================
-- 7. 周报记录表（扩展小工具）
-- ============================================================
DROP TABLE IF EXISTS `work_weekly`;
CREATE TABLE `work_weekly` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '周报记录ID',
    `user_id`      BIGINT       NOT NULL COMMENT '所属用户ID',
    `week_start`   DATE         NOT NULL COMMENT '周起始日期（周一）',
    `week_end`     DATE         NOT NULL COMMENT '周结束日期（周日）',
    `project_id`   BIGINT       DEFAULT NULL COMMENT '绑定项目ID',
    `task_content` VARCHAR(200) NOT NULL COMMENT '任务内容',
    `task_result`  VARCHAR(500) DEFAULT NULL COMMENT '任务结果',
    `resolved_problem` VARCHAR(500) DEFAULT NULL COMMENT '已解决问题及解决方式',
    `problem`      VARCHAR(500) DEFAULT NULL COMMENT '仍存在的问题',
    `output`       VARCHAR(500) DEFAULT NULL COMMENT '产出',
    `extra`        VARCHAR(500) DEFAULT NULL COMMENT '其他补充',
    `create_time`  DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_time`  DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_weekly_user_week` (`user_id`, `week_start`),
    KEY `idx_weekly_user_project` (`user_id`, `project_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '周报记录表';

-- ============================================================
-- 8. 计时表（扩展小工具）
-- ============================================================
DROP TABLE IF EXISTS `work_timer`;
CREATE TABLE `work_timer` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '计时ID',
    `user_id`     BIGINT      NOT NULL COMMENT '所属用户ID',
    `name`        VARCHAR(30) NOT NULL COMMENT '计时名称',
    `mode`        VARCHAR(20) NOT NULL COMMENT '模式（countdown_day：倒数日，countup_day：正数日，precise：精准倒计时）',
    `target_time` DATETIME    NOT NULL COMMENT '目标时间（日期模式为当天零点，精准模式精确到分钟）',
    `create_time` DATETIME    DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME    DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_timer_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '计时表';

-- ============================================================
-- 9. 初始化账号
--    管理员 admin / 123456，普通用户 demo / 123456
--    密码均为 BCrypt 哈希，不可直接改成明文
-- ============================================================
INSERT INTO `sys_user` (`id`, `name`, `password`, `real_name`, `phone`, `email`, `role`, `status`, `create_time`, `update_time`)
VALUES
(1, 'admin', '$2a$10$KokTJd5qxyOW4E1VFTdcGuhp/yLkZdzJLnJ0nCQRTWihwpKI5LxSC', '杨庆硕', '13800000000', 'admin@wokit.cn', 1, 1, NOW(), NOW()),
(2, 'demo',  '$2a$10$KokTJd5qxyOW4E1VFTdcGuhp/yLkZdzJLnJ0nCQRTWihwpKI5LxSC', '演示用户', '13900000000', 'demo@wokit.cn', 0, 1, NOW(), NOW());

-- ============================================================
-- 10. 任务演示数据（以执行脚本当天为基准，避免日期过期）
-- ============================================================
INSERT INTO `work_task` (`user_id`, `title`, `description`, `start_time`, `deadline`, `status`, `create_time`, `update_time`)
VALUES
(1, 'Wokit 每日任务模块联调', '走通日历标记、当日任务列表、新建与状态切换的完整链路。',
 TIMESTAMP(CURDATE(), '09:30:00'), TIMESTAMP(CURDATE(), '11:00:00'), 0, NOW(), NOW()),
(1, '用户管理权限校验', '确认普通用户无法进入用户管理，停用账号无法登录。',
 TIMESTAMP(CURDATE(), '14:00:00'), TIMESTAMP(CURDATE(), '16:30:00'), 0, NOW(), NOW()),
(1, '整理后端接口注释', '补齐任务、备忘录、工时登记模块的接口说明。',
 TIMESTAMP(CURDATE(), '19:30:00'), TIMESTAMP(CURDATE(), '21:00:00'), 1, NOW(), NOW()),
(1, '输出本周工作周报', '汇总本周任务完成情况与工时分布。',
 TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '10:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '12:00:00'), 0, NOW(), NOW()),
(1, '登录页视觉走查', '核对输入框、按钮、错误提示在 1440 与 390 宽度下的表现。',
 TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '15:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '17:00:00'), 1, NOW(), NOW()),
(1, '梳理一期功能清单', '确认一期只做登录、用户管理、每日任务，其余工具预留入口。',
 TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '09:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '10:00:00'), 1, NOW(), NOW()),
(1, '移动端自适应验收', '检查侧边栏收起、底部导航、日历与任务列表的纵向排列。',
 TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 3 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 3 DAY), '18:00:00'), 0, NOW(), NOW()),
(1, '12 月迭代规划', '排定备忘录与工时登记的二期需求。',
 TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 7 DAY), '14:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 7 DAY), '16:00:00'), 0, NOW(), NOW());

-- ============================================================
-- 11. 备忘录演示数据
-- ============================================================
INSERT INTO `work_memo` (`user_id`, `content`, `color`, `pinned`, `create_time`, `update_time`)
VALUES
(1, '登录页先做账号密码登录，验证码与第三方登录放到二期。', 'blue', 1, NOW(), NOW()),
(1, '日历上任务的标记只区分「有未完成任务」和「全部完成」两种状态。', 'amber', 0, NOW(), NOW()),
(1, '每周五 17:00 前提交工时登记，避免月底补录。', 'green', 0, NOW(), NOW());

-- ============================================================
-- 12. 工时登记演示数据
-- ============================================================
INSERT INTO `work_log` (`user_id`, `work_date`, `project`, `content`, `hours`, `create_time`, `update_time`)
VALUES
(1, CURDATE(), 'Wokit 工作助手', '每日任务模块接口开发与联调', 6.5, NOW(), NOW()),
(1, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'Wokit 工作助手', '登录鉴权、用户管理页面开发', 7.0, NOW(), NOW()),
(1, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 'Wokit 工作助手', '数据库设计与项目骨架搭建', 5.5, NOW(), NOW());

-- ============================================================
-- 13. 周报演示数据（本周一为基准，随执行日期自动调整）
-- ============================================================
INSERT INTO `work_weekly` (`user_id`, `week_start`, `week_end`, `task_content`, `task_result`, `problem`, `output`, `extra`, `create_time`, `update_time`)
VALUES
(1, DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 6 DAY),
 '完成每日任务模块前后端联调', '日历打点、当日任务列表、新建与状态切换全部跑通。', '跨月切换时任务标记刷新略慢，需确认是否数据量导致。', '任务页面、8 个任务接口、演示数据。', '下周补充批量导入任务的入口。', NOW(), NOW()),
(1, DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 6 DAY),
 '完成登录鉴权与用户管理', '管理员可新增、编辑、启停账号，普通账号无法进入用户管理。', '被停用账号的登录提示文案还需再确认。', '登录页、用户管理页、JWT 拦截器与权限校验。', 'demo 账号保留用于权限验证。', NOW(), NOW()),
(1, DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 6 DAY),
 '完成移动端自适应与视觉统一', '390 宽度下侧栏收起、底部导航与表单弹窗均可正常使用。', '窄屏下宽表格需要横向滚动，阅读体验一般。', '7 张桌面与移动端页面截图，统一绿色主题样式。', '主色统一为 #12B76A，后续新增页面沿用变量。', NOW(), NOW());
