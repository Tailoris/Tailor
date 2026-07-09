#!/bin/bash
set -e

echo "=============================================="
echo " 文件编码检查脚本"
echo "=============================================="
echo ""

PROJECT_DIR="${PROJECT_DIR:-.}"
FAIL_ON_ERROR="${FAIL_ON_ERROR:-true}"

echo "检查目录: $PROJECT_DIR"
echo "失败时终止: $FAIL_ON_ERROR"
echo ""

non_utf8_files=()
bom_files=()

check_encoding() {
    local file="$1"
    
    if file -I "$file" | grep -q "charset=bom"; then
        bom_files+=("$file")
        return 1
    fi
    
    local encoding=$(file -I "$file" | grep -oE "charset=[^;]+")
    if [[ "$encoding" != "charset=utf-8" && "$encoding" != "charset=us-ascii" ]]; then
        non_utf8_files+=("$file ($encoding)")
        return 1
    fi
    
    return 0
}

echo "[1] 检查Java源文件编码"
echo "----------------------"
find "$PROJECT_DIR/tailor-is" -name "*.java" -type f | while read -r file; do
    check_encoding "$file"
done

echo "[2] 检查配置文件编码"
echo "-------------------"
find "$PROJECT_DIR/tailor-is" -name "*.yml" -o -name "*.yaml" -o -name "*.xml" -o -name "*.sql" | while read -r file; do
    check_encoding "$file"
done

echo "[3] 检查前端文件编码"
echo "-------------------"
find "$PROJECT_DIR/tailor-is-frontend" -name "*.ts" -o -name "*.tsx" -o -name "*.vue" -o -name "*.js" -o -name "*.json" | while read -r file; do
    check_encoding "$file"
done

echo "[4] 检查脚本文件编码"
echo "-------------------"
find "$PROJECT_DIR/deploy/scripts" -name "*.sh" | while read -r file; do
    check_encoding "$file"
done

echo ""
echo "[5] 检查结果汇总"
echo "----------------"

if [ ${#bom_files[@]} -gt 0 ]; then
    echo "✗ 发现 ${#bom_files[@]} 个带BOM的UTF-8文件:"
    for file in "${bom_files[@]}"; do
        echo "  - $file"
    done
    echo ""
fi

if [ ${#non_utf8_files[@]} -gt 0 ]; then
    echo "✗ 发现 ${#non_utf8_files[@]} 个非UTF-8编码文件:"
    for file in "${non_utf8_files[@]}"; do
        echo "  - $file"
    done
    echo ""
fi

if [ ${#bom_files[@]} -eq 0 ] && [ ${#non_utf8_files[@]} -eq 0 ]; then
    echo "✓ 所有文件编码均为UTF-8，无BOM"
    exit 0
else
    echo ""
    echo "建议修复命令:"
    echo "  # 移除BOM"
    echo "  sed -i '1s/^\xef\xbb\xbf//' <filename>"
    echo ""
    echo "  # 转换编码为UTF-8"
    echo "  iconv -f <source-encoding> -t utf-8 <input-file> -o <output-file>"
    
    if [ "$FAIL_ON_ERROR" = "true" ]; then
        exit 1
    fi
fi