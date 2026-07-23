-- ============================================================
-- Tailor IS 平台 - IM即时通讯服务数据库表结构
-- 文件: 18_im_system.sql
-- 版本: 1.0.0
-- 数据库: MySQL 8.0
-- 创建时间: 2026-07-20
-- ============================================================

CREATE DATABASE IF NOT EXISTS `tailor_is_im` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `tailor_is_im`;

-- ============================================================
-- 1. 会话表 (im_conversation)
-- ============================================================
CREATE TABLE IF NOT EXISTS `im_conversation` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `conversation_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
  `conversation_type` VARCHAR(16) NOT NULL DEFAULT 'single' COMMENT '会话类型：single/group',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `target_id` BIGINT UNSIGNED NOT NULL COMMENT '目标ID（对方用户ID或群ID）',
  `last_message` VARCHAR(1024) DEFAULT NULL COMMENT '最后一条消息',
  `last_message_time` DATETIME DEFAULT NULL COMMENT '最后消息时间',
  `unread_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '未读数',
  `is_top` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶',
  `is_muted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否免打扰',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target` (`user_id`, `target_id`, `conversation_type`),
  KEY `idx_conversation_id` (`conversation_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_last_message_time` (`last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会话表';

-- ============================================================
-- 2. 消息表 (im_message)
-- ============================================================
CREATE TABLE IF NOT EXISTS `im_message` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `message_id` VARCHAR(64) NOT NULL COMMENT '消息ID',
  `conversation_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
  `sender_id` BIGINT UNSIGNED NOT NULL COMMENT '发送者ID',
  `receiver_id` BIGINT UNSIGNED NOT NULL COMMENT '接收者ID',
  `message_type` VARCHAR(16) NOT NULL DEFAULT 'text' COMMENT '消息类型：text/image/file/audio/video/system',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `extra_data` JSON DEFAULT NULL COMMENT '额外数据（图片尺寸、文件信息等）',
  `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读',
  `read_time` DATETIME DEFAULT NULL COMMENT '阅读时间',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-已撤回，1-正常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_id` (`message_id`),
  KEY `idx_conversation_id` (`conversation_id`),
  KEY `idx_sender_receiver` (`sender_id`, `receiver_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='消息表';

-- ============================================================
-- 3. 消息记录归档表 (im_message_archive)
-- ============================================================
CREATE TABLE IF NOT EXISTS `im_message_archive` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `message_id` VARCHAR(64) NOT NULL COMMENT '消息ID',
  `conversation_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
  `sender_id` BIGINT UNSIGNED NOT NULL COMMENT '发送者ID',
  `receiver_id` BIGINT UNSIGNED NOT NULL COMMENT '接收者ID',
  `message_type` VARCHAR(16) NOT NULL COMMENT '消息类型',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `extra_data` JSON DEFAULT NULL COMMENT '额外数据',
  `create_time` DATETIME NOT NULL COMMENT '原始创建时间',
  `archive_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',
  PRIMARY KEY (`id`),
  KEY `idx_conversation_id` (`conversation_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='消息归档表';