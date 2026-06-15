-- ============================================================
-- Sprint 8.1 商户服务数据库迁移脚本
-- 任务: MER-005 数据工作台 + MER-006 试运营考核 + MER-007 违规处罚
-- 执行环境: dev / staging / prod
-- 风险评估: 低（仅新增表/字段，不修改现有数据）
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

USE `tailor_is_merchant`;

-- ============================================================
-- 1. 商家数据工作台统计表 (merchant_dashboard_stats)
-- ============================================================
CREATE TABLE IF NOT EXISTS `merchant_dashboard_stats` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `merchant_id`       BIGINT          NOT NULL COMMENT '商家ID',
    `shop_id`           BIGINT          DEFAULT NULL COMMENT '店铺ID（为空表示全店汇总）',
    `stat_date`         DATE            NOT NULL COMMENT '统计日期',
    `stat_type`         TINYINT         NOT NULL DEFAULT 1 COMMENT '统计类型 1=日 2=周 3=月',
    `pv_count`          BIGINT          DEFAULT 0 COMMENT '浏览量PV',
    `uv_count`          BIGINT          DEFAULT 0 COMMENT '访客数UV',
    `product_view_count` BIGINT         DEFAULT 0 COMMENT '商品详情浏览数',
    `shop_follow_count` BIGINT          DEFAULT 0 COMMENT '店铺关注数',
    `cart_add_count`    BIGINT          DEFAULT 0 COMMENT '加购数',
    `order_count`       BIGINT          DEFAULT 0 COMMENT '订单数',
    `order_amount`      DECIMAL(15,2)   DEFAULT 0 COMMENT '订单金额',
    `paid_order_count`  BIGINT          DEFAULT 0 COMMENT '已支付订单数',
    `paid_order_amount` DECIMAL(15,2)   DEFAULT 0 COMMENT '已支付订单金额',
    `refund_count`      BIGINT          DEFAULT 0 COMMENT '退款单数',
    `refund_amount`     DECIMAL(15,2)   DEFAULT 0 COMMENT '退款金额',
    `conversion_rate`   DECIMAL(5,4)    DEFAULT 0 COMMENT '转化率',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_merchant_shop_date_type` (`merchant_id`, `shop_id`, `stat_date`, `stat_type`),
    KEY `idx_merchant_id` (`merchant_id`),
    KEY `idx_shop_id` (`shop_id`),
    KEY `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家数据工作台统计';

-- ============================================================
-- 2. 试运营考核记录表 (merchant_trial_assessment)
-- ============================================================
CREATE TABLE IF NOT EXISTS `merchant_trial_assessment` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `merchant_id`       BIGINT          NOT NULL COMMENT '商家ID',
    `trial_start_date`  DATE            NOT NULL COMMENT '试运营开始日期',
    `trial_end_date`    DATE            NOT NULL COMMENT '试运营结束日期',
    `assessment_date`   DATE            DEFAULT NULL COMMENT '考核日期',
    `total_days`        INT             DEFAULT 30 COMMENT '试运营总天数',
    `actual_days`       INT             DEFAULT 0 COMMENT '实际运营天数',
    `order_count`       BIGINT          DEFAULT 0 COMMENT '期间订单数',
    `order_amount`      DECIMAL(15,2)   DEFAULT 0 COMMENT '期间订单金额',
    `product_count`     BIGINT          DEFAULT 0 COMMENT '上架商品数',
    `refund_rate`       DECIMAL(5,4)    DEFAULT 0 COMMENT '退款率',
    `complaint_count`   BIGINT          DEFAULT 0 COMMENT '投诉数',
    `violation_count`   BIGINT          DEFAULT 0 COMMENT '违规次数',
    `score`             DECIMAL(5,2)    DEFAULT 0 COMMENT '综合得分（0-100）',
    `result`            TINYINT         DEFAULT 0 COMMENT '考核结果 0=待考核 1=通过 2=未通过 3=延期',
    `is_promoted`       TINYINT         DEFAULT 0 COMMENT '是否已转正 0否 1是',
    `promote_time`      DATETIME        DEFAULT NULL COMMENT '转正时间',
    `remark`            VARCHAR(500)    DEFAULT NULL COMMENT '考核备注',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_merchant_id` (`merchant_id`),
    KEY `idx_trial_end_date` (`trial_end_date`),
    KEY `idx_result` (`result`),
    KEY `idx_assessment_date` (`assessment_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试运营考核记录';

-- ============================================================
-- 3. 商家违规记录表 (merchant_violation)
-- ============================================================
CREATE TABLE IF NOT EXISTS `merchant_violation` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `merchant_id`       BIGINT          NOT NULL COMMENT '商家ID',
    `shop_id`           BIGINT          DEFAULT NULL COMMENT '关联店铺ID',
    `violation_type`    TINYINT         NOT NULL COMMENT '违规类型 1=商品违规 2=价格违规 3=虚假宣传 4=售后违规 5=资质过期 6=其他',
    `violation_level`   TINYINT         NOT NULL COMMENT '违规级别 1=轻微 2=一般 3=严重 4=特别严重',
    `title`             VARCHAR(200)    NOT NULL COMMENT '违规标题',
    `description`       VARCHAR(2000)   NOT NULL COMMENT '违规描述',
    `evidence`          TEXT            DEFAULT NULL COMMENT '违规证据（JSON）',
    `punishment_type`   TINYINT         DEFAULT 0 COMMENT '处罚类型 0=待定 1=警告 2=限流 3=下架 4=封禁 5=清退',
    `punishment_days`   INT             DEFAULT 0 COMMENT '处罚天数（0=永久）',
    `punishment_start`  DATETIME        DEFAULT NULL COMMENT '处罚开始时间',
    `punishment_end`    DATETIME        DEFAULT NULL COMMENT '处罚结束时间',
    `status`            TINYINT         DEFAULT 0 COMMENT '状态 0=待处理 1=已处罚 2=已申诉 3=已撤销 4=已解除',
    `is_appealed`       TINYINT         DEFAULT 0 COMMENT '是否申诉 0否 1是',
    `appeal_content`    VARCHAR(2000)   DEFAULT NULL COMMENT '申诉内容',
    `appeal_time`       DATETIME        DEFAULT NULL COMMENT '申诉时间',
    `appeal_result`     VARCHAR(500)    DEFAULT NULL COMMENT '申诉处理结果',
    `reporter_id`       BIGINT          DEFAULT NULL COMMENT '举报人ID（系统举报为空）',
    `handler_id`        BIGINT          DEFAULT NULL COMMENT '处理人ID',
    `handle_time`       DATETIME        DEFAULT NULL COMMENT '处理时间',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_merchant_id` (`merchant_id`),
    KEY `idx_shop_id` (`shop_id`),
    KEY `idx_violation_type` (`violation_type`),
    KEY `idx_violation_level` (`violation_level`),
    KEY `idx_status` (`status`),
    KEY `idx_punishment_end` (`punishment_end`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家违规处罚记录';

-- ============================================================
-- 4. 商家员工权限模板表 (merchant_role_template)
-- ============================================================
CREATE TABLE IF NOT EXISTS `merchant_role_template` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_code`         VARCHAR(50)     NOT NULL COMMENT '角色代码',
    `role_name`         VARCHAR(50)     NOT NULL COMMENT '角色名称',
    `role_type`         TINYINT         NOT NULL COMMENT '角色类型 1=系统预设 2=商家自定义',
    `merchant_id`       BIGINT          DEFAULT NULL COMMENT '商家ID（自定义角色时填写）',
    `permissions`       JSON            NOT NULL COMMENT '权限列表（JSON数组）',
    `description`       VARCHAR(500)    DEFAULT NULL COMMENT '角色描述',
    `sort_order`        INT             DEFAULT 0 COMMENT '排序',
    `is_enabled`        TINYINT         DEFAULT 1 COMMENT '是否启用 0否 1是',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`           TINYINT         DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (`id`),
    KEY `idx_role_code` (`role_code`),
    KEY `idx_merchant_id` (`merchant_id`),
    KEY `idx_role_type` (`role_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家员工角色权限模板';

-- ============================================================
-- 5. 商家当前切换店铺表 (merchant_current_shop)
-- ============================================================
CREATE TABLE IF NOT EXISTS `merchant_current_shop` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`           BIGINT          NOT NULL COMMENT '用户ID',
    `merchant_id`       BIGINT          NOT NULL COMMENT '商家ID',
    `current_shop_id`   BIGINT          DEFAULT NULL COMMENT '当前操作的店铺ID',
    `last_switch_time`  DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近切换时间',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_merchant` (`user_id`, `merchant_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_current_shop_id` (`current_shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家当前操作店铺';

-- ============================================================
-- 6. 商家员工角色关系扩展字段
-- ============================================================
ALTER TABLE `merchant_employee`
    ADD COLUMN IF NOT EXISTS `role_code`     VARCHAR(50)     DEFAULT NULL COMMENT '角色代码（关联merchant_role_template）' AFTER `role`,
    ADD COLUMN IF NOT EXISTS `shop_ids`      VARCHAR(500)    DEFAULT NULL COMMENT '可访问店铺ID列表（逗号分隔，空=全部）' AFTER `permissions`,
    ADD COLUMN IF NOT EXISTS `last_active_time` DATETIME      DEFAULT NULL COMMENT '最后活跃时间' AFTER `last_login_time`,
    ADD COLUMN IF NOT EXISTS `login_count`   INT             DEFAULT 0 COMMENT '登录次数' AFTER `last_active_time`,
    ADD KEY `idx_role_code` (`role_code`);

-- ============================================================
-- 7. 商家表扩展字段（试运营/违规/转正）
-- ============================================================
ALTER TABLE `merchant`
    ADD COLUMN IF NOT EXISTS `is_trial`      TINYINT         DEFAULT 1 COMMENT '是否试运营 0否 1是' AFTER `status`,
    ADD COLUMN IF NOT EXISTS `trial_start_date` DATE         DEFAULT NULL COMMENT '试运营开始日期' AFTER `is_trial`,
    ADD COLUMN IF NOT EXISTS `trial_end_date`   DATE         DEFAULT NULL COMMENT '试运营结束日期' AFTER `trial_start_date`,
    ADD COLUMN IF NOT EXISTS `is_promoted`   TINYINT         DEFAULT 0 COMMENT '是否已转正 0否 1是' AFTER `trial_end_date`,
    ADD COLUMN IF NOT EXISTS `promote_time`  DATETIME        DEFAULT NULL COMMENT '转正时间' AFTER `is_promoted`,
    ADD COLUMN IF NOT EXISTS `violation_score` INT           DEFAULT 0 COMMENT '违规扣分（满分100）' AFTER `promote_time`,
    ADD COLUMN IF NOT EXISTS `punishment_status` TINYINT     DEFAULT 0 COMMENT '当前处罚状态 0=正常 1=限流 2=下架 3=封禁' AFTER `violation_score`,
    ADD COLUMN IF NOT EXISTS `punishment_end`  DATETIME      DEFAULT NULL COMMENT '处罚结束时间' AFTER `punishment_status`,
    ADD KEY `idx_is_trial` (`is_trial`),
    ADD KEY `idx_trial_end_date` (`trial_end_date`),
    ADD KEY `idx_punishment_status` (`punishment_status`);

-- ============================================================
-- 8. 初始化系统预设角色模板
-- ============================================================
INSERT IGNORE INTO `merchant_role_template` (`role_code`, `role_name`, `role_type`, `permissions`, `description`, `sort_order`, `is_enabled`) VALUES
('shop_manager', '店长', 1,
 JSON_ARRAY(
   'product:create','product:update','product:delete','product:list',
   'order:list','order:detail','order:refund','order:export',
   'employee:list','employee:add','employee:remove',
   'shop:update','shop:decoration','shop:settings',
   'data:dashboard','data:export','finance:settle','finance:bill',
   'review:reply','review:feature','review:hide'
 ), '店长：拥有店铺全部管理权限', 1, 1),
('operator', '运营', 1,
 JSON_ARRAY(
   'product:create','product:update','product:list',
   'order:list','order:detail',
   'shop:update','shop:decoration',
   'data:dashboard',
   'review:reply'
 ), '运营：商品+订单+装修', 2, 1),
('customer_service', '客服', 1,
 JSON_ARRAY(
   'order:list','order:detail',
   'review:reply','review:hide',
   'data:dashboard'
 ), '客服：订单+评价回复', 3, 1),
('warehouse', '库管', 1,
 JSON_ARRAY(
   'product:list','product:update',
   'order:list','order:detail'
 ), '库管：商品库存+发货', 4, 1),
('finance', '财务', 1,
 JSON_ARRAY(
   'order:list','order:detail','order:export',
   'finance:settle','finance:bill','finance:export',
   'data:dashboard'
 ), '财务：订单+结算+对账', 5, 1);

-- ============================================================
-- 9. 初始化权限按钮字典（前端按钮级别控制用）
-- ============================================================
CREATE TABLE IF NOT EXISTS `merchant_permission_dict` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `permission_code`   VARCHAR(100)    NOT NULL COMMENT '权限代码',
    `permission_name`   VARCHAR(100)    NOT NULL COMMENT '权限名称',
    `module`            VARCHAR(50)     NOT NULL COMMENT '所属模块',
    `description`       VARCHAR(200)    DEFAULT NULL COMMENT '权限描述',
    `sort_order`        INT             DEFAULT 0 COMMENT '排序',
    `is_enabled`        TINYINT         DEFAULT 1 COMMENT '是否启用',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`permission_code`),
    KEY `idx_module` (`module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家权限按钮字典';

INSERT IGNORE INTO `merchant_permission_dict` (`permission_code`, `permission_name`, `module`, `description`, `sort_order`) VALUES
-- 商品模块
('product:create', '新建商品', 'product', '创建新商品按钮', 101),
('product:update', '编辑商品', 'product', '编辑商品按钮', 102),
('product:delete', '删除商品', 'product', '删除商品按钮', 103),
('product:list', '查看商品列表', 'product', '商品列表查看', 104),
('product:audit', '商品审核', 'product', '商品审核按钮', 105),
-- 订单模块
('order:list', '查看订单', 'order', '订单列表', 201),
('order:detail', '订单详情', 'order', '查看订单详情', 202),
('order:refund', '订单退款', 'order', '处理退款按钮', 203),
('order:export', '导出订单', 'order', '导出订单按钮', 204),
('order:ship', '订单发货', 'order', '订单发货按钮', 205),
-- 员工模块
('employee:list', '查看员工', 'employee', '员工列表', 301),
('employee:add', '添加员工', 'employee', '添加员工按钮', 302),
('employee:remove', '移除员工', 'employee', '移除员工按钮', 303),
('employee:permission', '设置权限', 'employee', '权限设置按钮', 304),
-- 店铺模块
('shop:update', '编辑店铺', 'shop', '店铺基本信息', 401),
('shop:decoration', '店铺装修', 'shop', '店铺装修入口', 402),
('shop:settings', '店铺设置', 'shop', '店铺设置按钮', 403),
-- 数据模块
('data:dashboard', '数据工作台', 'data', '查看数据工作台', 501),
('data:export', '数据导出', 'data', '数据导出按钮', 502),
-- 财务模块
('finance:settle', '结算管理', 'finance', '结算列表', 601),
('finance:bill', '账单管理', 'finance', '账单查询', 602),
('finance:export', '账单导出', 'finance', '账单导出', 603),
-- 评价模块
('review:reply', '评价回复', 'review', '商家回复评价', 701),
('review:feature', '精选评价', 'review', '设置精选', 702),
('review:hide', '隐藏评价', 'review', '隐藏评价', 703);

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 回滚脚本（紧急情况使用）
-- ============================================================
/*
DROP TABLE IF EXISTS `merchant_dashboard_stats`;
DROP TABLE IF EXISTS `merchant_trial_assessment`;
DROP TABLE IF EXISTS `merchant_violation`;
DROP TABLE IF EXISTS `merchant_role_template`;
DROP TABLE IF EXISTS `merchant_current_shop`;
DROP TABLE IF EXISTS `merchant_permission_dict`;

ALTER TABLE `merchant_employee`
    DROP COLUMN `role_code`,
    DROP COLUMN `last_active_time`,
    DROP COLUMN `login_count`;

ALTER TABLE `merchant`
    DROP COLUMN `is_trial`,
    DROP COLUMN `trial_start_date`,
    DROP COLUMN `trial_end_date`,
    DROP COLUMN `is_promoted`,
    DROP COLUMN `promote_time`,
    DROP COLUMN `violation_score`,
    DROP COLUMN `punishment_status`,
    DROP COLUMN `punishment_end`;
*/
