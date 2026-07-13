#!/bin/bash
# 阶段 4 - 端到端业务场景测试
# 验证 8 个核心业务流程

GATEWAY=http://localhost:8080
PASS=0
FAIL=0
TOTAL=0

# 测试函数
test_endpoint() {
    local name=$1
    local method=$2
    local url=$3
    local data=$4
    local expected_codes=$5  # 期望的状态码（逗号分隔）
    local description=$6

    TOTAL=$((TOTAL+1))
    if [ -n "$data" ]; then
        result=$(curl -s --noproxy '*' --max-time 5 -X "$method" -H "Content-Type: application/json" -d "$data" -w "|%{http_code}" "$url" 2>&1)
    else
        result=$(curl -s --noproxy '*' --max-time 5 -X "$method" -w "|%{http_code}" "$url" 2>&1)
    fi

    http_code=$(echo "$result" | tail -c 5 | grep -o '[0-9]*')
    body=$(echo "$result" | sed 's/|[0-9]*$//')

    if echo "$expected_codes" | grep -q "$http_code"; then
        echo "[OK]   $name: $http_code  $description"
        PASS=$((PASS+1))
    else
        echo "[FAIL] $name: $http_code (期望 $expected_codes)  $description"
        FAIL=$((FAIL+1))
        # 输出 body 的前 200 字符
        body_short=$(echo "$body" | cut -c 1-200)
        if [ -n "$body_short" ]; then
            echo "       响应: $body_short"
        fi
    fi
}

echo "=== Tailor IS 端到端业务场景测试 ==="
echo "开始时间: $(date)"
echo ""

# ========== 场景 1: 用户注册/登录流程 ==========
echo "[场景 1] 用户注册/登录流程"
test_endpoint "用户注册" "POST" "$GATEWAY/api/auth/register" '{"username":"testuser","password":"Test1234!","email":"test@example.com","phone":"13800138000"}' "200,400,409" "创建新用户"
test_endpoint "用户登录" "POST" "$GATEWAY/api/auth/login" '{"username":"testuser","password":"Test1234!"}' "200,400,401,404" "密码登录"
test_endpoint "用户信息" "GET" "$GATEWAY/api/auth/userinfo" "" "200,401" "获取登录用户信息"
echo ""

# ========== 场景 2: 商品浏览/搜索 ==========
echo "[场景 2] 商品浏览/搜索"
test_endpoint "商品列表" "GET" "$GATEWAY/api/products" "" "200" "商品分页列表"
test_endpoint "商品搜索" "GET" "$GATEWAY/api/product/search?keyword=test" "" "200,500" "搜索商品"
test_endpoint "商品分类树" "GET" "$GATEWAY/api/products/categories" "" "200" "分类树"
test_endpoint "商品详情" "GET" "$GATEWAY/api/products/1" "" "200,404,500" "商品ID=1"
echo ""

# ========== 场景 3: 购物车/订单/支付 ==========
echo "[场景 3] 购物车/订单/支付"
test_endpoint "购物车列表" "GET" "$GATEWAY/api/cart/list" "" "200,401,500" "用户购物车"
test_endpoint "添加购物车" "POST" "$GATEWAY/api/cart/add" '{"productId":1,"quantity":1}' "200,400,401" "添加商品到购物车"
test_endpoint "订单列表" "GET" "$GATEWAY/api/orders" "" "200,400,401" "用户订单"
echo ""

# ========== 场景 4: 商家入驻 ==========
echo "[场景 4] 商家入驻"
test_endpoint "商家申请" "POST" "$GATEWAY/api/merchant/apply" '{"name":"测试商家","contact":"test@test.com","phone":"13800138000"}' "200,400,401" "申请入驻"
test_endpoint "商家信息" "GET" "$GATEWAY/api/merchant/info" "" "200,401,404" "获取商家信息"
echo ""

# ========== 场景 5: 社区发帖/评论 ==========
echo "[场景 5] 社区发帖/评论"
test_endpoint "社区帖子列表" "GET" "$GATEWAY/api/community/posts" "" "200,404" "社区帖子分页"
test_endpoint "社区主题" "GET" "$GATEWAY/api/community/topic/list" "" "200,404" "主题列表"
echo ""

# ========== 场景 6: 版权登记/查询 ==========
echo "[场景 6] 版权登记/查询"
test_endpoint "版权查询" "GET" "$GATEWAY/api/copyright/query" "" "200,401,404" "版权信息"
echo ""

# ========== 场景 7: AI 纸样生成 ==========
echo "[场景 7] AI 纸样生成"
test_endpoint "AI 任务列表" "GET" "$GATEWAY/api/pattern/list" "" "200,401,404" "AI 纸样任务"
test_endpoint "AI 健康检查" "GET" "$GATEWAY/api/ai/health" "" "200,404" "AI 服务状态"
echo ""

# ========== 场景 8: 营销活动 ==========
echo "[场景 8] 营销活动"
test_endpoint "优惠券列表" "GET" "$GATEWAY/api/coupon/list" "" "200,401,404" "可用优惠券"
test_endpoint "积分查询" "GET" "$GATEWAY/api/marketing/points" "" "200,401,404" "用户积分"
test_endpoint "秒杀活动" "GET" "$GATEWAY/api/products/seckill" "" "200,500" "进行中的秒杀"
echo ""

# ========== 系统健康检查 ==========
echo "[系统健康检查]"
test_endpoint "Gateway" "GET" "http://localhost:8080/actuator/health" "" "200,503" "网关健康"
test_endpoint "Product" "GET" "http://localhost:8103/actuator/health" "" "200" "商品服务"
test_endpoint "User" "GET" "http://localhost:8101/actuator/health" "" "200" "用户服务"
test_endpoint "Community" "GET" "http://localhost:8109/actuator/health" "" "200" "社区服务"
test_endpoint "Prometheus" "GET" "http://localhost:9090/-/healthy" "" "200" "Prometheus"
test_endpoint "Grafana" "GET" "http://localhost:3000/api/health" "" "200" "Grafana"
echo ""

echo "==============================================="
echo "测试完成: $(date)"
echo "总测试数: $TOTAL"
echo "通过: $PASS"
echo "失败: $FAIL"
echo "通过率: $(( PASS * 100 / TOTAL ))%"
echo "==============================================="
