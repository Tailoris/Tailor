#!/usr/bin/env bash
# =============================================================================
# Tailor-IS 一键回滚脚本 (P1-T03)
# -----------------------------------------------------------------------------
# 用途: 系统出现严重回归/故障时，快速回滚到指定 Git Tag 对应的可信版本，
#       并重新构建前端 + 重启容器，确保部署产物与版本一致。
#
# 对应: regression-prevention-master-plan.md 方案3.2（一键回滚机制）
#
# 用法:
#   ./scripts/rollback.sh <git-tag>            # 回滚并重建前端 + 重启容器
#   ./scripts/rollback.sh <git-tag> --no-build # 仅切版本，不重建前端
#   ./scripts/rollback.sh --list               # 列出最近的可信 tag
#
# 示例:
#   ./scripts/rollback.sh dashboard-v2.0-golden
#   ./scripts/rollback.sh fix/merchant-menu-20260803
# =============================================================================
set -euo pipefail

# ---- 颜色输出 ----
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*" >&2; }

# ---- 项目根目录 ----
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

# ---- 参数解析 ----
NO_BUILD=0
LIST_ONLY=0
TAG=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --no-build) NO_BUILD=1; shift ;;
        --list)     LIST_ONLY=1; shift ;;
        -h|--help)
            sed -n '2,20p' "$0"
            exit 0
            ;;
        *)
            if [[ -z "$TAG" ]]; then
                TAG="$1"
            else
                error "多余参数: $1"
                exit 2
            fi
            shift
            ;;
    esac
done

# ---- --list 模式: 列出最近 tag ----
if [[ $LIST_ONLY -eq 1 ]]; then
    info "最近的可信 Git Tag（按时间倒序）:"
    if git tag | grep -q .; then
        git tag --sort=-creatordate | head -20
    else
        warn "当前仓库无任何 tag，建议在每次稳定发布后打 tag:"
        echo "  git tag -a <tag-name> -m '稳定版本说明'"
    fi
    exit 0
fi

# ---- 校验 tag ----
if [[ -z "$TAG" ]]; then
    error "未指定回滚目标 tag"
    echo "用法: $0 <git-tag>          回滚到指定 tag"
    echo "      $0 --list             列出可用 tag"
    echo ""
    echo "可用 tag:"
    git tag --sort=-creatordate | head -20 || true
    exit 1
fi

if ! git rev-parse "$TAG" >/dev/null 2>&1; then
    error "Git Tag 不存在: $TAG"
    echo "可用 tag:"
    git tag --sort=-creatordate | head -20 || true
    exit 1
fi

# ---- 安全检查: 工作区是否有未提交变更 ----
if [[ -n "$(git status --porcelain)" ]]; then
    warn "检测到工作区有未提交变更，回滚前将执行 git stash 暂存:"
    git status --short
    echo ""
    read -r -p "是否继续? 输入 yes 暂存并回滚，输入 no 中止: [yes/no] " CONFIRM
    if [[ "$CONFIRM" != "yes" ]]; then
        info "已中止回滚"
        exit 0
    fi
    git stash push -u -m "rollback-stash-before-$TAG-$(date +%Y%m%d%H%M%S)"
    info "已暂存未提交变更到 git stash"
fi

CURRENT_TAG="$(git describe --tags --exact-match 2>/dev/null || echo '')"
CURRENT_BRANCH="$(git rev-parse --abbrev-ref HEAD)"
info "当前: branch=$CURRENT_BRANCH tag=${CURRENT_TAG:-无}"
info "回滚目标: $TAG"
info "目标 commit: $(git rev-parse --short "$TAG") - $(git log -1 --format='%s' "$TAG")"

# ---- 记录回滚前的 commit，便于恢复 ----
PREV_COMMIT="$(git rev-parse HEAD)"
ROLLBACK_LOG="$PROJECT_ROOT/.rollback-history"
echo "$(date '+%Y-%m-%d %H:%M:%S') | from=$PREV_COMMIT | to=$TAG($(git rev-parse "$TAG")) | op=rollback" >> "$ROLLBACK_LOG"
info "回滚记录已写入 $ROLLBACK_LOG （恢复命令: git checkout $PREV_COMMIT）"

# ---- 执行 checkout ----
info "切换到 $TAG ..."
git checkout "$TAG"
info "已切换到 $TAG"

# ---- 重建前端 + 重启容器 ----
if [[ $NO_BUILD -eq 1 ]]; then
    warn "已指定 --no-build，跳过前端重建与容器重启"
    warn "注意: 不重建前端将导致 Nginx 仍服务旧产物，浏览器需硬刷新"
    exit 0
fi

FRONTEND_DIR="$PROJECT_ROOT/tailor-is-frontend/platform-admin"
if [[ -d "$FRONTEND_DIR" ]]; then
    info "清理前端构建缓存与旧产物 ..."
    rm -rf "$FRONTEND_DIR/dist" "$FRONTEND_DIR/node_modules/.vite"
    info "重新构建前端 (vite build) ..."
    (cd "$FRONTEND_DIR" && npm run build)
    info "前端构建完成: $FRONTEND_DIR/dist"
else
    warn "前端目录不存在: $FRONTEND_DIR，跳过前端构建"
fi

# ---- 重启前端容器以加载新 dist ----
COMPOSE_FILE="$PROJECT_ROOT/deploy/docker-compose.services.yml"
FRONTEND_COMPOSE="$PROJECT_ROOT/deploy/docker-compose.frontend.yml"
RESTART_TARGET=""
if [[ -f "$FRONTEND_COMPOSE" ]]; then
    RESTART_TARGET="-f $FRONTEND_COMPOSE"
elif [[ -f "$COMPOSE_FILE" ]]; then
    RESTART_TARGET="-f $COMPOSE_FILE"
fi

if [[ -n "$RESTART_TARGET" ]] && command -v docker-compose >/dev/null 2>&1; then
    info "重启前端容器以加载新 dist ..."
    docker-compose $RESTART_TARGET restart frontend 2>/dev/null \
        || docker-compose $RESTART_TARGET restart tailor-is-frontend 2>/dev/null \
        || warn "前端容器重启失败，请手动执行: docker-compose $RESTART_TARGET restart frontend"
    info "前端容器已重启"
elif command -v docker >/dev/null 2>&1; then
    info "尝试通过 docker restart 重启前端容器 ..."
    docker restart tailor-is-frontend 2>/dev/null \
        || warn "未找到 tailor-is-frontend 容器，请手动重启"
else
    warn "未检测到 docker-compose / docker，请手动重启前端容器"
fi

# ---- 构建指纹校验 ----
FINGERPRINT_SCRIPT="$PROJECT_ROOT/scripts/verify-build-fingerprint.sh"
if [[ -x "$FINGERPRINT_SCRIPT" ]]; then
    info "执行构建指纹校验 ..."
    if "$FINGERPRINT_SCRIPT"; then
        info "构建指纹校验通过"
    else
        warn "构建指纹校验未通过，部署产物可能与目标版本不一致，请人工核查"
    fi
fi

echo ""
info "========== 回滚完成 =========="
info "目标版本: $TAG"
info "请执行硬刷新 (Ctrl+Shift+R / Cmd+Shift+R) 清除浏览器缓存后验证"
info "如需恢复到回滚前状态: git checkout $PREV_COMMIT"
echo ""
