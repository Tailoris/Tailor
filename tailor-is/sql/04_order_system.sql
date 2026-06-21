-- ============================================================
-- Tailor IS 平台 - 订单系统数据库表结构
-- 文件: 04_order_system.sql
-- 版本: 1.0.0
-- 数据库: MySQL 8.0
-- 创建时间: 2026-05-29
-- ============================================================

-- ⚠️ 警告：此脚本仅用于初始化部署，请勿在生产环境执行 DROP TABLE 操作
-- 生产环境数据库变更请使用 Flyway/Liquibase 版本化迁移工具管理

-- 创建数据库（如不存在）
CREATE DATABASE IF NOT EXISTS `tailor_is_order` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `tailor_is_order`;

-- ============================================================
-- 1. 购物车表 (shopping_cart)
-- ============================================================
CREATE TABLE IF NOT EXISTS `shopping_cart` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '购物车ID（主键）',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
  `sku_id` BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
  `shop_id` BIGINT UNSIGNED NOT NULL COMMENT '店铺ID',
  `merchant_id` BIGINT UNSIGNED NOT NULL COMMENT '商家ID',
  `quantity` INT NOT NULL DEFAULT 1 COMMENT '购买数量',
  `checked` TINYINT NOT NULL DEFAULT 1 COMMENT '是否选中：0-未选中，1-选中',
  `price_snapshot` DECIMAL(12, 2) DEFAULT NULL COMMENT '加入购物车时的价格快照',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_sku_id` (`sku_id`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_checked` (`checked`),
  UNIQUE KEY `uk_user_sku` (`user_id`, `sku_id`)
  -- 外键约束保持禁用，原因：
  -- 1. 跨库引用：user_id 指向 tailor_is_user.sys_user，product_id/sku_id 指向 tailor_is_product，MySQL 不支持跨库外键
  -- 2. 分库分表：本表已按 merchant_id 取模分片（t_shopping_cart_0~3，见 10_sharding_migration.sql），
  --    ShardingSphere 分布式环境下外键约束不被支持且性能开销大
  -- 引用完整性由应用层（OrderService/CartService）保证
  -- CONSTRAINT `fk_cart_user` FOREIGN KEY (`user_id`) REFERENCES `tailor_is_user`.`sys_user` (`id`),
  -- CONSTRAINT `fk_cart_product` FOREIGN KEY (`product_id`) REFERENCES `tailor_is_product`.`product` (`id`),
  -- CONSTRAINT `fk_cart_sku` FOREIGN KEY (`sku_id`) REFERENCES `tailor_is_product`.`product_sku` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='购物车表';

-- ============================================================
-- 2. 订单表 (order_info)
-- ============================================================
CREATE TABLE IF NOT EXISTS `order_info` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单ID（主键）',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号（业务唯一）',
  `parent_order_no` VARCHAR(64) DEFAULT NULL COMMENT '父订单编号（拆单场景）',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '下单用户ID',
  `shop_id` BIGINT UNSIGNED NOT NULL COMMENT '店铺ID',
  `merchant_id` BIGINT UNSIGNED NOT NULL COMMENT '商家ID',
  `product_type` TINYINT NOT NULL DEFAULT 1 COMMENT '商品类型：1-实物商品，2-虚拟商品，3-服务商品，4-定制商品',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0-待付款，1-已付款/待发货，2-已发货/待收货，3-已收货/待评价，4-已完成，5-已取消，6-已关闭，7-退款中，8-已退款',
  `total_amount` DECIMAL(12, 2) NOT NULL COMMENT '订单总金额（元）',
  `discount_amount` DECIMAL(12, 2) DEFAULT 0.00 COMMENT '优惠金额（元）',
  `coupon_amount` DECIMAL(12, 2) DEFAULT 0.00 COMMENT '优惠券抵扣金额（元）',
  `points_amount` DECIMAL(12, 2) DEFAULT 0.00 COMMENT '积分抵扣金额（元）',
  `freight_amount` DECIMAL(12, 2) DEFAULT 0.00 COMMENT '运费金额（元）',
  `pay_amount` DECIMAL(12, 2) NOT NULL COMMENT '实际应付金额（元）',
  `pay_type` TINYINT DEFAULT NULL COMMENT '支付方式：1-微信支付，2-支付宝，3-银行卡，4-余额支付',
  `pay_status` TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0-未支付，1-已支付，2-已退款，3-部分退款',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `expire_time` DATETIME DEFAULT NULL COMMENT '订单超时自动取消时间',
  `address_snapshot` JSON DEFAULT NULL COMMENT '收货地址快照（JSON格式）',
  `remark` VARCHAR(512) DEFAULT NULL COMMENT '买家备注',
  `seller_remark` VARCHAR(512) DEFAULT NULL COMMENT '卖家备注',
  `invoice_type` TINYINT DEFAULT 0 COMMENT '发票类型：0-不需要，1-电子发票，2-纸质发票',
  `invoice_content` VARCHAR(256) DEFAULT NULL COMMENT '发票内容',
  `coupon_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '使用的优惠券ID',
  `points_used` INT DEFAULT 0 COMMENT '使用的积分数量',
  `cancel_reason` VARCHAR(256) DEFAULT NULL COMMENT '取消原因',
  `cancel_time` DATETIME DEFAULT NULL COMMENT '取消时间',
  `confirm_receive_time` DATETIME DEFAULT NULL COMMENT '确认收货时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_status` (`status`),
  KEY `idx_pay_status` (`pay_status`),
  KEY `idx_product_type` (`product_type`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_expire_time` (`expire_time`)
  -- 外键约束保持禁用，原因：
  -- 1. 跨库引用：user_id 指向 tailor_is_user.sys_user，MySQL 不支持跨库外键
  -- 2. 分库分表：本表已按 merchant_id 取模分片（t_order_0~3，见 10_sharding_migration.sql），
  --    ShardingSphere 分布式环境下外键约束不被支持且性能开销大
  -- 引用完整性由应用层（OrderService）保证
  -- CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `tailor_is_user`.`sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单信息表';

-- ============================================================
-- 3. 订单明细表 (order_item)
-- ============================================================
CREATE TABLE IF NOT EXISTS `order_item` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单明细ID（主键）',
  `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号（冗余字段，方便查询）',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
  `sku_id` BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
  `product_name` VARCHAR(256) NOT NULL COMMENT '商品名称（快照）',
  `product_image` VARCHAR(512) DEFAULT NULL COMMENT '商品主图（快照）',
  `sku_attributes` JSON DEFAULT NULL COMMENT 'SKU属性快照（JSON格式）',
  `sku_attribute_text` VARCHAR(256) DEFAULT NULL COMMENT 'SKU属性描述文本（快照）',
  `quantity` INT NOT NULL COMMENT '购买数量',
  `price` DECIMAL(12, 2) NOT NULL COMMENT '商品单价（元）',
  `subtotal` DECIMAL(12, 2) NOT NULL COMMENT '小计金额（元）',
  `discount_amount` DECIMAL(12, 2) DEFAULT 0.00 COMMENT '分摊的优惠金额（元）',
  `pay_amount` DECIMAL(12, 2) NOT NULL COMMENT '实付金额（元）',
  `is_commented` TINYINT DEFAULT 0 COMMENT '是否已评价：0-否，1-是',
  `after_sale_status` TINYINT DEFAULT 0 COMMENT '售后状态：0-无售后，1-售后中，2-售后完成，3-售后关闭',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_sku_id` (`sku_id`),
  KEY `idx_after_sale_status` (`after_sale_status`)
  -- 外键约束保持禁用，原因：
  -- 分库分表：本表及 order_info 均已按 merchant_id 取模分片（t_order_item_0~3，见 10_sharding_migration.sql），
  -- ShardingSphere 分布式环境下外键约束不被支持且性能开销大
  -- 引用完整性由应用层（OrderService）保证
  -- CONSTRAINT `fk_item_order` FOREIGN KEY (`order_id`) REFERENCES `order_info` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单明细表';

-- ============================================================
-- 4. 物流信息表 (order_logistics)
-- ============================================================
CREATE TABLE IF NOT EXISTS `order_logistics` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '物流ID（主键）',
  `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号（冗余字段）',
  `logistics_company` VARCHAR(64) NOT NULL COMMENT '物流公司编码',
  `logistics_company_name` VARCHAR(128) DEFAULT NULL COMMENT '物流公司名称',
  `logistics_no` VARCHAR(64) NOT NULL COMMENT '物流单号',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '物流状态：0-待发货，1-已发货，2-运输中，3-派送中，4-已签收，5-拒签，6-异常',
  `shipped_at` DATETIME DEFAULT NULL COMMENT '发货时间',
  `delivered_at` DATETIME DEFAULT NULL COMMENT '签收时间',
  `logistics_info` JSON DEFAULT NULL COMMENT '物流轨迹信息（JSON格式）',
  `receiver_name` VARCHAR(64) DEFAULT NULL COMMENT '收货人姓名（快照）',
  `receiver_phone` VARCHAR(20) DEFAULT NULL COMMENT '收货人电话（快照）',
  `receiver_address` VARCHAR(512) DEFAULT NULL COMMENT '收货地址（快照）',
  `remark` VARCHAR(256) DEFAULT NULL COMMENT '物流备注',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_logistics_no` (`logistics_no`),
  KEY `idx_status` (`status`)
  -- 外键约束保持禁用，原因：
  -- 分库分表：order_info 已按 merchant_id 取模分片（见 10_sharding_migration.sql），
  -- ShardingSphere 分布式环境下外键约束不被支持且性能开销大
  -- 引用完整性由应用层（OrderService）保证
  -- CONSTRAINT `fk_logistics_order` FOREIGN KEY (`order_id`) REFERENCES `order_info` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单物流信息表';

-- ============================================================
-- 5. 售后工单表 (after_sale_ticket)
-- ============================================================
CREATE TABLE IF NOT EXISTS `after_sale_ticket` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '售后工单ID（主键）',
  `ticket_no` VARCHAR(64) NOT NULL COMMENT '售后工单编号',
  `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号（冗余字段）',
  `order_item_id` BIGINT UNSIGNED NOT NULL COMMENT '订单明细ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '申请用户ID',
  `merchant_id` BIGINT UNSIGNED NOT NULL COMMENT '商家ID',
  `shop_id` BIGINT UNSIGNED NOT NULL COMMENT '店铺ID',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
  `sku_id` BIGINT UNSIGNED DEFAULT NULL COMMENT 'SKU ID',
  `ticket_type` TINYINT NOT NULL COMMENT '售后类型：1-仅退款，2-退货退款，3-换货，4-补发，5-维修',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '售后状态：0-待商家处理，1-商家已同意，2-商家已拒绝，3-买家已退货，4-商家已收货，5-退款中，6-退款成功，7-退款失败，8-售后关闭，9-平台介入中，10-平台已处理',
  `reason` VARCHAR(128) NOT NULL COMMENT '售后原因',
  `description` VARCHAR(1000) DEFAULT NULL COMMENT '售后详细说明',
  `images` JSON DEFAULT NULL COMMENT '售后凭证图片（JSON数组）',
  `video_url` VARCHAR(512) DEFAULT NULL COMMENT '售后凭证视频URL',
  `refund_amount` DECIMAL(12, 2) NOT NULL COMMENT '申请退款金额（元）',
  `refund_quantity` INT DEFAULT 1 COMMENT '退货数量',
  `return_logistics_company` VARCHAR(64) DEFAULT NULL COMMENT '退货物流公司',
  `return_logistics_no` VARCHAR(64) DEFAULT NULL COMMENT '退货物流单号',
  `merchant_remark` VARCHAR(512) DEFAULT NULL COMMENT '商家处理备注',
  `merchant_handle_time` DATETIME DEFAULT NULL COMMENT '商家处理时间',
  `platform_intervene` TINYINT DEFAULT 0 COMMENT '是否平台介入：0-否，1-是',
  `platform_handler` BIGINT UNSIGNED DEFAULT NULL COMMENT '平台处理人ID',
  `platform_result` VARCHAR(512) DEFAULT NULL COMMENT '平台处理结果',
  `platform_handle_time` DATETIME DEFAULT NULL COMMENT '平台处理时间',
  `processed_at` DATETIME DEFAULT NULL COMMENT '售后完成时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ticket_no` (`ticket_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_ticket_type` (`ticket_type`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
  -- 外键约束保持禁用，原因：
  -- 1. 跨库引用：user_id 指向 tailor_is_user.sys_user，MySQL 不支持跨库外键
  -- 2. 分库分表：本表及 order_info 均已按 merchant_id 取模分片（t_after_sale_ticket_0~3，见 10_sharding_migration.sql），
  --    ShardingSphere 分布式环境下外键约束不被支持且性能开销大
  -- 引用完整性由应用层（AfterSaleService）保证
  -- CONSTRAINT `fk_ticket_order` FOREIGN KEY (`order_id`) REFERENCES `order_info` (`id`),
  -- CONSTRAINT `fk_ticket_user` FOREIGN KEY (`user_id`) REFERENCES `tailor_is_user`.`sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='售后工单表';
