-- ============================================================
-- Tailor IS 平台 - 消息/私信系统数据库表结构
-- 文件: 10_message_system.sql
-- 版本: 1.0.0
-- 数据库: MySQL 8.0
-- 创建时间: 2026-05-29
-- ============================================================

-- 创建数据库（如不存在）
CREATE DATABASE IF NOT EXISTS `tailor_is_message` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `tailor_is_message`;

-- ============================================================
-- 1. 会话表 (message_session)
-- ============================================================
DROP TABLE IF EXISTS `message_session`;
CREATE TABLE `message_session` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '会话ID（主键）',
  `session_no` VARCHAR(64) NOT NULL COMMENT '会话编号（平台唯一）',
  `user_id_1` BIGINT UNSIGNED NOT NULL COMMENT '会话参与方1用户ID',
  `user_id_2` BIGINT UNSIGNED NOT NULL COMMENT '会话参与方2用户ID',
  `session_type` TINYINT NOT NULL DEFAULT 1 COMMENT '会话类型：1-私聊，2-客服会话，3-系统通知',
  `target_type` VARCHAR(32) DEFAULT NULL COMMENT '目标类型：merchant/shop/order/after_sale等',
  `target_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '目标业务ID（如订单ID、售后ID等）',
  `last_message` VARCHAR(512) DEFAULT NULL COMMENT '最后一条消息内容',
  `last_message_type` TINYINT DEFAULT 1 COMMENT '最后一条消息类型：1-文本，2-图片，3-语音，4-视频，5-文件，6-链接，7-系统消息',
  `last_sender_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '最后消息发送者ID',
  `last_message_time` DATETIME DEFAULT NULL COMMENT '最后消息时间',
  `unread_count_1` INT NOT NULL DEFAULT 0 COMMENT '用户1未读数',
  `unread_count_2` INT NOT NULL DEFAULT 0 COMMENT '用户2未读数',
  `message_count` INT NOT NULL DEFAULT 0 COMMENT '消息总数',
  `is_pinned_1` TINYINT DEFAULT 0 COMMENT '用户1是否置顶：0-否，1-是',
  `is_pinned_2` TINYINT DEFAULT 0 COMMENT '用户2是否置顶：0-否，1-是',
  `is_muted_1` TINYINT DEFAULT 0 COMMENT '用户1是否免打扰：0-否，1-是',
  `is_muted_2` TINYINT DEFAULT 0 COMMENT '用户2是否免打扰：0-否，1-是',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-已关闭，1-正常',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_no` (`session_no`),
  UNIQUE KEY `uk_user_pair` (`user_id_1`, `user_id_2`, `session_type`, `target_id`),
  KEY `idx_user_id_1` (`user_id_1`),
  KEY `idx_user_id_2` (`user_id_2`),
  KEY `idx_session_type` (`session_type`),
  KEY `idx_last_message_time` (`last_message_time`),
  KEY `idx_unread_1` (`unread_count_1`),
  KEY `idx_unread_2` (`unread_count_2`),
  KEY `idx_status` (`status`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_ms_user1` FOREIGN KEY (`user_id_1`) REFERENCES `tailor_is_user`.`sys_user` (`id`),
  -- CONSTRAINT `fk_ms_user2` FOREIGN KEY (`user_id_2`) REFERENCES `tailor_is_user`.`sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='消息会话表';

-- ============================================================
-- 2. 消息内容表 (message_content)
-- ============================================================
DROP TABLE IF EXISTS `message_content`;
CREATE TABLE `message_content` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息ID（主键）',
  `session_id` BIGINT UNSIGNED NOT NULL COMMENT '会话ID',
  `session_no` VARCHAR(64) NOT NULL COMMENT '会话编号（冗余字段）',
  `sender_id` BIGINT UNSIGNED NOT NULL COMMENT '发送者用户ID',
  `receiver_id` BIGINT UNSIGNED NOT NULL COMMENT '接收者用户ID',
  `content` VARCHAR(2000) NOT NULL COMMENT '消息内容',
  `content_type` TINYINT NOT NULL DEFAULT 1 COMMENT '消息类型：1-文本，2-图片，3-语音，4-视频，5-文件，6-链接，7-表情，8-系统消息，9-订单卡片，10-售后卡片',
  `content_extra` JSON DEFAULT NULL COMMENT '消息扩展内容（JSON格式：图片URL/语音时长/文件信息等）',
  `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
  `read_time` DATETIME DEFAULT NULL COMMENT '已读时间',
  `is_recalled` TINYINT DEFAULT 0 COMMENT '是否已撤回：0-否，1-是',
  `recall_time` DATETIME DEFAULT NULL COMMENT '撤回时间',
  `reply_to_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '回复的消息ID',
  `mention_users` JSON DEFAULT NULL COMMENT '@的用户列表（JSON数组）',
  `ip_address` VARCHAR(45) DEFAULT NULL COMMENT '发送IP地址',
  `device_info` VARCHAR(128) DEFAULT NULL COMMENT '设备信息',
  `send_status` TINYINT NOT NULL DEFAULT 1 COMMENT '发送状态：0-发送中，1-发送成功，2-发送失败',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `send_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_session_no` (`session_no`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_receiver_id` (`receiver_id`),
  KEY `idx_content_type` (`content_type`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_send_time` (`send_time`),
  KEY `idx_reply_to_id` (`reply_to_id`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_mc_session` FOREIGN KEY (`session_id`) REFERENCES `message_session` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='消息内容表';

-- ============================================================
-- 3. 敏感词表 (sensitive_word)
-- ============================================================
DROP TABLE IF EXISTS `sensitive_word`;
CREATE TABLE `sensitive_word` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '敏感词ID（主键）',
  `word` VARCHAR(128) NOT NULL COMMENT '敏感词内容',
  `level` TINYINT NOT NULL DEFAULT 1 COMMENT '敏感级别：1-低（替换），2-中（拦截并警告），3-高（拦截并封禁）',
  `category` VARCHAR(32) DEFAULT NULL COMMENT '敏感词分类：政治/色情/广告/辱骂/违规等',
  `action` TINYINT NOT NULL DEFAULT 1 COMMENT '处理方式：1-替换为**，2-拦截发送，3-拦截并记录，4-仅记录',
  `replace_text` VARCHAR(64) DEFAULT '***' COMMENT '替换文本',
  `match_type` TINYINT NOT NULL DEFAULT 1 COMMENT '匹配类型：1-精确匹配，2-模糊匹配，3-正则匹配',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `hit_count` INT NOT NULL DEFAULT 0 COMMENT '命中次数',
  `remark` VARCHAR(256) DEFAULT NULL COMMENT '备注说明',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_word` (`word`),
  KEY `idx_level` (`level`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_hit_count` (`hit_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='敏感词库表';

-- ============================================================
-- 4. 系统通知表 (system_notification)
-- ============================================================
DROP TABLE IF EXISTS `system_notification`;
CREATE TABLE `system_notification` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '通知ID（主键）',
  `title` VARCHAR(256) NOT NULL COMMENT '通知标题',
  `content` TEXT NOT NULL COMMENT '通知内容',
  `type` TINYINT NOT NULL DEFAULT 1 COMMENT '通知类型：1-系统公告，2-订单通知，3-物流通知，4-营销通知，5-售后通知，6-安全通知',
  `sender_type` TINYINT NOT NULL DEFAULT 1 COMMENT '发送方类型：1-系统，2-商家，3-平台客服',
  `sender_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '发送方ID',
  `target_type` TINYINT NOT NULL DEFAULT 1 COMMENT '目标类型：1-全体用户，2-指定用户，3-指定角色，4-指定商家',
  `target_ids` JSON DEFAULT NULL COMMENT '目标用户/商家ID列表（JSON数组）',
  `related_type` VARCHAR(32) DEFAULT NULL COMMENT '关联业务类型：order/payment/after_sale等',
  `related_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联业务ID',
  `related_no` VARCHAR(64) DEFAULT NULL COMMENT '关联业务编号',
  `push_channels` TINYINT NOT NULL DEFAULT 1 COMMENT '推送渠道：1-站内信，2-短信，4-邮件，8-APP推送（位运算组合）',
  `push_status` TINYINT NOT NULL DEFAULT 0 COMMENT '推送状态：0-待推送，1-推送中，2-推送完成，3-推送失败',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-草稿，1-已发布，2-已下架',
  `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
  `image_url` VARCHAR(512) DEFAULT NULL COMMENT '通知配图URL',
  `action_url` VARCHAR(512) DEFAULT NULL COMMENT '跳转链接',
  `created_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人ID',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_type` (`type`),
  KEY `idx_target_type` (`target_type`),
  KEY `idx_status` (`status`),
  KEY `idx_publish_time` (`publish_time`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统通知表';

-- ============================================================
-- 5. 用户通知记录表 (user_notification)
-- ============================================================
DROP TABLE IF EXISTS `user_notification`;
CREATE TABLE `user_notification` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户通知记录ID（主键）',
  `notification_id` BIGINT UNSIGNED NOT NULL COMMENT '通知ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '接收用户ID',
  `title` VARCHAR(256) NOT NULL COMMENT '通知标题（冗余字段）',
  `content` TEXT DEFAULT NULL COMMENT '通知内容（冗余字段）',
  `type` TINYINT NOT NULL COMMENT '通知类型（冗余字段）',
  `related_type` VARCHAR(32) DEFAULT NULL COMMENT '关联业务类型',
  `related_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联业务ID',
  `related_no` VARCHAR(64) DEFAULT NULL COMMENT '关联业务编号',
  `image_url` VARCHAR(512) DEFAULT NULL COMMENT '通知配图URL',
  `action_url` VARCHAR(512) DEFAULT NULL COMMENT '跳转链接',
  `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
  `read_time` DATETIME DEFAULT NULL COMMENT '已读时间',
  `push_channel` TINYINT DEFAULT 1 COMMENT '推送渠道：1-站内信，2-短信，4-邮件，8-APP推送',
  `push_status` TINYINT DEFAULT 0 COMMENT '推送状态：0-待推送，1-已推送，2-推送失败',
  `push_time` DATETIME DEFAULT NULL COMMENT '推送时间',
  `fail_reason` VARCHAR(256) DEFAULT NULL COMMENT '推送失败原因',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_notification_id` (`notification_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_type` (`type`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_created_at` (`created_at`)
  -- 外键约束（可选）
  -- CONSTRAINT `fk_un_notification` FOREIGN KEY (`notification_id`) REFERENCES `system_notification` (`id`),
  -- CONSTRAINT `fk_un_user` FOREIGN KEY (`user_id`) REFERENCES `tailor_is_user`.`sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户通知记录表';

-- ============================================================
-- 6. 消息敏感词检测记录表 (message_sensitive_log)
-- ============================================================
DROP TABLE IF EXISTS `message_sensitive_log`;
CREATE TABLE `message_sensitive_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '检测记录ID（主键）',
  `message_id` BIGINT UNSIGNED NOT NULL COMMENT '消息ID',
  `session_id` BIGINT UNSIGNED NOT NULL COMMENT '会话ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '发送者用户ID',
  `content` VARCHAR(512) NOT NULL COMMENT '触发敏感词的内容片段',
  `matched_words` JSON NOT NULL COMMENT '匹配到的敏感词列表（JSON数组）',
  `highest_level` TINYINT NOT NULL COMMENT '最高敏感级别',
  `action_taken` TINYINT NOT NULL COMMENT '执行的动作：1-替换，2-拦截，3-拦截并记录，4-仅记录',
  `is_processed` TINYINT DEFAULT 0 COMMENT '是否已人工处理：0-否，1-是',
  `handler_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '处理人ID',
  `handler_remark` VARCHAR(256) DEFAULT NULL COMMENT '处理备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_message_id` (`message_id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_highest_level` (`highest_level`),
  KEY `idx_is_processed` (`is_processed`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='消息敏感词检测记录表';

-- ============================================================
-- 初始化数据：敏感词（示例）
-- ============================================================
INSERT INTO `sensitive_word` (`word`, `level`, `category`, `action`, `replace_text`, `match_type`, `status`) VALUES
('测试敏感词', 1, '广告', 1, '***', 1, 1),
('违规推广', 2, '广告', 2, NULL, 1, 1),
('违法内容', 3, '违规', 3, NULL, 2, 1);

-- ============================================================
-- 初始化数据：系统通知模板
-- ============================================================
INSERT INTO `system_notification` (`title`, `content`, `type`, `sender_type`, `target_type`, `push_channels`, `push_status`, `status`, `publish_time`) VALUES
('平台服务协议更新', '尊敬的用户，平台服务协议已更新，请您及时查看最新内容。', 1, 1, 1, 1, 2, 1, NOW()),
('系统维护通知', '平台将于近期进行系统维护升级，届时可能影响部分功能使用，请您提前做好安排。', 1, 1, 1, 1, 2, 1, NOW()),
('欢迎加入Tailor IS平台', '感谢您加入Tailor IS平台，祝您在这里找到最优质的服装定制服务！', 1, 1, 2, 1, 2, 1, NOW());
