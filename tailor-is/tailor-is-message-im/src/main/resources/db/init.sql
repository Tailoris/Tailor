CREATE TABLE IF NOT EXISTS `im_conversation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id1` BIGINT NOT NULL COMMENT '参与者1用户ID',
  `user_id2` BIGINT NOT NULL COMMENT '参与者2用户ID',
  `last_message` VARCHAR(500) COMMENT '最后一条消息内容',
  `last_message_at` DATETIME COMMENT '最后一条消息时间',
  `unread_count` INT DEFAULT 0 COMMENT '未读消息数',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_pair` (`user_id1`, `user_id2`),
  KEY `idx_user_id1` (`user_id1`),
  KEY `idx_user_id2` (`user_id2`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

CREATE TABLE IF NOT EXISTS `im_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `from_user_id` BIGINT NOT NULL COMMENT '发送者用户ID',
  `to_user_id` BIGINT NOT NULL COMMENT '接收者用户ID',
  `content` TEXT COMMENT '消息内容',
  `message_type` TINYINT DEFAULT 1 COMMENT '消息类型1文本2图片3语音4文件',
  `conversation_id` BIGINT COMMENT '会话ID',
  `status` TINYINT DEFAULT 0 COMMENT '消息状态0未读1已读',
  `sent_at` DATETIME COMMENT '发送时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_conversation_id` (`conversation_id`),
  KEY `idx_from_user_id` (`from_user_id`),
  KEY `idx_to_user_id` (`to_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';