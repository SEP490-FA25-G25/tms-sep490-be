package org.fyp.tmssep490be.services.impl.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.StudentSession;
import org.fyp.tmssep490be.entities.TeachingSlot;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.NotificationPriority;
import org.fyp.tmssep490be.entities.enums.NotificationType;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.entities.enums.TeachingSlotStatus;
import org.fyp.tmssep490be.repositories.NotificationRepository;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.fyp.tmssep490be.repositories.TeachingSlotRepository;
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

/**
 * Scheduled job to remind teachers about attendance and report submission
 * 
 * Checks for:
 * 1. Sessions ending in 10 minutes - if attendance not submitted
 * 2. Sessions ended 24 hours ago - if attendance not submitted
 * 3. Sessions ended 36 hours ago - if attendance not submitted
 * 4. Sessions ended 1 hour ago - if report not submitted
 */
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
    private final NotificationRepository notificationRepository;

    @Value("${tms.scheduler.jobs.teacher-attendance-reminder.warning-minutes-before-end:10}")
    private int warningMinutesBeforeEnd;

    /**
     * Main scheduled method to check and send attendance reminders
     * Runs every 5 minutes by default
     */
    @Scheduled(cron = "${tms.scheduler.jobs.teacher-attendance-reminder.cron:0 */5 * * * *}")
    @Transactional
    public void checkAndSendAttendanceReminders() {
        logJobStart("TeacherAttendanceReminderJob");

        try {
            LocalDateTime now = LocalDateTime.now();
            int notificationsSent = 0;

            // 1. Check sessions ending in 10 minutes
            notificationsSent += checkSessionsEndingSoon(now);

            // 2. Check sessions ended 24 hours ago
            notificationsSent += checkSessionsEnded24HoursAgo(now);

            // 3. Check sessions ended 36 hours ago
            notificationsSent += checkSessionsEnded36HoursAgo(now);

            // 4. Check sessions ended 1 hour ago - if report not submitted
            notificationsSent += checkSessionsEnded1HourAgoForReport(now);

            logJobEnd("TeacherAttendanceReminderJob", 
                String.format("Sent %d attendance reminder notifications", notificationsSent));

        } catch (Exception e) {
            logJobError("TeacherAttendanceReminderJob", e);
            throw e; // Re-throw to prevent silent failures
        }
    }

    /**
     * Check sessions ending in 10 minutes (or configured minutes) - if attendance not submitted
     */
    private int checkSessionsEndingSoon(LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        LocalTime warningTime = now.toLocalTime().plusMinutes(warningMinutesBeforeEnd);

        // Find sessions today that are ending soon
        List<Session> sessionsToday = sessionRepository.findByDate(today);
        
        int notificationsSent = 0;
        for (Session session : sessionsToday) {
            if (session.getTimeSlotTemplate() == null || 
                session.getTimeSlotTemplate().getEndTime() == null ||
                session.getStatus() == SessionStatus.CANCELLED) {
                continue;
            }

            LocalTime sessionEndTime = session.getTimeSlotTemplate().getEndTime();
            LocalTime currentTime = now.toLocalTime();
            
            // Check if session ends within the warning window (e.g., within next 10 minutes)
            // Session end time should be after current time and before or equal to warning time
            if (sessionEndTime.isAfter(currentTime) && 
                (sessionEndTime.isBefore(warningTime) || sessionEndTime.equals(warningTime))) {
                
                // Check if attendance is already submitted
                if (!isAttendanceSubmitted(session.getId())) {
                    // Get teachers for this session
                    List<TeachingSlot> teachingSlots = teachingSlotRepository
                        .findBySessionIdAndStatus(session.getId(), TeachingSlotStatus.SCHEDULED);
                    
                    for (TeachingSlot slot : teachingSlots) {
                        Long teacherId = slot.getTeacher().getUserAccount().getId();
                        
                        // Check if notification already exists for this session and teacher
                        if (!hasNotificationForSession(teacherId, session.getId(), "ATTENDANCE_WARNING_10MIN")) {
                            String className = session.getClassEntity().getName();
                            String formattedDate = session.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                            
                            // Create notification with action URL
                            notificationService.createNotificationFromRequest(
                                org.fyp.tmssep490be.dtos.notification.NotificationRequestDTO.builder()
                                    .recipientId(teacherId)
                                    .type(NotificationType.CLASS_REMINDER)
                                    .title("Nhắc nhở: Buổi học sắp kết thúc")
                                    .message(String.format("Buổi học %s ngày %s sẽ kết thúc trong %d phút. Vui lòng điểm danh cho học sinh.",
                                        className, formattedDate, warningMinutesBeforeEnd))
                                    .priority(NotificationPriority.HIGH)
                                    .referenceType("SESSION")
                                    .referenceId(session.getId())
                                    .actionUrl(String.format("/teacher/attendance/%d", session.getId()))
                                    .metadata("{\"reminderType\":\"ATTENDANCE_WARNING_10MIN\"}")
                                    .build()
                            );
                            
                            notificationsSent++;
                            logJobInfo(String.format("Sent 10-min warning notification to teacher %d for session %d", 
                                teacherId, session.getId()));
                        }
                    }
                }
            }
        }

        return notificationsSent;
    }

    /**
     * Check sessions ended 24 hours ago - if attendance not submitted
     */
    private int checkSessionsEnded24HoursAgo(LocalDateTime now) {
        LocalDateTime twentyFourHoursAgo = now.minusHours(24);
        LocalDate checkDate = twentyFourHoursAgo.toLocalDate();

        // Find sessions that ended around 24 hours ago
        List<Session> sessions = sessionRepository.findByDate(checkDate);
        
        int notificationsSent = 0;
        for (Session session : sessions) {
            if (session.getTimeSlotTemplate() == null || 
                session.getTimeSlotTemplate().getEndTime() == null ||
                session.getStatus() == SessionStatus.CANCELLED) {
                continue;
            }

            LocalTime sessionEndTime = session.getTimeSlotTemplate().getEndTime();
            LocalDateTime sessionEndDateTime = LocalDateTime.of(checkDate, sessionEndTime);
            
            // Check if session ended approximately 24 hours ago (within 5 minute window)
            if (sessionEndDateTime.isAfter(twentyFourHoursAgo.minusMinutes(5)) && 
                sessionEndDateTime.isBefore(twentyFourHoursAgo.plusMinutes(5))) {
                
                // Check if attendance is already submitted
                if (!isAttendanceSubmitted(session.getId())) {
                    // Get teachers for this session
                    List<TeachingSlot> teachingSlots = teachingSlotRepository
                        .findBySessionIdAndStatus(session.getId(), TeachingSlotStatus.SCHEDULED);
                    
                    for (TeachingSlot slot : teachingSlots) {
                        Long teacherId = slot.getTeacher().getUserAccount().getId();
                        
                        // Check if notification already exists for this session and teacher
                        if (!hasNotificationForSession(teacherId, session.getId(), "ATTENDANCE_REMINDER_24H")) {
                            String className = session.getClassEntity().getName();
                            String formattedDate = session.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                            
                            notificationService.createNotificationFromRequest(
                                org.fyp.tmssep490be.dtos.notification.NotificationRequestDTO.builder()
                                    .recipientId(teacherId)
                                    .type(NotificationType.CLASS_REMINDER)
                                    .title("Nhắc nhở: Chưa điểm danh sau 24 giờ")
                                    .message(String.format("Bạn chưa điểm danh cho buổi học %s ngày %s. Buổi học đã kết thúc 24 giờ trước.",
                                        className, formattedDate))
                                    .priority(NotificationPriority.HIGH)
                                    .referenceType("SESSION")
                                    .referenceId(session.getId())
                                    .actionUrl(String.format("/teacher/attendance/%d", session.getId()))
                                    .metadata("{\"reminderType\":\"ATTENDANCE_REMINDER_24H\"}")
                                    .build()
                            );
                            
                            notificationsSent++;
                            logJobInfo(String.format("Sent 24h reminder notification to teacher %d for session %d", 
                                teacherId, session.getId()));
                        }
                    }
                }
            }
        }

        return notificationsSent;
    }

    /**
     * Check sessions ended 36 hours ago - if attendance not submitted
     */
    private int checkSessionsEnded36HoursAgo(LocalDateTime now) {
        LocalDateTime thirtySixHoursAgo = now.minusHours(36);
        LocalDate checkDate = thirtySixHoursAgo.toLocalDate();

        // Find sessions that ended around 36 hours ago
        List<Session> sessions = sessionRepository.findByDate(checkDate);
        
        int notificationsSent = 0;
        for (Session session : sessions) {
            if (session.getTimeSlotTemplate() == null || 
                session.getTimeSlotTemplate().getEndTime() == null ||
                session.getStatus() == SessionStatus.CANCELLED) {
                continue;
            }

            LocalTime sessionEndTime = session.getTimeSlotTemplate().getEndTime();
            LocalDateTime sessionEndDateTime = LocalDateTime.of(checkDate, sessionEndTime);
            
            // Check if session ended approximately 36 hours ago (within 5 minute window)
            if (sessionEndDateTime.isAfter(thirtySixHoursAgo.minusMinutes(5)) && 
                sessionEndDateTime.isBefore(thirtySixHoursAgo.plusMinutes(5))) {
                
                // Check if attendance is already submitted
                if (!isAttendanceSubmitted(session.getId())) {
                    // Get teachers for this session
                    List<TeachingSlot> teachingSlots = teachingSlotRepository
                        .findBySessionIdAndStatus(session.getId(), TeachingSlotStatus.SCHEDULED);
                    
                    for (TeachingSlot slot : teachingSlots) {
                        Long teacherId = slot.getTeacher().getUserAccount().getId();
                        
                        // Check if notification already exists for this session and teacher
                        if (!hasNotificationForSession(teacherId, session.getId(), "ATTENDANCE_REMINDER_36H")) {
                            String className = session.getClassEntity().getName();
                            String formattedDate = session.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                            
                            notificationService.createNotificationFromRequest(
                                org.fyp.tmssep490be.dtos.notification.NotificationRequestDTO.builder()
                                    .recipientId(teacherId)
                                    .type(NotificationType.CLASS_REMINDER)
                                    .title("Nhắc nhở: Chưa điểm danh sau 36 giờ")
                                    .message(String.format("Bạn chưa điểm danh cho buổi học %s ngày %s. Buổi học đã kết thúc 36 giờ trước.",
                                        className, formattedDate))
                                    .priority(NotificationPriority.URGENT)
                                    .referenceType("SESSION")
                                    .referenceId(session.getId())
                                    .actionUrl(String.format("/teacher/attendance/%d", session.getId()))
                                    .metadata("{\"reminderType\":\"ATTENDANCE_REMINDER_36H\"}")
                                    .build()
                            );
                            
                            notificationsSent++;
                            logJobInfo(String.format("Sent 36h reminder notification to teacher %d for session %d", 
                                teacherId, session.getId()));
                        }
                    }
                }
            }
        }

        return notificationsSent;
    }

    /**
     * Check sessions ended 1 hour ago - if report not submitted
     */
    private int checkSessionsEnded1HourAgoForReport(LocalDateTime now) {
        LocalDateTime oneHourAgo = now.minusHours(1);
        LocalDate checkDate = oneHourAgo.toLocalDate();
        LocalTime checkTimeStart = oneHourAgo.minusMinutes(5).toLocalTime();
        LocalTime checkTimeEnd = oneHourAgo.plusMinutes(5).toLocalTime();

        List<Session> sessions = sessionRepository.findSessionsEndedBetween(
            checkDate, checkTimeStart, checkTimeEnd, SessionStatus.CANCELLED
        );

        int notificationsSent = 0;
        for (Session session : sessions) {
            if (session.getTimeSlotTemplate() == null ||
                session.getTimeSlotTemplate().getEndTime() == null) {
                continue;
            }

            // Check if report is already submitted
            if (!isReportSubmitted(session)) {
                // Get teachers for this session
                List<TeachingSlot> teachingSlots = teachingSlotRepository
                    .findBySessionIdAndStatus(session.getId(), TeachingSlotStatus.SCHEDULED);

                for (TeachingSlot slot : teachingSlots) {
                    Long teacherId = slot.getTeacher().getUserAccount().getId();

                    // Check if notification already exists for this session and teacher
                    if (!hasNotificationForSession(teacherId, session.getId(), "REPORT_REMINDER_1H")) {
                        String className = session.getClassEntity().getName();
                        String formattedDate = session.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                        notificationService.createNotificationFromRequest(
                            org.fyp.tmssep490be.dtos.notification.NotificationRequestDTO.builder()
                                .recipientId(teacherId)
                                .type(NotificationType.CLASS_REMINDER)
                                .title("Nhắc nhở: Chưa nộp báo cáo buổi học")
                                .message(String.format("Bạn chưa nộp báo cáo buổi học cho lớp %s ngày %s. Buổi học đã kết thúc 1 giờ trước.",
                                    className, formattedDate))
                                .priority(NotificationPriority.MEDIUM)
                                .referenceType("SESSION")
                                .referenceId(session.getId())
                                .actionUrl(String.format("/teacher/attendance/%d", session.getId()))
                                .metadata("{\"reminderType\":\"REPORT_REMINDER_1H\"}")
                                .build()
                        );

                        notificationsSent++;
                        logJobInfo(String.format("Sent 1h report reminder notification to teacher %d for session %d",
                            teacherId, session.getId()));
                    }
                }
            }
        }

        return notificationsSent;
    }

    /**
     * Check if attendance has been submitted for a session
     * Attendance is considered submitted if any StudentSession has attendanceStatus != PLANNED
     */
    private boolean isAttendanceSubmitted(Long sessionId) {
        List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(sessionId);
        return studentSessions.stream()
            .anyMatch(ss -> ss.getAttendanceStatus() != null && 
                           ss.getAttendanceStatus() != AttendanceStatus.PLANNED);
    }

    /**
     * Check if report has been submitted for a session
     * Report is considered submitted if teacherNote is not null and not empty
     */
    private boolean isReportSubmitted(Session session) {
        return session.getTeacherNote() != null && 
               !session.getTeacherNote().trim().isEmpty();
    }

    /**
     * Check if a notification already exists for this session and teacher with the same reminder type
     */
    private boolean hasNotificationForSession(Long teacherId, Long sessionId, String reminderType) {
        return notificationRepository.existsByRecipientIdAndReferenceTypeAndReferenceIdAndMetadataContaining(
            teacherId, "SESSION", sessionId, reminderType);
    }
}

