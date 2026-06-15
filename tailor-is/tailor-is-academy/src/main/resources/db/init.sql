CREATE TABLE IF NOT EXISTS `course` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(200) NOT NULL COMMENT '课程标题',
  `description` TEXT COMMENT '课程描述',
  `cover_image` VARCHAR(255) COMMENT '封面图片地址',
  `video_url` VARCHAR(255) COMMENT '视频地址',
  `category_id` BIGINT COMMENT '分类ID',
  `duration` INT COMMENT '课程时长（分钟）',
  `view_count` BIGINT DEFAULT 0 COMMENT '浏览量',
  `status` TINYINT DEFAULT 1 COMMENT '状态0下架1上架',
  `author_id` BIGINT COMMENT '作者/讲师用户ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_author_id` (`author_id`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

CREATE TABLE IF NOT EXISTS `course_chapter` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `course_id` BIGINT NOT NULL COMMENT '所属课程ID',
  `title` VARCHAR(200) NOT NULL COMMENT '章节标题',
  `video_url` VARCHAR(255) COMMENT '章节视频地址',
  `duration` INT COMMENT '章节时长（分钟）',
  `sort_order` INT DEFAULT 0 COMMENT '排序序号',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程章节表';