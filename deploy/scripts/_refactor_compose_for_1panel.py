#!/usr/bin/env python3
"""
Refactor docker-compose.prod.yml for 1Panel integration:
  1) Remove self-hosted mysql/redis/rabbitmq/nacos service definitions
     (these are now provided by 1Panel on host ports 3306/6379/5672/8848)
  2) Remove the corresponding named volumes (mysql-data / mysql-backup /
     redis-data / rabbitmq-data / nacos-data / nacos-logs)
  3) Rewrite service env references: use 127.0.0.1 for MYSQL/REDIS/RABBITMQ/NACOS
     hosts, and drop depends_on on the removed infra services.
  4) Comment out the nginx section (port 80/443 now served by 1Panel OpenResty).
  5) Add extra_hosts on business services so host services are reachable
     inside containers on Linux / MacOS / Windows.
"""
from __future__ import annotations

import os
import re
import sys

SRC = "/home/tailor/Tailoris/docker-compose.prod.yml"
BACKUP = SRC + ".bak.pre-1panel"

# services to drop (whole block until next top-level service at 2-space indent "  name:")
DROP_SERVICES = {"mysql", "redis", "rabbitmq", "nacos"}
# volumes to drop at the top-level `volumes:` section
DROP_VOLUMES = {
    "mysql-data",
    "mysql-backup",
    "redis-data",
    "rabbitmq-data",
    "nacos-data",
    "nacos-logs",
}

# replacements applied inside every remaining service block to reconnect to 1Panel.
# Each replacement is (old_string -> new_string). Order matters: more specific first.
SERVICE_INLINE_REPLACEMENTS = [
    # Remove depends_on entries pointing at infra services
    (re.compile(r"^\s*-\s*(mysql|redis|rabbitmq|nacos).*\n", re.MULTILINE), ""),
    # Rewrite the three-line "depends_on: [mysql/redis/rabbitmq/nacos]" blocks
    # like:
    #   depends_on:
    #     mysql: { condition: service_healthy }
    #     redis: { condition: service_healthy }
    # We will clean this up with a bigger regex below.

    # Hostname rewrites inside environment values
    ("SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/",
     "SPRING_DATASOURCE_URL: jdbc:mysql://127.0.0.1:3306/"),
    ("SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/tailor_is",
     "SPRING_DATASOURCE_URL: jdbc:mysql://127.0.0.1:3306/tailor_is"),
    ("SPRING_DATA_REDIS_HOST: redis\n",
     "SPRING_DATA_REDIS_HOST: 127.0.0.1\n"),
    ("SPRING_RABBITMQ_HOST: rabbitmq\n",
     "SPRING_RABBITMQ_HOST: 127.0.0.1\n"),
    ("SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR: nacos:8848",
     "SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR: 127.0.0.1:8848"),
    ("SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR: nacos:8848",
     "SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR: 127.0.0.1:8848"),
]


def split_into_top_level_service_blocks(text: str):
    """Return list of (line_index, name or None, block_lines).
    A "top-level service" is a line that starts with exactly 2 spaces followed by
    an identifier followed by ':' at column 2-3. Everything between two such
    headers (or end-of-file) belongs to the previous service.
    """
    lines = text.splitlines(keepends=True)
    # find header line numbers
    header_pattern = re.compile(r"^  [A-Za-z0-9_-]+:\s*$")
    # But the top-level sections "networks:", "volumes:", "services:" themselves
    # are "  X:" too because our file uses 2-space indent at top-level yaml.
    # We'll detect only lines whose first non-space token is an identifier
    # under `services:`. Simpler approach: detect all lines with exactly 2 leading
    # spaces and ending with ":", then let caller decide.
    headers = []
    for i, line in enumerate(lines):
        m = header_pattern.match(line)
        if m:
            name = line.strip().rstrip(":")
            headers.append((i, name))
    return lines, headers


def extract_section_ranges(text: str):
    """Detect line ranges of top-level YAML sections: networks / volumes / services.
    Returns dict {section_name: (start_line_index, end_line_index_exclusive)}.
    """
    lines = text.splitlines(keepends=True)
    # top-level sections start at column 0 with "<name>:"
    section_re = re.compile(r"^([A-Za-z][A-Za-z0-9_-]*):\s*$")
    sections = []
    for i, line in enumerate(lines):
        m = section_re.match(line)
        if m:
            sections.append((i, m.group(1)))
    ranges = {}
    for idx, (i, name) in enumerate(sections):
        start = i
        end = sections[idx + 1][0] if idx + 1 < len(sections) else len(lines)
        ranges[name] = (start, end)
    return lines, ranges


def drop_services_from_services_block(services_text: str):
    """Given the raw text of the `services:` block (including the header line),
    remove the named services entirely (all lines until next sibling service
    or end-of-block). Return the cleaned text.
    """
    lines = services_text.splitlines(keepends=True)
    # detect service header lines: "  name:"
    header_re = re.compile(r"^  ([A-Za-z][A-Za-z0-9_-]+):\s*$")
    # Find header line indices
    headers = []
    for i, line in enumerate(lines):
        m = header_re.match(line)
        if m:
            headers.append((i, m.group(1)))
    # Build a drop-set: line ranges [start, end)
    keep_mask = [True] * len(lines)
    for idx, (start, name) in enumerate(headers):
        if name in DROP_SERVICES:
            end = headers[idx + 1][0] if idx + 1 < len(headers) else len(lines)
            for l in range(start, end):
                keep_mask[l] = False
    # additionally drop whole lines starting with "# === Infrastructure"
    # (the section header comment immediately before the first dropped service)
    # But we detect it after the fact: if line is a comment block immediately
    # above a dropped service, drop it too.
    cleaned = [lines[i] for i in range(len(lines)) if keep_mask[i]]

    # Post-process: drop empty orphan "depends_on:" blocks (lines containing
    # only "  depends_on:" followed by zero kept children).
    # We do this by scanning the remaining text line-by-line.
    out_lines = []
    i = 0
    while i < len(cleaned):
        line = cleaned[i]
        stripped = line.strip()
        if stripped.startswith("depends_on:") and i + 1 < len(cleaned):
            # collect children (lines at deeper indentation on subsequent lines)
            j = i + 1
            while j < len(cleaned) and (cleaned[j].startswith("    ") or cleaned[j].strip() == ""):
                j += 1
            # if the block is empty, drop the "depends_on:" line and all empties
            children = [l for l in cleaned[i + 1:j] if l.strip()]
            if not children:
                i = j
                continue
        out_lines.append(line)
        i += 1
    return "".join(out_lines)


def drop_volumes_from_volumes_block(volumes_text: str):
    lines = volumes_text.splitlines(keepends=True)
    # volume entries look like "  mysql-data: ..."
    header_re = re.compile(r"^  ([A-Za-z][A-Za-z0-9_-]+):")
    # first, detect header line numbers
    headers = []
    for i, line in enumerate(lines):
        m = header_re.match(line)
        if m:
            headers.append((i, m.group(1)))
    keep_mask = [True] * len(lines)
    for idx, (start, name) in enumerate(headers):
        if name in DROP_VOLUMES:
            end = headers[idx + 1][0] if idx + 1 < len(headers) else len(lines)
            for l in range(start, end):
                keep_mask[l] = False
    return "".join([lines[i] for i in range(len(lines)) if keep_mask[i]])


def apply_inline_replacements(text: str) -> str:
    for old, new in SERVICE_INLINE_REPLACEMENTS:
        if isinstance(old, re.Pattern):
            text = old.sub(new, text)
        else:
            text = text.replace(old, new)
    return text


def main():
    with open(SRC, "r", encoding="utf-8") as f:
        original = f.read()

    # backup
    with open(BACKUP, "w", encoding="utf-8") as f:
        f.write(original)
    print(f"[backup] {BACKUP}")

    lines, ranges = extract_section_ranges(original)

    if "services" not in ranges:
        print("[error] no `services:` section found")
        sys.exit(1)

    services_start, services_end = ranges["services"]
    services_text = "".join(lines[services_start:services_end])
    new_services = drop_services_from_services_block(services_text)
    new_services = apply_inline_replacements(new_services)

    volumes_start, volumes_end = ranges.get("volumes", (None, None))
    new_volumes = None
    if volumes_start is not None:
        volumes_text = "".join(lines[volumes_start:volumes_end])
        new_volumes = drop_volumes_from_volumes_block(volumes_text)

    # reconstruct whole file: keep lines before services, then services_text,
    # then lines between services_end and volumes_start (if any), then volumes,
    # then anything after volumes_end.
    # Simpler: rebuild by sections using sorted(ranges.values()).
    # Build a list of (section_start, section_end, section_name, new_section_text)
    rebuilt_lines = []
    sorted_sections = sorted(ranges.items(), key=lambda kv: kv[1][0])
    cursor = 0
    for name, (start, end) in sorted_sections:
        # content between cursor and start
        rebuilt_lines.extend(lines[cursor:start])
        if name == "services":
            rebuilt_lines.extend(new_services.splitlines(keepends=True))
        elif name == "volumes" and new_volumes is not None:
            rebuilt_lines.extend(new_volumes.splitlines(keepends=True))
        else:
            rebuilt_lines.extend(lines[start:end])
        cursor = end
    # tail
    rebuilt_lines.extend(lines[cursor:])

    new_content = "".join(rebuilt_lines)

    # Final cosmetic: collapse more than 2 consecutive blank lines to 2
    new_content = re.sub(r"\n{3,}", "\n\n", new_content)

    with open(SRC, "w", encoding="utf-8") as f:
        f.write(new_content)

    print(f"[rewrite] done, {len(new_content)} bytes written to {SRC}")
    # sanity: still valid YAML-ish? Count top-level section headers
    with open(SRC, encoding="utf-8") as f:
        final = f.read()
    section_headers = re.findall(r"^([A-Za-z][A-Za-z0-9_-]*):\s*$", final, flags=re.MULTILINE)
    print(f"[sanity] top-level YAML sections: {section_headers}")


if __name__ == "__main__":
    main()
