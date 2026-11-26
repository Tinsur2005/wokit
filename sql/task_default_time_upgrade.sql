-- ============================================================
-- Wokit 工作助手：新建任务默认时间配置 线上升级脚本
-- 适用版本：已包含系统信息页（/settings/general）的线上数据库
-- 特点：
--   1. 只新增两条 sys_config 默认配置，不修改已有配置值；
--   2. 复用系统信息页现有权限（页面 201、按钮 config:save），不新增权限节点；
--   3. 补齐内置管理员的 config:save 授权，其他角色不自动授权；
--   4. 可重复执行。
-- ============================================================

SET NAMES utf8mb4;
USE `wokit`;

-- ============================================================
-- 1. 新建任务默认时间（只保存时分，日期由任务页使用当天日期）
-- ============================================================
INSERT INTO `sys_config` (`config_key`, `config_value`, `description`, `update_time`)
VALUES
('task_default_start_time', '09:00', '新建任务默认开始时间，仅保存时分，不包含日期', NOW()),
('task_default_end_time', '18:30', '新建任务默认截止时间，仅保存时分，不包含日期', NOW())
ON DUPLICATE KEY UPDATE
    `description` = VALUES(`description`),
    `update_time` = VALUES(`update_time`);

-- ============================================================
-- 2. 权限数据：本功能没有新增页面/按钮，沿用 201 系统信息 + 2401 config:save
--    仅为内置管理员补齐按钮授权，避免旧库缺少该授权
-- ============================================================
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
JOIN `sys_permission` p ON p.`permission_value` = 'config:save'
WHERE r.`code` = 'admin';

-- ============================================================
-- 3. 执行结果核对：应为 2 条配置、1 条管理员授权
-- ============================================================
SELECT `config_key`, `config_value`, `description`
FROM `sys_config`
WHERE `config_key` IN ('task_default_start_time', 'task_default_end_time')
ORDER BY `config_key`;

SELECT r.`code` AS `role_code`, p.`name` AS `permission_name`, p.`permission_value`
FROM `sys_role_permission` rp
JOIN `sys_role` r ON r.`id` = rp.`role_id`
JOIN `sys_permission` p ON p.`id` = rp.`permission_id`
WHERE r.`code` = 'admin' AND p.`permission_value` = 'config:save';
