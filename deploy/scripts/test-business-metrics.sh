#!/bin/bash
# ==============================================================================
#  Tailor IS - 业务指标端到端测试脚本
# ==============================================================================
#  功能：
#    1) 用 Spring Boot Maven 插件在后台启动 tailor-is-user (端口 18080)
#    2) 用 curl 依次调用登录、失败登录、密码重置、模拟攻击等接口
#    3) 调用 /actuator/prometheus 拉取指标并验证关键字段
#    4) 输出测试报告（PASS / FAIL + 指标快照）
#
#  使用：
#    cd deploy/scripts
#    bash test-business-metrics.sh          # 自动构建+启动+测试
#  或：
#    SKIP_BUILD=1 bash test-business-metrics.sh   # 跳过 mvn 构建（已构建过）
# ==============================================================================

set -u

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
USER_DIR="$PROJECT_ROOT/tailor-is-user"
PORT=18080
BASE="http://127.0.0.1:$PORT"
LOG_FILE="$PROJECT_ROOT/tailor-is-user-test.log"
PID_FILE="$PROJECT_ROOT/tailor-is-user-test.pid"

PASS=0
FAIL=0

echo "================================================================"
echo "  Tailor IS - 业务指标端到端测试"
echo "  项目根目录: $PROJECT_ROOT"
echo "  服务地址:   $BASE"
echo "  日志文件:   $LOG_FILE"
echo "================================================================"

# -------- 1) 构建并启动服务 --------
echo ""
echo "[1/5] 构建并启动 tailor-is-user ..."

if [ -z "${SKIP_BUILD:-}" ]; then
  cd "$USER_DIR"
  if command -v mvn >/dev/null 2>&1; then
    echo "  → mvn clean package -DskipTests"
    if mvn clean package -DskipTests -q 2>&1 | tail -5; then
      echo "  ✅ 构建成功"
    else
      echo "  ❌ Maven 构建失败，请检查"
      exit 1
    fi
  else
    echo "  ❌ 未检测到 mvn 命令。请先安装 Maven 或在有 JDK 的环境执行"
    exit 1
  fi
else
  echo "  ⏭  SKIP_BUILD 已设置，跳过构建"
fi

# 停止旧进程
if [ -f "$PID_FILE" ]; then
  OLD_PID=$(cat "$PID_FILE")
  if kill -0 "$OLD_PID" 2>/dev/null; then
    echo "  → 停止旧进程 PID=$OLD_PID"
    kill "$OLD_PID" 2>/dev/null
    sleep 2
  fi
  rm -f "$PID_FILE"
fi

# 启动服务（后台）
JAR=$(find "$USER_DIR/target" -maxdepth 1 -name "*.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar" 2>/dev/null | head -n1)
if [ -z "$JAR" ]; then
  echo "  ❌ 未找到构建产物 jar，请先运行 mvn package"
  exit 1
fi

echo "  → 启动 JAR: $JAR"
nohup java -jar "$JAR" --server.port="$PORT" > "$LOG_FILE" 2>&1 &
echo $! > "$PID_FILE"

# 等待健康检查通过
echo -n "  等待服务启动..."
for i in $(seq 1 40); do
  if curl -sS "$BASE/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
    echo " ✅"
    break
  fi
  sleep 1
  echo -n "."
done
echo ""

if ! curl -sS "$BASE/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
  echo "  ❌ 服务未在 40s 内启动，查看日志:"
  tail -40 "$LOG_FILE"
  exit 1
fi

# -------- 2) 登录成功测试（管理员 + 普通用户） --------
echo ""
echo "[2/5] 登录成功测试 ..."

admin_login_before=$(curl -sS "$BASE/api/metrics/snapshot" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d['data']['metrics']['admin_login_count'])")
user_login_before=$(curl -sS "$BASE/api/metrics/snapshot" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d['data']['metrics']['total_login_attempts'])")

echo "  → 管理员登录 (admin / admin123)"
resp=$(curl -sS -X POST "$BASE/api/auth/login" -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')
echo "     $resp"
if echo "$resp" | grep -q '"code":200'; then PASS=$((PASS+1)); else FAIL=$((FAIL+1)); echo "     ❌ 管理员登录失败"; fi

echo "  → 普通用户登录 (demo-user / user123)"
resp=$(curl -sS -X POST "$BASE/api/auth/login" -H "Content-Type: application/json" \
  -d '{"username":"demo-user","password":"user123"}')
echo "     $resp"
if echo "$resp" | grep -q '"code":200'; then PASS=$((PASS+1)); else FAIL=$((FAIL+1)); echo "     ❌ 普通用户登录失败"; fi

echo "  → 商户登录 (test-merchant / merchant123)"
resp=$(curl -sS -X POST "$BASE/api/auth/login" -H "Content-Type: application/json" \
  -d '{"username":"test-merchant","password":"merchant123"}')
echo "     $resp"
if echo "$resp" | grep -q '"code":200'; then PASS=$((PASS+1)); else FAIL=$((FAIL+1)); echo "     ❌ 商户登录失败"; fi

# -------- 3) 登录失败测试 + 模拟攻击 --------
echo ""
echo "[3/5] 登录失败 + 模拟撞库攻击 ..."

echo "  → 5 次密码错误（测试 failed_login_attempts 累计）"
for i in 1 2 3 4 5; do
  resp=$(curl -sS -X POST "$BASE/api/auth/login" -H "Content-Type: application/json" \
    -d "{\"username\":\"admin\",\"password\":\"wrong-pass-$i\"}")
done
echo "  → 模拟撞库：POST /api/metrics/simulate?count=30"
resp=$(curl -sS -X POST "$BASE/api/metrics/simulate?count=30")
echo "     $resp"
if echo "$resp" | grep -q '"code":200'; then PASS=$((PASS+1)); else FAIL=$((FAIL+1)); echo "     ❌ 模拟失败"; fi

# -------- 4) 密码重置测试 --------
echo ""
echo "[4/5] 密码重置请求测试 ..."

echo "  → 请求重置验证码（admin）"
resp=$(curl -sS -X POST "$BASE/api/auth/password/reset-request" \
  -H "Content-Type: application/json" -d '{"username":"admin"}')
echo "     $resp"
if echo "$resp" | grep -q '"code":200'; then PASS=$((PASS+1)); else FAIL=$((FAIL+1)); echo "     ❌ 密码重置请求失败"; fi

echo "  → 再请求 2 次（用于测试 password_reset_requests 累计）"
curl -sS -X POST "$BASE/api/auth/password/reset-request" \
  -H "Content-Type: application/json" -d '{"username":"demo-user"}' >/dev/null
curl -sS -X POST "$BASE/api/auth/password/reset-request" \
  -H "Content-Type: application/json" -d '{"username":"test-merchant"}' >/dev/null

# -------- 5) 拉取指标 + 断言 --------
echo ""
echo "[5/5] 拉取 Prometheus 指标并断言 ..."

METRICS=$(curl -sS "$BASE/actuator/prometheus")

echo ""
echo "  --- Prometheus 关键字段快照 ---"
echo "$METRICS" | grep -E "^(admin_login_count_total|failed_login_attempts_total|total_login_attempts_total|password_reset_requests_total|business_active_users|abnormal_login_rate)" | head -20

# 断言：admin_login_count >= 1
admin_val=$(echo "$METRICS" | grep -E "^admin_login_count_total" | head -n1 | awk '{print $NF}')
echo ""
echo "  admin_login_count=$admin_val (期望 >=1)"
if [ -n "$admin_val" ] && [ "$(printf "%.0f" "$admin_val" 2>/dev/null)" -ge 1 ]; then
  PASS=$((PASS+1)); echo "  ✅ PASS"
else FAIL=$((FAIL+1)); echo "  ❌ FAIL (实际值: $admin_val)"; fi

# 断言：failed_login_attempts > 10
failed_val=$(echo "$METRICS" | grep -E "^failed_login_attempts_total.*role=\"all\"" | head -n1 | awk '{print $NF}')
if [ -z "$failed_val" ]; then failed_val=$(echo "$METRICS" | grep -E "^failed_login_attempts_total" | head -n1 | awk '{print $NF}'); fi
echo "  failed_login_attempts(role=all)=$failed_val (期望 >=10)"
if [ -n "$failed_val" ] && [ "$(printf "%.0f" "$failed_val" 2>/dev/null)" -ge 10 ]; then
  PASS=$((PASS+1)); echo "  ✅ PASS"
else FAIL=$((FAIL+1)); echo "  ❌ FAIL (实际值: $failed_val)"; fi

# 断言：password_reset_requests >= 3
pr_val=$(echo "$METRICS" | grep -E "^password_reset_requests_total" | head -n1 | awk '{print $NF}')
echo "  password_reset_requests=$pr_val (期望 >=3)"
if [ -n "$pr_val" ] && [ "$(printf "%.0f" "$pr_val" 2>/dev/null)" -ge 3 ]; then
  PASS=$((PASS+1)); echo "  ✅ PASS"
else FAIL=$((FAIL+1)); echo "  ❌ FAIL (实际值: $pr_val)"; fi

# 断言：business_active_users >= 0（有 token 就会有值）
au_val=$(echo "$METRICS" | grep -E "^business_active_users" | head -n1 | awk '{print $NF}')
echo "  business_active_users=$au_val"
if [ -n "$au_val" ] && [ "$(printf "%.0f" "$au_val" 2>/dev/null)" -ge 0 ]; then
  PASS=$((PASS+1)); echo "  ✅ PASS（指标已暴露，数值将随活跃 token 刷新）"
else FAIL=$((FAIL+1)); echo "  ❌ FAIL (实际值: $au_val)"; fi

# 断言：abnormal_login_rate > 0
abn_val=$(echo "$METRICS" | grep -E "^abnormal_login_rate" | head -n1 | awk '{print $NF}')
echo "  abnormal_login_rate=$abn_val (期望 > 0)"
if [ -n "$abn_val" ] && [ "$(printf "%.0f" "$abn_val" 2>/dev/null)" -gt 0 ]; then
  PASS=$((PASS+1)); echo "  ✅ PASS"
else FAIL=$((FAIL+1)); echo "  ❌ FAIL (实际值: $abn_val)"; fi

# -------- 汇总 --------
echo ""
echo "================================================================"
echo "  测试汇总"
echo "================================================================"
echo "  PASS : $PASS"
echo "  FAIL : $FAIL"
echo ""
if [ "$FAIL" -eq 0 ]; then
  echo "  ✅ 所有断言通过！业务指标接入正常。"
else
  echo "  ❌ 存在失败断言，请检查日志：$LOG_FILE"
fi
echo "================================================================"

# 停止测试进程
PID=$(cat "$PID_FILE" 2>/dev/null)
if [ -n "$PID" ]; then
  echo "  → 停止测试进程 PID=$PID"
  kill "$PID" 2>/dev/null
  sleep 1
fi
echo ""
echo "  提示："
echo "    · /actuator/prometheus 暴露的指标可直接被 Prometheus 采集"
echo "    · 相关告警规则见 deploy/alerts.yml（admin_login_count / failed_login_attempts 等）"
echo "    · alert-webhook 可将告警发送到飞书/钉钉/邮件（deploy/alert-webhook）"
echo ""

exit 0
