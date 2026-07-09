-- ============================================================
-- Tailor IS - Flyway Migration V1: 社区系统数据库初始化
-- 数据库: tailor_is_community
-- 创建时间: 2026-07-09
-- 说明: 基线迁移，创建帖子、评论、点赞、收藏等核心表
-- ============================================================

CREATE TABLE IF NOT EXISTS `community_post` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '帖子ID（主键）',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '发帖用户ID',
  `title` VARCHAR(256) NOT NULL COMMENT '帖子标题',
  `content` TEXT NOT NULL COMMENT '帖子正文内容',
  `images` JSON DEFAULT NULL COMMENT '帖子图片（JSON数组）',
  `video_url` VARCHAR(512) DEFAULT NULL COMMENT '帖子视频URL',
  `type` TINYINT NOT NULL DEFAULT 1 COMMENT '帖子类型：1-图文，2-视频，3-问答，4-教程，5-晒单，6-投票',
  `category_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '帖子分类ID',
  `tags` JSON DEFAULT NULL COMMENT '帖子标签（JSON数组）',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览量',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  `comment_count` INT NOT NULL DEFAULT 0 COMMENT '评论数',
  `share_count` INT NOT NULL DEFAULT 0 COMMENT '分享数',
  `collect_count` INT NOT NULL DEFAULT 0 COMMENT '收藏数',
  `is_top` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶：0-否，1-是',
  `is_essential` TINYINT NOT NULL DEFAULT 0 COMMENT '是否精华：0-否，1-是',
  `is_recommend` TINYINT NOT NULL DEFAULT 0 COMMENT '是否推荐：0-否，1-是',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-草稿，1-待审核，2-已发布，3-已删除，4-审核不通过',
  `audit_remark` VARCHAR(512) DEFAULT NULL COMMENT '审核备注',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `audit_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '审核人ID',
  `related_product_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联商品ID',
  `related_shop_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联店铺ID',
  `ip_address` VARCHAR(45) DEFAULT NULL COMMENT '发帖IP地址',
  `device_info` VARCHAR(128) DEFAULT NULL COMMENT '设备信息',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_type` (`type`),
  KEY `idx_status` (`status`),
  KEY `idx_view_count` (`view_count`),
  KEY `idx_like_count` (`like_count`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_is_top` (`is_top`),
  KEY `idx_is_essential` (`is_essential`),
  FULLTEXT KEY `ft_title_content` (`title`, `content`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='社区帖子表';

CREATE TABLE IF NOT EXISTS `community_comment` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评论ID（主键）',
  `post_id` BIGINT UNSIGNED NOT NULL COMMENT '帖子ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '评论用户ID',
  `parent_id` BIGINT UNSIGNED DEFAULT 0 COMMENT '父评论ID（0表示一级评论）',
  `reply_to_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '回复的用户ID',
  `content` VARCHAR(1000) NOT NULL COMMENT '评论内容',
  `images` JSON DEFAULT NULL COMMENT '评论图片（JSON数组）',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  `floor` INT DEFAULT NULL COMMENT '楼层号',
  `ip_address` VARCHAR(45) DEFAULT NULL COMMENT '评论IP地址',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-待审核，1-正常，2-已删除，3-审核不通过',
  `audit_remark` VARCHAR(512) DEFAULT NULL COMMENT '审核备注',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_reply_to_user_id` (`reply_to_user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='社区评论表';

CREATE TABLE IF NOT EXISTS `community_like` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '点赞ID（主键）',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '点赞用户ID',
  `target_type` TINYINT NOT NULL COMMENT '目标类型：1-帖子，2-评论',
  `target_id` BIGINT UNSIGNED NOT NULL COMMENT '目标ID（帖子ID或评论ID）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target` (`user_id`, `target_type`, `target_id`),
  KEY `idx_target_type_id` (`target_type`, `target_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='社区点赞表';

CREATE TABLE IF NOT EXISTS `community_report` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '举报ID（主键）',
  `post_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '被举报帖子ID',
  `comment_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '被举报评论ID',
  `reported_user_id` BIGINT UNSIGNED NOT NULL COMMENT '被举报用户ID',
  `reporter_id` BIGINT UNSIGNED NOT NULL COMMENT '举报人用户ID',
  `reason` VARCHAR(128) NOT NULL COMMENT '举报原因：1-垃圾广告，2-色情低俗，3-人身攻击，4-侵权，5-虚假信息，6-其他',
  `description` VARCHAR(1000) DEFAULT NULL COMMENT '举报详细说明',
  `evidence_images` JSON DEFAULT NULL COMMENT '举报证据图片（JSON数组）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态：0-待处理，1-处理中，2-举报成立，3-举报不成立，4-已忽略',
  `handler_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '处理人ID',
  `handler_remark` VARCHAR(512) DEFAULT NULL COMMENT '处理备注',
  `handled_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  `punishment_type` TINYINT DEFAULT NULL COMMENT '处罚类型：1-删除内容，2-警告，3-禁言，4-封号',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_comment_id` (`comment_id`),
  KEY `idx_reported_user_id` (`reported_user_id`),
  KEY `idx_reporter_id` (`reporter_id`),
  KEY `idx_reason` (`reason`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='社区举报表';

CREATE TABLE IF NOT EXISTS `community_collect` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '收藏ID（主键）',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '收藏用户ID',
  `post_id` BIGINT UNSIGNED NOT NULL COMMENT '帖子ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_post` (`user_id`, `post_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='帖子收藏表';

CREATE TABLE IF NOT EXISTS `community_category` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分类ID（主键）',
  `name` VARCHAR(64) NOT NULL COMMENT '分类名称',
  `icon` VARCHAR(512) DEFAULT NULL COMMENT '分类图标URL',
  `description` VARCHAR(256) DEFAULT NULL COMMENT '分类描述',
  `sort` INT DEFAULT 0 COMMENT '排序权重',
  `post_count` INT DEFAULT 0 COMMENT '帖子数量',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_sort` (`sort`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='社区分类表';

CREATE TABLE IF NOT EXISTS `user_mute_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '禁言记录ID（主键）',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '被禁言用户ID',
  `operator_id` BIGINT UNSIGNED NOT NULL COMMENT '操作人ID（管理员）',
  `reason` VARCHAR(256) NOT NULL COMMENT '禁言原因',
  `related_type` VARCHAR(32) DEFAULT NULL COMMENT '关联类型：post/comment/report',
  `related_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联内容ID',
  `mute_start_time` DATETIME NOT NULL COMMENT '禁言开始时间',
  `mute_end_time` DATETIME NOT NULL COMMENT '禁言结束时间',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-禁言中，2-已解除，3-已过期',
  `unmute_time` DATETIME DEFAULT NULL COMMENT '实际解除时间',
  `unmute_operator` BIGINT UNSIGNED DEFAULT NULL COMMENT '解除操作人ID',
  `remark` VARCHAR(256) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_status` (`status`),
  KEY `idx_mute_end_time` (`mute_end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户禁言记录表';

INSERT INTO `community_category` (`name`, `icon`, `description`, `sort`, `status`) VALUES
('设计灵感', '/icons/community/design.svg', '分享创意设计灵感与趋势', 1, 1),
('面料交流', '/icons/community/fabric.svg', '面料知识分享与交流', 2, 1),
('定制经验', '/icons/community/custom.svg', '服装定制经验与技巧', 3, 1),
('作品展示', '/icons/community/showcase.svg', '展示你的设计作品', 4, 1),
('问答互助', '/icons/community/qa.svg', '提问与解答', 5, 1),
('行业动态', '/icons/community/news.svg', '行业资讯与动态', 6, 1);