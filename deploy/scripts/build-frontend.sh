#!/usr/bin/env bash
# =============================================================================
# Tailor IS 前端构建脚本
# 对应: Phase 1 / P1-6 / H-04
# 用法: ./build-frontend.sh [project]
#   project 可选: pc-mall | merchant-admin | platform-admin | mobile-app | all
# =============================================================================
set -euo pipefail

PROJECT=${1:-all}
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)/../tailor-is-frontend"
# 使用用户级日志目录 (避免 sudo)
BUILD_LOG="${HOME}/.local/log/tailor-is-frontend-build.log"
mkdir -p "$(dirname "$BUILD_LOG")"

log() { echo "[$(date +'%H:%M:%S')] $*" | tee -a "$BUILD_LOG"; }

build_project() {
    local proj=$1
    log "==> 构建项目: $proj"
    cd "$ROOT_DIR/$proj"
    if [ ! -f package.json ]; then
        log "  [WARN] $proj 不存在 package.json, 跳过"
        return 0
    fi
    if [ ! -d node_modules ]; then
        log "  [INFO] 安装依赖..."
        npm ci --no-audit --no-fund 2>&1 | tee -a "$BUILD_LOG" | tail -5
    fi
    log "  [INFO] 编译..."
    NODE_ENV=production npm run build 2>&1 | tee -a "$BUILD_LOG" | tail -20

    if [ -d dist ]; then
        local size=$(du -sh dist | cut -f1)
        log "  [OK] $proj 构建成功, 产物 $size, 位置: $ROOT_DIR/$proj/dist"
    else
        log "  [ERROR] $proj 构建失败, dist 不存在"
        return 1
    fi
}

case "$PROJECT" in
    all)
        for p in pc-mall merchant-admin platform-admin; do
            build_project "$p" || exit 1
        done
        ;;
    pc-mall|merchant-admin|platform-admin|mobile-app)
        build_project "$PROJECT"
        ;;
    *)
        echo "用法: $0 [pc-mall|merchant-admin|platform-admin|mobile-app|all]"
        exit 1
        ;;
esac

log "==> 全部构建任务完成"
echo ""
echo "下一步:"
echo "  1. docker compose -f deploy/docker-compose.frontend.yml up -d"
echo "  2. 访问 http://localhost:8080/"
echo "  3. 验证 API: curl http://localhost:8080/api/core/actuator/health"
