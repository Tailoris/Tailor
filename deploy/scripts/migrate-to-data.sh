#!/bin/bash
# ============================================================
# 功能：将 deploy/tailor-is-prod 目录迁移至 /data/tailor-is-prod
# 用法：sudo bash /home/tailor/Tailoris/deploy/migrate-to-data.sh
# ============================================================
set -e

SRC_DIR="/home/tailor/Tailoris/deploy/tailor-is-prod"
DEST_DIR="/data/tailor-is-prod"

echo "[1/4] 创建 /data 目录..."
mkdir -p /data

echo "[2/4] 复制目录结构至 $DEST_DIR ..."
cp -r "$SRC_DIR" "$DEST_DIR"

echo "[3/4] 设置权限..."
chmod -R 755 "$DEST_DIR"

echo "[4/4] 验证迁移结果..."
echo ""
echo "=== 目录结构 ==="
find "$DEST_DIR" -type d | sort
echo ""
echo "=== 环境变量文件 ==="
find "$DEST_DIR/env" -type f -exec sh -c 'echo "  $1"' _ {} \;
echo ""
echo "=== Docker 网络 ==="
docker network ls | grep tailor

echo ""
echo "迁移完成！/data/tailor-is-prod 已就绪。"