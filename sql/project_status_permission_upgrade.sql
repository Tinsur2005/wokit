-- Wokit 项目状态修改按钮权限
-- 新增：1609 修改项目状态 project:status
-- 授予已拥有“编辑项目”权限的角色，管理员兜底授权
-- 可重复执行

INSERT INTO `sys_permission`
(`id`, `parent_id`, `name`, `type`, `path`, `permission_value`, `icon`, `sort`, `status`, `create_time`, `update_time`)
VALUES
(1609, 107, '修改项目状态', 2, NULL, 'project:status', NULL, 9, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`),
  `name` = VALUES(`name`),
  `type` = VALUES(`type`),
  `path` = VALUES(`path`),
  `permission_value` = VALUES(`permission_value`),
  `icon` = VALUES(`icon`),
  `sort` = VALUES(`sort`),
  `status` = 1,
  `update_time` = NOW();

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT `role_id`, 1609
FROM `sys_role_permission`
WHERE `permission_id` = 1602
UNION
SELECT `id`, 1609
FROM `sys_role`
WHERE `id` = 1;

SELECT
  (SELECT COUNT(*) FROM `sys_permission` WHERE `id` = 1609 AND `permission_value` = 'project:status') AS permission_count,
  (SELECT COUNT(*) FROM `sys_role_permission` WHERE `permission_id` = 1609) AS grant_role_count;
