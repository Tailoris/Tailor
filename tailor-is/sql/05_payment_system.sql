-- ============================================================
-- Tailor IS 平台 - 支付系统数据库表结构
-- 文件: 05_payment_system.sql
-- 版本: 1.0.0
-- 数据库: MySQL 8.0
-- 创建时间: 2026-05-29
-- ============================================================

-- 创建数据库（如不存在）
CREATE DATABASE IF NOT EXISTS `tailor_is_payment` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `tailor_is_payment`;

-- ============================================================
-- 1. 支付记录表 (payment_record)
-- ============================================================
DROP TABLE IF EXISTS `payment_record`;
CREATE TABLE `payment_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '支付记录ID（主键）',
  `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号（冗余字段）',
  `payment_no` VARCHAR(64) NOT NULL COMMENT '支付流水号（平台唯一）',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '支付用户ID',
  `amount` DECIMAL(12, 2) NOT NULL COMMENT '支付金额（元）',
  `pay_channel` TINYINT NOT NULL COMMENT '支付渠道：1-微信支付，2-支付宝，3-银行卡，4-余额支付，5-Apple Pay，6-银联',
  `pay_method` VARCHAR(32) DEFAULT NULL COMMENT '支付方式明细：如 wx_app/wx_jsapi/alipay_pc 等',
  `pay_status` TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0-待支付，1-支付中，2-支付成功，3-支付失败，4-已关闭',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付成功时间',
  `expire_time` DATETIME DEFAULT NULL COMMENT '支付超时时间',
  `transaction_id` VARCHAR(128) DEFAULT NULL COMMENT '第三方交易流水号',
  `channel_request` JSON DEFAULT NULL COMMENT '请求渠道参数快照（JSON）',
  `channel_response` JSON DEFAULT NULL COMMENT '渠道返回参数快照（JSON）',
  `notify_url` VARCHAR(512) DEFAULT NULL COMMENT '异步回调地址',
  `notify_status` TINYINT DEFAULT 0 COMMENT '回调状态：0-未回调，1-回调成功，2-回调失败',
  `notify_time` DATETIME DEFAULT NULL COMMENT '回调时间',
  `client_ip` VARCHAR(45) DEFAULT NULL COMMENT '客户端IP',
  `device_type` TINYINT DEFAULT 0 COMMENT '设备类型：0-未知，1-PC，2-H5，3-小程序，4-APP',
  `remark` VARCHAR(256) DEFAULT NULL COMMENT '支付备注',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  UNIQUE KEY `uk_transaction_id` (`transaction_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_pay_channel` (`pay_channel`),
  KEY `idx_pay_status` (`pay_status`),
  KEY `idx_pay_time` (`pay_time`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_pay_order` FOREIGN KEY (`order_id`) REFERENCES `tailor_is_order`.`order_info` (`id`),
  -- CONSTRAINT `fk_pay_user` FOREIGN KEY (`user_id`) REFERENCES `tailor_is_user`.`sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='支付记录表';

-- ============================================================
-- 2. 退款记录表 (refund_record)
-- ============================================================
DROP TABLE IF EXISTS `refund_record`;
CREATE TABLE `refund_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '退款记录ID（主键）',
  `ticket_id` BIGINT UNSIGNED NOT NULL COMMENT '售后工单ID',
  `ticket_no` VARCHAR(64) NOT NULL COMMENT '售后工单编号',
  `refund_no` VARCHAR(64) NOT NULL COMMENT '退款流水号（平台唯一）',
  `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
  `order_item_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '订单明细ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '退款用户ID',
  `merchant_id` BIGINT UNSIGNED NOT NULL COMMENT '商家ID',
  `amount` DECIMAL(12, 2) NOT NULL COMMENT '退款金额（元）',
  `refund_channel` TINYINT NOT NULL COMMENT '退款渠道：1-微信，2-支付宝，3-银行卡，4-余额',
  `refund_status` TINYINT NOT NULL DEFAULT 0 COMMENT '退款状态：0-待退款，1-退款中，2-退款成功，3-退款失败，4-已关闭',
  `refund_time` DATETIME DEFAULT NULL COMMENT '退款成功时间',
  `channel_refund_no` VARCHAR(128) DEFAULT NULL COMMENT '渠道退款流水号',
  `channel_request` JSON DEFAULT NULL COMMENT '请求渠道参数快照',
  `channel_response` JSON DEFAULT NULL COMMENT '渠道返回参数快照',
  `fail_reason` VARCHAR(256) DEFAULT NULL COMMENT '退款失败原因',
  `retry_count` INT DEFAULT 0 COMMENT '重试次数',
  `remark` VARCHAR(256) DEFAULT NULL COMMENT '退款备注',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  UNIQUE KEY `uk_channel_refund_no` (`channel_refund_no`),
  KEY `idx_ticket_id` (`ticket_id`),
  KEY `idx_ticket_no` (`ticket_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_refund_channel` (`refund_channel`),
  KEY `idx_refund_status` (`refund_status`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_refund_ticket` FOREIGN KEY (`ticket_id`) REFERENCES `tailor_is_order`.`after_sale_ticket` (`id`),
  -- CONSTRAINT `fk_refund_order` FOREIGN KEY (`order_id`) REFERENCES `tailor_is_order`.`order_info` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='退款记录表';

-- ============================================================
-- 3. 结算记录表 (settlement_record)
-- ============================================================
DROP TABLE IF EXISTS `settlement_record`;
CREATE TABLE `settlement_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '结算记录ID（主键）',
  `settlement_no` VARCHAR(64) NOT NULL COMMENT '结算单号（平台唯一）',
  `merchant_id` BIGINT UNSIGNED NOT NULL COMMENT '商家ID',
  `shop_id` BIGINT UNSIGNED NOT NULL COMMENT '店铺ID',
  `order_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '订单ID（可为空，表示批量结算）',
  `order_no` VARCHAR(64) DEFAULT NULL COMMENT '订单编号',
  `settlement_type` TINYINT NOT NULL DEFAULT 1 COMMENT '结算类型：1-订单结算，2-提现结算，3-手动结算，4-批量结算',
  `order_amount` DECIMAL(12, 2) NOT NULL COMMENT '订单金额（元）',
  `platform_fee` DECIMAL(12, 2) NOT NULL COMMENT '平台服务费/佣金（元）',
  `platform_fee_rate` DECIMAL(5, 4) NOT NULL COMMENT '平台费率（如 0.0500 表示 5%）',
  `coupon_subsidy` DECIMAL(12, 2) DEFAULT 0.00 COMMENT '优惠券补贴金额（元）',
  `merchant_amount` DECIMAL(12, 2) NOT NULL COMMENT '商家应得金额（元）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '结算状态：0-待结算，1-结算中，2-已结算，3-结算失败',
  `settled_at` DATETIME DEFAULT NULL COMMENT '结算完成时间',
  `settlement_cycle` TINYINT DEFAULT 1 COMMENT '结算周期：1-T+1，2-T+7，3-T+15，4-月结',
  `remark` VARCHAR(256) DEFAULT NULL COMMENT '结算备注',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_settlement_no` (`settlement_no`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_settlement_type` (`settlement_type`),
  KEY `idx_status` (`status`),
  KEY `idx_settled_at` (`settled_at`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_settle_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `tailor_is_merchant`.`merchant` (`id`),
  -- CONSTRAINT `fk_settle_shop` FOREIGN KEY (`shop_id`) REFERENCES `tailor_is_merchant`.`merchant_shop` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='结算记录表';

-- ============================================================
-- 4. 商家账户表 (merchant_account)
-- ============================================================
DROP TABLE IF EXISTS `merchant_account`;
CREATE TABLE `merchant_account` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '账户ID（主键）',
  `merchant_id` BIGINT UNSIGNED NOT NULL COMMENT '商家ID',
  `balance` DECIMAL(14, 2) NOT NULL DEFAULT 0.00 COMMENT '账户余额（元，可提现）',
  `withdrawable_balance` DECIMAL(14, 2) NOT NULL DEFAULT 0.00 COMMENT '可提现余额（元）',
  `frozen_amount` DECIMAL(14, 2) NOT NULL DEFAULT 0.00 COMMENT '冻结金额（元）',
  `pending_amount` DECIMAL(14, 2) NOT NULL DEFAULT 0.00 COMMENT '待结算金额（元）',
  `total_income` DECIMAL(14, 2) NOT NULL DEFAULT 0.00 COMMENT '累计收入（元）',
  `total_expense` DECIMAL(14, 2) NOT NULL DEFAULT 0.00 COMMENT '累计支出（元）',
  `total_withdraw` DECIMAL(14, 2) NOT NULL DEFAULT 0.00 COMMENT '累计提现（元）',
  `total_settlement` DECIMAL(14, 2) NOT NULL DEFAULT 0.00 COMMENT '累计结算（元）',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号（乐观锁）',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_id` (`merchant_id`),
  KEY `idx_balance` (`balance`),
  KEY `idx_frozen_amount` (`frozen_amount`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_maccount_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `tailor_is_merchant`.`merchant` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商家账户表';

-- ============================================================
-- 5. 提现记录表 (withdraw_record)
-- ============================================================
DROP TABLE IF EXISTS `withdraw_record`;
CREATE TABLE `withdraw_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '提现记录ID（主键）',
  `withdraw_no` VARCHAR(64) NOT NULL COMMENT '提现单号（平台唯一）',
  `merchant_id` BIGINT UNSIGNED NOT NULL COMMENT '商家ID',
  `amount` DECIMAL(12, 2) NOT NULL COMMENT '提现金额（元）',
  `fee` DECIMAL(12, 2) DEFAULT 0.00 COMMENT '提现手续费（元）',
  `actual_amount` DECIMAL(12, 2) NOT NULL COMMENT '实际到账金额（元）',
  `bank_name` VARCHAR(64) NOT NULL COMMENT '开户银行名称',
  `bank_branch` VARCHAR(128) DEFAULT NULL COMMENT '开户支行',
  `bank_account` VARCHAR(64) NOT NULL COMMENT '银行账号',
  `account_name` VARCHAR(64) NOT NULL COMMENT '开户人姓名',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '提现状态：0-待审核，1-审核通过/提现中，2-提现成功，3-提现失败，4-已驳回',
  `fail_reason` VARCHAR(256) DEFAULT NULL COMMENT '失败原因',
  `channel_transaction_id` VARCHAR(128) DEFAULT NULL COMMENT '渠道交易流水号',
  `audit_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '审核人ID',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `audit_remark` VARCHAR(256) DEFAULT NULL COMMENT '审核备注',
  `processed_at` DATETIME DEFAULT NULL COMMENT '处理完成时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_withdraw_no` (`withdraw_no`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_status` (`status`),
  KEY `idx_bank_account` (`bank_account`),
  KEY `idx_created_at` (`created_at`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_withdraw_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `tailor_is_merchant`.`merchant` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商家提现记录表';

-- ============================================================
-- 6. 用户账户表 (user_account)
-- ============================================================
DROP TABLE IF EXISTS `user_account`;
CREATE TABLE `user_account` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '账户ID（主键）',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `balance` DECIMAL(14, 2) NOT NULL DEFAULT 0.00 COMMENT '账户余额（元）',
  `frozen_amount` DECIMAL(14, 2) NOT NULL DEFAULT 0.00 COMMENT '冻结金额（元）',
  `total_recharge` DECIMAL(14, 2) NOT NULL DEFAULT 0.00 COMMENT '累计充值（元）',
  `total_consume` DECIMAL(14, 2) NOT NULL DEFAULT 0.00 COMMENT '累计消费（元）',
  `total_refund` DECIMAL(14, 2) NOT NULL DEFAULT 0.00 COMMENT '累计退款退回（元）',
  `points` INT NOT NULL DEFAULT 0 COMMENT '当前积分',
  `total_points_earned` INT NOT NULL DEFAULT 0 COMMENT '累计获得积分',
  `total_points_spent` INT NOT NULL DEFAULT 0 COMMENT '累计消耗积分',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号（乐观锁）',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_balance` (`balance`),
  KEY `idx_points` (`points`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_uaccount_user` FOREIGN KEY (`user_id`) REFERENCES `tailor_is_user`.`sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户账户表';

-- ============================================================
-- 7. 充值记录表 (recharge_record)
-- ============================================================
DROP TABLE IF EXISTS `recharge_record`;
CREATE TABLE `recharge_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '充值记录ID（主键）',
  `recharge_no` VARCHAR(64) NOT NULL COMMENT '充值单号（平台唯一）',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '充值用户ID',
  `amount` DECIMAL(12, 2) NOT NULL COMMENT '充值金额（元）',
  `bonus_amount` DECIMAL(12, 2) DEFAULT 0.00 COMMENT '赠送金额（元）',
  `pay_channel` TINYINT NOT NULL COMMENT '支付渠道：1-微信支付，2-支付宝，3-银行卡',
  `pay_status` TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0-待支付，1-支付中，2-支付成功，3-支付失败',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付成功时间',
  `transaction_id` VARCHAR(128) DEFAULT NULL COMMENT '第三方交易流水号',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '充值状态：0-待充值，1-充值中，2-充值成功，3-充值失败，4-已关闭',
  `remark` VARCHAR(256) DEFAULT NULL COMMENT '充值备注',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_recharge_no` (`recharge_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_pay_channel` (`pay_channel`),
  KEY `idx_pay_status` (`pay_status`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_recharge_user` FOREIGN KEY (`user_id`) REFERENCES `tailor_is_user`.`sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户充值记录表';

-- ============================================================
-- 8. 质保金表 (quality_deposit)
-- ============================================================
DROP TABLE IF EXISTS `quality_deposit`;
CREATE TABLE `quality_deposit` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '质保金记录ID（主键）',
  `merchant_id` BIGINT UNSIGNED NOT NULL COMMENT '商家ID',
  `deposit_amount` DECIMAL(14, 2) NOT NULL DEFAULT 0.00 COMMENT '质保金总额（元）',
  `frozen_amount` DECIMAL(14, 2) NOT NULL DEFAULT 0.00 COMMENT '冻结金额（元，用于赔付）',
  `available_amount` DECIMAL(14, 2) NOT NULL DEFAULT 0.00 COMMENT '可用余额（元）',
  `min_deposit` DECIMAL(14, 2) NOT NULL DEFAULT 0.00 COMMENT '最低质保金要求（元）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-未缴纳，1-已缴纳，2-不足，3-已退还',
  `pay_time` DATETIME DEFAULT NULL COMMENT '最近缴纳时间',
  `refund_time` DATETIME DEFAULT NULL COMMENT '退还时间',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号（乐观锁）',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_id` (`merchant_id`),
  KEY `idx_status` (`status`),
  KEY `idx_deposit_amount` (`deposit_amount`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_deposit_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `tailor_is_merchant`.`merchant` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商家质保金表';

-- ============================================================
-- 9. 账户流水表 (account_transaction)
-- ============================================================
DROP TABLE IF EXISTS `account_transaction`;
CREATE TABLE `account_transaction` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '流水ID（主键）',
  `transaction_no` VARCHAR(64) NOT NULL COMMENT '流水号（平台唯一）',
  `account_type` TINYINT NOT NULL COMMENT '账户类型：1-用户账户，2-商家账户',
  `account_id` BIGINT UNSIGNED NOT NULL COMMENT '账户ID（user_id 或 merchant_id）',
  `related_type` TINYINT NOT NULL COMMENT '关联业务类型：1-订单支付，2-退款，3-充值，4-提现，5-结算，6-佣金，7-赔付，8-罚款，9-质保金',
  `related_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联业务ID',
  `related_no` VARCHAR(64) DEFAULT NULL COMMENT '关联业务编号',
  `amount` DECIMAL(14, 2) NOT NULL COMMENT '变动金额（正数为收入，负数为支出）',
  `balance_before` DECIMAL(14, 2) NOT NULL COMMENT '变动前余额',
  `balance_after` DECIMAL(14, 2) NOT NULL COMMENT '变动后余额',
  `direction` TINYINT NOT NULL COMMENT '资金方向：1-收入，2-支出',
  `remark` VARCHAR(256) DEFAULT NULL COMMENT '流水备注说明',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_transaction_no` (`transaction_no`),
  KEY `idx_account_type` (`account_type`),
  KEY `idx_account_id` (`account_id`),
  KEY `idx_related_type` (`related_type`),
  KEY `idx_related_id` (`related_id`),
  KEY `idx_direction` (`direction`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='账户资金流水表';
