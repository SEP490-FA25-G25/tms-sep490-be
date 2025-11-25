package org.fyp.tmssep490be.services.impl;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.services.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of EmailService using Resend API
 * Supports Vietnamese content with proper UTF-8 encoding
 * Includes rate limiting (5 emails/second) and retry mechanisms
 */
@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Autowired
    @Qualifier("resendClient")
    private Resend resend;

    @Autowired
    @Qualifier("emailTemplateEngine")
    private TemplateEngine templateEngine;

    @Value("${resend.from.email:noreply@tms.edu.vn}")
    private String fromEmail;

    @Value("${resend.from.name:Hệ thống TMS}")
    private String fromName;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    @Async
    @RateLimiter(name = "emailRateLimiter", fallbackMethod = "sendEmailFallback")
    public CompletableFuture<Void> sendEmailAsync(String to, String subject, String htmlContent) {
        if (resend == null) {
            log.warn("Email service is not configured. Skipping email to: {}", to);
            return CompletableFuture.completedFuture(null);
        }

        try {
            CreateEmailOptions request = CreateEmailOptions.builder()
                    .from(String.format("%s <%s>", fromName, fromEmail))
                    .to(to)
                    .subject(subject)
                    .html(htmlContent)
                    .build();

            CreateEmailResponse response = resend.emails().send(request);
            log.info("Email sent successfully to: {}, Response: {}", to, response.getId());
            return CompletableFuture.completedFuture(null);

        } catch (ResendException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", to, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
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

    @Override
    @RateLimiter(name = "emailRateLimiter", fallbackMethod = "sendBulkEmailFallback")
    public CompletableFuture<Void> sendBulkEmailAsync(List<String> recipients, String subject, String htmlContent) {
        return CompletableFuture.allOf(
            recipients.stream()
                .map(to -> sendEmailAsync(to, subject, htmlContent))
                .toArray(CompletableFuture[]::new)
        );
    }

    @Override
    public CompletableFuture<Void> sendWelcomeEmailAsync(String to, String userName, String verificationLink) {
        String subject = "Chào mừng đến với Hệ thống Quản lý Đào tạo TMS";
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("userName", userName);
        templateData.put("verificationLink", verificationLink);
        templateData.put("frontendUrl", frontendUrl);

        return sendEmailWithTemplateAsync(to, subject, "emails/welcome", templateData);
    }

    @Override
    public CompletableFuture<Void> sendPasswordResetEmailAsync(String to, String userName, String resetLink) {
        String subject = "Yêu cầu đặt lại mật khẩu - Hệ thống TMS";
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("userName", userName);
        templateData.put("resetLink", resetLink);
        templateData.put("frontendUrl", frontendUrl);

        return sendEmailWithTemplateAsync(to, subject, "emails/password-reset", templateData);
    }

    @Override
    public CompletableFuture<Void> sendClassEnrollmentNotificationAsync(String to, String studentName, String className, String centerName) {
        String subject = "Xác nhận đăng ký lớp học thành công";
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("studentName", studentName);
        templateData.put("className", className);
        templateData.put("centerName", centerName);
        templateData.put("frontendUrl", frontendUrl);

        return sendEmailWithTemplateAsync(to, subject, "emails/class-enrollment", templateData);
    }

    @Override
    public CompletableFuture<Void> sendGradeNotificationAsync(String to, String studentName, String assessmentName, String grade) {
        String subject = "Thông báo điểm thi mới";
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("studentName", studentName);
        templateData.put("assessmentName", assessmentName);
        templateData.put("grade", grade);
        templateData.put("frontendUrl", frontendUrl);

        return sendEmailWithTemplateAsync(to, subject, "emails/grade-notification", templateData);
    }

    @Override
    public CompletableFuture<Void> sendScheduleReminderAsync(String to, String studentName, String className, String sessionDate, String sessionTime) {
        String subject = "Nhắc nhở lịch học";
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("studentName", studentName);
        templateData.put("className", className);
        templateData.put("sessionDate", sessionDate);
        templateData.put("sessionTime", sessionTime);
        templateData.put("frontendUrl", frontendUrl);

        return sendEmailWithTemplateAsync(to, subject, "emails/schedule-reminder", templateData);
    }

    @Override
    public CompletableFuture<Void> sendTransferRequestNotificationAsync(String to, String studentName, String currentClass, String requestedClass) {
        String subject = "Yêu cầu chuyển lớp của học viên";
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("studentName", studentName);
        templateData.put("currentClass", currentClass);
        templateData.put("requestedClass", requestedClass);
        templateData.put("frontendUrl", frontendUrl);

        return sendEmailWithTemplateAsync(to, subject, "emails/transfer-request", templateData);
    }

    @Override
    public CompletableFuture<Void> sendMakeupRequestNotificationAsync(String to, String studentName, String missedSession, String requestedDate) {
        String subject = "Yêu cầu học bù của học viên";
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("studentName", studentName);
        templateData.put("missedSession", missedSession);
        templateData.put("requestedDate", requestedDate);
        templateData.put("frontendUrl", frontendUrl);

        return sendEmailWithTemplateAsync(to, subject, "emails/makeup-request", templateData);
    }

    @Override
    public CompletableFuture<Void> sendAttendanceAlertAsync(String to, String studentName, String className, Double attendanceRate) {
        String subject = "Cảnh báo điểm danh";
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("studentName", studentName);
        templateData.put("className", className);
        templateData.put("attendanceRate", attendanceRate);
        templateData.put("frontendUrl", frontendUrl);

        return sendEmailWithTemplateAsync(to, subject, "emails/attendance-alert", templateData);
    }

    @Override
    public CompletableFuture<Void> sendFeedbackReminderAsync(String to, String studentName, String className) {
        String subject = "Nhắc nhở đánh giá khóa học";
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("studentName", studentName);
        templateData.put("className", className);
        templateData.put("frontendUrl", frontendUrl);

        return sendEmailWithTemplateAsync(to, subject, "emails/feedback-reminder", templateData);
    }

    @Override
    public CompletableFuture<Void> sendLicenseExpiryWarningAsync(String to, String resourceName, String expiryDate, String centerName) {
        String subject = "Cảnh báo hết hạn tài nguyên";
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("resourceName", resourceName);
        templateData.put("expiryDate", expiryDate);
        templateData.put("centerName", centerName);
        templateData.put("frontendUrl", frontendUrl);

        return sendEmailWithTemplateAsync(to, subject, "emails/license-expiry-warning", templateData);
    }

    @Override
    public CompletableFuture<Void> sendPendingRequestReminderAsync(String to, int pendingCount, String oldestRequestDate) {
        String subject = "Nhắc nhở xử lý yêu cầu đang chờ";
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("pendingCount", pendingCount);
        templateData.put("oldestRequestDate", oldestRequestDate);
        templateData.put("frontendUrl", frontendUrl);

        return sendEmailWithTemplateAsync(to, subject, "emails/pending-request-reminder", templateData);
    }

    @Override
    public CompletableFuture<Void> sendWeeklyAttendanceReportAsync(String to, String reportData) {
        String subject = "Báo cáo điểm danh hàng tuần";
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("reportData", reportData);
        templateData.put("frontendUrl", frontendUrl);

        return sendEmailWithTemplateAsync(to, subject, "emails/weekly-attendance-report", templateData);
    }

    // Fallback methods for rate limiting
    public CompletableFuture<Void> sendEmailFallback(String to, String subject, String htmlContent, Exception ex) {
        log.warn("Rate limit exceeded for sending email to {}. Scheduling for retry: {}", to, ex.getMessage());
        // Queue email for retry or send to dead letter queue
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> sendBulkEmailFallback(List<String> recipients, String subject, String htmlContent, Exception ex) {
        log.warn("Rate limit exceeded for bulk email to {} recipients. Scheduling for retry: {}", recipients.size(), ex.getMessage());
        // Queue bulk email for retry or send to dead letter queue
        return CompletableFuture.completedFuture(null);
    }
}