-- ============================================================
-- Tailor IS - Flyway Migration V1: 商品系统数据库初始化
-- 数据库: tailor_is_product
-- 创建时间: 2026-07-09
-- 说明: 基线迁移，创建商品分类、商品、SKU、属性、评价等核心表
-- ============================================================

CREATE TABLE IF NOT EXISTS `product_category` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分类ID（主键）',
  `name` VARCHAR(64) NOT NULL COMMENT '分类名称',
  `parent_id` BIGINT UNSIGNED DEFAULT 0 COMMENT '父分类ID（0表示顶级分类）',
  `level` TINYINT NOT NULL DEFAULT 1 COMMENT '分类层级：1-一级，2-二级，3-三级',
  `sort` INT DEFAULT 0 COMMENT '排序权重（数值越小越靠前）',
  `icon` VARCHAR(512) DEFAULT NULL COMMENT '分类图标URL',
  `image` VARCHAR(512) DEFAULT NULL COMMENT '分类展示图片URL',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `description` VARCHAR(256) DEFAULT NULL COMMENT '分类描述',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_level` (`level`),
  KEY `idx_sort` (`sort`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品分类表';

CREATE TABLE IF NOT EXISTS `product` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '商品ID（主键）',
  `merchant_id` BIGINT UNSIGNED NOT NULL COMMENT '商家ID',
  `shop_id` BIGINT UNSIGNED NOT NULL COMMENT '店铺ID',
  `category_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '商品分类ID',
  `product_type` TINYINT NOT NULL DEFAULT 1 COMMENT '商品类型：1-实物商品，2-虚拟商品，3-服务商品，4-定制商品',
  `name` VARCHAR(256) NOT NULL COMMENT '商品名称',
  `sub_title` VARCHAR(256) DEFAULT NULL COMMENT '商品副标题/卖点',
  `main_image` VARCHAR(512) NOT NULL COMMENT '商品主图URL',
  `images` JSON DEFAULT NULL COMMENT '商品图片列表（JSON数组）',
  `video_url` VARCHAR(512) DEFAULT NULL COMMENT '商品视频URL',
  `description` TEXT DEFAULT NULL COMMENT '商品详情描述（富文本）',
  `specifications` JSON DEFAULT NULL COMMENT '商品规格说明（JSON）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '商品状态：0-草稿，1-待审核，2-已上架，3-已下架，4-已驳回',
  `audit_status` TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态：0-待审核，1-审核中，2-已通过，3-已驳回',
  `audit_remark` VARCHAR(512) DEFAULT NULL COMMENT '审核备注',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `audit_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '审核人ID',
  `copyright_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '版权标识：0-无版权，1-原创已认证，2-原创未认证，3-授权使用',
  `copyright_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '版权登记ID',
  `brand_name` VARCHAR(64) DEFAULT NULL COMMENT '品牌名称',
  `weight` DECIMAL(10, 2) DEFAULT NULL COMMENT '商品重量（kg）',
  `length` DECIMAL(10, 2) DEFAULT NULL COMMENT '长度（cm）',
  `width` DECIMAL(10, 2) DEFAULT NULL COMMENT '宽度（cm）',
  `height` DECIMAL(10, 2) DEFAULT NULL COMMENT '高度（cm）',
  `freight_template_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '运费模板ID',
  `sale_count` INT DEFAULT 0 COMMENT '销量',
  `view_count` INT DEFAULT 0 COMMENT '浏览量',
  `comment_count` INT DEFAULT 0 COMMENT '评论数',
  `favorable_rate` DECIMAL(5, 2) DEFAULT 100.00 COMMENT '好评率（0.00-100.00）',
  `lower_shelf_reason` VARCHAR(256) DEFAULT NULL COMMENT '下架原因',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_product_type` (`product_type`),
  KEY `idx_status` (`status`),
  KEY `idx_audit_status` (`audit_status`),
  KEY `idx_copyright_flag` (`copyright_flag`),
  KEY `idx_name` (`name`),
  KEY `idx_created_at` (`created_at`),
  FULLTEXT KEY `ft_name_subtitle` (`name`, `sub_title`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品表';

CREATE TABLE IF NOT EXISTS `product_sku` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'SKU ID（主键）',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
  `sku_code` VARCHAR(64) DEFAULT NULL COMMENT 'SKU编码（商家自定义）',
  `barcode` VARCHAR(64) DEFAULT NULL COMMENT '商品条码/ISBN',
  `attributes` JSON NOT NULL COMMENT 'SKU属性（JSON格式，如：{"颜色":"红色","尺寸":"XL"}）',
  `attribute_text` VARCHAR(256) DEFAULT NULL COMMENT 'SKU属性描述文本（用于快速展示）',
  `price` DECIMAL(12, 2) NOT NULL COMMENT '销售价格（元）',
  `original_price` DECIMAL(12, 2) DEFAULT NULL COMMENT '原价/划线价（元）',
  `cost_price` DECIMAL(12, 2) DEFAULT NULL COMMENT '成本价（元）',
  `stock` INT NOT NULL DEFAULT 0 COMMENT '库存数量',
  `warning_stock` INT DEFAULT 10 COMMENT '预警库存（低于此值触发预警）',
  `weight` DECIMAL(10, 2) DEFAULT NULL COMMENT 'SKU重量（kg）',
  `image` VARCHAR(512) DEFAULT NULL COMMENT 'SKU专属图片URL',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `sales_count` INT DEFAULT 0 COMMENT 'SKU销量',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_sku_code` (`sku_code`),
  KEY `idx_barcode` (`barcode`),
  KEY `idx_status` (`status`),
  KEY `idx_stock` (`stock`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品SKU表';

CREATE TABLE IF NOT EXISTS `product_attribute` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '属性ID（主键）',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
  `attr_name` VARCHAR(64) NOT NULL COMMENT '属性名称',
  `attr_value` VARCHAR(256) NOT NULL COMMENT '属性值',
  `attr_type` TINYINT NOT NULL DEFAULT 1 COMMENT '属性类型：1-销售属性（规格），2-商品属性（参数），3-自定义属性',
  `sort` INT DEFAULT 0 COMMENT '排序权重',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_attr_type` (`attr_type`),
  KEY `idx_attr_name` (`attr_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品属性表';

CREATE TABLE IF NOT EXISTS `product_tag` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '标签ID（主键）',
  `name` VARCHAR(64) NOT NULL COMMENT '标签名称',
  `color` VARCHAR(16) DEFAULT '#409EFF' COMMENT '标签颜色（HEX格式）',
  `sort` INT DEFAULT 0 COMMENT '排序权重',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_status` (`status`),
  KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品标签表';

CREATE TABLE IF NOT EXISTS `product_tag_mapping` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
  `tag_id` BIGINT UNSIGNED NOT NULL COMMENT '标签ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_tag` (`product_id`, `tag_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品标签关联表';

CREATE TABLE IF NOT EXISTS `product_review` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评价ID（主键）',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
  `sku_id` BIGINT UNSIGNED DEFAULT NULL COMMENT 'SKU ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '评价用户ID',
  `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  `order_item_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '订单明细ID',
  `rating` TINYINT NOT NULL COMMENT '评分：1-5星',
  `content` VARCHAR(1000) DEFAULT NULL COMMENT '评价内容',
  `images` JSON DEFAULT NULL COMMENT '评价图片（JSON数组）',
  `video_url` VARCHAR(512) DEFAULT NULL COMMENT '评价视频URL',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-隐藏，1-显示，2-待审核，3-已删除',
  `is_anonymous` TINYINT DEFAULT 0 COMMENT '是否匿名：0-否，1-是',
  `is_additional` TINYINT DEFAULT 0 COMMENT '是否追评：0-否，1-是',
  `parent_id` BIGINT UNSIGNED DEFAULT 0 COMMENT '父评价ID（追评关联）',
  `merchant_reply` VARCHAR(1000) DEFAULT NULL COMMENT '商家回复',
  `merchant_reply_time` DATETIME DEFAULT NULL COMMENT '商家回复时间',
  `like_count` INT DEFAULT 0 COMMENT '点赞数',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_rating` (`rating`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品评价表';

INSERT INTO `product_category` (`name`, `parent_id`, `level`, `sort`, `icon`, `status`) VALUES
('服装定制', 0, 1, 1, '/icons/clothing.svg', 1),
('面料辅料', 0, 1, 2, '/icons/fabric.svg', 1),
('设计服务', 0, 1, 3, '/icons/design.svg', 1),
('设备工具', 0, 1, 4, '/icons/equipment.svg', 1),
('图案素材', 0, 1, 5, '/icons/pattern.svg', 1),
('成品服饰', 0, 1, 6, '/icons/garment.svg', 1);

INSERT INTO `product_category` (`name`, `parent_id`, `level`, `sort`, `icon`, `status`) VALUES
('T恤定制', 1, 2, 1, NULL, 1),
('卫衣定制', 1, 2, 2, NULL, 1),
('衬衫定制', 1, 2, 3, NULL, 1),
('裤子定制', 1, 2, 4, NULL, 1),
('外套定制', 1, 2, 5, NULL, 1),
('旗袍定制', 1, 2, 6, NULL, 1);

INSERT INTO `product_category` (`name`, `parent_id`, `level`, `sort`, `icon`, `status`) VALUES
('棉布面料', 2, 2, 1, NULL, 1),
('丝绸面料', 2, 2, 2, NULL, 1),
('麻布面料', 2, 2, 3, NULL, 1),
('化纤面料', 2, 2, 4, NULL, 1),
('拉链配件', 2, 2, 5, NULL, 1),
('纽扣配件', 2, 2, 6, NULL, 1);

INSERT INTO `product_category` (`name`, `parent_id`, `level`, `sort`, `icon`, `status`) VALUES
('服装设计', 3, 2, 1, NULL, 1),
('图案设计', 3, 2, 2, NULL, 1),
('Logo设计', 3, 2, 3, NULL, 1),
('版式设计', 3, 2, 4, NULL, 1);

INSERT INTO `product_category` (`name`, `parent_id`, `level`, `sort`, `icon`, `status`) VALUES
('缝纫机', 4, 2, 1, NULL, 1),
('裁剪工具', 4, 2, 2, NULL, 1),
('熨烫设备', 4, 2, 3, NULL, 1),
('绣花机', 4, 2, 4, NULL, 1);

INSERT INTO `product_category` (`name`, `parent_id`, `level`, `sort`, `icon`, `status`) VALUES
('印花图案', 5, 2, 1, NULL, 1),
('绣花图案', 5, 2, 2, NULL, 1),
('数码印花', 5, 2, 3, NULL, 1);

INSERT INTO `product_category` (`name`, `parent_id`, `level`, `sort`, `icon`, `status`) VALUES
('成衣T恤', 6, 2, 1, NULL, 1),
('成衣衬衫', 6, 2, 2, NULL, 1),
('成衣裤子', 6, 2, 3, NULL, 1),
('成衣外套', 6, 2, 4, NULL, 1);

INSERT INTO `product_tag` (`name`, `color`, `sort`, `status`) VALUES
('原创设计', '#E6A23C', 1, 1),
('热门', '#F56C6C', 2, 1),
('新品', '#409EFF', 3, 1),
('版权保护', '#67C23A', 4, 1),
('限时特价', '#E6A23C', 5, 1),
('工厂直供', '#909399', 6, 1),
('高端定制', '#E6A23C', 7, 1),
('快速出货', '#409EFF', 8, 1);