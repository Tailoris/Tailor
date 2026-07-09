-- ============================================================
-- Tailor IS - 修复购物车唯一键约束
-- 文件: V10_1__fix_shopping_cart_unique_key.sql
-- 版本: 1.0.0
-- 说明: 将 uk_user_sku 唯一键增加 deleted 字段，解决逻辑删除后
--       无法重新添加同一SKU的问题
-- ============================================================

USE `tailor_is_order`;

-- 1. 删除旧唯一键
ALTER TABLE `shopping_cart` DROP INDEX `uk_user_sku`;

-- 2. 创建新唯一键（包含 deleted 字段）
ALTER TABLE `shopping_cart` ADD UNIQUE KEY `uk_user_sku` (`user_id`, `sku_id`, `deleted`);

-- 3. 分片表同步修改（若存在）
-- ALTER TABLE `t_shopping_cart_0` DROP INDEX `uk_user_sku`;
-- ALTER TABLE `t_shopping_cart_0` ADD UNIQUE KEY `uk_user_sku` (`user_id`, `sku_id`, `deleted`);
-- ALTER TABLE `t_shopping_cart_1` DROP INDEX `uk_user_sku`;
-- ALTER TABLE `t_shopping_cart_1` ADD UNIQUE KEY `uk_user_sku` (`user_id`, `sku_id`, `deleted`);
-- ALTER TABLE `t_shopping_cart_2` DROP INDEX `uk_user_sku`;
-- ALTER TABLE `t_shopping_cart_2` ADD UNIQUE KEY `uk_user_sku` (`user_id`, `sku_id`, `deleted`);
-- ALTER TABLE `t_shopping_cart_3` DROP INDEX `uk_user_sku`;
-- ALTER TABLE `t_shopping_cart_3` ADD UNIQUE KEY `uk_user_sku` (`user_id`, `sku_id`, `deleted`);