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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//Service to automatically update session status and attendance when date passes
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

            // Find sessions that have ended (passed end time) and have teacher note
            List<Session> endedSessionsWithNote = sessionRepository.findEndedSessionsWithTeacherNote(
                today, currentTime, SessionStatus.PLANNED);
            
            if (endedSessionsWithNote.isEmpty()) {
                logJobEnd("SessionAutoUpdate", "No ended sessions with teacher note to update");
                return;
            }

            logJobInfo(String.format("Found %d ended sessions with teacher note (date < %s OR (date = %s AND endTime < %s))", 
                endedSessionsWithNote.size(), today, today, currentTime));

            // Get a QA user to assign as reportedBy for auto-created reports
            Optional<UserAccount> qaUserOpt = userAccountRepository.findUsersByRole("QA").stream()
                    .findFirst();
            
            if (qaUserOpt.isEmpty()) {
                logJobInfo("No QA user found. Skipping QA report creation.");
            }

            // Update sessions to DONE status (only those with teacher note and ended)
            int updatedSessionCount = sessionRepository.updateEndedSessionsWithTeacherNoteToDone(
                today, currentTime, SessionStatus.PLANNED, SessionStatus.DONE);
            
            int updatedAttendanceCount = 0;
            int createdQAReportCount = 0;
            List<StudentSession> studentSessionsToSave = new ArrayList<>();
            List<QAReport> qaReportsToSave = new ArrayList<>();
            
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
            
            // Batch save all updated student sessions
            if (!studentSessionsToSave.isEmpty()) {
                studentSessionRepository.saveAll(studentSessionsToSave);
            }
            
            // Batch save all created QA reports
            if (!qaReportsToSave.isEmpty()) {
                qaReportRepository.saveAll(qaReportsToSave);
            }

            logJobInfo(String.format("Updated %d sessions to DONE status", updatedSessionCount));
            logJobInfo(String.format("Auto-marked %d attendance records as ABSENT", updatedAttendanceCount));
            logJobInfo(String.format("Created %d QA reports for auto-completed sessions", createdQAReportCount));

            // Also handle sessions that ended more than 48 hours ago without teacher note
            LocalDateTime fortyEightHoursAgo = LocalDateTime.now().minusHours(48);
            LocalDate cutoffDate = fortyEightHoursAgo.toLocalDate();
            LocalTime cutoffTime = fortyEightHoursAgo.toLocalTime();

            List<Session> endedSessionsWithoutNote = sessionRepository.findEndedSessionsWithoutTeacherNoteAfter48Hours(
                    cutoffDate, cutoffTime, SessionStatus.PLANNED);

            if (!endedSessionsWithoutNote.isEmpty()) {
                logJobInfo(String.format("Found %d sessions ended more than 48 hours ago without teacher note", 
                        endedSessionsWithoutNote.size()));

                int updatedWithoutNoteCount = sessionRepository.updateEndedSessionsWithoutTeacherNoteAfter48HoursToDone(
                        cutoffDate, cutoffTime, SessionStatus.PLANNED, SessionStatus.DONE);

                int updatedAttendanceWithoutNoteCount = 0;
                int createdQAReportWithoutNoteCount = 0;
                List<StudentSession> studentSessionsWithoutNoteToSave = new ArrayList<>();
                List<QAReport> qaReportsWithoutNoteToSave = new ArrayList<>();

                for (Session session : endedSessionsWithoutNote) {
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

                    // Create QA report with special content indicating no teacher note
                    if (refreshedSession.getStatus() == SessionStatus.DONE && qaUserOpt.isPresent()) {
                        long existingSubmittedReports = qaReportRepository.countSubmittedReportsBySessionId(refreshedSession.getId());

                        if (existingSubmittedReports == 0) {
                            QAReport autoReport = QAReport.builder()
                                    .classEntity(refreshedSession.getClassEntity())
                                    .session(refreshedSession)
                                    .reportedBy(qaUserOpt.get())
                                    .reportType(QAReportType.CLASSROOM_OBSERVATION)
                                    .status(QAReportStatus.SUBMITTED)
                                    .content(String.format(
                                            "Buổi học đã tự động được đánh dấu hoàn thành sau 48 giờ kể từ khi kết thúc. " +
                                            "Chưa có báo cáo từ giáo viên. Session ID: %d, Date: %s, End Time: %s",
                                            refreshedSession.getId(),
                                            refreshedSession.getDate(),
                                            refreshedSession.getTimeSlotTemplate() != null && refreshedSession.getTimeSlotTemplate().getEndTime() != null
                                                    ? refreshedSession.getTimeSlotTemplate().getEndTime().toString()
                                                    : "N/A"))
                                    .build();
                            qaReportsWithoutNoteToSave.add(autoReport);
                            createdQAReportWithoutNoteCount++;
                        }
                    }
                }

                // Batch save all updated student sessions
                if (!studentSessionsWithoutNoteToSave.isEmpty()) {
                    studentSessionRepository.saveAll(studentSessionsWithoutNoteToSave);
                }

                // Batch save all created QA reports
                if (!qaReportsWithoutNoteToSave.isEmpty()) {
                    qaReportRepository.saveAll(qaReportsWithoutNoteToSave);
                }

                logJobInfo(String.format("Updated %d sessions to DONE status (without teacher note after 48h)", updatedWithoutNoteCount));
                logJobInfo(String.format("Auto-marked %d attendance records as ABSENT (sessions without note)", updatedAttendanceWithoutNoteCount));
                logJobInfo(String.format("Created %d QA reports for sessions without teacher note", createdQAReportWithoutNoteCount));
            }

            logJobEnd("SessionAutoUpdate", updatedSessionCount);

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

