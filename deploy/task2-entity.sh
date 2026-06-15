#!/bin/bash
exec 2>&1
echo "=== AI 实体类分析 ==="

# 解压 AI 服务的 entity 类
mkdir -p /tmp/ai-classes
cd /tmp/ai-classes
unzip -o /opt/tailor-is/jars/tailor-is-ai-1.0.0.jar 'BOOT-INF/classes/com/tailoris/ai/entity/*' 2>&1 | tail -5
echo ""
echo "--- Entity 类列表 ---"
ls BOOT-INF/classes/com/tailoris/ai/entity/ 2>&1

echo ""
echo "--- Mapper XML 文件 ---"
unzip -o /opt/tailor-is/jars/tailor-is-ai-1.0.0.jar 'BOOT-INF/classes/mapper/**' 2>&1 | tail -5
find /tmp/ai-classes -name "*.xml" 2>&1 | head -10

echo ""
echo "--- 通过 javap 反编译 Entity ---"
cd /tmp/ai-classes
for f in BOOT-INF/classes/com/tailoris/ai/entity/*.class; do
    if [ -f "$f" ]; then
        name=$(basename "$f" .class)
        echo ""
        echo "=== $name ==="
        javap -p "$f" 2>&1 | grep -E "private|public" | head -25
    fi
done
