package org.fyp.tmssep490be.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.report.StudentAttendanceAlertDTO;
import org.fyp.tmssep490be.dtos.report.WeeklyAttendanceReportDTO;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.enums.NotificationType;
import org.fyp.tmssep490be.repositories.EnrollmentRepository;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.fyp.tmssep490be.services.EmailService;
import org.fyp.tmssep490be.services.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Job: Weekly Attendance Report
// Tạo báo cáo chuyên cần hàng tuần và cảnh báo học viên có tỷ lệ chuyên cần thấp.
//
// - Tính chuyên cần theo lớp trong tuần (từ tuầnStart đến tuầnEnd)
// - Gửi notification + email báo cáo cho Academic Affairs
// - Gửi notification cảnh báo cho học viên có attendance rate < threshold
//
// Mặc định chạy Chủ nhật 18:00 (có thể override bằng cấu hình).
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

    // Map raw data -> DTO cho báo cáo lớp
    private List<WeeklyAttendanceReportDTO> mapToWeeklyAttendanceReportDTO(List<Object[]> rawData) {
        return rawData.stream()
                .map(row -> new WeeklyAttendanceReportDTO(
                        (Long) row[0],      // classId
                        (String) row[1],    // className
                        (Long) row[2],      // totalSessions
                        (Long) row[3],      // presentSessions
                        (Long) row[4],      // absentSessions
                        row[5] != null ? ((Number) row[5]).doubleValue() : null // attendanceRate
                ))
                .collect(Collectors.toList());
    }

    // Map raw data -> DTO cho cảnh báo học viên
    private List<StudentAttendanceAlertDTO> mapToStudentAttendanceAlertDTO(List<Object[]> rawData) {
        return rawData.stream()
                .map(row -> new StudentAttendanceAlertDTO(
                        (Long) row[0],      // studentId
                        (String) row[1],    // studentName
                        (String) row[2],    // email
                        (String) row[3],    // className
                        (Long) row[4],      // presentCount
                        (Long) row[5],      // totalCount
                        row[6] != null ? ((Number) row[6]).doubleValue() : null // attendanceRate
                ))
                .collect(Collectors.toList());
    }

    // Chạy mặc định: Chủ nhật 18:00
    @Scheduled(cron = "${tms.scheduler.jobs.weekly-attendance-report.cron:0 0 18 * * SUN}")
    @Transactional
    public void generateWeeklyAttendanceReport() {
        String jobName = "WeeklyAttendanceReportJob";
        logJobStart(jobName);

        try {
            LocalDate weekEnd = LocalDate.now();
            LocalDate weekStart = weekEnd.minusDays(6);

            logJobInfo(String.format("Generating attendance report for week %s to %s", weekStart, weekEnd));

            // Lấy dữ liệu chuyên cần theo lớp
            List<WeeklyAttendanceReportDTO> classReports = mapToWeeklyAttendanceReportDTO(
                    enrollmentRepository.findWeeklyAttendanceRawData(weekStart, weekEnd));

            // Học viên có tỷ lệ chuyên cần thấp
            List<StudentAttendanceAlertDTO> lowAttendanceStudents = mapToStudentAttendanceAlertDTO(
                    enrollmentRepository.findStudentsWithLowAttendanceRawData(weekStart, weekEnd, lowAttendanceThreshold));

            // Lấy Academic Affairs để gửi báo cáo
            List<UserAccount> academicAffairsUsers = userAccountRepository.findUsersByRole("ACADEMIC_AFFAIR");

            if (academicAffairsUsers.isEmpty()) {
                logJobWarning("No Academic Affairs users found to send weekly attendance report to");
            }

            int totalNotificationsSent = 0;

            // Gửi báo cáo cho Academic Affairs
            for (UserAccount aaUser : academicAffairsUsers) {
                try {
                    String reportSummary = String.format(
                            "Báo cáo điểm danh tuần %s - %s:\n" +
                                    "- %d lớp có buổi học trong tuần\n" +
                                    "- %d học viên có tỷ lệ chuyên cần thấp (< %.1f%%)",
                            weekStart, weekEnd, classReports.size(), lowAttendanceStudents.size(), lowAttendanceThreshold
                    );

                    notificationService.createNotification(
                            aaUser.getId(),
                            NotificationType.SYSTEM,
                            "Báo cáo điểm danh hàng tuần",
                            reportSummary
                    );
                    totalNotificationsSent++;

                    // Gửi email chi tiết (nếu có email)
                    sendWeeklyAttendanceReportEmail(
                            aaUser.getEmail(),
                            classReports,
                            lowAttendanceStudents,
                            weekStart,
                            weekEnd
                    );

                } catch (Exception e) {
                    logJobWarning(String.format("Failed to send weekly report to AA user %d: %s",
                            aaUser.getId(), e.getMessage()));
                }
            }

            // Gửi cảnh báo cho học viên có tỷ lệ chuyên cần thấp
            for (StudentAttendanceAlertDTO student : lowAttendanceStudents) {
                try {
                    String message = String.format(
                            "Tỷ lệ chuyên cần của bạn tuần này cho lớp %s là %.1f%%. Vui lòng cải thiện sự chuyên cần.",
                            student.getClassName(),
                            student.getAttendanceRate()
                    );

                    notificationService.createNotification(
                            student.getStudentId(),
                            NotificationType.REMINDER,
                            "Cảnh báo chuyên cần thấp",
                            message
                    );
                    totalNotificationsSent++;
                } catch (Exception e) {
                    logJobWarning(String.format("Failed to send low attendance alert to student %d: %s",
                            student.getStudentId(), e.getMessage()));
                }
            }

            logJobEnd(jobName, String.format(
                    "Processed %d class reports, %d low-attendance students, sent %d notifications",
                    classReports.size(), lowAttendanceStudents.size(), totalNotificationsSent));

        } catch (Exception e) {
            logJobError(jobName, e);
            throw e;
        }
    }

    // Gửi email báo cáo điểm danh hàng tuần cho Academic Affairs
    private void sendWeeklyAttendanceReportEmail(
            String email,
            List<WeeklyAttendanceReportDTO> classReports,
            List<StudentAttendanceAlertDTO> lowAttendanceStudents,
            LocalDate weekStart,
            LocalDate weekEnd
    ) {
        try {
            if (email == null || email.trim().isEmpty()) {
                logJobWarning("Cannot send weekly report email - recipient email is null or empty");
                return;
            }

            Map<String, Object> reportData = new HashMap<>();

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String periodLabel = String.format("%s - %s",
                    weekStart.format(fmt),
                    weekEnd.format(fmt));

            // Summary
            int totalSessions = classReports.stream()
                    .mapToInt(r -> r.getTotalSessions().intValue())
                    .sum();
            double overallAttendanceRate = classReports.stream()
                    .filter(r -> r.getAttendanceRate() != null)
                    .mapToDouble(WeeklyAttendanceReportDTO::getAttendanceRate)
                    .average()
                    .orElse(0.0);
            int totalAbsences = classReports.stream()
                    .mapToInt(r -> r.getAbsentSessions().intValue())
                    .sum();

            reportData.put("reportPeriod", periodLabel);
            reportData.put("totalSessions", totalSessions);
            reportData.put("attendanceRate", String.format("%.1f", overallAttendanceRate));
            reportData.put("absentCount", totalAbsences);

            // Chi tiết từng lớp
            List<Map<String, Object>> classDetails = new ArrayList<>();
            for (WeeklyAttendanceReportDTO classReport : classReports) {
                Map<String, Object> classInfo = new HashMap<>();
                classInfo.put("className", classReport.getClassName());
                classInfo.put("totalSessions", classReport.getTotalSessions());
                classInfo.put("presentCount", classReport.getPresentSessions());
                classInfo.put("absentCount", classReport.getAbsentSessions());
                classInfo.put("attendanceRate", classReport.getAttendanceRate() != null
                        ? String.format("%.1f", classReport.getAttendanceRate())
                        : "N/A");
                classDetails.add(classInfo);
            }
            reportData.put("classDetails", classDetails);

            // Học viên chuyên cần thấp
            List<Map<String, Object>> lowAttendanceList = new ArrayList<>();
            for (StudentAttendanceAlertDTO student : lowAttendanceStudents) {
                Map<String, Object> studentInfo = new HashMap<>();
                studentInfo.put("studentName", student.getStudentName());
                studentInfo.put("className", student.getClassName());
                studentInfo.put("totalSessions", student.getTotalCount());
                studentInfo.put("presentCount", student.getPresentCount());
                long absentCount = student.getTotalCount() - student.getPresentCount();
                studentInfo.put("absentCount", absentCount);
                studentInfo.put("attendanceRate", student.getAttendanceRate() != null
                        ? String.format("%.1f", student.getAttendanceRate())
                        : "N/A");
                lowAttendanceList.add(studentInfo);
            }
            reportData.put("lowAttendanceStudents", lowAttendanceList);

            // Dùng template email tổng hợp (bạn có thể tạo template sau, tạm thời gửi HTML đơn giản)
            String subject = "Báo cáo điểm danh hàng tuần";

            StringBuilder html = new StringBuilder();
            html.append("<h3>Báo cáo điểm danh tuần ").append(periodLabel).append("</h3>");
            html.append("<p>Tổng số buổi học: ").append(totalSessions)
                    .append(", vắng: ").append(totalAbsences)
                    .append(", tỷ lệ chuyên cần trung bình: ").append(String.format("%.1f", overallAttendanceRate))
                    .append("%</p>");

            html.append("<h4>Chi tiết theo lớp</h4><ul>");
            for (Map<String, Object> c : classDetails) {
                html.append("<li>Lớp ")
                        .append(c.get("className"))
                        .append(": ")
                        .append(c.get("attendanceRate"))
                        .append("% (")
                        .append(c.get("presentCount"))
                        .append("/")
                        .append(c.get("totalSessions"))
                        .append(" buổi có mặt)</li>");
            }
            html.append("</ul>");

            if (!lowAttendanceList.isEmpty()) {
                html.append("<h4>Học viên có chuyên cần thấp</h4><ul>");
                for (Map<String, Object> s : lowAttendanceList) {
                    html.append("<li>")
                            .append(s.get("studentName"))
                            .append(" - ")
                            .append(s.get("className"))
                            .append(": ")
                            .append(s.get("attendanceRate"))
                            .append("%</li>");
                }
                html.append("</ul>");
            }

            emailService.sendEmailAsync(email, subject, html.toString());
        } catch (Exception e) {
            logJobWarning("Failed to send weekly attendance report email: " + e.getMessage());
        }
    }
}


