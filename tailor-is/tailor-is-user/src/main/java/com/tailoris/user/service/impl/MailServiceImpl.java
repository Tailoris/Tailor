package com.tailoris.user.service.impl;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.user.service.MailService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MailServiceImpl implements MailService {

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${resend.email-from}")
    private String emailFrom;

    @Value("${resend.platform-name}")
    private String platformName;

    private Resend resendClient;

    @PostConstruct
    public void init() {
        if (apiKey != null && !apiKey.isBlank()) {
            resendClient = new Resend(apiKey);
            log.info("Resend 邮件服务初始化完成: from={}", emailFrom);
        } else {
            log.warn("Resend API Key 未配置，邮件发送功能将不可用");
        }
    }

    @Override
    public void sendVerificationCode(String email, String code) {
        if (resendClient == null) {
            throw new BusinessException("邮件服务未配置，请联系管理员");
        }

        try {
            String subject = platformName + " - 验证码";
            String html = "<div style=\"font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;\">" +
                    "<div style=\"max-width: 400px; margin: 0 auto; padding: 24px;\">" +
                    "<h2 style=\"color: #333; font-size: 18px; margin-bottom: 16px;\">你的验证码</h2>" +
                    "<div style=\"background: #f5f5f5; border-radius: 8px; padding: 16px; text-align: center;\">" +
                    "<span style=\"font-size: 32px; font-weight: bold; color: #409eff;\">" + code + "</span>" +
                    "</div>" +
                    "<p style=\"color: #999; font-size: 14px; margin-top: 16px;\">验证码有效期5分钟，请尽快使用。</p>" +
                    "<p style=\"color: #666; font-size: 12px; margin-top: 24px;\">—— " + platformName + "</p>" +
                    "</div>" +
                    "</div>";

            CreateEmailOptions request = CreateEmailOptions.builder()
                    .from(emailFrom)
                    .to(email)
                    .subject(subject)
                    .html(html)
                    .text("您的验证码是：" + code + "\n\n验证码有效期5分钟，请尽快使用。\n\n—— " + platformName)
                    .build();

            CreateEmailResponse response = resendClient.emails().send(request);
            log.info("邮箱验证码已发送: email={}, messageId={}", email, response.getId());
        } catch (ResendException e) {
            log.error("发送邮箱验证码失败: email={}, error={}", email, e.getMessage(), e);
            throw new BusinessException("发送邮件失败，请稍后重试");
        } catch (Exception e) {
            log.error("发送邮箱验证码异常: email={}, error={}", email, e.getMessage(), e);
            throw new BusinessException("发送邮件失败，请稍后重试");
        }
    }
}
