#!/usr/bin/env python3
"""
批量替换错误密码:
  - redis_Y658iD  → redis_RSeR4G
  - mysql_2HjCZj  → mysql_CA75Yk

作用范围: tailor-is/ 目录下的 .sh, .md, .yml 文件
"""
import os
import re
import sys

REPLACEMENTS = [
    ('redis_Y658iD', 'redis_RSeR4G'),
    ('mysql_2HjCZj', 'mysql_CA75Yk'),
]

EXCLUDE_DIRS = {
    'target', 'node_modules', '.git', '.idea', 'logs',
    'backup', 'data', 'dist', 'build', '.mvn',
}

# 文件扩展名白名单
EXTS = {'.sh', '.yml', '.yaml', '.md', '.txt', '.env', '.properties'}


def should_process(filepath: str) -> bool:
    parts = filepath.replace('\\', '/').split('/')
    for p in parts:
        if p in EXCLUDE_DIRS:
            return False
    ext = os.path.splitext(filepath)[1].lower()
    return ext in EXTS


def main():
    root = sys.argv[1] if len(sys.argv) > 1 else 'tailor-is'
    print(f'扫描根目录: {root}\n')

    total_files = 0
    total_replacements = 0

    for dirpath, dirnames, filenames in os.walk(root):
        # 过滤排除目录
        dirnames[:] = [d for d in dirnames if d not in EXCLUDE_DIRS]
        for fname in filenames:
            fpath = os.path.join(dirpath, fname)
            if not should_process(fpath):
                continue
            try:
                with open(fpath, 'r', encoding='utf-8') as f:
                    content = f.read()
            except (UnicodeDecodeError, OSError):
                continue

            original = content
            file_replacements = 0
            for old, new in REPLACEMENTS:
                count = content.count(old)
                if count:
                    content = content.replace(old, new)
                    file_replacements += count

            if file_replacements > 0:
                with open(fpath, 'w', encoding='utf-8') as f:
                    f.write(content)
                print(f'  [FIX] {fpath}: {file_replacements} 处替换')
                total_files += 1
                total_replacements += file_replacements

    print(f'\n========================================')
    print(f'  总计: {total_files} 个文件, {total_replacements} 处替换')
    print(f'========================================')


if __name__ == '__main__':
    main()
