-- ============================================================
-- Sprint 8.2 营销与社区数据库迁移脚本
-- 任务: MKT-002/003/004 拼团/秒杀/阶梯满减 + 社区功能增强
-- 执行环境: dev / staging / prod
-- 风险评估: 中（新增表/字段，不修改现有数据）
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 拼团活动表 (mkt_group_buy)
-- ============================================================
USE `tailor_is_marketing`;

CREATE TABLE IF NOT EXISTS `mkt_group_buy` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `activity_name`     VARCHAR(200)    NOT NULL COMMENT '活动名称',
    `product_id`        BIGINT          NOT NULL COMMENT '关联商品ID',
    `sku_id`            BIGINT          DEFAULT NULL COMMENT '关联SKU ID',
    `shop_id`           BIGINT          DEFAULT NULL COMMENT '商家ID',
    `group_size`        INT             NOT NULL DEFAULT 2 COMMENT '成团人数',
    `group_price`       DECIMAL(15,2)   NOT NULL COMMENT '拼团价',
    `original_price`    DECIMAL(15,2)   NOT NULL COMMENT '原价',
    `total_stock`       INT             NOT NULL DEFAULT 0 COMMENT '活动总库存',
    `sold_count`        INT             DEFAULT 0 COMMENT '已售数量',
    `group_count`       INT             DEFAULT 0 COMMENT '开团数',
    `success_count`     INT             DEFAULT 0 COMMENT '成团数',
    `limit_per_user`    INT             DEFAULT 1 COMMENT '每人限购数',
    `valid_hours`       INT             NOT NULL DEFAULT 24 COMMENT '拼团有效期（小时）',
    `start_time`        DATETIME        NOT NULL COMMENT '活动开始时间',
    `end_time`          DATETIME        NOT NULL COMMENT '活动结束时间',
    `status`            TINYINT         DEFAULT 0 COMMENT '状态 0=未开始 1=进行中 2=已结束 3=已取消',
    `description`       TEXT            DEFAULT NULL COMMENT '活动描述',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`           TINYINT         DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_shop_id` (`shop_id`),
    KEY `idx_status_time` (`status`, `start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团活动表';

-- ============================================================
-- 2. 拼团实例表 (mkt_group_buy_instance)
-- ============================================================
CREATE TABLE IF NOT EXISTS `mkt_group_buy_instance` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `activity_id`       BIGINT          NOT NULL COMMENT '活动ID',
    `group_no`          VARCHAR(64)     NOT NULL COMMENT '团编号',
    `leader_user_id`    BIGINT          NOT NULL COMMENT '团长用户ID',
    `current_size`      INT             DEFAULT 1 COMMENT '当前人数',
    `group_size`        INT             NOT NULL COMMENT '成团人数',
    `status`            TINYINT         DEFAULT 0 COMMENT '状态 0=进行中 1=已成团 2=已失败 3=已退款',
    `expire_time`       DATETIME        NOT NULL COMMENT '拼团过期时间',
    `complete_time`     DATETIME        DEFAULT NULL COMMENT '成团时间',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_no` (`group_no`),
    KEY `idx_activity_id` (`activity_id`),
    KEY `idx_leader` (`leader_user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团实例表';

-- ============================================================
-- 3. 拼团成员表 (mkt_group_buy_member)
-- ============================================================
CREATE TABLE IF NOT EXISTS `mkt_group_buy_member` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `instance_id`       BIGINT          NOT NULL COMMENT '拼团实例ID',
    `user_id`           BIGINT          NOT NULL COMMENT '用户ID',
    `order_id`          BIGINT          DEFAULT NULL COMMENT '订单ID',
    `is_leader`         TINYINT         DEFAULT 0 COMMENT '是否团长 0否 1是',
    `join_time`         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `status`            TINYINT         DEFAULT 0 COMMENT '状态 0=待支付 1=已支付 2=已退款 3=已取消',
    `pay_time`          DATETIME        DEFAULT NULL COMMENT '支付时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_instance_user` (`instance_id`, `user_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团成员表';

-- ============================================================
-- 4. 阶梯满减满赠规则表 (mkt_promotion_rule)
-- ============================================================
CREATE TABLE IF NOT EXISTS `mkt_promotion_rule` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `promotion_name`    VARCHAR(200)    NOT NULL COMMENT '活动名称',
    `promotion_type`    TINYINT         NOT NULL COMMENT '类型 1=满减 2=满折 3=满赠 4=满件折',
    `shop_id`           BIGINT          DEFAULT NULL COMMENT '商家ID（NULL=平台）',
    `scope_type`        TINYINT         NOT NULL DEFAULT 1 COMMENT '范围 1=全场 2=类目 3=商品 4=店铺',
    `scope_value`       VARCHAR(1000)   DEFAULT NULL COMMENT '范围值（JSON数组）',
    `threshold_type`    TINYINT         NOT NULL DEFAULT 1 COMMENT '门槛类型 1=金额 2=件数',
    `start_time`        DATETIME        NOT NULL COMMENT '开始时间',
    `end_time`          DATETIME        NOT NULL COMMENT '结束时间',
    `status`            TINYINT         DEFAULT 0 COMMENT '状态 0=未开始 1=进行中 2=已结束 3=已取消',
    `priority`          INT             DEFAULT 0 COMMENT '优先级（数字越大越优先）',
    `stackable`         TINYINT         DEFAULT 0 COMMENT '是否可叠加 0否 1是',
    `description`       TEXT            DEFAULT NULL COMMENT '活动描述',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`           TINYINT         DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_shop_id` (`shop_id`),
    KEY `idx_status_time` (`status`, `start_time`, `end_time`),
    KEY `idx_promotion_type` (`promotion_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阶梯满减满赠活动表';

-- ============================================================
-- 5. 阶梯规则明细表 (mkt_promotion_step)
-- ============================================================
CREATE TABLE IF NOT EXISTS `mkt_promotion_step` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `promotion_id`      BIGINT          NOT NULL COMMENT '促销ID',
    `threshold_value`   DECIMAL(15,2)   NOT NULL COMMENT '门槛值（满多少/几件）',
    `discount_type`     TINYINT         NOT NULL COMMENT '优惠类型 1=减金额 2=打折 3=赠品',
    `discount_value`    DECIMAL(15,2)   DEFAULT NULL COMMENT '优惠值（金额/折扣）',
    `gift_product_id`   BIGINT          DEFAULT NULL COMMENT '赠品商品ID',
    `gift_quantity`     INT             DEFAULT 1 COMMENT '赠品数量',
    `max_discount`      DECIMAL(15,2)   DEFAULT NULL COMMENT '最大优惠金额（封顶）',
    `sort_order`        INT             DEFAULT 0 COMMENT '排序',
    PRIMARY KEY (`id`),
    KEY `idx_promotion_id` (`promotion_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='促销阶梯明细表';

-- ============================================================
-- 6. 营销活动报表 (mkt_promotion_stats)
-- ============================================================
CREATE TABLE IF NOT EXISTS `mkt_promotion_stats` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `promotion_type`    TINYINT         NOT NULL COMMENT '促销类型 1=优惠券 2=拼团 3=秒杀 4=满减',
    `promotion_id`      BIGINT          NOT NULL COMMENT '促销ID',
    `promotion_name`    VARCHAR(200)    DEFAULT NULL COMMENT '促销名称',
    `stat_date`         DATE            NOT NULL COMMENT '统计日期',
    `exposure_count`    BIGINT          DEFAULT 0 COMMENT '曝光数',
    `click_count`       BIGINT          DEFAULT 0 COMMENT '点击数',
    `participate_count` BIGINT          DEFAULT 0 COMMENT '参与数',
    `order_count`       BIGINT          DEFAULT 0 COMMENT '订单数',
    `order_amount`      DECIMAL(15,2)   DEFAULT 0 COMMENT '订单金额',
    `discount_amount`   DECIMAL(15,2)   DEFAULT 0 COMMENT '优惠金额',
    `roi`               DECIMAL(5,4)    DEFAULT 0 COMMENT '投资回报率',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_promotion_date` (`promotion_type`, `promotion_id`, `stat_date`),
    KEY `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='营销活动报表';

-- ============================================================
-- 7. 订单营销关联表 (mkt_order_promotion)
-- ============================================================
CREATE TABLE IF NOT EXISTS `mkt_order_promotion` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_id`          BIGINT          NOT NULL COMMENT '订单ID',
    `promotion_type`    TINYINT         NOT NULL COMMENT '促销类型 1=优惠券 2=拼团 3=秒杀 4=满减 5=积分',
    `promotion_id`      BIGINT          NOT NULL COMMENT '促销ID',
    `promotion_name`    VARCHAR(200)    DEFAULT NULL COMMENT '促销名称',
    `discount_amount`   DECIMAL(15,2)   NOT NULL DEFAULT 0 COMMENT '优惠金额',
    `coupon_id`         BIGINT          DEFAULT NULL COMMENT '优惠券ID',
    `group_instance_id` BIGINT          DEFAULT NULL COMMENT '拼团实例ID',
    `seckill_id`        BIGINT          DEFAULT NULL COMMENT '秒杀ID',
    `points_used`       INT             DEFAULT 0 COMMENT '使用积分',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_promotion_id` (`promotion_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单营销关联表';

-- ============================================================
-- 8. 营销SKU价格表 (mkt_sku_promotion_price)
-- ============================================================
CREATE TABLE IF NOT EXISTS `mkt_sku_promotion_price` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `sku_id`            BIGINT          NOT NULL COMMENT 'SKU ID',
    `product_id`        BIGINT          NOT NULL COMMENT '商品ID',
    `promotion_type`    TINYINT         NOT NULL COMMENT '促销类型',
    `promotion_id`      BIGINT          NOT NULL COMMENT '促销ID',
    `promotion_price`   DECIMAL(15,2)   NOT NULL COMMENT '促销价',
    `original_price`    DECIMAL(15,2)   NOT NULL COMMENT '原价',
    `stock`             INT             DEFAULT 0 COMMENT '活动库存',
    `sold_count`        INT             DEFAULT 0 COMMENT '已售',
    `start_time`        DATETIME        DEFAULT NULL COMMENT '开始时间',
    `end_time`          DATETIME        DEFAULT NULL COMMENT '结束时间',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sku_promotion` (`sku_id`, `promotion_type`, `promotion_id`),
    KEY `idx_promotion` (`promotion_id`, `promotion_type`),
    KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SKU促销价格表';

-- ============================================================
-- 9. 社区关注表扩展 (community_follow 增强)
-- ============================================================
USE `tailor_is_community`;

CREATE TABLE IF NOT EXISTS `community_follow` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`           BIGINT          NOT NULL COMMENT '用户ID（粉丝）',
    `target_user_id`    BIGINT          DEFAULT NULL COMMENT '目标用户ID（互关时使用）',
    `target_type`       TINYINT         DEFAULT 1 COMMENT '目标类型 1=用户 2=话题 3=店铺',
    `target_id`         BIGINT          NOT NULL COMMENT '目标ID',
    `mutual`            TINYINT         DEFAULT 0 COMMENT '是否互关 0否 1是',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`           TINYINT         DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_target` (`user_id`, `target_type`, `target_id`),
    KEY `idx_target` (`target_type`, `target_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区关注表';

-- ============================================================
-- 10. 社区收藏表 (community_favorite)
-- ============================================================
CREATE TABLE IF NOT EXISTS `community_favorite` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`           BIGINT          NOT NULL COMMENT '用户ID',
    `post_id`           BIGINT          NOT NULL COMMENT '帖子ID',
    `folder_name`       VARCHAR(50)     DEFAULT '默认收藏' COMMENT '收藏夹名称',
    `remark`            VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP,
    `deleted`           TINYINT         DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_post` (`user_id`, `post_id`),
    KEY `idx_post_id` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区收藏表';

-- ============================================================
-- 11. 社区话题表 (community_topic)
-- ============================================================
CREATE TABLE IF NOT EXISTS `community_topic` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `topic_name`        VARCHAR(100)    NOT NULL COMMENT '话题名称',
    `topic_desc`        VARCHAR(500)    DEFAULT NULL COMMENT '话题描述',
    `cover_image`       VARCHAR(512)    DEFAULT NULL COMMENT '话题封面',
    `creator_id`        BIGINT          DEFAULT NULL COMMENT '创建人',
    `post_count`        INT             DEFAULT 0 COMMENT '帖子数',
    `follow_count`      INT             DEFAULT 0 COMMENT '关注数',
    `view_count`        BIGINT          DEFAULT 0 COMMENT '浏览数',
    `is_hot`            TINYINT         DEFAULT 0 COMMENT '是否热门 0否 1是',
    `is_official`       TINYINT         DEFAULT 0 COMMENT '是否官方 0否 1是',
    `sort_order`        INT             DEFAULT 0 COMMENT '排序',
    `status`            TINYINT         DEFAULT 1 COMMENT '状态 0=禁用 1=正常',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`           TINYINT         DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_topic_name` (`topic_name`),
    KEY `idx_is_hot` (`is_hot`),
    KEY `idx_post_count` (`post_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区话题表';

-- ============================================================
-- 12. 帖子话题关联表 (community_post_topic)
-- ============================================================
CREATE TABLE IF NOT EXISTS `community_post_topic` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `post_id`           BIGINT          NOT NULL COMMENT '帖子ID',
    `topic_id`          BIGINT          NOT NULL COMMENT '话题ID',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_topic` (`post_id`, `topic_id`),
    KEY `idx_topic_id` (`topic_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子话题关联表';

-- ============================================================
-- 13. 社区消息表 (community_message)
-- ============================================================
CREATE TABLE IF NOT EXISTS `community_message` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`           BIGINT          NOT NULL COMMENT '接收用户ID',
    `sender_id`         BIGINT          DEFAULT NULL COMMENT '发送者ID（系统消息为NULL）',
    `msg_type`          TINYINT         NOT NULL COMMENT '消息类型 1=评论 2=点赞 3=关注 4=@ 5=系统',
    `biz_type`          VARCHAR(20)     DEFAULT NULL COMMENT '业务类型 post/comment/like/follow',
    `biz_id`            BIGINT          DEFAULT NULL COMMENT '业务ID',
    `title`             VARCHAR(200)    DEFAULT NULL COMMENT '消息标题',
    `content`           TEXT            DEFAULT NULL COMMENT '消息内容',
    `is_read`           TINYINT         DEFAULT 0 COMMENT '是否已读 0否 1是',
    `read_time`         DATETIME        DEFAULT NULL COMMENT '已读时间',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_read` (`user_id`, `is_read`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区消息表';

-- ============================================================
-- 14. 用户屏蔽表 (community_block)
-- ============================================================
CREATE TABLE IF NOT EXISTS `community_block` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`           BIGINT          NOT NULL COMMENT '用户ID（屏蔽方）',
    `blocked_user_id`   BIGINT          NOT NULL COMMENT '被屏蔽用户ID',
    `reason`            VARCHAR(200)    DEFAULT NULL COMMENT '屏蔽原因',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_blocked` (`user_id`, `blocked_user_id`),
    KEY `idx_blocked_user` (`blocked_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户屏蔽表';

-- ============================================================
-- 15. 举报处理结果表 (community_report_action)
-- ============================================================
CREATE TABLE IF NOT EXISTS `community_report_action` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `report_id`         BIGINT          NOT NULL COMMENT '举报ID',
    `handler_id`        BIGINT          NOT NULL COMMENT '处理人ID',
    `action_type`       TINYINT         NOT NULL COMMENT '处理动作 1=警告 2=删除内容 3=禁言1天 4=禁言7天 5=封禁',
    `action_reason`     VARCHAR(500)    DEFAULT NULL COMMENT '处理原因',
    `action_days`       INT             DEFAULT 0 COMMENT '禁言/封禁天数（0=永久）',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_report_id` (`report_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='举报处理结果表';

-- ============================================================
-- 16. 帖子扩展字段
-- ============================================================
ALTER TABLE `community_post`
    ADD COLUMN IF NOT EXISTS `topic_ids`       VARCHAR(500)     DEFAULT NULL COMMENT '话题ID列表（逗号分隔）' AFTER `category_id`,
    ADD COLUMN IF NOT EXISTS `product_ids`     VARCHAR(500)     DEFAULT NULL COMMENT '关联商品ID列表（逗号分隔）' AFTER `topic_ids`,
    ADD COLUMN IF NOT EXISTS `view_count`      INT              DEFAULT 0 COMMENT '浏览数' AFTER `like_count`,
    ADD COLUMN IF NOT EXISTS `comment_count`   INT              DEFAULT 0 COMMENT '评论数' AFTER `view_count`,
    ADD COLUMN IF NOT EXISTS `share_count`     INT              DEFAULT 0 COMMENT '分享数' AFTER `comment_count`,
    ADD COLUMN IF NOT EXISTS `favorite_count`  INT              DEFAULT 0 COMMENT '收藏数' AFTER `share_count`,
    ADD COLUMN IF NOT EXISTS `is_top`          TINYINT          DEFAULT 0 COMMENT '是否置顶 0否 1是' AFTER `status`,
    ADD COLUMN IF NOT EXISTS `is_essence`      TINYINT          DEFAULT 0 COMMENT '是否精华 0否 1是' AFTER `is_top`,
    ADD COLUMN IF NOT EXISTS `audit_status`    TINYINT          DEFAULT 1 COMMENT '审核状态 0=待审 1=通过 2=拒绝' AFTER `is_essence`,
    ADD COLUMN IF NOT EXISTS `audit_time`      DATETIME         DEFAULT NULL COMMENT '审核时间' AFTER `audit_status`,
    ADD COLUMN IF NOT EXISTS `audit_user_id`   BIGINT           DEFAULT NULL COMMENT '审核人ID' AFTER `audit_time`,
    ADD COLUMN IF NOT EXISTS `location`        VARCHAR(200)     DEFAULT NULL COMMENT '发布地点' AFTER `audit_user_id`,
    ADD COLUMN IF NOT EXISTS `longitude`       DECIMAL(10,6)    DEFAULT NULL COMMENT '经度' AFTER `location`,
    ADD COLUMN IF NOT EXISTS `latitude`        DECIMAL(10,6)    DEFAULT NULL COMMENT '纬度' AFTER `longitude`,
    ADD KEY `idx_audit_status` (`audit_status`),
    ADD KEY `idx_is_top` (`is_top`),
    ADD KEY `idx_is_essence` (`is_essence`);

-- ============================================================
-- 17. 评论扩展字段
-- ============================================================
ALTER TABLE `community_comment`
    ADD COLUMN IF NOT EXISTS `parent_id`       BIGINT           DEFAULT NULL COMMENT '父评论ID（二级评论）' AFTER `post_id`,
    ADD COLUMN IF NOT EXISTS `reply_to_user_id` BIGINT          DEFAULT NULL COMMENT '被回复用户ID' AFTER `parent_id`,
    ADD COLUMN IF NOT EXISTS `like_count`      INT              DEFAULT 0 COMMENT '点赞数' AFTER `content`,
    ADD COLUMN IF NOT EXISTS `reply_count`     INT              DEFAULT 0 COMMENT '回复数' AFTER `like_count`,
    ADD COLUMN IF NOT EXISTS `status`          TINYINT          DEFAULT 1 COMMENT '状态 0=隐藏 1=显示 2=删除' AFTER `reply_count`,
    ADD COLUMN IF NOT EXISTS `audit_status`    TINYINT          DEFAULT 1 COMMENT '审核状态 0=待审 1=通过 2=拒绝' AFTER `status`,
    ADD KEY `idx_parent_id` (`parent_id`),
    ADD KEY `idx_audit_status` (`audit_status`);

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 回滚脚本（紧急情况使用）
-- ============================================================
/*
USE tailor_is_marketing;
DROP TABLE IF EXISTS `mkt_group_buy`;
DROP TABLE IF EXISTS `mkt_group_buy_instance`;
DROP TABLE IF EXISTS `mkt_group_buy_member`;
DROP TABLE IF EXISTS `mkt_promotion_rule`;
DROP TABLE IF EXISTS `mkt_promotion_step`;
DROP TABLE IF EXISTS `mkt_promotion_stats`;
DROP TABLE IF EXISTS `mkt_order_promotion`;
DROP TABLE IF EXISTS `mkt_sku_promotion_price`;

USE tailor_is_community;
DROP TABLE IF EXISTS `community_follow`;
DROP TABLE IF EXISTS `community_favorite`;
DROP TABLE IF EXISTS `community_topic`;
DROP TABLE IF EXISTS `community_post_topic`;
DROP TABLE IF EXISTS `community_message`;
DROP TABLE IF EXISTS `community_block`;
DROP TABLE IF EXISTS `community_report_action`;

ALTER TABLE `community_post`
    DROP COLUMN `topic_ids`,
    DROP COLUMN `product_ids`,
    DROP COLUMN `view_count`,
    DROP COLUMN `comment_count`,
    DROP COLUMN `share_count`,
    DROP COLUMN `favorite_count`,
    DROP COLUMN `is_top`,
    DROP COLUMN `is_essence`,
    DROP COLUMN `audit_status`,
    DROP COLUMN `audit_time`,
    DROP COLUMN `audit_user_id`,
    DROP COLUMN `location`,
    DROP COLUMN `longitude`,
    DROP COLUMN `latitude`;

ALTER TABLE `community_comment`
    DROP COLUMN `parent_id`,
    DROP COLUMN `reply_to_user_id`,
    DROP COLUMN `like_count`,
    DROP COLUMN `reply_count`,
    DROP COLUMN `status`,
    DROP COLUMN `audit_status`;
*/
