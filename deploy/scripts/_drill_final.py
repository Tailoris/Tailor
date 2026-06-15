#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""告警端到端演练 - 修复 Cloudflare UA 问题后的验证"""
import json, os, sys, threading, time, urllib.request

os.environ['PORT'] = '8085'
os.environ['RESEND_API_KEY'] = 're_B2hPwaVx_JoC3P7wVVdK8yiKxnW22Y3TS'
os.environ['ALERT_TO_EMAIL'] = '619539948@qq.com'
os.environ['ALERT_FROM_EMAIL'] = 'Tailor IS 告警 <noreply@tailorbot.top>'

sys.path.insert(0, '/home/tailor/Tailoris/deploy/alert-webhook')
import importlib.util
spec = importlib.util.spec_from_file_location('ws', '/home/tailor/Tailoris/deploy/alert-webhook/server.py')
ws = importlib.util.module_from_spec(spec); spec.loader.exec_module(ws)

server = ws.HTTPServer(('127.0.0.1', 8085), ws.WebhookHandler)
threading.Thread(target=server.serve_forever, daemon=True).start()
time.sleep(0.8)

cases = [
    ('默认兜底',      '/api/v1/alerts/default',     'UnknownSignalSpike',     'warning',  'tailor-is-gateway', 8080, '网关出现大量未归类请求'),
    ('严重-错误率',    '/api/v1/alerts/critical',    'HighErrorRate',          'critical', 'tailor-is-user',    8101, '用户服务近 5 分钟 5xx 错误率达 12.3%，登录可能受阻'),
    ('服务下线-支付',   '/api/v1/alerts/service-down','ServiceDown',            'critical', 'tailor-is-payment', 8105, '支付服务 2 分钟不可达'),
    ('警告-JVM内存',    '/api/v1/alerts/warning',     'HighJVMMemory',          'warning',  'tailor-is-product',  8103, 'JVM 堆内存 > 85%'),
    ('严重-异常登录',   '/api/v1/alerts/critical',    'AbnormalLoginRateCritical','critical','tailor-is-user', 8101, '失败登录比例超 50%，疑似撞库'),
    ('业务-活跃用户降',  '/api/v1/alerts/business',    'ActiveUsersDropped',     'warning',  'tailor-is-user',    8101, '活跃用户相比 1 小时前下降超过 50%'),
]

print('=' * 70)
print('  告警端到端演练（修复 Cloudflare UA 后）')
print('  邮件 -> 619539948@qq.com  | 钉钉/飞书/企微: 本次未配置')
print('=' * 70)

email_ok = 0
email_total = 0
for i,(cn,path,alertname,sev,srv,port,desc) in enumerate(cases,1):
    payload = {
        'receiver': f'{sev}-receiver', 'status': 'firing',
        'alerts': [{'status':'firing',
                    'labels':{'alertname':alertname,'severity':sev,'service':srv,'port':str(port),'env':'production'},
                    'annotations':{'summary':f'{srv} {alertname}','description':desc},
                    'startsAt':time.strftime('%Y-%m-%dT%H:%M:%S.000Z',time.gmtime()),
                    'endsAt':'0001-01-01T00:00:00Z',
                    'generatorURL':f'http://prometheus:9090/graph?g0.expr={alertname}',
                    'fingerprint':f'{alertname}_{srv}_{i}'}],
        'groupLabels':{'alertname':alertname,'service':srv},
        'commonLabels':{'alertname':alertname,'severity':sev},
        'commonAnnotations':{'summary':f'{srv} {alertname}','description':desc},
        'externalURL':'http://alertmanager:9093',
    }
    data = json.dumps(payload, ensure_ascii=False).encode('utf-8')
    req = urllib.request.Request(f'http://127.0.0.1:8085{path}',
        data=data, headers={'Content-Type':'application/json'}, method='POST')
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            resp = json.loads(r.read().decode('utf-8'))
            dispatched = resp.get('dispatched', {})
            mark = '✅' if r.status == 200 else '❌'
            email_status = '✉️ 已发送' if dispatched.get('email') is True else (
                '✉️ 未发送(未启用)' if 'email' not in dispatched else '✉️ 发送失败')
            if 'email' in dispatched:
                email_total += 1
                if dispatched['email']:
                    email_ok += 1
            print(f'  [{i}/{len(cases)}] {mark} {cn:14s} -> {path}  {email_status}')
    except Exception as e:
        print(f'  [{i}/{len(cases)}] ❌ {cn} -> {path}: {e}')
    time.sleep(0.8)

print()
print('=' * 70)
print(f'  结果: email 发送 {email_ok}/{email_total} 成功')
print('  请人工核对邮箱 619539948@qq.com 中的告警邮件')
print('=' * 70)

server.shutdown()
