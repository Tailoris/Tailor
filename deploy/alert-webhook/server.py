#!/usr/bin/env python3
"""
Tailor IS Alert Webhook 中继服务
- 接收 Prometheus/Alertmanager 的告警请求
- 根据 severity/category 分发到: 钉钉机器人 / 飞书机器人 / 企业微信 / 邮件 (Resend)
"""

import json
import os
import smtplib
import ssl
import time
import urllib.request
import urllib.parse
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, HTTPServer
from email.message import EmailMessage

PORT = int(os.getenv('PORT', '8080'))

DINGTALK_WEBHOOK = os.getenv('DINGTALK_WEBHOOK', '')
FEISHU_WEBHOOK = os.getenv('FEISHU_WEBHOOK', '')
WECOM_WEBHOOK = os.getenv('WECOM_WEBHOOK', '')
RESEND_API_KEY = os.getenv('RESEND_API_KEY', '')
ALERT_TO_EMAIL = os.getenv('ALERT_TO_EMAIL', 'oncall@tailoris.com')
ALERT_FROM_EMAIL = os.getenv('ALERT_FROM_EMAIL', 'alerts@tailoris.com')
SLACK_WEBHOOK = os.getenv('SLACK_WEBHOOK', '')


def send_http_post(url, payload_dict, headers=None):
    """通用 HTTP POST（使用标准库，无第三方依赖）"""
    req = urllib.request.Request(
        url,
        data=json.dumps(payload_dict, ensure_ascii=False).encode('utf-8'),
        headers={'Content-Type': 'application/json', **(headers or {})},
        method='POST'
    )
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            return resp.status, resp.read().decode('utf-8', errors='ignore')
    except Exception as e:
        return 500, str(e)


def format_alert_text(alerts_data):
    """将 alerts 格式化为人类可读文本"""
    lines = []
    common_labels = alerts_data.get('commonLabels', {})
    common_annotations = alerts_data.get('commonAnnotations', {})
    status = alerts_data.get('status', 'firing')
    lines.append(f"【{status.upper()}】Tailor IS 告警")
    lines.append(f"集群: {common_labels.get('cluster', 'N/A')}")
    lines.append(f"服务: {common_labels.get('service', common_labels.get('application', 'N/A'))}")
    lines.append(f"严重度: {common_labels.get('severity', 'warning')}")
    lines.append(f"告警名: {common_labels.get('alertname', 'N/A')}")

    for idx, alert in enumerate(alerts_data.get('alerts', []), 1):
        labels = alert.get('labels', {})
        annotations = alert.get('annotations', {})
        starts = alert.get('startsAt', '')
        lines.append(f"--- #{idx} ---")
        lines.append(f"告警: {labels.get('alertname', labels.get('summary', 'N/A'))}")
        lines.append(f"实例: {labels.get('instance', labels.get('exported_instance', 'N/A'))}")
        lines.append(f"描述: {annotations.get('description', annotations.get('message', 'N/A'))}")
        lines.append(f"时间: {starts}")

    return '\n'.join(lines)


def send_to_dingtalk(text):
    if not DINGTALK_WEBHOOK:
        return False
    payload = {'msgtype': 'text', 'text': {'content': text}}
    status, body = send_http_post(DINGTALK_WEBHOOK, payload)
    print(f"[DingTalk] status={status}, body={body[:200]}")
    return 200 <= status < 300


def send_to_feishu(text):
    if not FEISHU_WEBHOOK:
        return False
    payload = {'msg_type': 'text', 'content': {'text': text}}
    status, body = send_http_post(FEISHU_WEBHOOK, payload)
    print(f"[Feishu] status={status}, body={body[:200]}")
    return 200 <= status < 300


def send_to_wecom(text):
    if not WECOM_WEBHOOK:
        return False
    payload = {'msgtype': 'text', 'text': {'content': text}}
    status, body = send_http_post(WECOM_WEBHOOK, payload)
    print(f"[WeCom] status={status}, body={body[:200]}")
    return 200 <= status < 300


def send_to_slack(text):
    if not SLACK_WEBHOOK:
        return False
    payload = {'text': text}
    status, body = send_http_post(SLACK_WEBHOOK, payload)
    print(f"[Slack] status={status}, body={body[:200]}")
    return 200 <= status < 300


def send_email_via_resend(subject, body):
    """通过 Resend HTTP API 发送邮件（无需 SMTP 配置）"""
    if not RESEND_API_KEY:
        return False
    recipients = [e.strip() for e in ALERT_TO_EMAIL.split(',') if e.strip()]
    payload = {
        'from': ALERT_FROM_EMAIL,
        'to': recipients,
        'subject': subject,
        'text': body
    }
    status, resp_body = send_http_post(
        'https://api.resend.com/emails',
        payload,
        headers={'Authorization': f'Bearer {RESEND_API_KEY}'}
    )
    print(f"[Resend Email] status={status}, body={resp_body[:200]}")
    return 200 <= status < 300


def dispatch_alerts(path, alerts_data):
    """根据 path + severity 分发告警"""
    severity = alerts_data.get('commonLabels', {}).get('severity', 'warning')
    text = format_alert_text(alerts_data)
    subject = f"[Tailor IS] {severity.upper()} - {alerts_data.get('commonLabels', {}).get('alertname', 'Alert')}"

    results = {}

    if severity == 'critical' or 'critical' in path or 'service-down' in path:
        results['dingtalk'] = send_to_dingtalk(text)
        results['feishu'] = send_to_feishu(text)
        results['wecom'] = send_to_wecom(text)
        results['slack'] = send_to_slack(text)
        results['email'] = send_email_via_resend(subject, text)
    elif severity == 'warning' or 'warning' in path:
        results['feishu'] = send_to_feishu(text)
        results['email'] = send_email_via_resend(subject, text)
    elif 'security' in path:
        results['dingtalk'] = send_to_dingtalk(text)
        results['feishu'] = send_to_feishu(text)
        results['email'] = send_email_via_resend(subject, text)
    elif 'business' in path:
        results['wecom'] = send_to_wecom(text)
        results['email'] = send_email_via_resend(subject, text)
    else:
        results['feishu'] = send_to_feishu(text)

    return results


class AlertHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/health':
            self._send_json(200, {'status': 'ok', 'time': datetime.now(timezone.utc).isoformat()})
        elif self.path == '/':
            self._send_json(200, {
                'service': 'Tailor IS Alert Webhook',
                'status': 'running',
                'port': PORT,
                'endpoints': {
                    '/health': '健康检查',
                    '/api/v1/alerts/default': '默认告警',
                    '/api/v1/alerts/critical': '严重告警',
                    '/api/v1/alerts/warning': '警告',
                    '/api/v1/alerts/security': '安全告警',
                    '/api/v1/alerts/business': '业务告警',
                    '/api/v1/alerts/service-down': '服务下线告警'
                },
                'configured_channels': {
                    'dingtalk': bool(DINGTALK_WEBHOOK),
                    'feishu': bool(FEISHU_WEBHOOK),
                    'wecom': bool(WECOM_WEBHOOK),
                    'slack': bool(SLACK_WEBHOOK),
                    'email_resend': bool(RESEND_API_KEY)
                }
            })
        else:
            self._send_json(404, {'error': 'Not Found'})

    def do_POST(self):
        content_length = int(self.headers.get('Content-Length', 0))
        raw_body = self.rfile.read(content_length) if content_length > 0 else b''

        try:
            alerts_data = json.loads(raw_body.decode('utf-8'))
        except json.JSONDecodeError:
            self._send_json(400, {'error': 'Invalid JSON'})
            return

        print(f"\n[{datetime.now()}] POST {self.path} - alerts: {len(alerts_data.get('alerts', []))}")

        results = dispatch_alerts(self.path, alerts_data)
        dispatched = sum(1 for v in results.values() if v)

        self._send_json(200, {
            'status': 'received',
            'alerts': len(alerts_data.get('alerts', [])),
            'severity': alerts_data.get('commonLabels', {}).get('severity', 'warning'),
            'dispatched_channels': dispatched,
            'channel_results': {k: 'sent' if v else 'not_configured' for k, v in results.items()}
        })

    def _send_json(self, code, data):
        body = json.dumps(data, ensure_ascii=False).encode('utf-8')
        self.send_response(code)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format, *args):
        # 简化日志输出，避免污染 stdout
        print(f"[{self.address_string()}] {format % args}")


def main():
    server = HTTPServer(('0.0.0.0', PORT), AlertHandler)
    print(f"Tailor IS Alert Webhook 启动于 http://0.0.0.0:{PORT}")
    print(f"告警通道 - DingTalk: {bool(DINGTALK_WEBHOOK)}, Feishu: {bool(FEISHU_WEBHOOK)}, WeCom: {bool(WECOM_WEBHOOK)}, Slack: {bool(SLACK_WEBHOOK)}, Email: {bool(RESEND_API_KEY)}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\n正在停止...")
        server.server_close()


if __name__ == '__main__':
    main()
