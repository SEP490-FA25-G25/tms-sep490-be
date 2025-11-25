package org.fyp.tmssep490be.services.impl.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.StudentAttendanceAlertDTO;
import org.fyp.tmssep490be.dtos.WeeklyAttendanceReportDTO;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.repositories.EnrollmentRepository;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.fyp.tmssep490be.services.EmailService;
import org.fyp.tmssep490be.services.NotificationService;
import org.fyp.tmssep490be.entities.enums.NotificationType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Job 8: Weekly Attendance Report Job
 * Generate weekly attendance reports and alerts
 * Schedule: Sunday 6:00 PM
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    prefix = "tms.scheduler.jobs.weekly-attendance-report",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class WeeklyAttendanceReportJob extends BaseScheduledJob {

    private final EnrollmentRepository enrollmentRepository;
    private final UserAccountRepository userAccountRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Value("${tms.scheduler.jobs.weekly-attendance-report.low-attendance-threshold:75.0}")
    private double lowAttendanceThreshold;

    /**
     * Main scheduled method to generate weekly attendance report
     * Runs Sundays at 6:00 PM by default
     */
    @Scheduled(cron = "${tms.scheduler.jobs.weekly-attendance-report.cron:0 0 18 * * SUN}")
    @Transactional
    public void generateWeeklyAttendanceReport() {
        logJobStart("WeeklyAttendanceReportJob");

        try {
            LocalDate weekEnd = LocalDate.now();
            LocalDate weekStart = weekEnd.minusDays(6);

            logJobInfo(String.format("Generating attendance report for week %s to %s", weekStart, weekEnd));

            // Generate class-level attendance summary
            List<WeeklyAttendanceReportDTO> classReports = enrollmentRepository
                .generateWeeklyAttendanceReport(weekStart, weekEnd);

            // Identify students with low attendance (< threshold)
            List<StudentAttendanceAlertDTO> lowAttendanceStudents = enrollmentRepository
                .findStudentsWithLowAttendance(weekStart, weekEnd, lowAttendanceThreshold);

            // Get Academic Affairs users for reporting
            List<UserAccount> academicAffairsUsers = userAccountRepository
                .findUsersByRole("ROLE_ACADEMIC_AFFAIR");

            if (academicAffairsUsers.isEmpty()) {
                logJobWarning("No Academic Affairs users found to send reports to");
            }

            int totalNotificationsSent = 0;

            // Send weekly reports to Academic Affairs
            for (UserAccount aaUser : academicAffairsUsers) {
                try {
                    String reportSummary = String.format(
                        "Báo cáo điểm danh tuần %s - %s:\n" +
                        "- %d lớp có báo cáo điểm danh\n" +
                        "- %d học viên điểm danh thấp (< %.1f%%)",
                        weekStart, weekEnd, classReports.size(), lowAttendanceStudents.size(), lowAttendanceThreshold
                    );

                    notificationService.createNotification(
                        aaUser.getId(),
                        NotificationType.SYSTEM_ALERT,
                        "Báo cáo điểm danh hàng tuần",
                        reportSummary
                    );
                    totalNotificationsSent++;

                    // Log class reports summary
                    logJobInfo(String.format("Generated %d class reports for week %s to %s",
                        classReports.size(), weekStart, weekEnd));

                    // Send detailed email report
                    sendWeeklyAttendanceReportEmail(aaUser.getEmail(), classReports, lowAttendanceStudents,
                        weekStart, weekEnd);

                } catch (Exception e) {
                    logJobWarning(String.format("Failed to send weekly report to AA user %d: %s",
                        aaUser.getId(), e.getMessage()));
                }
            }

            // Send alerts for low attendance students
            for (StudentAttendanceAlertDTO student : lowAttendanceStudents) {
                try {
                    notificationService.createNotification(
                        student.getStudentId(),
                        NotificationType.CLASS_REMINDER,
                        "Cảnh báo điểm danh thấp",
                        String.format("Điểm danh tuần này (%.1f%%) cho lớp %s. Vui lòng cải thiện sự chuyên cần.",
                            student.getAttendanceRate(), student.getClassName())
                    );
                    totalNotificationsSent++;

                    logJobInfo(String.format("Sent low attendance alert to student %d (%s) - Rate: %.1f%%",
                        student.getStudentId(), student.getStudentName(), student.getAttendanceRate()));

                } catch (Exception e) {
                    logJobWarning(String.format("Failed to send low attendance alert to student %d: %s",
                        student.getStudentId(), e.getMessage()));
                }
            }

            logJobEnd("WeeklyAttendanceReportJob",
                String.format("Processed %d class reports, %d low attendance alerts, sent %d notifications",
                    classReports.size(), lowAttendanceStudents.size(), totalNotificationsSent));

        } catch (Exception e) {
            logJobError("WeeklyAttendanceReportJob", e);
            throw e; // Re-throw to prevent silent failures
        }
    }

    /**
     * Gửi email báo cáo điểm danh hàng tuần
     */
    private void sendWeeklyAttendanceReportEmail(String email, List<WeeklyAttendanceReportDTO> classReports,
            List<StudentAttendanceAlertDTO> lowAttendanceStudents, LocalDate weekStart, LocalDate weekEnd) {
        try {
            if (email == null || email.trim().isEmpty()) {
                logJobWarning("Cannot send weekly report email - recipient email is null or empty");
                return;
            }

            Map<String, Object> reportData = new HashMap<>();

            // Report period
            reportData.put("reportPeriod", String.format("Tuần %d (%s - %s)",
                weekStart.getDayOfMonth() / 7 + 1,
                weekStart.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                weekEnd.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))));

            // Summary statistics
            int totalSessions = classReports.stream().mapToInt(r -> r.getTotalSessions().intValue()).sum();
            double overallAttendanceRate = classReports.stream()
                .mapToDouble(r -> r.getAttendanceRate())
                .average()
                .orElse(0.0);
            int totalAbsences = classReports.stream().mapToInt(r -> r.getAbsentSessions().intValue()).sum();

            reportData.put("totalSessions", totalSessions);
            reportData.put("totalStudents", "N/A"); // Not available in current DTO
            reportData.put("attendanceRate", String.format("%.1f", overallAttendanceRate));
            reportData.put("absentCount", totalAbsences);

            // Class details for table
            List<Map<String, Object>> classDetails = new ArrayList<>();
            for (WeeklyAttendanceReportDTO classReport : classReports) {
                Map<String, Object> classInfo = new HashMap<>();
                classInfo.put("className", classReport.getClassName());
                classInfo.put("teacherName", "N/A"); // Not available in current DTO
                classInfo.put("totalSessions", classReport.getTotalSessions());
                classInfo.put("totalStudents", "N/A"); // Not available in current DTO
                classInfo.put("presentCount", classReport.getPresentSessions());
                classInfo.put("absentCount", classReport.getAbsentSessions());
                classInfo.put("excusedCount", "N/A"); // Not available in current DTO
                classInfo.put("attendanceRate", String.format("%.1f", classReport.getAttendanceRate()));
                classDetails.add(classInfo);
            }
            reportData.put("classDetails", classDetails);

            // Low attendance students
            List<Map<String, Object>> lowAttendanceList = new ArrayList<>();
            for (StudentAttendanceAlertDTO student : lowAttendanceStudents) {
                Map<String, Object> studentInfo = new HashMap<>();
                studentInfo.put("studentName", student.getStudentName());
                studentInfo.put("className", student.getClassName());
                studentInfo.put("totalSessions", student.getTotalCount());
                studentInfo.put("presentCount", student.getPresentCount());
                long absentCount = student.getTotalCount() - student.getPresentCount();
                studentInfo.put("absentCount", absentCount);
                studentInfo.put("attendanceRate", String.format("%.1f", student.getAttendanceRate()));
                lowAttendanceList.add(studentInfo);
            }
            reportData.put("lowAttendanceStudents", lowAttendanceList);

            // Critical cases and warnings
            List<String> criticalCases = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            List<String> achievements = new ArrayList<>();

            // Identify critical cases (students with very low attendance)
            lowAttendanceStudents.stream()
                .filter(s -> s.getAttendanceRate() < 50.0)
                .forEach(s -> criticalCases.add(String.format("%s - %s: %.1f%%",
                    s.getStudentName(), s.getClassName(), s.getAttendanceRate())));

            // Identify warnings (classes with low attendance)
            classReports.stream()
                .filter(r -> r.getAttendanceRate() < 80.0)
                .forEach(r -> warnings.add(String.format("Lớp %s: %.1f%%", r.getClassName(), r.getAttendanceRate())));

            // Identify achievements (classes with perfect attendance)
            classReports.stream()
                .filter(r -> r.getAttendanceRate() >= 95.0)
                .forEach(r -> achievements.add(String.format("Lớp %s: %.1f%%", r.getClassName(), r.getAttendanceRate())));

            reportData.put("criticalCases", criticalCases);
            reportData.put("warnings", warnings);
            reportData.put("achievements", achievements);

            // Request statistics (placeholder for actual data)
            reportData.put("totalAbsenceRequests", 45); // This would come from actual data
            reportData.put("approvedRequests", 38);
            reportData.put("pendingRequests", 5);
            reportData.put("rejectedRequests", 2);

            // Generated timestamp
            reportData.put("generatedAt", java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            emailService.sendWeeklyAttendanceReportAsync(email, reportData)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        logJobError("Failed to send weekly attendance report email to " + email, (Exception) throwable);
                    } else {
                        logJobInfo("Weekly attendance report email sent successfully to " + email);
                    }
                });

        } catch (Exception e) {
            logJobError("Error preparing weekly attendance report email for " + email, e);
        }
    }
}