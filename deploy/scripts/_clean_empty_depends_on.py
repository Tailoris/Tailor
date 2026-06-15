#!/usr/bin/env python3
"""Remove empty `depends_on:` blocks (header + only blank children)."""
import re

SRC = "/home/tailor/Tailoris/docker-compose.prod.yml"

with open(SRC, encoding="utf-8") as f:
    lines = f.readlines()

out = []
i = 0
while i < len(lines):
    line = lines[i]
    if re.match(r"^\s+depends_on:\s*$", line):
        # scan children
        j = i + 1
        while j < len(lines) and (re.match(r"^\s{4,}", lines[j]) or lines[j].strip() == ""):
            j += 1
        has_content = any(l.strip() for l in lines[i + 1:j])
        if not has_content:
            # drop empty block
            i = j
            continue
    out.append(line)
    i += 1

with open(SRC, "w", encoding="utf-8") as f:
    f.writelines(out)

# report
print(f"[done] {len(out)} lines written, {len(lines) - len(out)} removed")
print("remaining depends_on:")
for ln in out:
    if "depends_on" in ln:
        print("  ", ln.rstrip())
