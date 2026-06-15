// ==============================================================================
// Tailor IS 裁智云 - 邮件服务功能测试脚本
// ==============================================================================
// 功能:
//   1. 验证 Resend API Key 配置
//   2. 发送基础测试邮件
//   3. 测试所有业务邮件模板
//   4. 输出完整测试报告
//
// 使用方式:
//   node deploy/scripts/test-email-service.js <收件人邮箱>
//
// 示例:
//   node deploy/scripts/test-email-service.js your@email.com
// ==============================================================================

const https = require('https');
const fs = require('fs');
const path = require('path');

// ============ 环境变量加载 ============
function loadEnv() {
  const envPath = path.resolve(__dirname, '../../.env.local');
  const prodPath = path.resolve(__dirname, '../.env.production');

  let loaded = false;
  for (const p of [envPath, prodPath]) {
    try {
      if (fs.existsSync(p)) {
        const content = fs.readFileSync(p, 'utf-8');
        content.split('\n').forEach((line) => {
          const trimmed = line.trim();
          if (trimmed && !trimmed.startsWith('#') && trimmed.includes('=')) {
            const [key, ...valueParts] = trimmed.split('=');
            process.env[key.trim()] = valueParts.join('=').trim().replace(/^["']|["']$/g, '');
          }
        });
        console.log(`[环境] 已加载配置: ${p}`);
        loaded = true;
        return;
      }
    } catch (e) { /* ignore */ }
  }
  if (!loaded) {
    console.log('[环境] 使用进程环境变量');
  }
}

loadEnv();

const API_KEY = process.env.RESEND_API_KEY || '';
const DEFAULT_FROM = process.env.EMAIL_FROM || 'noreply@tailorbot.top';
const DEFAULT_FROM_NAME = process.env.EMAIL_FROM_NAME || '服装纸样平台';
const PLATFORM_NAME = process.env.PLATFORM_NAME || '服装纸样平台';
const REPLY_TO = process.env.EMAIL_REPLY_TO || 'support@tailorbot.top';

// ============ 收件人解析 ============
const args = process.argv.slice(2);
let recipient = process.env.EMAIL_TEST_RECIPIENT || '';
if (args.length > 0) {
  recipient = args[0];
}

if (!recipient || recipient === 'test@example.com' || recipient === '') {
  console.error('\n❌ 错误: 未指定测试收件人邮箱');
  console.error('   使用方式: node deploy/scripts/test-email-service.js <your@email.com>');
  console.error('   或设置环境变量: export EMAIL_TEST_RECIPIENT=your@email.com\n');
  process.exit(1);
}

// ============ 核心邮件发送函数 ============
function sendEmail(options) {
  const timestamp = new Date();

  if (!API_KEY || API_KEY.trim() === '') {
    return Promise.resolve({ success: false, error: 'RESEND_API_KEY 未配置' });
  }

  const fromAddress = `${options.fromName || DEFAULT_FROM_NAME} <${options.from || DEFAULT_FROM}>`;
  const htmlContent = options.html || '';
  const textContent = options.text || htmlContent.replace(/<[^>]*>/g, '').trim();

  const payload = {
    from: fromAddress,
    to: Array.isArray(options.to) ? options.to : [options.to],
    subject: options.subject,
    html: htmlContent,
    text: textContent,
    reply_to: options.replyTo || REPLY_TO,
  };

  if (options.tags) payload.tags = options.tags;

  return new Promise((resolve) => {
    const postData = JSON.stringify(payload);
    const reqOptions = {
      hostname: 'api.resend.com',
      port: 443,
      path: '/emails',
      method: 'POST',
      headers: {
        Authorization: `Bearer ${API_KEY}`,
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(postData),
      },
      timeout: 30000,
    };

    const req = https.request(reqOptions, (res) => {
      let data = '';
      res.on('data', (chunk) => { data += chunk; });
      res.on('end', () => {
        try {
          const response = JSON.parse(data);
          if (res.statusCode >= 200 && res.statusCode < 300) {
            resolve({ success: true, emailId: response.id, statusCode: res.statusCode });
          } else {
            resolve({
              success: false,
              error: response.message || response.error || `HTTP ${res.statusCode}`,
              statusCode: res.statusCode,
              rawResponse: data,
            });
          }
        } catch (parseErr) {
          resolve({ success: false, error: `响应解析失败: ${parseErr.message}`, rawResponse: data });
        }
      });
    });

    req.on('error', (error) => resolve({ success: false, error: `网络错误: ${error.message}` }));
    req.on('timeout', () => req.destroy(new Error('请求超时')));
    req.write(postData);
    req.end();
  });
}

// ============ 邮件模板 ============
function buildWelcomeEmail(userName, loginUrl) {
  return {
    subject: `欢迎加入${PLATFORM_NAME}！`,
    html: `
      <div style="font-family:-apple-system,BlinkMacSystemFont,'Segoe UI','PingFang SC','Microsoft YaHei',sans-serif;max-width:600px;margin:0 auto;padding:20px;background:#f8f9fa;">
        <div style="background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);padding:30px;border-radius:12px 12px 0 0;text-align:center;">
          <h1 style="color:white;margin:0;font-size:28px;font-weight:600;">欢迎，${userName}！</h1>
          <p style="color:rgba(255,255,255,0.9);">您的账号已创建成功</p>
        </div>
        <div style="background:white;padding:30px;border-radius:0 0 12px 12px;box-shadow:0 2px 10px rgba(0,0,0,0.1);">
          <p style="font-size:16px;color:#333;line-height:1.8;">您好 ${userName}，</p>
          <p style="font-size:16px;color:#333;line-height:1.8;">感谢您注册${PLATFORM_NAME}！现在您可以浏览海量专业服装纸样设计、与专业版师开启定制合作、发布和管理您的个人作品集。</p>
          <div style="margin:30px 0;text-align:center;">
            <a href="${loginUrl}" style="display:inline-block;background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);color:white;padding:14px 32px;text-decoration:none;border-radius:8px;font-weight:600;font-size:16px;">立即登录</a>
          </div>
          <p style="font-size:14px;color:#666;margin-top:30px;padding-top:20px;border-top:1px solid #eee;">测试邮件 · ${new Date().toLocaleString('zh-CN')}</p>
        </div>
      </div>
    `,
    tags: [{ name: 'category', value: 'welcome' }],
  };
}

function buildVerificationEmail(code, expiresMinutes) {
  return {
    subject: `【${PLATFORM_NAME}】邮箱验证码 - ${code}`,
    html: `
      <div style="font-family:-apple-system,BlinkMacSystemFont,'Segoe UI','PingFang SC','Microsoft YaHei',sans-serif;max-width:600px;margin:0 auto;padding:20px;background:#f8f9fa;">
        <div style="background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);padding:30px;border-radius:12px 12px 0 0;text-align:center;">
          <h1 style="color:white;margin:0;font-size:28px;font-weight:600;">邮箱验证</h1>
        </div>
        <div style="background:white;padding:30px;border-radius:0 0 12px 12px;box-shadow:0 2px 10px rgba(0,0,0,0.1);">
          <p style="font-size:16px;color:#333;line-height:1.8;">您好，</p>
          <p style="font-size:16px;color:#333;line-height:1.8;">您的邮箱验证码为：</p>
          <div style="background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);color:white;font-size:48px;font-weight:bold;text-align:center;padding:30px 0;border-radius:12px;margin:30px 0;letter-spacing:8px;">${code}</div>
          <p style="font-size:14px;color:#666;line-height:1.6;text-align:center;">此验证码将在 <strong>${expiresMinutes} 分钟</strong> 后失效</p>
          <p style="font-size:14px;color:#666;margin-top:30px;padding-top:20px;border-top:1px solid #eee;">测试邮件 · 如未发起请忽略</p>
        </div>
      </div>
    `,
    tags: [{ name: 'category', value: 'verification' }],
  };
}

function buildPasswordResetEmail(resetUrl, expiresMinutes) {
  return {
    subject: `【${PLATFORM_NAME}】密码重置请求`,
    html: `
      <div style="font-family:-apple-system,BlinkMacSystemFont,'Segoe UI','PingFang SC','Microsoft YaHei',sans-serif;max-width:600px;margin:0 auto;padding:20px;background:#f8f9fa;">
        <div style="background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);padding:20px;border-radius:12px 12px 0 0;text-align:center;">
          <h1 style="color:white;margin:0;font-size:24px;font-weight:600;">密码重置</h1>
        </div>
        <div style="background:white;padding:30px;border-radius:0 0 12px 12px;box-shadow:0 2px 10px rgba(0,0,0,0.1);">
          <p style="font-size:16px;color:#333;line-height:1.8;">您好，</p>
          <p style="font-size:16px;color:#333;line-height:1.8;">您请求重置账号密码，请点击下方链接完成重置：</p>
          <div style="margin:30px 0;text-align:center;">
            <a href="${resetUrl}" style="display:inline-block;background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);color:white;padding:14px 32px;text-decoration:none;border-radius:8px;font-weight:600;font-size:16px;">重置密码</a>
          </div>
          <p style="font-size:14px;color:#666;line-height:1.6;"><strong>重要提示：</strong>此链接将在 <strong>${expiresMinutes} 分钟</strong> 后失效。如未发起请忽略。</p>
          <div style="background:#f8f9fa;padding:15px;border-radius:8px;margin-top:20px;">
            <p style="font-size:12px;color:#666;margin:0;word-break:break-all;font-family:monospace;">${resetUrl}</p>
          </div>
        </div>
      </div>
    `,
    tags: [{ name: 'category', value: 'password-reset' }],
  };
}

function buildOrderConfirmationEmail(orderNo, productName, amount) {
  return {
    subject: `【${PLATFORM_NAME}】订单确认 - ${orderNo}`,
    html: `
      <div style="font-family:-apple-system,BlinkMacSystemFont,'Segoe UI','PingFang SC','Microsoft YaHei',sans-serif;max-width:600px;margin:0 auto;padding:20px;background:#f8f9fa;">
        <div style="background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);padding:30px;border-radius:12px 12px 0 0;text-align:center;">
          <h1 style="color:white;margin:0;font-size:28px;font-weight:600;">订单确认</h1>
          <p style="color:rgba(255,255,255,0.9);">感谢您的购买！</p>
        </div>
        <div style="background:white;padding:30px;border-radius:0 0 12px 12px;box-shadow:0 2px 10px rgba(0,0,0,0.1);">
          <p style="font-size:16px;color:#333;line-height:1.8;">您好，</p>
          <p style="font-size:16px;color:#333;line-height:1.8;">您的订单已成功创建：</p>
          <div style="background:#f8f9fa;padding:20px;border-radius:8px;margin:20px 0;">
            <p style="font-size:14px;color:#666;margin:10px 0;"><strong>订单号：</strong>${orderNo}</p>
            <p style="font-size:14px;color:#666;margin:10px 0;"><strong>商品：</strong>${productName}</p>
            <p style="font-size:14px;color:#666;margin:10px 0;"><strong>金额：</strong>¥${amount.toFixed(2)}</p>
            <p style="font-size:14px;color:#666;margin:10px 0;"><strong>时间：</strong>${new Date().toLocaleString('zh-CN')}</p>
          </div>
        </div>
      </div>
    `,
    tags: [{ name: 'category', value: 'order-confirmation' }],
  };
}

function buildBasicTestEmail() {
  return {
    subject: `【测试】来自 ${PLATFORM_NAME} 的邮件服务测试`,
    html: `
      <div style="font-family:-apple-system,BlinkMacSystemFont,'Segoe UI','PingFang SC','Microsoft YaHei',sans-serif;max-width:600px;margin:0 auto;padding:30px;background:white;border:1px solid #e0e0e0;border-radius:12px;">
        <h1 style="color:#667eea;margin-top:0;">✉️ 邮件服务测试成功</h1>
        <p style="font-size:16px;color:#333;line-height:1.6;">如果您看到这封邮件，说明 <strong>${PLATFORM_NAME}</strong> 的邮件服务已正确配置并可用。</p>
        <div style="background:#f8f9fa;padding:20px;border-radius:8px;margin:20px 0;">
          <p style="font-size:14px;color:#555;margin:5px 0;"><strong>发件人：</strong>${DEFAULT_FROM_NAME} &lt;${DEFAULT_FROM}&gt;</p>
          <p style="font-size:14px;color:#555;margin:5px 0;"><strong>发件域名：</strong>@tailorbot.top</p>
          <p style="font-size:14px;color:#555;margin:5px 0;"><strong>发送时间：</strong>${new Date().toLocaleString('zh-CN')}</p>
          <p style="font-size:14px;color:#555;margin:5px 0;"><strong>邮件服务：</strong>Resend (HTTP API)</p>
        </div>
        <p style="font-size:12px;color:#999;margin-top:30px;padding-top:20px;border-top:1px solid #eee;">此为测试邮件，请确认已收到后告知平台管理员。</p>
      </div>
    `,
    tags: [{ name: 'category', value: 'test' }],
  };
}

// ============ 主测试流程 ============
async function main() {
  console.log('\n' + '='.repeat(70));
  console.log('  Tailor IS 裁智云 - 邮件服务功能测试');
  console.log('='.repeat(70));
  console.log(`  发件域名: @tailorbot.top`);
  console.log(`  发件人: ${DEFAULT_FROM_NAME} <${DEFAULT_FROM}>`);
  console.log(`  回复地址: ${REPLY_TO}`);
  console.log(`  测试收件人: ${recipient}`);
  console.log(`  平台名称: ${PLATFORM_NAME}`);
  console.log('='.repeat(70) + '\n');

  // ============ 前置检查 ============
  console.log('▶ 检查 1/2: API Key 配置');
  if (!API_KEY || API_KEY.trim() === '') {
    console.log('  ❌ 失败: RESEND_API_KEY 未配置\n');
    process.exit(1);
  }
  console.log(`  ✅ 已配置 (长度: ${API_KEY.length} 位, 前缀: ${API_KEY.substring(0, 6)}...)`);

  console.log('\n▶ 检查 2/2: 网络连通性 (api.resend.com:443)');
  try {
    await new Promise((resolve, reject) => {
      const req = https.get({ hostname: 'api.resend.com', port: 443, path: '/', timeout: 10000 }, (res) => {
        resolve(true);
      });
      req.on('error', reject);
      req.on('timeout', () => reject(new Error('超时')));
    });
    console.log('  ✅ 网络连通正常\n');
  } catch (e) {
    console.log(`  ⚠️  警告: 无法直接连通 (可能被CDN拦截，不影响API调用): ${e.message}\n`);
  }

  // ============ 邮件发送测试 ============
  const tests = [
    { name: '基础测试邮件', builder: buildBasicTestEmail },
    { name: '用户注册欢迎邮件', builder: () => buildWelcomeEmail('测试用户', 'https://www.tailoris.com/login') },
    { name: '邮箱验证码邮件', builder: () => buildVerificationEmail('882371', 10) },
    { name: '密码重置邮件', builder: () => buildPasswordResetEmail('https://www.tailoris.com/reset?token=test123', 30) },
    { name: '订单确认邮件', builder: () => buildOrderConfirmationEmail('ORD-' + Date.now(), '专业女装纸样 - A字裙', 128.00) },
  ];

  const results = [];
  let passed = 0;
  let failed = 0;

  console.log('▶ 开始邮件发送测试 (' + tests.length + ' 项)\n');

  for (let i = 0; i < tests.length; i++) {
    const test = tests[i];
    console.log(`  [${i + 1}/${tests.length}] ${test.name} ...`);

    const start = Date.now();
    try {
      const emailContent = test.builder();
      const result = await sendEmail({
        to: recipient,
        subject: emailContent.subject,
        html: emailContent.html,
        tags: emailContent.tags,
      });

      const duration = Date.now() - start;
      if (result.success) {
        console.log(`      ✅ 发送成功 (耗时: ${duration}ms, 邮件ID: ${result.emailId || 'N/A'})`);
        passed++;
        results.push({ name: test.name, status: 'PASS', duration, emailId: result.emailId });
      } else {
        console.log(`      ❌ 发送失败 (耗时: ${duration}ms)`);
        console.log(`         错误: ${result.error}`);
        failed++;
        results.push({ name: test.name, status: 'FAIL', duration, error: result.error });
      }
    } catch (err) {
      console.log(`      ❌ 异常: ${err.message}`);
      failed++;
      results.push({ name: test.name, status: 'ERROR', error: err.message });
    }

    // 间隔 800ms 避免触发速率限制
    if (i < tests.length - 1) {
      await new Promise((r) => setTimeout(r, 800));
    }
  }

  // ============ 测试报告 ============
  console.log('\n' + '='.repeat(70));
  console.log('  测试报告');
  console.log('='.repeat(70));
  console.log(`  总计: ${tests.length} 项`);
  console.log(`  通过: ${passed} 项`);
  console.log(`  失败: ${failed} 项`);
  console.log(`  时间: ${new Date().toLocaleString('zh-CN')}`);
  console.log('-' .repeat(70));

  results.forEach((r, idx) => {
    const icon = r.status === 'PASS' ? '✅' : '❌';
    const statusText = r.status === 'PASS'
      ? `成功 - ${r.duration}ms`
      : `失败 - ${r.error || '未知错误'}`;
    console.log(`  ${icon} [${idx + 1}] ${r.name}: ${statusText}`);
  });

  console.log('-' .repeat(70));

  if (failed === 0) {
    console.log('\n🎉 所有邮件测试通过！请检查收件箱（含垃圾邮件）确认已收到 ' + passed + ' 封测试邮件。');
    console.log('   发件人应为: ' + DEFAULT_FROM_NAME + ' <' + DEFAULT_FROM + '>');
  } else {
    console.log('\n⚠️  部分测试失败，请检查配置后重试。');
    process.exit(1);
  }

  console.log('='.repeat(70) + '\n');
}

main().catch((err) => {
  console.error('\n❌ 测试脚本发生未捕获的错误:', err.message);
  process.exit(1);
});
