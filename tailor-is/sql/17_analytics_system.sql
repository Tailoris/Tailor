-- ============================================================
-- Tailor IS 平台 - 数据分析服务数据库表结构
-- 文件: 17_analytics_system.sql
-- 版本: 1.0.0
-- 数据库: MySQL 8.0
-- 创建时间: 2026-07-20
-- ============================================================

CREATE DATABASE IF NOT EXISTS `tailor_is_analytics` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `tailor_is_analytics`;

-- ============================================================
-- 1. 平台统计表 (platform_statistics)
-- ============================================================
CREATE TABLE IF NOT EXISTS `platform_statistics` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `stat_date` DATE NOT NULL COMMENT '统计日期',
  `stat_type` VARCHAR(32) NOT NULL COMMENT '统计类型：daily/monthly',
  `total_users` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总用户数',
  `new_users` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '新增用户数',
  `active_users` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '活跃用户数',
  `total_orders` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总订单数',
  `completed_orders` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '完成订单数',
  `total_revenue` DECIMAL(16,2) NOT NULL DEFAULT 0.00 COMMENT '总营收',
  `total_refund` DECIMAL(16,2) NOT NULL DEFAULT 0.00 COMMENT '总退款金额',
  `conversion_rate` DECIMAL(5,4) DEFAULT 0.0000 COMMENT '转化率',
  `avg_order_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '平均客单价',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date_type` (`stat_date`, `stat_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='平台统计表';

-- ============================================================
-- 2. 用户画像表 (user_profile)
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_profile` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `age_group` VARCHAR(16) DEFAULT NULL COMMENT '年龄段',
  `gender` TINYINT DEFAULT NULL COMMENT '性别：0-未知，1-男，2-女',
  `region` VARCHAR(64) DEFAULT NULL COMMENT '地区',
  `preferred_categories` JSON DEFAULT NULL COMMENT '偏好分类',
  `preferred_styles` JSON DEFAULT NULL COMMENT '偏好风格',
  `price_sensitivity` VARCHAR(16) DEFAULT NULL COMMENT '价格敏感度：low/medium/high',
  `purchase_frequency` VARCHAR(16) DEFAULT NULL COMMENT '购买频率：low/medium/high',
  `total_spent` DECIMAL(16,2) NOT NULL DEFAULT 0.00 COMMENT '累计消费金额',
  `order_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '累计订单数',
  `last_order_time` DATETIME DEFAULT NULL COMMENT '最后下单时间',
  `tags` JSON DEFAULT NULL COMMENT '用户标签',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户画像表';

-- ============================================================
-- 3. 数据大屏配置表 (dashboard_config)
-- ============================================================
CREATE TABLE IF NOT EXISTS `dashboard_config` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dashboard_name` VARCHAR(64) NOT NULL COMMENT '大屏名称',
  `dashboard_type` VARCHAR(32) NOT NULL COMMENT '类型：realtime/daily_report/executive',
  `config_json` JSON NOT NULL COMMENT '大屏配置JSON',
  `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据大屏配置表';

-- ============================================================
-- 4. 经营分析表 (business_analysis)
-- ============================================================
CREATE TABLE IF NOT EXISTS `business_analysis` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `merchant_id` BIGINT UNSIGNED NOT NULL COMMENT '商户ID',
  `analysis_date` DATE NOT NULL COMMENT '分析日期',
  `analysis_type` VARCHAR(32) NOT NULL COMMENT '分析类型：daily/weekly/monthly',
  `sales_amount` DECIMAL(16,2) NOT NULL DEFAULT 0.00 COMMENT '销售额',
  `order_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '订单数',
  `refund_amount` DECIMAL(16,2) NOT NULL DEFAULT 0.00 COMMENT '退款金额',
  `new_customers` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '新客户数',
  `repeat_customers` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '回头客数',
  `top_products` JSON DEFAULT NULL COMMENT '热销商品',
  `analysis_data` JSON DEFAULT NULL COMMENT '详细分析数据',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_date_type` (`merchant_id`, `analysis_date`, `analysis_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='经营分析表';