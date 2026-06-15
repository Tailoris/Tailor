#!/usr/bin/env python3
"""检查 Prometheus 配置和告警规则状态"""
import json
import os
import socket
import urllib.request

# 禁用代理
os.environ['NO_PROXY'] = '*'

# 使用原始 socket 绕过 urllib 的代理设置
PROM_URL = 'http://localhost:9090'
PROM_HOST = 'localhost'
PROM_PORT = 9090


def http_get_raw(path):
    """使用 socket 直接 HTTP 请求，绕过代理"""
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.settimeout(5)
    s.connect((PROM_HOST, PROM_PORT))
    request = f'GET {path} HTTP/1.1\r\nHost: {PROM_HOST}:{PROM_PORT}\r\nConnection: close\r\n\r\n'
    s.sendall(request.encode())
    chunks = []
    while True:
        data = s.recv(4096)
        if not data:
            break
        chunks.append(data)
    s.close()
    raw = b''.join(chunks).decode('utf-8', errors='ignore')
    # 分离 header 和 body
    if '\r\n\r\n' in raw:
        body = raw.split('\r\n\r\n', 1)[1]
    else:
        body = raw
    # 处理 chunked transfer encoding
    if body.startswith('23c1\r\n'):
        # 简单的 chunked 解码
        lines = body.split('\r\n')
        decoded = []
        i = 1
        while i < len(lines):
            if lines[i] == '':
                i += 1
                continue
            # 检查是否是 chunk size
            try:
                size = int(lines[i], 16)
                if size == 0:
                    break
                decoded.append(lines[i + 1] if i + 1 < len(lines) else '')
                i += 2
            except ValueError:
                i += 1
        body = '\n'.join(decoded)
    try:
        return json.loads(body)
    except json.JSONDecodeError:
        return {'raw': body[:500]}


def main():
    print('=' * 60)
    print('  Prometheus 配置状态检查')
    print('=' * 60)

    # 1. Active targets
    print('\n=== 1. Active Targets 抓取状态 ===')
    targets = http_get_raw('/api/v1/targets')
    if 'data' in targets:
        active = targets['data'].get('activeTargets', [])
        up = sum(1 for t in active if t['health'] == 'up')
        down = len(active) - up
        print(f'  总计: {len(active)} | UP: {up} | DOWN: {down}')
        for t in active:
            svc = t['labels'].get('service', t['labels'].get('job', 'unknown'))
            status = '[UP]  ' if t['health'] == 'up' else '[DOWN]'
            err = t.get('lastError', '')[:60] if t['health'] != 'up' else ''
            print(f'  {status} {svc:30s} {err}')
    else:
        print(f'  [ERROR] {targets}')

    # 2. Rules
    print('\n=== 2. 告警规则 ===')
    rules = http_get_raw('/api/v1/rules')
    if 'data' in rules:
        for group in rules['data'].get('groups', []):
            print(f'  组 [{group["name"]}]:')
            for rule in group.get('rules', []):
                name = rule.get('name', 'unknown')
                state = rule.get('state', 'inactive')
                health = rule.get('health', 'ok')
                print(f'    - {name:25s} state={state:8s} health={health}')
    else:
        print(f'  [ERROR] {rules}')

    # 3. Active alerts
    print('\n=== 3. 活跃告警 ===')
    alerts = http_get_raw('/api/v1/alerts')
    if 'data' in alerts:
        active = alerts['data'].get('alerts', [])
        firing = sum(1 for a in active if a['state'] == 'firing')
        pending = sum(1 for a in active if a['state'] == 'pending')
        print(f'  Firing: {firing} | Pending: {pending} | Inactive: {len(active) - firing - pending}')
        for a in active:
            name = a['labels'].get('alertname', 'unknown')
            svc = a['labels'].get('service', 'unknown')
            state = a['state']
            val = a.get('value', 'N/A')
            print(f'    [{state:8s}] {name:20s} service={svc:25s} value={val}')
    else:
        print(f'  [ERROR] {alerts}')

    # 4. 告警管理器
    print('\n=== 4. 告警管理器 ===')
    am_status = http_get_raw('/api/v1/alertmanagers')
    if 'data' in am_status:
        ams = am_status['data'].get('activeAlertmanagers', [])
        for am in ams:
            print(f'  - {am.get("url", "?")}')
        if not ams:
            print('  [WARN] 无活跃 Alertmanager')
    else:
        print(f'  [ERROR] {am_status}')

    # 5. 配置状态
    print('\n=== 5. 配置状态 ===')
    status = http_get_raw('/api/v1/status/config')
    if 'data' in status:
        cfg = status['data']
        yaml = cfg.get('yaml', '')
        # 检查关键内容
        checks = [
            ('alerting 配置', 'alerting:' in yaml),
            ('Alertmanager 9093', "localhost:9093" in yaml),
            ('Gateway 8080', "localhost:8080" in yaml),
            ('alerts.yml 加载', 'alerts.yml' in yaml),
        ]
        for name, ok in checks:
            mark = '[OK]  ' if ok else '[WARN]'
            print(f'  {mark} {name}')
    else:
        print(f'  [ERROR] {status}')

    print('\n' + '=' * 60)


if __name__ == '__main__':
    main()
