-- ============================================================
-- Tailor IS - Flyway Migration V1: 知识库系统数据库初始化
-- 数据库: tailor_is_knowledge
-- 创建时间: 2026-07-10
-- 说明: 基线迁移，创建知识库系统所有表结构
-- ============================================================

CREATE TABLE IF NOT EXISTS `knowledge_category` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分类ID（主键）',
  `name` VARCHAR(100) NOT NULL COMMENT '分类名称',
  `parent_id` BIGINT UNSIGNED DEFAULT 0 COMMENT '父级分类ID（0为一级分类）',
  `description` VARCHAR(500) COMMENT '分类描述',
  `icon` VARCHAR(255) COMMENT '分类图标',
  `sort_order` INT DEFAULT 0 COMMENT '排序序号',
  `article_count` INT DEFAULT 0 COMMENT '文章数量',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库分类表';

CREATE TABLE IF NOT EXISTS `knowledge_tag` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '标签ID（主键）',
  `name` VARCHAR(100) NOT NULL COMMENT '标签名称',
  `description` VARCHAR(500) COMMENT '标签描述',
  `article_count` INT DEFAULT 0 COMMENT '关联文章数量',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库标签表';

CREATE TABLE IF NOT EXISTS `knowledge_article` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '文章ID（主键）',
  `article_no` VARCHAR(64) NOT NULL COMMENT '文章编号',
  `title` VARCHAR(200) NOT NULL COMMENT '文章标题',
  `summary` VARCHAR(500) COMMENT '文章摘要',
  `content` LONGTEXT COMMENT '文章内容',
  `cover_image` VARCHAR(512) COMMENT '封面图片',
  `category_id` BIGINT UNSIGNED COMMENT '分类ID',
  `category_name` VARCHAR(100) COMMENT '分类名称（冗余）',
  `tag_ids` VARCHAR(500) COMMENT '标签ID列表（逗号分隔）',
  `view_count` BIGINT DEFAULT 0 COMMENT '浏览量',
  `like_count` BIGINT DEFAULT 0 COMMENT '点赞数',
  `comment_count` BIGINT DEFAULT 0 COMMENT '评论数',
  `author_id` BIGINT UNSIGNED COMMENT '作者用户ID',
  `author_name` VARCHAR(64) COMMENT '作者名称（冗余）',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-草稿，1-发布，2-下架',
  `is_top` TINYINT DEFAULT 0 COMMENT '是否置顶：0-否，1-是',
  `is_recommend` TINYINT DEFAULT 0 COMMENT '是否推荐：0-否，1-是',
  `source_type` VARCHAR(32) COMMENT '来源类型：original/republish/translation',
  `source_url` VARCHAR(512) COMMENT '来源链接',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_no` (`article_no`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_author_id` (`author_id`),
  KEY `idx_status` (`status`),
  KEY `idx_is_top` (`is_top`),
  KEY `idx_is_recommend` (`is_recommend`),
  KEY `idx_view_count` (`view_count`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库文章表';

CREATE TABLE IF NOT EXISTS `knowledge_comment` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评论ID（主键）',
  `article_id` BIGINT UNSIGNED NOT NULL COMMENT '文章ID',
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
  KEY `idx_article_id` (`article_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库评论表';

INSERT INTO `knowledge_category` (`name`, `parent_id`, `description`, `sort_order`, `status`) VALUES
('打版教程', 0, '服装打版基础与进阶教程', 1, 1),
('面料知识', 0, '面料材质、特性与选用指南', 2, 1),
('版型设计', 0, '服装版型设计原理与技巧', 3, 1),
('工艺技术', 0, '服装制作工艺与技术规范', 4, 1),
('行业资讯', 0, '服装行业最新动态与趋势', 5, 1),
('设计灵感', 0, '服装设计灵感与创意分享', 6, 1),
('入门基础', 1, '打版入门基础知识', 1, 1),
('高级打版', 1, '高级打版技巧与案例', 2, 1),
('针织面料', 2, '针织面料特性与应用', 1, 1),
('梭织面料', 2, '梭织面料特性与应用', 2, 1);

INSERT INTO `knowledge_tag` (`name`, `description`, `status`) VALUES
('新手入门', '适合新手学习的内容', 1),
('高级技巧', '高级进阶技巧', 1),
('版型', '版型相关', 1),
('面料', '面料相关', 1),
('工艺', '工艺相关', 1),
('设计', '设计相关', 1),
('教程', '教程类内容', 1),
('案例', '实际案例分享', 1);