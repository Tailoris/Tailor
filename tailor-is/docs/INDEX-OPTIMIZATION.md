# 数据库索引优化指南

> 本文档为 Tailor IS 平台提供数据库索引设计、分析和维护的最佳实践。

## 目录

- [慢查询分析](#慢查询分析)
- [索引设计原则](#索引设计原则)
- [核心表索引设计示例](#核心表索引设计示例)
- [索引监控与维护](#索引监控与维护)
- [相关文档](#相关文档)

---

## 慢查询分析

### 1. 开启慢查询日志

```sql
-- 开启慢查询日志（生产环境建议在 my.cnf 中配置）
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL slow_query_log_file = '/var/log/mysql/slow-query.log';
SET GLOBAL long_query_time = 1;  -- 超过1秒的查询记录为慢查询
SET GLOBAL log_queries_not_using_indexes = 'ON';
```

### 2. 分析慢查询

```bash
# 使用 mysqldumpslow 工具分析慢查询日志
mysqldumpslow -s t -t 10 /var/log/mysql/slow-query.log

# -s t: 按总时间排序
# -t 10: 显示前10条
```

### 3. 使用 EXPLAIN 分析查询计划

```sql
-- 查看查询是否使用索引
EXPLAIN SELECT * FROM order_info WHERE user_id = 1 AND status = 1 ORDER BY created_at DESC LIMIT 20;

-- 关注字段：
-- type: 访问类型 (ALL < index < range < ref < eq_ref < const)
-- key: 实际使用的索引
-- rows: 扫描行数
-- Extra: 额外信息（Using filesort 表示需要额外排序）
```

### 4. 使用 SHOW PROFILE 分析查询各阶段耗时

```sql
SET profiling = 1;
SELECT * FROM order_info WHERE user_id = 1 LIMIT 100;
SHOW PROFILES;
SHOW PROFILE FOR QUERY 1;
```

### 5. 识别全表扫描

```sql
-- 查询执行计划中 type 为 ALL 的语句
SELECT * FROM performance_schema.events_statements_summary_by_digest
WHERE DIGEST_TEXT LIKE '%SELECT%'
ORDER BY AVG_TIMER_WAIT DESC
LIMIT 10;
```

---

## 索引设计原则

### 1. 最左前缀原则

复合索引 `(a, b, c)` 可以支持以下查询：
- `WHERE a = ?`
- `WHERE a = ? AND b = ?`
- `WHERE a = ? AND b = ? AND c = ?`

**但不能支持**：
- `WHERE b = ?` （缺少最左列）
- `WHERE b = ? AND c = ?` （缺少最左列）

### 2. 索引列选择优先级

| 优先级 | 列特征 | 示例 |
|--------|--------|------|
| 1 | WHERE 条件列（高选择性） | `user_id`, `order_no` |
| 2 | JOIN 连接列 | `order_id`, `product_id` |
| 3 | ORDER BY 列 | `created_at`, `price` |
| 4 | GROUP BY 列 | `merchant_id`, `category_id` |

### 3. 避免过度索引

- 单表索引数量建议不超过 **10 个**
- 每个索引都会增加 INSERT/UPDATE/DELETE 的开销
- 定期清理未使用的索引

### 4. 选择性原则

索引列的选择性 = `COUNT(DISTINCT column) / COUNT(*)`

- 选择性 > 0.1：适合建索引
- 选择性 < 0.01：不适合建索引（如 `status` 列只有几个枚举值）

### 5. 覆盖索引优先

```sql
-- 好：索引覆盖，不需要回表
CREATE INDEX idx_order_user_created ON order_info(user_id, created_at, order_no);
-- 查询 SELECT user_id, created_at, order_no WHERE user_id = ? 可以直接从索引获取
```

### 6. 避免在索引列上使用函数

```sql
-- 慢：无法使用索引
SELECT * FROM order_info WHERE DATE(created_at) = '2024-01-01';

-- 快：可以使用范围索引
SELECT * FROM order_info WHERE created_at >= '2024-01-01' AND created_at < '2024-01-02';
```

---

## 核心表索引设计示例

### 1. 订单表 (order_info)

**常见查询模式：**

| 查询场景 | SQL 示例 | 推荐索引 |
|---------|---------|---------|
| 用户订单列表 | `WHERE user_id = ? ORDER BY created_at DESC` | `idx_order_user_created (user_id, created_at)` |
| 用户+状态过滤 | `WHERE user_id = ? AND status = ?` | `idx_order_user_status_created (user_id, status, created_at)` |
| 商户订单列表 | `WHERE merchant_id = ? AND status = ?` | `idx_order_merchant_status (merchant_id, status)` |
| 订单号查询 | `WHERE order_no = ?` | `idx_order_order_no (order_no)` |
| 超时订单清理 | `WHERE status = ? AND created_at < ?` | `idx_order_status_created (status, created_at)` |

**已创建索引（参见 `sql/V9_1__Sprint9_QA_Index_Optimization.sql`）：**
- `idx_order_user_status_created` - 覆盖用户订单列表+状态过滤
- `idx_order_user_created` - 覆盖用户订单时间排序
- `idx_order_merchant_status` - 覆盖商户订单管理
- `idx_order_status_pay_time` - 覆盖支付状态查询
- `idx_order_status_created` - 覆盖超时订单清理
- `idx_order_order_no` - 覆盖订单号精确查询

### 2. 商品表 (product)

**常见查询模式：**

| 查询场景 | SQL 示例 | 推荐索引 |
|---------|---------|---------|
| 商户商品列表 | `WHERE merchant_id = ? AND status = ?` | `idx_product_merchant_status (merchant_id, status)` |
| 分类商品列表 | `WHERE category_id = ? AND status = ?` | `idx_product_category_status (category_id, status)` |
| 推荐商品 | `WHERE status = 1 AND is_recommend = 1 ORDER BY created_at` | `idx_product_status_recommend (status, is_recommend, created_at)` |
| 销量排行 | `WHERE status = 1 ORDER BY sales_count DESC` | `idx_product_status_sales (status, sales_count DESC)` |

### 3. 用户表 (sys_user)

**常见查询模式：**

| 查询场景 | SQL 示例 | 推荐索引 |
|---------|---------|---------|
| 手机号登录 | `WHERE phone = ?` | `idx_sys_user_phone (phone)` |
| 邮箱登录 | `WHERE email = ?` | `idx_sys_user_email (email)` |
| 用户状态过滤 | `WHERE status = ? ORDER BY created_at` | `idx_sys_user_status_created (status, created_at)` |

---

## 索引监控与维护

### 1. 查看索引使用情况

```sql
-- 查看表的索引信息
SHOW INDEX FROM order_info;

-- 查看索引基数（Cardinality），值越高区分度越好
SELECT
    INDEX_NAME,
    CARDINALITY,
    COLUMN_NAME
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = 'tailor_is' AND TABLE_NAME = 'order_info';
```

### 2. 识别未使用的索引

```sql
-- MySQL 8.0+ 使用 performance_schema
SELECT
    OBJECT_SCHEMA,
    OBJECT_NAME,
    INDEX_NAME
FROM performance_schema.table_io_waits_summary_by_index_usage
WHERE INDEX_NAME IS NOT NULL
  AND COUNT_STAR = 0
  AND OBJECT_SCHEMA = 'tailor_is'
ORDER BY OBJECT_NAME, INDEX_NAME;
```

### 3. 索引碎片清理

```sql
-- 定期优化表（重组索引，释放空间）
OPTIMIZE TABLE order_info;
OPTIMIZE TABLE product;
OPTIMIZE TABLE sys_user;
```

### 4. 更新索引统计信息

```sql
-- 优化器依赖统计信息做执行计划选择
ANALYZE TABLE order_info, product, sys_user;
```

### 5. 监控视图

已创建 `v_slow_query_monitor` 视图用于监控索引状态：

```sql
-- 查看所有非主键索引的状态
SELECT * FROM v_slow_query_monitor;

-- 关注 INDEX_STATUS 为 'UNUSED' 或 'LOW_CARDINALITY' 的索引
```

### 6. 索引优化 checklist

- [ ] 慢查询日志已开启并定期分析
- [ ] EXPLAIN 验证关键查询走了索引
- [ ] 单表索引数量 < 10
- [ ] 已清理未使用索引
- [ ] 定期执行 ANALYZE TABLE 更新统计信息
- [ ] 大表定期 OPTIMIZE TABLE 减少碎片
- [ ] 新查询上线前已评估索引需求

---

## 相关文档

- [V9_1__Sprint9_QA_Index_Optimization.sql](../sql/V9_1__Sprint9_QA_Index_Optimization.sql) - 完整的索引创建脚本
- [DATABASE-SHARDING-GUIDE.md](./DATABASE-SHARDING-GUIDE.md) - 分库分表指南
- [CACHE-LAYERING-GUIDE.md](./CACHE-LAYERING-GUIDE.md) - 缓存分层指南
