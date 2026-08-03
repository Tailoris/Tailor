-- ============================================================
-- Tailor IS 平台 - 商家系统优化与扩展
-- 文件: 13_merchant_optimization.sql
-- 版本: 2.0.0
-- 数据库: MySQL 8.0
-- 创建时间: 2026-08-02
-- 说明: 店铺分类、入驻类型、商家修改申请、保障金流水等
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `tailor_is_merchant` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `tailor_is_merchant`;

-- ============================================================
-- 1. 店铺分类表 (shop_category)
-- ============================================================
CREATE TABLE IF NOT EXISTS `shop_category` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分类ID（主键）',
    `category_name`     VARCHAR(64)     NOT NULL COMMENT '分类名称',
    `parent_id`         BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父级分类ID（0为一级分类，支持2级树）',
    `category_code`     VARCHAR(32)     DEFAULT NULL COMMENT '分类编码，如 FEMALE_DRESS',
    `sort_order`        INT             NOT NULL DEFAULT 0 COMMENT '排序（值小靠前）',
    `commission_rate`   DECIMAL(5,4)    NOT NULL DEFAULT 0.0500 COMMENT '该分类平台佣金费率',
    `deposit_min`       DECIMAL(14,2)   NOT NULL DEFAULT 5000.00 COMMENT '该分类最低保障金',
    `icon`              VARCHAR(256)    DEFAULT NULL COMMENT '分类图标URL',
    `description`       VARCHAR(256)    DEFAULT NULL COMMENT '分类描述',
    `status`            TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `deleted`           TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `created_by`        VARCHAR(64)     DEFAULT NULL COMMENT '创建人',
    `updated_by`        VARCHAR(64)     DEFAULT NULL COMMENT '更新人',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_name` (`category_name`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_status` (`status`),
    KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='店铺分类表';

-- ============================================================
-- 2. 入驻类型配置表 (apply_type)
-- ============================================================
CREATE TABLE IF NOT EXISTS `apply_type` (
    `id`                    BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `type_code`             VARCHAR(32)     NOT NULL COMMENT '类型编码：PERSONAL/ENTERPRISE/BRAND/FACTORY/SELF_OP',
    `type_name`             VARCHAR(64)     NOT NULL COMMENT '类型名称：个人店/企业店/品牌店/工厂店/自营店',
    `required_qualifications` JSON          DEFAULT NULL COMMENT '必传资质类型数组，如 [1,2,3]（对应merchant_qualification.cert_type）',
    `deposit_amount`        DECIMAL(14,2)   NOT NULL DEFAULT 0.00 COMMENT '基础保障金',
    `commission_extra_rate` DECIMAL(5,4)    NOT NULL DEFAULT 0.0000 COMMENT '额外加成佣金',
    `audit_required`        TINYINT         NOT NULL DEFAULT 1 COMMENT '是否需人工审核：0-否，1-是',
    `trial_days`            INT             NOT NULL DEFAULT 30 COMMENT '免费试用天数',
    `sort_order`            INT             NOT NULL DEFAULT 0 COMMENT '排序',
    `description`           VARCHAR(256)    DEFAULT NULL COMMENT '描述',
    `status`                TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `deleted`               TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `created_by`            VARCHAR(64)     DEFAULT NULL COMMENT '创建人',
    `updated_by`            VARCHAR(64)     DEFAULT NULL COMMENT '更新人',
    `created_at`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_code` (`type_code`),
    KEY `idx_status` (`status`),
    KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='入驻类型配置表';

-- ============================================================
-- 3. 商家分类关联表 (merchant_category_rel)
-- ============================================================
CREATE TABLE IF NOT EXISTS `merchant_category_rel` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `merchant_id`       BIGINT          NOT NULL COMMENT '商家ID',
    `category_id`       BIGINT          NOT NULL COMMENT '分类ID（关联shop_category.id）',
    `is_primary`        TINYINT         NOT NULL DEFAULT 0 COMMENT '是否主营分类：0-否，1-是',
    `audit_status`      TINYINT         NOT NULL DEFAULT 0 COMMENT '审核状态：0-待审，1-通过，2-驳回',
    `audit_remark`      VARCHAR(256)    DEFAULT NULL COMMENT '审核备注',
    `audit_by`          BIGINT          DEFAULT NULL COMMENT '审核人ID',
    `audit_time`        DATETIME        DEFAULT NULL COMMENT '审核时间',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_merchant_category` (`merchant_id`, `category_id`),
    KEY `idx_merchant` (`merchant_id`),
    KEY `idx_category` (`category_id`),
    KEY `idx_audit_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商家分类关联表';

-- ============================================================
-- 4. 商家修改申请表 (merchant_modify_apply)
-- ============================================================
CREATE TABLE IF NOT EXISTS `merchant_modify_apply` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `apply_no`          VARCHAR(64)     NOT NULL COMMENT '申请单号：MA+YYYYMMDDNNNN',
    `merchant_id`       BIGINT          NOT NULL COMMENT '商家ID',
    `shop_id`           BIGINT          DEFAULT NULL COMMENT '店铺ID',
    `apply_type`        TINYINT         NOT NULL COMMENT '申请类型：1-基本信息修改，2-店铺信息修改，3-资质变更，4-分类变更，5-银行信息变更',
    `modify_content`    JSON            NOT NULL COMMENT '修改的字段键值对，如 {"contact_name":"张三","phone":"xxx"}',
    `original_snapshot` JSON            DEFAULT NULL COMMENT '修改前快照（留痕）',
    `reason`            VARCHAR(512)    DEFAULT NULL COMMENT '修改原因',
    `attachments`       JSON            DEFAULT NULL COMMENT '附件URL数组',
    `status`            TINYINT         NOT NULL DEFAULT 0 COMMENT '状态：0-待审核，1-审核通过，2-已驳回，3-已撤回',
    `audit_remark`      VARCHAR(512)    DEFAULT NULL COMMENT '审核备注',
    `audit_by`          BIGINT          DEFAULT NULL COMMENT '审核人ID',
    `audit_time`        DATETIME        DEFAULT NULL COMMENT '审核时间',
    `submitted_by`      BIGINT          DEFAULT NULL COMMENT '提交人（商家用户ID）',
    `deleted`           TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `created_by`        VARCHAR(64)     DEFAULT NULL COMMENT '创建人',
    `updated_by`        VARCHAR(64)     DEFAULT NULL COMMENT '更新人',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_apply_no` (`apply_no`),
    KEY `idx_merchant_id` (`merchant_id`),
    KEY `idx_status` (`status`),
    KEY `idx_apply_type` (`apply_type`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商家修改申请表';

-- ============================================================
-- 5. 商家信息修改日志表 (merchant_modify_log)
-- ============================================================
CREATE TABLE IF NOT EXISTS `merchant_modify_log` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `merchant_id`       BIGINT          NOT NULL COMMENT '商家ID',
    `shop_id`           BIGINT          DEFAULT NULL COMMENT '店铺ID',
    `operator_id`       BIGINT          NOT NULL COMMENT '操作人ID（商家员工或平台管理员）',
    `operator_type`     TINYINT         NOT NULL COMMENT '操作人类型：1-商家，2-平台管理员',
    `operate_type`      TINYINT         NOT NULL COMMENT '操作类型：1-新增，2-修改，3-删除，4-审核通过，5-审核驳回，6-冻结，7-解冻，8-保证金变更，9-提现，10-结算',
    `operate_module`    VARCHAR(64)     DEFAULT NULL COMMENT '操作模块：merchant/shop/qualification/deposit/withdraw/settlement等',
    `field_name`        VARCHAR(64)     DEFAULT NULL COMMENT '修改字段名',
    `value_before`      TEXT            DEFAULT NULL COMMENT '修改前值（JSON或原始值）',
    `value_after`       TEXT            DEFAULT NULL COMMENT '修改后值（JSON或原始值）',
    `remark`            VARCHAR(512)    DEFAULT NULL COMMENT '备注',
    `ip`                VARCHAR(45)     DEFAULT NULL COMMENT '操作IP',
    `user_agent`        VARCHAR(512)    DEFAULT NULL COMMENT 'User-Agent',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_merchant` (`merchant_id`),
    KEY `idx_operator` (`operator_id`),
    KEY `idx_operate_type` (`operate_type`),
    KEY `idx_operate_module` (`operate_module`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商家信息修改日志/审计轨迹';

-- ============================================================
-- 6. 保障金流水表 (deposit_flow)
-- ============================================================
CREATE TABLE IF NOT EXISTS `deposit_flow` (
    `id`                    BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `flow_no`               VARCHAR(64)     NOT NULL COMMENT '流水号：DF+YYYYMMDDNNNNNN',
    `merchant_id`           BIGINT          NOT NULL COMMENT '商家ID',
    `quality_deposit_id`    BIGINT          NOT NULL COMMENT '关联quality_deposit表ID',
    `flow_type`             TINYINT         NOT NULL COMMENT '流水类型：1-缴纳，2-补缴，3-平台扣除赔付，4-退还，5-保证金释放，6-调增，7-调减',
    `amount`                DECIMAL(14,2)   NOT NULL COMMENT '变动金额：收入为正，支出为负',
    `balance_before`        DECIMAL(14,2)   DEFAULT NULL COMMENT '变动前deposit_amount余额',
    `balance_after`         DECIMAL(14,2)   DEFAULT NULL COMMENT '变动后deposit_amount余额',
    `related_type`          TINYINT         DEFAULT NULL COMMENT '关联类型：1-商家入驻，2-修改申请，3-售后工单，4-违规处罚，5-提现结算，6-手动调整',
    `related_id`            BIGINT          DEFAULT NULL COMMENT '关联业务ID',
    `related_no`            VARCHAR(64)     DEFAULT NULL COMMENT '关联业务单号',
    `operator_id`           BIGINT          DEFAULT NULL COMMENT '操作人ID（平台管理员或商家）',
    `operator_type`         TINYINT         DEFAULT NULL COMMENT '操作人类型：1-商家，2-平台',
    `remark`                VARCHAR(256)    DEFAULT NULL COMMENT '备注',
    `created_at`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_flow_no` (`flow_no`),
    KEY `idx_merchant` (`merchant_id`),
    KEY `idx_quality_deposit_id` (`quality_deposit_id`),
    KEY `idx_flow_type` (`flow_type`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='保障金流水表';

-- ============================================================
-- 7. merchant 表新增字段
-- ============================================================
ALTER TABLE `merchant`
    ADD COLUMN `shop_category_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '主营店铺分类ID（冗余，关联shop_category.id）' AFTER `business_scope`,
    ADD COLUMN `apply_type_code` VARCHAR(32) DEFAULT NULL COMMENT '入驻类型编码（关联apply_type.type_code）' AFTER `shop_category_id`,
    ADD COLUMN `deposit_paid` TINYINT NOT NULL DEFAULT 0 COMMENT '保障金是否已足额缴纳：0-未缴/不足，1-已足额' AFTER `apply_type_code`;

-- ============================================================
-- 8. merchant_qualification 表新增字段
-- ============================================================
ALTER TABLE `merchant_qualification`
    ADD COLUMN `merchant_modify_apply_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '如果来自修改申请则关联ID' AFTER `audit_by`;

SET FOREIGN_KEY_CHECKS = 1;
