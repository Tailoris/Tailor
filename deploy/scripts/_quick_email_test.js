const https = require('https');
const fs = require('fs');
const env = fs.readFileSync('/home/tailor/Tailoris/.env.local', 'utf-8');
const key = env.split('\n').find(l => l.startsWith('RESEND_API_KEY=')).split('=')[1];

console.log('=== Tailor IS 邮件服务快速测试 ===');
console.log('API Key 前缀:', key.substring(0, 6) + '..., 长度:', key.length);
console.log('');

const postData = JSON.stringify({
  from: '服装纸样平台 <noreply@tailorbot.top>',
  to: ['delivered@resend.dev'],
  subject: '[Tailor IS Test] Resend API 验证',
  html: '<div style="font-family:sans-serif;padding:24px;"><h1 style="color:#667eea;">✉️ Tailor IS 邮件服务测试</h1><p>Resend API 配置验证通过，邮件服务功能正常可用。</p><p style="color:#999;font-size:12px;margin-top:24px;">测试时间: ' + new Date().toLocaleString('zh-CN') + '</p></div>',
  reply_to: 'support@tailorbot.top'
});

const opts = {
  hostname: 'api.resend.com', port: 443, path: '/emails',
  method: 'POST', timeout: 30000,
  headers: {
    'Authorization': 'Bearer ' + key,
    'Content-Type': 'application/json',
    'Content-Length': Buffer.byteLength(postData)
  }
};

const start = Date.now();
const req = https.request(opts, (res) => {
  let data = '';
  res.on('data', c => data += c);
  res.on('end', () => {
    const dur = Date.now() - start;
    console.log('HTTP 状态码:', res.statusCode);
    console.log('响应时间:', dur + 'ms');
    try {
      const json = JSON.parse(data);
      console.log('邮件 ID:', json.id || '(无)');
      console.log('');
      if (res.statusCode >= 200 && res.statusCode < 300) {
        console.log('✅ 测试通过 - Resend 邮件服务完全可用');
      } else {
        console.log('❌ 测试失败 - API 返回非2xx状态');
        console.log('响应:', data);
      }
    } catch (e) {
      console.log('响应:', data);
    }
  });
});
req.on('error', (e) => console.log('❌ 网络错误:', e.message));
req.on('timeout', () => { req.destroy(new Error('超时')); });
req.write(postData);
req.end();
