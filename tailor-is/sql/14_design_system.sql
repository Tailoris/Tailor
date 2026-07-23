-- ============================================================
-- Tailor IS 平台 - 设计服务数据库表结构
-- 文件: 14_design_system.sql
-- 版本: 1.0.0
-- 数据库: MySQL 8.0
-- 创建时间: 2026-07-20
-- ============================================================

CREATE DATABASE IF NOT EXISTS `tailor_is_design` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `tailor_is_design`;

-- ============================================================
-- 1. 设计作品表 (design_work)
-- ============================================================
CREATE TABLE IF NOT EXISTS `design_work` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `design_name` VARCHAR(128) NOT NULL COMMENT '设计名称',
  `design_code` VARCHAR(64) NOT NULL COMMENT '设计编码',
  `designer_id` BIGINT UNSIGNED NOT NULL COMMENT '设计师ID',
  `category_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '分类ID',
  `garment_type` VARCHAR(32) NOT NULL COMMENT '服装类型',
  `style_tags` JSON DEFAULT NULL COMMENT '风格标签',
  `description` TEXT DEFAULT NULL COMMENT '设计描述',
  `inspiration` TEXT DEFAULT NULL COMMENT '设计灵感',
  `fabric_requirements` TEXT DEFAULT NULL COMMENT '面料需求',
  `file_url` VARCHAR(512) DEFAULT NULL COMMENT '设计文件URL',
  `file_type` VARCHAR(16) DEFAULT NULL COMMENT '文件类型',
  `thumbnail_url` VARCHAR(512) DEFAULT NULL COMMENT '缩略图URL',
  `sketch_urls` JSON DEFAULT NULL COMMENT '草图URL列表',
  `copyright_hash` VARCHAR(128) DEFAULT NULL COMMENT '区块链版权哈希',
  `copyright_status` VARCHAR(32) DEFAULT 'unregistered' COMMENT '版权状态：unregistered/registering/registered',
  `license_type` VARCHAR(32) DEFAULT 'all_rights_reserved' COMMENT '授权类型',
  `like_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '点赞数',
  `view_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-下架，1-上架，2-审核中',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_design_code` (`design_code`),
  KEY `idx_designer_id` (`designer_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_garment_type` (`garment_type`),
  KEY `idx_copyright_status` (`copyright_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='设计作品表';

-- ============================================================
-- 2. 设计分类表 (design_category)
-- ============================================================
CREATE TABLE IF NOT EXISTS `design_category` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` BIGINT UNSIGNED DEFAULT 0 COMMENT '父分类ID',
  `category_name` VARCHAR(64) NOT NULL COMMENT '分类名称',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='设计分类表';

-- ============================================================
-- 3. 设计审核记录表 (design_review)
-- ============================================================
CREATE TABLE IF NOT EXISTS `design_review` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `design_id` BIGINT UNSIGNED NOT NULL COMMENT '设计ID',
  `reviewer_id` BIGINT UNSIGNED NOT NULL COMMENT '审核人ID',
  `review_status` VARCHAR(32) NOT NULL COMMENT '审核结果：approved/rejected/need_modify',
  `review_comment` TEXT DEFAULT NULL COMMENT '审核意见',
  `similarity_score` DECIMAL(5,2) DEFAULT NULL COMMENT 'AI相似度检测分数',
  `similarity_detail` JSON DEFAULT NULL COMMENT '相似度检测详情',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_design_id` (`design_id`),
  KEY `idx_reviewer_id` (`reviewer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='设计审核记录表';

-- ============================================================
-- 初始数据
-- ============================================================
INSERT INTO `design_category` (`parent_id`, `category_name`, `sort_order`) VALUES
(0, '时装设计', 1),
(0, '职业装设计', 2),
(0, '运动装设计', 3),
(0, '民族服饰设计', 4),
(0, '婚纱礼服设计', 5)
ON DUPLICATE KEY UPDATE `category_name` = VALUES(`category_name`);