// ==============================================================================
// Tailor IS 裁智云 - Resend 邮件服务层 (Node.js 纯 JavaScript版本)
// ==============================================================================
// 特点: 无需npm包安装，使用Node.js内置https模块直接调用Resend HTTP API
// 依赖: 仅需Node.js (v16+)
// API文档: https://resend.com/docs/api-reference/send-email
// 发件域名: @tailorbot.top
// ==============================================================================

const https = require('https');

// ============ 环境变量加载 ============
try {
  // 尝试从.env.local加载环境变量（如存在dotenv包自动加载，否则已通过其他方式设置
  if (process.env.RESEND_API_KEY === undefined || process.env.RESEND_API_KEY === '') {
    try {
      require('dotenv').config({ path: '/home/tailor/Tailoris/.env.local' });
    } catch (e) {
      // dotenv不可用则尝试读取.env文件
      try {
        const fs = require('fs');
        const envContent = fs.readFileSync('/home/tailor/Tailoris/.env.local', 'utf-8');
        envContent.split('\n').forEach((line) => {
          const trimmed = line.trim();
          if (trimmed && !trimmed.startsWith('#') && trimmed.includes('=')) {
            const [key, ...valueParts] = trimmed.split('=');
            process.env[key.trim()] = valueParts.join('=').trim().replace(/^["']|["']$/g, '');
          }
        });
      } catch (readErr) {
        console.log('[提示] .env.local不存在，尝试使用.env.production');
        try {
          const fs = require('fs');
          const envContent = fs.readFileSync('/home/tailor/Tailoris/deploy/.env.production', 'utf-8');
          envContent.split('\n').forEach((line) => {
            const trimmed = line.trim();
            if (trimmed && !trimmed.startsWith('#') && trimmed.includes('=')) {
              const [key, ...valueParts] = trimmed.split('=');
              process.env[key.trim()] = valueParts.join('=').trim().replace(/^["']|["']$/g, '');
            }
          });
        } catch (e2) {
          console.log('[提示] 环境变量需要手动设置');
        }
      }
    }
  }
} catch (e) {
  // 忽略加载失败
}

const API_KEY = process.env.RESEND_API_KEY || '';
const DEFAULT_FROM = process.env.EMAIL_FROM || 'noreply@tailorbot.top';
const DEFAULT_FROM_NAME = process.env.EMAIL_FROM_NAME || '服装纸样平台';
const PLATFORM_NAME = process.env.PLATFORM_NAME || '服装纸样平台';
const REPLY_TO = process.env.EMAIL_REPLY_TO || 'support@tailorbot.top';

// ============ 核心邮件发送函数 (直接HTTPS调用 ============
async function sendEmail(options) {
  const timestamp = new Date();

  if (!API_KEY || API_KEY.trim() === '') {
    return {
      success: false,
      error: 'RESEND_API_KEY 未配置。请设置环境变量: export RESEND_API_KEY=your_api_key',
      timestamp,
    };
  }

  if (!options.to || (Array.isArray(options.to) && options.to.length === 0) {
    return {
      success: false,
      error: '收件人不能为空',
      timestamp,
    };
  }

  if (!options.subject || options.subject.trim() === '') {
    return {
      success: false,
      error: '邮件主题不能为空',
      timestamp,
    };
  }

  const fromAddress = `${options.fromName || DEFAULT_FROM_NAME} <${options.from || DEFAULT_FROM}>`;

  // 强制同时提供 html 和 text 内容（防止空邮件）
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

  if (options.cc) payload.cc = Array.isArray(options.cc) ? options.cc : [options.cc];
  if (options.bcc) payload.bcc = Array.isArray(options.bcc) ? options.bcc : [options.bcc];
  if (options.tags) payload.tags = options.tags;
  if (options.headers) payload.headers = options.headers;

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
      timeout: 30000, // 30秒超时
    };

    const req = https.request(reqOptions, (res) => {
      let data = '';
      res.on('data', (chunk) => {
        data += chunk;
      });

      res.on('end', () => {
        try {
          const response = JSON.parse(data);
          if (res.statusCode >= 200 && res.statusCode < 300) {
            console.log(`[邮件发送成功] ${timestamp.toISOString()} ID: ${response.id || 'unknown'}`);
            resolve({
              success: true,
              emailId: response.id,
              timestamp,
            });
          } else {
            console.error(`[邮件发送失败] ${timestamp.toISOString()} 状态码: ${res.statusCode}`);
            console.error(`  响应: ${data}`);
            resolve({
              success: false,
              error: response.message || response.error || `HTTP ${res.statusCode}`,
              timestamp,
            });
          }
        } catch (parseErr) {
          console.error(`[邮件发送异常] ${timestamp.toISOString()}:`, parseErr.message);
          resolve({
            success: false,
            error: `解析响应失败: ${parseErr.message}，原始响应: ${data}`,
            timestamp,
          });
        }
      });
    });

    req.on('error', (error) => {
      console.error(`[邮件发送网络错误] ${timestamp.toISOString()}:`, error.message);
      resolve({
        success: false,
        error: `网络错误: ${error.message}`,
        timestamp,
      });
    });

    req.on('timeout', () => {
      req.destroy(new Error('请求超时'));
    });

    req.write(postData);
    req.end();
  });
}

// ==============================================================================
// 邮件模板: 注册欢迎邮件
// ==============================================================================
async function sendWelcomeEmail(userEmail, userName, loginUrl = 'https://www.tailoris.com') {
  const subject = `欢迎加入${PLATFORM_NAME}！`;
  const html = `
    <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background: #f8f9fa;">
      <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; border-radius: 12px 12px 0 0; text-align: center;">
        <h1 style="color: white; margin: 0; font-size: 28px; font-weight: 600;">欢迎，${userName}！</h1>
        <p style="color: rgba(255,255,255,0.9);">您的账号已创建成功</p>
      </div>
      <div style="background: white; padding: 30px; border-radius: 0 0 12px 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
        <p style="font-size: 16px; color: #333; line-height: 1.8;">您好 ${userName}，</p>
        <p style="font-size: 16px; color: #333; line-height: 1.8;">感谢您注册${PLATFORM_NAME}！现在您可以：</p>
        <ul style="font-size: 16px; color: #333; line-height: 2;">
          <li>浏览海量专业服装纸样设计</li>
          <li>与专业版师开启定制合作</li>
          <li>发布和管理您的个人作品集</li>
          <li>享受社区交流与学习</li>
        </ul>
        <div style="margin: 30px 0; text-align: center;">
          <a href="${loginUrl}" style="display: inline-block; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 14px 32px; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 16px;">立即登录</a>
        </div>
        <p style="font-size: 14px; color: #666; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee;">如有任何问题，请回复此邮件或访问 <a href="mailto:${REPLY_TO}" style="color: #667eea;">${REPLY_TO}</a></p>
      </div>
      <div style="text-align: center; padding: 20px; font-size: 12px; color: #999;">
        <p>&copy; ${new Date().getFullYear()} ${PLATFORM_NAME}. All rights reserved.</p>
        <p>© tailorbot.top - 专业服装纸样平台</p>
      </div>
    </div>
  `;

  return await sendEmail({
    to: userEmail,
    subject,
    html,
    tags: [{ name: 'category', value: 'welcome' }],
  });
}

// ==============================================================================
// 邮件模板: 密码重置邮件
// ==============================================================================
async function sendPasswordResetEmail(userEmail, resetToken, resetUrl, expiresInMinutes = 30) {
  const subject = `【${PLATFORM_NAME}】密码重置请求`;
  const html = `
    <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background: #f8f9fa;">
      <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 20px; border-radius: 12px 12px 0 0; text-align: center;">
        <h1 style="color: white; margin: 0; font-size: 24px; font-weight: 600;">密码重置</h1>
      </div>
      <div style="background: white; padding: 30px; border-radius: 0 0 12px 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
        <p style="font-size: 16px; color: #333; line-height: 1.8;">您好，</p>
        <p style="font-size: 16px; color: #333; line-height: 1.8;">您请求重置账号密码，请点击下方链接完成重置：</p>
        <div style="margin: 30px 0; text-align: center;">
          <a href="${resetUrl}" style="display: inline-block; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 14px 32px; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 16px;">重置密码</a>
        </div>
        <p style="font-size: 14px; color: #666; line-height: 1.6;"><strong>重要提示：</strong></p>
        <ul style="font-size: 14px; color: #666; line-height: 1.8;">
          <li>此链接将在 <strong>${expiresInMinutes} 分钟</strong> 后失效</li>
          <li>如果您未发起此请求，请忽略此邮件，您的密码不会被更改</li>
          <li>请不要将此链接分享给他人</li>
        </ul>
        <div style="background: #f8f9fa; padding: 15px; border-radius: 8px; margin-top: 20px;">
          <p style="font-size: 14px; color: #666; margin: 0;"><strong>复制链接：</strong><br/><span style="word-break: break-all; font-family: monospace; font-size: 12px;">${resetUrl}</span></p>
        </div>
        <p style="font-size: 14px; color: #666; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee;">如有疑问，请联系 <a href="mailto:${REPLY_TO}" style="color: #667eea;">${REPLY_TO}</a></p>
      </div>
      <div style="text-align: center; padding: 20px; font-size: 12px; color: #999;">
        <p>&copy; ${new Date().getFullYear()} ${PLATFORM_NAME}. 请妥善保管您的账号安全</p>
      </div>
    </div>
  `;

  return await sendEmail({
    to: userEmail,
    subject,
    html,
    tags: [{ name: 'category', value: 'password-reset' }],
  });
}

// ==============================================================================
// 邮件模板: 邮箱验证码邮件
// ==============================================================================
async function sendVerificationEmail(userEmail, verificationCode, expiresInMinutes = 10) {
  const subject = `【${PLATFORM_NAME}】邮箱验证码 - ${verificationCode}`;
  const html = `
    <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background: #f8f9fa;">
      <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; border-radius: 12px 12px 0 0; text-align: center;">
        <h1 style="color: white; margin: 0; font-size: 28px; font-weight: 600;">邮箱验证</h1>
      </div>
      <div style="background: white; padding: 30px; border-radius: 0 0 12px 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
        <p style="font-size: 16px; color: #333; line-height: 1.8;">您好，</p>
        <p style="font-size: 16px; color: #333; line-height: 1.8;">您的邮箱验证码为：</p>
        <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; font-size: 48px; font-weight: bold; text-align: center; padding: 30px 0; border-radius: 12px; margin: 30px 0; letter-spacing: 8px;">${verificationCode}</div>
        <p style="font-size: 14px; color: #666; line-height: 1.6; text-align: center;">此验证码将在 <strong>${expiresInMinutes} 分钟</strong> 后失效</p>
        <p style="font-size: 14px; color: #666; line-height: 1.6; text-align: center;">请在注册页面输入此验证码完成邮箱验证</p>
        <p style="font-size: 14px; color: #666; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee;">如果您未发起此请求，请忽略此邮件</p>
      </div>
      <div style="text-align: center; padding: 20px; font-size: 12px; color: #999;">
        <p>&copy; ${new Date().getFullYear()} ${PLATFORM_NAME}. 请妥善保管您的验证码</p>
      </div>
    </div>
  `;

  return await sendEmail({
    to: userEmail,
    subject,
    html,
    tags: [{ name: 'category', value: 'verification' }],
  });
}

// ==============================================================================
// 邮件模板: 订单确认邮件
// ==============================================================================
async function sendOrderConfirmationEmail(userEmail, orderNo, orderDetails, orderUrl = 'https://www.tailoris.com/orders') {
  const subject = `【${PLATFORM_NAME}】订单确认 - ${orderNo}`;
  const html = `
    <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background: #f8f9fa;">
      <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; border-radius: 12px 12px 0 0; text-align: center;">
        <h1 style="color: white; margin: 0; font-size: 28px; font-weight: 600;">订单确认</h1>
        <p style="color: rgba(255,255,255,0.9);">感谢您的购买！</p>
      </div>
      <div style="background: white; padding: 30px; border-radius: 0 0 12px 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
        <p style="font-size: 16px; color: #333; line-height: 1.8;">您好，</p>
        <p style="font-size: 16px; color: #333; line-height: 1.8;">您的订单已成功创建，以下是订单详情：</p>
        <div style="background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;">
          <p style="font-size: 14px; color: #666; margin: 10px 0;"><strong>订单号：</strong>${orderNo}</p>
          <p style="font-size: 14px; color: #666; margin: 10px 0;"><strong>商品：</strong>${orderDetails.productName}</p>
          <p style="font-size: 14px; color: #666; margin: 10px 0;"><strong>金额：</strong>¥${orderDetails.amount.toFixed(2)}</p>
          <p style="font-size: 14px; color: #666; margin: 10px 0;"><strong>时间：</strong>${orderDetails.orderDate}</p>
        </div>
        <div style="margin: 30px 0; text-align: center;">
          <a href="${orderUrl}" style="display: inline-block; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 14px 32px; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 16px;">查看订单详情</a>
        </div>
        <p style="font-size: 14px; color: #666; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee;">如有任何问题，请联系 <a href="mailto:${REPLY_TO}" style="color: #667eea;">${REPLY_TO}</a></p>
      </div>
      <div style="text-align: center; padding: 20px; font-size: 12px; color: #999;">
        <p>&copy; ${new Date().getFullYear()} ${PLATFORM_NAME}. 感谢您的信任与支持</p>
      </div>
    </div>
  `;

  return await sendEmail({
    to: userEmail,
    subject,
    html,
    tags: [{ name: 'category', value: 'order-confirmation' }],
  });
}

// ==============================================================================
// 导出所有函数供其他文件使用
// ==============================================================================
module.exports = {
  sendEmail,
  sendWelcomeEmail,
  sendPasswordResetEmail,
  sendVerificationEmail,
  sendOrderConfirmationEmail,
  DEFAULT_FROM,
  DEFAULT_FROM_NAME,
  PLATFORM_NAME,
  REPLY_TO,
};

console.log(`[${new Date().toISOString()}] Resend邮件服务已初始化完成。发件域: @tailorbot.top`);
