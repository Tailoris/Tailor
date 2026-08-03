-- ============================================================
-- Tailor IS 平台 - 移除区域代理商功能模块
-- 文件: V12__Drop_Region_Agent_Module.sql
-- 版本: 1.0.0
-- 数据库: MySQL 8.0
-- 创建时间: 2026-08-03
-- 说明: 彻底清除区域代理商(merchant_region_agent)功能相关的表结构、
--       权限配置、菜单记录，确保清除操作不影响其他功能模块正常运行
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

USE `tailor_is_merchant`;

-- ============================================================
-- 1. 删除区域代理商表（含数据和索引）
-- ============================================================
DROP TABLE IF EXISTS `merchant_region_agent`;

USE `tailor_is_admin`;

-- ============================================================
-- 2. 清理权限系统中的区域代理商相关权限记录
--    权限编码: merchant:agent:view / add / edit / audit / delete
-- ============================================================
DELETE FROM `sys_permission` WHERE `permission_code` IN (
    'merchant:agent:view',
    'merchant:agent:add',
    'merchant:agent:edit',
    'merchant:agent:audit',
    'merchant:agent:delete'
);

-- ============================================================
-- 3. 清理角色-权限关联表中的区域代理商权限绑定
-- ============================================================
DELETE FROM `sys_role_permission` WHERE `permission_id` IN (
    SELECT `id` FROM `sys_permission` WHERE `permission_code` LIKE 'merchant:agent:%'
);

-- 兜底：若权限表已无记录，按 permission_code 前缀清理可能的残留关联（兼容不同命名）
DELETE FROM `sys_role_permission` WHERE `permission_code` LIKE 'merchant:agent:%';

-- ============================================================
-- 4. 清理菜单表中的"区域代理商"导航入口
--    按菜单 path、name、title 关键字过滤
-- ============================================================
DELETE FROM `sys_menu` WHERE `path` = '/region-agent'
                         OR `name` = 'RegionAgent'
                         OR `title` LIKE '%区域代理商%'
                         OR `component` LIKE '%RegionAgentView%';

USE `tailor_is_merchant`;

-- ============================================================
-- 5. 清理商家修改日志表中与 region_agent 模块相关的历史操作记录
--    operate_module='region_agent' 的所有历史日志（保留其他模块日志）
-- ============================================================
DELETE FROM `merchant_modify_log` WHERE `operate_module` = 'region_agent';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 验证：清除完成后，以下查询应返回 0
-- ============================================================
-- SELECT COUNT(*) FROM information_schema.tables
--   WHERE table_schema = 'tailor_is_merchant' AND table_name = 'merchant_region_agent';
-- SELECT COUNT(*) FROM `tailor_is_admin`.`sys_permission` WHERE `permission_code` LIKE 'merchant:agent:%';
-- SELECT COUNT(*) FROM `tailor_is_admin`.`sys_menu` WHERE `path` = '/region-agent';
-- SELECT COUNT(*) FROM `tailor_is_merchant`.`merchant_modify_log` WHERE `operate_module` = 'region_agent';
