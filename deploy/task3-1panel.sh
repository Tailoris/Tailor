#!/bin/bash
exec 2>&1
echo "=== 1Panel Nacos 配置检查 ==="

# 1Panel 应用配置位置
echo "--- 1Panel Nacos 应用配置 ---"
find /opt/1panel -name "*.yml" -o -name "*.yaml" -o -name "*.json" 2>/dev/null | xargs grep -l "nacos" 2>/dev/null | head -10

echo ""
ls -la /opt/1panel/apps/nacos/ 2>&1 | head -10

# 查找 1Panel 应用数据
echo ""
find / -name "nacos*" -type d 2>/dev/null | head -20
