-- ============================================================
-- Tailor IS - Flyway Migration V2: 质量评估修复汇总
-- 说明: 包含U-001购物车唯一键修复、U-005/F-005/F-013 BaseEntity字段补全
-- 日期: 2026-07-09
-- ============================================================

-- ============================================================
-- 1. U-001: 购物车唯一键约束修复（包含deleted字段）
-- 数据库: tailor_is_order
-- ============================================================
-- 注意：如果表已存在且uk_user_sku不包含deleted字段，执行以下操作
-- ALTER TABLE `shopping_cart` DROP INDEX `uk_user_sku`;
-- ALTER TABLE `shopping_cart` ADD UNIQUE KEY `uk_user_sku` (`user_id`, `sku_id`, `deleted`);

-- ============================================================
-- 2. U-005: 86张表批量添加 created_by / updated_by 字段
-- 以下为各数据库缺失字段的表清单及修复SQL
-- ============================================================

-- 2.1 tailor_is_merchant (4张表)
-- ALTER TABLE `tailor_is_merchant`.`merchant` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_merchant`.`merchant_employee` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_merchant`.`merchant_qualification` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_merchant`.`merchant_shop` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;

-- 2.2 tailor_is_product (4张表)
-- ALTER TABLE `tailor_is_product`.`product_attribute` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_product`.`product_review` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_product`.`product_tag` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_product`.`product_tag_mapping` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;

-- 2.3 tailor_is_order (16张表，含分片表)
-- ALTER TABLE `tailor_is_order`.`after_sale_ticket` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_order`.`order_logistics` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- 分片表: t_after_sale_ticket_0~3, t_order_0~3, t_order_item_0~3, t_shopping_cart_0~3 (各4张)

-- 2.4 tailor_is_payment (22张表，含分片表)
-- ALTER TABLE `tailor_is_payment`.`account_transaction` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_payment`.`merchant_account` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_payment`.`payment_record` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_payment`.`quality_deposit` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_payment`.`recharge_record` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_payment`.`refund_record` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_payment`.`settlement_record` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_payment`.`user_account` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_payment`.`withdraw_record` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- 分片表: t_account_transaction_0~3, t_payment_record_0~3, t_refund_record_0~3, t_settlement_record_0~3, t_withdraw_record_0~3

-- 2.5 tailor_is_marketing (9张表)
-- ALTER TABLE `tailor_is_marketing`.`checkin_record` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_marketing`.`coupon_template` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_marketing`.`member_level` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_marketing`.`points_mall_product` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_marketing`.`points_record` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_marketing`.`seckill_activity` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_marketing`.`seckill_product` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_marketing`.`shop_member` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_marketing`.`user_coupon` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;

-- 2.6 tailor_is_copyright (5张表)
-- ALTER TABLE `tailor_is_copyright`.`arbitration_record` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_copyright`.`copyright_appeal` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_copyright`.`copyright_license` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_copyright`.`copyright_record` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_copyright`.`infringement_record` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;

-- 2.7 tailor_is_community (6张表)
-- ALTER TABLE `tailor_is_community`.`community_category` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_community`.`community_collect` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_community`.`community_comment` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_community`.`community_like` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_community`.`community_report` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_community`.`user_mute_record` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;

-- 2.8 tailor_is_supply (5张表)
-- ALTER TABLE `tailor_is_supply`.`supplier_directory` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_supply`.`supply_contact_record` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_supply`.`supply_demand_post` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_supply`.`supply_match_record` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_supply`.`supply_review` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;

-- 2.9 tailor_is_message (6张表)
-- ALTER TABLE `tailor_is_message`.`message_content` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_message`.`message_sensitive_log` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_message`.`message_session` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_message`.`sensitive_word` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_message`.`user_notification` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;
-- ALTER TABLE `tailor_is_message`.`system_notification` ADD COLUMN `created_by` VARCHAR(64) NULL, ADD COLUMN `updated_by` VARCHAR(64) NULL;

-- ============================================================
-- 3. F-005: order_item表添加created_by/updated_by字段
-- 数据库: tailor_is_order
-- ============================================================
-- ALTER TABLE `order_item` ADD COLUMN `created_by` VARCHAR(64) NULL;
-- ALTER TABLE `order_item` ADD COLUMN `updated_by` VARCHAR(64) NULL;

-- ============================================================
-- 4. F-013: product_sku表添加缺失字段
-- 数据库: tailor_is_product
-- ============================================================
-- ALTER TABLE `product_sku` ADD COLUMN `created_by` VARCHAR(64) NULL AFTER `updated_at`, ADD COLUMN `updated_by` VARCHAR(64) NULL AFTER `created_by`;