-- ============================================================
-- Tailor IS 平台 - 知识库服务数据库表结构
-- 文件: 16_knowledge_system.sql
-- 版本: 1.0.0
-- 数据库: MySQL 8.0
-- 创建时间: 2026-07-20
-- ============================================================

CREATE DATABASE IF NOT EXISTS `tailor_is_knowledge` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `tailor_is_knowledge`;

-- ============================================================
-- 1. 知识条目表 (knowledge_article)
-- ============================================================
CREATE TABLE IF NOT EXISTS `knowledge_article` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` VARCHAR(256) NOT NULL COMMENT '标题',
  `category_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '分类ID',
  `author_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '作者ID',
  `summary` VARCHAR(512) DEFAULT NULL COMMENT '摘要',
  `content` LONGTEXT DEFAULT NULL COMMENT '内容',
  `content_format` VARCHAR(16) NOT NULL DEFAULT 'markdown' COMMENT '内容格式：markdown/richtext/html',
  `cover_url` VARCHAR(512) DEFAULT NULL COMMENT '封面图URL',
  `tags` JSON DEFAULT NULL COMMENT '标签',
  `view_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `like_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '点赞数',
  `collect_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '收藏数',
  `is_featured` TINYINT NOT NULL DEFAULT 0 COMMENT '是否精选：0-否，1-是',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-下架，1-上架，2-审核中',
  `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_author_id` (`author_id`),
  KEY `idx_status` (`status`),
  KEY `idx_publish_time` (`publish_time`),
  FULLTEXT KEY `ft_title_content` (`title`, `content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识条目表';

-- ============================================================
-- 2. 知识分类表 (knowledge_category)
-- ============================================================
CREATE TABLE IF NOT EXISTS `knowledge_category` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` BIGINT UNSIGNED DEFAULT 0 COMMENT '父分类ID',
  `category_name` VARCHAR(64) NOT NULL COMMENT '分类名称',
  `icon` VARCHAR(128) DEFAULT NULL COMMENT '图标',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识分类表';

-- ============================================================
-- 3. 知识收藏表 (knowledge_favorite)
-- ============================================================
CREATE TABLE IF NOT EXISTS `knowledge_favorite` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `article_id` BIGINT UNSIGNED NOT NULL COMMENT '文章ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_article` (`user_id`, `article_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_article_id` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识收藏表';

-- ============================================================
-- 初始数据
-- ============================================================
INSERT INTO `knowledge_category` (`parent_id`, `category_name`, `sort_order`) VALUES
(0, '版型知识', 1),
(0, '面料百科', 2),
(0, '工艺技巧', 3),
(0, '行业标准', 4),
(0, '设计趋势', 5)
ON DUPLICATE KEY UPDATE `category_name` = VALUES(`category_name`);