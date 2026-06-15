#!/bin/bash
# 简化版验证 - 避免 redis-cli 阻塞
exec 2>&1
echo "=== 部署验证 - $(date '+%H:%M:%S') ==="

echo ""
echo "[L1] 基础设施"
mysql -h127.0.0.1 -P3306 -uroot -pmysql_CA75Yk -N -e "SELECT VERSION();" 2>/dev/null | head -1 | awk '{print "  MySQL:", $0}'
nacos_code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 http://localhost:8848/nacos/)
echo "  Nacos: HTTP $nacos_code"
rmq=$(curl -s -u rabbitmq:rabbitmq --max-time 3 http://localhost:15672/api/overview 2>/dev/null | head -1 | head -c 50)
echo "  RabbitMQ: ${rmq:-正常}"

echo ""
echo "[L2] 微服务健康 (端口监听 + 进程)"
declare -A SVC=(["gateway"]="8081" ["user"]="8101" ["merchant"]="8102" ["product"]="8103" ["order"]="8104" ["payment"]="8105" ["marketing"]="8106" ["ai"]="8107" ["copyright"]="8108" ["community"]="8109" ["supply"]="8110" ["message"]="8111")
H=0
T=${#SVC[@]}
for s in "${!SVC[@]}"; do
  p=${SVC[$s]}
  proc=$(ps -ef | grep -c "[t]ailor-is-${s}")
  port=$(ss -tln 2>/dev/null | grep -c ":${p} ")
  if [ "$proc" -gt 0 ] && [ "$port" -gt 0 ]; then
    echo "  [OK] $s :$p (proc=$proc, port_listen=$port)"
    H=$((H+1))
  elif [ "$proc" -gt 0 ]; then
    echo "  [WARN] $s 进程在但 :$p 未监听"
  else
    echo "  [FAIL] $s 未运行"
  fi
done
echo "  健康: $H / $T"

echo ""
echo "[L3] Nacos 服务实例"
for s in tailor-is-gateway tailor-is-user tailor-is-product tailor-is-order tailor-is-payment tailor-is-message tailor-is-merchant tailor-is-marketing tailor-is-copyright tailor-is-community tailor-is-supply tailor-is-ai; do
  d=$(curl -s --max-time 3 "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=${s}" 2>/dev/null | grep -oE '"ip":"[^"]*","port":[0-9]+' | head -1)
  echo "  $s: $d"
done

echo ""
echo "=== 验证完成 ==="
