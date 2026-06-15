#!/bin/bash
exec 2>&1
echo "=== 任务 2: AI 数据库初始化 v2 - $(date) ==="

# 1. 通过 Docker 将 SQL 复制到 /opt/tailor-is/sql/
echo "--- 步骤 1: 复制 SQL 到目标位置 ---"
docker run --rm \
    -v /mnt/host/f/Tailor/Tailor\ is/deploy:/src:ro \
    -v /opt/tailor-is/sql:/dest \
    alpine:latest \
    cp /src/11_ai_system.sql /dest/11_ai_system.sql 2>&1
ls -la /opt/tailor-is/sql/11_ai_system.sql

# 2. 通过 Docker 执行 mysql 客户端
echo ""
echo "--- 步骤 2: 执行 SQL ---"
docker run --rm \
    --network host \
    -v /opt/tailor-is/sql:/sql:ro \
    mysql:8.0 \
    sh -c "mysql -h127.0.0.1 -P3306 -uroot -pmysql_CA75Yk < /sql/11_ai_system.sql" 2>&1

# 3. 验证
echo ""
echo "--- 步骤 3: 验证表 ---"
mysql -h127.0.0.1 -P3306 -uroot -pmysql_CA75Yk -e "USE tailor_is_ai; SHOW TABLES;" 2>&1
echo ""
echo "表数量: $(mysql -h127.0.0.1 -P3306 -uroot -pmysql_CA75Yk -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='tailor_is_ai';" 2>/dev/null)"

echo ""
echo "--- 步骤 4: 验证各表列数 ---"
for tbl in body_size_data pattern_record pattern_version pattern_iteration; do
    cnt=$(mysql -h127.0.0.1 -P3306 -uroot -pmysql_CA75Yk -N -e "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='tailor_is_ai' AND table_name='$tbl';" 2>/dev/null)
    echo "  $tbl: $cnt 列"
done

echo ""
echo "--- 步骤 5: 验证索引 ---"
mysql -h127.0.0.1 -P3306 -uroot -pmysql_CA75Yk -e "
SELECT TABLE_NAME, INDEX_NAME, COLUMN_NAME, INDEX_TYPE 
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA='tailor_is_ai' 
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;" 2>&1

echo ""
echo "--- 步骤 6: 验证字符集 ---"
mysql -h127.0.0.1 -P3306 -uroot -pmysql_CA75Yk -e "
SELECT TABLE_NAME, TABLE_COLLATION 
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA='tailor_is_ai';" 2>&1

echo ""
echo "=== 任务 2 完成 ==="
