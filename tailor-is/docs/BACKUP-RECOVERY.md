# 备份与灾难恢复指南

本文档描述 Tailor IS（裁智云）的数据库备份、恢复和灾难恢复流程。

## 数据库备份

### 自动备份

使用 `deploy/database/backup.sh` 脚本执行定时备份：

```bash
#!/bin/bash
# 每日凌晨 2 点备份
0 2 * * * /path/to/deploy/database/backup.sh >> /var/log/db-backup.log 2>&1
```

脚本功能：
- 使用 `mysqldump` 导出所有业务数据库
- 按日期命名备份文件（`backup_YYYYMMDD_HHMMSS.sql.gz`）
- 自动压缩并保留最近 30 天的备份

### 手动备份

```bash
# 备份指定数据库
mysqldump -u root -p tailor_is_user > backup_user_$(date +%Y%m%d).sql

# 备份全部数据库
mysqldump -u root -p --all-databases > backup_all_$(date +%Y%m%d).sql

# 备份并压缩
mysqldump -u root -p tailor_is_order | gzip > backup_order_$(date +%Y%m%d).sql.gz
```

### Redis 备份

```bash
# 触发 RDB 快照
redis-cli BGSAVE

# 复制 RDB 文件
cp /var/lib/redis/dump.rdb /backup/redis/dump_$(date +%Y%m%d).rdb

# AOF 备份（如果启用）
cp /var/lib/redis/appendonly.aof /backup/redis/appendonly_$(date +%Y%m%d).aof
```

## 数据恢复

### 数据库恢复

```bash
# 恢复单个数据库
mysql -u root -p tailor_is_user < backup_user_20260611.sql

# 恢复压缩备份
gunzip < backup_order_20260611.sql.gz | mysql -u root -p tailor_is_order
```

### 恢复步骤

1. **停止应用服务** — 避免恢复期间产生新数据
2. **执行恢复** — 按上述命令恢复数据库
3. **验证数据** — 检查关键表数据完整性
4. **重启服务** — 按依赖顺序启动微服务

### 部分恢复

如果只需要恢复特定表：

```bash
# 从完整备份中提取单个表
sed -n '/^CREATE TABLE.*`users`/,/^CREATE TABLE\|^-- Table/p' backup_all.sql > users_table.sql
mysql -u root -p tailor_is_user < users_table.sql
```

## 灾难恢复计划

### 场景一：单节点故障

1. 切换到备用节点
2. 从最近备份恢复数据库
3. 更新 DNS / Nginx 指向新节点
4. 验证服务可用性

### 场景二：数据损坏

1. 确认损坏范围和时间点
2. 从损坏前的备份恢复
3. 如有 binlog，可重放到指定时间点：

```bash
# 使用 binlog 恢复到指定时间点
mysqlbinlog --stop-datetime="2026-06-11 14:00:00" /var/lib/mysql/mysql-bin.000001 | mysql -u root -p
```

### 场景三：完全灾难（机房级别）

1. 在备用机房部署基础设施
2. 从异地备份恢复数据库
3. 部署所有微服务
4. 切换流量到备用机房
5. 数据同步与一致性校验

## 备份策略建议

| 数据类型 | 备份频率 | 保留时间 | 存储位置 |
|----------|----------|----------|----------|
| MySQL 全量 | 每日 | 30 天 | 本地 + 异地 |
| MySQL binlog | 实时 | 7 天 | 本地 |
| Redis | 每日 | 7 天 | 本地 |
| 配置文件 | 变更时 | 永久 | Git 仓库 |

## 演练

建议每季度执行一次灾难恢复演练：

1. 选择非高峰时段
2. 在隔离环境执行完整恢复流程
3. 记录恢复时间（RTO）和数据丢失量（RPO）
4. 总结改进点并更新文档

## 监控告警

确保以下备份相关告警已配置：

- 备份任务失败
- 备份文件大小异常（可能为空）
- 备份磁盘空间不足（阈值 80%）
- 备份超过 24 小时未执行
