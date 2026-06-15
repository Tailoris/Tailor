-- ============================================================
-- Tailor IS 平台 - 版权系统数据库表结构
-- 文件: 07_copyright_system.sql
-- 版本: 1.0.0
-- 数据库: MySQL 8.0
-- 创建时间: 2026-05-29
-- ============================================================

-- 创建数据库（如不存在）
CREATE DATABASE IF NOT EXISTS `tailor_is_copyright` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `tailor_is_copyright`;

-- ============================================================
-- 1. 版权登记表 (copyright_record)
-- ============================================================
DROP TABLE IF EXISTS `copyright_record`;
CREATE TABLE `copyright_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '版权登记ID（主键）',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '版权所有者用户ID',
  `product_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联商品ID（可为空，先登记后关联）',
  `work_name` VARCHAR(256) NOT NULL COMMENT '作品名称',
  `work_type` TINYINT NOT NULL COMMENT '作品类型：1-图案设计，2-服装设计，3-印花图案，4-绣花图案，5-版式设计，6-其他',
  `file_hash` VARCHAR(128) NOT NULL COMMENT '文件哈希值（SHA-256，用于唯一标识）',
  `file_type` VARCHAR(32) NOT NULL COMMENT '文件类型：PNG/JPG/SVG/AI/PSD/PDF等',
  `file_size` BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
  `file_url` VARCHAR(512) NOT NULL COMMENT '原始文件存储URL',
  `thumbnail_url` VARCHAR(512) DEFAULT NULL COMMENT '缩略图URL',
  `description` VARCHAR(1000) DEFAULT NULL COMMENT '作品描述',
  `blockchain_tx_hash` VARCHAR(128) DEFAULT NULL COMMENT '区块链交易哈希（存证哈希）',
  `blockchain_tx_time` DATETIME DEFAULT NULL COMMENT '区块链存证时间',
  `blockchain_platform` VARCHAR(64) DEFAULT NULL COMMENT '区块链平台（如：蚂蚁链/腾讯云链等）',
  `blockchain_cert_no` VARCHAR(128) DEFAULT NULL COMMENT '区块链存证证书编号',
  `certificate_url` VARCHAR(512) DEFAULT NULL COMMENT '版权证书下载URL',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待提交，1-存证中，2-存证成功，3-存证失败，4-已撤销',
  `fail_reason` VARCHAR(512) DEFAULT NULL COMMENT '存证失败原因',
  `registered_at` DATETIME DEFAULT NULL COMMENT '登记成功时间',
  `expire_time` DATETIME DEFAULT NULL COMMENT '版权有效期（可选）',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_hash` (`file_hash`),
  UNIQUE KEY `uk_blockchain_tx_hash` (`blockchain_tx_hash`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_work_type` (`work_type`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_cr_user` FOREIGN KEY (`user_id`) REFERENCES `tailor_is_user`.`sys_user` (`id`),
  -- CONSTRAINT `fk_cr_product` FOREIGN KEY (`product_id`) REFERENCES `tailor_is_product`.`product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='版权登记存证表';

-- ============================================================
-- 2. 侵权举报表 (infringement_record)
-- ============================================================
DROP TABLE IF EXISTS `infringement_record`;
CREATE TABLE `infringement_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '侵权举报ID（主键）',
  `report_no` VARCHAR(64) NOT NULL COMMENT '举报编号（平台唯一）',
  `reporter_id` BIGINT UNSIGNED NOT NULL COMMENT '举报人用户ID',
  `copyright_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联版权登记ID',
  `reported_product_id` BIGINT UNSIGNED NOT NULL COMMENT '被举报商品ID',
  `reported_user_id` BIGINT UNSIGNED NOT NULL COMMENT '被举报用户/商家ID',
  `reported_shop_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '被举报店铺ID',
  `reported_merchant_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '被举报商家ID',
  `infringement_type` TINYINT NOT NULL COMMENT '侵权类型：1-盗用图片，2-抄袭设计，3-冒用品牌，4-仿冒商品，5-其他',
  `reason` VARCHAR(256) NOT NULL COMMENT '举报原因简述',
  `description` TEXT DEFAULT NULL COMMENT '侵权详细说明',
  `evidence_images` JSON DEFAULT NULL COMMENT '举证图片（JSON数组）',
  `evidence_files` JSON DEFAULT NULL COMMENT '举证文件（JSON数组）',
  `evidence_urls` JSON DEFAULT NULL COMMENT '举证链接（JSON数组）',
  `comparison_description` TEXT DEFAULT NULL COMMENT '原创作品与侵权作品对比说明',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态：0-待审核，1-审核中，2-举报成立，3-举报不成立，4-已撤销，5-平台介入中，6-仲裁中，7-仲裁完成',
  `urgency` TINYINT DEFAULT 1 COMMENT '紧急程度：1-普通，2-紧急，3-非常紧急',
  `handler_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '处理人ID（平台管理员）',
  `handler_remark` VARCHAR(512) DEFAULT NULL COMMENT '处理备注/判定说明',
  `handled_at` DATETIME DEFAULT NULL COMMENT '处理完成时间',
  `punishment_type` TINYINT DEFAULT NULL COMMENT '处罚类型：1-下架商品，2-警告，3-扣分，4-冻结店铺，5-封号',
  `punishment_detail` JSON DEFAULT NULL COMMENT '处罚详情（JSON）',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（举报时间）',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_no` (`report_no`),
  KEY `idx_reporter_id` (`reporter_id`),
  KEY `idx_copyright_id` (`copyright_id`),
  KEY `idx_reported_product_id` (`reported_product_id`),
  KEY `idx_reported_user_id` (`reported_user_id`),
  KEY `idx_reported_merchant_id` (`reported_merchant_id`),
  KEY `idx_infringement_type` (`infringement_type`),
  KEY `idx_status` (`status`),
  KEY `idx_urgency` (`urgency`),
  KEY `idx_created_at` (`created_at`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_ir_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `tailor_is_user`.`sys_user` (`id`),
  -- CONSTRAINT `fk_ir_copyright` FOREIGN KEY (`copyright_id`) REFERENCES `copyright_record` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='侵权举报表';

-- ============================================================
-- 3. 仲裁记录表 (arbitration_record)
-- ============================================================
DROP TABLE IF EXISTS `arbitration_record`;
CREATE TABLE `arbitration_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '仲裁记录ID（主键）',
  `arbitration_no` VARCHAR(64) NOT NULL COMMENT '仲裁编号（平台唯一）',
  `infringement_id` BIGINT UNSIGNED NOT NULL COMMENT '关联侵权举报ID',
  `report_no` VARCHAR(64) NOT NULL COMMENT '举报编号（冗余字段）',
  `arbitrator_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '仲裁员ID（平台指定或第三方）',
  `arbitrator_name` VARCHAR(64) DEFAULT NULL COMMENT '仲裁员姓名',
  `arbitrator_type` TINYINT DEFAULT 1 COMMENT '仲裁员类型：1-平台仲裁员，2-第三方仲裁机构，3-专家评审',
  `result` TINYINT NOT NULL DEFAULT 0 COMMENT '仲裁结果：0-待仲裁，1-侵权成立，2-侵权不成立，3-部分侵权',
  `result_description` TEXT DEFAULT NULL COMMENT '仲裁结果详细说明',
  `confidence_level` TINYINT DEFAULT NULL COMMENT '判定置信度：1-确定，2-高度可能，3-可能，4-存疑',
  `evidence_analysis` TEXT DEFAULT NULL COMMENT '证据分析说明',
  `evidence` JSON DEFAULT NULL COMMENT '仲裁证据材料（JSON数组）',
  `penalty_recommendation` JSON DEFAULT NULL COMMENT '处罚建议（JSON格式）',
  `reporter_appeal` TINYINT DEFAULT 0 COMMENT '举报方是否申诉：0-否，1-是',
  `reporter_appeal_reason` TEXT DEFAULT NULL COMMENT '举报方申诉理由',
  `reported_appeal` TINYINT DEFAULT 0 COMMENT '被举报方是否申诉：0-否，1-是',
  `reported_appeal_reason` TEXT DEFAULT NULL COMMENT '被举报方申诉理由',
  `final_result` TINYINT DEFAULT NULL COMMENT '最终结果（考虑申诉后）：1-维持原判，2-改判',
  `closed_at` DATETIME DEFAULT NULL COMMENT '仲裁结案时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_arbitration_no` (`arbitration_no`),
  KEY `idx_infringement_id` (`infringement_id`),
  KEY `idx_report_no` (`report_no`),
  KEY `idx_arbitrator_id` (`arbitrator_id`),
  KEY `idx_result` (`result`),
  KEY `idx_created_at` (`created_at`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_ar_infringement` FOREIGN KEY (`infringement_id`) REFERENCES `infringement_record` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='侵权仲裁记录表';

-- ============================================================
-- 4. 版权授权表 (copyright_license)
-- ============================================================
DROP TABLE IF EXISTS `copyright_license`;
CREATE TABLE `copyright_license` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '授权记录ID（主键）',
  `license_no` VARCHAR(64) NOT NULL COMMENT '授权编号（平台唯一）',
  `copyright_id` BIGINT UNSIGNED NOT NULL COMMENT '版权登记ID',
  `licensor_id` BIGINT UNSIGNED NOT NULL COMMENT '授权方用户ID（版权所有者）',
  `licensee_id` BIGINT UNSIGNED NOT NULL COMMENT '被授权方用户ID',
  `license_type` TINYINT NOT NULL COMMENT '授权类型：1-独占授权，2-排他授权，3-普通授权',
  `scope` TINYINT NOT NULL COMMENT '授权范围：1-生产使用权，2-销售权，3-修改权，4-全权',
  `authorized_products` JSON DEFAULT NULL COMMENT '授权使用的商品范围（JSON数组）',
  `start_date` DATE NOT NULL COMMENT '授权生效日期',
  `end_date` DATE NOT NULL COMMENT '授权到期日期',
  `license_fee` DECIMAL(12, 2) DEFAULT 0.00 COMMENT '授权费用（元）',
  `royalty_rate` DECIMAL(5, 4) DEFAULT NULL COMMENT '版税分成比例（如0.1000表示10%）',
  `agreement_url` VARCHAR(512) DEFAULT NULL COMMENT '授权协议文件URL',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待生效，1-生效中，2-已到期，3-已撤销，4-已违约',
  `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注说明',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_license_no` (`license_no`),
  KEY `idx_copyright_id` (`copyright_id`),
  KEY `idx_licensor_id` (`licensor_id`),
  KEY `idx_licensee_id` (`licensee_id`),
  KEY `idx_license_type` (`license_type`),
  KEY `idx_status` (`status`),
  KEY `idx_end_date` (`end_date`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_cl_copyright` FOREIGN KEY (`copyright_id`) REFERENCES `copyright_record` (`id`),
  -- CONSTRAINT `fk_cl_licensor` FOREIGN KEY (`licensor_id`) REFERENCES `tailor_is_user`.`sys_user` (`id`),
  -- CONSTRAINT `fk_cl_licensee` FOREIGN KEY (`licensee_id`) REFERENCES `tailor_is_user`.`sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='版权授权记录表';

-- ============================================================
-- 5. 版权投诉申诉表 (copyright_appeal)
-- ============================================================
DROP TABLE IF EXISTS `copyright_appeal`;
CREATE TABLE `copyright_appeal` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '申诉ID（主键）',
  `appeal_no` VARCHAR(64) NOT NULL COMMENT '申诉编号（平台唯一）',
  `infringement_id` BIGINT UNSIGNED NOT NULL COMMENT '关联侵权举报ID',
  `appeal_user_id` BIGINT UNSIGNED NOT NULL COMMENT '申诉人用户ID',
  `appeal_type` TINYINT NOT NULL COMMENT '申诉类型：1-举报方申诉，2-被举报方申诉',
  `reason` VARCHAR(256) NOT NULL COMMENT '申诉原因',
  `description` TEXT DEFAULT NULL COMMENT '申诉详细说明',
  `evidence` JSON DEFAULT NULL COMMENT '申诉证据（JSON数组）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待审核，1-审核中，2-申诉成功，3-申诉驳回',
  `handler_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '处理人ID',
  `handler_remark` VARCHAR(512) DEFAULT NULL COMMENT '处理备注',
  `handled_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_appeal_no` (`appeal_no`),
  KEY `idx_infringement_id` (`infringement_id`),
  KEY `idx_appeal_user_id` (`appeal_user_id`),
  KEY `idx_appeal_type` (`appeal_type`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_ca_infringement` FOREIGN KEY (`infringement_id`) REFERENCES `infringement_record` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='版权投诉申诉表';
