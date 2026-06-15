#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
=============================================================
Alertmanager Webhook 中继服务 - Tailor IS
=============================================================
功能:
  - 接收 Alertmanager Webhook 回调 (JSON)
  - 根据告警级别自动分发到不同渠道:
      · 钉钉机器人 (DingTalk Markdown)
      · 飞书机器人 (Feishu Interactive Card)
      · 企业微信 (WeCom Markdown)
      · 邮件通知 (Resend HTTP API，@tailorbot.top)

环境变量:
  DINGTALK_WEBHOOK  - 钉钉机器人 webhook URL (含 access_token)
  DINGTALK_SECRET   - 钉钉加签密钥（可选）
  FEISHU_WEBHOOK    - 飞书机器人 webhook URL
  WECOM_WEBHOOK     - 企业微信机器人 webhook URL
  RESEND_API_KEY    - Resend API Key（用于告警邮件）
  ALERT_FROM_EMAIL  - 发件邮箱，默认 alerts@tailorbot.top
  ALERT_TO_EMAIL    - 告警收件邮箱，逗号分隔多收件人
  PORT              - 监听端口，默认 8080

部署:
  docker build -t tailor-is-alert-webhook .
  docker run -d -p 8080:8080 \
      -e DINGTALK_WEBHOOK=https://oapi.dingtalk.com/robot/send?access_token=XXX \
      -e FEISHU_WEBHOOK=https://open.feishu.cn/open-apis/bot/v2/hook/XXX \
      -e RESEND_API_KEY=re_xxxxxxxxxxxxxxxxxxxxxxxxxx \
      -e ALERT_TO_EMAIL=oncall@example.com \
      tailor-is-alert-webhook

Alertmanager 对接:
  receivers:
    - name: 'critical-receiver'
      webhook_configs:
        - url: 'http://alert-webhook:8080/api/v1/alerts/critical'
          send_resolved: true
=============================================================
"""

import os
import json
import logging
import hashlib
import base64
import hmac
import time
import datetime
import urllib.parse
import urllib.request
import ssl
from http.server import BaseHTTPRequestHandler, HTTPServer
from typing import Dict, List, Optional

# ---------- 日志 ----------
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(name)s: %(message)s'
)
logger = logging.getLogger('alert-webhook')

# ---------- 环境变量 ----------
DINGTALK_WEBHOOK = os.getenv('DINGTALK_WEBHOOK', '')
DINGTALK_SECRET = os.getenv('DINGTALK_SECRET', '')
FEISHU_WEBHOOK = os.getenv('FEISHU_WEBHOOK', '')
WECOM_WEBHOOK = os.getenv('WECOM_WEBHOOK', '')
RESEND_API_KEY = os.getenv('RESEND_API_KEY', '')
ALERT_FROM_EMAIL = os.getenv('ALERT_FROM_EMAIL', 'Tailor IS 告警 <noreply@tailorbot.top>')
ALERT_TO_EMAIL_RAW = os.getenv('ALERT_TO_EMAIL', '')
ALERT_TO_EMAILS = [e.strip() for e in ALERT_TO_EMAIL_RAW.split(',') if e.strip()]
PORT = int(os.getenv('PORT', '8080'))

# ---------- 告警级别颜色 ----------
SEVERITY_COLORS = {
    'critical': 'FF0000',
    'warning':  'FFA500',
    'info':     '00AA00',
}
SEVERITY_EMOJI = {
    'critical': '🚨',
    'warning':  '⚠️',
    'info':     'ℹ️',
}
SEVERITY_LABEL_CN = {
    'critical': '严重',
    'warning':  '警告',
    'info':     '信息',
}

# ---------- 路由: 不同 URL path 触发不同渠道组合 ----------
CHANNEL_MAP = {
    '/api/v1/alerts/default':   {'dingtalk': True,  'feishu': True,  'wecom': False, 'email': False},
    '/api/v1/alerts/critical':  {'dingtalk': True,  'feishu': True,  'wecom': True,  'email': True},
    '/api/v1/alerts/warning':   {'dingtalk': False, 'feishu': True,  'wecom': False, 'email': False},
    '/api/v1/alerts/business':  {'dingtalk': False, 'feishu': False, 'wecom': True,  'email': True},
    '/api/v1/alerts/service-down': {'dingtalk': True, 'feishu': True, 'wecom': True, 'email': True},
}

# ---------- 工具 ----------
# 标准浏览器 UA，避免被 Cloudflare (Resend API 的 CDN) 识别为机器人并返回 1010
_DEFAULT_UA = 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36'

def http_post_json(url: str, payload: dict, headers: Optional[Dict[str, str]] = None, timeout: int = 15) -> tuple:
    """通用 HTTP POST JSON，返回 (status_code, response_body)"""
    try:
        data = json.dumps(payload).encode('utf-8')
        req_headers = {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            'User-Agent': _DEFAULT_UA,
        }
        if headers:
            req_headers.update(headers)
        req = urllib.request.Request(url, data=data, headers=req_headers, method='POST')
        ctx = ssl.create_default_context()
        with urllib.request.urlopen(req, timeout=timeout, context=ctx) as resp:
            body = resp.read().decode('utf-8', errors='replace')
            return resp.status, body
    except Exception as e:
        logger.error(f'http_post_json 失败: {e}')
        return 0, str(e)

def sign_dingtalk() -> str:
    """钉钉加签（如配置了 SECRET）"""
    if not DINGTALK_SECRET:
        return ''
    timestamp = str(round(time.time() * 1000))
    string_to_sign = f'{timestamp}\n{DINGTALK_SECRET}'
    hmac_code = hmac.new(
        DINGTALK_SECRET.encode('utf-8'),
        string_to_sign.encode('utf-8'),
        digestmod=hashlib.sha256
    ).digest()
    sign = urllib.parse.quote_plus(base64.b64encode(hmac_code))
    return f'&timestamp={timestamp}&sign={sign}'

# ---------- 格式化 ----------
def format_markdown_for_dingtalk(alerts: List[dict], status: str) -> str:
    """钉钉 Markdown 消息内容"""
    firing = [a for a in alerts if a.get('status', status) == 'firing' or status == 'firing']
    resolved = [a for a in alerts if a.get('status') == 'resolved']
    lines = [f"# {SEVERITY_EMOJI.get('critical', '📢')} Tailor IS 平台告警 - {status.upper()}"]
    lines.append(f"> **时间**: {datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")

    for alert in alerts[:15]:
        labels = alert.get('labels', {})
        ann = alert.get('annotations', {})
        sev = labels.get('severity', 'info')
        lines.append(f"## {SEVERITY_EMOJI.get(sev, '📢')} [{SEVERITY_LABEL_CN.get(sev, sev)}] {labels.get('alertname', 'Unknown')}")
        lines.append(f"- **服务**: `{labels.get('service', 'unknown')}`")
        lines.append(f"- **端口**: `{labels.get('port', '-')}`")
        lines.append(f"- **环境**: `{labels.get('env', 'production')}`")
        if 'description' in ann:
            lines.append(f"- **描述**: {ann['description']}")
        if 'summary' in ann:
            lines.append(f"- **摘要**: {ann['summary']}")
        lines.append(f"- **开始时间**: {alert.get('startsAt', '-')}")
        if alert.get('status') == 'resolved':
            lines.append(f"- **结束时间**: {alert.get('endsAt', '-')}")
        lines.append('')

    if len(alerts) > 15:
        lines.append(f"> 另有 {len(alerts) - 15} 条告警已折叠")

    lines.append(f"\n> 共 {len(alerts)} 条告警 · {len(firing)} 触发 · {len(resolved)} 已恢复")
    return '\n'.join(lines)

def format_card_for_feishu(alerts: List[dict], status: str) -> dict:
    """飞书交互式卡片消息"""
    elements = []
    for alert in alerts[:10]:
        labels = alert.get('labels', {})
        ann = alert.get('annotations', {})
        sev = labels.get('severity', 'info')
        text = (
            f"**【{SEVERITY_LABEL_CN.get(sev, sev)}】{labels.get('alertname', 'Unknown')}**\n"
            f"> 服务: {labels.get('service', 'unknown')} 端口: {labels.get('port', '-')}\n"
            f"> 环境: {labels.get('env', 'production')}\n"
            f"> {ann.get('description', ann.get('summary', '无详情'))}"
        )
        elements.append({'tag': 'div', 'text': {'tag': 'lark_md', 'content': text}})

    _top_sev = next((a.get('labels', {}).get('severity', 'info') for a in alerts), 'info')
    color = SEVERITY_COLORS.get(_top_sev, '808080')
    _emoji = SEVERITY_EMOJI.get(_top_sev, '📢')
    return {
        'msg_type': 'interactive',
        'card': {
            'config': {'wide_screen_mode': True},
            'header': {
                'title': {
                    'tag': 'plain_text',
                    'content': f'{_emoji} Tailor IS 告警 - {status.upper()}'
                },
                'template': {
                    'FF0000': 'red',
                    'FFA500': 'orange',
                    '00AA00': 'green',
                }.get(color, 'blue')
            },
            'elements': elements
        }
    }

def format_markdown_for_wecom(alerts: List[dict], status: str) -> str:
    """企业微信 Markdown"""
    content = f"<font color='warning'>Tailor IS 平台告警 - {status.upper()}</font>\n\n"
    for alert in alerts[:10]:
        labels = alert.get('labels', {})
        ann = alert.get('annotations', {})
        sev = labels.get('severity', 'info')
        content += (
            f"> **【{SEVERITY_LABEL_CN.get(sev, sev)}】{labels.get('alertname', 'Unknown')}**\n"
            f"> 服务: {labels.get('service', 'unknown')} | 端口: {labels.get('port', '-')}\n"
            f"> {ann.get('description', ann.get('summary', '无详情'))}\n\n"
        )
    return content

def format_email_html(alerts: List[dict], status: str) -> str:
    """告警邮件 HTML 内容"""
    rows_html = ''
    for alert in alerts[:30]:
        labels = alert.get('labels', {})
        ann = alert.get('annotations', {})
        sev = labels.get('severity', 'info')
        sev_color = SEVERITY_COLORS.get(sev, '808080')
        rows_html += f"""
        <tr>
            <td style="padding:10px;border:1px solid #ddd;">
                <span style="color:#{sev_color};font-weight:bold;">{SEVERITY_LABEL_CN.get(sev, sev)}</span>
            </td>
            <td style="padding:10px;border:1px solid #ddd;">{labels.get('alertname', 'Unknown')}</td>
            <td style="padding:10px;border:1px solid #ddd;">{labels.get('service', 'unknown')}</td>
            <td style="padding:10px;border:1px solid #ddd;">{labels.get('port', '-')}</td>
            <td style="padding:10px;border:1px solid #ddd;">{ann.get('description', ann.get('summary', '-'))}</td>
            <td style="padding:10px;border:1px solid #ddd;">{alert.get('startsAt', '-')}</td>
        </tr>
        """
    return f"""
    <html><body style="font-family:-apple-system,'PingFang SC','Microsoft YaHei',sans-serif;">
        <div style="background:#fff;padding:24px;max-width:800px;">
            <h1 style="color:#E53935;margin:0 0 12px 0;">🚨 Tailor IS 平台告警通知 - {status.upper()}</h1>
            <p style="color:#555;">时间: {datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')} &nbsp;|&nbsp; 共 {len(alerts)} 条告警</p>
            <table style="border-collapse:collapse;width:100%;margin-top:12px;">
                <thead style="background:#f5f5f5;">
                    <tr>
                        <th style="padding:10px;border:1px solid #ddd;text-align:left;">级别</th>
                        <th style="padding:10px;border:1px solid #ddd;text-align:left;">告警名</th>
                        <th style="padding:10px;border:1px solid #ddd;text-align:left;">服务</th>
                        <th style="padding:10px;border:1px solid #ddd;text-align:left;">端口</th>
                        <th style="padding:10px;border:1px solid #ddd;text-align:left;">描述</th>
                        <th style="padding:10px;border:1px solid #ddd;text-align:left;">开始时间</th>
                    </tr>
                </thead>
                <tbody>{rows_html}</tbody>
            </table>
            <p style="color:#999;font-size:12px;margin-top:24px;">本邮件由 Alertmanager 自动发送，请勿直接回复。如需调整告警规则请联系平台管理员。</p>
        </div>
    </body></html>
    """

# ---------- 发送函数 ----------
def send_dingtalk(alerts: List[dict], status: str) -> bool:
    if not DINGTALK_WEBHOOK:
        logger.info('钉钉渠道未配置，跳过')
        return False
    try:
        url = DINGTALK_WEBHOOK + sign_dingtalk()
        payload = {
            'msgtype': 'markdown',
            'markdown': {
                'title': f'Tailor IS 告警 - {status.upper()}',
                'text': format_markdown_for_dingtalk(alerts, status)
            }
        }
        code, body = http_post_json(url, payload)
        logger.info(f'钉钉通知 HTTP {code}: {body[:200]}')
        return 200 <= code < 300
    except Exception as e:
        logger.error(f'钉钉通知异常: {e}')
        return False

def send_feishu(alerts: List[dict], status: str) -> bool:
    if not FEISHU_WEBHOOK:
        logger.info('飞书渠道未配置，跳过')
        return False
    try:
        payload = format_card_for_feishu(alerts, status)
        code, body = http_post_json(FEISHU_WEBHOOK, payload)
        logger.info(f'飞书通知 HTTP {code}: {body[:200]}')
        return 200 <= code < 300
    except Exception as e:
        logger.error(f'飞书通知异常: {e}')
        return False

def send_wecom(alerts: List[dict], status: str) -> bool:
    if not WECOM_WEBHOOK:
        logger.info('企业微信渠道未配置，跳过')
        return False
    try:
        payload = {'msgtype': 'markdown', 'markdown': {'content': format_markdown_for_wecom(alerts, status)}}
        code, body = http_post_json(WECOM_WEBHOOK, payload)
        logger.info(f'企业微信通知 HTTP {code}: {body[:200]}')
        return 200 <= code < 300
    except Exception as e:
        logger.error(f'企业微信通知异常: {e}')
        return False

def send_email_via_resend(alerts: List[dict], status: str) -> bool:
    """通过 Resend HTTP API 发送告警邮件"""
    if not RESEND_API_KEY:
        logger.info('Resend API Key 未配置，跳过邮件渠道')
        return False
    if not ALERT_TO_EMAILS:
        logger.info('邮件收件人未配置 (ALERT_TO_EMAIL)，跳过')
        return False

    try:
        subject = f'[{status.upper()}] Tailor IS 平台告警 - {len(alerts)} 条 - {datetime.datetime.now().strftime("%H:%M")}'
        payload = {
            'from': ALERT_FROM_EMAIL,
            'to': ALERT_TO_EMAILS,
            'subject': subject,
            'html': format_email_html(alerts, status),
            'reply_to': 'support@tailorbot.top',
            'tags': [{'name': 'category', 'value': 'alert'}]
        }
        code, body = http_post_json(
            'https://api.resend.com/emails',
            payload,
            headers={'Authorization': f'Bearer {RESEND_API_KEY}'}
        )
        logger.info(f'Resend 邮件 HTTP {code}: {body[:200]}')
        return 200 <= code < 300
    except Exception as e:
        logger.error(f'Resend 邮件发送异常: {e}')
        return False


class WebhookHandler(BaseHTTPRequestHandler):
    """Alertmanager webhook HTTP 处理器"""

    def do_POST(self):
        try:
            content_length = int(self.headers.get('Content-Length', 0))
            raw = self.rfile.read(content_length)
            try:
                payload = json.loads(raw.decode('utf-8'))
            except Exception:
                self.send_response(400)
                self.send_header('Content-Type', 'application/json')
                self.end_headers()
                self.wfile.write(json.dumps({'error': 'invalid JSON'}).encode('utf-8'))
                return

            alerts = payload.get('alerts', [])
            status = payload.get('status', 'firing')

            # 匹配路由 → 渠道
            channels = CHANNEL_MAP.get(self.path, CHANNEL_MAP['/api/v1/alerts/default'])
            logger.info(f'收到告警: path={self.path} status={status} alerts={len(alerts)} channels={channels}')

            results = {}
            if channels.get('dingtalk') and DINGTALK_WEBHOOK:
                results['dingtalk'] = send_dingtalk(alerts, status)
            if channels.get('feishu') and FEISHU_WEBHOOK:
                results['feishu'] = send_feishu(alerts, status)
            if channels.get('wecom') and WECOM_WEBHOOK:
                results['wecom'] = send_wecom(alerts, status)
            if channels.get('email') and RESEND_API_KEY and ALERT_TO_EMAILS:
                results['email'] = send_email_via_resend(alerts, status)

            if not any(channels.values()):
                # 退化到默认：至少尝试飞书 / 钉钉
                if FEISHU_WEBHOOK:
                    results['feishu_fallback'] = send_feishu(alerts, status)
                elif DINGTALK_WEBHOOK:
                    results['dingtalk_fallback'] = send_dingtalk(alerts, status)
                elif RESEND_API_KEY and ALERT_TO_EMAILS:
                    results['email_fallback'] = send_email_via_resend(alerts, status)

            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps({'status': 'ok', 'dispatched': results}).encode('utf-8'))
        except Exception as e:
            logger.exception(f'处理 webhook 失败: {e}')
            self.send_response(500)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps({'error': str(e)}).encode('utf-8'))

    def do_GET(self):
        """健康检查 / 渠道状态查看"""
        if self.path in ('/health', '/', '/ready'):
            body = {
                'status': 'ok',
                'time': datetime.datetime.now().isoformat(),
                'channels': {
                    'dingtalk': bool(DINGTALK_WEBHOOK),
                    'feishu': bool(FEISHU_WEBHOOK),
                    'wecom': bool(WECOM_WEBHOOK),
                    'email_resend': bool(RESEND_API_KEY and ALERT_TO_EMAILS),
                },
                'email_from': ALERT_FROM_EMAIL,
                'email_to': ALERT_TO_EMAILS,
            }
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps(body, indent=2, ensure_ascii=False).encode('utf-8'))
        else:
            self.send_response(404)
            self.end_headers()

    def log_message(self, format, *args):
        logger.info(format % args)


def run():
    server = HTTPServer(('0.0.0.0', PORT), WebhookHandler)
    logger.info(f'Alert Webhook 中继启动于 0.0.0.0:{PORT}')
    logger.info(f'  · 钉钉: {"启用" if DINGTALK_WEBHOOK else "禁用"}')
    logger.info(f'  · 飞书: {"启用" if FEISHU_WEBHOOK else "禁用"}')
    logger.info(f'  · 企业微信: {"启用" if WECOM_WEBHOOK else "禁用"}')
    logger.info(f'  · 邮件(Resend): {"启用 -> " + ",".join(ALERT_TO_EMAILS) if RESEND_API_KEY and ALERT_TO_EMAILS else "禁用"}')
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        server.shutdown()
        logger.info('服务已停止')


if __name__ == '__main__':
    run()
