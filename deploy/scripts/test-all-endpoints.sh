#!/usr/bin/env bash
set -eo pipefail

RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
GREEN='\033[0;32m'
NC='\033[0m'

BASE_URL="http://localhost"

SUCCESS_COUNT=0
FAIL_COUNT=0
TEST_CASES=0

log_pass() { echo -e "${GREEN}✅ PASS${NC}: $1"; SUCCESS_COUNT=$((SUCCESS_COUNT + 1)); }
log_fail() { echo -e "${RED}❌ FAIL${NC}: $1"; FAIL_COUNT=$((FAIL_COUNT + 1)); }
log_skip() { echo -e "${YELLOW}⏭️ SKIP${NC}: $1"; }
log_section() { echo ""; echo -e "${BLUE}===== $1 =====${NC}"; }
log_test() { echo -e "   [$((++TEST_CASES))] $1"; }

TEST_RESULTS=()
add_result() { TEST_RESULTS+=("$1"); }

test_http() {
  local url="$1"
  local method="$2"
  local expected_code="$3"
  local description="$4"
  local extra_args="$5"
  
  log_test "$description"
  
  local status_code
  if [[ -n "$extra_args" ]]; then
    status_code=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" "$extra_args" "$url" 2>/dev/null || echo "000")
  else
    status_code=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" "$url" 2>/dev/null || echo "000")
  fi
  
  status_code=$(echo "$status_code" | tr -d "'")
  
  if [[ "$status_code" == "$expected_code" ]]; then
    log_pass "$url [$method] -> $status_code"
    add_result "PASS|$url|$method|$status_code|$description"
  else
    log_fail "$url [$method] -> $status_code (expected $expected_code)"
    add_result "FAIL|$url|$method|$status_code|$description"
  fi
}

test_response() {
  local url="$1"
  local method="$2"
  local expected_content="$3"
  local description="$4"
  local extra_args="$5"
  
  log_test "$description"
  
  local response
  if [[ -n "$extra_args" ]]; then
    response=$(curl -s -X "$method" "$extra_args" "$url" 2>/dev/null)
  else
    response=$(curl -s -X "$method" "$url" 2>/dev/null)
  fi
  
  if echo "$response" | grep -qi "$expected_content"; then
    log_pass "$url [$method] contains '$expected_content'"
    add_result "PASS|$url|$method|200|$description"
  else
    log_fail "$url [$method] does not contain '$expected_content'"
    echo "   Response: ${response:0:200}"
    add_result "FAIL|$url|$method|200|$description"
  fi
}

log_section "Tailor IS v4.0 全链路网络地址测试"
echo "测试时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "基础URL: $BASE_URL"
echo ""

# 一、健康检查端点
log_section "一、健康检查端点"
test_http "$BASE_URL/healthz" "GET" "200" "Nginx健康检查端点"
test_http "$BASE_URL/health" "GET" "200" "备用健康检查端点"

# 二、前端页面URL
log_section "二、前端页面URL"
test_http "$BASE_URL/" "GET" "200" "PC商城首页"
test_response "$BASE_URL/" "GET" "html" "PC商城首页返回HTML"

test_http "$BASE_URL/merchant/" "GET" "200" "商户后台首页"
test_response "$BASE_URL/merchant/" "GET" "html" "商户后台返回HTML"

test_http "$BASE_URL/admin/" "GET" "200" "平台管理后台首页"
test_response "$BASE_URL/admin/" "GET" "html" "平台管理返回HTML"

# 三、Core Gateway API端点 - 用户认证
log_section "三、Core Gateway API端点 - 用户认证"
test_http "$BASE_URL/api/auth/login" "POST" "200" "用户登录接口(POST)" "-H 'Content-Type: application/json' -d '{\"username\":\"test\",\"password\":\"test\"}'"
test_http "$BASE_URL/api/v1/auth/login" "POST" "200" "用户登录接口v1(POST)" "-H 'Content-Type: application/json' -d '{\"username\":\"test\",\"password\":\"test\"}'"
test_http "$BASE_URL/api/user/info" "GET" "401" "未登录获取用户信息(应返回401)"
test_http "$BASE_URL/api/v1/user/info" "GET" "401" "未登录获取用户信息v1(应返回401)"
test_http "$BASE_URL/api/user/address" "GET" "401" "未登录获取地址(应返回401)"
test_http "$BASE_URL/api/user/roles" "GET" "401" "未登录获取角色(应返回401)"

# 四、Core Gateway API端点 - 商品服务
log_section "四、Core Gateway API端点 - 商品服务"
test_http "$BASE_URL/api/product/list" "GET" "200" "商品列表(公共接口)"
test_http "$BASE_URL/api/v1/product/list" "GET" "200" "商品列表v1(公共接口)"
test_http "$BASE_URL/api/products" "GET" "200" "商品列表旧接口(兼容)"
test_http "$BASE_URL/api/product/search" "GET" "200" "商品搜索"
test_http "$BASE_URL/api/product/category" "GET" "200" "商品分类"
test_http "$BASE_URL/api/product/sku" "GET" "200" "商品SKU"
test_http "$BASE_URL/api/favorite/list" "GET" "401" "收藏列表(需登录)"
test_http "$BASE_URL/api/v1/favorite/list" "GET" "401" "收藏列表v1(需登录)"

# 五、Core Gateway API端点 - 订单服务
log_section "五、Core Gateway API端点 - 订单服务"
test_http "$BASE_URL/api/order/list" "GET" "401" "订单列表(需登录)"
test_http "$BASE_URL/api/v1/order/list" "GET" "401" "订单列表v1(需登录)"
test_http "$BASE_URL/api/cart/list" "GET" "401" "购物车列表(需登录)"
test_http "$BASE_URL/api/v1/cart/list" "GET" "401" "购物车列表v1(需登录)"
test_http "$BASE_URL/api/order/after-sale" "GET" "401" "售后列表(需登录)"

# 六、Core Gateway API端点 - 支付服务
log_section "六、Core Gateway API端点 - 支付服务"
test_http "$BASE_URL/api/v1/payment/options" "GET" "200" "支付选项(公共接口)"
test_http "$BASE_URL/api/v1/payment/create" "POST" "401" "创建支付(需登录)" "-H 'Content-Type: application/json' -d '{}'"
test_http "$BASE_URL/api/settlement/list" "GET" "401" "结算列表(需登录)"
test_http "$BASE_URL/api/account/info" "GET" "401" "账户信息(需登录)"
test_http "$BASE_URL/api/sandbox/pay" "POST" "200" "沙箱支付测试" "-H 'Content-Type: application/json' -d '{}'"

# 七、Core Gateway API端点 - 营销服务
log_section "七、Core Gateway API端点 - 营销服务"
test_http "$BASE_URL/api/marketing/coupon/list" "GET" "200" "优惠券列表(公共接口)"
test_http "$BASE_URL/api/v1/marketing/coupon/list" "GET" "200" "优惠券列表v1(公共接口)"
test_http "$BASE_URL/api/coupon/list" "GET" "200" "优惠券列表(兼容)"
test_http "$BASE_URL/api/points/balance" "GET" "401" "积分余额(需登录)"
test_http "$BASE_URL/api/seckill/list" "GET" "200" "秒杀活动列表(公共接口)"

# 八、Core Gateway API端点 - AI服务
log_section "八、Core Gateway API端点 - AI服务"
test_http "$BASE_URL/api/v1/ai/size/analyze" "POST" "401" "体型分析(需登录)" "-H 'Content-Type: application/json' -d '{}'"
test_http "$BASE_URL/api/v1/ai/pattern/optimize" "POST" "401" "纸样优化(需登录)" "-H 'Content-Type: application/json' -d '{}'"
test_http "$BASE_URL/api/body-size/template" "GET" "200" "体型模板(公共接口)"

# 九、Core Gateway API端点 - 版权服务
log_section "九、Core Gateway API端点 - 版权服务"
test_http "$BASE_URL/api/v1/copyright/list" "GET" "401" "版权列表(需登录)"
test_http "$BASE_URL/api/v1/copyright/infringement/check" "POST" "401" "侵权检测(需登录)" "-H 'Content-Type: application/json' -d '{}'"

# 十、Core Gateway API端点 - 商家服务
log_section "十、Core Gateway API端点 - 商家服务"
test_http "$BASE_URL/api/merchant/list" "GET" "401" "商家列表(需登录)"
test_http "$BASE_URL/api/v1/merchant/list" "GET" "401" "商家列表v1(需登录)"
test_http "$BASE_URL/api/merchant/shop" "GET" "401" "店铺信息(需登录)"
test_http "$BASE_URL/api/v1/merchant/dashboard" "GET" "401" "商家仪表盘(需登录)"

# 十一、Core Gateway API端点 - 平台管理
log_section "十一、Core Gateway API端点 - 平台管理"
test_http "$BASE_URL/api/admin/dashboard" "GET" "401" "管理后台仪表盘(需管理员)"
test_http "$BASE_URL/api/v1/admin/dashboard" "GET" "401" "管理后台仪表盘v1(需管理员)"
test_http "$BASE_URL/api/admin/users" "GET" "401" "用户管理(需管理员)"
test_http "$BASE_URL/api/admin/roles" "GET" "401" "角色管理(需管理员)"
test_http "$BASE_URL/api/admin/audit-logs" "GET" "401" "审计日志(需管理员)"

# 十二、Core Gateway API端点 - 纸样服务
log_section "十二、Core Gateway API端点 - 纸样服务"
test_http "$BASE_URL/api/v1/pattern/list" "GET" "401" "纸样列表(需登录)"
test_http "$BASE_URL/api/v1/pattern/detail" "GET" "401" "纸样详情(需登录)"

# 十三、Lite Gateway API端点 - 社区服务
log_section "十三、Lite Gateway API端点 - 社区服务"
test_http "$BASE_URL/api/community/post/list" "GET" "200" "社区帖子列表(公共接口)"
test_http "$BASE_URL/api/v1/community/post/list" "GET" "200" "社区帖子列表v1(公共接口)"
test_http "$BASE_URL/api/community/post/create" "POST" "401" "创建帖子(需登录)" "-H 'Content-Type: application/json' -d '{}'"
test_http "$BASE_URL/api/community/interaction" "GET" "200" "社区互动(公共接口)"

# 十四、Lite Gateway API端点 - 学院服务
log_section "十四、Lite Gateway API端点 - 学院服务"
test_http "$BASE_URL/api/academy/course/list" "GET" "200" "课程列表(公共接口)"
test_http "$BASE_URL/api/v1/academy/course/list" "GET" "200" "课程列表v1(公共接口)"

# 十五、Lite Gateway API端点 - 供应链服务
log_section "十五、Lite Gateway API端点 - 供应链服务"
test_http "$BASE_URL/api/supply/list" "GET" "200" "供应链列表(公共接口)"
test_http "$BASE_URL/api/v1/supply/list" "GET" "200" "供应链列表v1(公共接口)"
test_http "$BASE_URL/api/supply/match" "GET" "200" "供需匹配(公共接口)"

# 十六、Lite Gateway API端点 - 消息服务
log_section "十六、Lite Gateway API端点 - 消息服务"
test_http "$BASE_URL/api/message/list" "GET" "401" "消息列表(需登录)"
test_http "$BASE_URL/api/v1/message/list" "GET" "401" "消息列表v1(需登录)"
test_http "$BASE_URL/api/notice/list" "GET" "200" "公告列表(公共接口)"

# 十七、Lite Gateway API端点 - IM服务
log_section "十七、Lite Gateway API端点 - IM服务"
test_http "$BASE_URL/api/v1/im/history" "GET" "401" "IM聊天记录(需登录)"
test_http "$BASE_URL/api/im-message/send" "POST" "401" "发送消息(需登录)" "-H 'Content-Type: application/json' -d '{}'"

# 十八、Lite Gateway API端点 - 数据分析
log_section "十八、Lite Gateway API端点 - 数据分析"
test_http "$BASE_URL/api/v1/analytics/overview" "GET" "401" "数据分析概览(需登录)"
test_http "$BASE_URL/api/v1/metrics/sales" "GET" "401" "销售指标(需登录)"

# 十九、文件上传接口测试
log_section "十九、文件上传接口测试"
test_http "$BASE_URL/api/upload/image" "POST" "401" "图片上传(需登录)"

# 二十、安全端点访问控制测试
log_section "二十、安全端点访问控制测试"
test_http "$BASE_URL/actuator/health" "GET" "403" "Actuator端点(应被拦截)"
test_http "$BASE_URL/actuator/info" "GET" "403" "Actuator info(应被拦截)"
test_http "$BASE_URL/nginx_status" "GET" "403" "Nginx状态(仅限内网)"

# 二十一、v4.0 规范兼容性测试
log_section "二十一、v4.0 规范兼容性测试"
test_http "$BASE_URL/api/user/list" "GET" "401" "旧格式用户接口(兼容)"
test_http "$BASE_URL/api/product/detail" "GET" "200" "旧格式商品接口(兼容)"
test_http "$BASE_URL/api/order/detail" "GET" "401" "旧格式订单接口(兼容)"

# 测试总结报告
log_section "测试总结报告"

echo ""
echo "┌─────────────────────────────────────────────────────────────────────┐"
echo "│                  Tailor IS v4.0 全链路测试报告                      │"
echo "├─────────────────────────────────────────────────────────────────────┤"
echo "│  测试时间:    $(date '+%Y-%m-%d %H:%M:%S')"
echo "│  测试总数:    $TEST_CASES"
echo "│  ✅ 通过:     ${GREEN}$SUCCESS_COUNT${NC}"
echo "│  ❌ 失败:     ${RED}$FAIL_COUNT${NC}"
echo "│  通过率:      $((SUCCESS_COUNT * 100 / TEST_CASES))%"
echo "└─────────────────────────────────────────────────────────────────────┘"
echo ""

if [[ "$FAIL_COUNT" -gt 0 ]]; then
  echo "失败案例详情:"
  echo "─────────────"
  for result in "${TEST_RESULTS[@]}"; do
    if echo "$result" | grep -q "^FAIL|"; then
      IFS='|' read -r status url method code desc <<< "$result"
      echo -e "${RED}❌${NC} $url [$method] -> $code: $desc"
    fi
  done
  echo ""
  echo "建议排查方向:"
  echo "  1. 检查网关服务是否正常启动"
  echo "  2. 检查Nginx配置是否正确"
  echo "  3. 检查服务注册到Nacos是否成功"
  echo "  4. 检查数据库连接是否正常"
else
  echo -e "${GREEN}🎉 所有测试通过！${NC}"
fi

REPORT_FILE="test-report-$(date +%Y%m%d-%H%M%S).txt"
echo "生成测试报告文件: $REPORT_FILE"

{
  echo "========================================"
  echo "Tailor IS v4.0 全链路测试报告"
  echo "测试时间: $(date '+%Y-%m-%d %H:%M:%S')"
  echo "基础URL: $BASE_URL"
  echo "========================================"
  echo ""
  echo "测试统计:"
  echo "  测试总数: $TEST_CASES"
  echo "  通过: $SUCCESS_COUNT"
  echo "  失败: $FAIL_COUNT"
  echo "  通过率: $((SUCCESS_COUNT * 100 / TEST_CASES))%"
  echo ""
  echo "测试详情:"
  echo "─────────"
  for result in "${TEST_RESULTS[@]}"; do
    echo "$result"
  done
} > "$REPORT_FILE"

exit $((FAIL_COUNT > 0 ? 1 : 0))