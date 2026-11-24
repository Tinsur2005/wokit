-- MySQL dump 10.13  Distrib 8.0.36, for Win64 (x86_64)
--
-- Host: localhost    Database: wokit
-- ------------------------------------------------------
-- Server version	8.0.36

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `name` varchar(50) NOT NULL COMMENT '登录账号',
  `password` varchar(60) NOT NULL COMMENT '密码哈希（BCrypt）',
  `real_name` varchar(50) DEFAULT NULL COMMENT '姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像地址',
  `role` tinyint NOT NULL DEFAULT '0' COMMENT '角色（0：普通用户，1：管理员）',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态（0：停用，1：启用）',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0：未删除，1：已删除）',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (1,'admin','$2a$10$KokTJd5qxyOW4E1VFTdcGuhp/yLkZdzJLnJ0nCQRTWihwpKI5LxSC','Tinsur','13800000000','admin@wokit.cn',NULL,1,1,0,'2026-11-24 15:37:33','2026-11-24 16:44:58'),(3,'demo','$2a$10$rme0qeL5GJtllNOX8oNPvOcUyLVtEDIpP4qW14IWc9xN7UBIAcfMq','','','',NULL,0,1,1,'2026-11-24 17:00:09','2026-11-24 17:01:09');
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `work_log`
--

DROP TABLE IF EXISTS `work_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `work_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工时记录ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `work_date` date NOT NULL COMMENT '工作日期',
  `project` varchar(100) NOT NULL COMMENT '项目/事项名称',
  `content` varchar(500) DEFAULT NULL COMMENT '工作内容',
  `hours` decimal(5,1) NOT NULL DEFAULT '0.0' COMMENT '工时（小时）',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0：未删除，1：已删除）',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_log_user_date` (`user_id`,`work_date`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工时登记表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `work_log`
--

LOCK TABLES `work_log` WRITE;
/*!40000 ALTER TABLE `work_log` DISABLE KEYS */;
INSERT INTO `work_log` VALUES (4,1,'2026-11-24','移动端布局自测','验证工时登记页面在 390 宽度下的标题、筛选与卡片列表布局。',2.0,1,'2026-11-24 16:48:52','2026-11-24 16:49:52'),(5,1,'2026-11-24','DataEase 视图插件','对所有插件进行测试和对「散点连线图」插件进行BUG修复',8.0,0,'2026-11-24 16:58:19','2026-11-24 16:58:19');
/*!40000 ALTER TABLE `work_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `work_memo`
--

DROP TABLE IF EXISTS `work_memo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `work_memo` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '备忘录ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `content` varchar(1000) NOT NULL COMMENT '备忘内容',
  `color` varchar(20) NOT NULL DEFAULT 'blue' COMMENT '卡片颜色标记',
  `pinned` tinyint NOT NULL DEFAULT '0' COMMENT '是否置顶（0：否，1：是）',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0：未删除，1：已删除）',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_memo_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='备忘录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `work_memo`
--

LOCK TABLES `work_memo` WRITE;
/*!40000 ALTER TABLE `work_memo` DISABLE KEYS */;
/*!40000 ALTER TABLE `work_memo` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `work_task`
--

DROP TABLE IF EXISTS `work_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `work_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `title` varchar(100) NOT NULL COMMENT '任务标题',
  `description` varchar(500) DEFAULT NULL COMMENT '任务描述',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `deadline` datetime NOT NULL COMMENT '截止时间',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '任务状态（0：待完成，1：已完成）',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0：未删除，1：已删除）',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_user_deadline` (`user_id`,`deadline`),
  KEY `idx_task_user_status` (`user_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='每日任务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `work_task`
--

LOCK TABLES `work_task` WRITE;
/*!40000 ALTER TABLE `work_task` DISABLE KEYS */;
/*!40000 ALTER TABLE `work_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `work_weekly`
--

DROP TABLE IF EXISTS `work_weekly`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `work_weekly` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '周报记录ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `week_start` date NOT NULL COMMENT '周起始日期（周一）',
  `week_end` date NOT NULL COMMENT '周结束日期（周日）',
  `task_content` varchar(200) NOT NULL COMMENT '任务内容',
  `task_result` varchar(500) DEFAULT NULL COMMENT '任务结果',
  `problem` varchar(500) DEFAULT NULL COMMENT '存在的问题',
  `output` varchar(500) DEFAULT NULL COMMENT '产出',
  `extra` varchar(500) DEFAULT NULL COMMENT '其他补充',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0：未删除，1：已删除）',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_weekly_user_week` (`user_id`,`week_start`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='周报记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `work_weekly`
--

LOCK TABLES `work_weekly` WRITE;
/*!40000 ALTER TABLE `work_weekly` DISABLE KEYS */;
INSERT INTO `work_weekly` VALUES (5,1,'2026-11-23','2026-11-29','安装SQLBot，熟悉相关功能','','','','',0,'2026-11-24 16:42:09','2026-11-24 16:42:09'),(6,1,'2026-11-23','2026-11-29','空字段导出校验','','','','',1,'2026-11-24 16:43:35','2026-11-24 16:44:29'),(7,1,'2026-11-23','2026-11-29','DE V2 数据库MySQL迁移TDSQL实施，完成迁移与数据一致性校验，实测在V2.10.26和2.10.28版本下，安装时使用Mysql，安装完成后将数据迁移到TDSQL、修改配置文件并重启后均可正常运行。','','','','',0,'2026-11-24 16:43:50','2026-11-24 16:43:50'),(8,1,'2026-11-23','2026-11-29','参考wiki，修复插件字段配置区域需要等待较长时间才显示的问题','','','','',0,'2026-11-24 16:46:50','2026-11-24 16:46:50'),(9,1,'2026-11-23','2026-11-29','修复在颜色面板拖动选色时，连续触发大量渲染 / 接口请求，短时间高频重复执行渲染逻辑，造成页面卡顿、性能开销高的问题','','','','',0,'2026-11-24 16:46:59','2026-11-24 16:46:59'),(10,1,'2026-11-23','2026-11-29','修复视图插件查看数据弹窗空白、导出 Excel 和原始明细失败的问题','','','输出wiki：https://wiki.fit2cloud.cn/pages/viewpage.action?pageId=249463146','',0,'2026-11-24 16:47:19','2026-11-24 16:47:19');
/*!40000 ALTER TABLE `work_weekly` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-24 17:17:04
