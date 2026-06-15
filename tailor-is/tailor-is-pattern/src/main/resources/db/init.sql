CREATE TABLE IF NOT EXISTS `pattern` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL COMMENT '版型名称',
  `description` TEXT COMMENT '版型描述',
  `category` VARCHAR(50) COMMENT '版型分类',
  `image_url` VARCHAR(255) COMMENT '版型图片',
  `dimensions` JSON COMMENT '规格尺寸',
  `status` TINYINT DEFAULT 1 COMMENT '状态0禁用1启用',
  `merchant_id` BIGINT COMMENT '商户ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='版型表';