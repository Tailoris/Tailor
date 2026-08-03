#!/bin/bash
# ==============================================================================
# Tailor-IS 构建产物指纹校验脚本
# ==============================================================================
# 功能: 构建后将关键 chunk 的 sha256 指纹与 git 版本绑定，部署后校验 Nginx
#       返回的 JS hash 与指纹一致，不一致则阻断部署
# 对应: regression-prevention-master-plan.md 方案3.2（构建产物指纹校验）
# 用法:
#   生成指纹: ./scripts/verify-build-fingerprint.sh generate
#   校验指纹: ./scripts/verify-build-fingerprint.sh verify
# ==============================================================================
set -e

BUILD_DIR="tailor-is-frontend/platform-admin/dist"
FINGERPRINT_FILE="deploy/build-fingerprint.txt"
NGINX_BASE="http://localhost/admin"

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

# 关键 chunk 列表（新增模块时在此追加）
KEY_CHUNKS=(
    "AdminLayout"
    "DashboardView"
    "MerchantAuditView"
    "MerchantListView"
    "DepositConfigView"
    "DepositFlowView"
    "MerchantWithdrawView"
    "MerchantBillView"
    "ShopManageView"
    "ShopDepositView"
)

ACTION=${1:-generate}

if [ "$ACTION" = "generate" ]; then
    # ==================================================================
    # 生成指纹
    # ==================================================================
    if [ ! -d "$BUILD_DIR" ]; then
        echo -e "${RED}✗ 构建产物目录不存在: $BUILD_DIR${NC}"
        echo "  请先执行: cd tailor-is-frontend/platform-admin && npm run build"
        exit 1
    fi

    echo "=== 生成构建产物指纹 ==="
    {
        echo "# Tailor-IS 构建产物指纹"
        echo "# Build Time: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
        echo "# Git Commit: $(git rev-parse HEAD 2>/dev/null || echo 'unknown')"
        echo "# Git Branch: $(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')"
        echo "# Git Tag: $(git describe --tags --always 2>/dev/null || echo 'none')"
        echo ""
        echo "## 关键 chunk 指纹 (sha256)"
        for chunk in "${KEY_CHUNKS[@]}"; do
            file=$(ls "$BUILD_DIR/js/${chunk}"*.js 2>/dev/null | head -1)
            if [ -n "$file" ]; then
                hash=$(sha256sum "$file" | cut -d' ' -f1)
                echo "$chunk|$(basename $file)|$hash"
            else
                echo "$chunk|MISSING|none"
            fi
        done
    } > "$FINGERPRINT_FILE"

    echo -e "${GREEN}✓ 指纹已写入 $FINGERPRINT_FILE${NC}"
    cat "$FINGERPRINT_FILE"

elif [ "$ACTION" = "verify" ]; then
    # ==================================================================
    # 校验指纹：对比 Nginx 返回的 JS 与指纹记录
    # ==================================================================
    if [ ! -f "$FINGERPRINT_FILE" ]; then
        echo -e "${RED}✗ 指纹文件不存在: $FINGERPRINT_FILE${NC}"
        echo "  请先执行: ./scripts/verify-build-fingerprint.sh generate"
        exit 1
    fi

    echo "=== 校验 Nginx 部署产物与指纹一致性 ==="
    FAIL=0

    while IFS='|' read -r chunk filename hash; do
        # 跳过注释行
        case "$chunk" in \#*) continue ;; esac
        [ -z "$chunk" ] && continue
        [ "$hash" = "none" ] && { echo -e "${RED}✗ $chunk 指纹缺失${NC}"; FAIL=1; continue; }

        # 从 Nginx 拉取该 chunk
        url="$NGINX_BASE/js/$filename"
        nginx_hash=$(curl -s "$url" 2>/dev/null | sha256sum | cut -d' ' -f1)

        if [ -z "$nginx_hash" ]; then
            echo -e "${RED}✗ $chunk Nginx 返回空: $url${NC}"
            FAIL=1
        elif [ "$nginx_hash" = "$hash" ]; then
            echo -e "${GREEN}✓ $chunk 一致 ($filename)${NC}"
        else
            echo -e "${RED}✗ $chunk 不一致${NC}"
            echo "  指纹记录: $hash"
            echo "  Nginx实际: $nginx_hash"
            FAIL=1
        fi
    done < "$FINGERPRINT_FILE"

    echo ""
    if [ $FAIL -eq 0 ]; then
        echo -e "${GREEN}=== 指纹校验通过，部署产物与构建版本一致 ===${NC}"
        exit 0
    else
        echo -e "${RED}=== 指纹校验失败，Nginx 加载的产物与构建版本不一致 ===${NC}"
        echo "  可能原因: 浏览器缓存 / Nginx 未重启 / dist 未更新 / 增量构建残留"
        echo "  修复步骤: 1) rm -rf dist && npm run build  2) 重启 frontend 容器  3) 重新校验"
        exit 1
    fi

else
    echo "用法: $0 {generate|verify}"
    echo "  generate  构建后生成指纹"
    echo "  verify    部署后校验 Nginx 产物与指纹一致性"
    exit 1
fi
