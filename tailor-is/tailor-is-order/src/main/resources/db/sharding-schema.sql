-- ============================================================
-- ShardingSphere 分片表 DDL for Order Service
-- ============================================================
-- 数据库: tailor_is_order (MySQL)
-- 说明: 配合 ShardingSphere 分库分表使用，按 merchant_id 分片
--       分为 4 个分片表 (t_order_0 ~ t_order_3, t_order_item_0 ~ t_order_item_3)
--       分片键 merchant_id 必须为 NOT NULL
-- 执行方式: 在 MySQL 中手动执行此脚本，或通过 flyway/liquibase 管理
-- ============================================================

-- ============================================================
-- 订单信息表分片 (t_order_0 ~ t_order_3)
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_order_0` (
    `id` BIGINT NOT NULL COMMENT '订单ID（主键，由 ShardingSphere Snowflake 生成）',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
    `parent_order_no` VARCHAR(64) DEFAULT NULL COMMENT '父订单编号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `shop_id` BIGINT NOT NULL COMMENT '店铺ID',
    `merchant_id` BIGINT NOT NULL COMMENT '商户ID（分片键）',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单信息表-分片0';

CREATE TABLE IF NOT EXISTS `t_order_1` LIKE `t_order_0`;
CREATE TABLE IF NOT EXISTS `t_order_2` LIKE `t_order_0`;
CREATE TABLE IF NOT EXISTS `t_order_3` LIKE `t_order_0`;

-- ============================================================
-- 订单明细表分片 (t_order_item_0 ~ t_order_item_3)
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_order_item_0` (
    `id` BIGINT NOT NULL COMMENT '订单明细ID（主键，由 ShardingSphere Snowflake 生成）',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单明细表-分片0';

CREATE TABLE IF NOT EXISTS `t_order_item_1` LIKE `t_order_item_0`;
CREATE TABLE IF NOT EXISTS `t_order_item_2` LIKE `t_order_item_0`;
CREATE TABLE IF NOT EXISTS `t_order_item_3` LIKE `t_order_item_0`;

-- ============================================================
-- 购物车表分片 (t_shopping_cart_0 ~ t_shopping_cart_3)
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_shopping_cart_0` (
    `id` BIGINT NOT NULL COMMENT '购物车ID（主键）',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `merchant_id` BIGINT NOT NULL COMMENT '商户ID（分片键）',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `sku_id` BIGINT NOT NULL COMMENT 'SKU ID',
    `product_name` VARCHAR(256) DEFAULT NULL COMMENT '商品名称',
    `product_image` VARCHAR(512) DEFAULT NULL COMMENT '商品图片',
    `sku_attributes` VARCHAR(512) DEFAULT NULL COMMENT 'SKU属性(JSON)',
    `sku_attribute_text` VARCHAR(256) DEFAULT NULL COMMENT 'SKU属性文本',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '数量',
    `price` DECIMAL(12, 2) DEFAULT NULL COMMENT '单价',
    `selected` TINYINT NOT NULL DEFAULT 1 COMMENT '是否选中(0:未选中,1:已选中)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_merchant_id` (`merchant_id`),
    KEY `idx_user_merchant` (`user_id`, `merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='购物车表-分片0';

CREATE TABLE IF NOT EXISTS `t_shopping_cart_1` LIKE `t_shopping_cart_0`;
CREATE TABLE IF NOT EXISTS `t_shopping_cart_2` LIKE `t_shopping_cart_0`;
CREATE TABLE IF NOT EXISTS `t_shopping_cart_3` LIKE `t_shopping_cart_0`;

-- ============================================================
-- 售后工单表分片 (t_after_sale_ticket_0 ~ t_after_sale_ticket_3)
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_after_sale_ticket_0` (
    `id` BIGINT NOT NULL COMMENT '工单ID（主键）',
    `ticket_no` VARCHAR(64) NOT NULL COMMENT '工单编号',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
    `order_item_id` BIGINT DEFAULT NULL COMMENT '订单明细ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `merchant_id` BIGINT NOT NULL COMMENT '商户ID（分片键）',
    `shop_id` BIGINT NOT NULL COMMENT '店铺ID',
    `type` INT NOT NULL COMMENT '售后类型(1:退款,2:退货退款,3:换货)',
    `reason` VARCHAR(512) DEFAULT NULL COMMENT '售后原因',
    `images` TEXT DEFAULT NULL COMMENT '凭证图片(JSON)',
    `refund_amount` DECIMAL(12, 2) DEFAULT 0.00 COMMENT '退款金额',
    `status` INT NOT NULL DEFAULT 0 COMMENT '工单状态',
    `apply_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `process_time` DATETIME DEFAULT NULL COMMENT '处理时间',
    `process_by` BIGINT DEFAULT NULL COMMENT '处理人',
    `process_remark` VARCHAR(512) DEFAULT NULL COMMENT '处理备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (`id`),
    KEY `idx_ticket_no` (`ticket_no`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_merchant_id` (`merchant_id`),
    KEY `idx_status` (`status`),
    KEY `idx_apply_time` (`apply_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='售后工单表-分片0';

CREATE TABLE IF NOT EXISTS `t_after_sale_ticket_1` LIKE `t_after_sale_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_after_sale_ticket_2` LIKE `t_after_sale_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_after_sale_ticket_3` LIKE `t_after_sale_ticket_0`;