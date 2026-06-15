#!/bin/bash
echo "=== 各服务启动日志（最后 20 行）==="
echo ""
for svc in gateway user merchant marketing copyright community supply ai; do
    log="/tmp/tailor-is-logs/${svc}.log"
    if [ -f "$log" ]; then
        echo "============================================"
        echo "=== $svc 日志（最后 20 行）==="
        echo "============================================"
        tail -20 "$log"
        echo ""
    else
        echo "  $svc 日志不存在"
    fi
done
