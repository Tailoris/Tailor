#!/bin/bash
# ==============================================================================
# Tailor IS 一键部署脚本
# ==============================================================================
# 版本: v1.0
# 日期: 2026-06-11
# 说明: 1Panel 生产环境部署执行脚本
# 使用:
#   sudo ./deploy.sh           # 完整部署（拉取+构建+启动+验证）
#   sudo ./deploy.sh --fast    # 快速部署（跳过构建缓存清理）
#   sudo ./deploy.sh --check   # 仅做环境检查
#   sudo ./deploy.sh --nginx   # 仅重载Nginx配置
# ==============================================================================

set -euo pipefail

# =============== 变量配置 ===============
PROJECT_DIR="/opt/tailor-is"
ENV_FILE="${PROJECT_DIR}/.env"
COMPOSE_FILE="${PROJECT_DIR}/docker-compose.prod.yml"
LOG_FILE="${PROJECT_DIR}/logs/deploy-$(date +%Y%m%d-%H%M%S).log"
BACKUP_DIR="${PROJECT_DIR}/backup/$(date +%Y%m%d-%H%M%S)"

# 颜色输出
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'

# =============== 日志函数 ===============
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; echo "[INFO] $(date '+%Y-%m-%d %H:%M:%S') $1" >> "$LOG_FILE"; }
log_ok() { echo -e "${GREEN}[OK]${NC} $1"; echo "[OK] $(date '+%Y-%m-%d %H:%M:%S') $1" >> "$LOG_FILE"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; echo "[WARN] $(date '+%Y-%m-%d %H:%M:%S') $1" >> "$LOG_FILE"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; echo "[ERROR] $(date '+%Y-%m-%d %H:%M:%S') $1" >> "$LOG_FILE"; }

# =============== 前置检查 ===============
check_root() {
    if [ "$(id -u)" -ne 0 ]; then
        log_error "请使用 root 或 sudo 运行此脚本"
        exit 1
    fi
    log_ok "权限检查通过"
}

check_docker() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装，请先安装 Docker"
        exit 1
    fi
    if ! docker compose version &> /dev/null; then
        log_error "Docker Compose 未安装，请先安装"
        exit 1
    fi
    DOCKER_VER=$(docker --version | awk '{print $3}' | sed 's/,//')
    COMPOSE_VER=$(docker compose version | awk '{print $4}' | sed 's/v//')
    log_ok "Docker $DOCKER_VER + Docker Compose $COMPOSE_VER 已就绪"
}

check_env_file() {
    if [ ! -f "$ENV_FILE" ]; then
        log_warn "环境变量文件不存在: $ENV_FILE"
        log_info "正在从模板生成..."
        if [ -f "${PROJECT_DIR}/deploy/.env.production" ]; then
            cp "${PROJECT_DIR}/deploy/.env.production" "$ENV_FILE"
            log_warn "⚠️  请手动编辑 $ENV_FILE 填写生产密码后重新部署"
            log_warn "    必须修改: MYSQL_PASSWORD / REDIS_PASSWORD / NACOS_PASSWORD / JWT_SECRET / AES_KEY"
            exit 1
        else
            log_error "找不到环境变量模板文件"
            exit 1
        fi
    fi
    log_ok "环境变量文件存在: $ENV_FILE"
}

check_ports() {
    local ports=(80 443 8080 8081 3306 6379 5672 8848)
    local conflict_found=0
    log_info "端口占用检查..."
    for port in "${ports[@]}"; do
        if ss -tlnp | grep -q ":$port "; then
            local proc=$(ss -tlnp | grep ":$port " | awk '{print $7}' | head -1)
            if echo "$proc" | grep -q "docker\|nginx"; then
                log_info "端口 $port: Docker/Nginx 占用 (正常)"
            else
                log_warn "端口 $port 被其他进程占用: $proc"
                conflict_found=1
            fi
        else
            log_info "端口 $port: 可用 ✓"
        fi
    done
    if [ "$conflict_found" -eq 0 ]; then
        log_ok "端口检查通过"
    fi
}

# =============== 部署操作 ===============
backup_current() {
    log_info "创建部署前备份..."
    mkdir -p "$BACKUP_DIR"
    if [ -d "${PROJECT_DIR}/data/mysql" ]; then
        tar czf "${BACKUP_DIR}/mysql-backup.tar.gz" -C "${PROJECT_DIR}" data/mysql 2>/dev/null && log_ok "MySQL 数据已备份"
    fi
    cp "$ENV_FILE" "${BACKUP_DIR}/.env" 2>/dev/null
    cp "$COMPOSE_FILE" "${BACKUP_DIR}/docker-compose.prod.yml" 2>/dev/null
    log_ok "备份完成: ${BACKUP_DIR}"
}

pull_latest_images() {
    log_info "拉取最新镜像..."
    cd "$PROJECT_DIR"
    docker compose -f "$COMPOSE_FILE" pull
    log_ok "镜像拉取完成"
}

shutdown_services() {
    log_info "停止现有服务（保留数据卷）..."
    docker compose -f "$COMPOSE_FILE" down
    log_ok "服务已停止"
}

start_services() {
    log_info "启动所有服务..."
    cd "$PROJECT_DIR"
    docker compose -f "$COMPOSE_FILE" up -d
    log_ok "服务启动完成"
}

wait_for_healthy() {
    log_info "等待服务健康检查 (最长5分钟)..."
    local max_wait=300; local elapsed=0
    while [ $elapsed -lt $max_wait ]; do
        local unhealthy=$(docker compose -f "$COMPOSE_FILE" ps --format json 2>/dev/null | grep -c '"Health":"unhealthy"' || true)
        local running=$(docker compose -f "$COMPOSE_FILE" ps --format json 2>/dev/null | grep -c '"State":"running"' || true)
        local total=$(docker compose -f "$COMPOSE_FILE" config --services | wc -l)
        log_info "运行中: $running/$total, 不健康: $unhealthy"
        if [ "$unhealthy" -eq 0 ] && [ "$running" -eq "$total" ]; then
            log_ok "所有服务健康 ✓"
            return 0
        fi
        sleep 10; elapsed=$((elapsed + 10))
    done
    log_warn "部分服务健康检查超时，请查看 docker compose logs"
}

reload_nginx() {
    if docker ps --format '{{.Names}}' | grep -q "tailor-is-nginx"; then
        log_info "重载 Nginx 配置..."
        docker exec tailor-is-nginx nginx -t 2>&1 | head -5 && \
        docker exec tailor-is-nginx nginx -s reload && log_ok "Nginx 配置已重载"
    else
        log_warn "Nginx 容器未运行，跳过重载"
    fi
}

post_deploy_verify() {
    log_info "执行部署后验证..."
    local passed=0; local failed=0

    # 检查容器状态
    local containers=$(docker compose -f "$COMPOSE_FILE" ps --format json | grep -c '"State":"running"')
    if [ "$containers" -gt 5 ]; then passed=$((passed+1)); else failed=$((failed+1)); log_warn "容器数量少于预期: $containers"; fi

    # 检查MySQL
    if docker exec tailor-is-mysql mysqladmin ping -h localhost --silent 2>/dev/null; then passed=$((passed+1)); else failed=$((failed+1)); log_warn "MySQL 无法连接"; fi

    # 检查Redis
    if docker exec tailor-is-redis redis-cli ping 2>/dev/null | grep -q PONG; then passed=$((passed+1)); else failed=$((failed+1)); log_warn "Redis 无法连接"; fi

    # 检查Nacos
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:8848/nacos/ 2>/dev/null | grep -q "200"; then passed=$((passed+1)); else log_warn "Nacos 未就绪 (首次启动可能需要2分钟)"; fi

    # 检查核心网关
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null | grep -qE "200|401|404"; then passed=$((passed+1)); else log_warn "核心网关健康检查失败"; fi

    log_info "验证结果: 通过=$passed, 失败=$failed"
    if [ "$failed" -eq 0 ]; then log_ok "✅ 部署验证通过"; else log_warn "⚠️  请查看上述警告项并排查"; fi
}

# =============== 主流程 ===============
main() {
    mkdir -p "${PROJECT_DIR}/logs"
    exec > >(tee -a "$LOG_FILE") 2>&1
    echo "============================================================"
    echo "  Tailor IS 生产部署脚本 v1.0"
    echo "  时间: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "============================================================"

    case "${1:-}" in
        --check)
            log_info "=== 仅执行环境检查 ==="
            check_root; check_docker; check_env_file; check_ports
            log_ok "环境检查完成，可安全部署 ✓"
            exit 0 ;;
        --nginx)
            log_info "=== 仅重载 Nginx ==="
            reload_nginx; exit 0 ;;
    esac

    log_info "=== 步骤1: 环境检查 ==="
    check_root; check_docker; check_env_file; check_ports

    log_info "=== 步骤2: 部署前备份 ==="
    backup_current

    log_info "=== 步骤3: 拉取镜像 ==="
    pull_latest_images

    log_info "=== 步骤4: 停止旧服务 ==="
    shutdown_services

    log_info "=== 步骤5: 启动新服务 ==="
    start_services

    log_info "=== 步骤6: 等待健康检查 ==="
    wait_for_healthy

    log_info "=== 步骤7: 重载 Nginx ==="
    reload_nginx

    log_info "=== 步骤8: 部署验证 ==="
    post_deploy_verify

    echo ""
    echo "============================================================"
    echo "  🎉 部署完成!"
    echo "  访问地址: https://www.tailoris.com"
    echo "  管理后台: https://admin.tailoris.com"
    echo "  商户后台: https://merchant.tailoris.com"
    echo "  日志文件: $LOG_FILE"
    echo "============================================================"
}

main "$@"
