-- ============================================================
-- Tailor IS 平台 - 纸样服务数据库表结构
-- 文件: 13_pattern_system.sql
-- 版本: 1.0.0
-- 数据库: MySQL 8.0
-- 创建时间: 2026-07-20
-- ============================================================

CREATE DATABASE IF NOT EXISTS `tailor_is_pattern` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `tailor_is_pattern`;

-- ============================================================
-- 1. 纸样库表 (pattern_library)
-- ============================================================
CREATE TABLE IF NOT EXISTS `pattern_library` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `pattern_name` VARCHAR(128) NOT NULL COMMENT '纸样名称',
  `pattern_code` VARCHAR(64) NOT NULL COMMENT '纸样编码',
  `category_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '分类ID',
  `garment_type` VARCHAR(32) NOT NULL COMMENT '服装类型：top/bottom/dress/outerwear/suit/other',
  `gender` TINYINT DEFAULT 0 COMMENT '适用性别：0-通用，1-男，2-女',
  `size_range` VARCHAR(128) DEFAULT NULL COMMENT '尺码范围',
  `body_type` VARCHAR(64) DEFAULT NULL COMMENT '体型：standard/plus/petite/tall',
  `description` TEXT DEFAULT NULL COMMENT '纸样描述',
  `file_url` VARCHAR(512) DEFAULT NULL COMMENT '纸样文件URL',
  `file_type` VARCHAR(16) DEFAULT NULL COMMENT '文件类型：pdf/dxf/ai/psd',
  `file_size` BIGINT UNSIGNED DEFAULT NULL COMMENT '文件大小(字节)',
  `thumbnail_url` VARCHAR(512) DEFAULT NULL COMMENT '缩略图URL',
  `preview_3d_url` VARCHAR(512) DEFAULT NULL COMMENT '3D预览URL',
  `version` VARCHAR(32) NOT NULL DEFAULT '1.0' COMMENT '版本号',
  `accuracy_score` DECIMAL(3,1) DEFAULT NULL COMMENT '版型精准度评分(1-5)',
  `craft_score` DECIMAL(3,1) DEFAULT NULL COMMENT '工艺清晰度评分(1-5)',
  `adaptability_score` DECIMAL(3,1) DEFAULT NULL COMMENT '适配性评分(1-5)',
  `download_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '下载次数',
  `view_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-下架，1-上架，2-审核中',
  `creator_id` BIGINT UNSIGNED NOT NULL COMMENT '创建者ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pattern_code` (`pattern_code`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_garment_type` (`garment_type`),
  KEY `idx_creator_id` (`creator_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='纸样库表';

-- ============================================================
-- 2. 纸样分类表 (pattern_category)
-- ============================================================
CREATE TABLE IF NOT EXISTS `pattern_category` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` BIGINT UNSIGNED DEFAULT 0 COMMENT '父分类ID',
  `category_name` VARCHAR(64) NOT NULL COMMENT '分类名称',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='纸样分类表';

-- ============================================================
-- 3. 纸样需求表 (pattern_requirement)
-- ============================================================
CREATE TABLE IF NOT EXISTS `pattern_requirement` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `requirement_no` VARCHAR(64) NOT NULL COMMENT '需求编号',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '需求用户ID',
  `pattern_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联纸样ID',
  `garment_type` VARCHAR(32) NOT NULL COMMENT '服装类型',
  `body_measurements` JSON DEFAULT NULL COMMENT '人体尺寸数据',
  `special_requirements` TEXT DEFAULT NULL COMMENT '特殊要求',
  `design_reference_url` VARCHAR(512) DEFAULT NULL COMMENT '设计参考URL',
  `budget_range` VARCHAR(64) DEFAULT NULL COMMENT '预算范围',
  `is_urgent` TINYINT NOT NULL DEFAULT 0 COMMENT '是否加急：0-否，1-是',
  `status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/reviewing/drafting/modifying/delivered/completed',
  `progress` TINYINT NOT NULL DEFAULT 0 COMMENT '进度百分比(0-100)',
  `assignee_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '指派版师ID',
  `expected_delivery_date` DATE DEFAULT NULL COMMENT '预计交付日期',
  `actual_delivery_date` DATE DEFAULT NULL COMMENT '实际交付日期',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_requirement_no` (`requirement_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_pattern_id` (`pattern_id`),
  KEY `idx_status` (`status`),
  KEY `idx_assignee_id` (`assignee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='纸样需求表';

-- ============================================================
-- 初始数据
-- ============================================================
INSERT INTO `pattern_category` (`parent_id`, `category_name`, `sort_order`) VALUES
(0, '女装', 1),
(0, '男装', 2),
(0, '童装', 3),
(0, '大码定制', 4),
(0, '娇小体型', 5)
ON DUPLICATE KEY UPDATE `category_name` = VALUES(`category_name`);