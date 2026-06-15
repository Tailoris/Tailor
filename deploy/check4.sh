#!/bin/bash
echo "=== start-service.sh 内容 ==="
cat /opt/tailor-is/scripts/start-service.sh 2>&1
echo ""
echo "=== start-gateway.sh 内容 ==="
cat /opt/tailor-is/scripts/start-gateway.sh 2>&1
echo ""
echo "=== start-user.sh 内容 ==="
cat /opt/tailor-is/scripts/start-user.sh 2>&1
echo ""
echo "=== Nacos 服务注册列表 ==="
curl -s -X POST 'http://localhost:8848/nacos/v1/ns/catalog/services' --data-urlencode "pageNo=1" --data-urlencode "pageSize=20" 2>/dev/null | python3 -c "import sys, json; d=json.load(sys.stdin); print('服务数量:', len(d.get('serviceList',[]))); [print(' -', s) for s in d.get('serviceList',[])]" 2>/dev/null || curl -s 'http://localhost:8848/nacos/v1/ns/catalog/services?pageNo=1&pageSize=20' 2>&1
