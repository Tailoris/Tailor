-- ============================================================
-- TiDB Schema DDL for Order Service
-- ============================================================
-- 数据库: tailor_is_order (TiDB)
-- 说明: 使用 TiDB 特有优化特性，包括 AUTO_RANDOM、SHARD_ROW_ID_BITS、
--       PRE_SPLIT_REGIONS、RANGE 分区等，防止热点写入并优化查询性能。
-- ============================================================

-- ============================================================
-- 1. 订单信息表 (order_info)
-- ============================================================
CREATE TABLE IF NOT EXISTS `order_info` (
    `id` BIGINT NOT NULL AUTO_RANDOM COMMENT '订单ID（主键，AUTO_RANDOM 防热点）',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
    `parent_order_no` VARCHAR(64) DEFAULT NULL COMMENT '父订单编号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `shop_id` BIGINT NOT NULL COMMENT '店铺ID',
    `merchant_id` BIGINT NOT NULL COMMENT '商户ID',
    `product_type` INT DEFAULT NULL COMMENT '商品类型',
    `status` INT NOT NULL DEFAULT 0 COMMENT '订单状态',
    `total_amount` DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    `discount_amount` DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
    `coupon_amount` DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '优惠券金额',
    `points_amount` DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '积分抵扣金额',
    `freight_amount` DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '运费',
    `pay_amount` DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
    `pay_type` INT DEFAULT NULL COMMENT '支付方式',
    `pay_status` INT DEFAULT NULL COMMENT '支付状态',
    `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
    `address_snapshot` TEXT DEFAULT NULL COMMENT '地址快照(JSON)',
    `remark` VARCHAR(512) DEFAULT NULL COMMENT '用户备注',
    `seller_remark` VARCHAR(512) DEFAULT NULL COMMENT '商家备注',
    `invoice_type` INT DEFAULT NULL COMMENT '发票类型',
    `invoice_content` VARCHAR(256) DEFAULT NULL COMMENT '发票内容',
    `coupon_id` BIGINT DEFAULT NULL COMMENT '优惠券ID',
    `points_used` INT DEFAULT 0 COMMENT '使用积分',
    `logistics_no` VARCHAR(128) DEFAULT NULL COMMENT '物流单号',
    `ship_time` DATETIME DEFAULT NULL COMMENT '发货时间',
    `cancel_reason` VARCHAR(512) DEFAULT NULL COMMENT '取消原因',
    `cancel_time` DATETIME DEFAULT NULL COMMENT '取消时间',
    `confirm_receive_time` DATETIME DEFAULT NULL COMMENT '确认收货时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记(0:未删除,1:已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_merchant_id` (`merchant_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`created_at`),
    KEY `idx_merchant_status` (`merchant_id`, `status`),
    KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
/*!90000 SHARD_ROW_ID_BITS=4 PRE_SPLIT_REGIONS=4 */
PARTITION BY RANGE (TO_DAYS(`created_at`)) (
    PARTITION p202401 VALUES LESS THAN (TO_DAYS('2024-07-01')),
    PARTITION p202402 VALUES LESS THAN (TO_DAYS('2025-01-01')),
    PARTITION p202501 VALUES LESS THAN (TO_DAYS('2025-07-01')),
    PARTITION p202502 VALUES LESS THAN (TO_DAYS('2026-01-01')),
    PARTITION p202601 VALUES LESS THAN (TO_DAYS('2026-07-01')),
    PARTITION p202602 VALUES LESS THAN (TO_DAYS('2027-01-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- ============================================================
-- 2. 订单明细表 (order_item)
-- ============================================================
CREATE TABLE IF NOT EXISTS `order_item` (
    `id` BIGINT NOT NULL AUTO_RANDOM COMMENT '订单明细ID（主键，AUTO_RANDOM 防热点）',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `sku_id` BIGINT NOT NULL COMMENT 'SKU ID',
    `product_name` VARCHAR(256) NOT NULL COMMENT '商品名称',
    `product_image` VARCHAR(512) DEFAULT NULL COMMENT '商品图片',
    `sku_attributes` VARCHAR(512) DEFAULT NULL COMMENT 'SKU属性(JSON)',
    `sku_attribute_text` VARCHAR(256) DEFAULT NULL COMMENT 'SKU属性文本',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '购买数量',
    `price` DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '单价',
    `subtotal` DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '小计',
    `discount_amount` DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
    `pay_amount` DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
    `is_commented` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已评价(0:未评价,1:已评价)',
    `after_sale_status` INT DEFAULT NULL COMMENT '售后状态',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记(0:未删除,1:已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
/*!90000 SHARD_ROW_ID_BITS=4 PRE_SPLIT_REGIONS=4 */
PARTITION BY RANGE (TO_DAYS(`created_at`)) (
    PARTITION p202401 VALUES LESS THAN (TO_DAYS('2024-07-01')),
    PARTITION p202402 VALUES LESS THAN (TO_DAYS('2025-01-01')),
    PARTITION p202501 VALUES LESS THAN (TO_DAYS('2025-07-01')),
    PARTITION p202502 VALUES LESS THAN (TO_DAYS('2026-01-01')),
    PARTITION p202601 VALUES LESS THAN (TO_DAYS('2026-07-01')),
    PARTITION p202602 VALUES LESS THAN (TO_DAYS('2027-01-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);