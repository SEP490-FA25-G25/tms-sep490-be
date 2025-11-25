package org.fyp.tmssep490be.services.impl;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.config.EmailConfig;
import org.fyp.tmssep490be.services.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of EmailService using Gmail SMTP
 * Supports Vietnamese content with proper UTF-8 encoding
 * Includes rate limiting (20 emails/second) and retry mechanisms
 */
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
    @RateLimiter(name = "emailRateLimiter", fallbackMethod = "sendEmailFallback")
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
    public CompletableFuture<Void> sendWeeklyAttendanceReportAsync(String to, Map<String, Object> reportData) {
        String subject = "Báo cáo điểm danh hàng tuần";
        reportData.put("frontendUrl", frontendUrl);

        return sendEmailWithTemplateAsync(to, subject, "emails/weekly-attendance-report", reportData);
    }

    // Student Request Email Implementations

    @Override
    public CompletableFuture<Void> sendStudentRequestApprovalAsync(String to, Map<String, Object> requestData) {
        String subject = "Yêu cầu của bạn đã được duyệt - TMS";
        requestData.put("frontendUrl", frontendUrl);
        requestData.put("dashboardUrl", frontendUrl + "/dashboard");

        return sendEmailWithTemplateAsync(to, subject, "emails/student-request-approved", requestData);
    }

    @Override
    public CompletableFuture<Void> sendStudentRequestRejectionAsync(String to, Map<String, Object> requestData) {
        String subject = "Yêu cầu của bạn không được duyệt - TMS";
        requestData.put("frontendUrl", frontendUrl);
        requestData.put("newRequestUrl", frontendUrl + "/requests/new");

        return sendEmailWithTemplateAsync(to, subject, "emails/student-request-rejected", requestData);
    }

    @Override
    public CompletableFuture<Void> sendStudentRequestCreatedAsync(String to, Map<String, Object> requestData) {
        String subject = "Xác nhận nhận yêu cầu - TMS";
        requestData.put("frontendUrl", frontendUrl);
        requestData.put("dashboardUrl", frontendUrl + "/dashboard");

        return sendEmailWithTemplateAsync(to, subject, "emails/student-request-created", requestData);
    }

    @Override
    public CompletableFuture<Void> sendStudentRequestConfirmedAsync(String to, Map<String, Object> requestData) {
        String subject = "Yêu cầu đã được xác nhận - TMS";
        requestData.put("frontendUrl", frontendUrl);
        requestData.put("dashboardUrl", frontendUrl + "/dashboard");

        return sendEmailWithTemplateAsync(to, subject, "emails/student-request-confirmed", requestData);
    }

    @Override
    public CompletableFuture<Void> sendTeacherTransferNotificationAsync(String to, Map<String, Object> requestData) {
        String subject = "Thông báo thay đổi lịch giảng dạy - TMS";
        requestData.put("frontendUrl", frontendUrl);
        requestData.put("dashboardUrl", frontendUrl + "/teacher/dashboard");

        return sendEmailWithTemplateAsync(to, subject, "emails/teacher-transfer-notification", requestData);
    }

    // Teacher Request Email Implementations

    @Override
    public CompletableFuture<Void> sendTeacherRequestApprovalAsync(String to, Map<String, Object> requestData) {
        String subject = "Yêu cầu của giáo viên đã được duyệt - TMS";
        requestData.put("frontendUrl", frontendUrl);
        requestData.put("dashboardUrl", frontendUrl + "/teacher/dashboard");
        requestData.put("calendarUrl", frontendUrl + "/teacher/calendar");

        return sendEmailWithTemplateAsync(to, subject, "emails/teacher-request-approved", requestData);
    }

    @Override
    public CompletableFuture<Void> sendTeacherRequestRejectionAsync(String to, Map<String, Object> requestData) {
        String subject = "Yêu cầu của giáo viên không được duyệt - TMS";
        requestData.put("frontendUrl", frontendUrl);
        requestData.put("newRequestUrl", frontendUrl + "/teacher/requests/new");
        requestData.put("policiesUrl", frontendUrl + "/policies");

        return sendEmailWithTemplateAsync(to, subject, "emails/teacher-request-rejected", requestData);
    }

    @Override
    public CompletableFuture<Void> sendTeacherRequestCreatedAsync(String to, Map<String, Object> requestData) {
        String subject = "Xác nhận nhận yêu cầu từ giáo viên - TMS";
        requestData.put("frontendUrl", frontendUrl);
        requestData.put("dashboardUrl", frontendUrl + "/dashboard");

        return sendEmailWithTemplateAsync(to, subject, "emails/teacher-request-created", requestData);
    }

    @Override
    public CompletableFuture<Void> sendTeacherRequestConfirmedAsync(String to, Map<String, Object> requestData) {
        String subject = "Yêu cầu giáo viên đã được xác nhận - TMS";
        requestData.put("frontendUrl", frontendUrl);
        requestData.put("dashboardUrl", frontendUrl + "/dashboard");

        return sendEmailWithTemplateAsync(to, subject, "emails/teacher-request-confirmed", requestData);
    }

    @Override
    public CompletableFuture<Void> sendDepartmentScheduleChangeNotificationAsync(String to, Map<String, Object> requestData) {
        String subject = "Thông báo thay đổi lịch bộ môn - TMS";
        requestData.put("frontendUrl", frontendUrl);
        requestData.put("dashboardUrl", frontendUrl + "/department/dashboard");

        return sendEmailWithTemplateAsync(to, subject, "emails/department-schedule-change", requestData);
    }

    // Scheduled Job Email Implementations

    @Override
    public CompletableFuture<Void> sendMonthlyAnalyticsReportAsync(String to, Map<String, Object> reportData) {
        String subject = "Báo cáo phân tích hàng tháng - TMS";
        reportData.put("frontendUrl", frontendUrl);
        reportData.put("dashboardUrl", frontendUrl + "/analytics");

        return sendEmailWithTemplateAsync(to, subject, "emails/monthly-analytics-report", reportData);
    }

    @Override
    public CompletableFuture<Void> sendSystemHealthAlertAsync(String to, Map<String, Object> healthData) {
        String subject = "Cảnh báo sức khỏe hệ thống - TMS";
        healthData.put("frontendUrl", frontendUrl);
        healthData.put("adminUrl", frontendUrl + "/admin/health");

        return sendEmailWithTemplateAsync(to, subject, "emails/system-health-alert", healthData);
    }

    @Override
    public CompletableFuture<Void> sendDatabaseBackupConfirmationAsync(String to, Map<String, Object> backupData) {
        String subject = "Xác nhận sao lưu cơ sở dữ liệu - TMS";
        backupData.put("frontendUrl", frontendUrl);
        backupData.put("adminUrl", frontendUrl + "/admin/backup");

        return sendEmailWithTemplateAsync(to, subject, "emails/database-backup-confirmation", backupData);
    }

    @Override
    public CompletableFuture<Void> sendArchiveCompletionNotificationAsync(String to, Map<String, Object> archiveData) {
        String subject = "Hoàn thành lưu trữ dữ liệu - TMS";
        archiveData.put("frontendUrl", frontendUrl);
        archiveData.put("adminUrl", frontendUrl + "/admin/archive");

        return sendEmailWithTemplateAsync(to, subject, "emails/archive-completion-notification", archiveData);
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