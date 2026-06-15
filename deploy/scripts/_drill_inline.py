#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""告警端到端演练 - 在同一进程内: 启动 webhook 线程 + 发送告警"""
import json, os, sys, threading, time, urllib.request, urllib.error, socket

os.environ.setdefault('PORT', '8083')
os.environ.setdefault('RESEND_API_KEY', 're_B2hPwaVx_JoC3P7wVVdK8yiKxnW22Y3TS')
os.environ.setdefault('ALERT_TO_EMAIL', '619539948@qq.com')
os.environ.setdefault('ALERT_FROM_EMAIL', 'Tailor IS 告警 <noreply@tailorbot.top>')

# 导入/执行 alert-webhook 模块，作为后台线程运行
sys.path.insert(0, '/home/tailor/Tailoris/deploy/alert-webhook')
import importlib.util
spec = importlib.util.spec_from_file_location('alert_webhook_server', '/home/tailor/Tailoris/deploy/alert-webhook/server.py')
ws = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ws)

PORT = 8083

# ---------- 启动 HTTP 服务器（后台线程）----------
server = ws.HTTPServer(('127.0.0.1', PORT), ws.WebhookHandler)
t = threading.Thread(target=server.serve_forever, daemon=True)
t.start()
time.sleep(1.0)

print('=' * 70)
print('  Tailor IS 告警端到端演练')
print('=' * 70)
print(f'  服务: http://127.0.0.1:{PORT}  (后台线程)')
print(f'  钉钉  = {bool(ws.DINGTALK_WEBHOOK)}  飞书={bool(ws.FEISHU_WEBHOOK)}  企业微信={bool(ws.WECOM_WEBHOOK)}')
print(f'  邮件  = {bool(ws.RESEND_API_KEY and ws.ALERT_TO_EMAILS)}  -> {ws.ALERT_TO_EMAILS}')
print('-' * 70)
print()

# 健康检查
try:
    r = urllib.request.urlopen(f'http://127.0.0.1:{PORT}/health', timeout=5)
    body = json.loads(r.read().decode('utf-8'))
    print(f'[OK] /health -> channels={body["channels"]}')
except Exception as e:
    print(f'[FAIL] /health: {e}')
    sys.exit(1)

# ---------- 告警用例 ----------
cases = [
    ('默认兜底-未知信号', '/api/v1/alerts/default', 'UnknownSignalSpike', 'warning', 'tailor-is-gateway', 8080,
     '网关出现大量未归类请求，疑似异常流量注入'),
    ('严重-错误率',       '/api/v1/alerts/critical', 'HighErrorRate', 'critical', 'tailor-is-user', 8101,
     '用户服务近 5 分钟 5xx 错误率达 12.3%，登录与个人中心可能受阻'),
    ('服务下线-支付',      '/api/v1/alerts/service-down', 'ServiceDown', 'critical', 'tailor-is-payment', 8105,
     '支付服务在端口 8105 连续 2 分钟不可达，影响下单与支付流程'),
    ('警告-JVM内存',       '/api/v1/alerts/warning', 'HighJVMMemory', 'warning', 'tailor-is-product', 8103,
     'tailor-is-product JVM 堆内存持续超过 85%，GC 压力上升'),
    ('严重-异常登录比例',  '/api/v1/alerts/critical', 'AbnormalLoginRateCritical', 'critical', 'tailor-is-user', 8101,
     '检测到近 5 分钟失败登录比例超 50%，疑似撞库攻击'),
    ('业务-活跃用户骤降',  '/api/v1/alerts/business', 'ActiveUsersDropped', 'warning', 'tailor-is-user', 8101,
     '活跃用户数相比 1 小时前下降超过 50%，请核查登录入口或通知渠道'),
]

ok_count = 0
results = []
for i,(cn,path,alertname,sev,srv,port,desc) in enumerate(cases,1):
    payload = {
        'receiver': f'{sev}-receiver',
        'status': 'firing',
        'alerts': [{
            'status': 'firing',
            'labels': {'alertname': alertname, 'severity': sev, 'service': srv,
                       'port': str(port), 'env': 'production'},
            'annotations': {'summary': f'{srv} {alertname}', 'description': desc},
            'startsAt': time.strftime('%Y-%m-%dT%H:%M:%S.000Z', time.gmtime()),
            'endsAt': '0001-01-01T00:00:00Z',
            'generatorURL': f'http://prometheus:9090/graph?g0.expr={alertname}',
            'fingerprint': f'{alertname}_{srv}_{i}',
        }],
        'groupLabels': {'alertname': alertname, 'service': srv},
        'commonLabels': {'alertname': alertname, 'severity': sev},
        'commonAnnotations': {'summary': f'{srv} {alertname}', 'description': desc},
        'externalURL': 'http://alertmanager:9093',
    }
    data = json.dumps(payload, ensure_ascii=False).encode('utf-8')
    req = urllib.request.Request(
        f'http://127.0.0.1:{PORT}{path}',
        data=data, headers={'Content-Type': 'application/json'}, method='POST',
    )
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            resp_body = r.read().decode('utf-8', errors='replace')
            ok = 200 <= r.status < 300
    except urllib.error.HTTPError as e:
        resp_body, ok = f'HTTP {e.code}', False
    except Exception as e:
        resp_body, ok = f'ERR: {e}', False

    if ok: ok_count += 1
    results.append((cn, path, 'ok' if ok else 'fail', resp_body[:180]))
    print(f'  [{i}/{len(cases)}] {cn}  {"✅" if ok else "❌"}  dispatched={resp_body[:80]}')
    time.sleep(0.8)

print()
print('=' * 70)
print(f'  汇总: {ok_count}/{len(cases)} 个 HTTP 200')
print('-' * 70)
for cn,path,status,body in results:
    print(f'  · {cn:12s} -> {path:34s} [{status}]  {body[:80]}')
print('-' * 70)
print('  请人工核对:')
print('    1) 邮箱 619539948@qq.com 中是否收到 3 封告警邮件:')
print('       · HighErrorRate        (critical 路径 → 邮件)')
print('       · ServiceDown          (service-down 路径 → 邮件)')
print('       · AbnormalLoginRateCritical (critical 路径 → 邮件)')
print('       · ActiveUsersDropped   (business 路径 → 邮件)')
print('    2) 如需验证钉钉/飞书，设置 DINGTALK_WEBHOOK / FEISHU_WEBHOOK 环境变量后重跑')
print('=' * 70)

server.shutdown()
