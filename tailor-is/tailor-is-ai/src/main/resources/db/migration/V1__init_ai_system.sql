-- ============================================================
-- Tailor IS - Flyway Migration V1: AI算法系统数据库初始化
-- 数据库: tailor_is_ai
-- 创建时间: 2026-07-10
-- 说明: 基线迁移，创建AI算法系统所有表结构
-- ============================================================

CREATE TABLE IF NOT EXISTS `body_size_data` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '人体尺寸数据ID（主键）',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `height` DECIMAL(5, 1) COMMENT '身高(cm)',
  `weight` DECIMAL(5, 1) COMMENT '体重(kg)',
  `chest` DECIMAL(5, 1) COMMENT '胸围(cm)',
  `waist` DECIMAL(5, 1) COMMENT '腰围(cm)',
  `hip` DECIMAL(5, 1) COMMENT '臀围(cm)',
  `shoulder_width` DECIMAL(5, 1) COMMENT '肩宽(cm)',
  `arm_length` DECIMAL(5, 1) COMMENT '袖长(cm)',
  `leg_length` DECIMAL(5, 1) COMMENT '裤长(cm)',
  `neck_circumference` DECIMAL(5, 1) COMMENT '颈围(cm)',
  `bust` DECIMAL(5, 1) COMMENT '上胸围(cm)',
  `underbust` DECIMAL(5, 1) COMMENT '下胸围(cm)',
  `size_type` VARCHAR(32) COMMENT '体型类型：standard/slim/athletic/curvy/petite/tall',
  `remark` VARCHAR(500) COMMENT '备注信息',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-无效，1-有效',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_size_type` (`size_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='人体尺寸数据表';

CREATE TABLE IF NOT EXISTS `pattern_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '纸样记录ID（主键）',
  `pattern_no` VARCHAR(64) NOT NULL COMMENT '纸样编号',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `pattern_type` VARCHAR(50) NOT NULL COMMENT '纸样类型：DRESS/JACKET/SHIRT/PANTS/SKIRT/COAT等',
  `body_size_id` BIGINT UNSIGNED COMMENT '关联人体尺寸ID',
  `pattern_config` JSON COMMENT '纸样配置参数',
  `pattern_data` LONGTEXT COMMENT '纸样数据（DXF/JSON格式）',
  `image_url` VARCHAR(512) COMMENT '纸样预览图URL',
  `status` TINYINT DEFAULT 0 COMMENT '状态：0-生成中，1-生成成功，2-生成失败',
  `error_message` VARCHAR(1000) COMMENT '错误信息',
  `model_used` VARCHAR(100) COMMENT '使用的AI模型',
  `version` INT DEFAULT 1 COMMENT '版本号',
  `merchant_id` BIGINT UNSIGNED COMMENT '商户ID',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pattern_no` (`pattern_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_pattern_type` (`pattern_type`),
  KEY `idx_body_size_id` (`body_size_id`),
  KEY `idx_status` (`status`),
  KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='纸样记录表';

CREATE TABLE IF NOT EXISTS `pattern_version` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '版本ID（主键）',
  `pattern_id` BIGINT UNSIGNED NOT NULL COMMENT '纸样记录ID',
  `pattern_no` VARCHAR(64) NOT NULL COMMENT '纸样编号（冗余）',
  `version` INT NOT NULL COMMENT '版本号',
  `change_log` VARCHAR(1000) COMMENT '变更日志',
  `pattern_data` LONGTEXT COMMENT '纸样数据（DXF/JSON格式）',
  `image_url` VARCHAR(512) COMMENT '纸样预览图URL',
  `is_current` TINYINT DEFAULT 0 COMMENT '是否当前版本：0-否，1-是',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pattern_version` (`pattern_id`, `version`),
  KEY `idx_pattern_id` (`pattern_id`),
  KEY `idx_is_current` (`is_current`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='纸样版本表';

CREATE TABLE IF NOT EXISTS `pattern_iteration` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '迭代记录ID（主键）',
  `pattern_id` BIGINT UNSIGNED NOT NULL COMMENT '纸样记录ID',
  `iteration_no` VARCHAR(64) NOT NULL COMMENT '迭代编号',
  `iteration_count` INT DEFAULT 1 COMMENT '迭代次数',
  `user_feedback` TEXT COMMENT '用户反馈',
  `adjustment_params` JSON COMMENT '调整参数',
  `status` TINYINT DEFAULT 0 COMMENT '状态：0-处理中，1-完成，2-失败',
  `before_image_url` VARCHAR(512) COMMENT '迭代前预览图',
  `after_image_url` VARCHAR(512) COMMENT '迭代后预览图',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iteration_no` (`iteration_no`),
  KEY `idx_pattern_id` (`pattern_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='纸样迭代记录表';

CREATE TABLE IF NOT EXISTS `ai_task` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '任务ID（主键）',
  `task_no` VARCHAR(64) NOT NULL COMMENT '任务编号',
  `task_type` VARCHAR(50) NOT NULL COMMENT '任务类型：pattern_generate/pattern_iteration/body_analysis/optimization等',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `input_data` TEXT COMMENT '输入数据JSON',
  `output_data` LONGTEXT COMMENT '输出数据',
  `status` TINYINT DEFAULT 0 COMMENT '状态：0-排队中，1-处理中，2-完成，3-失败，4-超时',
  `progress` INT DEFAULT 0 COMMENT '进度百分比0-100',
  `error_message` VARCHAR(1000) COMMENT '错误信息',
  `model_name` VARCHAR(100) COMMENT '使用的模型名称',
  `priority` TINYINT DEFAULT 0 COMMENT '优先级：0-普通，1-加急',
  `queue_name` VARCHAR(100) COMMENT '队列名称',
  `start_time` DATETIME COMMENT '开始时间',
  `end_time` DATETIME COMMENT '结束时间',
  `execution_time_ms` BIGINT COMMENT '执行时长（毫秒）',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_no` (`task_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_task_type` (`task_type`),
  KEY `idx_status` (`status`),
  KEY `idx_priority` (`priority`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI任务表';

CREATE TABLE IF NOT EXISTS `model_performance` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '性能记录ID（主键）',
  `model_name` VARCHAR(100) NOT NULL COMMENT '模型名称',
  `task_type` VARCHAR(50) COMMENT '任务类型',
  `request_count` INT DEFAULT 0 COMMENT '请求次数',
  `success_count` INT DEFAULT 0 COMMENT '成功次数',
  `failure_count` INT DEFAULT 0 COMMENT '失败次数',
  `avg_response_time_ms` DECIMAL(10, 2) COMMENT '平均响应时间（毫秒）',
  `min_response_time_ms` INT COMMENT '最小响应时间（毫秒）',
  `max_response_time_ms` INT COMMENT '最大响应时间（毫秒）',
  `error_rate` DECIMAL(5, 2) COMMENT '错误率',
  `performance_date` DATE NOT NULL COMMENT '统计日期',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_model_date` (`model_name`, `performance_date`),
  KEY `idx_model_name` (`model_name`),
  KEY `idx_performance_date` (`performance_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='模型性能统计表';