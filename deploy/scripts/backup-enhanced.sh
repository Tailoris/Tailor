#!/bin/bash
set -euo pipefail

# ============================================================
# 增强版生产环境备份脚本
# 功能：MySQL / Nacos / Redis / Docker Compose 配置 全量备份
# 作者：Tailor-IS Ops Team
# ============================================================

# ---------- 颜色输出常量 ----------
RED='\033[0;31m'      # 错误信息
GREEN='\033[0;32m'    # 成功信息
YELLOW='\033[1;33m'   # 警告信息
BLUE='\033[0;34m'     # 普通信息
NC='\033[0m'          # 重置颜色

# ---------- 日志函数 ----------
log_info()    { echo -e "${BLUE}[INFO]    $(date '+%Y-%m-%d %H:%M:%S') $*${NC}"; }
log_success() { echo -e "${GREEN}[SUCCESS] $(date '+%Y-%m-%d %H:%M:%S') $*${NC}"; }
log_warning() { echo -e "${YELLOW}[WARNING] $(date '+%Y-%m-%d %H:%M:%S') $*${NC}"; }
log_error()   { echo -e "${RED}[ERROR]   $(date '+%Y-%m-%d %H:%M:%S') $*${NC}" >&2; }

# ---------- 配置变量区（请根据实际环境修改） ----------
MYSQL_HOST="127.0.0.1"
MYSQL_PORT="3306"
MYSQL_USER="root"
MYSQL_PASSWORD="your_mysql_password_here"

REDIS_HOST="127.0.0.1"
REDIS_PORT="6379"
REDIS_PASSWORD=""
REDIS_DATA_DIR="/var/lib/redis"   # Redis 数据目录（dump.rdb / AOF 文件位置）

NACOS_HOST="127.0.0.1"
NACOS_PORT="8848"
NACOS_USERNAME=""
NACOS_PASSWORD=""

BACKUP_DIR="/opt/tailor-is/backups"
RETENTION_DAYS=7

# Docker Compose 相关文件路径
COMPOSE_FILE="/opt/tailor-is/docker-compose.prod.yml"
ENV_FILE="/opt/tailor-is/.env.production"
NGINX_CONF_DIR="/opt/tailor-is/nginx"

# 通知配置（可选）
NOTIFY_EMAIL=""          # 接收备份报告的邮箱，留空则禁用邮件通知
NOTIFY_WEBHOOK=""        # 企业微信/钉钉/Slack Webhook URL，留空则禁用

# ---------- 全局变量 ----------
TIMESTAMP=$(date '+%Y-%m-%d_%H-%M-%S')
CURRENT_BACKUP_DIR="${BACKUP_DIR}/${TIMESTAMP}"
LOG_FILE="${CURRENT_BACKUP_DIR}/backup.log"
SUMMARY_FILE="${CURRENT_BACKUP_DIR}/summary.txt"
BACKUP_STATUS="SUCCESS"   # 整体备份状态 SUCCESS / FAILED
BACKUP_ITEMS=()           # 记录每个备份项的信息
ERROR_MESSAGES=()         # 记录错误信息

# ============================================================
# 辅助函数：打印一条分隔线
# ============================================================
print_separator() {
    echo -e "${BLUE}------------------------------------------------------------${NC}"
}

# ============================================================
# 辅助函数：格式化文件大小（B / KB / MB / GB）
# ============================================================
human_size() {
    local bytes=$1
    if [ "$bytes" -lt 1024 ]; then
        echo "${bytes} B"
    elif [ "$bytes" -lt 1048576 ]; then
        echo "$(awk "BEGIN {printf \"%.2f\", $bytes/1024}") KB"
    elif [ "$bytes" -lt 1073741824 ]; then
        echo "$(awk "BEGIN {printf \"%.2f\", $bytes/1048576}") MB"
    else
        echo "$(awk "BEGIN {printf \"%.2f\", $bytes/1073741824}") GB"
    fi
}

# ============================================================
# 辅助函数：记录一个备份项的信息
# 参数：$1=类型, $2=文件路径, $3=状态(ok/fail), $4=额外信息
# ============================================================
record_item() {
    local type="$1"
    local path="$2"
    local status="$3"
    local extra="${4:-}"
    local size="0"

    if [ -e "$path" ]; then
        size=$(du -b "$path" 2>/dev/null | awk '{print $1}' || echo 0)
    fi
    BACKUP_ITEMS+=("${type}|${path}|${size}|${status}|${extra}")
}

# ============================================================
# 初始化：创建备份目录与日志
# ============================================================
init_backup() {
    log_info "初始化备份环境..."

    # 确保备份主目录存在，权限 750
    mkdir -p "${BACKUP_DIR}"
    chmod 750 "${BACKUP_DIR}"

    # 为本次备份创建独立目录
    mkdir -p "${CURRENT_BACKUP_DIR}"/{mysql,nacos,redis,configs}
    chmod 750 "${CURRENT_BACKUP_DIR}" "${CURRENT_BACKUP_DIR}"/{mysql,nacos,redis,configs}

    # 初始化日志文件
    touch "${LOG_FILE}"
    chmod 640 "${LOG_FILE}"

    # 将脚本输出同时写到日志文件
    exec > >(tee -a "${LOG_FILE}") 2>&1

    log_info "备份目录：${CURRENT_BACKUP_DIR}"
    log_info "日志文件：${LOG_FILE}"
    log_success "初始化完成"
}

# ============================================================
# 模块1：MySQL 数据库完整备份
# 备份所有非系统数据库，每个数据库生成独立的 .sql.gz 文件
# 使用 --single-transaction 保证InnoDB备份一致性，不锁表
# ============================================================
backup_mysql() {
    print_separator
    log_info "开始备份 MySQL 数据库..."

    local mysql_backup_dir="${CURRENT_BACKUP_DIR}/mysql"
    local start_ts=$(date +%s)

    # 检查 mysqldump 是否可用
    if ! command -v mysqldump &>/dev/null; then
        log_error "未找到 mysqldump 命令，跳过 MySQL 备份"
        ERROR_MESSAGES+=("mysqldump 不可用")
        BACKUP_STATUS="FAILED"
        record_item "mysql" "N/A" "fail" "mysqldump not found"
        return 0
    fi

    # MySQL 连接参数（-p 后面不能有空格）
    local conn_args="-h${MYSQL_HOST} -P${MYSQL_PORT} -u${MYSQL_USER}"
    if [ -n "${MYSQL_PASSWORD}" ]; then
        conn_args="${conn_args} -p${MYSQL_PASSWORD}"
    fi

    # 获取所有用户数据库列表，排除系统库
    log_info "获取数据库列表..."
    local databases
    databases=$(mysql ${conn_args} -N -e "SHOW DATABASES;" 2>/dev/null | grep -Ev "^(mysql|information_schema|performance_schema|sys)$") || {
        log_error "无法连接 MySQL 或获取数据库列表失败"
        ERROR_MESSAGES+=("MySQL 连接失败")
        BACKUP_STATUS="FAILED"
        record_item "mysql" "N/A" "fail" "connection error"
        return 0
    }

    if [ -z "${databases}" ]; then
        log_warning "未发现可备份的用户数据库"
        record_item "mysql" "N/A" "ok" "no user databases"
        return 0
    fi

    # 逐个数据库备份
    local db_count=0
    for db in ${databases}; do
        log_info "正在备份数据库：${db}"
        local dump_file="${mysql_backup_dir}/${db}.sql.gz"

        # 使用 mysqldump 导出，并通过 gzip 压缩
        # --single-transaction：对 InnoDB 开启一致性快照
        # --quick：逐行读取，避免在大表时占用大量内存
        # --lock-tables=false：不锁表，配合 --single-transaction 使用
        if mysqldump ${conn_args} \
                --single-transaction --quick --lock-tables=false \
                --databases "${db}" 2>/dev/null | gzip > "${dump_file}"; then
            chmod 640 "${dump_file}"
            local fsize
            fsize=$(stat -c%s "${dump_file}" 2>/dev/null || echo 0)
            log_success "  数据库 ${db} 备份完成 ($(human_size "$fsize"))"
            record_item "mysql" "${dump_file}" "ok" "${db}"
            db_count=$((db_count + 1))
        else
            log_error "  数据库 ${db} 备份失败"
            rm -f "${dump_file}" 2>/dev/null || true
            ERROR_MESSAGES+=("MySQL 备份失败：${db}")
            BACKUP_STATUS="FAILED"
            record_item "mysql" "${dump_file}" "fail" "${db}"
        fi
    done

    local end_ts=$(date +%s)
    local duration=$((end_ts - start_ts))
    log_info "MySQL 备份共完成 ${db_count} 个数据库，耗时 ${duration} 秒"
}

# ============================================================
# 模块2：Nacos 配置导出
# 通过 Nacos Open API 导出所有命名空间的所有配置
# ============================================================
backup_nacos() {
    print_separator
    log_info "开始备份 Nacos 配置..."

    local nacos_backup_dir="${CURRENT_BACKUP_DIR}/nacos"
    local nacos_base_url="http://${NACOS_HOST}:${NACOS_PORT}"
    local auth_args=""

    # 如有鉴权，构造 access_token（Nacos 2.x 推荐方式）
    if [ -n "${NACOS_USERNAME}" ] && [ -n "${NACOS_PASSWORD}" ]; then
        local token_resp
        token_resp=$(curl -s -X POST "${nacos_base_url}/nacos/v1/auth/login" \
            -d "username=${NACOS_USERNAME}&password=${NACOS_PASSWORD}" 2>/dev/null) || true
        local access_token
        access_token=$(echo "${token_resp}" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4 || true)
        if [ -n "${access_token}" ]; then
            auth_args="&accessToken=${access_token}"
            log_info "已获取 Nacos 访问令牌"
        else
            log_warning "Nacos 登录失败，尝试不使用令牌访问"
        fi
    fi

    # 第一步：获取所有命名空间
    log_info "获取命名空间列表..."
    local ns_resp
    ns_resp=$(curl -s "${nacos_base_url}/nacos/v1/console/namespaces" 2>/dev/null) || true

    local namespaces
    namespaces=$(echo "${ns_resp}" | grep -o '"namespace":"[^"]*"' | cut -d'"' -f4 || true)
    # public 命名空间可能为空字符串，补充一个
    if ! echo "${namespaces}" | grep -q "^$"; then
        namespaces=" ${namespaces}"
    fi

    if [ -z "${namespaces}" ]; then
        log_warning "未获取到命名空间，尝试使用默认（public）"
        namespaces=""
    fi

    local ns_count=0
    local cfg_total=0

    # 第二步：遍历每个命名空间，分页拉取全部配置
    for ns in ${namespaces}; do
        local ns_display="${ns:-public}"
        local ns_file="${nacos_backup_dir}/namespace_${ns_display}.json"
        local ns_param=""
        [ -n "${ns}" ] && ns_param="&tenant=${ns}"

        log_info "导出命名空间：${ns_display}"

        # 调用配置列表接口：pageSize=1000，足够覆盖大多数场景
        local config_resp
        config_resp=$(curl -s "${nacos_base_url}/nacos/v1/cs/configs?dataId=&group=&pageNo=1&pageSize=1000${ns_param}${auth_args}" 2>/dev/null) || true

        if [ -z "${config_resp}" ] || [ "${config_resp}" = "null" ]; then
            log_warning "  命名空间 ${ns_display} 无配置返回或请求失败"
            record_item "nacos" "N/A" "fail" "namespace ${ns_display} empty"
            continue
        fi

        # 以 JSON 格式保存全部配置信息
        echo "${config_resp}" > "${ns_file}"
        chmod 640 "${ns_file}"

        # 统计配置条数
        local cfg_count
        cfg_count=$(echo "${config_resp}" | grep -o '"dataId"' | wc -l || echo 0)

        local fsize
        fsize=$(stat -c%s "${ns_file}" 2>/dev/null || echo 0)
        log_success "  命名空间 ${ns_display} 导出完成：${cfg_count} 条配置 ($(human_size "$fsize"))"
        record_item "nacos" "${ns_file}" "ok" "ns=${ns_display},count=${cfg_count}"
        ns_count=$((ns_count + 1))
        cfg_total=$((cfg_total + cfg_count))
    done

    log_info "Nacos 备份共完成 ${ns_count} 个命名空间，合计 ${cfg_total} 条配置"
}

# ============================================================
# 模块3：Redis 数据持久化
# 触发 BGSAVE，并复制 dump.rdb 到备份目录；如启用 AOF 也同步备份
# ============================================================
backup_redis() {
    print_separator
    log_info "开始备份 Redis 数据..."

    local redis_backup_dir="${CURRENT_BACKUP_DIR}/redis"

    # 检查 redis-cli 是否可用
    if ! command -v redis-cli &>/dev/null; then
        log_error "未找到 redis-cli 命令，跳过 Redis 备份"
        ERROR_MESSAGES+=("redis-cli not found")
        BACKUP_STATUS="FAILED"
        record_item "redis" "N/A" "fail" "redis-cli not found"
        return 0
    fi

    # 构造 redis-cli 连接参数
    local cli_args="-h ${MYSQL_HOST} -p ${REDIS_PORT}"
    # 修正：redis-cli 使用 REDIS_HOST
    cli_args="-h ${REDIS_HOST} -p ${REDIS_PORT}"
    [ -n "${REDIS_PASSWORD}" ] && cli_args="${cli_args} -a ${REDIS_PASSWORD} --no-auth-warning"

    # 通过 INFO 命令检查连接是否可用
    if ! redis-cli ${cli_args} INFO server &>/dev/null; then
        log_error "Redis 连接失败"
        ERROR_MESSAGES+=("Redis 连接失败")
        BACKUP_STATUS="FAILED"
        record_item "redis" "N/A" "fail" "connection error"
        return 0
    fi

    # 记录 BGSAVE 之前的 rdb_last_bgsave_time_sec
    local before_save
    before_save=$(redis-cli ${cli_args} INFO persistence 2>/dev/null | grep "rdb_last_bgsave_time_sec" | cut -d: -f2 | tr -d '[:space:]' || echo "")

    # 触发 BGSAVE
    log_info "触发 BGSAVE 命令..."
    local bgsave_resp
    bgsave_resp=$(redis-cli ${cli_args} BGSAVE 2>/dev/null || echo "")
    log_info "BGSAVE 响应：${bgsave_resp}"

    # 等待 BGSAVE 完成（最长等待 300 秒）
    log_info "等待持久化完成..."
    local wait_time=0
    local max_wait=300
    while [ $wait_time -lt $max_wait ]; do
        local status
        status=$(redis-cli ${cli_args} INFO persistence 2>/dev/null | grep "rdb_bgsave_in_progress" | cut -d: -f2 | tr -d '[:space:]' || echo "1")
        if [ "${status}" = "0" ]; then
            break
        fi
        sleep 5
        wait_time=$((wait_time + 5))
        log_info "  已等待 ${wait_time} 秒..."
    done

    # 检查 BGSAVE 是否成功
    local bgsave_status
    bgsave_status=$(redis-cli ${cli_args} INFO persistence 2>/dev/null | grep "rdb_last_bgsave_status" | cut -d: -f2 | tr -d '[:space:]' || echo "")
    if [ "${bgsave_status}" != "ok" ]; then
        log_error "BGSAVE 执行失败（rdb_last_bgsave_status=${bgsave_status}）"
        ERROR_MESSAGES+=("Redis BGSAVE 失败")
        BACKUP_STATUS="FAILED"
        record_item "redis" "N/A" "fail" "bgsave failed"
        return 0
    fi
    log_success "BGSAVE 完成"

    # 获取 Redis 实际的 data 目录（优先从 CONFIG GET dir 读取）
    local real_data_dir
    real_data_dir=$(redis-cli ${cli_args} CONFIG GET dir 2>/dev/null | tail -n1 | tr -d '[:space:]' || echo "")
    if [ -z "${real_data_dir}" ] || [ ! -d "${real_data_dir}" ]; then
        real_data_dir="${REDIS_DATA_DIR}"
        log_warning "无法从 Redis 获取 data 目录，使用默认：${real_data_dir}"
    fi
    log_info "Redis 数据目录：${real_data_dir}"

    # 备份 dump.rdb
    local rdb_file="${real_data_dir}/dump.rdb"
    if [ -f "${rdb_file}" ]; then
        local dest_file="${redis_backup_dir}/dump.rdb"
        cp -f "${rdb_file}" "${dest_file}"
        chmod 640 "${dest_file}"
        local fsize
        fsize=$(stat -c%s "${dest_file}" 2>/dev/null || echo 0)
        log_success "dump.rdb 已备份 ($(human_size "$fsize"))"
        record_item "redis" "${dest_file}" "ok" "dump.rdb"
    else
        log_warning "未找到 dump.rdb 文件"
        record_item "redis" "N/A" "fail" "dump.rdb missing"
    fi

    # 检查 AOF 是否启用，如果启用则备份
    local aof_enabled
    aof_enabled=$(redis-cli ${cli_args} INFO persistence 2>/dev/null | grep "aof_enabled" | cut -d: -f2 | tr -d '[:space:]' || echo "0")
    if [ "${aof_enabled}" = "1" ]; then
        log_info "检测到 AOF 已启用，备份 AOF 文件..."

        # 常见 AOF 文件名：appendonly.aof
        local aof_file="${real_data_dir}/appendonly.aof"
        if [ -f "${aof_file}" ]; then
            local dest_aof="${redis_backup_dir}/appendonly.aof"
            cp -f "${aof_file}" "${dest_aof}"
            chmod 640 "${dest_aof}"
            local asize
            asize=$(stat -c%s "${dest_aof}" 2>/dev/null || echo 0)
            log_success "AOF 文件已备份 ($(human_size "$asize"))"
            record_item "redis" "${dest_aof}" "ok" "appendonly.aof"
        else
            log_warning "未找到 AOF 文件"
            record_item "redis" "N/A" "fail" "aof missing"
        fi
    else
        log_info "AOF 未启用，跳过 AOF 备份"
    fi
}

# ============================================================
# 模块4：Docker Compose 配置文件备份
# 备份 docker-compose.prod.yml、.env.production 和 nginx 配置目录
# ============================================================
backup_configs() {
    print_separator
    log_info "开始备份配置文件..."

    local config_backup_dir="${CURRENT_BACKUP_DIR}/configs"
    local item_count=0

    # 备份 docker-compose.prod.yml
    if [ -f "${COMPOSE_FILE}" ]; then
        cp -f "${COMPOSE_FILE}" "${config_backup_dir}/"
        chmod 640 "${config_backup_dir}/$(basename "${COMPOSE_FILE}")"
        local fsize
        fsize=$(stat -c%s "${config_backup_dir}/$(basename "${COMPOSE_FILE}")" 2>/dev/null || echo 0)
        log_success "已备份 ${COMPOSE_FILE} ($(human_size "$fsize"))"
        record_item "configs" "${config_backup_dir}/$(basename "${COMPOSE_FILE}")" "ok" "compose"
        item_count=$((item_count + 1))
    else
        log_warning "未找到 ${COMPOSE_FILE}"
        record_item "configs" "N/A" "fail" "${COMPOSE_FILE} missing"
    fi

    # 备份 .env.production
    if [ -f "${ENV_FILE}" ]; then
        cp -f "${ENV_FILE}" "${config_backup_dir}/"
        chmod 640 "${config_backup_dir}/$(basename "${ENV_FILE}")"
        local fsize
        fsize=$(stat -c%s "${config_backup_dir}/$(basename "${ENV_FILE}")" 2>/dev/null || echo 0)
        log_success "已备份 ${ENV_FILE} ($(human_size "$fsize"))"
        record_item "configs" "${config_backup_dir}/$(basename "${ENV_FILE}")" "ok" "env"
        item_count=$((item_count + 1))
    else
        log_warning "未找到 ${ENV_FILE}"
        record_item "configs" "N/A" "fail" "${ENV_FILE} missing"
    fi

    # 备份 nginx 配置目录
    if [ -d "${NGINX_CONF_DIR}" ]; then
        local nginx_tar="${config_backup_dir}/nginx-config.tar.gz"
        tar -czf "${nginx_tar}" -C "$(dirname "${NGINX_CONF_DIR}")" "$(basename "${NGINX_CONF_DIR}")" 2>/dev/null
        chmod 640 "${nginx_tar}"
        local fsize
        fsize=$(stat -c%s "${nginx_tar}" 2>/dev/null || echo 0)
        log_success "已备份 ${NGINX_CONF_DIR} ($(human_size "$fsize"))"
        record_item "configs" "${nginx_tar}" "ok" "nginx"
        item_count=$((item_count + 1))
    else
        log_warning "未找到 ${NGINX_CONF_DIR}"
        record_item "configs" "N/A" "fail" "nginx dir missing"
    fi

    log_info "配置文件备份完成：${item_count} 项"
}

# ============================================================
# 模块5：备份文件管理
# - 更新 latest 符号链接
# - 清理超过 RETENTION_DAYS 天的旧备份
# - 生成 SHA256 校验和
# ============================================================
manage_backups() {
    print_separator
    log_info "备份文件管理..."

    # 生成 SHA256 校验和
    log_info "生成 SHA256 校验和..."
    local checksum_file="${CURRENT_BACKUP_DIR}/sha256sums.txt"
    find "${CURRENT_BACKUP_DIR}" -type f \( -name "*.sql.gz" -o -name "*.json" -o -name "*.rdb" -o -name "*.aof" -o -name "*.yml" -o -name "*.yaml" -o -name "*.tar.gz" -o -name ".env*" \) \
        -exec sha256sum {} + | sed "s|${CURRENT_BACKUP_DIR}/||" > "${checksum_file}" 2>/dev/null || true
    chmod 640 "${checksum_file}"
    log_success "SHA256 校验和已生成"

    # 更新 latest 符号链接，指向本次备份
    log_info "更新 latest 符号链接..."
    ln -sfn "${CURRENT_BACKUP_DIR}" "${BACKUP_DIR}/latest"
    log_success "latest -> ${CURRENT_BACKUP_DIR}"

    # 清理过期备份（保留最近 RETENTION_DAYS 天）
    log_info "清理 ${RETENTION_DAYS} 天前的旧备份..."
    local cleaned=0
    while IFS= read -r dir; do
        if [ -n "${dir}" ] && [ "${dir}" != "${CURRENT_BACKUP_DIR}" ]; then
            log_info "  删除旧备份：${dir}"
            rm -rf "${dir}"
            cleaned=$((cleaned + 1))
        fi
    done < <(find "${BACKUP_DIR}" -maxdepth 1 -type d -mtime +${RETENTION_DAYS} -name "????-??-??_??-??-??" 2>/dev/null)

    # 同时清理不再指向有效目录的 latest 链接
    if [ -L "${BACKUP_DIR}/latest" ] && [ ! -e "${BACKUP_DIR}/latest" ]; then
        log_warning "latest 符号链接失效，已重置"
        ln -sfn "${CURRENT_BACKUP_DIR}" "${BACKUP_DIR}/latest"
    fi

    log_info "共清理 ${cleaned} 个旧备份目录"
}

# ============================================================
# 模块6：生成摘要报告 + 通知
# ============================================================
generate_report() {
    print_separator
    log_info "生成备份摘要报告..."

    local total_size=0
    local total_items=0
    local success_items=0
    local fail_items=0

    # 统计整体信息
    for item in "${BACKUP_ITEMS[@]}"; do
        IFS='|' read -r _type _path _size _status _extra <<< "$item"
        total_items=$((total_items + 1))
        total_size=$((total_size + _size))
        if [ "${_status}" = "ok" ]; then
            success_items=$((success_items + 1))
        else
            fail_items=$((fail_items + 1))
        fi
    done

    # 写入摘要文件
    {
        echo "=============== Tailor-IS 备份摘要报告 ==============="
        echo "备份时间   : ${TIMESTAMP}"
        echo "备份目录   : ${CURRENT_BACKUP_DIR}"
        echo "整体状态   : ${BACKUP_STATUS}"
        echo "备份项总数 : ${total_items}"
        echo "成功项     : ${success_items}"
        echo "失败项     : ${fail_items}"
        echo "总大小     : $(human_size "${total_size}")"
        echo "保留天数   : ${RETENTION_DAYS}"
        echo "-------------------------------------------------------"
        echo "详细项列表："
        for item in "${BACKUP_ITEMS[@]}"; do
            IFS='|' read -r _type _path _size _status _extra <<< "$item"
            printf "  [%-6s] [%-4s] %-60s %s  %s\n" \
                "$_type" "$_status" "$(basename "$_path" 2>/dev/null || echo "$_path")" "$(human_size "$_size")" "$_extra"
        done
        echo "-------------------------------------------------------"
        if [ ${#ERROR_MESSAGES[@]} -gt 0 ]; then
            echo "错误信息："
            for err in "${ERROR_MESSAGES[@]}"; do
                echo "  - ${err}"
            done
        else
            echo "错误信息：无"
        fi
        echo "======================================================="
    } > "${SUMMARY_FILE}"
    chmod 640 "${SUMMARY_FILE}"

    cat "${SUMMARY_FILE}"

    # 发送通知（邮件 + Webhook）
    send_notification
}

# ============================================================
# 辅助函数：发送通知（邮件 / Webhook）
# ============================================================
send_notification() {
    local subject="[Tailor-IS] 备份${BACKUP_STATUS} - ${TIMESTAMP}"
    local body
    body=$(cat "${SUMMARY_FILE}")

    # 邮件通知（需要 mail / sendmail 命令）
    if [ -n "${NOTIFY_EMAIL}" ]; then
        if command -v mail &>/dev/null; then
            echo "${body}" | mail -s "${subject}" "${NOTIFY_EMAIL}" 2>/dev/null && \
                log_success "邮件通知已发送到 ${NOTIFY_EMAIL}" || \
                log_warning "邮件发送失败"
        else
            log_warning "未找到 mail 命令，跳过邮件通知"
        fi
    fi

    # Webhook 通知（兼容 企业微信 / 钉钉 / Slack 的通用 JSON 格式）
    if [ -n "${NOTIFY_WEBHOOK}" ]; then
        local payload
        payload=$(printf '{"msgtype":"text","text":{"content":"%s"}}' "${subject}

${body}")
        curl -s -X POST -H "Content-Type: application/json" \
            -d "${payload}" "${NOTIFY_WEBHOOK}" >/dev/null 2>&1 && \
            log_success "Webhook 通知已发送" || \
            log_warning "Webhook 发送失败"
    fi

    # 失败时强制警告一次
    if [ "${BACKUP_STATUS}" = "FAILED" ]; then
        log_error "备份整体状态为 FAILED，请管理员检查！"
    fi
}

# ============================================================
# 主流程：执行完整备份
# ============================================================
do_full_backup() {
    print_separator
    log_info "========== Tailor-IS 增强版备份脚本启动 =========="
    log_info "开始时间：$(date '+%Y-%m-%d %H:%M:%S')"

    local start_time
    start_time=$(date +%s)

    init_backup
    backup_mysql
    backup_nacos
    backup_redis
    backup_configs
    manage_backups
    generate_report

    local end_time
    end_time=$(date +%s)
    local total_duration=$((end_time - start_time))

    print_separator
    log_info "总耗时：${total_duration} 秒"
    log_info "结束时间：$(date '+%Y-%m-%d %H:%M:%S')"
    log_success "========== 备份流程结束，整体状态：${BACKUP_STATUS} =========="

    # 失败时返回非零退出码，便于 cron 监控
    if [ "${BACKUP_STATUS}" = "FAILED" ]; then
        return 1
    fi
    return 0
}

# ============================================================
# 恢复功能：--list 列出所有备份
# ============================================================
list_backups() {
    print_separator
    log_info "可用备份列表（${BACKUP_DIR}）："
    print_separator
    printf "%-25s  %-12s  %-12s  %s\n" "备份目录" "大小" "状态" "创建时间"
    printf "%-25s  %-12s  %-12s  %s\n" "-------------------------" "----------" "----------" "-------------------"

    local found=0
    while IFS= read -r dir; do
        if [ -n "${dir}" ]; then
            local name
            name=$(basename "${dir}")
            local dsize
            dsize=$(du -sh "${dir}" 2>/dev/null | awk '{print $1}' || echo "?")
            local ctime
            ctime=$(stat -c '%y' "${dir}" 2>/dev/null | cut -d. -f1 || echo "?")
            local status="OK"
            [ -f "${dir}/summary.txt" ] && status=$(grep "整体状态" "${dir}/summary.txt" 2>/dev/null | awk -F: '{print $2}' | tr -d ' ' || echo "OK")
            printf "%-25s  %-12s  %-12s  %s\n" "$name" "$dsize" "$status" "$ctime"
            found=$((found + 1))
        fi
    done < <(find "${BACKUP_DIR}" -maxdepth 1 -type d -name "????-??-??_??-??-??" | sort -r 2>/dev/null)

    if [ "$found" -eq 0 ]; then
        log_warning "未发现任何备份目录"
    fi

    if [ -L "${BACKUP_DIR}/latest" ]; then
        local linked
        linked=$(readlink "${BACKUP_DIR}/latest")
        log_info "latest 指向：${linked}"
    fi
}

# ============================================================
# 恢复功能：--restore <备份目录> <类型>
# type: mysql / redis / configs
# ============================================================
restore_backup() {
    local backup_name="$1"
    local restore_type="$2"

    local restore_dir="${BACKUP_DIR}/${backup_name}"
    # 允许传入绝对路径
    if [ -d "${backup_name}" ]; then
        restore_dir="${backup_name}"
    fi

    if [ ! -d "${restore_dir}" ]; then
        log_error "备份目录不存在：${restore_dir}"
        return 1
    fi

    log_info "准备从 ${restore_dir} 恢复 [${restore_type}]"
    log_warning "恢复操作会覆盖现有数据，请确认已了解风险！"

    case "${restore_type}" in
        mysql)
            log_info "开始恢复 MySQL..."
            local mysql_dir="${restore_dir}/mysql"
            if [ ! -d "${mysql_dir}" ]; then
                log_error "备份目录中没有 mysql 子目录"
                return 1
            fi
            local conn_args="-h${MYSQL_HOST} -P${MYSQL_PORT} -u${MYSQL_USER}"
            [ -n "${MYSQL_PASSWORD}" ] && conn_args="${conn_args} -p${MYSQL_PASSWORD}"

            for f in "${mysql_dir}"/*.sql.gz; do
                [ -f "$f" ] || continue
                local db_name
                db_name=$(basename "$f" .sql.gz)
                log_info "恢复数据库：${db_name}"
                zcat "$f" | mysql ${conn_args} 2>/dev/null && \
                    log_success "  ${db_name} 恢复完成" || \
                    log_error "  ${db_name} 恢复失败"
            done
            ;;
        redis)
            log_info "开始恢复 Redis（需要重启 redis-server 使 dump.rdb 生效）..."
            local rdb_file="${restore_dir}/redis/dump.rdb"
            if [ ! -f "${rdb_file}" ]; then
                log_error "备份目录中没有 dump.rdb"
                return 1
            fi
            # 先从 Redis 读取 data 目录
            local cli_args="-h ${REDIS_HOST} -p ${REDIS_PORT}"
            [ -n "${REDIS_PASSWORD}" ] && cli_args="${cli_args} -a ${REDIS_PASSWORD} --no-auth-warning"
            local real_data_dir
            real_data_dir=$(redis-cli ${cli_args} CONFIG GET dir 2>/dev/null | tail -n1 | tr -d '[:space:]' || echo "${REDIS_DATA_DIR}")

            log_warning "请先停止 redis-server，再执行："
            echo "  cp ${rdb_file} ${real_data_dir}/dump.rdb"
            echo "  chown redis:redis ${real_data_dir}/dump.rdb"
            echo "  # 然后启动 redis-server"
            ;;
        configs)
            log_info "开始恢复配置文件..."
            local conf_dir="${restore_dir}/configs"
            if [ ! -d "${conf_dir}" ]; then
                log_error "备份目录中没有 configs 子目录"
                return 1
            fi
            [ -f "${conf_dir}/docker-compose.prod.yml" ] && \
                cp -f "${conf_dir}/docker-compose.prod.yml" "${COMPOSE_FILE}" && \
                log_success "已恢复 docker-compose.prod.yml"
            [ -f "${conf_dir}/.env.production" ] && \
                cp -f "${conf_dir}/.env.production" "${ENV_FILE}" && \
                log_success "已恢复 .env.production"
            if [ -f "${conf_dir}/nginx-config.tar.gz" ]; then
                tar -xzf "${conf_dir}/nginx-config.tar.gz" -C "$(dirname "${NGINX_CONF_DIR}")" 2>/dev/null && \
                    log_success "已恢复 nginx 配置目录"
            fi
            ;;
        *)
            log_error "不支持的恢复类型：${restore_type}（可选：mysql / redis / configs）"
            return 1
            ;;
    esac

    log_success "恢复流程结束，请在应用端验证数据完整性"
}

# ============================================================
# 帮助信息
# ============================================================
show_help() {
    cat <<EOF
用法：
  $0                     执行完整备份
  $0 --list              列出所有可用备份
  $0 --restore <dir> <type>  从指定备份恢复，type 可选 mysql / redis / configs
  $0 --help              显示本帮助

示例：
  $0
  $0 --list
  $0 --restore 2026-06-11_02-30-00 mysql
  $0 --restore /opt/tailor-is/backups/2026-06-11_02-30-00 configs

配置说明：
  - 在脚本顶部的【配置变量区】修改 MySQL / Redis / Nacos / 备份路径 / 通知 等参数
  - 备份目录默认为 ${BACKUP_DIR}
  - 保留天数默认为 ${RETENTION_DAYS} 天
EOF
}

# ============================================================
# 入口：解析命令行参数
# ============================================================
case "${1:-}" in
    "--list")
        list_backups
        ;;
    "--restore")
        if [ $# -lt 3 ]; then
            log_error "参数不足，用法：$0 --restore <备份目录> <类型>"
            show_help
            exit 1
        fi
        restore_backup "$2" "$3"
        ;;
    "--help" | "-h")
        show_help
        ;;
    "")
        do_full_backup
        ;;
    *)
        log_error "未知参数：$1"
        show_help
        exit 1
        ;;
esac
