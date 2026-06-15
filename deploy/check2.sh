#!/bin/bash
echo "=== 部署目录 ==="
ls -la /opt/tailor-is/ 2>&1
echo ""
echo "=== JAR 包 ==="
find /opt/tailor-is -name '*.jar' 2>/dev/null
echo ""
echo "=== 日志目录 ==="
ls -la /opt/tailor-is/logs/ 2>/dev/null | head -20
echo ""
echo "=== Gateway 8081 健康 ==="
curl -s --max-time 5 http://localhost:8081/actuator/health 2>&1 | head -3
echo ""
echo "=== 8103-8111 端口测试 ==="
for p in 8103 8104 8105 8111; do
    echo "--- Port $p ---"
    curl -s --max-time 3 "http://localhost:${p}/actuator/health" 2>&1 | head -3
done
echo ""
echo "=== 网关访问外部测试（80端口）==="
curl -s --max-time 5 -o /dev/null -w "HTTP %{http_code}\n" http://localhost:80/ 2>&1
