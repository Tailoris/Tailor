-- ============================================================
-- Tailor IS 平台 - 用户系统数据库表结构
-- 文件: 01_user_system.sql
-- 版本: 1.0.0
-- 数据库: MySQL 8.0
-- 创建时间: 2026-05-29
-- ============================================================

-- 创建数据库（如不存在）
CREATE DATABASE IF NOT EXISTS `tailor_is_user` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `tailor_is_user`;

-- ============================================================
-- 1. 用户表 (sys_user)
-- ============================================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID（主键）',
  `username` VARCHAR(64) NOT NULL COMMENT '用户名（登录账号）',
  `password` VARCHAR(128) NOT NULL COMMENT '密码（BCrypt加密）',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱地址',
  `avatar` VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
  `real_name` VARCHAR(64) DEFAULT NULL COMMENT '真实姓名',
  `id_card` VARCHAR(18) DEFAULT NULL COMMENT '身份证号码',
  `gender` TINYINT DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
  `birthday` DATE DEFAULT NULL COMMENT '生日',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` VARCHAR(45) DEFAULT NULL COMMENT '最后登录IP',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统用户表';

-- ============================================================
-- 2. 角色表 (sys_role)
-- ============================================================
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色ID（主键）',
  `role_name` VARCHAR(64) NOT NULL COMMENT '角色名称',
  `role_code` VARCHAR(64) NOT NULL COMMENT '角色编码（唯一标识）',
  `description` VARCHAR(256) DEFAULT NULL COMMENT '角色描述',
  `data_scope` TINYINT DEFAULT 1 COMMENT '数据权限范围：1-全部，2-本部门，3-本部门及以下，4-仅本人',
  `sort` INT DEFAULT 0 COMMENT '排序权重（数值越小越靠前）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`),
  KEY `idx_status` (`status`),
  KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统角色表';

-- ============================================================
-- 3. 用户角色关联表 (sys_user_role)
-- ============================================================
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
  -- 外键约束（可选，根据性能需求决定是否启用）
  -- CONSTRAINT `fk_ur_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
  -- CONSTRAINT `fk_ur_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户角色关联表';

-- ============================================================
-- 4. 权限表 (sys_permission)
-- ============================================================
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '权限ID（主键）',
  `permission_name` VARCHAR(64) NOT NULL COMMENT '权限名称',
  `permission_code` VARCHAR(128) NOT NULL COMMENT '权限编码（唯一标识）',
  `type` TINYINT NOT NULL COMMENT '权限类型：0-目录，1-菜单，2-按钮',
  `path` VARCHAR(256) DEFAULT NULL COMMENT '路由路径',
  `component` VARCHAR(256) DEFAULT NULL COMMENT '组件路径',
  `parent_id` BIGINT UNSIGNED DEFAULT 0 COMMENT '父级权限ID（0表示顶级）',
  `icon` VARCHAR(64) DEFAULT NULL COMMENT '图标',
  `sort` INT DEFAULT 0 COMMENT '排序权重',
  `visible` TINYINT DEFAULT 1 COMMENT '是否可见：0-隐藏，1-显示',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_type` (`type`),
  KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统权限表';

-- ============================================================
-- 5. 角色权限关联表 (sys_role_permission)
-- ============================================================
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  `permission_id` BIGINT UNSIGNED NOT NULL COMMENT '权限ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_permission_id` (`permission_id`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_rp_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`),
  -- CONSTRAINT `fk_rp_permission` FOREIGN KEY (`permission_id`) REFERENCES `sys_permission` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色权限关联表';

-- ============================================================
-- 6. 用户地址表 (user_address)
-- ============================================================
DROP TABLE IF EXISTS `user_address`;
CREATE TABLE `user_address` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '地址ID（主键）',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `name` VARCHAR(64) NOT NULL COMMENT '收货人姓名',
  `phone` VARCHAR(20) NOT NULL COMMENT '收货人电话',
  `province` VARCHAR(64) NOT NULL COMMENT '省份',
  `city` VARCHAR(64) NOT NULL COMMENT '城市',
  `district` VARCHAR(64) NOT NULL COMMENT '区/县',
  `street` VARCHAR(128) DEFAULT NULL COMMENT '街道/乡镇',
  `detail` VARCHAR(256) NOT NULL COMMENT '详细地址',
  `postal_code` VARCHAR(10) DEFAULT NULL COMMENT '邮政编码',
  `longitude` DECIMAL(10, 6) DEFAULT NULL COMMENT '经度',
  `latitude` DECIMAL(10, 6) DEFAULT NULL COMMENT '纬度',
  `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认地址：0-否，1-是',
  `tag` VARCHAR(32) DEFAULT NULL COMMENT '地址标签：家/公司/学校等',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_default` (`is_default`),
  KEY `idx_phone` (`phone`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_address_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户收货地址表';

-- ============================================================
-- 初始化数据
-- ============================================================

-- 初始化角色数据
INSERT INTO `sys_role` (`role_name`, `role_code`, `description`, `data_scope`, `sort`, `status`) VALUES
('超级管理员', 'SUPER_ADMIN', '拥有系统所有权限，可管理所有模块', 1, 1, 1),
('平台管理员', 'PLATFORM_ADMIN', '平台运营管理人员，管理商家和商品审核', 2, 2, 1),
('商家', 'MERCHANT', '入驻商家角色，管理自己的店铺和商品', 4, 3, 1),
('普通用户', 'USER', '平台普通用户，可浏览商品和下单购买', 4, 4, 1),
('客服', 'CUSTOMER_SERVICE', '平台客服人员，处理售后和用户问题', 3, 5, 1),
('审核员', 'AUDITOR', '商品和商家资质审核人员', 2, 6, 1);

-- 初始化权限数据
INSERT INTO `sys_permission` (`permission_name`, `permission_code`, `type`, `path`, `component`, `parent_id`, `icon`, `sort`, `visible`, `status`) VALUES
-- 一级菜单（目录）
('系统管理', 'system', 0, '/system', 'Layout', 0, 'setting', 1, 1, 1),
('用户管理', 'user', 0, '/user', 'Layout', 0, 'user', 2, 1, 1),
('商家管理', 'merchant', 0, '/merchant', 'Layout', 0, 'shop', 3, 1, 1),
('商品管理', 'product', 0, '/product', 'Layout', 0, 'goods', 4, 1, 1),
('订单管理', 'order', 0, '/order', 'Layout', 0, 'order', 5, 1, 1),
('营销管理', 'marketing', 0, '/marketing', 'Layout', 0, 'gift', 6, 1, 1),
('版权管理', 'copyright', 0, '/copyright', 'Layout', 0, 'copyright', 7, 1, 1),
('社区管理', 'community', 0, '/community', 'Layout', 0, 'comment', 8, 1, 1),
('数据统计', 'statistics', 0, '/statistics', 'Layout', 0, 'chart', 9, 1, 1);

-- 系统管理子菜单
INSERT INTO `sys_permission` (`permission_name`, `permission_code`, `type`, `path`, `component`, `parent_id`, `icon`, `sort`, `visible`, `status`) VALUES
('角色管理', 'system:role', 1, '/system/role', 'system/role/index', 1, '', 1, 1, 1),
('角色列表', 'system:role:list', 2, NULL, NULL, 10, '', 1, 1, 1),
('角色新增', 'system:role:add', 2, NULL, NULL, 10, '', 2, 1, 1),
('角色编辑', 'system:role:edit', 2, NULL, NULL, 10, '', 3, 1, 1),
('角色删除', 'system:role:delete', 2, NULL, NULL, 10, '', 4, 1, 1),
('权限管理', 'system:permission', 1, '/system/permission', 'system/permission/index', 1, '', 2, 1, 1),
('权限列表', 'system:permission:list', 2, NULL, NULL, 16, '', 1, 1, 1),
('权限新增', 'system:permission:add', 2, NULL, NULL, 16, '', 2, 1, 1),
('权限编辑', 'system:permission:edit', 2, NULL, NULL, 16, '', 3, 1, 1),
('权限删除', 'system:permission:delete', 2, NULL, NULL, 16, '', 4, 1, 1);

-- 用户管理子菜单
INSERT INTO `sys_permission` (`permission_name`, `permission_code`, `type`, `path`, `component`, `parent_id`, `icon`, `sort`, `visible`, `status`) VALUES
('用户列表', 'user:list', 1, '/user/list', 'user/list/index', 2, '', 1, 1, 1),
('用户查看', 'user:view', 2, NULL, NULL, 22, '', 1, 1, 1),
('用户编辑', 'user:edit', 2, NULL, NULL, 22, '', 2, 1, 1),
('用户禁用', 'user:disable', 2, NULL, NULL, 22, '', 3, 1, 1),
('用户删除', 'user:delete', 2, NULL, NULL, 22, '', 4, 1, 1);

-- 初始化超级管理员用户（密码：admin123，BCrypt加密）
INSERT INTO `sys_user` (`username`, `password`, `phone`, `email`, `real_name`, `status`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '13800138000', 'admin@tailoris.com', '系统管理员', 0);

-- 为超级管理员分配所有角色
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1);

-- 为超级管理员分配所有权限（获取所有权限ID并关联）
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 1, id FROM `sys_permission` WHERE `deleted` = 0;
