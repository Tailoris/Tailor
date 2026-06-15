#!/bin/bash
# 系统安全补丁检查脚本
# 用法: check-system-patches.sh

echo "=========================================="
echo "系统安全补丁检查 - $(date)"
echo "=========================================="

echo ""
echo "[1] 系统信息:"
uname -a

echo ""
echo "[2] 可用更新检查:"
if command -v apt-get > /dev/null 2>&1; then
    echo "检测到Debian/Ubuntu系统"
    UPDATES=$(apt list --upgradable 2>/dev/null | wc -l)
    SECURITY_UPDATES=$(apt list --upgradable 2>/dev/null | grep -i security | wc -l)
    echo "可用更新: $UPDATES 个"
    echo "安全更新: $SECURITY_UPDATES 个"
elif command -v yum > /dev/null 2>&1; then
    echo "检测到RHEL/CentOS系统"
    UPDATES=$(yum check-update 2>/dev/null | wc -l)
    echo "可用更新: $UPDATES 个"
fi

echo ""
echo "[3] 安全建议:"
echo " - 高危补丁: 72小时内应用"
echo " - 中危补丁: 下一个维护窗口应用"
echo " - 低危补丁: 常规更新应用"
echo ""
echo "检查完成 - $(date)"
