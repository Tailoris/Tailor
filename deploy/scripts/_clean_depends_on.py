#!/usr/bin/env python3
"""
Clean orphan `depends_on` entries referencing dropped services (mysql/redis/
rabbitmq/nacos). Handles both multi-line form:
    depends_on:
      mysql: { condition: service_healthy }
      redis: { condition: service_healthy }
and inline form:
    depends_on: { nacos: { condition: ... }, mysql: ... }

Also removes empty depends_on blocks left after cleaning.
"""
from __future__ import annotations

import re
import sys

SRC = "/home/tailor/Tailoris/docker-compose.prod.yml"
DROPPED = {"mysql", "redis", "rabbitmq", "nacos"}


def clean_depends_on_lines(lines: list[str]) -> list[str]:
    out: list[str] = []
    i = 0
    n = len(lines)
    while i < n:
        line = lines[i]
        stripped = line.strip()
        if stripped == "depends_on:":
            # multi-line block: collect subsequent children (4+ spaces / dash-space)
            j = i + 1
            while j < n and (lines[j].startswith("    ") or lines[j].strip() == ""):
                j += 1
            block = lines[i + 1:j]
            kept = []
            for child in block:
                s = child.strip()
                if not s:
                    kept.append(child)
                    continue
                # child form: "  mysql: { condition: service_healthy }"
                # or: "  - mysql"
                # extract identifier: first token before ':' or '-'
                token = None
                if s.startswith("-"):
                    parts = s.split(None, 1)
                    if len(parts) > 1:
                        token = parts[1].strip().rstrip(":")
                else:
                    token = s.split(":", 1)[0]
                if token and token.lower() in DROPPED:
                    continue
                kept.append(child)
            # if no kept children, drop the whole block
            if any(k.strip() for k in kept):
                out.append(line)
                out.extend(kept)
            # else: drop entirely
            i = j
            continue
        # Inline form: depends_on: { nacos: { ... }, mysql: { ... }, ... }
        m_inline = re.match(r"^(\s*depends_on:\s*)\{(.*)\}\s*$", line)
        if m_inline and any(s in line.lower() for s in DROPPED):
            content = m_inline.group(2)
            # split by ", " but need to respect nested braces
            parts = split_top_level_commas(content)
            kept = []
            for p in parts:
                ident = p.split(":", 1)[0].strip()
                if ident.lower() in DROPPED:
                    continue
                kept.append(p)
            if kept:
                out.append(f"{m_inline.group(1)}{{{', '.join(kept)}}}\n")
            # else: drop the line
            i += 1
            continue
        out.append(line)
        i += 1
    return out


def split_top_level_commas(s: str) -> list[str]:
    depth = 0
    start = 0
    out: list[str] = []
    for i, ch in enumerate(s):
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
        elif ch == "," and depth == 0:
            out.append(s[start:i])
            start = i + 1
    out.append(s[start:])
    return out


def main():
    with open(SRC, encoding="utf-8") as f:
        text = f.read()
    lines = text.splitlines(keepends=True)
    new_lines = clean_depends_on_lines(lines)
    new_text = "".join(new_lines)
    # collapse triple+ newlines
    new_text = re.sub(r"\n{3,}", "\n\n", new_text)
    with open(SRC, "w", encoding="utf-8") as f:
        f.write(new_text)

    # Report what we still see
    leftover = re.findall(r"depends_on:.*", new_text)
    print(f"[clean] remaining depends_on lines: {len(leftover)}")
    for l in leftover[:20]:
        print(" ", l.rstrip())


if __name__ == "__main__":
    main()
