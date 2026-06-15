#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Alertmanager Webhook 端到端告警演练脚本
----------------------------------------
功能:
  1) 在后台启动 alert-webhook 中继服务 (localhost:8080)
  2) 构造符合 Alertmanager 标准格式的模拟告警 JSON
  3) 依次 POST 到多个路由路径（default / critical / warning / business / service-down）
  4) 打印响应状态码与响应体（便于人工核对钉钉/飞书是否收到通知）

使用:
  python3 deploy/scripts/run-alert-drill.py [--port 8080] [--to-email your@email.com]

注意:
  - 请先在环境变量中设置 Webhook / API Key（见 deploy/alert-webhook/server.py 顶部）
  - 若仅需测试服务逻辑而不真正发通知，可临时将 DINGTALK_WEBHOOK 等留空
"""

import json
import os
import subprocess
import sys
import time
import urllib.request
import urllib.error
import signal
from datetime import datetime, timezone

# ---------- 路径 ----------
PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WEBHOOK_PY = os.path.join(PROJECT_ROOT, 'alert-webhook', 'server.py')

# ---------- Alertmanager 标准 Webhook Payload 构造 ----------
def build_alert(alertname, severity, service, port, summary, description, status='firing', category=None):
    now = datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%S.000Z')
    labels = {
        'alertname': alertname,
        'severity': severity,
        'service': service,
        'port': str(port),
        'env': 'production',
    }
    if category:
        labels['category'] = category

    return {
        'receiver': f'{severity}-receiver',
        'status': status,
        'alerts': [
            {
                'status': status,
                'labels': labels,
                'annotations': {'summary': summary, 'description': description},
                'startsAt': now,
                'endsAt': '0001-01-01T00:00:00Z',
                'generatorURL': f'http://prometheus:9090/graph?g0.expr={alertname}',
                'fingerprint': f'{alertname}_{service}_{int(time.time())}',
            }
        ],
        'groupLabels': {'alertname': alertname, 'service': service},
        'commonLabels': labels,
        'commonAnnotations': {'summary': summary, 'description': description},
        'externalURL': 'http://alertmanager:9093',
    }


# ---------- 演练用例 ----------
DRILL_CASES = [
    {
        'path': '/api/v1/alerts/default',
        'name': '[默认兜底] 未知分类告警',
        'payload': build_alert(
            'UnknownSignalSpike',
            'warning',
            'tailor-is-gateway',
            8080,
            '网关出现未知分类信号峰值',
            'tailor-is-gateway 收到大量未归类请求，需核查路由配置。',
        ),
    },
    {
        'path': '/api/v1/alerts/critical',
        'name': '[严重] 高错误率告警',
        'payload': build_alert(
            'HighErrorRate',
            'critical',
            'tailor-is-user',
            8101,
            'tailor-is-user 错误率超过 10%',
            '用户服务近 5 分钟 5xx 错误率达到 12.3%，可能导致登录失败。',
        ),
    },
    {
        'path': '/api/v1/alerts/service-down',
        'name': '[严重] 服务下线告警',
        'payload': build_alert(
            'ServiceDown',
            'critical',
            'tailor-is-payment',
            8105,
            'tailor-is-payment 支付服务已下线',
            '支付服务在端口 8105 连续 2 分钟不可达，支付能力可能受损。',
        ),
    },
    {
        'path': '/api/v1/alerts/warning',
        'name': '[警告] JVM 堆内存使用率偏高',
        'payload': build_alert(
            'HighJVMMemory',
            'warning',
            'tailor-is-product',
            8103,
            'tailor-is-product JVM 堆内存 > 85%',
            '商品服务 JVM 堆内存使用率持续超过 85%，请注意 GC 压力。',
        ),
    },
    {
        'path': '/api/v1/alerts/critical',
        'name': '[安全] 异常登录比例过高',
        'payload': build_alert(
            'AbnormalLoginRateCritical',
            'critical',
            'tailor-is-user',
            8101,
            '近 5 分钟失败登录比例 > 50%',
            '检测到大量失败登录，疑似字典攻击或撞库行为，请立即核查。',
            category='security',
        ),
    },
    {
        'path': '/api/v1/alerts/business',
        'name': '[业务] 活跃用户骤降',
        'payload': build_alert(
            'ActiveUsersDropped',
            'warning',
            'tailor-is-user',
            8101,
            '活跃用户相比 1 小时前下降超过 50%',
            '可能存在登录入口/会话模块或通知渠道故障，请确认。',
            category='business',
        ),
    },
]


def post_alert(path, payload, port=8080):
    url = f'http://127.0.0.1:{port}{path}'
    data = json.dumps(payload).encode('utf-8')
    req = urllib.request.Request(
        url,
        data=data,
        headers={'Content-Type': 'application/json'},
        method='POST',
    )
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            return r.status, r.read().decode('utf-8', errors='replace')
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8', errors='replace')
    except Exception as e:
        return -1, str(e)


def main():
    port = int(os.environ.get('ALERT_WEBHOOK_PORT', '8080'))
    print('=' * 70)
    print('  Tailor IS 告警端到端演练')
    print('=' * 70)
    print(f'  webhook 路径: http://127.0.0.1:{port}')
    print(f'  DINGTALK_WEBHOOK = {bool(os.environ.get("DINGTALK_WEBHOOK"))}')
    print(f'  FEISHU_WEBHOOK   = {bool(os.environ.get("FEISHU_WEBHOOK"))}')
    print(f'  WECOM_WEBHOOK    = {bool(os.environ.get("WECOM_WEBHOOK"))}')
    print(f'  RESEND_API_KEY   = {bool(os.environ.get("RESEND_API_KEY"))}')
    print(f'  ALERT_TO_EMAIL   = {os.environ.get("ALERT_TO_EMAIL", "")}')
    print('=' * 70)

    # ---------- 启动 alert-webhook 服务 ----------
    print('\n[1/3] 启动 alert-webhook 中继服务...')
    if not os.path.exists(WEBHOOK_PY):
        print(f'❌ 找不到 {WEBHOOK_PY}')
        sys.exit(1)

    # 先用简单 HTTP 健康检查看是否已有服务在监听
    try:
        urllib.request.urlopen(f'http://127.0.0.1:{port}/health', timeout=2)
        proc = None
        print(f'  ℹ️  已存在监听端口 {port} 的服务，直接复用。')
    except Exception:
        # 新起一个后台进程
        env = os.environ.copy()
        env.setdefault('PORT', str(port))
        proc = subprocess.Popen(
            [sys.executable, WEBHOOK_PY],
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            env=env,
            preexec_fn=os.setsid,  # 便于一起 kill
        )
        print(f'  PID = {proc.pid}，等待服务启动...')
        # 等待健康检查通过
        for _ in range(30):
            time.sleep(0.5)
            if proc.poll() is not None:
                print(f'  ❌ alert-webhook 已退出，代码={proc.returncode}')
                try:
                    out = proc.stdout.read().decode('utf-8', errors='replace')
                    print(out[:2000])
                except Exception:
                    pass
                sys.exit(1)
            try:
                with urllib.request.urlopen(f'http://127.0.0.1:{port}/health', timeout=2) as r:
                    if r.status == 200:
                        print(f'  ✅ 服务已就绪 (HTTP 200 /health)')
                        break
            except Exception:
                continue
        else:
            print('  ❌ 启动超时（30s内未就绪）')
            try:
                os.killpg(os.getpgid(proc.pid), signal.SIGTERM)
            except Exception:
                pass
            sys.exit(1)

    # ---------- 发送告警 ----------
    print(f'\n[2/3] 发送 {len(DRILL_CASES)} 条模拟告警...')
    results = []
    for i, case in enumerate(DRILL_CASES, 1):
        print(f'  [{i}/{len(DRILL_CASES)}] {case["name"]} → POST {case["path"]}')
        status, body = post_alert(case['path'], case['payload'], port=port)
        status_ok = 200 <= status < 300
        results.append((case['name'], case['path'], status, body, status_ok))
        print(f'      HTTP {status}  →  {"✅" if status_ok else "❌"}  body={body[:180]}')
        time.sleep(0.8)  # 避免机器人限流

    # ---------- 总结 ----------
    print('\n[3/3] 演练总结:')
    print('-' * 70)
    success = sum(1 for *_, ok in results if ok)
    print(f'  成功: {success}/{len(results)}')
    for name, path, status, body, ok in results:
        icon = '✅' if ok else '❌'
        print(f'  {icon} [{status}] {name} → {path}')
    print('-' * 70)
    print('  请人工检查：')
    print('    · 钉钉群 / 飞书群 / 企业微信群 是否收到对应告警卡片')
    print('    · 配置了 ALERT_TO_EMAIL 时，收件邮箱是否收到 HTML 告警邮件')
    print('-' * 70)

    # ---------- 清理：若我们新起了进程，询问是否终止 ----------
    if proc is not None:
        try:
            ans = input('  输入 y 终止后台 alert-webhook 进程，其它键保留: ').strip().lower()
        except EOFError:
            ans = 'n'
        if ans == 'y':
            try:
                os.killpg(os.getpgid(proc.pid), signal.SIGTERM)
                print('  已发送 SIGTERM')
            except Exception as e:
                print(f'  终止失败: {e}')
        else:
            print(f'  保留进程 PID={proc.pid}，可手动: kill {proc.pid}')


if __name__ == '__main__':
    main()
