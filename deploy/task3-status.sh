#!/bin/bash
exec 2>&1
echo "=== 检查服务存活与 Nacos 集成 ==="

echo "--- 服务进程存活 ---"
ps -ef | grep -E "tailor-is-(user|merchant|product|order|payment|message|marketing|community|copyright|supply|ai|gateway)" | grep -v grep | awk '{print $2, $NF}' | sort

echo ""
echo "--- 各服务端口监听 ---"
for p in 8081 8101 8102 8103 8104 8105 8106 8107 8108 8109 8110 8111; do
    cnt=$(ss -tln 2>/dev/null | grep -c ":${p} ")
    echo "  port ${p}: ${cnt}"
done

echo ""
echo "--- Nacos 服务注册查询 ---"
# 在 Nacos 容器内查询
docker exec 1Panel-nacos-gJky-standalone \
    curl -s 'http://localhost:8848/nacos/v1/ns/catalog/services?pageNo=1&pageSize=20' 2>&1 | head -3

echo ""
echo "--- 各服务日志最后 3 行（看 Nacos 连接状态）---"
for svc in user merchant product order payment message marketing community copyright supply ai gateway; do
    if [ -f /opt/tailor-is/logs/${svc}.log ]; then
        last=$(tail -3 /opt/tailor-is/logs/${svc}.log 2>&1 | grep -oE 'nacos|register|registry|discon' | head -2 | xargs)
        echo "  $svc: ${last:-无 nacos 日志}"
    fi
done
