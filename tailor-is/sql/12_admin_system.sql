-- ============================================================
-- Tailor IS 平台 - 管理后台数据库表结构
-- 文件: 12_admin_system.sql
-- 版本: 1.0.0
-- 数据库: MySQL 8.0
-- 创建时间: 2026-07-20
-- ============================================================

-- ⚠️ 警告：此脚本仅用于初始化部署，请勿在生产环境执行 DROP TABLE 操作
-- 生产环境数据库变更请使用 Flyway/Liquibase 版本化迁移工具管理

CREATE DATABASE IF NOT EXISTS `tailor_is_admin` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `tailor_is_admin`;

-- ============================================================
-- 1. 平台配置表 (sys_config)
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_config` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key` VARCHAR(128) NOT NULL COMMENT '配置键',
  `config_value` TEXT NOT NULL COMMENT '配置值',
  `config_type` VARCHAR(32) NOT NULL DEFAULT 'string' COMMENT '配置类型：string/number/boolean/json',
  `description` VARCHAR(256) DEFAULT NULL COMMENT '配置说明',
  `is_system` TINYINT NOT NULL DEFAULT 0 COMMENT '是否系统配置：0-否，1-是',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='平台配置表';

-- ============================================================
-- 2. 操作日志表 (sys_operation_log)
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_operation_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '操作用户ID',
  `username` VARCHAR(64) DEFAULT NULL COMMENT '操作用户名',
  `module` VARCHAR(64) NOT NULL COMMENT '操作模块',
  `operation` VARCHAR(64) NOT NULL COMMENT '操作类型',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '操作描述',
  `request_method` VARCHAR(16) DEFAULT NULL COMMENT '请求方法',
  `request_url` VARCHAR(512) DEFAULT NULL COMMENT '请求URL',
  `request_params` TEXT DEFAULT NULL COMMENT '请求参数',
  `ip_address` VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` VARCHAR(512) DEFAULT NULL COMMENT '用户代理',
  `execution_time` BIGINT DEFAULT NULL COMMENT '执行耗时(ms)',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '操作状态：0-失败，1-成功',
  `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_module` (`module`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='操作日志表';

-- ============================================================
-- 3. 佣金配置表 (commission_config)
-- ============================================================
CREATE TABLE IF NOT EXISTS `commission_config` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `category_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '商品分类ID，NULL表示全局默认',
  `commission_rate` DECIMAL(5,4) NOT NULL DEFAULT 0.0000 COMMENT '佣金比例',
  `min_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '最低佣金金额',
  `max_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '最高佣金金额',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='佣金配置表';

-- ============================================================
-- 4. 财务报表表 (financial_report)
-- ============================================================
CREATE TABLE IF NOT EXISTS `financial_report` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `report_type` VARCHAR(32) NOT NULL COMMENT '报表类型：daily/monthly/quarterly/yearly',
  `report_date` DATE NOT NULL COMMENT '报表日期',
  `total_revenue` DECIMAL(16,2) NOT NULL DEFAULT 0.00 COMMENT '总营收',
  `total_commission` DECIMAL(16,2) NOT NULL DEFAULT 0.00 COMMENT '总佣金',
  `total_orders` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总订单数',
  `total_users` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '新增用户数',
  `total_merchants` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '新增商户数',
  `report_data` JSON DEFAULT NULL COMMENT '详细报表数据',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-草稿，1-已发布',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report` (`report_type`, `report_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='财务报表表';

-- ============================================================
-- 初始数据
-- ============================================================
INSERT INTO `sys_config` (`config_key`, `config_value`, `config_type`, `description`, `is_system`) VALUES
('platform_name', 'Tailor IS 裁智云', 'string', '平台名称', 1),
('platform_logo', '/logo.png', 'string', '平台Logo', 1),
('default_commission_rate', '0.0500', 'number', '默认佣金比例', 1),
('max_upload_size', '10485760', 'number', '最大上传文件大小(字节)', 1),
('order_auto_cancel_minutes', '30', 'number', '订单自动取消时间(分钟)', 1)
ON DUPLICATE KEY UPDATE `config_value` = VALUES(`config_value`);