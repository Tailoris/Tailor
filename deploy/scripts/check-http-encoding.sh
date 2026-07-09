#!/bin/bash
set -e

API_HOST="${API_HOST:-localhost}"
API_PORT="${API_PORT:-8080}"
BASE_URL="http://${API_HOST}:${API_PORT}"

echo "=============================================="
echo " HTTP 响应头编码验证脚本"
echo "=============================================="
echo "目标地址: $BASE_URL"
echo ""

check_encoding() {
    local endpoint=$1
    local description=$2
    
    echo "[检查] $description"
    echo "       $endpoint"
    
    RESPONSE=$(curl -sI "$BASE_URL$endpoint" 2>/dev/null)
    
    if [ -z "$RESPONSE" ]; then
        echo "       ✗ 无法访问"
        return 1
    fi
    
    CONTENT_TYPE=$(echo "$RESPONSE" | grep -i "Content-Type" | head -1)
    SERVER=$(echo "$RESPONSE" | grep -i "^Server:" | head -1 | awk '{print $2}')
    STATUS=$(echo "$RESPONSE" | head -1 | awk '{print $2}')
    
    echo "       状态码: $STATUS"
    echo "       Server: $SERVER"
    echo "       Content-Type: $CONTENT_TYPE"
    
    if echo "$CONTENT_TYPE" | grep -qi "charset=utf-8"; then
        echo "       ✓ 响应头包含 charset=UTF-8"
        return 0
    elif echo "$CONTENT_TYPE" | grep -qi "charset="; then
        echo "       ✗ 响应头charset不是UTF-8: $CONTENT_TYPE"
        return 1
    else
        echo "       ⚠️  响应头缺少charset参数"
        return 1
    fi
}

echo "[1] 基础健康检查接口"
echo "-------------------"
check_encoding "/actuator/health" "健康检查"

echo ""
echo "[2] API接口编码检查"
echo "-------------------"
check_encoding "/api/user/info" "用户信息API"
check_encoding "/api/product/list" "商品列表API"
check_encoding "/api/order/list" "订单列表API"

echo ""
echo "[3] 静态资源编码检查"
echo "-------------------"
check_encoding "/favicon.ico" "favicon"

echo ""
echo "[4] 字符编码测试"
echo "---------------"
echo "正在测试中文响应..."
TEST_RESPONSE=$(curl -s "$BASE_URL/api/test/encoding" 2>/dev/null || echo "")
if [ -n "$TEST_RESPONSE" ]; then
    if echo "$TEST_RESPONSE" | grep -q "中文"; then
        echo "✓ 中文响应正常"
    else
        echo "✗ 中文响应可能存在乱码"
        echo "  响应内容: $TEST_RESPONSE"
    fi
else
    echo "⚠️  测试接口不可用"
fi

echo ""
echo "[5] 请求编码测试"
echo "---------------"
TEST_POST=$(curl -s -X POST "$BASE_URL/api/test/encoding" \
    -H "Content-Type: application/json;charset=UTF-8" \
    -H "Accept: application/json;charset=UTF-8" \
    -d '{"test": "中文测试"}' 2>/dev/null || echo "")

if [ -n "$TEST_POST" ]; then
    echo "✓ POST请求发送成功"
else
    echo "⚠️  POST请求测试失败"
fi

echo ""
echo "=============================================="
echo " 检查完成"
echo "=============================================="
echo ""
echo "建议的Nginx配置（确保响应头正确）："
echo "http {"
echo "    charset utf-8;"
echo "    ..."
echo "}"
echo ""
echo "server {"
echo "    ..."
echo "    add_header Content-Type \"application/json;charset=UTF-8\" always;"
echo "    charset utf-8;"
echo "}"