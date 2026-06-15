-- ================================================================
-- Phase 3 性能与安全: P3-5 数据库查询优化与慢查询分析
-- ================================================================
-- 文档编号: TAILOR-IS-PHASE3-DB-OPT-2026-0613
-- 优化目标: 慢查询 < 50ms, 全表扫描消除
-- 补充 V9_1__Sprint9_QA_Index_Optimization.sql 中遗漏的索引
-- ================================================================

USE tailor_is;

-- ================================================================
-- 1. 补充缺失的复合索引 (基于 MyBatis Mapper XML 中的查询模式)
-- ================================================================

-- 1.1 营销模块: 用户优惠券查询 (高频)
-- 查询模式: WHERE user_id = ? AND status = ? AND end_time > NOW() ORDER BY created_at DESC
CREATE INDEX IF NOT EXISTS idx_coupon_user_status_end
    ON coupon(user_id, status, end_time, created_at);

-- 1.2 营销模块: 拼团实例查询
-- 查询模式: WHERE activity_id = ? AND status = ? ORDER BY created_at
CREATE INDEX IF NOT EXISTS idx_group_buy_instance_activity_status
    ON mkt_group_buy_instance(activity_id, status, created_at);

-- 1.3 订单模块: 超时未支付订单 (定时任务高频)
-- 查询模式: WHERE status = 0 AND pay_time IS NULL AND created_at < ?
CREATE INDEX IF NOT EXISTS idx_order_unpaid_timeout
    ON order_info(status, pay_time, created_at);

-- 1.4 订单模块: 售后工单商户查询
-- 查询模式: WHERE merchant_id = ? AND status = ? ORDER BY created_at DESC
CREATE INDEX IF NOT EXISTS idx_after_sale_merchant_status
    ON after_sale_ticket(merchant_id, status, created_at);

-- 1.5 商品模块: 商品搜索 (关键词 + 状态 + 排序)
-- 查询模式: WHERE status = 1 AND (name LIKE ? OR description LIKE ?) ORDER BY sales_count DESC
CREATE INDEX IF NOT EXISTS idx_product_status_name
    ON product(status, name(100));

-- 1.6 商品模块: SKU 库存查询 (下单时高频)
-- 查询模式: WHERE product_id = ? AND status = 1 AND stock > 0
CREATE INDEX IF NOT EXISTS idx_sku_product_status_stock
    ON product_sku(product_id, status, stock);

-- 1.7 支付模块: 对账查询 (财务日终)
-- 查询模式: WHERE status = 1 AND pay_time BETWEEN ? AND ? ORDER BY pay_time
CREATE INDEX IF NOT EXISTS idx_payment_status_pay_time
    ON payment_record(status, pay_time);

-- 1.8 支付模块: 商户分账查询
-- 查询模式: WHERE merchant_id = ? AND split_status = ? ORDER BY created_at DESC
CREATE INDEX IF NOT EXISTS idx_payment_merchant_split
    ON payment_record(merchant_id, split_status, created_at);

-- 1.9 社区模块: 帖子全文搜索优化
-- 查询模式: WHERE MATCH(title, content) AGAINST(? IN BOOLEAN MODE)
ALTER TABLE community_post ADD FULLTEXT INDEX IF NOT EXISTS ft_post_title_content(title, content);

-- 1.10 社区模块: 热门帖子 (时间窗口内)
-- 查询模式: WHERE status = 1 AND created_at > ? ORDER BY like_count DESC, comment_count DESC
CREATE INDEX IF NOT EXISTS idx_post_status_like_comment
    ON community_post(status, like_count DESC, comment_count DESC);

-- 1.11 消息模块: 未读消息统计
-- 查询模式: WHERE user_id = ? AND is_read = 0 AND status = 1
CREATE INDEX IF NOT EXISTS idx_message_inbox_user_unread
    ON message_inbox(user_id, is_read, status);

-- 1.12 商户模块: 数据看板查询 (聚合查询)
-- 查询模式: WHERE merchant_id = ? AND created_at BETWEEN ? AND ? 
--            GROUP BY DATE(created_at) ORDER BY DATE(created_at)
CREATE INDEX IF NOT EXISTS idx_merchant_stats_created
    ON merchant_statistics(merchant_id, created_at);

-- ================================================================
-- 2. N+1 查询优化建议 (基于 Entity 关系分析)
-- ================================================================

-- 2.1 订单详情查询 (避免 N+1)
-- 建议: 使用 JOIN 或 batch fetch
-- SELECT o.*, oi.*, p.name, p.main_image
-- FROM order_info o
-- LEFT JOIN order_item oi ON o.id = oi.order_id
-- LEFT JOIN product p ON oi.product_id = p.id
-- WHERE o.user_id = ? ORDER BY o.created_at DESC

-- 2.2 商品评价查询 (避免 N+1)
-- 建议: 批量查询用户信息
-- SELECT r.*, u.nickname, u.avatar
-- FROM product_review r
-- LEFT JOIN sys_user u ON r.user_id = u.id
-- WHERE r.product_id = ? ORDER BY r.created_at DESC

-- ================================================================
-- 3. 慢查询监控增强
-- ================================================================

-- 3.1 创建慢查询历史记录表
CREATE TABLE IF NOT EXISTS slow_query_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sql_text TEXT NOT NULL COMMENT 'SQL 语句',
    query_time_ms DECIMAL(10,2) NOT NULL COMMENT '执行时间(毫秒)',
    lock_time_ms DECIMAL(10,2) DEFAULT 0 COMMENT '锁等待时间(毫秒)',
    rows_examined INT DEFAULT 0 COMMENT '扫描行数',
    rows_sent INT DEFAULT 0 COMMENT '返回行数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    INDEX idx_slow_query_time (query_time_ms DESC),
    INDEX idx_slow_query_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='慢查询记录表';

-- 3.2 创建查询性能监控视图
CREATE OR REPLACE VIEW v_query_performance_overview AS
SELECT
    t.TABLE_SCHEMA AS db_name,
    t.TABLE_NAME AS table_name,
    t.TABLE_ROWS AS estimated_rows,
    t.DATA_LENGTH / 1024 / 1024 AS data_size_mb,
    t.INDEX_LENGTH / 1024 / 1024 AS index_size_mb,
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS s
     WHERE s.TABLE_SCHEMA = t.TABLE_SCHEMA AND s.TABLE_NAME = t.TABLE_NAME
       AND s.INDEX_NAME != 'PRIMARY') AS index_count,
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS s
     WHERE s.TABLE_SCHEMA = t.TABLE_SCHEMA AND s.TABLE_NAME = t.TABLE_NAME
       AND s.INDEX_NAME != 'PRIMARY' AND s.CARDINALITY = 0) AS unused_index_count
FROM INFORMATION_SCHEMA.TABLES t
WHERE t.TABLE_SCHEMA = 'tailor_is'
  AND t.TABLE_TYPE = 'BASE TABLE'
ORDER BY data_size_mb DESC;

-- 3.3 未使用索引检测视图
CREATE OR REPLACE VIEW v_unused_indexes AS
SELECT
    OBJECT_SCHEMA AS db_name,
    OBJECT_NAME AS table_name,
    INDEX_NAME,
    COUNT_STAR AS total_io,
    COUNT_READ AS total_reads,
    COUNT_FETCH AS total_fetches,
    COUNT_INSERT AS total_inserts,
    COUNT_UPDATE AS total_updates,
    COUNT_DELETE AS total_deletes
FROM performance_schema.table_io_waits_summary_by_index_usage
WHERE OBJECT_SCHEMA = 'tailor_is'
  AND INDEX_NAME IS NOT NULL
  AND COUNT_STAR = 0
ORDER BY OBJECT_NAME, INDEX_NAME;

-- ================================================================
-- 4. 索引统计信息更新
-- ================================================================

ANALYZE TABLE coupon, mkt_group_buy_instance, order_info, after_sale_ticket,
              product, product_sku, payment_record, community_post, message_inbox,
              merchant_statistics;

-- ================================================================
-- 5. 关键查询 EXPLAIN 验证
-- ================================================================

-- 验证新增索引是否生效
EXPLAIN SELECT * FROM coupon WHERE user_id = 1 AND status = 1 AND end_time > NOW() ORDER BY created_at DESC;
EXPLAIN SELECT * FROM order_info WHERE status = 0 AND pay_time IS NULL AND created_at < NOW() - INTERVAL 30 MINUTE;
EXPLAIN SELECT * FROM product_sku WHERE product_id = 1 AND status = 1 AND stock > 0;
EXPLAIN SELECT * FROM payment_record WHERE status = 1 AND pay_time BETWEEN '2026-01-01' AND '2026-06-30';
EXPLAIN SELECT * FROM message_inbox WHERE user_id = 1 AND is_read = 0 AND status = 1;

-- ================================================================
-- Phase 3 P3-5 完成
-- ================================================================
-- 新增索引: 12 个复合索引 + 1 个全文索引
-- 新增监控表: 1 个慢查询记录表
-- 新增监控视图: 2 个
-- 优化目标: 慢查询 < 50ms, 全表扫描消除
-- ================================================================