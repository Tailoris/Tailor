-- ============================================================
-- Tailor IS - Flyway Migration V1: 版型系统数据库初始化
-- 数据库: tailor_is_pattern
-- 创建时间: 2026-07-10
-- 说明: 基线迁移，创建版型系统所有表结构
-- ============================================================

CREATE TABLE IF NOT EXISTS `pattern` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '版型ID（主键）',
  `name` VARCHAR(100) NOT NULL COMMENT '版型名称',
  `description` TEXT COMMENT '版型描述',
  `category` VARCHAR(50) COMMENT '版型分类',
  `image_url` VARCHAR(255) COMMENT '版型图片',
  `dimensions` JSON COMMENT '规格尺寸',
  `status` TINYINT DEFAULT 1 COMMENT '状态0禁用1启用',
  `merchant_id` BIGINT UNSIGNED COMMENT '商户ID',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='版型表';