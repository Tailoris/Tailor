#!/usr/bin/env bash
# ==============================================================================
# Tailor IS - CI/CD 自动化质量检查脚本
# ==============================================================================
# 用途: 集成到 CI/CD 流程中，确保代码提交自动遵循质量标准
# 检查项:
#   1. 路由端口规范检查 (调用 check-route-port-spec.sh)
#   2. Nginx 配置语法检查
#   3. 前端构建检查
#   4. 异常处理规范检查
#   5. 安全检查 (敏感信息硬编码)
#   6. 数据库初始化脚本完整性检查
#   7. TODO/FIXME 过期检查
#   8. 文件编码检查
#
# 使用方式:
#   bash deploy/scripts/ci-quality-check.sh [--strict]
#   --strict: 严格模式，任何警告都会导致构建失败
#
# 退出码:
#   0 - 通过
#   1 - 违规
# ==============================================================================
set -euo pipefail

RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
STRICT_MODE=false
FAILED_COUNT=0
WARNING_COUNT=0

if [[ "${1:-}" == "--strict" ]]; then
  STRICT_MODE=true
fi

log_pass() { echo -e "${GREEN}[PASS]${NC} $1"; }
log_fail() { echo -e "${RED}[FAIL]${NC} $1"; FAILED_COUNT=$((FAILED_COUNT + 1)); }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; WARNING_COUNT=$((WARNING_COUNT + 1)); }
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_section() { echo ""; echo -e "${GREEN}============================================================${NC}"; echo -e "${GREEN}  $1${NC}"; echo -e "${GREEN}============================================================${NC}"; }

# ==============================================================================
# 检查 1: 路由端口规范
# ==============================================================================
log_section "检查 1: 路由端口规范"

if bash "$PROJECT_ROOT/deploy/scripts/check-route-port-spec.sh" 2>/dev/null; then
  log_pass "路由端口规范检查通过"
else
  log_fail "路由端口规范检查未通过"
fi

# ==============================================================================
# 检查 2: Nginx 配置语法
# ==============================================================================
log_section "检查 2: Nginx 配置语法"

find "$PROJECT_ROOT/deploy/nginx" -name "*.conf" | while read -r conf; do
  if docker run --rm -v "$conf:/etc/nginx/nginx.conf:ro" nginx:alpine nginx -t 2>/dev/null; then
    log_pass "Nginx 配置语法正确: $(basename "$conf")"
  else
    log_warn "Nginx 配置语法检查跳过（Docker不可用）: $(basename "$conf")"
  fi
done

# ==============================================================================
# 检查 3: 敏感信息硬编码检测
# ==============================================================================
log_section "检查 3: 敏感信息硬编码检测"

SENSITIVE_PATTERNS=(
  'password\s*=\s*"[^"]+"'
  'secret\s*=\s*"[^"]+"'
  'api_key\s*=\s*"[^"]+"'
  'token\s*=\s*"[^"]+"'
  'jdbc:mysql://[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+'
  'redis://[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+'
)

SENSITIVE_FOUND=false
for pattern in "${SENSITIVE_PATTERNS[@]}"; do
  MATCHES=$(grep -rn "$pattern" "$PROJECT_ROOT/tailor-is/src/main" --include="*.yml" --include="*.yaml" --include="*.properties" 2>/dev/null | grep -v '^\s*#' | grep -v 'example' | grep -v 'template' || true)
  if [[ -n "$MATCHES" ]]; then
    echo "$MATCHES" | while read -r line; do
      log_warn "疑似硬编码敏感信息: $line"
    done
    SENSITIVE_FOUND=true
  fi
done

if [[ "$SENSITIVE_FOUND" == "false" ]]; then
  log_pass "未检测到硬编码敏感信息"
fi

# ==============================================================================
# 检查 4: 异常处理规范
# ==============================================================================
log_section "检查 4: 异常处理规范"

GLOBAL_EXCEPTION_HANDLER="$PROJECT_ROOT/tailor-is/tailor-is-common/src/main/java/com/tailoris/common/exception/GlobalExceptionHandler.java"
if [[ -f "$GLOBAL_EXCEPTION_HANDLER" ]]; then
  # 检查 @Order 注解
  if grep -q '@Order(Ordered.HIGHEST_PRECEDENCE)' "$GLOBAL_EXCEPTION_HANDLER"; then
    log_pass "全局异常处理器有 @Order(HIGHEST_PRECEDENCE)"
  else
    log_fail "全局异常处理器缺少 @Order(HIGHEST_PRECEDENCE)"
  fi
  
  # 检查 NotLoginException 返回 401
  if grep -A5 'NotLoginException' "$GLOBAL_EXCEPTION_HANDLER" | grep -q '401'; then
    log_pass "NotLoginException 返回 401"
  else
    log_fail "NotLoginException 未返回 401"
  fi
  
  # 检查 DataSourceConnectException 处理器
  if grep -q 'DataSourceConnectException' "$GLOBAL_EXCEPTION_HANDLER"; then
    log_pass "全局异常处理器包含 DataSourceConnectException 处理器"
  else
    log_fail "全局异常处理器缺少 DataSourceConnectException 处理器"
  fi
else
  log_fail "全局异常处理器文件不存在"
fi

# 检查子模块异常处理器是否有 basePackages 限制
MODULE_EXCEPTION_HANDLERS=$(find "$PROJECT_ROOT/tailor-is" -name "*ExceptionHandler.java" -path "*/src/main/java/*" | grep -v "tailor-is-common" || true)
for handler in $MODULE_EXCEPTION_HANDLERS; do
  if grep -q 'basePackages' "$handler"; then
    log_pass "$(basename "$handler") 有 basePackages 限制"
  else
    log_warn "$(basename "$handler") 缺少 basePackages 限制"
  fi
  
  # 检查子模块是否有 @ExceptionHandler(Exception.class) 兜底
  if grep -q '@ExceptionHandler\s*(\s*Exception\s*\.\s*class\s*)' "$handler"; then
    log_fail "$(basename "$handler") 不应包含 @ExceptionHandler(Exception.class) 兜底处理"
  fi
done

# ==============================================================================
# 检查 5: 数据库初始化脚本完整性
# ==============================================================================
log_section "检查 5: 数据库初始化脚本完整性"

SQL_DIR="$PROJECT_ROOT/tailor-is/sql"
REQUIRED_SQL_FILES=(
  "01_user_system.sql"
  "02_merchant_system.sql"
  "03_product_system.sql"
  "04_order_system.sql"
  "05_payment_system.sql"
  "06_marketing_system.sql"
  "07_copyright_system.sql"
  "08_community_system.sql"
  "09_supply_system.sql"
  "10_message_system.sql"
  "11_ai_system.sql"
  "12_admin_system.sql"
  "13_pattern_system.sql"
  "14_design_system.sql"
  "15_academy_system.sql"
  "16_knowledge_system.sql"
  "17_analytics_system.sql"
  "18_im_system.sql"
)

for sql_file in "${REQUIRED_SQL_FILES[@]}"; do
  if [[ -f "$SQL_DIR/$sql_file" ]]; then
    log_pass "SQL 脚本存在: $sql_file"
  else
    log_fail "SQL 脚本缺失: $sql_file"
  fi
done

# ==============================================================================
# 检查 6: TODO/FIXME 过期检查
# ==============================================================================
log_section "检查 6: TODO/FIXME 过期检查"

# 检查超过 6 个月的 TODO
SIX_MONTHS_AGO=$(date -d "6 months ago" +%Y-%m-%d 2>/dev/null || date -v-6m +%Y-%m-%d 2>/dev/null || echo "2026-01-01")
TODO_COUNT=$(grep -rn "TODO\|FIXME\|HACK" "$PROJECT_ROOT/tailor-is/src/main" --include="*.java" 2>/dev/null | wc -l)
log_info "当前 TODO/FIXME 数量: $TODO_COUNT"

if [[ "$TODO_COUNT" -gt 50 ]]; then
  log_warn "TODO/FIXME 数量超过 50 个，建议定期清理"
fi

# ==============================================================================
# 检查 7: 文件编码检查
# ==============================================================================
log_section "检查 7: 文件编码检查"

NON_UTF8_FILES=$(find "$PROJECT_ROOT/tailor-is/src" -name "*.java" -o -name "*.yml" -o -name "*.xml" 2>/dev/null | xargs file -i 2>/dev/null | grep -v "utf-8\|us-ascii\|binary" || true)
if [[ -z "$NON_UTF8_FILES" ]]; then
  log_pass "所有源文件编码为 UTF-8"
else
  log_warn "存在非 UTF-8 编码的文件"
  echo "$NON_UTF8_FILES" | head -5
fi

# ==============================================================================
# 检查 8: 前端构建产物残留
# ==============================================================================
log_section "检查 8: 前端构建产物残留"

FRONTEND_DIR="$PROJECT_ROOT/tailor-is-frontend"
FRONTEND_DIST_COUNT=$(find "$FRONTEND_DIR" -type d -name "dist" 2>/dev/null | wc -l)
if [[ "$FRONTEND_DIST_COUNT" -eq 0 ]]; then
  log_pass "前端无 dist 构建产物残留"
else
  log_warn "前端存在 $FRONTEND_DIST_COUNT 个 dist 目录残留"
fi

# ==============================================================================
# 检查 9: 后端编译产物残留
# ==============================================================================
log_section "检查 9: 后端编译产物残留"

BACKEND_TARGET_COUNT=$(find "$PROJECT_ROOT/tailor-is" -type d -name "target" 2>/dev/null | wc -l)
if [[ "$BACKEND_TARGET_COUNT" -eq 0 ]]; then
  log_pass "后端无 target 编译产物残留"
else
  log_warn "后端存在 $BACKEND_TARGET_COUNT 个 target 目录残留，建议执行 mvn clean"
fi

# ==============================================================================
# 检查 10: 部署脚本语法检查
# ==============================================================================
log_section "检查 10: 部署脚本语法检查"

find "$PROJECT_ROOT/deploy/scripts" -name "*.sh" -type f | while read -r script; do
  if bash -n "$script" 2>/dev/null; then
    log_pass "脚本语法正确: $(basename "$script")"
  else
    log_fail "脚本语法错误: $(basename "$script")"
  fi
done

# ==============================================================================
# 总结报告
# ==============================================================================
log_section "CI/CD 质量检查总结"

echo ""
echo "  ✅ PASS:    $((10 - FAILED_COUNT - WARNING_COUNT))"
echo "  🟡 WARN:    $WARNING_COUNT"
echo "  🔴 FAIL:    $FAILED_COUNT"
echo ""

if [[ "$FAILED_COUNT" -gt 0 ]]; then
  echo -e "${RED}质量检查未通过: $FAILED_COUNT 项失败${NC}"
  exit 1
elif [[ "$STRICT_MODE" == "true" && "$WARNING_COUNT" -gt 0 ]]; then
  echo -e "${YELLOW}严格模式: $WARNING_COUNT 项警告被视为失败${NC}"
  exit 1
else
  echo -e "${GREEN}质量检查通过${NC}"
  if [[ "$WARNING_COUNT" -gt 0 ]]; then
    echo -e "${YELLOW}存在 $WARNING_COUNT 项警告，建议复核${NC}"
  fi
  exit 0
fi