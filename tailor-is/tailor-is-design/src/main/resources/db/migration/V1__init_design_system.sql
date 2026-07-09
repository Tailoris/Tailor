-- ============================================================
-- Tailor IS - Flyway Migration V1: 服装设计系统数据库初始化
-- 数据库: tailor_is_design
-- 创建时间: 2026-07-10
-- 说明: 基线迁移，创建服装设计系统所有表结构
-- ============================================================

CREATE TABLE IF NOT EXISTS `design` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '设计ID（主键）',
  `design_no` VARCHAR(64) NOT NULL COMMENT '设计编号',
  `name` VARCHAR(100) NOT NULL COMMENT '设计名称',
  `description` TEXT COMMENT '设计描述',
  `cover_image` VARCHAR(512) COMMENT '封面图片',
  `design_type` VARCHAR(50) COMMENT '设计类型：服装/配饰/鞋包等',
  `style_category` VARCHAR(100) COMMENT '风格分类：简约/复古/时尚/休闲等',
  `color` VARCHAR(100) COMMENT '颜色',
  `material` VARCHAR(100) COMMENT '面料材质',
  `size_range` VARCHAR(100) COMMENT '尺码范围',
  `design_data` LONGTEXT COMMENT '设计数据（JSON/DXF格式）',
  `difficulty` VARCHAR(32) COMMENT '难度等级：beginner/intermediate/advanced',
  `work_time` INT COMMENT '制作工时（小时）',
  `price` DECIMAL(10, 2) DEFAULT 0 COMMENT '价格',
  `is_free` TINYINT DEFAULT 0 COMMENT '是否免费：0-收费，1-免费',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-下架，1-上架',
  `like_count` BIGINT DEFAULT 0 COMMENT '点赞数',
  `view_count` BIGINT DEFAULT 0 COMMENT '浏览量',
  `user_id` BIGINT UNSIGNED COMMENT '设计师用户ID',
  `merchant_id` BIGINT UNSIGNED COMMENT '商户ID',
  `pattern_id` BIGINT UNSIGNED COMMENT '关联版型ID',
  `copyright_status` TINYINT DEFAULT 0 COMMENT '版权状态：0-未存证，1-已存证',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_design_no` (`design_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_design_type` (`design_type`),
  KEY `idx_style_category` (`style_category`),
  KEY `idx_status` (`status`),
  KEY `idx_is_free` (`is_free`),
  KEY `idx_like_count` (`like_count`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='设计表';

CREATE TABLE IF NOT EXISTS `design_comment` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评论ID（主键）',
  `design_id` BIGINT UNSIGNED NOT NULL COMMENT '设计ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `content` VARCHAR(500) NOT NULL COMMENT '评论内容',
  `parent_id` BIGINT UNSIGNED DEFAULT 0 COMMENT '父评论ID（0为一级评论）',
  `like_count` INT DEFAULT 0 COMMENT '点赞数',
  `is_top` TINYINT DEFAULT 0 COMMENT '是否置顶：0-否，1-是',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_design_id` (`design_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='设计评论表';

CREATE TABLE IF NOT EXISTS `design_like` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '点赞记录ID（主键）',
  `design_id` BIGINT UNSIGNED NOT NULL COMMENT '设计ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_design_user` (`design_id`, `user_id`),
  KEY `idx_design_id` (`design_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='设计点赞表';

CREATE TABLE IF NOT EXISTS `design_collection` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '收藏记录ID（主键）',
  `design_id` BIGINT UNSIGNED NOT NULL COMMENT '设计ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_design_user` (`design_id`, `user_id`),
  KEY `idx_design_id` (`design_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='设计收藏表';