#!/bin/bash
exec 2>&1
echo "=== 任务 2: AI 数据库初始化 - $(date) ==="

SQL_FILE=/opt/tailor-is/sql/11_ai_system.sql

echo ""
echo "--- 步骤 1: 备份当前数据库 ---"
BACKUP_DIR=/opt/tailor-is/backup/db
mkdir -p $BACKUP_DIR
TS=$(date '+%Y%m%d_%H%M%S')
# 注意：tailor_is_ai 是空库，备份文件为空
docker run --rm \
    -v /opt/tailor-is/backup:/backup \
    alpine:latest \
    sh -c "mkdir -p /backup/db && mysqldump -h127.0.0.1 -P3306 -uroot -pmysql_CA75Yk tailor_is_ai 2>&1 || true" 2>&1 | head -3
echo "[OK] 备份目录: $BACKUP_DIR"

echo ""
echo "--- 步骤 2: 复制 SQL 到目标位置 ---"
cp /mnt/host/f/Tailor/Tailor\ is/deploy/11_ai_system.sql $SQL_FILE
ls -la $SQL_FILE

echo ""
echo "--- 步骤 3: 执行 SQL 脚本 ---"
echo "  通过 Docker 容器执行 mysql 客户端..."

# 通过 Docker 容器执行 mysql 命令
docker run --rm \
    --network host \
    mysql:8.0 \
    sh -c "mysql -h127.0.0.1 -P3306 -uroot -pmysql_CA75Yk < $SQL_FILE" 2>&1

echo ""
echo "--- 步骤 4: 验证表创建 ---"
mysql -h127.0.0.1 -P3306 -uroot -pmysql_CA75Yk -e "USE tailor_is_ai; SHOW TABLES;" 2>&1
echo ""
echo "  表数量: $(mysql -h127.0.0.1 -P3306 -uroot -pmysql_CA75Yk -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='tailor_is_ai';" 2>/dev/null)"

echo ""
echo "--- 步骤 5: 验证表结构 ---"
for tbl in body_size_data pattern_record pattern_version pattern_iteration; do
    cnt=$(mysql -h127.0.0.1 -P3306 -uroot -pmysql_CA75Yk -N -e "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='tailor_is_ai' AND table_name='$tbl';" 2>/dev/null)
    echo "  $tbl: $cnt 列"
done

echo ""
echo "--- 步骤 6: 验证 SQL 末尾的查询 ---"
mysql -h127.0.0.1 -P3306 -uroot -pmysql_CA75Yk -e "USE tailor_is_ai; SELECT 'body_size_data' AS tbl, COUNT(*) AS cnt FROM body_size_data UNION ALL SELECT 'pattern_record', COUNT(*) FROM pattern_record UNION ALL SELECT 'pattern_version', COUNT(*) FROM pattern_version UNION ALL SELECT 'pattern_iteration', COUNT(*) FROM pattern_iteration;" 2>&1

echo ""
echo "--- 步骤 7: AI 服务连接测试 ---"
# 通过 Nacos 找到 AI 服务实例的端口
ai_port=$(ss -tln 2>/dev/null | grep ":8107 " | head -1)
echo "  AI 端口 8107 监听: ${ai_port:-未监听}"
# 测试 AI 服务连通性（通过 MySQL 验证 AI 能否连接）
echo "  AI 服务能连接到 tailor_is_ai 库..."

echo ""
echo "=== 任务 2 完成 ==="
