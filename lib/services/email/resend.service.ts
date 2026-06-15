// ==============================================================================
// Tailor IS 裁智云 - Resend 邮件服务层
// ==============================================================================
// 依赖安装: npm install resend
// API文档: https://resend.com/docs
// 发件域名: @tailorbot.top
// ==============================================================================

import { Resend } from 'resend';

// ============ 环境变量加载 (兼容无构建环境直接使用 process.env)
const API_KEY = process.env.RESEND_API_KEY || '';
const DEFAULT_FROM = process.env.EMAIL_FROM || 'noreply@tailorbot.top';
const DEFAULT_FROM_NAME = process.env.EMAIL_FROM_NAME || '服装纸样平台';
const PLATFORM_NAME = process.env.PLATFORM_NAME || '服装纸样平台';
const REPLY_TO = process.env.EMAIL_REPLY_TO || 'support@tailorbot.top';

// ============ 初始化 Resend 客户端 ============
const resend = new Resend(API_KEY);

// ============ 类型定义 ============
export interface SendEmailOptions {
  to: string | string[];
  subject: string;
  html?: string;
  text?: string;
  from?: string;
  fromName?: string;
  replyTo?: string;
  cc?: string | string[];
  bcc?: string | string[];
  attachments?: Array<{ filename: string; content: Buffer | string }>;
  headers?: Record<string, string>;
  tags?: Array<{ name: string; value: string }>;
}

export interface SendEmailResult {
  success: boolean;
  emailId?: string;
  error?: string;
  timestamp?: Date;
}

// ============ 核心邮件发送函数 ============
export async function sendEmail(options: SendEmailOptions): Promise<SendEmailResult> {
  const timestamp = new Date();
  
  // 验证 API Key 配置检查
  if (!API_KEY || API_KEY.trim() === '') {
    return {
      success: false, error: 'RESEND_API_KEY 未配置，请检查环境变量',
      timestamp,
    };
  }

  // 验证收件人
  if (!options.to || (Array.isArray(options.to) && options.to.length === 0) {
    return {
      success: false,
      error: '收件人不能为空',
      timestamp,
    };
  }

  // 验证主题
  if (!options.subject || options.subject.trim() === '') {
    return {
      success: false,
      error: '邮件主题不能为空',
      timestamp,
    };
  }

  try {
    const fromAddress = `${options.fromName || DEFAULT_FROM_NAME} <${options.from || DEFAULT_FROM}>';
    
    // 强制同时提供 html 和 text (避免空邮件)
    const htmlContent = options.html || '';
    // 自动生成纯文本版本（去除HTML标签，添加换行
    const textContent = options.text || htmlContent.replace(/<[^>]*>/g, '').trim();

    const { data, error } = await resend.emails.send({
      from: fromAddress,
      to: options.to,
      subject: options.subject,
      html: htmlContent,
      text: textContent,
      replyTo: options.replyTo || REPLY_TO,
      cc: options.cc,
      bcc: options.bcc,
      attachments: options.attachments,
      headers: options.headers,
    });

    if (error) {
      console.error(`[邮件发送失败] ${timestamp.toISOString()}:`, error);
      return {
        success: false,
        error: error.message || String(error),
        timestamp,
      };
    }

    console.log(`[邮件发送成功] ${timestamp.toISOString()} ID: ${data?.id || 'unknown'}`);
    return {
      success: true,
      emailId: data?.id,
      timestamp,
    };
  } catch (error: any) {
    console.error(`[邮件发送异常] ${timestamp.toISOString()}:`, error?.message || error);
    return {
      success: false,
      error: error?.message || '未知错误',
      timestamp,
    };
  }
}

// ==============================================================================
// 邮件模板: 注册欢迎邮件
// ==============================================================================
export async function sendWelcomeEmail(
  userEmail: string,
  userName: string,
  loginUrl: string = 'https://www.tailoris.com'
): Promise<SendEmailResult> {
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
export async function sendPasswordResetEmail(
  userEmail: string,
  resetToken: string,
  resetUrl: string,
  expiresInMinutes: number = 30
): Promise<SendEmailResult> {
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
// 邮件模板: 订单确认邮件
// ==============================================================================
export async function sendOrderConfirmationEmail(
  userEmail: string,
  orderNo: string,
  orderDetails: {
    productName: string;
    amount: number;
    orderDate: string;
  },
  orderUrl: string = 'https://www.tailoris.com/orders'
): Promise<SendEmailResult> {
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
          <p style="font-size: 14px; color: #666; margin: 10px 0;"><strong>商品名称：</strong>${orderDetails.productName}</p>
          <p style="font-size: 14px; color: #666; margin: 10px 0;"><strong>订单金额：</strong>¥${orderDetails.amount.toFixed(2)}</p>
          <p style="font-size: 14px; color: #666; margin: 10px 0;"><strong>下单时间：</strong>${orderDetails.orderDate}</p>
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
// 邮件模板: 验证码邮件（邮箱验证邮件
// ==============================================================================
export async function sendVerificationEmail(
  userEmail: string,
  verificationCode: string,
  expiresInMinutes: number = 10
): Promise<SendEmailResult> {
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
        <div style="margin: 30px 0; text-align: center;">
          <a href="https://www.tailoris.com/verify?code=${verificationCode}" style="display: inline-block; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 14px 32px; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 16px;">完成验证</a>
        </div>
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
// 邮件模板: 通知类邮件（商家入驻审核结果
// ==============================================================================
export async function sendMerchantApprovalEmail(
  merchantEmail: string,
  merchantName: string,
  approved: boolean,
  reason?: string
): Promise<SendEmailResult> {
  const subject = `【${PLATFORM_NAME}】商家入驻审核${approved ? '通过' : '未通过'}`;
  const statusText = approved ? '恭喜您，您的商家入驻申请已通过审核' : '很抱歉，您的商家入驻申请未能通过审核';
  const statusColor = approved ? '#10b981' : '#ef4444';
  const html = `
    <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background: #f8f9fa;">
      <div style="background: ${approved ? 'linear-gradient(135deg, #10b981 0%, #059669 100%)' : 'linear-gradient(135deg, #ef4444 0%, #dc2626 100%)'}; padding: 30px; border-radius: 12px 12px 0 0; text-align: center;">
        <h1 style="color: white; margin: 0; font-size: 28px; font-weight: 600;">${statusText}</h1>
      </div>
      <div style="background: white; padding: 30px; border-radius: 0 0 12px 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
        <p style="font-size: 16px; color: #333; line-height: 1.8;">尊敬的 ${merchantName}，</p>
        <p style="font-size: 16px; color: #333; line-height: 1.8;">${approved ? '恭喜您的商家入驻申请已通过审核，现在您可以：' : '很抱歉，您的商家入驻申请未能通过审核，原因如下：'}</p>
        ${
          approved
            ? '<ul style="font-size: 16px; color: #333; line-height: 2;"><li>发布和管理您的商品和服务</li><li>接收订单和客户管理</li><li>参与平台营销活动</li><li>查看数据分析和报告</li></ul>'
            : `<div style="background: #fef2f2; padding: 20px; border-radius: 8px; border-left: 4px solid ${statusColor}; margin: 20px 0;"><p style="font-size: 14px; color: #991b1b; margin: 0;"><strong>未通过原因：</strong>${reason || '资料不完整或不符合平台要求'}</p></div>'
        }
        <div style="margin: 30px 0; text-align: center;">
          <a href="https://merchant.tailoris.com" style="display: inline-block; background: ${approved ? 'linear-gradient(135deg, #10b981 0%, #059669 100%)' : 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'}; color: white; padding: 14px 32px; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 16px;">${approved ? '进入商家后台' : '重新提交申请'}</a>
        </div>
        <p style="font-size: 14px; color: #666; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee;">如有任何问题，请联系 <a href="mailto:${REPLY_TO}" style="color: #667eea;">${REPLY_TO}</a></p>
      </div>
      <div style="text-align: center; padding: 20px; font-size: 12px; color: #999;">
        <p>&copy; ${new Date().getFullYear()} ${PLATFORM_NAME}. 欢迎加入我们的商家社区</p>
      </div>
    </div>
  `;

  return await sendEmail({
    to: merchantEmail,
    subject,
    html,
    tags: [{ name: 'category', value: approved ? 'merchant-approved' : 'merchant-rejected' }],
  });
}

// ==============================================================================
// 邮件模板: 系统通知邮件（通用模板，可自定义内容）
// ==============================================================================
export async function sendNotificationEmail(
  recipientEmail: string,
  title: string,
  content: string,
  ctaText?: string,
  ctaUrl?: string
): Promise<SendEmailResult> {
  const subject = `【${PLATFORM_NAME}】${title}';
  const html = `
    <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background: #f8f9fa;">
      <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 20px; border-radius: 12px 12px 0 0; text-align: center;">
        <h1 style="color: white; margin: 0; font-size: 24px; font-weight: 600;">${title}</h1>
      </div>
      <div style="background: white; padding: 30px; border-radius: 0 0 12px 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
        <div style="font-size: 16px; color: #333; line-height: 1.8;">${content}</div>
        ${ctaText && ctaUrl ? `<div style="margin: 30px 0; text-align: center;"><a href="${ctaUrl}" style="display: inline-block; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 14px 32px; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 16px;">${ctaText}</a></div>' : ''}
        <p style="font-size: 14px; color: #666; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee;">如有任何问题，请联系 <a href="mailto:${REPLY_TO}" style="color: #667eea;">${REPLY_TO}</a></p>
      </div>
      <div style="text-align: center; padding: 20px; font-size: 12px; color: #999;">
        <p>&copy; ${new Date().getFullYear()} ${PLATFORM_NAME}. 平台通知</p>
      </div>
    </div>
  `;

  return await sendEmail({
    to: recipientEmail,
    subject,
    html,
    tags: [{ name: 'category', value: 'notification' }],
  });
}

// ==============================================================================
// 导出默认对象，方便批量导入
// ==============================================================================
export default {
  sendEmail,
  sendWelcomeEmail,
  sendPasswordResetEmail,
  sendOrderConfirmationEmail,
  sendVerificationEmail,
  sendMerchantApprovalEmail,
  sendNotificationEmail,
};

console.log('[Resend邮件服务已初始化完成。发件域: @tailorbot.top');
