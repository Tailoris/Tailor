-- ============================================================
-- Tailor IS 平台 - 营销系统数据库表结构
-- 文件: 06_marketing_system.sql
-- 版本: 1.0.0
-- 数据库: MySQL 8.0
-- 创建时间: 2026-05-29
-- ============================================================

-- 创建数据库（如不存在）
CREATE DATABASE IF NOT EXISTS `tailor_is_marketing` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `tailor_is_marketing`;

-- ============================================================
-- 1. 优惠券模板表 (coupon_template)
-- ============================================================
DROP TABLE IF EXISTS `coupon_template`;
CREATE TABLE `coupon_template` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '优惠券模板ID（主键）',
  `name` VARCHAR(128) NOT NULL COMMENT '优惠券名称',
  `type` TINYINT NOT NULL COMMENT '优惠券类型：1-满减券，2-折扣券，3-立减券，4-运费券',
  `discount_type` TINYINT NOT NULL COMMENT '优惠类型：1-固定金额，2-百分比折扣',
  `discount_value` DECIMAL(12, 2) NOT NULL COMMENT '优惠面额/折扣值（如：满100减20则为20.00；8折则为8.00）',
  `min_amount` DECIMAL(12, 2) DEFAULT 0.00 COMMENT '最低消费金额（元），0表示无门槛',
  `max_discount` DECIMAL(12, 2) DEFAULT NULL COMMENT '最大抵扣金额（用于折扣券封顶）',
  `total_count` INT NOT NULL COMMENT '发放总量（-1表示不限量）',
  `issued_count` INT NOT NULL DEFAULT 0 COMMENT '已发放数量',
  `received_count` INT NOT NULL DEFAULT 0 COMMENT '已领取数量',
  `used_count` INT NOT NULL DEFAULT 0 COMMENT '已使用数量',
  `per_limit` INT NOT NULL DEFAULT 1 COMMENT '每人限领数量（-1表示不限）',
  `scope_type` TINYINT NOT NULL DEFAULT 1 COMMENT '适用范围：1-全场通用，2-指定分类，3-指定商品，4-指定店铺',
  `scope_value` JSON DEFAULT NULL COMMENT '适用范围值（JSON数组：分类ID/商品ID/店铺ID）',
  `start_time` DATETIME NOT NULL COMMENT '生效开始时间',
  `end_time` DATETIME NOT NULL COMMENT '生效结束时间',
  `receive_start_time` DATETIME DEFAULT NULL COMMENT '可领取开始时间',
  `receive_end_time` DATETIME DEFAULT NULL COMMENT '可领取结束时间',
  `days_after_receive` INT DEFAULT NULL COMMENT '领取后有效天数（为空则使用固定时间）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-未开始，1-进行中，2-已结束，3-已停用',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '优惠券说明',
  `remark` VARCHAR(256) DEFAULT NULL COMMENT '备注',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_type` (`type`),
  KEY `idx_status` (`status`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_end_time` (`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='优惠券模板表';

-- ============================================================
-- 2. 用户优惠券表 (user_coupon)
-- ============================================================
DROP TABLE IF EXISTS `user_coupon`;
CREATE TABLE `user_coupon` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户优惠券ID（主键）',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `coupon_id` BIGINT UNSIGNED NOT NULL COMMENT '优惠券模板ID',
  `coupon_code` VARCHAR(64) NOT NULL COMMENT '优惠券码（唯一）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-未使用，1-已使用，2-已过期，3-已锁定（下单占用）',
  `used_time` DATETIME DEFAULT NULL COMMENT '使用时间',
  `order_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '使用的订单ID',
  `order_no` VARCHAR(64) DEFAULT NULL COMMENT '使用的订单编号',
  `valid_start_time` DATETIME NOT NULL COMMENT '有效期开始时间',
  `valid_end_time` DATETIME NOT NULL COMMENT '有效期结束时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coupon_code` (`coupon_code`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_coupon_id` (`coupon_id`),
  KEY `idx_status` (`status`),
  KEY `idx_valid_end_time` (`valid_end_time`),
  KEY `idx_order_id` (`order_id`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_ucoupon_user` FOREIGN KEY (`user_id`) REFERENCES `tailor_is_user`.`sys_user` (`id`),
  -- CONSTRAINT `fk_ucoupon_template` FOREIGN KEY (`coupon_id`) REFERENCES `coupon_template` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户优惠券表';

-- ============================================================
-- 3. 秒杀活动表 (seckill_activity)
-- ============================================================
DROP TABLE IF EXISTS `seckill_activity`;
CREATE TABLE `seckill_activity` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '秒杀活动ID（主键）',
  `name` VARCHAR(128) NOT NULL COMMENT '秒杀活动名称',
  `start_time` DATETIME NOT NULL COMMENT '活动开始时间',
  `end_time` DATETIME NOT NULL COMMENT '活动结束时间',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-未开始，1-进行中，2-已结束，3-已停用',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '活动描述',
  `banner_image` VARCHAR(512) DEFAULT NULL COMMENT '活动Banner图片',
  `sort` INT DEFAULT 0 COMMENT '排序权重',
  `product_count` INT DEFAULT 0 COMMENT '参与商品数量',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_end_time` (`end_time`),
  KEY `idx_status` (`status`),
  KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='秒杀活动表';

-- ============================================================
-- 4. 秒杀商品表 (seckill_product)
-- ============================================================
DROP TABLE IF EXISTS `seckill_product`;
CREATE TABLE `seckill_product` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '秒杀商品ID（主键）',
  `activity_id` BIGINT UNSIGNED NOT NULL COMMENT '秒杀活动ID',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
  `sku_id` BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
  `seckill_price` DECIMAL(12, 2) NOT NULL COMMENT '秒杀价格（元）',
  `original_price` DECIMAL(12, 2) NOT NULL COMMENT '原价/日常价（元）',
  `stock` INT NOT NULL COMMENT '秒杀库存',
  `available_stock` INT NOT NULL COMMENT '剩余可用库存',
  `limit_count` INT DEFAULT 1 COMMENT '每人限购数量',
  `sort` INT DEFAULT 0 COMMENT '排序权重',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-下架，1-上架',
  `order_count` INT DEFAULT 0 COMMENT '订单数',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_sku_id` (`sku_id`),
  KEY `idx_status` (`status`),
  KEY `idx_sort` (`sort`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_sp_activity` FOREIGN KEY (`activity_id`) REFERENCES `seckill_activity` (`id`),
  -- CONSTRAINT `fk_sp_product` FOREIGN KEY (`product_id`) REFERENCES `tailor_is_product`.`product` (`id`),
  -- CONSTRAINT `fk_sp_sku` FOREIGN KEY (`sku_id`) REFERENCES `tailor_is_product`.`product_sku` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='秒杀商品表';

-- ============================================================
-- 5. 积分商城商品表 (points_mall_product)
-- ============================================================
DROP TABLE IF EXISTS `points_mall_product`;
CREATE TABLE `points_mall_product` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '积分商品ID（主键）',
  `name` VARCHAR(128) NOT NULL COMMENT '商品名称',
  `image` VARCHAR(512) NOT NULL COMMENT '商品图片URL',
  `images` JSON DEFAULT NULL COMMENT '商品图片列表（JSON）',
  `description` TEXT DEFAULT NULL COMMENT '商品描述',
  `points_required` INT NOT NULL COMMENT '所需积分',
  `cash_price` DECIMAL(12, 2) DEFAULT 0.00 COMMENT '现金补差价（元）',
  `stock` INT NOT NULL DEFAULT 0 COMMENT '库存数量',
  `limit_count` INT DEFAULT 1 COMMENT '每人限兑数量',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-下架，1-上架',
  `sort` INT DEFAULT 0 COMMENT '排序权重',
  `exchange_count` INT DEFAULT 0 COMMENT '已兑换数量',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_points_required` (`points_required`),
  KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='积分商城商品表';

-- ============================================================
-- 6. 积分记录表 (points_record)
-- ============================================================
DROP TABLE IF EXISTS `points_record`;
CREATE TABLE `points_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '积分记录ID（主键）',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `points_change` INT NOT NULL COMMENT '积分变动值（正数为增加，负数为减少）',
  `change_type` TINYINT NOT NULL COMMENT '变动类型：1-签到奖励，2-购物奖励，3-评价奖励，4-活动奖励，5-积分兑换，6-订单退款扣回，7-管理员调整，8-积分过期',
  `related_type` VARCHAR(32) DEFAULT NULL COMMENT '关联业务类型：order/coupon/admin等',
  `related_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联业务ID',
  `related_no` VARCHAR(64) DEFAULT NULL COMMENT '关联业务编号',
  `description` VARCHAR(256) DEFAULT NULL COMMENT '变动说明',
  `points_before` INT NOT NULL COMMENT '变动前积分',
  `points_after` INT NOT NULL COMMENT '变动后积分',
  `expire_time` DATETIME DEFAULT NULL COMMENT '积分过期时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_change_type` (`change_type`),
  KEY `idx_related_type` (`related_type`),
  KEY `idx_related_id` (`related_id`),
  KEY `idx_created_at` (`created_at`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_precord_user` FOREIGN KEY (`user_id`) REFERENCES `tailor_is_user`.`sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='积分变动记录表';

-- ============================================================
-- 7. 会员等级表 (member_level)
-- ============================================================
DROP TABLE IF EXISTS `member_level`;
CREATE TABLE `member_level` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '会员等级ID（主键）',
  `level_name` VARCHAR(64) NOT NULL COMMENT '等级名称',
  `level_code` VARCHAR(32) NOT NULL COMMENT '等级编码',
  `level_value` INT NOT NULL COMMENT '等级数值（越大等级越高）',
  `min_points` INT NOT NULL COMMENT '最低所需积分',
  `max_points` INT DEFAULT NULL COMMENT '最高积分上限（NULL表示无上限）',
  `discount_rate` DECIMAL(3, 2) DEFAULT 1.00 COMMENT '会员折扣率（如0.95表示95折）',
  `privileges` JSON DEFAULT NULL COMMENT '会员特权（JSON格式）',
  `icon` VARCHAR(512) DEFAULT NULL COMMENT '等级图标URL',
  `color` VARCHAR(16) DEFAULT '#FFD700' COMMENT '等级标识颜色',
  `description` VARCHAR(256) DEFAULT NULL COMMENT '等级描述',
  `sort` INT DEFAULT 0 COMMENT '排序权重',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_level_code` (`level_code`),
  KEY `idx_level_value` (`level_value`),
  KEY `idx_min_points` (`min_points`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会员等级表';

-- ============================================================
-- 8. 店铺会员表 (shop_member)
-- ============================================================
DROP TABLE IF EXISTS `shop_member`;
CREATE TABLE `shop_member` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '店铺会员ID（主键）',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `shop_id` BIGINT UNSIGNED NOT NULL COMMENT '店铺ID',
  `merchant_id` BIGINT UNSIGNED NOT NULL COMMENT '商家ID',
  `level` TINYINT NOT NULL DEFAULT 1 COMMENT '店铺会员等级：1-普通会员，2-银卡会员，3-金卡会员，4-钻石会员',
  `member_price_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用会员价：0-否，1-是',
  `total_consume` DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '在该店铺累计消费金额（元）',
  `order_count` INT NOT NULL DEFAULT 0 COMMENT '在该店铺订单数量',
  `points` INT NOT NULL DEFAULT 0 COMMENT '店铺专属积分',
  `join_time` DATETIME DEFAULT NULL COMMENT '成为会员时间',
  `expire_time` DATETIME DEFAULT NULL COMMENT '会员有效期',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-非会员，1-普通会员',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_shop` (`user_id`, `shop_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_level` (`level`),
  KEY `idx_status` (`status`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_sm_user` FOREIGN KEY (`user_id`) REFERENCES `tailor_is_user`.`sys_user` (`id`),
  -- CONSTRAINT `fk_sm_shop` FOREIGN KEY (`shop_id`) REFERENCES `tailor_is_merchant`.`merchant_shop` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='店铺会员表';

-- ============================================================
-- 9. 签到记录表 (checkin_record)
-- ============================================================
DROP TABLE IF EXISTS `checkin_record`;
CREATE TABLE `checkin_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '签到记录ID（主键）',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `checkin_date` DATE NOT NULL COMMENT '签到日期',
  `continuous_days` INT NOT NULL DEFAULT 1 COMMENT '连续签到天数',
  `points_earned` INT NOT NULL DEFAULT 0 COMMENT '获得积分',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`, `checkin_date`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_checkin_date` (`checkin_date`),
  KEY `idx_continuous_days` (`continuous_days`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_checkin_user` FOREIGN KEY (`user_id`) REFERENCES `tailor_is_user`.`sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户签到记录表';

-- ============================================================
-- 初始化数据：会员等级
-- ============================================================
INSERT INTO `member_level` (`level_name`, `level_code`, `level_value`, `min_points`, `max_points`, `discount_rate`, `privileges`, `icon`, `color`, `description`, `sort`, `status`) VALUES
('普通会员', 'NORMAL', 1, 0, 999, 1.00, '["基础购物权益"]', '/icons/member/normal.svg', '#909399', '注册即为普通会员，享受基础购物权益', 1, 1),
('银卡会员', 'SILVER', 2, 1000, 4999, 0.98, '["基础购物权益", "98折优惠", "专属客服"]', '/icons/member/silver.svg', '#C0C0C0', '累计1000积分升级，享98折优惠和专属客服', 2, 1),
('金卡会员', 'GOLD', 3, 5000, 19999, 0.95, '["基础购物权益", "95折优惠", "专属客服", "优先发货", "生日礼包"]', '/icons/member/gold.svg', '#FFD700', '累计5000积分升级，享95折优惠及更多特权', 3, 1),
('钻石会员', 'DIAMOND', 4, 20000, NULL, 0.90, '["基础购物权益", "9折优惠", "专属客服", "优先发货", "生日礼包", "免费退换", "专属活动"]', '/icons/member/diamond.svg', '#E6A23C', '累计20000积分升级，享9折及全部会员特权', 4, 1);

-- 初始化数据：优惠券模板
INSERT INTO `coupon_template` (`name`, `type`, `discount_type`, `discount_value`, `min_amount`, `total_count`, `per_limit`, `scope_type`, `start_time`, `end_time`, `receive_start_time`, `receive_end_time`, `status`, `description`) VALUES
('新用户专享券', 1, 1, 20.00, 99.00, 10000, 1, 1, '2026-01-01 00:00:00', '2026-12-31 23:59:59', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1, '新用户注册即领，满99减20'),
('全场满减券', 1, 1, 50.00, 299.00, 50000, 2, 1, '2026-01-01 00:00:00', '2026-12-31 23:59:59', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1, '全场通用满299减50'),
('8折优惠券', 2, 2, 8.00, 100.00, 5000, 1, 1, '2026-01-01 00:00:00', '2026-12-31 23:59:59', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1, '全场8折优惠，最高抵扣100元'),
('免邮券', 4, 1, 0.00, 0.00, 20000, 3, 1, '2026-01-01 00:00:00', '2026-12-31 23:59:59', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1, '全场免邮券，无门槛使用');
