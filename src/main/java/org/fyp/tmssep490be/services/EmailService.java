package org.fyp.tmssep490be.services;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.config.EmailConfig;
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
public class EmailService {

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

    @Async
    public void sendEmailAsync(String to, String subject, String htmlContent) {
        if (mailSender == null) {
            log.warn("Email service is not configured. Skipping email to: {}", to);
            CompletableFuture.completedFuture(null);
            return;
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
            CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            CompletableFuture.failedFuture(e);
        }
    }

    public void sendNewStudentCredentialsAsync(String to, String studentName, String studentCode,
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

        sendEmailWithTemplateAsync(to, subject, "emails/new-student-credentials", templateData);
    }

    public void sendNewUserCredentialsAsync(String to, String fullName, String email, String password, String branchName) {
        String subject = "Chào mừng đến với Hệ thống TMS - Thông tin đăng nhập";
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("fullName", fullName);
        templateData.put("email", email);
        templateData.put("password", password);
        templateData.put("branchName", branchName);
        templateData.put("loginUrl", frontendUrl + "/login");

        sendEmailWithTemplateAsync(to, subject, "emails/new-useraccount-credentials", templateData);
    }
    public void sendEmailWithTemplateAsync(String to, String subject, String templateName, Map<String, Object> templateData) {
        try {
            Context context = new Context();
            context.setVariables(templateData);

            String htmlContent = templateEngine.process(templateName, context);
            sendEmailAsync(to, subject, htmlContent);

        } catch (Exception e) {
            log.error("Failed to process email template {} for recipient {}: {}", templateName, to, e.getMessage(), e);
            CompletableFuture.failedFuture(e);
        }
    }

    public void sendClassEnrollmentNotificationAsync(String to, String studentName, String className, String centerName) {
        String subject = "Xác nhận đăng ký lớp học thành công";
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("studentName", studentName);
        templateData.put("className", className);
        templateData.put("centerName", centerName);
        templateData.put("frontendUrl", frontendUrl);

        sendEmailWithTemplateAsync(to, subject, "emails/class-enrollment", templateData);
    }


    public void sendFeedbackReminderAsync(String to, String studentName, String className) {
        String subject = "Nhắc nhở: Đánh giá khóa học";
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("studentName", studentName);
        templateData.put("className", className);
        templateData.put("feedbackUrl", frontendUrl + "/student/feedbacks");
        templateData.put("frontendUrl", frontendUrl);

        sendEmailWithTemplateAsync(to, subject, "emails/feedback-reminder", templateData);
    }

    public void sendPasswordResetEmailAsync(String to, String fullName, String resetLink) {
        String subject = "Đặt lại mật khẩu - Hệ thống TMS";
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("fullName", fullName);
        templateData.put("resetLink", resetLink);
        templateData.put("frontendUrl", frontendUrl);

        sendEmailWithTemplateAsync(to, subject, "emails/password-reset", templateData);
    }

    // Gửi email nhắc nhở Academic Affairs về số lượng yêu cầu pending
    // - pendingCount: tổng số yêu cầu student + teacher đang chờ duyệt
    // - oldestRequestDate: ngày sớm nhất mà job đang nhắc tới (string yyyy-MM-dd)
    public void sendPendingRequestReminderAsync(String to, int pendingCount, String oldestRequestDate) {
        String subject = "Nhắc nhở xử lý yêu cầu đang chờ";
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("pendingCount", pendingCount);
        templateData.put("oldestRequestDate", oldestRequestDate);
        templateData.put("frontendUrl", frontendUrl);

        sendEmailWithTemplateAsync(to, subject, "emails/pending-request-reminder", templateData);
    }

    // Gửi email thông báo student request được phê duyệt
    public void sendStudentRequestApprovedAsync(String to, String studentName, String requestType,
                                                 String className, String sessionInfo,
                                                 String makeupSessionInfo, String targetClassInfo,
                                                 String decidedBy, String decidedAt, String note) {
        String subject = "Yêu cầu của bạn đã được phê duyệt";
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("studentName", studentName);
        templateData.put("requestType", requestType);
        templateData.put("className", className);
        templateData.put("sessionInfo", sessionInfo);
        templateData.put("makeupSessionInfo", makeupSessionInfo);
        templateData.put("targetClassInfo", targetClassInfo);
        templateData.put("decidedBy", decidedBy);
        templateData.put("decidedAt", decidedAt);
        templateData.put("note", note);
        templateData.put("requestUrl", frontendUrl + "/student/requests");
        templateData.put("frontendUrl", frontendUrl);

        sendEmailWithTemplateAsync(to, subject, "emails/student-request-approved", templateData);
    }

    // Gửi email thông báo student request bị từ chối
    public void sendStudentRequestRejectedAsync(String to, String studentName, String requestType,
                                                 String className, String sessionInfo,
                                                 String decidedBy, String decidedAt, String rejectionReason) {
        String subject = "Yêu cầu của bạn đã bị từ chối";
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("studentName", studentName);
        templateData.put("requestType", requestType);
        templateData.put("className", className);
        templateData.put("sessionInfo", sessionInfo);
        templateData.put("decidedBy", decidedBy);
        templateData.put("decidedAt", decidedAt);
        templateData.put("rejectionReason", rejectionReason);
        templateData.put("requestUrl", frontendUrl + "/student/requests");
        templateData.put("frontendUrl", frontendUrl);

        sendEmailWithTemplateAsync(to, subject, "emails/student-request-rejected", templateData);
    }

    // Gửi email thông báo điểm số được cập nhật
    public void sendGradeUpdatedAsync(String to, String studentName, String className,
                                      String assessmentName, String phaseName, String teacherName,
                                      String score, String maxScore, String comment, String updatedAt) {
        String subject = "Điểm số đã được cập nhật - " + assessmentName;
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("studentName", studentName);
        templateData.put("className", className);
        templateData.put("assessmentName", assessmentName);
        templateData.put("phaseName", phaseName);
        templateData.put("teacherName", teacherName);
        templateData.put("score", score);
        templateData.put("maxScore", maxScore);
        templateData.put("comment", comment);
        templateData.put("updatedAt", updatedAt);
        templateData.put("gradesUrl", frontendUrl + "/student/grades");
        templateData.put("frontendUrl", frontendUrl);

        sendEmailWithTemplateAsync(to, subject, "emails/grade-updated", templateData);
    }

    // Gửi email cảnh báo về tình trạng vắng nhiều
    public void sendAttendanceWarningAsync(String to, String studentName, String className,
                                           String teacherName, String period,
                                           int absentCount, int totalSessions,
                                           String attendanceRate, int remainingAllowedAbsent) {
        String subject = "Cảnh báo: Tình trạng điểm danh của bạn";
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("studentName", studentName);
        templateData.put("className", className);
        templateData.put("teacherName", teacherName);
        templateData.put("period", period);
        templateData.put("absentCount", absentCount);
        templateData.put("totalSessions", totalSessions);
        templateData.put("attendanceRate", attendanceRate);
        templateData.put("remainingAllowedAbsent", remainingAllowedAbsent);
        templateData.put("attendanceUrl", frontendUrl + "/student/attendance");
        templateData.put("frontendUrl", frontendUrl);

        sendEmailWithTemplateAsync(to, subject, "emails/attendance-warning", templateData);
    }

}
