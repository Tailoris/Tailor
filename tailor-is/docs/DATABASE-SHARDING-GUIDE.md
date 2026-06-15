# 数据库分片与 TiDB 集成指南

> **版本**: 1.0.0  
> **更新日期**: 2026-06-11  
> **适用项目**: Tailor IS Platform (Spring Boot 3.3.5 + MyBatis-Plus 3.5.7)

---

## 一、架构概述

### 1.1 设计目标

随着 Tailor IS 平台业务增长，订单和支付系统面临高并发写入压力。本方案引入 **ShardingSphere-JDBC 5.5.0** + **TiDB** 实现数据库水平分片，将单表数据分散到多个分片中，提升写入性能和存储容量。

### 1.2 服务分类与数据源路由

| 服务分类 | 包含服务 | 数据源方案 | 说明 |
|---------|---------|-----------|------|
| **核心服务** | order, payment, ai, copyright | TiDB + ShardingSphere 分片 | 高并发写入、强一致性要求 |
| **非核心服务** | community, academy, supply | MySQL 主从架构 | 读多写少、可用性优先 |

### 1.3 分片规则

- **分片键**: `merchant_id`（商家ID）
- **分片算法**: Inline 表达式 `|hash(merchant_id)| % 4`
- **分片数量**: 4 个分片（可水平扩展至 8/16）
- **分布式 ID**: Snowflake 雪花算法

---

## 二、技术栈

| 组件 | 版本 | 用途 |
|-----|------|------|
| ShardingSphere-JDBC | 5.5.0 | 客户端分片中间件 |
| TiDB | 7.x (推荐) | 分布式 NewSQL 数据库 |
| Spring Boot | 3.3.5 | 应用框架 |
| MyBatis-Plus | 3.5.7 | ORM 框架 |
| Druid | 1.2.23 | 数据库连接池 |

---

## 三、配置文件说明

### 3.1 分片配置

#### Order 服务
- **路径**: `tailor-is-order/src/main/resources/application-sharding.yml`
- **分片表**: `order_info`, `order_item`, `shopping_cart`, `after_sale_ticket`
- **实际数据节点**: `ds0.t_order_${0..3}` 等

#### Payment 服务
- **路径**: `tailor-is-payment/src/main/resources/application-sharding.yml`
- **分片表**: `payment_record`, `refund_record`, `settlement_record`, `withdraw_record`, `account_transaction`
- **实际数据节点**: `ds0.t_payment_record_${0..3}` 等

### 3.2 TiDB 配置
- **路径**: `tailor-is-order/src/main/resources/application-tidb.yml`
- TiDB 完全兼容 MySQL 协议，默认端口 `4000`
- 启用批量写入优化 (`rewriteBatchedStatements=true`)
- 启用服务端预处理语句缓存

### 3.3 Java 配置类
- **路径**: `tailor-is-common/src/main/java/com/tailoris/common/config/ShardingStrategyConfig.java`
- 通过 `@ConfigurationProperties(prefix = "tailoris.sharding")` 绑定配置
- 提供 `isCoreService()`, `calculateShardIndex()`, `getDataSourceType()` 等方法

---

## 四、迁移步骤

### 4.1 前置准备

1. **部署 TiDB 集群**（参考 [TiDB 官方文档](https://docs.pingcap.com/tidb/stable)）
2. **确认 ShardingSphere-JDBC 5.5.0 兼容 Spring Boot 3.3.5**
3. **备份现有数据库**（mysqldump 或 TiDB Lightning）

### 4.2 数据库迁移

1. 执行分片建表脚本：
```bash
mysql -h <TIDB_HOST> -P 4000 -u root -p < sql/10_sharding_migration.sql
```

2. 验证分片表创建：
```sql
SELECT table_name, table_rows, data_length
FROM information_schema.tables
WHERE table_schema = 'tailor_is_order'
  AND table_name LIKE 't_order_%';
```

### 4.3 数据迁移（双写方案）

**阶段一：双写期（1-2 周）**
1. 应用同时写入原表和分片表
2. 通过 ShardingSphere-JDBC 读取分片表，与原表对比验证

**阶段二：数据同步（1-2 天）**
1. 使用专用迁移脚本将历史数据写入分片表
2. 校验数据一致性（行数、金额汇总等）

**阶段三：切换期（1 小时）**
1. 关闭原表写入，全部走分片表
2. 观察监控指标（QPS、延迟、错误率）
3. 确认无误后下线原表

### 4.4 启用分片配置

在 `application.yml` 中激活分片 profile：

```yaml
spring:
  profiles:
    active: sharding
```

或通过环境变量：

```bash
export SPRING_PROFILES_ACTIVE=sharding
```

---

## 五、TiDB 集成注意事项

### 5.1 主键设计

TiDB 不推荐使用自增主键（会导致 Region 热点问题）。应使用：

```yaml
mybatis-plus:
  global-config:
    db-config:
      id-type: assign_id  # 雪花算法
```

### 5.2 事务优化

- TiDB 使用 **乐观事务模型**，大事务可能触发重试
- 建议控制单事务大小，避免跨多行锁定
- 使用 `tidb_disable_txn_auto_retry = off` 启用自动重试

### 5.3 连接参数优化

```
rewriteBatchedStatements=true   # 批量 INSERT 性能提升 10x+
useServerPrepStmts=true         # 服务端预处理
cachePrepStmts=true             # 客户端预处理缓存
prepStmtCacheSize=250           # 缓存大小
prepStmtCacheSqlLimit=2048      # 最大 SQL 长度
```

### 5.4 TiDB vs MySQL 差异

| 特性 | MySQL | TiDB | 适配建议 |
|-----|-------|------|---------|
| 事务模型 | 悲观 | 乐观 | 减少事务冲突 |
| 自增主键 | 支持 | 支持但会热点 | 使用分布式 ID |
| 外键 | 支持 | 不支持 | 应用层关联 |
| 存储过程 | 支持 | 不支持 | 应用层实现 |
| 全局二级索引 | 需额外处理 | 原生支持 | 直接使用 |

---

## 六、监控与运维

### 6.1 ShardingSphere 监控

```yaml
spring:
  shardingsphere:
    props:
      sql-show: true              # 开发环境打印实际 SQL
      check-table-metadata-enabled: true  # 元数据校验
```

### 6.2 TiDB 监控

- 访问 TiDB Dashboard: `http://<TIDB_HOST>:2379/dashboard`
- 关键指标：QPS、延迟、Region 分布、存储空间

### 6.3 告警规则

| 指标 | 阈值 | 动作 |
|-----|------|------|
| 分片不均匀度 | > 20% | 检查分片算法 |
| TiDB 写入延迟 | > 100ms | 检查 Region 热点 |
| 连接池使用率 | > 80% | 扩容连接池 |

---

## 七、扩展与升级

### 7.1 分片扩容（4 → 8 分片）

1. 修改配置中的分片表达式：`% 8`
2. 新增 `t_*_4` ~ `t_*_7` 表
3. 使用 ShardingSphere 数据迁移工具重新分布数据

### 7.2 TiDB 集群扩容

```bash
# 添加 TiKV 节点
tiup cluster scale-out <cluster-name> scale-out.yaml

# 添加 TiDB 节点
tiup cluster scale-out <cluster-name> tidb-scale.yaml
```

---

## 八、回滚方案

如遇问题需回滚：

1. 切换 `spring.profiles.active` 回原有配置
2. 执行回滚脚本（`10_sharding_migration.sql` 底部注释部分）
3. 恢复原表数据
4. 通知相关服务降级

---

## 九、相关文件清单

| 文件 | 说明 |
|-----|------|
| `pom.xml` | 父 POM，添加 ShardingSphere-JDBC 5.5.0 依赖管理 |
| `tailor-is-order/pom.xml` | Order 服务依赖 |
| `tailor-is-payment/pom.xml` | Payment 服务依赖 |
| `tailor-is-order/src/main/resources/application-sharding.yml` | Order 分片配置 |
| `tailor-is-payment/src/main/resources/application-sharding.yml` | Payment 分片配置 |
| `tailor-is-order/src/main/resources/application-tidb.yml` | TiDB 连接配置模板 |
| `tailor-is-common/.../ShardingStrategyConfig.java` | 分片策略配置类 |
| `sql/10_sharding_migration.sql` | 分片建表迁移脚本 |

---

## 十、参考文档

- [ShardingSphere 官方文档](https://shardingsphere.apache.org/document/current/cn/overview/)
- [TiDB 官方文档](https://docs.pingcap.com/zh/tidb/stable)
- [TiDB 与 MySQL 兼容性说明](https://docs.pingcap.com/zh/tidb/stable/mysql-compatibility)
- [Spring Boot 3.3.5 文档](https://docs.spring.io/spring-boot/docs/3.3.5/reference/html/)
- [MyBatis-Plus 文档](https://baomidou.com/)
