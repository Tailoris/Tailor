#!/bin/bash
exec 2>&1
echo "=== Docker 方式归档 ==="

# 检查 docker 可用
echo "--- Docker 测试 ---"
docker ps 2>&1 | head -3
echo ""

# 使用 docker run 一个临时容器来归档日志
echo "--- 使用 Docker 容器归档 ---"
# 创建一个归档容器，挂载两个目录，然后执行 cp
docker run --rm \
    -v /tmp/tailor-is-logs:/src:ro \
    -v /tmp/tailor-is-pids:/src_pids:ro \
    -v /opt/tailor-is/logs:/dest \
    alpine:latest \
    sh -c "
        echo '开始归档...';
        cp -v /src/*.log /dest/ 2>&1;
        cp -v /src_pids/*.pid /dest/ 2>&1;
        ls -la /dest/ | head -20;
        echo '归档完成';
    " 2>&1

echo ""
echo "--- 验证归档结果 ---"
ls -la /opt/tailor-is/logs/ 2>&1 | head -20
echo ""
echo "/opt/tailor-is/logs/*.log 数量: $(ls -1 /opt/tailor-is/logs/*.log 2>/dev/null | wc -l)"

echo ""
echo "--- 验证文件大小匹配 ---"
ok=0; fail=0
for f in /tmp/tailor-is-logs/*.log; do
    n=$(basename $f)
    if [ -f /opt/tailor-is/logs/$n ]; then
        s1=$(stat -c%s "$f")
        s2=$(stat -c%s "/opt/tailor-is/logs/$n")
        if [ "$s1" = "$s2" ]; then
            ok=$((ok+1))
        else
            fail=$((fail+1))
            echo "  [DIFF] $n: src=$s1, dst=$s2"
        fi
    fi
done
echo "  匹配: $ok, 不匹配: $fail"

echo ""
echo "--- 清理源文件 ---"
if [ $fail -eq 0 ]; then
    rm -f /tmp/tailor-is-logs/*.log
    rm -f /tmp/tailor-is-pids/*.pid
    echo "[OK] 源文件已清理"
else
    echo "[SKIP] 存在不匹配，源文件未清理"
fi
