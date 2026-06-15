-- ============================================================
-- AI Service Database Schema
-- Generated for tailor_is_ai database
-- Date: 2026-06-03
-- ============================================================

CREATE DATABASE IF NOT EXISTS `tailor_is_ai` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `tailor_is_ai`;

-- ============================================================
-- Table: body_size_data (身材数据)
-- ============================================================
DROP TABLE IF EXISTS `body_size_data`;
CREATE TABLE `body_size_data` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `size_name` VARCHAR(64) DEFAULT NULL COMMENT '尺寸名称',
  `height` DECIMAL(5,2) DEFAULT NULL COMMENT '身高(cm)',
  `weight` DECIMAL(5,2) DEFAULT NULL COMMENT '体重(kg)',
  `shoulder_width` DECIMAL(5,2) DEFAULT NULL COMMENT '肩宽(cm)',
  `chest_circumference` DECIMAL(5,2) DEFAULT NULL COMMENT '胸围(cm)',
  `waist_circumference` DECIMAL(5,2) DEFAULT NULL COMMENT '腰围(cm)',
  `hip_circumference` DECIMAL(5,2) DEFAULT NULL COMMENT '臀围(cm)',
  `neck_circumference` DECIMAL(5,2) DEFAULT NULL COMMENT '颈围(cm)',
  `arm_length` DECIMAL(5,2) DEFAULT NULL COMMENT '臂长(cm)',
  `sleeve_length` DECIMAL(5,2) DEFAULT NULL COMMENT '袖长(cm)',
  `waist_length` DECIMAL(5,2) DEFAULT NULL COMMENT '腰长(cm)',
  `inseam_length` DECIMAL(5,2) DEFAULT NULL COMMENT '裤内缝长(cm)',
  `body_type` VARCHAR(32) DEFAULT NULL COMMENT '体型分类',
  `gender` TINYINT DEFAULT '0' COMMENT '性别: 0=未知, 1=男, 2=女',
  `is_default` TINYINT DEFAULT '0' COMMENT '是否默认: 0=否, 1=是',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT DEFAULT '0' COMMENT '逻辑删除: 0=未删, 1=已删',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_default` (`user_id`, `is_default`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户身材数据表';

-- ============================================================
-- Table: pattern_record (版型记录)
-- ============================================================
DROP TABLE IF EXISTS `pattern_record`;
CREATE TABLE `pattern_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `pattern_name` VARCHAR(128) NOT NULL COMMENT '版型名称',
  `pattern_type` TINYINT DEFAULT NULL COMMENT '版型类型: 1=上衣, 2=裤装, 3=裙装, 4=外套',
  `body_size_id` BIGINT DEFAULT NULL COMMENT '关联身材数据ID',
  `parameters` TEXT COMMENT '版型参数(JSON)',
  `pattern_data` LONGTEXT COMMENT '版型数据(JSON/二进制)',
  `pattern_file_url` VARCHAR(512) DEFAULT NULL COMMENT '版型文件URL',
  `thumbnail_url` VARCHAR(512) DEFAULT NULL COMMENT '缩略图URL',
  `export_format` VARCHAR(32) DEFAULT NULL COMMENT '导出格式: PDF/DXF/PNG',
  `check_result` TEXT COMMENT '版型校验结果(JSON)',
  `check_status` TINYINT DEFAULT '0' COMMENT '校验状态: 0=未校验, 1=通过, 2=失败',
  `status` TINYINT DEFAULT '1' COMMENT '状态: 0=禁用, 1=启用',
  `version` INT DEFAULT '1' COMMENT '版本号',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT DEFAULT '0' COMMENT '逻辑删除: 0=未删, 1=已删',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_pattern_type` (`pattern_type`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='版型记录表';

-- ============================================================
-- Table: pattern_version (版型版本)
-- ============================================================
DROP TABLE IF EXISTS `pattern_version`;
CREATE TABLE `pattern_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `pattern_id` BIGINT NOT NULL COMMENT '版型ID',
  `version_no` INT NOT NULL DEFAULT '1' COMMENT '版本号',
  `version_name` VARCHAR(128) DEFAULT NULL COMMENT '版本名称',
  `pattern_data` LONGTEXT COMMENT '版型数据',
  `change_description` TEXT COMMENT '变更说明',
  `parameters_snapshot` TEXT COMMENT '参数快照(JSON)',
  `is_current` TINYINT DEFAULT '0' COMMENT '是否当前版本: 0=否, 1=是',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT DEFAULT '0' COMMENT '逻辑删除: 0=未删, 1=已删',
  PRIMARY KEY (`id`),
  KEY `idx_pattern_id` (`pattern_id`),
  KEY `idx_pattern_version` (`pattern_id`, `version_no`),
  KEY `idx_is_current` (`is_current`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='版型版本表';

-- ============================================================
-- Table: pattern_iteration (版型迭代)
-- ============================================================
DROP TABLE IF EXISTS `pattern_iteration`;
CREATE TABLE `pattern_iteration` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `pattern_id` BIGINT NOT NULL COMMENT '版型ID',
  `iteration_type` TINYINT DEFAULT NULL COMMENT '迭代类型: 1=参数调整, 2=AI推荐, 3=人工修改',
  `old_parameters` TEXT COMMENT '旧参数(JSON)',
  `new_parameters` TEXT COMMENT '新参数(JSON)',
  `change_reason` VARCHAR(512) DEFAULT NULL COMMENT '变更原因',
  `change_result` TEXT COMMENT '变更结果(JSON)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT DEFAULT '0' COMMENT '逻辑删除: 0=未删, 1=已删',
  PRIMARY KEY (`id`),
  KEY `idx_pattern_id` (`pattern_id`),
  KEY `idx_iteration_type` (`iteration_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='版型迭代历史表';

-- ============================================================
-- Initial Data
-- ============================================================

-- 体型分类字典数据(预留)
-- 体型分类参考：苹果型、梨型、沙漏型、直筒型、倒三角

-- ============================================================
-- Verification Queries
-- ============================================================
SELECT 'body_size_data' AS table_name, COUNT(*) AS row_count FROM body_size_data WHERE deleted = 0
UNION ALL
SELECT 'pattern_record', COUNT(*) FROM pattern_record WHERE deleted = 0
UNION ALL
SELECT 'pattern_version', COUNT(*) FROM pattern_version WHERE deleted = 0
UNION ALL
SELECT 'pattern_iteration', COUNT(*) FROM pattern_iteration WHERE deleted = 0;
