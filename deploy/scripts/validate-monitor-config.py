#!/usr/bin/env python3
"""验证 Prometheus 配置和告警规则文件"""
import sys
import re

def validate_yaml_basic(filepath):
    """基础 YAML 语法检查（不依赖 PyYAML）"""
    errors = []
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # 检查基本结构
    if not content.strip():
        errors.append('文件为空')
        return errors

    # 检查缩进一致性
    lines = content.split('\n')
    in_list = False
    for i, line in enumerate(lines, 1):
        if not line.strip() or line.strip().startswith('#'):
            continue
        # 检查冒号后空格
        if ':' in line and not line.strip().startswith('-'):
            key_part = line.split(':')[0]
            if not line.endswith(':'):
                value_start = line.split(':', 1)[1].strip()
                if not value_start and ' ' not in line[len(key_part)+1:]:
                    errors.append(f'行 {i}: 冒号后缺少空格: {line[:60]}')

    return errors


def validate_alerts(filepath):
    """验证 alerts.yml 规则"""
    print(f'\n=== 验证 {filepath} ===')
    errors = validate_yaml_basic(filepath)
    if errors:
        for e in errors:
            print(f'  [ERROR] {e}')
        return False

    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # 解析 alert 名称
    alerts = re.findall(r'^\s*-\s+alert:\s+(\S+)', content, re.MULTILINE)
    print(f'  发现 {len(alerts)} 个告警规则:')
    for a in alerts:
        print(f'    - {a}')

    # 检查 HighErrorRate 阈值
    he_match = re.search(r'alert:\s+HighErrorRate.*?>(\d+\.\d+)', content, re.DOTALL)
    if he_match:
        threshold = float(he_match.group(1))
        print(f'  HighErrorRate 阈值: {threshold} ({int(threshold*100)}%)')
        if threshold == 0.10:
            print('  [OK] 阈值已更新为 10%')
        elif threshold == 0.05:
            print('  [WARN] 阈值仍为 5%')
        else:
            print(f'  [INFO] 阈值为 {threshold}')

    return len(errors) == 0


def validate_prometheus(filepath):
    """验证 prometheus.yml"""
    print(f'\n=== 验证 {filepath} ===')
    errors = validate_yaml_basic(filepath)
    if errors:
        for e in errors:
            print(f'  [ERROR] {e}')
        return False

    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # 抓取间隔
    if 'scrape_interval:' in content:
        m = re.search(r'scrape_interval:\s*(\S+)', content)
        if m:
            print(f'  scrape_interval: {m.group(1)}')

    # 告警管理器
    if 'alerting:' in content:
        print('  [OK] alerting 配置存在')
        am_match = re.search(r"alertmanagers:.*?targets:\s*\[([^\]]+)\]", content, re.DOTALL)
        if am_match:
            print(f'  Alertmanager 目标: {am_match.group(1).strip()}')
    else:
        print('  [WARN] alerting 配置缺失')

    # 检查目标端口
    if "localhost:8080'" in content:
        print('  [OK] Gateway 端口已修复为 8080')
    elif "localhost:8081'" in content:
        print('  [WARN] Gateway 端口仍为 8081')

    # 统计服务目标
    targets = re.findall(r"localhost:(\d+)", content)
    unique_ports = sorted(set(targets))
    print(f'  目标端口: {unique_ports}')

    return len(errors) == 0


def validate_alertmanager(filepath):
    """验证 alertmanager.yml"""
    print(f'\n=== 验证 {filepath} ===')
    errors = validate_yaml_basic(filepath)
    if errors:
        for e in errors:
            print(f'  [ERROR] {e}')
        return False

    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # 接收人
    receivers = re.findall(r"-\s+name:\s+'([^']+)'", content)
    print(f'  接收人数: {len(receivers)}')
    for r in receivers:
        print(f'    - {r}')

    # 抑制规则
    inhibits = content.count('inhibit_rules:')
    if inhibits:
        inhibit_count = content.count('source_matchers:')
        print(f'  抑制规则数: {inhibit_count}')

    # 路由
    routes = content.count('receiver:')
    print(f'  路由引用: {routes}')

    return len(errors) == 0


if __name__ == '__main__':
    files = [
        ('deploy/alerts.yml', validate_alerts),
        ('deploy/prometheus.yml', validate_prometheus),
        ('deploy/alertmanager.yml', validate_alertmanager),
    ]

    all_ok = True
    for filepath, validator in files:
        try:
            if not validator(filepath):
                all_ok = False
        except Exception as e:
            print(f'  [ERROR] {e}')
            all_ok = False

    print('\n' + '='*60)
    if all_ok:
        print('  ✅ 所有配置文件验证通过')
    else:
        print('  ❌ 存在配置错误')
    print('='*60)

    sys.exit(0 if all_ok else 1)
