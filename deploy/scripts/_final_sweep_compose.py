#!/usr/bin/env python3
"""Final sweep: drop empty depends_on blocks and rewrite host env values."""
from __future__ import annotations

import re

SRC = "/home/tailor/Tailoris/docker-compose.prod.yml"

with open(SRC, encoding="utf-8") as f:
    text = f.read()

# 1. Text-level: replace remaining host references in env values
HOST_REPLACEMENTS = [
    ("SPRING_DATA_REDIS_HOST: redis\n", "SPRING_DATA_REDIS_HOST: 127.0.0.1\n"),
    ("REDIS_HOST: redis\n", "REDIS_HOST: 127.0.0.1\n"),
    ("SPRING_RABBITMQ_HOST: rabbitmq\n", "SPRING_RABBITMQ_HOST: 127.0.0.1\n"),
    ("SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR: nacos:8848",
     "SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR: 127.0.0.1:8848"),
    ("SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR: nacos:8848",
     "SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR: 127.0.0.1:8848"),
    ("jdbc:mysql://mysql:3306/", "jdbc:mysql://127.0.0.1:3306/"),
]
for old, new in HOST_REPLACEMENTS:
    if old in text:
        n = text.count(old)
        text = text.replace(old, new)
        print(f"[replace] '{old.strip()}' -> '{new.strip()}'  x{n}")

# 2. Drop empty depends_on: lines (indented "depends_on:" with no non-blank children)
lines = text.splitlines(keepends=True)
out = []
i = 0
while i < len(lines):
    line = lines[i]
    if re.match(r"^[ \t]+depends_on:\s*$", line):
        # peek: any non-blank indented children before next sibling (same or lower indent)?
        base_indent = len(line) - len(line.lstrip(" \t"))
        j = i + 1
        while j < len(lines):
            nxt = lines[j]
            if nxt.strip() == "":
                j += 1
                continue
            indent = len(nxt) - len(nxt.lstrip(" \t"))
            if indent > base_indent:
                # child
                j += 1
                continue
            break
        children = [c for c in lines[i + 1:j] if c.strip()]
        if not children:
            print(f"[drop] empty depends_on at line ~{i + 1}")
            i = j
            continue
    out.append(line)
    i += 1

with open(SRC, "w", encoding="utf-8") as f:
    f.writelines(out)

# report remaining depends_on
print("\n[final] depends_on lines:")
for ln in out:
    if "depends_on" in ln:
        print("  ", ln.rstrip())

print("\n[final] any 'SPRING_DATA_REDIS_HOST: redis' or similar?")
for pattern in ["REDIS_HOST: redis", "SPRING_RABBITMQ_HOST: rabbitmq", "mysql:3306", "nacos:8848"]:
    count = "".join(out).count(pattern)
    print(f"  {pattern}: {count}")
