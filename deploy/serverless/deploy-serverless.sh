#!/usr/bin/env bash
# ============================================================
# Tailor IS Serverless 部署脚本
# 支持: Alibaba Cloud FC / AWS Lambda
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../tailor-is" && pwd)"

# ---- 颜色输出 ----
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }
log_step()  { echo -e "${BLUE}[STEP]${NC}  $*"; }

# ---- 默认值 ----
PLATFORM="${PLATFORM:-aliyun}"
REGION="${REGION:-cn-shanghai}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
SERVICE="${SERVICE:-all}"
DOCKER_REGISTRY="${DOCKER_REGISTRY:-registry.cn-shanghai.aliyuncs.com/tailoris}"
AWS_ACCOUNT_ID="${AWS_ACCOUNT_ID:-}"
AWS_ECR_REGISTRY="${AWS_ECR_REGISTRY:-}"

usage() {
    cat <<EOF
Usage: $0 [OPTIONS]

Options:
  -p, --platform   目标平台: aliyun | aws (默认: aliyun)
  -r, --region     部署区域 (默认: cn-shanghai)
  -t, --tag        镜像标签 (默认: latest)
  -s, --service    部署服务: community | academy | all (默认: all)
  --registry       容器镜像仓库 (默认: registry.cn-shanghai.aliyuncs.com/tailoris)
  --aws-account-id AWS 账户 ID (AWS 部署必需)
  -h, --help       显示帮助信息

Environment Variables:
  PLATFORM, REGION, IMAGE_TAG, SERVICE, DOCKER_REGISTRY
  AWS_ACCOUNT_ID, AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY
  ALIBABA_CLOUD_ACCOUNT_ID, ALIBABA_CLOUD_ACCESS_KEY_ID, ALIBABA_CLOUD_ACCESS_KEY_SECRET

Examples:
  $0 -p aliyun -s community
  $0 -p aws -r us-east-1 -s all --aws-account-id 123456789012
EOF
    exit 1
}

# ---- 参数解析 ----
while [[ $# -gt 0 ]]; do
    case "$1" in
        -p|--platform) PLATFORM="$2"; shift 2 ;;
        -r|--region)   REGION="$2"; shift 2 ;;
        -t|--tag)      IMAGE_TAG="$2"; shift 2 ;;
        -s|--service)  SERVICE="$2"; shift 2 ;;
        --registry)    DOCKER_REGISTRY="$2"; shift 2 ;;
        --aws-account-id) AWS_ACCOUNT_ID="$2"; shift 2 ;;
        -h|--help)     usage ;;
        *)             log_error "未知参数: $1"; usage ;;
    esac
done

# ---- 校验 ----
if [[ "$PLATFORM" == "aws" && -z "$AWS_ACCOUNT_ID" ]]; then
    log_error "AWS 部署需要 --aws-account-id 参数"
    exit 1
fi

# ---- 构建 Docker 镜像 ----
build_image() {
    local module="$1"
    local dockerfile="$PROJECT_ROOT/tailor-is-${module}/Dockerfile.serverless"
    local image_name="${DOCKER_REGISTRY}/${module}:${IMAGE_TAG}"

    log_step "构建 ${module} 镜像..."
    if [[ ! -f "$dockerfile" ]]; then
        log_error "Dockerfile 不存在: $dockerfile"
        exit 1
    fi

    docker build \
        -f "$dockerfile" \
        -t "$image_name" \
        "$PROJECT_ROOT"

    if [[ $? -ne 0 ]]; then
        log_error "${module} 镜像构建失败"
        exit 1
    fi
    log_info "${module} 镜像构建成功: $image_name"
}

# ---- 推送镜像 ----
push_image() {
    local module="$1"
    local image_name="${DOCKER_REGISTRY}/${module}:${IMAGE_TAG}"

    log_step "推送 ${module} 镜像到仓库..."

    if [[ "$PLATFORM" == "aws" ]]; then
        local ecr_registry="${AWS_ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"
        local ecr_image="${ecr_registry}/tailoris/${module}:${IMAGE_TAG}"

        # 登录 ECR
        aws ecr get-login-password --region "$REGION" | \
            docker login --username AWS --password-stdin "$ecr_registry"

        # 重新打标签并推送
        docker tag "$image_name" "$ecr_image"
        docker push "$ecr_image"
        log_info "推送到 ECR: $ecr_image"
    else
        # 阿里云容器镜像服务
        docker push "$image_name"
        log_info "推送到 ACR: $image_name"
    fi
}

# ---- 阿里云 FC 部署 ----
deploy_aliyun() {
    log_step "使用 Serverless Devs 部署到阿里云 FC..."

    if ! command -v s &> /dev/null; then
        log_error "Serverless Devs CLI 未安装，请执行: npm install -g @serverless-devs/s"
        exit 1
    fi

    cd "$SCRIPT_DIR"

    export REGION="$REGION"
    export IMAGE_TAG="$IMAGE_TAG"

    if [[ "$SERVICE" == "community" || "$SERVICE" == "all" ]]; then
        log_info "部署社区服务..."
        s community deploy -t serverless.yml
    fi

    if [[ "$SERVICE" == "academy" || "$SERVICE" == "all" ]]; then
        log_info "部署学院服务..."
        s academy deploy -t serverless.yml
    fi

    log_info "阿里云 FC 部署完成"
}

# ---- AWS Lambda 部署 ----
deploy_aws() {
    log_step "使用 AWS SAM 部署到 Lambda..."

    if ! command -v sam &> /dev/null; then
        log_error "AWS SAM CLI 未安装"
        exit 1
    fi

    cd "$SCRIPT_DIR"

    local s3_bucket="${S3_BUCKET:-tailor-is-sam-deploy-${AWS_ACCOUNT_ID}}"

    # 确保 S3 桶存在
    aws s3api head-bucket --bucket "$s3_bucket" 2>/dev/null || \
        aws s3 mb "s3://${s3_bucket}" --region "$REGION"

    sam build --template template.yml --region "$REGION"

    sam deploy \
        --template template.yml \
        --stack-name "tailor-is-serverless" \
        --region "$REGION" \
        --capabilities CAPABILITY_IAM CAPABILITY_AUTO_EXPAND \
        --s3-bucket "$s3_bucket" \
        --parameter-overrides \
            "ImageTag=${IMAGE_TAG}" \
            "VpcId=${VPC_ID:-}" \
            "SubnetIds=${SUBNET_IDS:-}" \
            "SecurityGroupId=${SECURITY_GROUP_ID:-}" \
            "MysqlHost=${MYSQL_HOST:-}" \
            "RedisHost=${REDIS_HOST:-}" \
        --no-fail-on-empty-changeset

    log_info "AWS Lambda 部署完成"
}

# ---- 主流程 ----
main() {
    log_info "=== Tailor IS Serverless 部署开始 ==="
    log_info "平台: ${PLATFORM}, 区域: ${REGION}, 服务: ${SERVICE}"
    log_info "镜像标签: ${IMAGE_TAG}"
    echo ""

    # 步骤 1: 构建镜像
    if [[ "$SERVICE" == "community" || "$SERVICE" == "all" ]]; then
        build_image "community"
    fi
    if [[ "$SERVICE" == "academy" || "$SERVICE" == "all" ]]; then
        build_image "academy"
    fi

    # 步骤 2: 推送镜像
    if [[ "$SERVICE" == "community" || "$SERVICE" == "all" ]]; then
        push_image "community"
    fi
    if [[ "$SERVICE" == "academy" || "$SERVICE" == "all" ]]; then
        push_image "academy"
    fi

    # 步骤 3: 部署
    case "$PLATFORM" in
        aliyun) deploy_aliyun ;;
        aws)    deploy_aws ;;
        *)
            log_error "不支持的平台: ${PLATFORM}，可选: aliyun | aws"
            exit 1
            ;;
    esac

    echo ""
    log_info "=== Tailor IS Serverless 部署完成 ==="
}

main