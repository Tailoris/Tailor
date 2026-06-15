#!/bin/bash
# ==============================================================================
# Tailor IS - 前端性能优化构建脚本
# Phase 3 P3-4: CDN + 前端性能优化 (首屏 < 2s)
# ==============================================================================
# 用途: 一键构建所有前端项目并生成性能报告
# 使用: ./tailor-is-frontend/build-all.sh [cdn_base_url]
# ==============================================================================

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
log_step()  { echo -e "${BLUE}[STEP]${NC} $1"; }

CDN_BASE="${1:-}"
FRONTEND_DIR="./tailor-is-frontend"

if [ -n "$CDN_BASE" ]; then
    export CDN_BASE_URL="$CDN_BASE"
    log_info "CDN 基础路径: $CDN_BASE_URL"
fi

echo "============================================================"
echo "  Tailor IS 前端构建 - 阶段3 (P3-4)"
echo "  目标: 首屏加载 < 2s"
echo "  时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "============================================================"

# 检查 Node.js 和 npm
if ! command -v node &> /dev/null; then
    echo "Error: Node.js 未安装"
    exit 1
fi

NODE_VERSION=$(node -v)
log_info "Node.js 版本: $NODE_VERSION"

# ============================================================================
# 1. PC Mall
# ============================================================================
log_step "1/4 构建 PC Mall..."
cd "$FRONTEND_DIR/pc-mall"
npm ci --silent 2>/dev/null || npm install --silent
npm run build
PC_MALL_SIZE=$(du -sh dist/ | cut -f1)
log_info "PC Mall 构建完成 (大小: $PC_MALL_SIZE)"

# 检查压缩产物
if ls dist/assets/*.gz 2>/dev/null | head -1 > /dev/null; then
    GZ_COUNT=$(ls dist/assets/*.gz 2>/dev/null | wc -l)
    log_info "  Gzip 压缩文件: $GZ_COUNT 个"
fi
if ls dist/assets/*.br 2>/dev/null | head -1 > /dev/null; then
    BR_COUNT=$(ls dist/assets/*.br 2>/dev/null | wc -l)
    log_info "  Brotli 压缩文件: $BR_COUNT 个"
fi

# ============================================================================
# 2. Merchant Admin
# ============================================================================
log_step "2/4 构建 Merchant Admin..."
cd "$FRONTEND_DIR/merchant-admin"
npm ci --silent 2>/dev/null || npm install --silent
npm run build
MERCHANT_SIZE=$(du -sh dist/ | cut -f1)
log_info "Merchant Admin 构建完成 (大小: $MERCHANT_SIZE)"

# ============================================================================
# 3. Platform Admin
# ============================================================================
log_step "3/4 构建 Platform Admin..."
cd "$FRONTEND_DIR/platform-admin"
npm ci --silent 2>/dev/null || npm install --silent
npm run build
PLATFORM_SIZE=$(du -sh dist/ | cut -f1)
log_info "Platform Admin 构建完成 (大小: $PLATFORM_SIZE)"

# ============================================================================
# 4. Mobile App
# ============================================================================
log_step "4/4 构建 Mobile App..."
cd "$FRONTEND_DIR/mobile-app"
npm ci --silent 2>/dev/null || npm install --silent
npm run build:h5 2>/dev/null || npm run build 2>/dev/null || log_info "Mobile App 构建跳过 (可能需要 UniApp CLI)"
MOBILE_SIZE=$(du -sh dist/ 2>/dev/null | cut -f1 || echo "N/A")
log_info "Mobile App 构建完成 (大小: $MOBILE_SIZE)"

# ============================================================================
# 5. 构建结果汇总
# ============================================================================
echo ""
echo "============================================================"
echo "  构建结果汇总"
echo "============================================================"
echo "  PC Mall:          $PC_MALL_SIZE"
echo "  Merchant Admin:   $MERCHANT_SIZE"
echo "  Platform Admin:   $PLATFORM_SIZE"
echo "  Mobile App:       $MOBILE_SIZE"
echo "============================================================"

# 性能检查清单
echo ""
log_info "性能优化检查清单:"
echo "  [ ] 检查 dist/ 目录是否有 .gz 和 .br 压缩文件"
echo "  [ ] 检查 JS chunk 是否已拆分 (vendor, element-plus, utils)"
echo "  [ ] 检查图片是否已分类到 images/ 目录"
echo "  [ ] 验证 sourcemap 已在生产构建中关闭"
echo "  [ ] 运行 Lighthouse 验证首屏加载时间 < 2s"
echo "  [ ] 验证 CDN 基础路径配置正确"
echo "  [ ] 检查 Nginx 是否配置了 gzip_static 和 brotli_static"