-- ============================================================
-- 每日任务新增优先级，并将状态扩展为四态（线上增量升级）
-- 执行前请先备份数据库；本脚本只执行一次，建议在新后端上线前执行。
-- ============================================================

ALTER TABLE `work_task`
    ADD COLUMN `priority` TINYINT NOT NULL DEFAULT 2
        COMMENT '优先级（0：紧急，1：重要，2：普通，3：低优先级）' AFTER `description`,
    MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 0
        COMMENT '任务状态（0：待处理，1：进行中，2：阻塞中，3：已完成）';

-- 旧版本中 status=1 表示已完成，新版本已完成改为 3
UPDATE `work_task` SET `status` = 3 WHERE `status` = 1;
