package org.fyp.tmssep490be.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.QAReport;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.StudentSession;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.QAReportStatus;
import org.fyp.tmssep490be.entities.enums.QAReportType;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.repositories.QAReportRepository;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//Service to automatically update session status and attendance for ALL sessions in the system
//This is a general cronjob, not specific to teachers or students
//Runs daily at 1:00 AM to mark past sessions as DONE, auto-absent students, and create QA reports
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionAutoUpdateService extends BaseScheduledJob {

    private final SessionRepository sessionRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final QAReportRepository qaReportRepository;
    private final UserAccountRepository userAccountRepository;

    /**
     * Update past sessions to DONE status when application is ready.
     * This ensures seed data has been loaded before checking for past sessions.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void updateOnStartup() {
        log.info("Application ready: Checking for past sessions that need to be updated to DONE status");
        try {
            // First, update attendance for all ended sessions to ensure attendance rate is correct
            // Note: Only updates PLANNED status. PRESENT and ABSENT remain unchanged (already recorded by teacher)
            LocalDate today = LocalDate.now();
            LocalTime currentTime = LocalTime.now();
            List<Session> allEndedSessions = sessionRepository.findAllEndedSessions(
                today, currentTime, SessionStatus.PLANNED);
            
            int updatedAttendanceCount = 0;
            List<StudentSession> attendanceToUpdate = new ArrayList<>();
            
            for (Session session : allEndedSessions) {
                List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(session.getId());
                for (StudentSession studentSession : studentSessions) {
                    // Only update if status is PLANNED (not yet recorded)
                    // If already PRESENT or ABSENT, keep it as is (teacher already recorded)
                    if (studentSession.getAttendanceStatus() == AttendanceStatus.PLANNED) {
                        studentSession.setAttendanceStatus(AttendanceStatus.ABSENT);
                        studentSession.setRecordedAt(OffsetDateTime.now());
                        attendanceToUpdate.add(studentSession);
                        updatedAttendanceCount++;
                    }
                }
            }
            
            if (!attendanceToUpdate.isEmpty()) {
                studentSessionRepository.saveAll(attendanceToUpdate);
                log.info("On startup: Auto-marked {} attendance records as ABSENT for ended sessions", updatedAttendanceCount);
            }
            
            // Then update session status to DONE
            updatePastSessionsToDone();
            // Also create QA reports for existing DONE sessions that don't have reports
            createQAReportsForDoneSessionsWithoutReports();
        } catch (Exception e) {
            log.error("Error updating sessions on startup: {}", e.getMessage(), e);
            // Don't throw exception to prevent application startup failure
        }
    }

     //Automatically mark sessions as DONE if they have ended (passed end time) AND have teacher note
     //Also marks students with PLANNED attendance as ABSENT
     //Creates QA reports for sessions that are automatically marked as DONE
     //Runs daily at 1:00 AM
    @Scheduled(cron = "0 0 1 * * ?") // Every day at 1:00 AM
    @Transactional
    public void updatePastSessionsToDone() {
        try {
            logJobStart("SessionAutoUpdate");
            
            LocalDate today = LocalDate.now();
            LocalTime currentTime = LocalTime.now();

            // First, auto-update attendance from PLANNED to ABSENT for all ended sessions
            // This ensures attendance rate is calculated correctly immediately
            // Note: Only updates PLANNED status. PRESENT and ABSENT remain unchanged (already recorded by teacher)
            List<Session> allEndedSessions = sessionRepository.findAllEndedSessions(
                today, currentTime, SessionStatus.PLANNED);
            
            int updatedAttendanceForEndedSessions = 0;
            List<StudentSession> attendanceToUpdate = new ArrayList<>();
            
            for (Session session : allEndedSessions) {
                List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(session.getId());
                for (StudentSession studentSession : studentSessions) {
                    // Only update if status is PLANNED (not yet recorded)
                    // If already PRESENT or ABSENT, keep it as is (teacher already recorded)
                    if (studentSession.getAttendanceStatus() == AttendanceStatus.PLANNED) {
                        studentSession.setAttendanceStatus(AttendanceStatus.ABSENT);
                        studentSession.setRecordedAt(OffsetDateTime.now());
                        attendanceToUpdate.add(studentSession);
                        updatedAttendanceForEndedSessions++;
                    }
                }
            }
            
            if (!attendanceToUpdate.isEmpty()) {
                studentSessionRepository.saveAll(attendanceToUpdate);
                logJobInfo(String.format("Auto-marked %d attendance records as ABSENT for ended sessions", updatedAttendanceForEndedSessions));
            }

            // Get a QA user to assign as reportedBy for auto-created reports
            Optional<UserAccount> qaUserOpt = userAccountRepository.findUsersByRole("QA").stream()
                    .findFirst();
            
            if (qaUserOpt.isEmpty()) {
                logJobInfo("No QA user found. Skipping QA report creation.");
            }

            // Find sessions that have ended (passed end time) and have teacher note
            List<Session> endedSessionsWithNote = sessionRepository.findEndedSessionsWithTeacherNote(
                today, currentTime, SessionStatus.PLANNED);
            
            int updatedSessionCount = 0;
            int updatedAttendanceCount = 0;
            int createdQAReportCount = 0;
            List<StudentSession> studentSessionsToSave = new ArrayList<>();
            List<QAReport> qaReportsToSave = new ArrayList<>();

            if (!endedSessionsWithNote.isEmpty()) {
                logJobInfo(String.format("Found %d ended sessions with teacher note (date < %s OR (date = %s AND endTime < %s))", 
                    endedSessionsWithNote.size(), today, today, currentTime));

                // Update sessions to DONE status (only those with teacher note and ended)
                updatedSessionCount = sessionRepository.updateEndedSessionsWithTeacherNoteToDone(
                    today, currentTime, SessionStatus.PLANNED, SessionStatus.DONE);
                
                // Update attendance and create QA reports for affected sessions
                for (Session session : endedSessionsWithNote) {
                    // Refresh session to get updated status
                    Session refreshedSession = sessionRepository.findById(session.getId())
                            .orElse(session);
                    
                    // Auto-mark students with PLANNED attendance as ABSENT
                    List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(refreshedSession.getId());
                    for (StudentSession studentSession : studentSessions) {
                        if (studentSession.getAttendanceStatus() == AttendanceStatus.PLANNED) {
                            studentSession.setAttendanceStatus(AttendanceStatus.ABSENT);
                            studentSession.setRecordedAt(OffsetDateTime.now());
                            studentSessionsToSave.add(studentSession);
                            updatedAttendanceCount++;
                        }
                    }
                    
                    // Create QA report if session is now DONE and doesn't have a submitted QA report yet
                    if (refreshedSession.getStatus() == SessionStatus.DONE && qaUserOpt.isPresent()) {
                        long existingSubmittedReports = qaReportRepository.countSubmittedReportsBySessionId(refreshedSession.getId());
                        
                        if (existingSubmittedReports == 0) {
                            QAReport autoReport = QAReport.builder()
                                    .classEntity(refreshedSession.getClassEntity())
                                    .session(refreshedSession)
                                    .reportedBy(qaUserOpt.get())
                                    .reportType(QAReportType.CLASSROOM_OBSERVATION)
                                    .status(QAReportStatus.SUBMITTED)
                                    .content(String.format("Buổi học đã tự động được đánh dấu hoàn thành do đã qua ngày. Session ID: %d, Date: %s", 
                                            refreshedSession.getId(), refreshedSession.getDate()))
                                    .build();
                            qaReportsToSave.add(autoReport);
                            createdQAReportCount++;
                        }
                    }
                }
            } else {
                logJobInfo("No ended sessions with teacher note to update");
            }
            
            // Batch save all updated student sessions
            if (!studentSessionsToSave.isEmpty()) {
                studentSessionRepository.saveAll(studentSessionsToSave);
            }
            
            // Batch save all created QA reports
            if (!qaReportsToSave.isEmpty()) {
                qaReportRepository.saveAll(qaReportsToSave);
            }

            if (updatedSessionCount > 0) {
                logJobInfo(String.format("Updated %d sessions to DONE status (with teacher note)", updatedSessionCount));
            }
            if (updatedAttendanceCount > 0) {
                logJobInfo(String.format("Auto-marked %d attendance records as ABSENT (sessions with note)", updatedAttendanceCount));
            }
            if (createdQAReportCount > 0) {
                logJobInfo(String.format("Created %d QA reports for auto-completed sessions", createdQAReportCount));
            }

            // Also handle sessions that have passed their date (date < today) without teacher note
            List<Session> pastSessionsWithoutNote = sessionRepository.findPastSessionsWithoutTeacherNote(
                    today, SessionStatus.PLANNED);

            int updatedWithoutNoteCount = 0;
            if (!pastSessionsWithoutNote.isEmpty()) {
                logJobInfo(String.format("Found %d sessions that have passed their date without teacher note", 
                        pastSessionsWithoutNote.size()));

                updatedWithoutNoteCount = sessionRepository.updatePastSessionsWithoutTeacherNoteToDone(
                        today, SessionStatus.PLANNED, SessionStatus.DONE);

                int updatedAttendanceWithoutNoteCount = 0;
                List<StudentSession> studentSessionsWithoutNoteToSave = new ArrayList<>();

                for (Session session : pastSessionsWithoutNote) {
                    Session refreshedSession = sessionRepository.findById(session.getId())
                            .orElse(session);

                    // Auto-mark students with PLANNED attendance as ABSENT
                    List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(refreshedSession.getId());
                    for (StudentSession studentSession : studentSessions) {
                        if (studentSession.getAttendanceStatus() == AttendanceStatus.PLANNED) {
                            studentSession.setAttendanceStatus(AttendanceStatus.ABSENT);
                            studentSession.setRecordedAt(OffsetDateTime.now());
                            studentSessionsWithoutNoteToSave.add(studentSession);
                            updatedAttendanceWithoutNoteCount++;
                        }
                    }
                    // Note: Do NOT create QA report here - only when 48h has passed after session end
                }

                // Batch save all updated student sessions
                if (!studentSessionsWithoutNoteToSave.isEmpty()) {
                    studentSessionRepository.saveAll(studentSessionsWithoutNoteToSave);
                }

                logJobInfo(String.format("Updated %d sessions to DONE status (passed date without teacher note)", updatedWithoutNoteCount));
                logJobInfo(String.format("Auto-marked %d attendance records as ABSENT (sessions without note)", updatedAttendanceWithoutNoteCount));
            } else {
                logJobInfo("No sessions that have passed their date without teacher note to update");
            }

            // Handle sessions that ended more than 48 hours ago without teacher note - create QA reports
            LocalDateTime fortyEightHoursAgo = LocalDateTime.now().minusHours(48);
            LocalDate cutoffDate = fortyEightHoursAgo.toLocalDate();
            LocalTime cutoffTime = fortyEightHoursAgo.toLocalTime();

            List<Session> endedSessionsWithoutNoteAfter48h = sessionRepository.findEndedSessionsWithoutTeacherNoteAfter48Hours(
                    cutoffDate, cutoffTime, SessionStatus.DONE);

            if (!endedSessionsWithoutNoteAfter48h.isEmpty()) {
                logJobInfo(String.format("Found %d DONE sessions ended more than 48 hours ago without teacher note - creating QA reports", 
                        endedSessionsWithoutNoteAfter48h.size()));

                int createdQAReportAfter48hCount = 0;
                List<QAReport> qaReportsAfter48hToSave = new ArrayList<>();

                for (Session session : endedSessionsWithoutNoteAfter48h) {
                    // Check if QA report already exists
                    long existingSubmittedReports = qaReportRepository.countSubmittedReportsBySessionId(session.getId());
                    
                    if (existingSubmittedReports == 0 && qaUserOpt.isPresent()) {
                        QAReport autoReport = QAReport.builder()
                                .classEntity(session.getClassEntity())
                                .session(session)
                                .reportedBy(qaUserOpt.get())
                                .reportType(QAReportType.CLASSROOM_OBSERVATION)
                                .status(QAReportStatus.SUBMITTED)
                                .content(String.format(
                                        "Buổi học đã tự động được đánh dấu hoàn thành sau 48 giờ kể từ khi kết thúc. " +
                                        "Chưa có báo cáo từ giáo viên. Session ID: %d, Date: %s, End Time: %s",
                                        session.getId(),
                                        session.getDate(),
                                        session.getTimeSlotTemplate() != null && session.getTimeSlotTemplate().getEndTime() != null
                                                ? session.getTimeSlotTemplate().getEndTime().toString()
                                                : "N/A"))
                                .build();
                        qaReportsAfter48hToSave.add(autoReport);
                        createdQAReportAfter48hCount++;
                    }
                }

                // Batch save all created QA reports
                if (!qaReportsAfter48hToSave.isEmpty()) {
                    qaReportRepository.saveAll(qaReportsAfter48hToSave);
                    logJobInfo(String.format("Created %d QA reports for sessions without teacher note after 48h", createdQAReportAfter48hCount));
                }
            }

            int totalUpdatedSessions = updatedSessionCount + updatedWithoutNoteCount;
            logJobEnd("SessionAutoUpdate", totalUpdatedSessions);

        } catch (Exception e) {
            logJobError("SessionAutoUpdate", e);
            throw e;
        }
    }

    @Transactional
    public void updatePastSessionsToDoneNow() {
        updatePastSessionsToDone();
        createQAReportsForDoneSessionsWithoutReports();
    }

    /**
     * Check and update a specific session status if it meets the criteria.
     * This is called when accessing session detail to ensure status is up-to-date.
     * Uses REQUIRES_NEW to avoid transaction conflicts with read-only transactions.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkAndUpdateSessionStatusIfNeeded(Long sessionId) {
        try {
            Optional<Session> sessionOpt = sessionRepository.findById(sessionId);
            if (sessionOpt.isEmpty()) {
                return;
            }

            Session session = sessionOpt.get();
            
            // Only process PLANNED sessions
            if (session.getStatus() != SessionStatus.PLANNED) {
                return;
            }

            LocalDate today = LocalDate.now();
            LocalTime currentTime = LocalTime.now();
            
            // Check if session has ended
            boolean hasEnded = false;
            if (session.getTimeSlotTemplate() != null && session.getTimeSlotTemplate().getEndTime() != null) {
                LocalDate sessionDate = session.getDate();
                LocalTime endTime = session.getTimeSlotTemplate().getEndTime();
                hasEnded = sessionDate.isBefore(today) || 
                          (sessionDate.equals(today) && endTime.isBefore(currentTime));
            } else {
                // If no end time, check by date only
                hasEnded = session.getDate().isBefore(today);
            }

            if (!hasEnded) {
                return;
            }

            // Check if session has teacher note
            boolean hasTeacherNote = session.getTeacherNote() != null && 
                                    !session.getTeacherNote().trim().isEmpty();

            if (hasTeacherNote) {
                // Update to DONE immediately if has teacher note
                session.setStatus(SessionStatus.DONE);
                sessionRepository.save(session);
                
                // Update attendance and create QA report
                updateAttendanceAndCreateQAReport(session);
                log.debug("Auto-updated session {} to DONE (has teacher note)", sessionId);
            } else {
                // Check if session date has passed (date < today)
                LocalDate sessionDate = session.getDate();
                
                if (sessionDate.isBefore(today)) {
                    // Update to DONE if date has passed (but don't create QA report yet)
                    session.setStatus(SessionStatus.DONE);
                    sessionRepository.save(session);
                    
                    // Only update attendance, don't create QA report (will be created after 48h)
                    updateAttendanceForEndedSession(session);
                    log.debug("Auto-updated session {} to DONE (date passed without teacher note, QA report will be created after 48h)", sessionId);
                } else {
                    // Just update attendance from PLANNED to ABSENT, but keep status as PLANNED
                    updateAttendanceForEndedSession(session);
                    log.debug("Updated attendance for session {} (ended but date not passed, no teacher note)", sessionId);
                }
            }
        } catch (Exception e) {
            log.error("Error checking/updating session {} status: {}", sessionId, e.getMessage(), e);
            // Don't rethrow to avoid aborting the calling transaction
        }
    }

    private void updateAttendanceAndCreateQAReport(Session session) {
        Optional<UserAccount> qaUserOpt = userAccountRepository.findUsersByRole("QA").stream()
                .findFirst();

        // Update attendance
        List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(session.getId());
        List<StudentSession> attendanceToUpdate = new ArrayList<>();
        for (StudentSession studentSession : studentSessions) {
            if (studentSession.getAttendanceStatus() == AttendanceStatus.PLANNED) {
                studentSession.setAttendanceStatus(AttendanceStatus.ABSENT);
                studentSession.setRecordedAt(OffsetDateTime.now());
                attendanceToUpdate.add(studentSession);
            }
        }
        if (!attendanceToUpdate.isEmpty()) {
            studentSessionRepository.saveAll(attendanceToUpdate);
        }

        // Create QA report
        if (qaUserOpt.isPresent()) {
            long existingSubmittedReports = qaReportRepository.countSubmittedReportsBySessionId(session.getId());
            if (existingSubmittedReports == 0) {
                QAReport autoReport = QAReport.builder()
                        .classEntity(session.getClassEntity())
                        .session(session)
                        .reportedBy(qaUserOpt.get())
                        .reportType(QAReportType.CLASSROOM_OBSERVATION)
                        .status(QAReportStatus.SUBMITTED)
                        .content(String.format("Buổi học đã tự động được đánh dấu hoàn thành do đã qua ngày. Session ID: %d, Date: %s", 
                                session.getId(), session.getDate()))
                        .build();
                qaReportRepository.save(autoReport);
            }
        }
    }

    private void updateAttendanceAndCreateQAReportWithoutNote(Session session) {
        Optional<UserAccount> qaUserOpt = userAccountRepository.findUsersByRole("QA").stream()
                .findFirst();

        // Update attendance
        List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(session.getId());
        List<StudentSession> attendanceToUpdate = new ArrayList<>();
        for (StudentSession studentSession : studentSessions) {
            if (studentSession.getAttendanceStatus() == AttendanceStatus.PLANNED) {
                studentSession.setAttendanceStatus(AttendanceStatus.ABSENT);
                studentSession.setRecordedAt(OffsetDateTime.now());
                attendanceToUpdate.add(studentSession);
            }
        }
        if (!attendanceToUpdate.isEmpty()) {
            studentSessionRepository.saveAll(attendanceToUpdate);
        }

        // Create QA report with special message
        if (qaUserOpt.isPresent()) {
            long existingSubmittedReports = qaReportRepository.countSubmittedReportsBySessionId(session.getId());
            if (existingSubmittedReports == 0) {
                QAReport autoReport = QAReport.builder()
                        .classEntity(session.getClassEntity())
                        .session(session)
                        .reportedBy(qaUserOpt.get())
                        .reportType(QAReportType.CLASSROOM_OBSERVATION)
                        .status(QAReportStatus.SUBMITTED)
                        .content(String.format(
                                "Buổi học đã tự động được đánh dấu hoàn thành vì đã qua ngày. " +
                                "Chưa có báo cáo từ giáo viên. Session ID: %d, Date: %s, End Time: %s",
                                session.getId(),
                                session.getDate(),
                                session.getTimeSlotTemplate() != null && session.getTimeSlotTemplate().getEndTime() != null
                                        ? session.getTimeSlotTemplate().getEndTime().toString()
                                        : "N/A"))
                        .build();
                qaReportRepository.save(autoReport);
            }
        }
    }

    private void updateAttendanceForEndedSession(Session session) {
        // Only update attendance from PLANNED to ABSENT, keep status as PLANNED
        List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(session.getId());
        List<StudentSession> attendanceToUpdate = new ArrayList<>();
        for (StudentSession studentSession : studentSessions) {
            if (studentSession.getAttendanceStatus() == AttendanceStatus.PLANNED) {
                studentSession.setAttendanceStatus(AttendanceStatus.ABSENT);
                studentSession.setRecordedAt(OffsetDateTime.now());
                attendanceToUpdate.add(studentSession);
            }
        }
        if (!attendanceToUpdate.isEmpty()) {
            studentSessionRepository.saveAll(attendanceToUpdate);
        }
    }

    /**
     * Create QA reports for sessions that are already DONE but don't have submitted QA reports yet.
     * This handles cases where sessions were set to DONE in seed data or by other means.
     */
    @Transactional
    public void createQAReportsForDoneSessionsWithoutReports() {
        try {
            LocalDate today = LocalDate.now();
            
            // Find all DONE sessions that are in the past
            List<Session> pastDoneSessions = sessionRepository.findPastSessionsByStatusAndDate(
                    SessionStatus.DONE, today);
            
            // Filter to only those without submitted QA reports
            List<Session> doneSessionsWithoutReports = pastDoneSessions.stream()
                    .filter(s -> qaReportRepository.countSubmittedReportsBySessionId(s.getId()) == 0)
                    .toList();
            
            if (doneSessionsWithoutReports.isEmpty()) {
                log.debug("No DONE sessions without QA reports found");
                return;
            }
            
            log.info("Found {} DONE sessions without submitted QA reports", doneSessionsWithoutReports.size());
            
            // Get a QA user to assign as reportedBy
            Optional<UserAccount> qaUserOpt = userAccountRepository.findUsersByRole("QA").stream()
                    .findFirst();
            
            if (qaUserOpt.isEmpty()) {
                log.warn("No QA user found. Cannot create QA reports for DONE sessions.");
                return;
            }
            
            List<QAReport> qaReportsToSave = new ArrayList<>();
            
            for (Session session : doneSessionsWithoutReports) {
                QAReport autoReport = QAReport.builder()
                        .classEntity(session.getClassEntity())
                        .session(session)
                        .reportedBy(qaUserOpt.get())
                        .reportType(QAReportType.CLASSROOM_OBSERVATION)
                        .status(QAReportStatus.SUBMITTED)
                        .content(String.format("Buổi học đã tự động được đánh dấu hoàn thành. Session ID: %d, Date: %s", 
                                session.getId(), session.getDate()))
                        .build();
                qaReportsToSave.add(autoReport);
            }
            
            if (!qaReportsToSave.isEmpty()) {
                qaReportRepository.saveAll(qaReportsToSave);
                log.info("Created {} QA reports for DONE sessions without reports", qaReportsToSave.size());
            }
            
        } catch (Exception e) {
            log.error("Error creating QA reports for DONE sessions", e);
            throw e;
        }
    }
}

