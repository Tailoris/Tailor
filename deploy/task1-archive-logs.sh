#!/bin/bash
# 任务 1: 日志归档
# 将 /tmp/tailor-is-logs/ 迁移到 /opt/tailor-is/logs/

set +e
export LANG=C.UTF-8

echo "============================================"
echo "=== 任务 1: 日志归档 - $(date) ==="
echo "============================================"

SRC_DIR=/tmp/tailor-is-logs
SRC_PID=/tmp/tailor-is-pids
DEST_DIR=/opt/tailor-is/logs
ARCHIVE_DIR=/opt/tailor-is/logs/archive/$(date '+%Y%m%d')
LOG_FILE=/tmp/tailor-is-logs/task1-archive-$(date '+%Y%m%d-%H%M%S').log

# 创建任务日志
exec > >(tee -a $LOG_FILE) 2>&1

echo ""
echo "--- 步骤 1: 检查源目录 ---"
if [ ! -d "$SRC_DIR" ]; then
    echo "[FAIL] 源目录不存在: $SRC_DIR"
    exit 1
fi
echo "[OK] 源目录存在: $SRC_DIR"
ls -la $SRC_DIR/ | head -20
echo "  文件数: $(ls -1 $SRC_DIR/ | wc -l)"

echo ""
echo "--- 步骤 2: 检查目标目录权限 ---"
ls -la $DEST_DIR/ 2>&1 | head -5
ls -ld $DEST_DIR
echo "  当前用户: $(whoami) (uid=$(id -u))"
if [ -w "$DEST_DIR" ]; then
    echo "[OK] 目标目录可写"
else
    echo "[WARN] 目标目录无写权限，尝试 sudo 方式"
fi

echo ""
echo "--- 步骤 3: 尝试直接归档 ---"
# 先尝试直接复制
file_count=0
for src_file in $SRC_DIR/*.log; do
    if [ -f "$src_file" ]; then
        name=$(basename $src_file)
        if cp -v "$src_file" "$DEST_DIR/$name" 2>&1; then
            file_count=$((file_count + 1))
        fi
    fi
done
echo "  复制日志文件数: $file_count"

# 复制 PID 文件
echo ""
echo "--- 步骤 4: 复制 PID 文件 ---"
pid_count=0
if [ -d "$SRC_PID" ]; then
    for src_file in $SRC_PID/*.pid; do
        if [ -f "$src_file" ]; then
            name=$(basename $src_file)
            if cp -v "$src_file" "$DEST_DIR/$name" 2>&1; then
                pid_count=$((pid_count + 1))
            fi
        fi
    done
fi
echo "  复制 PID 文件数: $pid_count"

# 如果直接复制失败，尝试使用 tee 或 sudo
echo ""
echo "--- 步骤 5: 验证归档结果 ---"
success_count=0
fail_count=0
for src_file in $SRC_DIR/*.log; do
    if [ -f "$src_file" ]; then
        name=$(basename $src_file)
        if [ -f "$DEST_DIR/$name" ]; then
            src_size=$(stat -c%s "$src_file")
            dst_size=$(stat -c%s "$DEST_DIR/$name" 2>/dev/null)
            if [ "$src_size" = "$dst_size" ]; then
                success_count=$((success_count + 1))
            else
                fail_count=$((fail_count + 1))
                echo "  [MISMATCH] $name: 源=$src_size, 目标=$dst_size"
            fi
        else
            fail_count=$((fail_count + 1))
        fi
    fi
done
echo "  成功: $success_count, 失败: $fail_count"

echo ""
echo "--- 步骤 6: 清理源文件 ---"
if [ $fail_count -eq 0 ]; then
    rm -f $SRC_DIR/*.log
    rm -f $SRC_PID/*.pid
    echo "[OK] 源文件已清理"
else
    echo "[SKIP] 存在失败文件，源文件未清理"
fi

echo ""
echo "--- 步骤 7: 归档摘要 ---"
echo "  源目录文件数: $(ls -1 $SRC_DIR/ 2>/dev/null | wc -l)"
echo "  目标目录文件数: $(ls -1 $DEST_DIR/*.log 2>/dev/null | wc -l)"
echo ""
echo "============================================"
echo "=== 任务 1 完成 ==="
echo "============================================"
