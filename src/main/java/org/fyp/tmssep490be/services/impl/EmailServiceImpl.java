package org.fyp.tmssep490be.services.impl;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.config.EmailConfig;
import org.fyp.tmssep490be.services.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Autowired
    @Qualifier("javaMailSender")
    private JavaMailSender mailSender;

    @Autowired
    @Qualifier("emailTemplateEngine")
    private TemplateEngine templateEngine;

    @Autowired
    private EmailConfig emailConfig;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    @Async
    public CompletableFuture<Void> sendEmailAsync(String to, String subject, String htmlContent) {
        if (mailSender == null) {
            log.warn("Email service is not configured. Skipping email to: {}", to);
            return CompletableFuture.completedFuture(null);
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

            helper.setFrom(String.format("%s <%s>", emailConfig.getFromName(), emailConfig.getFromEmail()));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Email sent successfully to: {}", to);
            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<Void> sendNewStudentCredentialsAsync(String to, String studentName, String studentCode,
                                                                  String email, String defaultPassword, String branchName) {
        String subject = "Chào mừng đến với Hệ thống Quản lý Đào tạo TMS - Thông tin đăng nhập";
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("studentName", studentName);
        templateData.put("studentCode", studentCode);
        templateData.put("email", email);
        templateData.put("defaultPassword", defaultPassword);
        templateData.put("branchName", branchName);
        templateData.put("loginUrl", frontendUrl + "/login");
        templateData.put("frontendUrl", frontendUrl);

        return sendEmailWithTemplateAsync(to, subject, "emails/new-student-credentials", templateData);
    }

    @Override
    public CompletableFuture<Void> sendEmailWithTemplateAsync(String to, String subject, String templateName, Map<String, Object> templateData) {
        try {
            Context context = new Context();
            context.setVariables(templateData);

            String htmlContent = templateEngine.process(templateName, context);
            return sendEmailAsync(to, subject, htmlContent);

        } catch (Exception e) {
            log.error("Failed to process email template {} for recipient {}: {}", templateName, to, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

}
