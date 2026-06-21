#!/bin/bash
# ==============================================================================
# Tailor IS - CDN 缓存预热脚本
# ==============================================================================
# 功能:
#   1. 预热 CDN 缓存 (预加载静态资源到 CDN 边缘节点)
#   2. 支持从 sitemap 读取 URL 列表
#   3. 支持从文件读取 URL 列表
#   4. 支持并发请求加速预热
#
# 使用方式:
#   chmod +x cdn-warmup.sh
#   ./cdn-warmup.sh --provider aliyun
#   ./cdn-warmup.sh --provider cloudfront --url-file urls.txt
#   ./cdn-warmup.sh --provider aliyun --concurrency 10
# ==============================================================================

set -euo pipefail

PROVIDER=""
URL_FILE=""
CONCURRENCY=5
DRY_RUN=false
ORIGIN_URL="${ORIGIN_URL:-https://www.tailoris.com}"
CDN_URL="${CDN_URL:-https://cdn.tailoris.com}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

# ==============================================================================
# 默认预热 URL 列表
# ==============================================================================
DEFAULT_URLS=(
    # 首页
    "/"
    "/pc-mall/"
    "/pc-mall/index.html"
    "/merchant-admin/"
    "/merchant-admin/index.html"
    "/platform-admin/"
    "/platform-admin/index.html"
    # 静态资源 (CSS)
    "/static/css/app.css"
    "/static/css/vendor.css"
    "/pc-mall/css/style.css"
    # 静态资源 (JS)
    "/static/js/app.js"
    "/static/js/vendor.js"
    "/pc-mall/js/main.js"
    # 图片
    "/static/img/logo.png"
    "/static/img/favicon.ico"
    "/static/fonts/iconfont.woff2"
    # 关键页面
    "/pc-mall/product/list"
    "/pc-mall/community"
)

# ==============================================================================
# 阿里云 CDN 预热
# ==============================================================================
warmup_aliyun() {
    local access_key_id="${ALIYUN_ACCESS_KEY_ID:-}"
    local access_key_secret="${ALIYUN_ACCESS_KEY_SECRET:-}"

    if [ -z "$access_key_id" ] || [ -z "$access_key_secret" ]; then
        log_error "请设置 ALIYUN_ACCESS_KEY_ID 和 ALIYUN_ACCESS_KEY_SECRET 环境变量"
        exit 1
    fi

    local urls=()
    if [ -n "$URL_FILE" ] && [ -f "$URL_FILE" ]; then
        mapfile -t urls < "$URL_FILE"
    else
        urls=("${DEFAULT_URLS[@]}")
    fi

    log_info "开始预热阿里云 CDN (共 ${#urls[@]} 个 URL, 并发: $CONCURRENCY)"

    if [ "$DRY_RUN" = true ]; then
        for url in "${urls[@]}"; do
            log_info "[DRY-RUN] 将预热: ${CDN_URL}${url}"
        done
        return 0
    fi

    # 分批预热
    local batch_size=50
    local total=${#urls[@]}
    local batches=$(( (total + batch_size - 1) / batch_size ))

    for ((i=0; i<batches; i++)); do
        local start=$((i * batch_size))
        local end=$((start + batch_size))
        if [ $end -gt $total ]; then
            end=$total
        fi

        local batch_urls=""
        for ((j=start; j<end; j++)); do
            batch_urls="${batch_urls}${CDN_URL}${urls[$j]}\n"
        done
        batch_urls=$(echo -e "$batch_urls" | tr '\n' ',' | sed 's/,$//')

        log_info "预热批次 $((i+1))/$batches ($start-$((end-1)))"
        aliyun cdn PushObjectCache \
            --ObjectPath "$batch_urls" \
            --Area overseas 2>/dev/null || {
            log_warn "aliyun CLI 预热失败，尝试 API 方式"
            # 使用阿里云 OpenAPI
            curl -s "https://cdn.aliyuncs.com/?Action=PushObjectCache&ObjectPath=${batch_urls}&Format=JSON&Version=2018-05-10&AccessKeyId=${access_key_id}&SignatureMethod=HMAC-SHA1&Timestamp=$(date -u +%Y-%m-%dT%H:%M:%SZ)&SignatureVersion=1.0" > /dev/null 2>&1
        }
    done

    log_info "阿里云 CDN 预热请求已提交"
}

# ==============================================================================
# AWS CloudFront 预热 (通过请求源站触发)
# ==============================================================================
warmup_cloudfront() {
    local urls=()
    if [ -n "$URL_FILE" ] && [ -f "$URL_FILE" ]; then
        mapfile -t urls < "$URL_FILE"
    else
        urls=("${DEFAULT_URLS[@]}")
    fi

    log_info "开始预热 CloudFront (共 ${#urls[@]} 个 URL, 并发: $CONCURRENCY)"

    if [ "$DRY_RUN" = true ]; then
        for url in "${urls[@]}"; do
            log_info "[DRY-RUN] 将预热: ${CDN_URL}${url}"
        done
        return 0
    fi

    # 通过请求 CDN URL 触发缓存
    local count=0
    local total=${#urls[@]}

    for url in "${urls[@]}"; do
        (
            curl -s -o /dev/null -w "  %{http_code} %{time_total}s ${CDN_URL}${url}\n" \
                -H "User-Agent: TailorIS-CDN-Warmup/1.0" \
                "${CDN_URL}${url}" 2>/dev/null || true
        ) &

        if [ $((++count % CONCURRENCY)) -eq 0 ]; then
            wait
        fi
    done
    wait

    log_info "CloudFront 预热完成"
}

# ==============================================================================
# 通用预热
# ==============================================================================
warmup_generic() {
    local urls=()
    if [ -n "$URL_FILE" ] && [ -f "$URL_FILE" ]; then
        mapfile -t urls < "$URL_FILE"
    else
        urls=("${DEFAULT_URLS[@]}")
    fi

    log_info "开始通用 CDN 预热 (共 ${#urls[@]} 个 URL, 并发: $CONCURRENCY)"

    if [ "$DRY_RUN" = true ]; then
        for url in "${urls[@]}"; do
            log_info "[DRY-RUN] 将预热: ${CDN_URL}${url}"
        done
        return 0
    fi

    local count=0
    for url in "${urls[@]}"; do
        (
            local status_code
            status_code=$(curl -s -o /dev/null -w "%{http_code}" \
                -H "User-Agent: TailorIS-CDN-Warmup/1.0" \
                "${CDN_URL}${url}" 2>/dev/null)
            if [ "$status_code" = "200" ] || [ "$status_code" = "304" ]; then
                echo "  ✓ ${CDN_URL}${url} ($status_code)"
            else
                echo "  ✗ ${CDN_URL}${url} ($status_code)"
            fi
        ) &

        if [ $((++count % CONCURRENCY)) -eq 0 ]; then
            wait
        fi
    done
    wait

    log_info "通用 CDN 预热完成"
}

# ==============================================================================
# 从 sitemap 读取 URL
# ==============================================================================
fetch_sitemap_urls() {
    local sitemap_url="${ORIGIN_URL}/sitemap.xml"
    local temp_file="/tmp/tailor-is-sitemap-urls.txt"

    log_info "从 sitemap 获取 URL 列表: $sitemap_url"
    curl -s "$sitemap_url" | grep -oP '<loc>\K[^<]+' > "$temp_file" 2>/dev/null || {
        log_warn "无法获取 sitemap，使用默认 URL 列表"
        return 1
    }

    # 转为相对路径
    sed -i "s|${ORIGIN_URL}||g" "$temp_file"
    URL_FILE="$temp_file"
    log_info "从 sitemap 获取了 $(wc -l < "$temp_file") 个 URL"
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
            --url-file|-f)
                URL_FILE="$2"
                shift 2
                ;;
            --concurrency|-c)
                CONCURRENCY="$2"
                shift 2
                ;;
            --sitemap|-s)
                fetch_sitemap_urls
                shift
                ;;
            --dry-run|-d)
                DRY_RUN=true
                shift
                ;;
            --help|-h)
                echo "用法: $0 [选项]"
                echo ""
                echo "选项:"
                echo "  --provider, -p   CDN 提供商 (aliyun/cloudfront/generic)"
                echo "  --url-file, -f   自定义 URL 列表文件 (每行一个 URL)"
                echo "  --concurrency, -c 并发请求数 (默认: 5)"
                echo "  --sitemap, -s    从 sitemap.xml 自动获取 URL"
                echo "  --dry-run, -d    预览模式"
                echo "  --help, -h       显示帮助"
                echo ""
                echo "环境变量:"
                echo "  ORIGIN_URL                  源站 URL (默认: https://www.tailoris.com)"
                echo "  CDN_URL                     CDN URL (默认: https://cdn.tailoris.com)"
                echo "  ALIYUN_ACCESS_KEY_ID        阿里云 AccessKey"
                echo "  ALIYUN_ACCESS_KEY_SECRET    阿里云 AccessKey Secret"
                echo ""
                echo "示例:"
                echo "  $0 -p aliyun                          # 预热阿里云 CDN"
                echo "  $0 -p cloudfront -s                   # 从 sitemap 预热"
                echo "  $0 -p aliyun -f urls.txt -c 10        # 并发10预热"
                echo "  $0 -p cloudfront -d                   # 预览模式"
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

    echo ""
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║           Tailor IS - CDN 缓存预热脚本                       ║"
    echo "╠══════════════════════════════════════════════════════════════╣"
    echo "║  提供商:    $PROVIDER"
    echo "║  源站:      $ORIGIN_URL"
    echo "║  CDN:       $CDN_URL"
    echo "║  并发:      $CONCURRENCY"
    echo "║  URL 文件:  ${URL_FILE:-默认内置}"
    echo "║  模式:      $([ "$DRY_RUN" = true ] && echo 'DRY-RUN' || echo 'LIVE')"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo ""

    case "$PROVIDER" in
        aliyun)
            warmup_aliyun
            ;;
        cloudfront)
            warmup_cloudfront
            ;;
        generic)
            warmup_generic
            ;;
        *)
            log_error "不支持的 CDN 提供商: $PROVIDER"
            exit 1
            ;;
    esac

    echo ""
    log_info "CDN 缓存预热完成"
    echo ""
    echo "提示: 预热后可通过以下命令验证:"
    echo "  curl -I ${CDN_URL}/static/img/logo.png | grep -i 'x-cache\|cf-cache'"
}

main "$@"