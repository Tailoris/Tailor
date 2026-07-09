-- ============================================================
-- Tailor IS - Flyway Migration V1: 订单系统数据库初始化
-- 数据库: tailor_is_order
-- 说明: 基线迁移，创建订单、购物车、售后等核心表
-- ============================================================

CREATE TABLE IF NOT EXISTS `shopping_cart` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
  `sku_id` BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
  `product_name` VARCHAR(256) DEFAULT NULL COMMENT '商品名称',
  `product_image` VARCHAR(512) DEFAULT NULL COMMENT '商品图片',
  `sku_attributes` JSON DEFAULT NULL COMMENT 'SKU属性',
  `quantity` INT NOT NULL DEFAULT 1 COMMENT '数量',
  `price` DECIMAL(10, 2) NOT NULL COMMENT '单价',
  `checked` TINYINT NOT NULL DEFAULT 1 COMMENT '是否选中',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_sku` (`user_id`, `sku_id`, `deleted`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='购物车表';

CREATE TABLE IF NOT EXISTS `order_info` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `shop_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '店铺ID',
  `merchant_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '商家ID',
  `product_type` TINYINT DEFAULT 1 COMMENT '商品类型',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态',
  `total_amount` DECIMAL(10, 2) NOT NULL COMMENT '订单总金额',
  `discount_amount` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '优惠金额',
  `pay_amount` DECIMAL(10, 2) NOT NULL COMMENT '实付金额',
  `pay_type` TINYINT DEFAULT NULL COMMENT '支付方式',
  `pay_status` TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态',
  `address_snapshot` TEXT DEFAULT NULL COMMENT '地址快照',
  `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `paid_at` DATETIME DEFAULT NULL COMMENT '支付时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单表';

CREATE TABLE IF NOT EXISTS `order_item` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
  `sku_id` BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
  `product_name` VARCHAR(256) DEFAULT NULL COMMENT '商品名称',
  `product_image` VARCHAR(512) DEFAULT NULL COMMENT '商品图片',
  `sku_attributes` JSON DEFAULT NULL COMMENT 'SKU属性',
  `quantity` INT NOT NULL COMMENT '数量',
  `price` DECIMAL(10, 2) NOT NULL COMMENT '单价',
  `subtotal` DECIMAL(10, 2) NOT NULL COMMENT '小计',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单明细表';

CREATE TABLE IF NOT EXISTS `order_logistics` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  `logistics_no` VARCHAR(64) DEFAULT NULL COMMENT '物流单号',
  `logistics_company` VARCHAR(128) DEFAULT NULL COMMENT '物流公司',
  `status` TINYINT DEFAULT 0 COMMENT '物流状态',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单物流表';

CREATE TABLE IF NOT EXISTS `after_sale_ticket` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `type` TINYINT NOT NULL COMMENT '售后类型',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '售后状态',
  `reason` VARCHAR(512) DEFAULT NULL COMMENT '原因',
  `refund_amount` DECIMAL(10, 2) DEFAULT NULL COMMENT '退款金额',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='售后工单表';