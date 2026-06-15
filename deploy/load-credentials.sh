#!/bin/bash
# =============================================================
# Tailor IS 凭证加载器 (从 .env.production 读取并导出环境变量)
# =============================================================
# 用法: source load-credentials.sh
# 作用: 统一管理所有服务密码,避免硬编码和密码不一致
# =============================================================

# 定位凭证文件 (向上 3 级目录查找)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CRED_FILE="$SCRIPT_DIR/.env.production"

# 检查凭证文件
if [ ! -f "$CRED_FILE" ]; then
    echo "[ERROR] 凭证文件不存在: $CRED_FILE" >&2
    return 1 2>/dev/null || exit 1
fi

# 解析 .env.production 并导出
while IFS='=' read -r key value; do
    # 去除 Windows 换行符 \r
    key="${key//$'\r'/}"
    value="${value//$'\r'/}"
    # 跳过空行和注释
    [[ -z "$key" || "$key" =~ ^[[:space:]]*# ]] && continue
    # 去除首尾空白
    key=$(echo "$key" | xargs)
    # 跳过空键名
    [[ -z "$key" ]] && continue
    # 去除值的首尾空白（允许空值）
    value=$(echo "$value" | xargs)
    # 导出（值可为空）
    export "${key}=${value}"
done < "$CRED_FILE"

# 验证关键凭证已加载
required_vars=("MYSQL_PASSWORD" "REDIS_PASSWORD" "NACOS_ADDR" "GATEWAY_PORT")
missing=()
for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        missing+=("$var")
    fi
done

if [ ${#missing[@]} -gt 0 ]; then
    echo "[ERROR] 以下凭证变量未加载: ${missing[*]}" >&2
    return 1 2>/dev/null || exit 1
fi

# 输出（可选）
if [ "${VERBOSE:-false}" = "true" ]; then
    echo "[OK] 凭证已加载:"
    echo "  MySQL: $MYSQL_HOST:$MYSQL_PORT"
    echo "  Redis: $REDIS_HOST:$REDIS_PORT"
    echo "  Nacos: $NACOS_ADDR"
fi
