-- ============================================================
-- Tailor IS 平台 - 商家系统数据库表结构
-- 文件: 02_merchant_system.sql
-- 版本: 1.0.0
-- 数据库: MySQL 8.0
-- 创建时间: 2026-05-29
-- ============================================================

-- 创建数据库（如不存在）
CREATE DATABASE IF NOT EXISTS `tailor_is_merchant` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `tailor_is_merchant`;

-- ============================================================
-- 1. 商家表 (merchant)
-- ============================================================
DROP TABLE IF EXISTS `merchant`;
CREATE TABLE `merchant` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '商家ID（主键）',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '关联用户ID',
  `merchant_type` TINYINT NOT NULL COMMENT '商家类型：1-个人，2-企业，3-个体工商户',
  `company_name` VARCHAR(128) DEFAULT NULL COMMENT '企业/公司名称',
  `license_no` VARCHAR(64) DEFAULT NULL COMMENT '营业执照号/统一社会信用代码',
  `contact_name` VARCHAR(64) NOT NULL COMMENT '联系人姓名',
  `contact_phone` VARCHAR(20) NOT NULL COMMENT '联系人电话',
  `contact_email` VARCHAR(128) DEFAULT NULL COMMENT '联系人邮箱',
  `province` VARCHAR(64) DEFAULT NULL COMMENT '所在省份',
  `city` VARCHAR(64) DEFAULT NULL COMMENT '所在城市',
  `district` VARCHAR(64) DEFAULT NULL COMMENT '所在区县',
  `address` VARCHAR(256) DEFAULT NULL COMMENT '详细地址',
  `business_scope` VARCHAR(512) DEFAULT NULL COMMENT '经营范围',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待审核，1-正常，2-冻结，3-注销',
  `audit_status` TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态：0-待审核，1-审核中，2-已通过，3-已驳回',
  `audit_remark` VARCHAR(512) DEFAULT NULL COMMENT '审核备注',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `audit_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '审核人ID',
  `join_time` DATETIME DEFAULT NULL COMMENT '入驻时间',
  `expire_time` DATETIME DEFAULT NULL COMMENT '资质到期时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_license_no` (`license_no`),
  KEY `idx_merchant_type` (`merchant_type`),
  KEY `idx_status` (`status`),
  KEY `idx_audit_status` (`audit_status`),
  KEY `idx_created_at` (`created_at`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_merchant_user` FOREIGN KEY (`user_id`) REFERENCES `tailor_is_user`.`sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商家信息表';

-- ============================================================
-- 2. 店铺表 (merchant_shop)
-- ============================================================
DROP TABLE IF EXISTS `merchant_shop`;
CREATE TABLE `merchant_shop` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '店铺ID（主键）',
  `merchant_id` BIGINT UNSIGNED NOT NULL COMMENT '商家ID',
  `shop_name` VARCHAR(128) NOT NULL COMMENT '店铺名称',
  `shop_logo` VARCHAR(512) DEFAULT NULL COMMENT '店铺Logo URL',
  `shop_banner` VARCHAR(512) DEFAULT NULL COMMENT '店铺Banner URL',
  `shop_desc` VARCHAR(512) DEFAULT NULL COMMENT '店铺描述/简介',
  `shop_status` TINYINT NOT NULL DEFAULT 0 COMMENT '店铺状态：0-装修中，1-营业中，2-暂停营业，3-已关闭',
  `decoration_config` JSON DEFAULT NULL COMMENT '店铺装修配置（JSON格式）',
  `shop_theme` VARCHAR(32) DEFAULT 'default' COMMENT '店铺主题模板',
  `announcement` VARCHAR(512) DEFAULT NULL COMMENT '店铺公告',
  `contact_service` VARCHAR(32) DEFAULT NULL COMMENT '客服联系方式',
  `province` VARCHAR(64) DEFAULT NULL COMMENT '店铺所在省份',
  `city` VARCHAR(64) DEFAULT NULL COMMENT '店铺所在城市',
  `district` VARCHAR(64) DEFAULT NULL COMMENT '店铺所在区县',
  `address` VARCHAR(256) DEFAULT NULL COMMENT '店铺详细地址',
  `longitude` DECIMAL(10, 6) DEFAULT NULL COMMENT '经度',
  `latitude` DECIMAL(10, 6) DEFAULT NULL COMMENT '纬度',
  `shop_rating` DECIMAL(3, 2) DEFAULT 5.00 COMMENT '店铺评分（0.00-5.00）',
  `follower_count` INT DEFAULT 0 COMMENT '关注数/粉丝数',
  `product_count` INT DEFAULT 0 COMMENT '商品数量',
  `sales_count` INT DEFAULT 0 COMMENT '总销量',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_shop_status` (`shop_status`),
  KEY `idx_shop_name` (`shop_name`),
  KEY `idx_created_at` (`created_at`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_shop_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `merchant` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商家店铺表';

-- ============================================================
-- 3. 员工表 (merchant_employee)
-- ============================================================
DROP TABLE IF EXISTS `merchant_employee`;
CREATE TABLE `merchant_employee` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '员工ID（主键）',
  `merchant_id` BIGINT UNSIGNED NOT NULL COMMENT '商家ID',
  `shop_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '店铺ID（为空表示管理所有店铺）',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '关联用户ID',
  `employee_name` VARCHAR(64) NOT NULL COMMENT '员工姓名',
  `employee_phone` VARCHAR(20) DEFAULT NULL COMMENT '员工电话',
  `role` TINYINT NOT NULL COMMENT '员工角色：1-店长，2-运营，3-客服，4-库管，5-财务',
  `permissions` JSON DEFAULT NULL COMMENT '权限配置（JSON格式）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
  `hire_date` DATE DEFAULT NULL COMMENT '入职日期',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_user` (`merchant_id`, `user_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role` (`role`),
  KEY `idx_status` (`status`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_emp_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `merchant` (`id`),
  -- CONSTRAINT `fk_emp_shop` FOREIGN KEY (`shop_id`) REFERENCES `merchant_shop` (`id`),
  -- CONSTRAINT `fk_emp_user` FOREIGN KEY (`user_id`) REFERENCES `tailor_is_user`.`sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商家员工表';

-- ============================================================
-- 4. 商家资质表 (merchant_qualification)
-- ============================================================
DROP TABLE IF EXISTS `merchant_qualification`;
CREATE TABLE `merchant_qualification` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '资质ID（主键）',
  `merchant_id` BIGINT UNSIGNED NOT NULL COMMENT '商家ID',
  `cert_type` TINYINT NOT NULL COMMENT '证件类型：1-营业执照，2-税务登记证，3-组织机构代码证，4-食品经营许可证，5-其他',
  `cert_name` VARCHAR(128) NOT NULL COMMENT '证件名称',
  `cert_no` VARCHAR(64) DEFAULT NULL COMMENT '证件编号',
  `cert_url` VARCHAR(512) NOT NULL COMMENT '证件图片URL',
  `cert_front_url` VARCHAR(512) DEFAULT NULL COMMENT '证件正面图片URL',
  `cert_back_url` VARCHAR(512) DEFAULT NULL COMMENT '证件反面图片URL',
  `issue_date` DATE DEFAULT NULL COMMENT '发证日期',
  `expire_date` DATE DEFAULT NULL COMMENT '到期日期',
  `audit_status` TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态：0-待审核，1-审核中，2-已通过，3-已驳回',
  `audit_remark` VARCHAR(512) DEFAULT NULL COMMENT '审核备注',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `audit_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '审核人ID',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_cert_type` (`cert_type`),
  KEY `idx_audit_status` (`audit_status`),
  KEY `idx_expire_date` (`expire_date`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_qual_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `merchant` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商家资质证件表';
