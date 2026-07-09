-- ============================================================
-- Tailor IS - Flyway Migration V1: IM即时通讯系统数据库初始化
-- 数据库: tailor_is_im
-- 创建时间: 2026-07-10
-- 说明: 基线迁移，创建IM系统所有表结构
-- ============================================================

CREATE TABLE IF NOT EXISTS `im_conversation` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '会话ID（主键）',
  `user_id1` BIGINT UNSIGNED NOT NULL COMMENT '参与者1用户ID',
  `user_id2` BIGINT UNSIGNED NOT NULL COMMENT '参与者2用户ID',
  `last_message` VARCHAR(500) COMMENT '最后一条消息内容',
  `last_message_at` DATETIME COMMENT '最后一条消息时间',
  `unread_count` INT DEFAULT 0 COMMENT '未读消息数',
  `status` TINYINT DEFAULT 1 COMMENT '状态0关闭1正常',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_pair` (`user_id1`, `user_id2`),
  KEY `idx_user_id1` (`user_id1`),
  KEY `idx_user_id2` (`user_id2`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会话表';

CREATE TABLE IF NOT EXISTS `im_message` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息ID（主键）',
  `from_user_id` BIGINT UNSIGNED NOT NULL COMMENT '发送者用户ID',
  `to_user_id` BIGINT UNSIGNED NOT NULL COMMENT '接收者用户ID',
  `content` TEXT COMMENT '消息内容',
  `message_type` TINYINT DEFAULT 1 COMMENT '消息类型1文本2图片3语音4文件',
  `conversation_id` BIGINT UNSIGNED COMMENT '会话ID',
  `status` TINYINT DEFAULT 0 COMMENT '消息状态0未读1已读',
  `sent_at` DATETIME COMMENT '发送时间',
  `is_recalled` TINYINT DEFAULT 0 COMMENT '是否已撤回：0-否，1-是',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_conversation_id` (`conversation_id`),
  KEY `idx_from_user_id` (`from_user_id`),
  KEY `idx_to_user_id` (`to_user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_sent_at` (`sent_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='聊天消息表';