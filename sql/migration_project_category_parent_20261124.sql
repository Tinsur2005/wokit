ALTER TABLE `work_project_category`
    ADD COLUMN `parent_id` BIGINT DEFAULT NULL COMMENT '父分类ID，空表示一级分类' AFTER `user_id`,
    DROP INDEX `uk_project_category_user_name`,
    ADD KEY `idx_project_category_user_name` (`user_id`, `name`),
    ADD KEY `idx_project_category_parent` (`user_id`, `parent_id`);
