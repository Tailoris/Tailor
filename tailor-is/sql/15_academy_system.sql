-- ============================================================
-- Tailor IS 平台 - 学院服务数据库表结构
-- 文件: 15_academy_system.sql
-- 版本: 1.0.0
-- 数据库: MySQL 8.0
-- 创建时间: 2026-07-20
-- ============================================================

CREATE DATABASE IF NOT EXISTS `tailor_is_academy` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `tailor_is_academy`;

-- ============================================================
-- 1. 课程表 (course)
-- ============================================================
CREATE TABLE IF NOT EXISTS `course` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_name` VARCHAR(128) NOT NULL COMMENT '课程名称',
  `course_code` VARCHAR(64) NOT NULL COMMENT '课程编码',
  `category_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '分类ID',
  `instructor_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '讲师ID',
  `description` TEXT DEFAULT NULL COMMENT '课程简介',
  `cover_url` VARCHAR(512) DEFAULT NULL COMMENT '封面图URL',
  `level` VARCHAR(16) NOT NULL DEFAULT 'beginner' COMMENT '难度等级：beginner/intermediate/advanced',
  `duration` INT UNSIGNED DEFAULT 0 COMMENT '总时长(分钟)',
  `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '课程价格',
  `is_free` TINYINT NOT NULL DEFAULT 0 COMMENT '是否免费：0-否，1-是',
  `enroll_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '报名人数',
  `rating` DECIMAL(2,1) DEFAULT 5.0 COMMENT '评分(1-5)',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-下架，1-上架，2-审核中',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_code` (`course_code`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_instructor_id` (`instructor_id`),
  KEY `idx_level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='课程表';

-- ============================================================
-- 2. 章节表 (chapter)
-- ============================================================
CREATE TABLE IF NOT EXISTS `chapter` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id` BIGINT UNSIGNED NOT NULL COMMENT '课程ID',
  `chapter_name` VARCHAR(128) NOT NULL COMMENT '章节名称',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `video_url` VARCHAR(512) DEFAULT NULL COMMENT '视频URL',
  `video_duration` INT UNSIGNED DEFAULT 0 COMMENT '视频时长(秒)',
  `content` TEXT DEFAULT NULL COMMENT '章节内容',
  `attachment_url` VARCHAR(512) DEFAULT NULL COMMENT '附件URL',
  `is_free_preview` TINYINT NOT NULL DEFAULT 0 COMMENT '是否免费试看：0-否，1-是',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='章节表';

-- ============================================================
-- 3. 学习进度表 (learning_progress)
-- ============================================================
CREATE TABLE IF NOT EXISTS `learning_progress` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `course_id` BIGINT UNSIGNED NOT NULL COMMENT '课程ID',
  `chapter_id` BIGINT UNSIGNED NOT NULL COMMENT '章节ID',
  `progress` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '学习进度百分比',
  `watch_duration` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '观看时长(秒)',
  `is_completed` TINYINT NOT NULL DEFAULT 0 COMMENT '是否完成：0-否，1-是',
  `last_watch_time` DATETIME DEFAULT NULL COMMENT '最后观看时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_chapter` (`user_id`, `chapter_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学习进度表';

-- ============================================================
-- 4. 课程分类表 (course_category)
-- ============================================================
CREATE TABLE IF NOT EXISTS `course_category` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` BIGINT UNSIGNED DEFAULT 0 COMMENT '父分类ID',
  `category_name` VARCHAR(64) NOT NULL COMMENT '分类名称',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='课程分类表';

-- ============================================================
-- 初始数据
-- ============================================================
INSERT INTO `course_category` (`parent_id`, `category_name`, `sort_order`) VALUES
(0, '服装设计基础', 1),
(0, '版型制作', 2),
(0, '面料知识', 3),
(0, '服装工艺', 4),
(0, '品牌运营', 5)
ON DUPLICATE KEY UPDATE `category_name` = VALUES(`category_name`);