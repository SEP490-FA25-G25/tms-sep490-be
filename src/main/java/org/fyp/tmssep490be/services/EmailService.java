package org.fyp.tmssep490be.services;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for sending emails using Resend API
 * Supports Vietnamese content with UTF-8 encoding
 */
public interface EmailService {

    /**
     * Send email asynchronously
     */
    CompletableFuture<Void> sendEmailAsync(String to, String subject, String htmlContent);

    /**
     * Send email with template
     */
    CompletableFuture<Void> sendEmailWithTemplateAsync(String to, String subject, String templateName, Map<String, Object> templateData);

    /**
     * Send email to multiple recipients
     */
    CompletableFuture<Void> sendBulkEmailAsync(List<String> recipients, String subject, String htmlContent);

    /**
     * Send welcome email for new user registration
     */
    CompletableFuture<Void> sendWelcomeEmailAsync(String to, String userName, String verificationLink);

    /**
     * Send password reset email
     */
    CompletableFuture<Void> sendPasswordResetEmailAsync(String to, String userName, String resetLink);

    /**
     * Send class enrollment notification
     */
    CompletableFuture<Void> sendClassEnrollmentNotificationAsync(String to, String studentName, String className, String centerName);

    /**
     * Send grade notification
     */
    CompletableFuture<Void> sendGradeNotificationAsync(String to, String studentName, String assessmentName, String grade);

    /**
     * Send schedule reminder
     */
    CompletableFuture<Void> sendScheduleReminderAsync(String to, String studentName, String className, String sessionDate, String sessionTime);

    /**
     * Send transfer request notification to academic affairs
     */
    CompletableFuture<Void> sendTransferRequestNotificationAsync(String to, String studentName, String currentClass, String requestedClass);

    /**
     * Send makeup request notification
     */
    CompletableFuture<Void> sendMakeupRequestNotificationAsync(String to, String studentName, String missedSession, String requestedDate);

    /**
     * Send attendance alert
     */
    CompletableFuture<Void> sendAttendanceAlertAsync(String to, String studentName, String className, Double attendanceRate);

    /**
     * Send feedback reminder
     */
    CompletableFuture<Void> sendFeedbackReminderAsync(String to, String studentName, String className);

    /**
     * Send license expiry warning
     */
    CompletableFuture<Void> sendLicenseExpiryWarningAsync(String to, String resourceName, String expiryDate, String centerName);

    /**
     * Send pending request reminder to academic affairs
     */
    CompletableFuture<Void> sendPendingRequestReminderAsync(String to, int pendingCount, String oldestRequestDate);

    /**
     * Send weekly attendance report
     */
    CompletableFuture<Void> sendWeeklyAttendanceReportAsync(String to, Map<String, Object> reportData);

    // Student Request Email Methods

    /**
     * Send student request approval notification
     */
    CompletableFuture<Void> sendStudentRequestApprovalAsync(String to, Map<String, Object> requestData);

    /**
     * Send student request rejection notification
     */
    CompletableFuture<Void> sendStudentRequestRejectionAsync(String to, Map<String, Object> requestData);

    /**
     * Send student request confirmation email when created
     */
    CompletableFuture<Void> sendStudentRequestCreatedAsync(String to, Map<String, Object> requestData);

    /**
     * Send student request confirmed notification
     */
    CompletableFuture<Void> sendStudentRequestConfirmedAsync(String to, Map<String, Object> requestData);

    /**
     * Send teacher transfer schedule notification
     */
    CompletableFuture<Void> sendTeacherTransferNotificationAsync(String to, Map<String, Object> requestData);

    // Teacher Request Email Methods

    /**
     * Send teacher request approval notification
     */
    CompletableFuture<Void> sendTeacherRequestApprovalAsync(String to, Map<String, Object> requestData);

    /**
     * Send teacher request rejection notification
     */
    CompletableFuture<Void> sendTeacherRequestRejectionAsync(String to, Map<String, Object> requestData);

    /**
     * Send teacher request confirmation email when created
     */
    CompletableFuture<Void> sendTeacherRequestCreatedAsync(String to, Map<String, Object> requestData);

    /**
     * Send teacher request confirmed notification
     */
    CompletableFuture<Void> sendTeacherRequestConfirmedAsync(String to, Map<String, Object> requestData);

    /**
     * Send department schedule change notification
     */
    CompletableFuture<Void> sendDepartmentScheduleChangeNotificationAsync(String to, Map<String, Object> requestData);

    // Scheduled Job Email Methods

    /**
     * Send monthly analytics report
     */
    CompletableFuture<Void> sendMonthlyAnalyticsReportAsync(String to, Map<String, Object> reportData);

    /**
     * Send system health alert
     */
    CompletableFuture<Void> sendSystemHealthAlertAsync(String to, Map<String, Object> healthData);

    /**
     * Send database backup confirmation
     */
    CompletableFuture<Void> sendDatabaseBackupConfirmationAsync(String to, Map<String, Object> backupData);

    /**
     * Send archive completion notification
     */
    CompletableFuture<Void> sendArchiveCompletionNotificationAsync(String to, Map<String, Object> archiveData);
}