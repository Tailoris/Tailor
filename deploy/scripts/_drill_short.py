#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""告警端到端演练（简化版）- 直接运行 + 输出到文件"""
import json, os, subprocess, sys, time, urllib.request, urllib.error, signal

PORT = 8082
WEBHOOK_PY = '/home/tailor/Tailoris/deploy/alert-webhook/server.py'
LOG = '/home/tailor/Tailoris/deploy/scripts/_drill_output.log'

# 写入环境变量（请按需修改）
os.environ.setdefault('PORT', str(PORT))
os.environ.setdefault('RESEND_API_KEY', 're_B2hPwaVx_JoC3P7wVVdK8yiKxnW22Y3TS')
os.environ.setdefault('ALERT_TO_EMAIL', '619539948@qq.com')
os.environ.setdefault('ALERT_FROM_EMAIL', 'Tailor IS 告警 <alerts@tailorbot.top>')
# 以下 IM Webhook 请填入真实值后再运行
# os.environ['DINGTALK_WEBHOOK'] = 'https://oapi.dingtalk.com/robot/send?access_token=XXX'
# os.environ['DINGTALK_SECRET'] = 'SECxxxx'
# os.environ['FEISHU_WEBHOOK'] = 'https://open.feishu.cn/open-apis/bot/v2/hook/XXX'
# os.environ['WECOM_WEBHOOK'] = 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=XXX'

lines = []
def log(s):
    print(s, flush=True)
    lines.append(s)

log('='*70)
log('  Tailor IS 告警端到端演练 - 简化版')
log('='*70)
log(f'  PORT={PORT}  RESEND={bool(os.environ.get("RESEND_API_KEY"))}  TO_EMAIL={os.environ.get("ALERT_TO_EMAIL")}')
log(f'  DINGTALK={bool(os.environ.get("DINGTALK_WEBHOOK"))}  FEISHU={bool(os.environ.get("FEISHU_WEBHOOK"))}  WECOM={bool(os.environ.get("WECOM_WEBHOOK"))}')
log('')

# ---------- 启动 ----------
log('[1/3] 启动 alert-webhook...')
proc = subprocess.Popen(
    [sys.executable, WEBHOOK_PY],
    stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
    env=os.environ.copy(), preexec_fn=os.setsid,
)
log(f'  PID={proc.pid}')
for _ in range(30):
    time.sleep(0.5)
    try:
        r = urllib.request.urlopen(f'http://127.0.0.1:{PORT}/health', timeout=2)
        if r.status == 200:
            log('  ✅ 服务就绪')
            health_body = json.loads(r.read().decode('utf-8'))
            log(f'  健康检查: channels={health_body.get("channels")}')
            break
    except Exception:
        pass
else:
    log('  ❌ 启动超时')
    try: os.killpg(os.getpgid(proc.pid), signal.SIGTERM)
    except Exception: pass
    sys.exit(1)

# ---------- 发送 6 条告警 ----------
log('')
log('[2/3] 发送模拟告警...')
cases = [
    ('/api/v1/alerts/default',      '默认兜底',  'UnknownSignalSpike', 'warning',  'tailor-is-gateway', 8080, '网关出现大量未归类请求'),
    ('/api/v1/alerts/critical',     '严重错误率','HighErrorRate',       'critical', 'tailor-is-user',    8101, '用户服务 5xx 错误率超 10%，登录可能受阻'),
    ('/api/v1/alerts/service-down', '服务下线',  'ServiceDown',         'critical', 'tailor-is-payment', 8105, '支付服务 2 分钟不可达，支付能力可能受影响'),
    ('/api/v1/alerts/warning',      '警告内存',  'HighJVMMemory',       'warning',  'tailor-is-product',  8103, '商品服务 JVM 堆内存使用率持续超过 85%'),
    ('/api/v1/alerts/critical',     '安全异常',  'AbnormalLoginRateCritical', 'critical', 'tailor-is-user', 8101, '检测到近 5 分钟失败登录比例超 50%，疑似撞库攻击'),
    ('/api/v1/alerts/business',     '业务异常',  'ActiveUsersDropped',  'warning',  'tailor-is-user',    8101, '活跃用户相比 1 小时前下降超过 50%，请核查登录入口'),
]

results = []
for i,(path,cn,alertname,sev,srv,port,desc) in enumerate(cases,1):
    payload = {
        'receiver': f'{sev}-receiver',
        'status': 'firing',
        'alerts': [{
            'status': 'firing',
            'labels': {
                'alertname': alertname, 'severity': sev, 'service': srv,
                'port': str(port), 'env': 'production',
            },
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
    req = urllib.request.Request(
        f'http://127.0.0.1:{PORT}{path}',
        data=json.dumps(payload).encode('utf-8'),
        headers={'Content-Type': 'application/json'},
        method='POST',
    )
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            body = r.read().decode('utf-8', errors='replace')
            results.append((path, cn, r.status, body, True))
            log(f'  [{i}/{len(cases)}] {cn} → HTTP {r.status}  body={body[:200]}')
    except urllib.error.HTTPError as e:
        results.append((path, cn, e.code, e.read().decode('utf-8', errors='replace'), False))
        log(f'  [{i}/{len(cases)}] {cn} → HTTP {e.code}  err={e.reason}')
    except Exception as e:
        results.append((path, cn, -1, str(e), False))
        log(f'  [{i}/{len(cases)}] {cn} → network error: {e}')
    time.sleep(0.8)

# ---------- 总结 ----------
ok = sum(1 for *_, o in results if o)
log('')
log('[3/3] 演练总结:')
log('-'*70)
log(f'  成功: {ok}/{len(cases)}')
for _,cn,status,body,ok2 in results:
    log(f'  {"✅" if ok2 else "❌"} [{status}] {cn}  dispatched={body[:80]}')
log('-'*70)
log('  请人工核对:')
log('    · 619539948@qq.com 是否收到 HTML 告警邮件（critical / service-down / business 这三类路径开启了 email）')
log('    · 若已填写 DINGTALK_WEBHOOK/FEISHU_WEBHOOK，钉钉/飞书群是否有对应告警卡片')
log('-'*70)

# 清理
log(f'\n终止 alert-webhook PID={proc.pid}')
try: os.killpg(os.getpgid(proc.pid), signal.SIGTERM)
except Exception: pass

# 写 log
with open(LOG, 'w', encoding='utf-8') as f:
    f.write('\n'.join(lines))
log(f'\n详细日志已保存到: {LOG}')
