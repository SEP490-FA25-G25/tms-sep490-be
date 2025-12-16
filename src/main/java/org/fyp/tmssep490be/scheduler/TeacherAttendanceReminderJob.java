package org.fyp.tmssep490be.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.StudentSession;
import org.fyp.tmssep490be.entities.TeachingSlot;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.NotificationType;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.fyp.tmssep490be.repositories.TeachingSlotRepository;
import org.fyp.tmssep490be.services.EmailService;
import org.fyp.tmssep490be.services.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

// Job: Teacher Attendance Reminder
// Nhắc nhở giáo viên điểm danh và nộp báo cáo sau buổi học.
//
// Kiểm tra:
// 1) Buổi học sắp kết thúc trong X phút nữa mà chưa điểm danh -> nhắc giáo viên.
// 2) Buổi học kết thúc khoảng 24 giờ trước mà chưa điểm danh -> nhắc lại (mức 2).
// 3) Buổi học kết thúc khoảng 36 giờ trước mà chưa điểm danh -> nhắc lần cuối (mức 3).
// 4) Buổi học kết thúc khoảng 1 giờ trước mà chưa có teacher note (báo cáo buổi học) -> nhắc nộp báo cáo.
//
// Lịch chạy mặc định: mỗi 5 phút (có thể override bằng cấu hình).
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        prefix = "tms.scheduler.jobs.teacher-attendance-reminder",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class TeacherAttendanceReminderJob extends BaseScheduledJob {

    private final SessionRepository sessionRepository;
    private final TeachingSlotRepository teachingSlotRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Value("${tms.scheduler.jobs.teacher-attendance-reminder.warning-minutes-before-end:10}")
    private int warningMinutesBeforeEnd;

    // Chạy mặc định 5 phút/lần
    @Scheduled(cron = "${tms.scheduler.jobs.teacher-attendance-reminder.cron:0 */5 * * * *}")
    @Transactional
    public void checkAndSendAttendanceReminders() {
        String jobName = "TeacherAttendanceReminderJob";
        logJobStart(jobName);

        try {
            LocalDateTime now = LocalDateTime.now();
            int notificationsSent = 0;

            // 1. Buổi học sắp kết thúc
            notificationsSent += checkSessionsEndingSoon(now);

            // 2. Buổi học kết thúc ~24h trước
            notificationsSent += checkSessionsEnded24HoursAgo(now);

            // 3. Buổi học kết thúc ~36h trước
            notificationsSent += checkSessionsEnded36HoursAgo(now);

            // 4. Buổi học kết thúc ~1h trước mà chưa có teacher note
            notificationsSent += checkSessionsEnded1HourAgoForReport(now);

            logJobEnd(jobName, String.format("Sent %d attendance/report reminder notifications", notificationsSent));
        } catch (Exception e) {
            logJobError(jobName, e);
            throw e;
        }
    }

    // 1) Nhắc buổi học sắp kết thúc (ví dụ 10 phút nữa) nếu chưa điểm danh
    private int checkSessionsEndingSoon(LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        LocalTime warningTime = now.toLocalTime().plusMinutes(warningMinutesBeforeEnd);

        List<Session> sessionsToday = sessionRepository.findByDate(today);

        int notificationsSent = 0;
        for (Session session : sessionsToday) {
            if (session.getTimeSlotTemplate() == null
                    || session.getTimeSlotTemplate().getEndTime() == null
                    || session.getStatus() == SessionStatus.CANCELLED) {
                continue;
            }

            LocalTime sessionEndTime = session.getTimeSlotTemplate().getEndTime();
            LocalTime currentTime = now.toLocalTime();

            // Session kết thúc sau thời điểm hiện tại và trước (hoặc bằng) warningTime
            if (sessionEndTime.isAfter(currentTime)
                    && (sessionEndTime.isBefore(warningTime) || sessionEndTime.equals(warningTime))) {

                if (!isAttendanceSubmitted(session.getId())) {
                    String classCode = session.getClassEntity() != null ? session.getClassEntity().getCode() : "N/A";
                    String formattedDate = session.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                    String title = "Nhắc nhở: Buổi học sắp kết thúc";
                    String message = String.format(
                            "Buổi học %s ngày %s sẽ kết thúc trong %d phút. Vui lòng hoàn thành điểm danh cho học viên.",
                            classCode, formattedDate, warningMinutesBeforeEnd);

                    sendReminderToTeachers(session, title, message);
                    notificationsSent++;
                }
            }
        }

        return notificationsSent;
    }

    // 2) Session kết thúc ~24h trước mà chưa điểm danh
    private int checkSessionsEnded24HoursAgo(LocalDateTime now) {
        LocalDateTime twentyFourHoursAgo = now.minusHours(24);
        LocalDate checkDate = twentyFourHoursAgo.toLocalDate();

        List<Session> sessions = sessionRepository.findByDate(checkDate);

        int notificationsSent = 0;
        for (Session session : sessions) {
            if (session.getTimeSlotTemplate() == null
                    || session.getTimeSlotTemplate().getEndTime() == null
                    || session.getStatus() == SessionStatus.CANCELLED) {
                continue;
            }

            LocalTime sessionEndTime = session.getTimeSlotTemplate().getEndTime();
            LocalDateTime sessionEndDateTime = LocalDateTime.of(checkDate, sessionEndTime);

            // Trong cửa sổ 5 phút quanh mốc 24h trước
            if (sessionEndDateTime.isAfter(twentyFourHoursAgo.minusMinutes(5))
                    && sessionEndDateTime.isBefore(twentyFourHoursAgo.plusMinutes(5))) {

                if (!isAttendanceSubmitted(session.getId())) {
                    String classCode = session.getClassEntity() != null ? session.getClassEntity().getCode() : "N/A";
                    String formattedDate = session.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                    String title = "Nhắc nhở: Chưa điểm danh sau 24 giờ";
                    String message = String.format(
                            "Bạn chưa điểm danh cho buổi học %s ngày %s. Buổi học đã kết thúc khoảng 24 giờ trước.",
                            classCode, formattedDate);

                    sendReminderToTeachers(session, title, message);
                    notificationsSent++;
                }
            }
        }

        return notificationsSent;
    }

    // 3) Session kết thúc ~36h trước mà chưa điểm danh
    private int checkSessionsEnded36HoursAgo(LocalDateTime now) {
        LocalDateTime thirtySixHoursAgo = now.minusHours(36);
        LocalDate checkDate = thirtySixHoursAgo.toLocalDate();

        List<Session> sessions = sessionRepository.findByDate(checkDate);

        int notificationsSent = 0;
        for (Session session : sessions) {
            if (session.getTimeSlotTemplate() == null
                    || session.getTimeSlotTemplate().getEndTime() == null
                    || session.getStatus() == SessionStatus.CANCELLED) {
                continue;
            }

            LocalTime sessionEndTime = session.getTimeSlotTemplate().getEndTime();
            LocalDateTime sessionEndDateTime = LocalDateTime.of(checkDate, sessionEndTime);

            // Trong cửa sổ 5 phút quanh mốc 36h trước
            if (sessionEndDateTime.isAfter(thirtySixHoursAgo.minusMinutes(5))
                    && sessionEndDateTime.isBefore(thirtySixHoursAgo.plusMinutes(5))) {

                if (!isAttendanceSubmitted(session.getId())) {
                    String classCode = session.getClassEntity() != null ? session.getClassEntity().getCode() : "N/A";
                    String formattedDate = session.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                    String title = "Nhắc nhở: Chưa điểm danh sau 36 giờ";
                    String message = String.format(
                            "Bạn chưa điểm danh cho buổi học %s ngày %s. Buổi học đã kết thúc khoảng 36 giờ trước.",
                            classCode, formattedDate);

                    sendReminderToTeachers(session, title, message);
                    notificationsSent++;
                }
            }
        }

        return notificationsSent;
    }

    // 4) Session kết thúc ~1h trước mà chưa có teacherNote
    private int checkSessionsEnded1HourAgoForReport(LocalDateTime now) {
        LocalDateTime oneHourAgo = now.minusHours(1);
        LocalDate checkDate = oneHourAgo.toLocalDate();
        LocalTime checkTimeStart = oneHourAgo.minusMinutes(5).toLocalTime();
        LocalTime checkTimeEnd = oneHourAgo.plusMinutes(5).toLocalTime();

        List<Session> sessions = sessionRepository.findSessionsEndedBetween(
                checkDate,
                checkTimeStart,
                checkTimeEnd,
                SessionStatus.CANCELLED);

        int notificationsSent = 0;
        for (Session session : sessions) {
            if (session.getTimeSlotTemplate() == null
                    || session.getTimeSlotTemplate().getEndTime() == null) {
                continue;
            }

            // Nếu đã có teacherNote thì bỏ qua
            if (session.getTeacherNote() != null && !session.getTeacherNote().trim().isEmpty()) {
                continue;
            }

            String classCode = session.getClassEntity() != null ? session.getClassEntity().getCode() : "N/A";
            String formattedDate = session.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            String title = "Nhắc nhở: Chưa nộp báo cáo buổi học";
            String message = String.format(
                    "Bạn chưa nộp báo cáo buổi học cho lớp %s ngày %s. Buổi học đã kết thúc khoảng 1 giờ trước.",
                    classCode, formattedDate);

            sendReminderToTeachers(session, title, message);
            notificationsSent++;
        }

        return notificationsSent;
    }

    // Kiểm tra attendance đã submit hay chưa
    private boolean isAttendanceSubmitted(Long sessionId) {
        List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(sessionId);
        return studentSessions.stream()
                .anyMatch(ss -> ss.getAttendanceStatus() != null
                        && ss.getAttendanceStatus() != AttendanceStatus.PLANNED);
    }

    // Gửi notification + (tuỳ chọn) email cho giáo viên
    private void sendReminderToTeachers(Session session, String title, String message) {
        if (session.getTeachingSlots() == null || session.getTeachingSlots().isEmpty()) {
            return;
        }

        for (TeachingSlot slot : session.getTeachingSlots()) {
            if (slot.getTeacher() == null || slot.getTeacher().getUserAccount() == null) {
                continue;
            }

            UserAccount teacherAccount = slot.getTeacher().getUserAccount();

            // Notification
            try {
                notificationService.createNotification(
                        teacherAccount.getId(),
                        NotificationType.REMINDER,
                        title,
                        message
                );
            } catch (Exception e) {
                logJobWarning("Không thể gửi notification nhắc nhở cho teacher " + teacherAccount.getId()
                        + " session " + session.getId() + ": " + e.getMessage());
            }

            // Email đơn giản (không bắt buộc, không block)
            try {
                if (teacherAccount.getEmail() != null && !teacherAccount.getEmail().trim().isEmpty()) {
                    String subject = title;
                    StringBuilder body = new StringBuilder();
                    body.append("Xin chào ").append(teacherAccount.getFullName()).append(",<br/><br/>")
                            .append(message)
                            .append("<br/><br/>Trân trọng,<br/>Hệ thống TMS");
                    emailService.sendEmailAsync(teacherAccount.getEmail(), subject, body.toString());
                }
            } catch (Exception e) {
                logJobWarning("Không thể gửi email nhắc nhở cho teacher " + teacherAccount.getId()
                        + " session " + session.getId() + ": " + e.getMessage());
            }
        }
    }
}


