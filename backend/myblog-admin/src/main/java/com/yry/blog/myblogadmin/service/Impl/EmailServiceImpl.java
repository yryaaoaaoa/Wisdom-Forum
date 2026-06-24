package com.yry.blog.myblogadmin.service.Impl;

import com.yry.blog.myblogadmin.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.Year;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username:noreply@example.com}")
    private String fromEmail;
    
    @Value("${app.name:智汇论坛}")
    private String appName;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String toEmail, String resetToken, String resetUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("【" + appName + "】密码重置邮件");
            
            String fullResetUrl = resetUrl + "?token=" + resetToken;
            String htmlContent = buildPasswordResetEmailHtml(fullResetUrl);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("密码重置邮件发送成功，收件人: {}", toEmail);
        } catch (MessagingException e) {
            log.error("密码重置邮件发送失败，收件人: {}, 错误: {}", toEmail, e.getMessage());
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    @Override
    @Async
    public void sendSimpleEmail(String toEmail, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(content);
        
        mailSender.send(message);
        log.info("简单邮件发送成功，收件人: {}", toEmail);
    }
    
    private String buildPasswordResetEmailHtml(String resetUrl) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>");
        sb.append("<html>");
        sb.append("<head>");
        sb.append("<meta charset=\"UTF-8\">");
        sb.append("<style>");
        sb.append("body { font-family: 'Microsoft YaHei', Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px; }");
        sb.append(".container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); overflow: hidden; }");
        sb.append(".header { background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%); color: white; padding: 30px; text-align: center; }");
        sb.append(".header h1 { margin: 0; font-size: 24px; }");
        sb.append(".content { padding: 30px; }");
        sb.append(".content p { color: #333; line-height: 1.8; margin-bottom: 20px; }");
        sb.append(".button { display: inline-block; padding: 12px 30px; background-color: #409eff; color: white; text-decoration: none; border-radius: 4px; font-weight: bold; }");
        sb.append(".button:hover { background-color: #66b1ff; }");
        sb.append(".warning { background-color: #fff3cd; border: 1px solid #ffc107; border-radius: 4px; padding: 15px; margin: 20px 0; }");
        sb.append(".warning p { margin: 0; color: #856404; font-size: 14px; }");
        sb.append(".footer { background-color: #f5f5f5; padding: 20px; text-align: center; color: #999; font-size: 12px; }");
        sb.append("</style>");
        sb.append("</head>");
        sb.append("<body>");
        sb.append("<div class=\"container\">");
        sb.append("<div class=\"header\">");
        sb.append("<h1>").append(appName).append("</h1>");
        sb.append("</div>");
        sb.append("<div class=\"content\">");
        sb.append("<p>您好！</p>");
        sb.append("<p>我们收到了您的密码重置请求。请点击下方按钮重置您的密码：</p>");
        sb.append("<p style=\"text-align: center;\">");
        sb.append("<a href=\"").append(resetUrl).append("\" class=\"button\">重置密码</a>");
        sb.append("</p>");
        sb.append("<p>或者复制以下链接到浏览器地址栏：</p>");
        sb.append("<p style=\"word-break: break-all; background-color: #f5f5f5; padding: 10px; border-radius: 4px; font-size: 12px;\">");
        sb.append(resetUrl);
        sb.append("</p>");
        sb.append("<div class=\"warning\">");
        sb.append("<p>此链接将在 <strong>30分钟</strong> 后失效，请尽快完成密码重置。</p>");
        sb.append("<p>如果您没有请求重置密码，请忽略此邮件。</p>");
        sb.append("</div>");
        sb.append("</div>");
        sb.append("<div class=\"footer\">");
        sb.append("<p>此邮件由系统自动发送，请勿直接回复。</p>");
        sb.append("<p>&copy; ").append(Year.now().getValue()).append(" ").append(appName).append(" 版权所有</p>");
        sb.append("</div>");
        sb.append("</div>");
        sb.append("</body>");
        sb.append("</html>");
        return sb.toString();
    }
}
