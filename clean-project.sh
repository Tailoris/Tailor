#!/bin/bash
# ============================================================
# Tailor IS 项目自动清理脚本
# 用途：清理编译缓存、编辑器配置、废弃备份文件
# 用法：./clean-project.sh
# ============================================================

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

echo "========================================="
echo " Tailor IS 项目清理脚本"
echo "========================================="

# 1. 清理编辑器缓存
echo "[1/5] 清理编辑器缓存..."
rm -rf .trae .obsidian .claudian .idea .vscode 2>/dev/null || true
echo "  ✓ 编辑器缓存已清理"

# 2. 清理编译产物
echo "[2/5] 清理编译产物..."
rm -rf dist build target out 2>/dev/null || true
echo "  ✓ 编译产物已清理"

# 3. 清理 Python 缓存
echo "[3/5] 清理 Python 缓存..."
find . -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true
find . -type f -name "*.pyc" -delete 2>/dev/null || true
echo "  ✓ Python 缓存已清理"

# 4. 清理日志和临时文件
echo "[4/5] 清理日志和临时文件..."
find . -type f \( -name "*.log" -o -name "*.tmp" -o -name "*.bak" \) -delete 2>/dev/null || true
echo "  ✓ 日志和临时文件已清理"

# 5. 清理废弃备份文件
echo "[5/5] 清理废弃备份文件..."
find . -type f \( -name "*-old.*" -o -name "*-backup.*" -o -name "*.old" -o -name "*备份*" \) -delete 2>/dev/null || true
echo "  ✓ 废弃备份文件已清理"

echo ""
echo "========================================="
echo " 项目清理完成！"
echo "========================================="