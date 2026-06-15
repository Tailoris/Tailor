#!/bin/bash
exec 2>&1
echo "=== BaseEntity 分析 ==="

# 找 common 包
mkdir -p /tmp/common-classes
cd /tmp/common-classes
unzip -o /opt/tailor-is/jars/tailor-is-common-*.jar 'BOOT-INF/classes/com/tailoris/common/entity/*' 2>&1 | tail -3 || \
unzip -o /opt/tailor-is/jars/tailor-is-common-1.0.0.jar 'BOOT-INF/classes/com/tailoris/common/entity/*' 2>&1 | tail -3

ls BOOT-INF/classes/com/tailoris/common/entity/ 2>&1 | head -10

echo ""
echo "=== BaseEntity 字段 ==="
javap -p BOOT-INF/classes/com/tailoris/common/entity/BaseEntity.class 2>&1 | head -30
