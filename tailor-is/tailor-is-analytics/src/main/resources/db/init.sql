CREATE TABLE IF NOT EXISTS `metrics_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `metric_type` VARCHAR(50) NOT NULL COMMENT '指标类型revenue/order/user/view等',
  `metric_key` VARCHAR(100) NOT NULL COMMENT '指标键名',
  `metric_value` DECIMAL(20, 4) COMMENT '指标数值',
  `snapshot_date` DATE NOT NULL COMMENT '快照日期',
  `dimension` VARCHAR(200) COMMENT '维度信息JSON格式',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_metric_type_date` (`metric_type`, `snapshot_date`),
  KEY `idx_metric_key` (`metric_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标快照表';