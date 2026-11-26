-- Wokit 任务分类菜单位置调整
-- 将“任务分类”移动到“用户管理”下面，保持其余系统设置菜单顺序不变
-- 可重复执行

UPDATE `sys_permission` SET `sort` = 3, `update_time` = NOW() WHERE `id` = 207;
UPDATE `sys_permission` SET `sort` = 4, `update_time` = NOW() WHERE `id` = 203;
UPDATE `sys_permission` SET `sort` = 5, `update_time` = NOW() WHERE `id` = 204;
UPDATE `sys_permission` SET `sort` = 6, `update_time` = NOW() WHERE `id` = 205;
UPDATE `sys_permission` SET `sort` = 7, `update_time` = NOW() WHERE `id` = 206;

SELECT `id`, `parent_id`, `name`, `sort`
FROM `sys_permission`
WHERE `parent_id` = 200
ORDER BY `sort`, `id`;
