-- ============================================================
-- Tailor IS 平台 - 数据库分片迁移脚本
-- 文件: 10_sharding_migration.sql
-- 版本: 1.0.0
-- 说明: 创建分片表结构，支持 ShardingSphere-JDBC + TiDB
-- 创建时间: 2026-06-11
-- ============================================================

-- ⚠️ 警告：此脚本仅用于初始化部署，请勿在生产环境执行 DROP TABLE 操作
-- 生产环境数据库变更请使用 Flyway/Liquibase 版本化迁移工具管理

-- ============================================================
-- 一、订单系统分片表 (tailor_is_order)
-- 分片规则: 按 merchant_id 取模分片 (4 分片)
-- ============================================================

CREATE DATABASE IF NOT EXISTS `tailor_is_order` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `tailor_is_order`;

-- ============================================================
-- 1.1 order_info 分片表 (t_order_0 ~ t_order_3)
-- ============================================================

CREATE TABLE IF NOT EXISTS `t_order_0` LIKE `order_info`;
CREATE TABLE IF NOT EXISTS `t_order_1` LIKE `order_info`;
CREATE TABLE IF NOT EXISTS `t_order_2` LIKE `order_info`;
CREATE TABLE IF NOT EXISTS `t_order_3` LIKE `order_info`;

-- ============================================================
-- 1.2 order_item 分片表 (t_order_item_0 ~ t_order_item_3)
-- ============================================================

CREATE TABLE IF NOT EXISTS `t_order_item_0` LIKE `order_item`;
CREATE TABLE IF NOT EXISTS `t_order_item_1` LIKE `order_item`;
CREATE TABLE IF NOT EXISTS `t_order_item_2` LIKE `order_item`;
CREATE TABLE IF NOT EXISTS `t_order_item_3` LIKE `order_item`;

-- ============================================================
-- 1.3 shopping_cart 分片表 (t_shopping_cart_0 ~ t_shopping_cart_3)
-- ============================================================

CREATE TABLE IF NOT EXISTS `t_shopping_cart_0` LIKE `shopping_cart`;
CREATE TABLE IF NOT EXISTS `t_shopping_cart_1` LIKE `shopping_cart`;
CREATE TABLE IF NOT EXISTS `t_shopping_cart_2` LIKE `shopping_cart`;
CREATE TABLE IF NOT EXISTS `t_shopping_cart_3` LIKE `shopping_cart`;

-- ============================================================
-- 1.4 after_sale_ticket 分片表 (t_after_sale_ticket_0 ~ t_after_sale_ticket_3)
-- ============================================================

CREATE TABLE IF NOT EXISTS `t_after_sale_ticket_0` LIKE `after_sale_ticket`;
CREATE TABLE IF NOT EXISTS `t_after_sale_ticket_1` LIKE `after_sale_ticket`;
CREATE TABLE IF NOT EXISTS `t_after_sale_ticket_2` LIKE `after_sale_ticket`;
CREATE TABLE IF NOT EXISTS `t_after_sale_ticket_3` LIKE `after_sale_ticket`;

-- ============================================================
-- 二、支付系统分片表 (tailor_is_payment)
-- 分片规则: 按 merchant_id 取模分片 (4 分片)
-- ============================================================

CREATE DATABASE IF NOT EXISTS `tailor_is_payment` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `tailor_is_payment`;

-- ============================================================
-- 2.1 payment_record 分片表 (t_payment_record_0 ~ t_payment_record_3)
-- ============================================================

CREATE TABLE IF NOT EXISTS `t_payment_record_0` LIKE `payment_record`;
CREATE TABLE IF NOT EXISTS `t_payment_record_1` LIKE `payment_record`;
CREATE TABLE IF NOT EXISTS `t_payment_record_2` LIKE `payment_record`;
CREATE TABLE IF NOT EXISTS `t_payment_record_3` LIKE `payment_record`;

-- ============================================================
-- 2.2 refund_record 分片表 (t_refund_record_0 ~ t_refund_record_3)
-- ============================================================

CREATE TABLE IF NOT EXISTS `t_refund_record_0` LIKE `refund_record`;
CREATE TABLE IF NOT EXISTS `t_refund_record_1` LIKE `refund_record`;
CREATE TABLE IF NOT EXISTS `t_refund_record_2` LIKE `refund_record`;
CREATE TABLE IF NOT EXISTS `t_refund_record_3` LIKE `refund_record`;

-- ============================================================
-- 2.3 settlement_record 分片表 (t_settlement_record_0 ~ t_settlement_record_3)
-- ============================================================

CREATE TABLE IF NOT EXISTS `t_settlement_record_0` LIKE `settlement_record`;
CREATE TABLE IF NOT EXISTS `t_settlement_record_1` LIKE `settlement_record`;
CREATE TABLE IF NOT EXISTS `t_settlement_record_2` LIKE `settlement_record`;
CREATE TABLE IF NOT EXISTS `t_settlement_record_3` LIKE `settlement_record`;

-- ============================================================
-- 2.4 withdraw_record 分片表 (t_withdraw_record_0 ~ t_withdraw_record_3)
-- ============================================================

CREATE TABLE IF NOT EXISTS `t_withdraw_record_0` LIKE `withdraw_record`;
CREATE TABLE IF NOT EXISTS `t_withdraw_record_1` LIKE `withdraw_record`;
CREATE TABLE IF NOT EXISTS `t_withdraw_record_2` LIKE `withdraw_record`;
CREATE TABLE IF NOT EXISTS `t_withdraw_record_3` LIKE `withdraw_record`;

-- ============================================================
-- 2.5 account_transaction 分片表 (t_account_transaction_0 ~ t_account_transaction_3)
-- ============================================================

CREATE TABLE IF NOT EXISTS `t_account_transaction_0` LIKE `account_transaction`;
CREATE TABLE IF NOT EXISTS `t_account_transaction_1` LIKE `account_transaction`;
CREATE TABLE IF NOT EXISTS `t_account_transaction_2` LIKE `account_transaction`;
CREATE TABLE IF NOT EXISTS `t_account_transaction_3` LIKE `account_transaction`;

-- ============================================================
-- 三、数据迁移 (从原始表迁移到分片表)
-- ============================================================

-- 注意: 实际迁移应在业务低峰期执行，建议使用双写迁移方案
-- 以下脚本仅为示例，实际迁移请使用专门的迁移工具

-- 订单数据迁移示例
-- INSERT INTO t_order_0 SELECT * FROM order_info WHERE ABS(MOD(merchant_id, 4)) = 0;
-- INSERT INTO t_order_1 SELECT * FROM order_info WHERE ABS(MOD(merchant_id, 4)) = 1;
-- INSERT INTO t_order_2 SELECT * FROM order_info WHERE ABS(MOD(merchant_id, 4)) = 2;
-- INSERT INTO t_order_3 SELECT * FROM order_info WHERE ABS(MOD(merchant_id, 4)) = 3;

-- ============================================================
-- 四、回滚脚本
-- ============================================================

-- 执行回滚前请确认已备份数据

-- DROP TABLE IF EXISTS `t_order_0`;
-- DROP TABLE IF EXISTS `t_order_1`;
-- DROP TABLE IF EXISTS `t_order_2`;
-- DROP TABLE IF EXISTS `t_order_3`;

-- DROP TABLE IF EXISTS `t_order_item_0`;
-- DROP TABLE IF EXISTS `t_order_item_1`;
-- DROP TABLE IF EXISTS `t_order_item_2`;
-- DROP TABLE IF EXISTS `t_order_item_3`;

-- DROP TABLE IF EXISTS `t_shopping_cart_0`;
-- DROP TABLE IF EXISTS `t_shopping_cart_1`;
-- DROP TABLE IF EXISTS `t_shopping_cart_2`;
-- DROP TABLE IF EXISTS `t_shopping_cart_3`;

-- DROP TABLE IF EXISTS `t_after_sale_ticket_0`;
-- DROP TABLE IF EXISTS `t_after_sale_ticket_1`;
-- DROP TABLE IF EXISTS `t_after_sale_ticket_2`;
-- DROP TABLE IF EXISTS `t_after_sale_ticket_3`;

-- DROP TABLE IF EXISTS `t_payment_record_0`;
-- DROP TABLE IF EXISTS `t_payment_record_1`;
-- DROP TABLE IF EXISTS `t_payment_record_2`;
-- DROP TABLE IF EXISTS `t_payment_record_3`;

-- DROP TABLE IF EXISTS `t_refund_record_0`;
-- DROP TABLE IF EXISTS `t_refund_record_1`;
-- DROP TABLE IF EXISTS `t_refund_record_2`;
-- DROP TABLE IF EXISTS `t_refund_record_3`;

-- DROP TABLE IF EXISTS `t_settlement_record_0`;
-- DROP TABLE IF EXISTS `t_settlement_record_1`;
-- DROP TABLE IF EXISTS `t_settlement_record_2`;
-- DROP TABLE IF EXISTS `t_settlement_record_3`;

-- DROP TABLE IF EXISTS `t_withdraw_record_0`;
-- DROP TABLE IF EXISTS `t_withdraw_record_1`;
-- DROP TABLE IF EXISTS `t_withdraw_record_2`;
-- DROP TABLE IF EXISTS `t_withdraw_record_3`;

-- DROP TABLE IF EXISTS `t_account_transaction_0`;
-- DROP TABLE IF EXISTS `t_account_transaction_1`;
-- DROP TABLE IF EXISTS `t_account_transaction_2`;
-- DROP TABLE IF EXISTS `t_account_transaction_3`;

-- ============================================================
-- 五、验证脚本
-- ============================================================

-- 验证分片表是否创建成功
-- SELECT table_name, table_rows, data_length, index_length
-- FROM information_schema.tables
-- WHERE table_schema = 'tailor_is_order' AND table_name LIKE 't_order_%'
-- ORDER BY table_name;

-- SELECT table_name, table_rows, data_length, index_length
-- FROM information_schema.tables
-- WHERE table_schema = 'tailor_is_payment' AND table_name LIKE 't_%'
-- ORDER BY table_name;
