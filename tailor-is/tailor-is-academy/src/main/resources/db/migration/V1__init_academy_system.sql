-- ============================================================
-- Tailor IS - Flyway Migration V1: 线上学堂系统数据库初始化
-- 数据库: tailor_is_academy
-- 创建时间: 2026-07-10
-- 说明: 基线迁移，创建学堂系统所有表结构
-- ============================================================

CREATE TABLE IF NOT EXISTS `course_category` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分类ID（主键）',
  `name` VARCHAR(100) NOT NULL COMMENT '分类名称',
  `parent_id` BIGINT UNSIGNED DEFAULT 0 COMMENT '父级分类ID',
  `description` VARCHAR(500) COMMENT '分类描述',
  `sort_order` INT DEFAULT 0 COMMENT '排序序号',
  `icon` VARCHAR(255) COMMENT '分类图标',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_sort_order` (`sort_order`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='课程分类表';

CREATE TABLE IF NOT EXISTS `course` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '课程ID（主键）',
  `title` VARCHAR(200) NOT NULL COMMENT '课程标题',
  `description` TEXT COMMENT '课程描述',
  `cover_image` VARCHAR(255) COMMENT '封面图片地址',
  `video_url` VARCHAR(255) COMMENT '视频地址',
  `category_id` BIGINT UNSIGNED COMMENT '分类ID',
  `duration` INT COMMENT '课程时长（分钟）',
  `view_count` BIGINT DEFAULT 0 COMMENT '浏览量',
  `status` TINYINT DEFAULT 1 COMMENT '状态0下架1上架',
  `author_id` BIGINT UNSIGNED COMMENT '作者/讲师用户ID',
  `price` DECIMAL(10, 2) DEFAULT 0 COMMENT '课程价格',
  `is_free` TINYINT DEFAULT 0 COMMENT '是否免费：0-收费，1-免费',
  `level` VARCHAR(32) COMMENT '难度等级：beginner/intermediate/advanced',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_author_id` (`author_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_is_free` (`is_free`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='课程表';

CREATE TABLE IF NOT EXISTS `course_chapter` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '章节ID（主键）',
  `course_id` BIGINT UNSIGNED NOT NULL COMMENT '所属课程ID',
  `title` VARCHAR(200) NOT NULL COMMENT '章节标题',
  `video_url` VARCHAR(255) COMMENT '章节视频地址',
  `duration` INT COMMENT '章节时长（分钟）',
  `sort_order` INT DEFAULT 0 COMMENT '排序序号',
  `free_access` TINYINT DEFAULT 0 COMMENT '是否免费访问：0-否，1-是',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='课程章节表';

CREATE TABLE IF NOT EXISTS `user_course_progress` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `course_id` BIGINT UNSIGNED NOT NULL COMMENT '课程ID',
  `chapter_id` BIGINT UNSIGNED COMMENT '当前章节ID',
  `progress` DECIMAL(5, 2) DEFAULT 0 COMMENT '学习进度百分比',
  `is_completed` TINYINT DEFAULT 0 COMMENT '是否已完成：0-未完成，1-已完成',
  `last_watch_time` DATETIME COMMENT '最后观看时间',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_course` (`user_id`, `course_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_is_completed` (`is_completed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户课程学习进度表';

INSERT INTO `course_category` (`name`, `parent_id`, `description`, `sort_order`, `status`) VALUES
('打版基础', 0, '服装打版入门知识', 1, 1),
('版型设计', 0, '高级版型设计技巧', 2, 1),
('面料工艺', 0, '面料选择与工艺处理', 3, 1),
('款式设计', 0, '服装款式设计原理', 4, 1),
('CAD应用', 0, '服装CAD软件操作', 5, 1);