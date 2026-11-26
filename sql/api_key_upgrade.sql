-- ============================================================
-- Wokit 工作助手：API Key / App 登录 / 接口文档 线上升级脚本
-- 适用版本：与当前项目源码一致的线上数据库
-- 特点：
--   1. 只新增表、权限节点和管理员授权，不修改、不删除任何已有业务数据；
--   2. 可重复执行；
--   3. 其他角色不会自动获得 API Key / 接口文档权限，需管理员在 RBAC 中自行分配。
-- ============================================================

SET NAMES utf8mb4;
USE `wokit`;

-- ============================================================
-- 1. API Key 表
-- ============================================================
CREATE TABLE IF NOT EXISTS `wokit_api_key` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `owner_user_id`   BIGINT       NOT NULL COMMENT '密钥所属用户ID',
    `app_name`        VARCHAR(60)  NOT NULL COMMENT '应用名称',
    `platform`        VARCHAR(20)  NOT NULL DEFAULT 'desktop' COMMENT '平台：desktop/mobile/server/other',
    `key_type`        VARCHAR(20)  NOT NULL DEFAULT 'app' COMMENT '类型：app（App登录）/service（服务对接）',
    `access_key`      VARCHAR(80)  NOT NULL COMMENT 'Access Key，公开应用标识',
    `secret_hash`     CHAR(64)     NOT NULL COMMENT 'Secret Key 的 SHA-256 哈希',
    `secret_hint`     VARCHAR(20)  DEFAULT NULL COMMENT 'Secret 掩码，仅用于页面展示',
    `secret_version`  INT          NOT NULL DEFAULT 1 COMMENT 'Secret 版本，重置后递增',
    `scopes`          VARCHAR(300) DEFAULT NULL COMMENT '授权范围，逗号分隔',
    `ip_whitelist`    VARCHAR(300) DEFAULT NULL COMMENT 'IP 白名单，逗号分隔，为空表示不限制',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态（0：停用，1：启用）',
    `expires_at`      DATETIME     DEFAULT NULL COMMENT '过期时间，空表示长期有效',
    `last_used_at`    DATETIME     DEFAULT NULL COMMENT '最后使用时间',
    `last_used_ip`    VARCHAR(64)  DEFAULT NULL COMMENT '最后使用IP',
    `remark`          VARCHAR(200) DEFAULT NULL COMMENT '备注',
    `create_time`     DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_time`     DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_api_key_access_key` (`access_key`),
    KEY `idx_api_key_owner` (`owner_user_id`),
    KEY `idx_api_key_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'API Key 表';

-- ============================================================
-- 2. App 登录 Refresh Token 表
-- ============================================================
CREATE TABLE IF NOT EXISTS `wokit_refresh_token` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
    `api_key_id`    BIGINT       DEFAULT NULL COMMENT '签发时使用的 API Key ID',
    `token_hash`    CHAR(64)     NOT NULL COMMENT 'Refresh Token 的 SHA-256 哈希',
    `device_id`     VARCHAR(100) DEFAULT NULL COMMENT '设备标识',
    `device_name`   VARCHAR(100) DEFAULT NULL COMMENT '设备名称',
    `ip`            VARCHAR(64)  DEFAULT NULL COMMENT '签发IP',
    `expires_at`    DATETIME     NOT NULL COMMENT '过期时间',
    `last_used_at`  DATETIME     DEFAULT NULL COMMENT '最后使用时间',
    `revoked_at`    DATETIME     DEFAULT NULL COMMENT '撤销时间，非空表示已失效',
    `create_time`   DATETIME     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_refresh_token_hash` (`token_hash`),
    KEY `idx_refresh_token_user` (`user_id`),
    KEY `idx_refresh_token_api_key` (`api_key_id`),
    KEY `idx_refresh_token_expires` (`expires_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'App 登录刷新令牌表';

-- ============================================================
-- 3. RBAC：系统设置下的两个新菜单
--    205 API Key，206 接口文档
-- ============================================================
INSERT INTO `sys_permission`
    (`id`, `parent_id`, `name`, `type`, `path`, `permission_value`, `icon`, `sort`, `status`, `create_time`, `update_time`)
VALUES
    (205, 200, 'API Key', 1, '/settings/api-keys', NULL, 'Key', 5, 1, NOW(), NOW()),
    (206, 200, '接口文档', 1, '/settings/api-docs', NULL, 'Document', 6, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    `parent_id` = VALUES(`parent_id`),
    `name` = VALUES(`name`),
    `type` = VALUES(`type`),
    `path` = VALUES(`path`),
    `permission_value` = VALUES(`permission_value`),
    `icon` = VALUES(`icon`),
    `sort` = VALUES(`sort`),
    `status` = VALUES(`status`),
    `update_time` = NOW();

-- ============================================================
-- 4. RBAC：API Key 操作按钮与接口文档导出按钮
-- ============================================================
INSERT INTO `sys_permission`
    (`id`, `parent_id`, `name`, `type`, `path`, `permission_value`, `icon`, `sort`, `status`, `create_time`, `update_time`)
VALUES
    (2501, 205, '新增密钥', 2, NULL, 'api:key:add', NULL, 1, 1, NOW(), NOW()),
    (2502, 205, '启用停用密钥', 2, NULL, 'api:key:toggle', NULL, 2, 1, NOW(), NOW()),
    (2503, 205, '重置Secret', 2, NULL, 'api:key:reset', NULL, 3, 1, NOW(), NOW()),
    (2504, 205, '删除密钥', 2, NULL, 'api:key:delete', NULL, 4, 1, NOW(), NOW()),
    (2601, 206, '导出接口文档', 2, NULL, 'api:doc:export', NULL, 1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    `parent_id` = VALUES(`parent_id`),
    `name` = VALUES(`name`),
    `type` = VALUES(`type`),
    `path` = VALUES(`path`),
    `permission_value` = VALUES(`permission_value`),
    `icon` = VALUES(`icon`),
    `sort` = VALUES(`sort`),
    `status` = VALUES(`status`),
    `update_time` = NOW();

-- ============================================================
-- 5. 只给内置管理员补齐新权限；其他角色保持原样
-- ============================================================
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
JOIN `sys_permission` p ON p.`id` IN (205, 206, 2501, 2502, 2503, 2504, 2601)
WHERE r.`code` = 'admin';

-- ============================================================
-- 6. 调整「权限管理」菜单图标，避免与「API Key」的钥匙图标重复
-- ============================================================
UPDATE `sys_permission`
SET `icon` = 'Lock', `update_time` = NOW()
WHERE `id` = 204 AND `path` = '/settings/permissions';

-- ============================================================
-- 7. 结果核对（执行后可直接查看）
-- ============================================================
SELECT `id`, `parent_id`, `name`, `type`, `path`, `icon`, `status`
FROM `sys_permission`
WHERE `id` IN (205, 206, 2501, 2502, 2503, 2504, 2601)
ORDER BY `id`;

SELECT r.`code` AS role_code, p.`id` AS permission_id, p.`name`
FROM `sys_role_permission` rp
JOIN `sys_role` r ON r.`id` = rp.`role_id`
JOIN `sys_permission` p ON p.`id` = rp.`permission_id`
WHERE p.`id` IN (205, 206, 2501, 2502, 2503, 2504, 2601)
ORDER BY r.`code`, p.`id`;
