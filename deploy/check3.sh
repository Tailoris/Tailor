#!/bin/bash
echo "=== Gateway 日志最后 30 行 ==="
tail -30 /opt/tailor-is/logs/gateway.log 2>&1
echo ""
echo "=== Order 日志最后 30 行 ==="
tail -30 /opt/tailor-is/logs/tailor-is-order.log 2>&1
echo ""
echo "=== Product 日志最后 20 行 ==="
tail -20 /opt/tailor-is/logs/tailor-is-product.log 2>&1
echo ""
echo "=== scripts 目录 ==="
ls -la /opt/tailor-is/scripts/ 2>&1
echo ""
echo "=== SQL 目录 ==="
ls -la /opt/tailor-is/sql/ 2>&1 | head -20
