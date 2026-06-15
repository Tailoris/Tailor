-- ============================================================
-- Sprint 8 数据库迁移脚本 (PRD-003/005/008/009)
-- ============================================================
-- 执行环境: dev / staging / prod
-- 风险评估: 低（仅新增表/字段，不修改现有数据）
-- 回滚: DROP TABLE IF EXISTS ...
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 数字纸样表 (digital_pattern)
-- ============================================================
CREATE TABLE IF NOT EXISTS `digital_pattern` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `product_id`        BIGINT          NOT NULL COMMENT '关联商品ID',
    `file_key`          VARCHAR(255)    NOT NULL COMMENT 'OSS存储key',
    `file_url`          VARCHAR(512)    DEFAULT NULL COMMENT '文件URL',
    `preview_url`       VARCHAR(512)    DEFAULT NULL COMMENT '预览图URL',
    `file_size`         BIGINT          DEFAULT 0 COMMENT '文件大小（字节）',
    `file_format`       VARCHAR(20)     DEFAULT NULL COMMENT '文件格式 SVG/PDF/DXF/AI',
    `pattern_type`      VARCHAR(50)     DEFAULT NULL COMMENT '纸样类型 裤子/上衣/...',
    `difficulty`        TINYINT         DEFAULT 1 COMMENT '难度 1=入门 2=中级 3=高级',
    `fabric_requirement` VARCHAR(500)   DEFAULT NULL COMMENT '面料需求',
    `license_type`      VARCHAR(20)     DEFAULT 'PERSONAL' COMMENT '授权类型 PERSONAL=个人 / COMMERCIAL=商业',
    `license_duration_days` INT          DEFAULT 0 COMMENT '授权天数 0=永久',
    `version`           VARCHAR(20)     DEFAULT 'v1.0' COMMENT '版本',
    `download_count`    INT             DEFAULT 0 COMMENT '下载次数',
    `preview_count`     INT             DEFAULT 0 COMMENT '预览次数',
    `design_price`      DECIMAL(10,2)   DEFAULT 0 COMMENT '设计版权费',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`           TINYINT         DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数字纸样表';

-- ============================================================
-- 2. 数字纸样下载token表 (pattern_download_token)
-- ============================================================
CREATE TABLE IF NOT EXISTS `pattern_download_token` (
    `id`                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`            BIGINT        NOT NULL COMMENT '用户ID',
    `order_id`           BIGINT        DEFAULT NULL COMMENT '订单ID',
    `pattern_id`         BIGINT        NOT NULL COMMENT '纸样ID',
    `product_id`         BIGINT        NOT NULL COMMENT '商品ID',
    `token`              VARCHAR(255)  NOT NULL COMMENT '下载token',
    `max_download_count` INT           DEFAULT 3 COMMENT '最大下载次数',
    `used_count`         INT           DEFAULT 0 COMMENT '已使用次数',
    `expire_time`        DATETIME      DEFAULT NULL COMMENT '过期时间',
    `create_time`        DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`            TINYINT       DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_token` (`token`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_pattern_id` (`pattern_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数字纸样下载token';

-- ============================================================
-- 3. 定制商品参数采集表 (custom_measurement)
-- ============================================================
CREATE TABLE IF NOT EXISTS `custom_measurement` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`         BIGINT          NOT NULL COMMENT '用户ID',
    `order_id`        BIGINT          NOT NULL COMMENT '订单ID',
    `product_id`      BIGINT          NOT NULL COMMENT '商品ID',
    `height`          DECIMAL(5,1)    DEFAULT NULL COMMENT '身高cm',
    `weight`          DECIMAL(5,1)    DEFAULT NULL COMMENT '体重kg',
    `bust`            DECIMAL(5,1)    DEFAULT NULL COMMENT '胸围cm',
    `waist`           DECIMAL(5,1)    DEFAULT NULL COMMENT '腰围cm',
    `hip`             DECIMAL(5,1)    DEFAULT NULL COMMENT '臀围cm',
    `shoulder`        DECIMAL(5,1)    DEFAULT NULL COMMENT '肩宽cm',
    `sleeve_length`   DECIMAL(5,1)    DEFAULT NULL COMMENT '袖长cm',
    `pants_length`    DECIMAL(5,1)    DEFAULT NULL COMMENT '裤长cm',
    `neck`            DECIMAL(5,1)    DEFAULT NULL COMMENT '颈围cm',
    `arm`             DECIMAL(5,1)    DEFAULT NULL COMMENT '臂围cm',
    `thigh`           DECIMAL(5,1)    DEFAULT NULL COMMENT '大腿围cm',
    `fit_preference`  VARCHAR(20)     DEFAULT NULL COMMENT '偏好版型 SLIM/REGULAR/LOOSE',
    `color_preference` VARCHAR(50)    DEFAULT NULL COMMENT '偏好颜色',
    `remark`          VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    `create_time`     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT         DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定制商品参数采集';

-- ============================================================
-- 4. 商品评价扩展字段 (product_review)
-- ============================================================
ALTER TABLE `product_review`
    ADD COLUMN `image_urls`        TEXT          DEFAULT NULL COMMENT '评价图片URL列表（JSON）' AFTER `content`,
    ADD COLUMN `tags`              VARCHAR(255)  DEFAULT NULL COMMENT '评价标签 JSON' AFTER `image_urls`,
    ADD COLUMN `sku_id`            BIGINT        DEFAULT NULL COMMENT '评价的SKU' AFTER `tags`,
    ADD COLUMN `merchant_reply`    VARCHAR(1000) DEFAULT NULL COMMENT '商家回复' AFTER `reply_time`,
    ADD COLUMN `merchant_reply_time` DATETIME    DEFAULT NULL COMMENT '商家回复时间' AFTER `merchant_reply`,
    ADD COLUMN `helpful_count`     INT           DEFAULT 0 COMMENT '有帮助数' AFTER `merchant_reply_time`,
    ADD COLUMN `report_count`      INT           DEFAULT 0 COMMENT '举报数' AFTER `helpful_count`,
    ADD COLUMN `is_featured`       TINYINT       DEFAULT 0 COMMENT '是否精选 0否 1是' AFTER `report_count`,
    ADD KEY `idx_sku_id` (`sku_id`),
    ADD KEY `idx_is_featured` (`is_featured`),
    ADD KEY `idx_helpful_count` (`helpful_count`);

-- ============================================================
-- 5. 商品咨询表 (product_inquiry) - PRD-009
-- ============================================================
CREATE TABLE IF NOT EXISTS `product_inquiry` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `product_id`    BIGINT        NOT NULL COMMENT '商品ID',
    `user_id`       BIGINT        DEFAULT NULL COMMENT '提问用户ID（匿名可空）',
    `user_name`     VARCHAR(50)   DEFAULT NULL COMMENT '提问人昵称',
    `question`      VARCHAR(500)  NOT NULL COMMENT '问题内容',
    `answer`        VARCHAR(1000) DEFAULT NULL COMMENT '回答内容',
    `answer_user_id` BIGINT       DEFAULT NULL COMMENT '回答人（商家）ID',
    `answer_time`   DATETIME      DEFAULT NULL COMMENT '回答时间',
    `status`        TINYINT       DEFAULT 0 COMMENT '0=待回复 1=已回复',
    `is_anonymous`  TINYINT       DEFAULT 0 COMMENT '是否匿名',
    `create_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT       DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品咨询表';

-- ============================================================
-- 6. 商品搜索关键词热度表 (product_search_keyword) - PRD-005
-- ============================================================
CREATE TABLE IF NOT EXISTS `product_search_keyword` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `keyword`        VARCHAR(100)  NOT NULL COMMENT '搜索关键词',
    `search_count`   BIGINT        DEFAULT 0 COMMENT '搜索次数',
    `result_count`   BIGINT        DEFAULT 0 COMMENT '结果数（最近一次）',
    `click_count`    BIGINT        DEFAULT 0 COMMENT '点击商品数',
    `is_hot`         TINYINT       DEFAULT 0 COMMENT '是否热词',
    `create_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT       DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_keyword` (`keyword`),
    KEY `idx_search_count` (`search_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品搜索关键词统计';

-- ============================================================
-- 7. 商品类型字段扩展 (product.product_type)
-- ============================================================
ALTER TABLE `product`
    ADD COLUMN IF NOT EXISTS `product_type` TINYINT DEFAULT 1 COMMENT '商品类型 1=实物 2=数字纸样 3=定制' AFTER `category_id`,
    ADD COLUMN IF NOT EXISTS `pattern_id`   BIGINT DEFAULT NULL COMMENT '关联数字纸样ID' AFTER `product_type`,
    ADD COLUMN IF NOT EXISTS `is_preset_measure` TINYINT DEFAULT 0 COMMENT '定制商品是否使用预设尺码' AFTER `pattern_id`,
    ADD KEY `idx_product_type` (`product_type`),
    ADD KEY `idx_pattern_id` (`pattern_id`);

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 回滚脚本（紧急情况使用）
-- ============================================================
/*
DROP TABLE IF EXISTS `digital_pattern`;
DROP TABLE IF EXISTS `pattern_download_token`;
DROP TABLE IF EXISTS `custom_measurement`;
DROP TABLE IF EXISTS `product_inquiry`;
DROP TABLE IF EXISTS `product_search_keyword`;

ALTER TABLE `product_review`
    DROP COLUMN `image_urls`,
    DROP COLUMN `tags`,
    DROP COLUMN `sku_id`,
    DROP COLUMN `merchant_reply`,
    DROP COLUMN `merchant_reply_time`,
    DROP COLUMN `helpful_count`,
    DROP COLUMN `report_count`,
    DROP COLUMN `is_featured`;

ALTER TABLE `product`
    DROP COLUMN `product_type`,
    DROP COLUMN `pattern_id`,
    DROP COLUMN `is_preset_measure`;
*/
