#!/bin/bash
exec 2>&1
echo "=== 深入检查 AI 服务数据库配置 ==="

echo ""
echo "--- AI 服务进程 ---"
ps -ef | grep "tailor-is-ai" | grep -v grep

echo ""
echo "--- AI 服务启动参数中的 DB ---"
ps -ef | grep "tailor-is-ai" | grep -v grep | grep -oE "jdbc:mysql://[^\"]+" | head -3

echo ""
echo "--- AI 日志中数据库相关信息 ---"
grep -E "DruidDataSource|init|started|Hibernate|create table|table|schema|jdbc" /opt/tailor-is/logs/ai.log 2>&1 | head -30

echo ""
echo "--- AI 服务注册的 DB 实际查询 ---"
mysql -h127.0.0.1 -P3306 -uroot -pmysql_CA75Yk -N -e "SELECT table_schema, COUNT(*) as table_count FROM information_schema.tables WHERE table_schema LIKE 'tailor_is%' GROUP BY table_schema ORDER BY table_schema;" 2>/dev/null

echo ""
echo "--- AI JAR 包内是否有 schema/资源 ---"
cd /opt/tailor-is/jars
unzip -l tailor-is-ai-1.0.0.jar 2>&1 | grep -iE "schema|sql|init|flyway|liquibase" | head -20
echo ""
unzip -l tailor-is-ai-1.0.0.jar 2>&1 | grep -E "application\.|application-" | head -10
echo ""
echo "--- 配置文件列表 ---"
unzip -l tailor-is-ai-1.0.0.jar 2>&1 | grep -E "BOOT-INF/classes" | head -20

echo ""
echo "--- 提取 application 配置 ---"
mkdir -p /tmp/ai-config
cd /tmp/ai-config
unzip -o /opt/tailor-is/jars/tailor-is-ai-1.0.0.jar 'BOOT-INF/classes/application*' 2>&1 | tail -5
ls -la BOOT-INF/classes/ 2>&1 | head -10

echo ""
echo "--- application.yml 内容 ---"
cat BOOT-INF/classes/application.yml 2>/dev/null | head -50
echo "---"
cat BOOT-INF/classes/application.properties 2>/dev/null | head -30
echo "---"
cat BOOT-INF/classes/application-dev.yml 2>/dev/null | head -30
