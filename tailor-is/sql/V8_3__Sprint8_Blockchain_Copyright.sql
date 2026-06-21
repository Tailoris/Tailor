-- ============================================================
-- Sprint 8.3: 区块链版权 (CR-001~CR-008)
-- 创建日期: 2026-06-03
-- 关联模块: tailor-is-copyright
-- ============================================================

-- ⚠️ 警告：此脚本仅用于初始化部署，请勿在生产环境执行 DROP TABLE 操作
-- 生产环境数据库变更请使用 Flyway/Liquibase 版本化迁移工具管理

-- 1. 扩展版权记录表
ALTER TABLE `copyright_record`
    ADD COLUMN `author_real_name` VARCHAR(64) DEFAULT NULL COMMENT '作者真实姓名' AFTER `user_id`,
    ADD COLUMN `author_id_card` VARCHAR(255) DEFAULT NULL COMMENT '作者身份证(AES加密)' AFTER `author_real_name`,
    ADD COLUMN `author_phone` VARCHAR(32) DEFAULT NULL COMMENT '作者手机号(AES加密)' AFTER `author_id_card`,
    ADD COLUMN `creation_start_time` DATETIME DEFAULT NULL COMMENT '创作开始时间',
    ADD COLUMN `creation_end_time` DATETIME DEFAULT NULL COMMENT '创作结束时间',
    ADD COLUMN `evidence_chain` TEXT DEFAULT NULL COMMENT '完整证据链JSON',
    ADD COLUMN `version` INT DEFAULT 1 COMMENT '版本号(支持迭代)',
    ADD COLUMN `parent_id` BIGINT DEFAULT NULL COMMENT '上一版本ID',
    ADD COLUMN `is_commercial` TINYINT DEFAULT 1 COMMENT '是否商用:0否1是',
    ADD COLUMN `license_type` INT DEFAULT 1 COMMENT '许可类型:1=个人 2=企业 3=非商用 4=CC-BY 5=CC-BY-NC',
    ADD COLUMN `license_text` VARCHAR(500) DEFAULT NULL COMMENT '许可说明',
    ADD COLUMN `watermark_enabled` TINYINT DEFAULT 0 COMMENT '水印启用',
    ADD COLUMN `blockchain_block_height` BIGINT DEFAULT NULL COMMENT '区块高度',
    ADD COLUMN `blockchain_node` VARCHAR(64) DEFAULT NULL COMMENT '链上节点',
    ADD COLUMN `signature` VARCHAR(512) DEFAULT NULL COMMENT '数字签名',
    ADD COLUMN `audit_status` INT DEFAULT 0 COMMENT '审核状态:0待审1通过2拒绝',
    ADD COLUMN `audit_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
    ADD COLUMN `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    ADD COLUMN `audit_by` BIGINT DEFAULT NULL COMMENT '审核人',
    ADD KEY `idx_user_status` (`user_id`, `status`),
    ADD KEY `idx_hash` (`file_hash`),
    ADD KEY `idx_audit` (`audit_status`, `create_time`),
    ADD KEY `idx_commercial` (`is_commercial`, `license_type`);

-- 2. 区块链事件表（链上回调）
CREATE TABLE IF NOT EXISTS `cr_blockchain_event` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `record_id` BIGINT DEFAULT NULL COMMENT '版权记录ID',
    `event_type` VARCHAR(32) NOT NULL COMMENT '事件类型:STORAGE_SUCCESS/CERT_ISSUED/REVOKE',
    `tx_hash` VARCHAR(128) DEFAULT NULL COMMENT '交易哈希',
    `block_height` BIGINT DEFAULT NULL COMMENT '区块高度',
    `block_time` DATETIME DEFAULT NULL COMMENT '区块时间',
    `platform` VARCHAR(32) DEFAULT NULL COMMENT '区块链平台',
    `node` VARCHAR(64) DEFAULT NULL COMMENT '节点',
    `event_data` TEXT DEFAULT NULL COMMENT '事件数据',
    `processed` TINYINT DEFAULT 0 COMMENT '是否已处理',
    `process_time` DATETIME DEFAULT NULL COMMENT '处理时间',
    `process_result` VARCHAR(500) DEFAULT NULL COMMENT '处理结果',
    `retry_count` INT DEFAULT 0 COMMENT '重试次数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_record` (`record_id`),
    KEY `idx_event_type` (`event_type`, `processed`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区块链事件表';

-- 3. 巡检任务表
CREATE TABLE IF NOT EXISTS `cr_inspection_task` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `task_type` INT NOT NULL COMMENT '任务类型:1机器日检 2人工月检 3专项',
    `task_name` VARCHAR(100) NOT NULL COMMENT '任务名称',
    `target_type` INT DEFAULT NULL COMMENT '巡检对象类型:1=版权 2=商品 3=订单',
    `target_id` BIGINT DEFAULT NULL COMMENT '巡检对象ID',
    `check_items` TEXT DEFAULT NULL COMMENT '检测项JSON',
    `check_result` INT DEFAULT NULL COMMENT '检查结果:0正常1异常2违规',
    `result_detail` TEXT DEFAULT NULL COMMENT '结果详情',
    `evidence` TEXT DEFAULT NULL COMMENT '证据(JSON)',
    `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `duration_ms` BIGINT DEFAULT NULL COMMENT '耗时毫秒',
    `executor_id` BIGINT DEFAULT NULL COMMENT '执行人ID(机器=0)',
    `executor_type` INT DEFAULT NULL COMMENT '执行人类型:1系统2人工',
    `status` INT DEFAULT 0 COMMENT '状态:0待执行1执行中2已完成3失败',
    `scheduled_time` DATETIME DEFAULT NULL COMMENT '计划执行时间',
    `next_run_time` DATETIME DEFAULT NULL COMMENT '下次执行时间',
    `cron_expr` VARCHAR(50) DEFAULT NULL COMMENT 'cron表达式',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_type_status` (`task_type`, `status`),
    KEY `idx_target` (`target_type`, `target_id`),
    KEY `idx_scheduled` (`scheduled_time`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检任务表';

-- 4. 违规处置记录表
CREATE TABLE IF NOT EXISTS `cr_violation_handling` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `record_id` BIGINT DEFAULT NULL COMMENT '版权记录ID',
    `inspection_id` BIGINT DEFAULT NULL COMMENT '巡检任务ID',
    `violation_type` INT NOT NULL COMMENT '违规类型:1抄袭2盗用3商用违规4其他',
    `violation_level` INT DEFAULT 1 COMMENT '违规等级:1轻微2一般3严重4重大',
    `description` VARCHAR(1000) DEFAULT NULL COMMENT '违规描述',
    `evidence_urls` TEXT DEFAULT NULL COMMENT '证据URLs',
    `handle_type` INT DEFAULT NULL COMMENT '处置方式:1警告2下架3封禁4追诉5删除',
    `handle_remark` VARCHAR(500) DEFAULT NULL COMMENT '处置说明',
    `handler_id` BIGINT DEFAULT NULL COMMENT '处置人',
    `handle_time` DATETIME DEFAULT NULL COMMENT '处置时间',
    `status` INT DEFAULT 0 COMMENT '状态:0待处置1已处置2申诉中3已申诉',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_record` (`record_id`),
    KEY `idx_inspection` (`inspection_id`),
    KEY `idx_type_level` (`violation_type`, `violation_level`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='违规处置表';

-- 5. 侵权检测与维权表
CREATE TABLE IF NOT EXISTS `cr_infringement_case` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `case_no` VARCHAR(64) NOT NULL COMMENT '案件编号',
    `record_id` BIGINT NOT NULL COMMENT '版权记录ID',
    `copyright_user_id` BIGINT NOT NULL COMMENT '版权人ID',
    `infringer_user_id` BIGINT DEFAULT NULL COMMENT '侵权人ID',
    `infringer_name` VARCHAR(100) DEFAULT NULL COMMENT '侵权人姓名/公司',
    `infringer_contact` VARCHAR(200) DEFAULT NULL COMMENT '侵权人联系方式',
    `infringement_source` VARCHAR(500) DEFAULT NULL COMMENT '侵权来源URL',
    `discovered_at` DATETIME DEFAULT NULL COMMENT '发现时间',
    `infringement_type` INT DEFAULT 1 COMMENT '侵权类型:1完全抄袭2部分抄袭3商用违规4未授权使用',
    `similarity_score` DECIMAL(5,2) DEFAULT NULL COMMENT '相似度(0-100)',
    `evidence_chain` TEXT DEFAULT NULL COMMENT '证据链JSON',
    `evidence_files` TEXT DEFAULT NULL COMMENT '证据文件清单JSON',
    `encrypted_evidence_key` VARCHAR(255) DEFAULT NULL COMMENT '加密证据密钥',
    `status` INT DEFAULT 0 COMMENT '状态:0待处理1取证中2申诉中3仲裁中4已立案5胜诉6败诉7和解8撤诉',
    `arbitration_deadline` DATETIME DEFAULT NULL COMMENT '仲裁截止(72小时)',
    `arbitration_result` VARCHAR(2000) DEFAULT NULL COMMENT '仲裁结果',
    `arbitrator_id` BIGINT DEFAULT NULL COMMENT '仲裁员ID',
    `arbitration_at` DATETIME DEFAULT NULL COMMENT '仲裁时间',
    `court_name` VARCHAR(100) DEFAULT NULL COMMENT '受理法院',
    `court_case_no` VARCHAR(64) DEFAULT NULL COMMENT '法院案号',
    `lawyer_name` VARCHAR(64) DEFAULT NULL COMMENT '代理律师',
    `lawyer_contact` VARCHAR(64) DEFAULT NULL COMMENT '律师联系方式',
    `compensation` DECIMAL(18,2) DEFAULT NULL COMMENT '判赔金额',
    `closed_at` DATETIME DEFAULT NULL COMMENT '结案时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_case_no` (`case_no`),
    KEY `idx_record` (`record_id`),
    KEY `idx_user` (`copyright_user_id`, `status`),
    KEY `idx_status` (`status`, `arbitration_deadline`),
    KEY `idx_discover` (`discovered_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='侵权案件表';

-- 6. 侵权案件流转日志
CREATE TABLE IF NOT EXISTS `cr_infringement_log` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `case_id` BIGINT NOT NULL COMMENT '案件ID',
    `from_status` INT DEFAULT NULL COMMENT '原状态',
    `to_status` INT NOT NULL COMMENT '新状态',
    `action` VARCHAR(50) DEFAULT NULL COMMENT '动作',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人',
    `operator_name` VARCHAR(64) DEFAULT NULL COMMENT '操作人名称',
    `operator_type` INT DEFAULT 1 COMMENT '操作人类型:1用户2运营3仲裁员4系统',
    `attachments` TEXT DEFAULT NULL COMMENT '附件JSON',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_case` (`case_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='侵权案件流转日志';

-- 7. 相似度检测记录
CREATE TABLE IF NOT EXISTS `cr_similarity_check` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `source_record_id` BIGINT NOT NULL COMMENT '源版权ID',
    `target_record_id` BIGINT DEFAULT NULL COMMENT '目标版权ID(库内比对)',
    `target_url` VARCHAR(500) DEFAULT NULL COMMENT '目标URL(库外比对)',
    `similarity_score` DECIMAL(5,2) NOT NULL COMMENT '相似度(0-100)',
    `check_method` VARCHAR(50) DEFAULT NULL COMMENT '检测方法:AI/AHASH/PERCEPTUAL',
    `check_engine` VARCHAR(64) DEFAULT NULL COMMENT '检测引擎',
    `check_cost_ms` BIGINT DEFAULT NULL COMMENT '检测耗时',
    `evidence_image_url` VARCHAR(500) DEFAULT NULL COMMENT '对比图',
    `check_time` DATETIME DEFAULT NULL COMMENT '检测时间',
    `is_infringement` TINYINT DEFAULT NULL COMMENT '是否侵权',
    `risk_level` INT DEFAULT NULL COMMENT '风险等级1-4',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_source` (`source_record_id`),
    KEY `idx_target` (`target_record_id`),
    KEY `idx_score` (`similarity_score`),
    KEY `idx_infringe` (`is_infringement`, `risk_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='相似度检测记录';

-- 8. 黑白名单库
CREATE TABLE IF NOT EXISTS `cr_ip_blacklist` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `list_type` INT NOT NULL COMMENT '1黑名单 2白名单 3灰名单',
    `target_type` INT NOT NULL COMMENT '1用户 2IP 3文件Hash 4内容关键词',
    `target_value` VARCHAR(500) NOT NULL COMMENT '目标值',
    `reason` VARCHAR(500) DEFAULT NULL COMMENT '原因',
    `evidence` TEXT DEFAULT NULL COMMENT '证据',
    `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间(永久=NULL)',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_type_value` (`list_type`, `target_type`, `target_value`(64)),
    KEY `idx_expire` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='黑白名单库';

-- 9. 通知消息表
CREATE TABLE IF NOT EXISTS `cr_notification` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '接收人',
    `biz_type` VARCHAR(32) NOT NULL COMMENT '业务类型:VIOLATION/INFRINGEMENT/INSPECTION',
    `biz_id` BIGINT DEFAULT NULL COMMENT '业务ID',
    `title` VARCHAR(200) DEFAULT NULL COMMENT '标题',
    `content` TEXT DEFAULT NULL COMMENT '内容',
    `level` INT DEFAULT 1 COMMENT '级别:1普通2重要3紧急',
    `is_read` TINYINT DEFAULT 0 COMMENT '已读',
    `read_time` DATETIME DEFAULT NULL COMMENT '阅读时间',
    `send_email` TINYINT DEFAULT 0 COMMENT '邮件已发送',
    `send_sms` TINYINT DEFAULT 0 COMMENT '短信已发送',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_read` (`user_id`, `is_read`, `create_time`),
    KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知消息表';

-- 10. 存证证书文件表
CREATE TABLE IF NOT EXISTS `cr_certificate_file` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `record_id` BIGINT NOT NULL COMMENT '版权记录ID',
    `cert_no` VARCHAR(64) NOT NULL COMMENT '证书编号',
    `file_url` VARCHAR(500) DEFAULT NULL COMMENT 'PDF文件URL',
    `qr_code_url` VARCHAR(500) DEFAULT NULL COMMENT '二维码URL',
    `qr_content` VARCHAR(500) DEFAULT NULL COMMENT '二维码内容(验证链接)',
    `signature` VARCHAR(1024) DEFAULT NULL COMMENT '数字签名',
    `signed_at` DATETIME DEFAULT NULL COMMENT '签名时间',
    `file_size` BIGINT DEFAULT NULL COMMENT '文件大小',
    `page_count` INT DEFAULT 1 COMMENT '页数',
    `download_count` INT DEFAULT 0 COMMENT '下载次数',
    `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
    `status` INT DEFAULT 0 COMMENT '状态:0生成中1就绪2失效',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_record` (`record_id`),
    KEY `idx_cert` (`cert_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='证书文件表';

-- 11. 区块链平台配置
CREATE TABLE IF NOT EXISTS `cr_blockchain_config` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `platform_code` VARCHAR(32) NOT NULL COMMENT '平台编码:ANTCHAIN/ZHIXIN/BSN',
    `platform_name` VARCHAR(64) NOT NULL COMMENT '平台名称',
    `endpoint` VARCHAR(200) DEFAULT NULL COMMENT 'API端点',
    `api_key` VARCHAR(500) DEFAULT NULL COMMENT 'API Key',
    `api_secret` VARCHAR(500) DEFAULT NULL COMMENT 'API Secret',
    `contract_name` VARCHAR(100) DEFAULT NULL COMMENT '合约名',
    `chain_id` VARCHAR(64) DEFAULT NULL COMMENT '链ID',
    `is_default` TINYINT DEFAULT 0 COMMENT '是否默认',
    `is_active` TINYINT DEFAULT 1 COMMENT '启用',
    `priority` INT DEFAULT 0 COMMENT '优先级',
    `qps_limit` INT DEFAULT 100 COMMENT 'QPS限制',
    `daily_limit` BIGINT DEFAULT 10000 COMMENT '日调用上限',
    `daily_used` BIGINT DEFAULT 0 COMMENT '已使用',
    `last_reset_date` DATE DEFAULT NULL COMMENT '上次重置日期',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`platform_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区块链平台配置';

-- 12. 初始化数据
INSERT INTO `cr_blockchain_config`
    (`id`, `platform_code`, `platform_name`, `endpoint`, `is_default`, `is_active`, `priority`, `qps_limit`, `daily_limit`)
VALUES
    (1, 'ANTCHAIN', '蚂蚁链', 'https://antchain.openapi.example/api', 1, 1, 100, 200, 50000),
    (2, 'ZHIXIN', '至信链', 'https://zhixin.openapi.example/api', 0, 1, 80, 150, 30000),
    (3, 'BSN', 'BSN文昌链', 'https://bsn.openapi.example/api', 0, 1, 60, 100, 20000);

-- 13. 索引优化
ALTER TABLE `copyright_record` ADD KEY `idx_commercial_license` (`is_commercial`, `license_type`, `deleted`);
ALTER TABLE `cr_infringement_case` ADD KEY `idx_user_status_discover` (`copyright_user_id`, `status`, `discovered_at`);
