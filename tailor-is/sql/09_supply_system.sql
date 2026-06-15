-- ============================================================
-- Tailor IS 平台 - 供应链系统数据库表结构
-- 文件: 09_supply_system.sql
-- 版本: 1.0.0
-- 数据库: MySQL 8.0
-- 创建时间: 2026-05-29
-- ============================================================

-- 创建数据库（如不存在）
CREATE DATABASE IF NOT EXISTS `tailor_is_supply` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `tailor_is_supply`;

-- ============================================================
-- 1. 供需发布表 (supply_demand_post)
-- ============================================================
DROP TABLE IF EXISTS `supply_demand_post`;
CREATE TABLE `supply_demand_post` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '发布ID（主键）',
  `post_no` VARCHAR(64) NOT NULL COMMENT '发布编号（平台唯一）',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '发布用户ID',
  `merchant_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联商家ID',
  `shop_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联店铺ID',
  `post_type` TINYINT NOT NULL COMMENT '发布类型：1-供应，2-需求，3-合作',
  `title` VARCHAR(256) NOT NULL COMMENT '发布标题',
  `content` TEXT NOT NULL COMMENT '发布正文内容',
  `category` TINYINT NOT NULL COMMENT '分类：1-面料供应，2-辅料供应，3-加工需求，4-设计需求，5-设备需求，6-物流需求，7-其他',
  `sub_category` VARCHAR(64) DEFAULT NULL COMMENT '子分类/细分领域',
  `region_province` VARCHAR(64) DEFAULT NULL COMMENT '需求/供应省份',
  `region_city` VARCHAR(64) DEFAULT NULL COMMENT '需求/供应城市',
  `contact_info` JSON NOT NULL COMMENT '联系方式（JSON格式：电话/微信/邮箱等）',
  `budget_min` DECIMAL(12, 2) DEFAULT NULL COMMENT '预算最低金额（元）',
  `budget_max` DECIMAL(12, 2) DEFAULT NULL COMMENT '预算最高金额（元）',
  `quantity` DECIMAL(12, 2) DEFAULT NULL COMMENT '数量',
  `quantity_unit` VARCHAR(32) DEFAULT NULL COMMENT '数量单位（米/件/吨等）',
  `images` JSON DEFAULT NULL COMMENT '发布图片（JSON数组）',
  `attachments` JSON DEFAULT NULL COMMENT '附件文件（JSON数组）',
  `deadline` DATETIME DEFAULT NULL COMMENT '截止/有效期',
  `urgency` TINYINT DEFAULT 1 COMMENT '紧急程度：1-普通，2-紧急，3-非常紧急',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-草稿，1-已发布，2-已关闭，3-已完成，4-已下架，5-审核不通过',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览量',
  `contact_count` INT NOT NULL DEFAULT 0 COMMENT '联系/咨询次数',
  `match_count` INT NOT NULL DEFAULT 0 COMMENT '匹配成功次数',
  `audit_status` TINYINT DEFAULT 0 COMMENT '审核状态：0-免审，1-待审核，2-已通过，3-已驳回',
  `audit_remark` VARCHAR(512) DEFAULT NULL COMMENT '审核备注',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `ip_address` VARCHAR(45) DEFAULT NULL COMMENT '发布IP地址',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_no` (`post_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_post_type` (`post_type`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_region_city` (`region_city`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_deadline` (`deadline`),
  FULLTEXT KEY `ft_title_content` (`title`, `content`) WITH PARSER ngram
  -- 外键约束（可选）
  -- CONSTRAINT `fk_sdp_user` FOREIGN KEY (`user_id`) REFERENCES `tailor_is_user`.`sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='供需信息发布表';

-- ============================================================
-- 2. 供需匹配记录表 (supply_match_record)
-- ============================================================
DROP TABLE IF EXISTS `supply_match_record`;
CREATE TABLE `supply_match_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '匹配记录ID（主键）',
  `post_id` BIGINT UNSIGNED NOT NULL COMMENT '原发布ID',
  `matched_post_id` BIGINT UNSIGNED NOT NULL COMMENT '匹配的发布ID',
  `match_type` TINYINT NOT NULL COMMENT '匹配方向：1-供应匹配需求，2-需求匹配供应，3-合作匹配',
  `match_source` TINYINT NOT NULL DEFAULT 1 COMMENT '匹配来源：1-系统自动匹配，2-用户手动关联',
  `match_score` DECIMAL(5, 2) DEFAULT 0.00 COMMENT '匹配度分数（0.00-100.00）',
  `match_reasons` JSON DEFAULT NULL COMMENT '匹配依据说明（JSON数组）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待确认，1-已确认对接，2-已建立联系，3-已达成合作，4-已关闭，5-已过期',
  `confirm_time` DATETIME DEFAULT NULL COMMENT '确认对接时间',
  `cooperate_time` DATETIME DEFAULT NULL COMMENT '达成合作时间',
  `cooperate_remark` VARCHAR(512) DEFAULT NULL COMMENT '合作备注',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_matched_post_id` (`matched_post_id`),
  KEY `idx_match_type` (`match_type`),
  KEY `idx_match_source` (`match_source`),
  KEY `idx_match_score` (`match_score`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_smr_post` FOREIGN KEY (`post_id`) REFERENCES `supply_demand_post` (`id`),
  -- CONSTRAINT `fk_smr_matched` FOREIGN KEY (`matched_post_id`) REFERENCES `supply_demand_post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='供需匹配记录表';

-- ============================================================
-- 3. 供需联系记录表 (supply_contact_record)
-- ============================================================
DROP TABLE IF EXISTS `supply_contact_record`;
CREATE TABLE `supply_contact_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '联系记录ID（主键）',
  `post_id` BIGINT UNSIGNED NOT NULL COMMENT '被联系的发布ID',
  `contact_user_id` BIGINT UNSIGNED NOT NULL COMMENT '联系人用户ID',
  `contact_merchant_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '联系人商家ID',
  `message` VARCHAR(1000) NOT NULL COMMENT '联系留言/意向说明',
  `contact_method` TINYINT DEFAULT 1 COMMENT '联系方式偏好：1-站内消息，2-电话，3-微信，4-邮件',
  `attachments` JSON DEFAULT NULL COMMENT '附件（报价单/样品图等，JSON数组）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待回复，1-已回复，2-已拒绝，3-已忽略，4-已达成意向',
  `reply_message` VARCHAR(1000) DEFAULT NULL COMMENT '回复内容',
  `reply_time` DATETIME DEFAULT NULL COMMENT '回复时间',
  `contact_count` INT DEFAULT 1 COMMENT '联系次数',
  `ip_address` VARCHAR(45) DEFAULT NULL COMMENT '联系IP地址',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_contact_user_id` (`contact_user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_scr_post` FOREIGN KEY (`post_id`) REFERENCES `supply_demand_post` (`id`),
  -- CONSTRAINT `fk_scr_user` FOREIGN KEY (`contact_user_id`) REFERENCES `tailor_is_user`.`sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='供需联系记录表';

-- ============================================================
-- 4. 供应商库表 (supplier_directory)
-- ============================================================
DROP TABLE IF EXISTS `supplier_directory`;
CREATE TABLE `supplier_directory` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '供应商ID（主键）',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '关联用户ID',
  `merchant_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联商家ID',
  `supplier_name` VARCHAR(128) NOT NULL COMMENT '供应商/企业名称',
  `supplier_type` TINYINT NOT NULL COMMENT '供应商类型：1-面料供应商，2-辅料供应商，3-加工厂，4-设计工作室，5-物流服务商，6-其他',
  `business_scope` VARCHAR(512) DEFAULT NULL COMMENT '经营范围/服务类型',
  `main_products` JSON DEFAULT NULL COMMENT '主营产品/服务（JSON数组）',
  `province` VARCHAR(64) DEFAULT NULL COMMENT '所在省份',
  `city` VARCHAR(64) DEFAULT NULL COMMENT '所在城市',
  `district` VARCHAR(64) DEFAULT NULL COMMENT '所在区县',
  `address` VARCHAR(256) DEFAULT NULL COMMENT '详细地址',
  `logo` VARCHAR(512) DEFAULT NULL COMMENT 'Logo图片URL',
  `images` JSON DEFAULT NULL COMMENT '展示图片（JSON数组）',
  `description` TEXT DEFAULT NULL COMMENT '企业简介',
  `certifications` JSON DEFAULT NULL COMMENT '资质认证（JSON数组）',
  `contact_name` VARCHAR(64) NOT NULL COMMENT '联系人姓名',
  `contact_phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
  `contact_email` VARCHAR(128) DEFAULT NULL COMMENT '联系邮箱',
  `contact_wechat` VARCHAR(64) DEFAULT NULL COMMENT '联系微信',
  `min_order_quantity` DECIMAL(12, 2) DEFAULT NULL COMMENT '最小起订量',
  `delivery_time` VARCHAR(128) DEFAULT NULL COMMENT '交货周期',
  `rating` DECIMAL(3, 2) DEFAULT 5.00 COMMENT '评分（0.00-5.00）',
  `transaction_count` INT DEFAULT 0 COMMENT '成交次数',
  `response_rate` DECIMAL(5, 2) DEFAULT 0.00 COMMENT '回复率（0.00-100.00）',
  `response_time` INT DEFAULT NULL COMMENT '平均响应时间（分钟）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-待审核，1-正常，2-已冻结',
  `audit_status` TINYINT DEFAULT 0 COMMENT '审核状态：0-待审核，1-已通过，2-已驳回',
  `is_verified` TINYINT DEFAULT 0 COMMENT '是否认证商家：0-否，1-是',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_supplier_type` (`supplier_type`),
  KEY `idx_city` (`city`),
  KEY `idx_status` (`status`),
  KEY `idx_rating` (`rating`),
  KEY `idx_is_verified` (`is_verified`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_sd_user` FOREIGN KEY (`user_id`) REFERENCES `tailor_is_user`.`sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='供应商信息库表';

-- ============================================================
-- 5. 供应链评价表 (supply_review)
-- ============================================================
DROP TABLE IF EXISTS `supply_review`;
CREATE TABLE `supply_review` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评价ID（主键）',
  `reviewer_id` BIGINT UNSIGNED NOT NULL COMMENT '评价人用户ID',
  `supplier_id` BIGINT UNSIGNED NOT NULL COMMENT '被评价供应商ID',
  `related_post_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联供需发布ID',
  `related_contact_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联联系记录ID',
  `overall_rating` TINYINT NOT NULL COMMENT '总体评分：1-5星',
  `quality_rating` TINYINT DEFAULT NULL COMMENT '质量评分：1-5星',
  `service_rating` TINYINT DEFAULT NULL COMMENT '服务评分：1-5星',
  `delivery_rating` TINYINT DEFAULT NULL COMMENT '交付评分：1-5星',
  `price_rating` TINYINT DEFAULT NULL COMMENT '价格评分：1-5星',
  `content` VARCHAR(1000) DEFAULT NULL COMMENT '评价内容',
  `images` JSON DEFAULT NULL COMMENT '评价图片（JSON数组）',
  `is_anonymous` TINYINT DEFAULT 0 COMMENT '是否匿名：0-否，1-是',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-待审核，1-已发布，2-已删除',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_reviewer_id` (`reviewer_id`),
  KEY `idx_supplier_id` (`supplier_id`),
  KEY `idx_related_post_id` (`related_post_id`),
  KEY `idx_overall_rating` (`overall_rating`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_sr_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `tailor_is_user`.`sys_user` (`id`),
  -- CONSTRAINT `fk_sr_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `supplier_directory` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='供应商评价表';
