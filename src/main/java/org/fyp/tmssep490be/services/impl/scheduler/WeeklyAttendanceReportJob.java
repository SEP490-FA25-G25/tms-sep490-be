package org.fyp.tmssep490be.services.impl.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.StudentAttendanceAlertDTO;
import org.fyp.tmssep490be.dtos.WeeklyAttendanceReportDTO;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.repositories.EnrollmentRepository;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.fyp.tmssep490be.services.NotificationService;
import org.fyp.tmssep490be.entities.enums.NotificationType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

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
}