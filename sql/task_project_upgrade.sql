-- Wokit 项目任务与项目状态增量升级脚本
-- 适用范围：线上当前版本（已包含系统设置、RBAC、API Key、任务默认时间）
-- 本脚本只包含本次新增内容，可重复执行。

-- ---------------------------------------------------------------------------
-- 1. 任务分类表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `work_task_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` VARCHAR(30) NOT NULL COMMENT '分类名称',
  `color` VARCHAR(20) NOT NULL DEFAULT '#1677ff' COMMENT '标签颜色',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_category_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目任务分类';

INSERT IGNORE INTO `work_task_category` (`name`, `color`, `sort`) VALUES
('新需求', '#1677ff', 1),
('BUG 修复', '#f04438', 2),
('性能优化', '#f79009', 3),
('技术改造', '#7a5af8', 4),
('联调测试', '#0e9384', 5),
('文档', '#667085', 6),
('运维支持', '#067647', 7),
('其他', '#98a2b3', 8);

-- ---------------------------------------------------------------------------
-- 2. 任务表新增字段
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS `wokit_add_column`;
DELIMITER $$
CREATE PROCEDURE `wokit_add_column`(
  IN p_table VARCHAR(64),
  IN p_column VARCHAR(64),
  IN p_definition TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = p_table
      AND column_name = p_column
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN ', p_definition);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL `wokit_add_column`('work_task', 'task_kind',
  '`task_kind` TINYINT NOT NULL DEFAULT 0 COMMENT ''任务类型：0普通任务 1项目任务'' AFTER `status`');
CALL `wokit_add_column`('work_task', 'task_category_id',
  '`task_category_id` BIGINT NULL COMMENT ''项目任务分类ID，创建后不可修改'' AFTER `project_id`');
CALL `wokit_add_column`('work_task', 'completed_at',
  '`completed_at` DATETIME NULL COMMENT ''实际完成时间'' AFTER `task_category_id`');

-- ---------------------------------------------------------------------------
-- 3. 项目表新增状态与归档字段
-- ---------------------------------------------------------------------------
CALL `wokit_add_column`('work_project', 'status',
  '`status` TINYINT NOT NULL DEFAULT 1 COMMENT ''0规划中 1进行中 2维护中 3暂停 4已完成 5已取消'' AFTER `name`');
CALL `wokit_add_column`('work_project', 'status_changed_at',
  '`status_changed_at` DATETIME NULL COMMENT ''状态最近变更时间'' AFTER `status`');
CALL `wokit_add_column`('work_project', 'released_at',
  '`released_at` DATETIME NULL COMMENT ''进入维护中/发布时间'' AFTER `status_changed_at`');
CALL `wokit_add_column`('work_project', 'completed_at',
  '`completed_at` DATETIME NULL COMMENT ''完成时间'' AFTER `released_at`');
CALL `wokit_add_column`('work_project', 'archived_at',
  '`archived_at` DATETIME NULL COMMENT ''归档时间；非空即已归档'' AFTER `completed_at`');
CALL `wokit_add_column`('work_project', 'archive_reason',
  '`archive_reason` VARCHAR(200) NULL COMMENT ''归档原因'' AFTER `archived_at`');
CALL `wokit_add_column`('work_project', 'status_before_archive',
  '`status_before_archive` TINYINT NULL COMMENT ''归档前状态，恢复时使用'' AFTER `archive_reason`');

DROP PROCEDURE IF EXISTS `wokit_add_column`;

-- ---------------------------------------------------------------------------
-- 4. 索引
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS `wokit_add_index`;
DELIMITER $$
CREATE PROCEDURE `wokit_add_index`(
  IN p_table VARCHAR(64),
  IN p_index VARCHAR(64),
  IN p_columns VARCHAR(200)
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = p_table
      AND index_name = p_index
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD INDEX `', p_index, '` (', p_columns, ')');
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL `wokit_add_index`('work_task', 'idx_task_project_kind_status',
  '`user_id`, `project_id`, `task_kind`, `status`');
CALL `wokit_add_index`('work_task', 'idx_task_project_category',
  '`project_id`, `task_category_id`');
CALL `wokit_add_index`('work_task', 'idx_task_completed_at',
  '`user_id`, `completed_at`');
CALL `wokit_add_index`('work_project', 'idx_project_user_archived_status',
  '`user_id`, `archived_at`, `status`');

DROP PROCEDURE IF EXISTS `wokit_add_index`;

-- ---------------------------------------------------------------------------
-- 5. 旧数据迁移
-- ---------------------------------------------------------------------------
SET @other_category_id = (
  SELECT `id` FROM `work_task_category` WHERE `name` = '其他' ORDER BY `sort` ASC LIMIT 1
);

UPDATE `work_task`
SET `task_kind` = 1
WHERE `project_id` IS NOT NULL;

UPDATE `work_task`
SET `task_category_id` = @other_category_id
WHERE `project_id` IS NOT NULL
  AND `task_category_id` IS NULL
  AND @other_category_id IS NOT NULL;

UPDATE `work_task`
SET `completed_at` = COALESCE(`completed_at`, `update_time`, `create_time`, NOW())
WHERE `status` = 3
  AND `completed_at` IS NULL;

UPDATE `work_project`
SET `status` = 1
WHERE `status` IS NULL;

-- ---------------------------------------------------------------------------
-- 6. 新权限节点
-- 207 任务分类页面，2701-2703 任务分类按钮，1607-1609 项目归档/恢复/状态修改
-- ---------------------------------------------------------------------------
INSERT INTO `sys_permission`
(`id`, `parent_id`, `name`, `type`, `path`, `permission_value`, `icon`, `sort`, `status`, `create_time`, `update_time`)
VALUES
(207, 200, '任务分类', 1, '/settings/task-categories', NULL, 'CollectionTag', 3, 1, NOW(), NOW()),
(2701, 207, '新增任务分类', 2, NULL, 'task:category:add', NULL, 1, 1, NOW(), NOW()),
(2702, 207, '编辑任务分类', 2, NULL, 'task:category:edit', NULL, 2, 1, NOW(), NOW()),
(2703, 207, '删除任务分类', 2, NULL, 'task:category:delete', NULL, 3, 1, NOW(), NOW()),
(1607, 107, '归档项目', 2, NULL, 'project:archive', NULL, 7, 1, NOW(), NOW()),
(1608, 107, '恢复项目', 2, NULL, 'project:restore', NULL, 8, 1, NOW(), NOW()),
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

-- 管理员角色初始授权
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 1, `id` FROM `sys_permission`
WHERE `id` IN (207, 2701, 2702, 2703, 1607, 1608, 1609);

-- 项目归档/恢复/状态修改属于普通用户原有项目管理能力，默认同步给普通用户角色；
-- 任务分类管理页面与按钮只给管理员，普通角色由 RBAC 按需分配。
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 2, `id` FROM `sys_permission`
WHERE `id` IN (1607, 1608, 1609);

-- 任务分类紧跟在“用户管理”下面，其余系统设置菜单顺延
UPDATE `sys_permission` SET `sort` = 4, `update_time` = NOW() WHERE `id` = 203;
UPDATE `sys_permission` SET `sort` = 5, `update_time` = NOW() WHERE `id` = 204;
UPDATE `sys_permission` SET `sort` = 6, `update_time` = NOW() WHERE `id` = 205;
UPDATE `sys_permission` SET `sort` = 7, `update_time` = NOW() WHERE `id` = 206;

-- ---------------------------------------------------------------------------
-- 7. 结果核对
-- ---------------------------------------------------------------------------
SELECT
  (SELECT COUNT(*) FROM `work_task_category`) AS task_category_count,
  (SELECT COUNT(*) FROM `work_task` WHERE `task_kind` = 1) AS project_task_count,
  (SELECT COUNT(*) FROM `work_task` WHERE `status` = 3 AND `completed_at` IS NOT NULL) AS completed_task_count,
  (SELECT COUNT(*) FROM `sys_permission` WHERE `id` IN (207, 2701, 2702, 2703, 1607, 1608, 1609)) AS new_permission_count,
  (SELECT COUNT(*) FROM `sys_role_permission` WHERE `role_id` = 1 AND `permission_id` IN (207, 2701, 2702, 2703, 1607, 1608, 1609)) AS admin_grant_count;
