-- 周报记录新增任务标题，并将任务内容长度扩展到 500（与每日任务描述上限一致）
-- 适用于已上线的 wokit 数据库，执行一次即可

ALTER TABLE `work_weekly`
    ADD COLUMN `task_title` VARCHAR(100) DEFAULT NULL COMMENT '任务标题' AFTER `project_id`;

ALTER TABLE `work_weekly`
    MODIFY COLUMN `task_content` VARCHAR(500) NOT NULL COMMENT '任务内容';
