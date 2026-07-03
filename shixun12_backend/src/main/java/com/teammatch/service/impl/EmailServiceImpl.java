package com.teammatch.service.impl;

import com.teammatch.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 邮件服务实现类
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendVerificationCode(String to, String code) {
        // 验证发件人邮箱是否配置
        if (fromEmail == null || fromEmail.trim().isEmpty()) {
            log.error("邮件发送失败：未配置发件人邮箱（spring.mail.username）");
            throw new RuntimeException("邮件服务未配置，请联系管理员");
        }
        
        log.info("准备发送邮件: from={}, to={}, code={}", fromEmail, to, code);
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("TeamMatch 邮箱验证码");
            
            // HTML 格式的邮件内容
            String htmlContent = buildVerificationEmailHtml(code);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("验证码邮件发送成功: {}", to);
            // 在控制台打印验证码，方便开发调试
            System.out.println("========== 邮箱验证码 ==========");
            System.out.println("收件人: " + to);
            System.out.println("验证码: " + code);
            System.out.println("有效期: 10 分钟");
            System.out.println("================================");
        } catch (MessagingException e) {
            log.error("发送验证码邮件失败: {}, 错误详情: {}", to, e.getMessage());
            log.error("邮箱配置可能不正确，建议检查: spring.mail 配置项");
            log.error("发件人邮箱: {}, 收件人邮箱: {}", fromEmail, to);
            throw new RuntimeException("发送邮件失败，请检查邮箱配置", e);
        } catch (Exception e) {
            log.error("发送验证码邮件异常: {}, 错误: {}", to, e.getMessage());
            log.error("如果这是开发环境，可以考虑配置 MailHog 或使用其他邮件测试工具");
            throw new RuntimeException("发送邮件失败，请稍后重试", e);
        }
    }

    /**
     * 构建验证码邮件的 HTML 内容
     */
    private String buildVerificationEmailHtml(String code) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 0; }" +
                ".container { max-width: 600px; margin: 40px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }" +
                ".header { background-color: #4A90E2; color: white; padding: 30px; text-align: center; }" +
                ".header h1 { margin: 0; font-size: 24px; }" +
                ".content { padding: 40px 30px; }" +
                ".content p { color: #333; line-height: 1.6; margin: 10px 0; }" +
                ".code-box { background-color: #f8f9fa; border: 2px dashed #4A90E2; border-radius: 6px; padding: 20px; margin: 20px 0; text-align: center; }" +
                ".code { font-size: 32px; font-weight: bold; color: #4A90E2; letter-spacing: 5px; }" +
                ".footer { background-color: #f8f9fa; padding: 20px; text-align: center; color: #999; font-size: 12px; }" +
                ".warning { color: #ff6b6b; font-size: 14px; margin-top: 20px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1>TeamMatch</h1>" +
                "</div>" +
                "<div class='content'>" +
                "<p>您好！</p>" +
                "<p>您正在 TeamMatch 平台进行邮箱验证，请使用以下验证码完成验证：</p>" +
                "<div class='code-box'>" +
                "<div class='code'>" + code + "</div>" +
                "</div>" +
                "<p>验证码有效期为 <strong>10 分钟</strong>，请尽快使用。</p>" +
                "<p class='warning'>⚠️ 如果这不是您本人的操作，请忽略此邮件，您的账号是安全的。</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>此邮件由系统自动发送，请勿回复</p>" +
                "<p>&copy; 2024 TeamMatch. All rights reserved.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}
