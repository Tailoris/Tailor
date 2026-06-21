#!/bin/bash
# ==============================================================================
# Tailor IS - CDN 缓存刷新脚本
# ==============================================================================
# 功能:
#   1. 刷新阿里云 CDN 缓存
#   2. 刷新 AWS CloudFront 缓存
#   3. 支持按目录/文件类型刷新
#   4. 支持全量刷新
#
# 使用方式:
#   chmod +x cdn-purge.sh
#   ./cdn-purge.sh --provider aliyun --path "/static/*"
#   ./cdn-purge.sh --provider cloudfront --all
#   ./cdn-purge.sh --provider aliyun --file-type js,css
# ==============================================================================

set -euo pipefail

PROVIDER=""
PURGE_PATH=""
PURGE_ALL=false
FILE_TYPE=""
DRY_RUN=false

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

# ==============================================================================
# 阿里云 CDN 刷新
# ==============================================================================
purge_aliyun() {
    local access_key_id="${ALIYUN_ACCESS_KEY_ID:-}"
    local access_key_secret="${ALIYUN_ACCESS_KEY_SECRET:-}"

    if [ -z "$access_key_id" ] || [ -z "$access_key_secret" ]; then
        log_error "请设置 ALIYUN_ACCESS_KEY_ID 和 ALIYUN_ACCESS_KEY_SECRET 环境变量"
        exit 1
    fi

    if [ "$DRY_RUN" = true ]; then
        log_info "[DRY-RUN] 将刷新阿里云 CDN: $PURGE_PATH"
        return 0
    fi

    if [ "$PURGE_ALL" = true ]; then
        log_info "刷新阿里云 CDN 全部缓存..."
        # 使用 aliyun CLI
        aliyun cdn RefreshObjectCaches \
            --ObjectType Directory \
            --ObjectPath "https://cdn.tailoris.com/" 2>/dev/null || {
            log_warn "aliyun CLI 未安装，使用 API 方式"
            # 使用阿里云 OpenAPI
            curl -s "https://cdn.aliyuncs.com/?Action=RefreshObjectCaches&ObjectType=Directory&ObjectPath=https://cdn.tailoris.com/&Format=JSON&Version=2018-05-10&AccessKeyId=${access_key_id}&SignatureMethod=HMAC-SHA1&Timestamp=$(date -u +%Y-%m-%dT%H:%M:%SZ)&SignatureVersion=1.0"
        }
    else
        log_info "刷新阿里云 CDN 路径: ${PURGE_PATH}"
        if [ -n "$FILE_TYPE" ]; then
            IFS=',' read -ra TYPES <<< "$FILE_TYPE"
            for type in "${TYPES[@]}"; do
                log_info "  刷新文件类型: *.$type"
                aliyun cdn RefreshObjectCaches \
                    --ObjectType File \
                    --ObjectPath "https://cdn.tailoris.com/**/*.${type}" 2>/dev/null || true
            done
        else
            aliyun cdn RefreshObjectCaches \
                --ObjectType Directory \
                --ObjectPath "${PURGE_PATH}" 2>/dev/null || {
                log_error "刷新失败，请检查 aliyun CLI 配置"
            }
        fi
    fi

    log_info "阿里云 CDN 刷新请求已提交"
}

# ==============================================================================
# AWS CloudFront 刷新
# ==============================================================================
purge_cloudfront() {
    local distribution_id="${CLOUDFRONT_DISTRIBUTION_ID:-}"

    if [ -z "$distribution_id" ]; then
        log_error "请设置 CLOUDFRONT_DISTRIBUTION_ID 环境变量"
        exit 1
    fi

    if [ "$DRY_RUN" = true ]; then
        log_info "[DRY-RUN] 将刷新 CloudFront: $PURGE_PATH"
        return 0
    fi

    if [ "$PURGE_ALL" = true ]; then
        log_info "刷新 CloudFront 全部缓存..."
        aws cloudfront create-invalidation \
            --distribution-id "$distribution_id" \
            --paths "/*" 2>/dev/null || {
            log_error "刷新失败，请检查 AWS CLI 配置"
        }
    else
        log_info "刷新 CloudFront 路径: ${PURGE_PATH}"
        local paths=""
        if [ -n "$FILE_TYPE" ]; then
            IFS=',' read -ra TYPES <<< "$FILE_TYPE"
            for type in "${TYPES[@]}"; do
                paths="${paths} /*.${type}"
            done
        else
            paths="${PURGE_PATH}"
        fi

        aws cloudfront create-invalidation \
            --distribution-id "$distribution_id" \
            --paths $paths 2>/dev/null || {
            log_error "刷新失败"
        }
    fi

    log_info "CloudFront 刷新请求已提交"
}

# ==============================================================================
# 通用刷新 (基于 curl 调用各 CDN API)
# ==============================================================================
purge_generic() {
    local urls=()

    if [ "$PURGE_ALL" = true ]; then
        urls+=("https://cdn.tailoris.com/*")
    elif [ -n "$FILE_TYPE" ]; then
        IFS=',' read -ra TYPES <<< "$FILE_TYPE"
        for type in "${TYPES[@]}"; do
            urls+=("https://cdn.tailoris.com/**/*.${type}")
        done
    else
        urls+=("${PURGE_PATH}")
    fi

    if [ "$DRY_RUN" = true ]; then
        for url in "${urls[@]}"; do
            log_info "[DRY-RUN] 将刷新: $url"
        done
        return 0
    fi

    for url in "${urls[@]}"; do
        log_info "刷新: $url"
        # 通用 curl 调用 (需要根据实际 CDN API 调整)
        curl -s -X POST "https://cdn-api.example.com/purge" \
            -H "Authorization: Bearer ${CDN_API_TOKEN:-}" \
            -d "{\"url\": \"$url\"}" 2>/dev/null || \
            log_warn "通用刷新暂不支持，请使用具体的 CDN 提供商"
    done
}

# ==============================================================================
# 主函数
# ==============================================================================
main() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --provider|-p)
                PROVIDER="$2"
                shift 2
                ;;
            --path|-P)
                PURGE_PATH="$2"
                shift 2
                ;;
            --all|-a)
                PURGE_ALL=true
                shift
                ;;
            --file-type|-t)
                FILE_TYPE="$2"
                shift 2
                ;;
            --dry-run|-d)
                DRY_RUN=true
                shift
                ;;
            --help|-h)
                echo "用法: $0 [选项]"
                echo ""
                echo "选项:"
                echo "  --provider, -p  CDN 提供商 (aliyun/cloudfront/generic)"
                echo "  --path, -P      刷新路径 (如 /static/*)"
                echo "  --all, -a       全量刷新"
                echo "  --file-type, -t 文件类型 (如 js,css,png)"
                echo "  --dry-run, -d   预览模式"
                echo "  --help, -h      显示帮助"
                echo ""
                echo "环境变量:"
                echo "  ALIYUN_ACCESS_KEY_ID      阿里云 AccessKey"
                echo "  ALIYUN_ACCESS_KEY_SECRET  阿里云 AccessKey Secret"
                echo "  CLOUDFRONT_DISTRIBUTION_ID CloudFront Distribution ID"
                echo ""
                echo "示例:"
                echo "  $0 -p aliyun -a                        # 刷新阿里云全部缓存"
                echo "  $0 -p cloudfront -P '/static/*'        # 刷新 CloudFront 静态资源"
                echo "  $0 -p aliyun -t js,css                 # 刷新所有 JS/CSS 文件"
                echo "  $0 -p aliyun -d -a                     # 预览模式"
                exit 0
                ;;
            *)
                log_error "未知参数: $1"
                exit 1
                ;;
        esac
    done

    if [ -z "$PROVIDER" ]; then
        log_error "请指定 CDN 提供商: --provider aliyun|cloudfront|generic"
        exit 1
    fi

    if [ "$PURGE_ALL" = false ] && [ -z "$PURGE_PATH" ] && [ -z "$FILE_TYPE" ]; then
        log_error "请指定刷新路径 (--path)、文件类型 (--file-type) 或全量刷新 (--all)"
        exit 1
    fi

    echo ""
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║           Tailor IS - CDN 缓存刷新脚本                       ║"
    echo "╠══════════════════════════════════════════════════════════════╣"
    echo "║  提供商:    $PROVIDER"
    echo "║  路径:      ${PURGE_PATH:-N/A}"
    echo "║  文件类型:  ${FILE_TYPE:-N/A}"
    echo "║  全量刷新:  $([ "$PURGE_ALL" = true ] && echo '是' || echo '否')"
    echo "║  模式:      $([ "$DRY_RUN" = true ] && echo 'DRY-RUN' || echo 'LIVE')"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo ""

    case "$PROVIDER" in
        aliyun)
            purge_aliyun
            ;;
        cloudfront)
            purge_cloudfront
            ;;
        generic)
            purge_generic
            ;;
        *)
            log_error "不支持的 CDN 提供商: $PROVIDER"
            exit 1
            ;;
    esac

    log_info "CDN 缓存刷新完成"
}

main "$@"