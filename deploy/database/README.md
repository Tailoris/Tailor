# Tailor IS 数据库备份与恢复指南

## 目录

1. [概述](#概述)
2. [快速使用](#快速使用)
3. [备份脚本](#备份脚本)
4. [恢复脚本](#恢复脚本)
5. [定时备份配置](#定时备份配置)
6. [Docker 环境使用](#docker-环境使用)
7. [备份策略建议](#备份策略建议)
8. [常见问题](#常见问题)

---

## 概述

Tailor IS 数据库备份方案包含两个核心脚本：

| 脚本 | 功能 | 路径 |
|------|------|------|
| `backup.sh` | 备份所有/指定 MySQL 数据库为 `.sql.gz` 压缩文件 | `deploy/database/backup.sh` |
| `restore.sh` | 从备份文件恢复数据库 | `deploy/database/restore.sh` |

### 支持的数据库

- `tailor_is_user` - 用户数据
- `tailor_is_merchant` - 商户数据
- `tailor_is_product` - 商品数据
- `tailor_is_order` - 订单数据
- `tailor_is_payment` - 支付数据
- `tailor_is_marketing` - 营销数据
- `tailor_is_ai` - AI 服务数据
- `tailor_is_copyright` - 版权数据
- `tailor_is_community` - 社区数据
- `tailor_is_academy` - 学院数据
- `tailor_is_supply` - 供应链数据
- `tailor_is_message` - 消息数据

---

## 快速使用

### 备份

```bash
# 备份所有数据库
./deploy/database/backup.sh

# 仅备份指定数据库
./deploy/database/backup.sh tailor_is_order

# 自定义备份目录
BACKUP_DIR=/tmp/backups ./deploy/database/backup.sh
```

### 恢复

```bash
# 从指定文件恢复
./deploy/database/restore.sh /opt/tailor-is/backups/tailor_is_order_20260611_020000.sql.gz

# 自动查找最新备份恢复
./deploy/database/restore.sh tailor_is_order

# 恢复所有数据库 (从最新备份)
./deploy/database/restore.sh --all

# 查看最近备份
./deploy/database/restore.sh
```

---

## 备份脚本 (backup.sh)

### 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `BACKUP_DIR` | `/opt/tailor-is/backups` | 备份文件存储目录 |
| `MYSQL_HOST` | `localhost` | MySQL 主机地址 |
| `MYSQL_PORT` | `3306` | MySQL 端口 |
| `MYSQL_USERNAME` | `root` | MySQL 用户名 |
| `MYSQL_PASSWORD` | 空 | MySQL 密码 |
| `RETENTION_DAYS` | `7` | 备份保留天数 |

### 备份内容

每个备份包含：
- ✅ 完整表结构和数据
- ✅ 存储过程 (routines)
- ✅ 触发器 (triggers)
- ✅ 事件调度器 (events)
- ✅ 使用 `--single-transaction` 确保一致性 (InnoDB)
- ✅ gzip 压缩

### 输出文件

```
/opt/tailor-is/backups/
├── tailor_is_user_20260611_020000.sql.gz
├── tailor_is_order_20260611_020000.sql.gz
├── tailor_is_payment_20260611_020000.sql.gz
├── ...
└── backup_20260611_020000.catalog    # 备份清单
```

---

## 恢复脚本 (restore.sh)

### 安全机制

1. **恢复前自动备份**: 恢复前会自动备份当前数据，文件名为 `{db}_pre_restore_{timestamp}.sql.gz`
2. **交互式确认**: 终端环境下会要求确认
3. **文件检查**: 自动验证备份文件是否存在

### 环境变量

与备份脚本相同。

---

## 定时备份配置

### Crontab 配置

```bash
# 编辑 crontab
crontab -e

# 每天凌晨 2 点备份
0 2 * * * /opt/tailor-is/deploy/database/backup.sh >> /var/log/tailor-is-backup.log 2>&1

# 每小时备份 (仅核心库)
0 * * * * MYSQL_PASSWORD=your_password BACKUP_DIR=/opt/tailor-is/backups/hourly /opt/tailor-is/deploy/database/backup.sh tailor_is_order tailor_is_payment tailor_is_user >> /var/log/tailor-is-hourly-backup.log 2>&1
```

### Systemd Timer (推荐)

创建 `/etc/systemd/system/tailor-is-backup.service`:

```ini
[Unit]
Description=Tailor IS Database Backup
After=network.target

[Service]
Type=oneshot
ExecStart=/opt/tailor-is/deploy/database/backup.sh
Environment=MYSQL_PASSWORD=your_password
Environment=BACKUP_DIR=/opt/tailor-is/backups
```

创建 `/etc/systemd/system/tailor-is-backup.timer`:

```ini
[Unit]
Description=Run Tailor IS Backup Daily

[Timer]
OnCalendar=daily
Persistent=true

[Install]
WantedBy=timers.target
```

启用定时器：

```bash
sudo systemctl daemon-reload
sudo systemctl enable tailor-is-backup.timer
sudo systemctl start tailor-is-backup.timer
```

---

## Docker 环境使用

### Docker Compose 环境

```bash
# 在 Docker 容器内执行备份
docker exec tailor-is-mysql bash -c '
  mysqldump -uroot -p"${MYSQL_ROOT_PASSWORD}" \
    --single-transaction --routines --triggers --events \
    tailor_is_order | gzip > /tmp/tailor_is_order_backup.sql.gz
'

# 复制到宿主机
docker cp tailor-is-mysql:/tmp/tailor_is_order_backup.sql.gz ./

# 恢复
docker cp tailor_is_order_20260611.sql.gz tailor-is-mysql:/tmp/
docker exec tailor-is-mysql bash -c '
  gunzip -c /tmp/tailor_is_order_20260611.sql.gz | mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" tailor_is_order
'
```

### 使用备份脚本 + Docker

```bash
# 设置环境变量指向 Docker MySQL
MYSQL_HOST=172.18.0.2 MYSQL_PASSWORD=your_password ./deploy/database/backup.sh
```

---

## 备份策略建议

### 三级备份策略

| 级别 | 频率 | 保留 | 内容 |
|------|------|------|------|
| 热备份 | 每小时 | 24小时 | 核心库 (user, order, payment) |
| 日备份 | 每天 | 7天 | 全部数据库 |
| 周备份 | 每周 | 30天 | 全部数据库 + 异地存储 |

### 数据量估算

| 数据库 | 预估大小 | 压缩后 |
|--------|----------|--------|
| user | 500 MB | 50 MB |
| order | 2 GB | 200 MB |
| product | 1 GB | 100 MB |
| 其他 | 100 MB | 10 MB |
| **总计** | **~5 GB** | **~500 MB** |

### 异地备份

```bash
# 使用 rclone 同步到对象存储
rclone sync /opt/tailor-is/backups remote:tailor-is-backups \
  --include "*.catalog" \
  --include "*_$(date +%Y%m%d)*"

# 使用 rsync 同步到远程服务器
rsync -avz /opt/tailor-is/backups/ backup-server:/opt/tailor-is/backups/
```

---

## 常见问题

### Q: 备份文件太大怎么办？

A: 使用 `--compress` 选项或只备份需要的表：
```bash
mysqldump -u root -p tailor_is_order order_item order_payment | gzip > partial_backup.sql.gz
```

### Q: 恢复时遇到权限错误？

A: 确保 MySQL 用户有足够权限：
```sql
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, LOCK TABLES, TRIGGER, EVENT, ROUTINE ON *.* TO 'root'@'%';
FLUSH PRIVILEGES;
```

### Q: 备份一致性如何保证？

A: 脚本使用 `--single-transaction` 参数，在 InnoDB 引擎下提供一致性快照，不影响正常业务。

### Q: 如何验证备份完整性？

A: 定期执行恢复测试：
```bash
# 在测试环境恢复备份
docker run --name test-mysql -e MYSQL_ROOT_PASSWORD=test -d mysql:8.0
sleep 10
./deploy/database/restore.sh /path/to/backup.sql.gz
# 验证数据...
docker stop test-mysql && docker rm test-mysql
```
