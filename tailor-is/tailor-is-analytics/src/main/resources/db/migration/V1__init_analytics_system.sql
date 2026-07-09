-- ============================================================
-- Tailor IS - Flyway Migration V1: 数据分析系统数据库初始化
-- 数据库: tailor_is_analytics
-- 创建时间: 2026-07-10
-- 说明: 基线迁移，创建数据分析系统所有表结构
-- ============================================================

CREATE TABLE IF NOT EXISTS `metrics_snapshot` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '快照ID（主键）',
  `metric_type` VARCHAR(50) NOT NULL COMMENT '指标类型revenue/order/user/view等',
  `metric_key` VARCHAR(100) NOT NULL COMMENT '指标键名',
  `metric_value` DECIMAL(20, 4) COMMENT '指标数值',
  `snapshot_date` DATE NOT NULL COMMENT '快照日期',
  `dimension` VARCHAR(200) COMMENT '维度信息JSON格式',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_metric_date` (`metric_type`, `metric_key`, `snapshot_date`),
  KEY `idx_metric_type_date` (`metric_type`, `snapshot_date`),
  KEY `idx_metric_key` (`metric_key`),
  KEY `idx_snapshot_date` (`snapshot_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='指标快照表';

CREATE TABLE IF NOT EXISTS `data_report` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '报告ID（主键）',
  `report_name` VARCHAR(200) NOT NULL COMMENT '报告名称',
  `report_type` VARCHAR(50) NOT NULL COMMENT '报告类型：monthly/quarterly/enterprise/custom',
  `user_id` BIGINT UNSIGNED COMMENT '用户ID',
  `merchant_id` BIGINT UNSIGNED COMMENT '商户ID',
  `report_period` VARCHAR(32) NOT NULL COMMENT '报告周期：202401/2024Q1等',
  `data_scope` VARCHAR(100) COMMENT '数据范围',
  `report_data` LONGTEXT COMMENT '报告数据JSON格式',
  `status` TINYINT DEFAULT 0 COMMENT '状态：0-生成中，1-已完成，2-失败',
  `download_url` VARCHAR(512) COMMENT '下载链接',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_report_type` (`report_type`),
  KEY `idx_status` (`status`),
  KEY `idx_report_period` (`report_period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据报告表';