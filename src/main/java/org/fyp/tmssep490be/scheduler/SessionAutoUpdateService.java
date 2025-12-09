package org.fyp.tmssep490be.scheduler;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.StudentSession;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

//Service to automatically update session status and attendance when date passes
//Runs daily at 1:00 AM to mark past sessions as DONE and auto-absent students
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionAutoUpdateService extends BaseScheduledJob {

    private final SessionRepository sessionRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final TransactionTemplate transactionTemplate;

    @PostConstruct
    public void updateOnStartup() {
        log.info("Server startup: Checking for past sessions that need to be updated to DONE status");
        try {
            transactionTemplate.executeWithoutResult(status -> {
                try {
                    updatePastSessionsToDone();
                } catch (Exception e) {
                    log.error("Error updating sessions on startup", e);
                    status.setRollbackOnly();
                }
            });
        } catch (Exception e) {
            log.error("Error updating sessions on startup: {}", e.getMessage(), e);
        }
    }

     //Automatically mark sessions as DONE if their date has passed
     //Also marks students with PLANNED attendance as ABSENT
     //Runs daily at 1:00 AM
    @Scheduled(cron = "0 0 1 * * ?") // Every day at 1:00 AM
    @Transactional
    public void updatePastSessionsToDone() {
        try {
            logJobStart("SessionAutoUpdate");
            
            LocalDate today = LocalDate.now();

            List<Session> pastSessions = sessionRepository.findPastSessionsByStatus(today, SessionStatus.PLANNED);
            
            if (pastSessions.isEmpty()) {
                logJobEnd("SessionAutoUpdate", "No past sessions to update");
                return;
            }

            logJobInfo(String.format("Found %d past sessions with PLANNED status (date < %s)", 
                pastSessions.size(), today));

            int updatedSessionCount = sessionRepository.updatePastSessionsStatus(
                today, SessionStatus.PLANNED, SessionStatus.DONE);
            
            int updatedAttendanceCount = 0;
            List<StudentSession> studentSessionsToSave = new ArrayList<>();
            
            // Update attendance for affected sessions
            for (Session session : pastSessions) {
                // Auto-mark students with PLANNED attendance as ABSENT
                List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(session.getId());
                for (StudentSession studentSession : studentSessions) {
                    if (studentSession.getAttendanceStatus() == AttendanceStatus.PLANNED) {
                        studentSession.setAttendanceStatus(AttendanceStatus.ABSENT);
                        studentSession.setRecordedAt(OffsetDateTime.now());
                        studentSessionsToSave.add(studentSession);
                        updatedAttendanceCount++;
                    }
                }
            }
            
            // Batch save all updated student sessions
            if (!studentSessionsToSave.isEmpty()) {
                studentSessionRepository.saveAll(studentSessionsToSave);
            }

            logJobInfo(String.format("Updated %d sessions to DONE status", updatedSessionCount));
            logJobInfo(String.format("Auto-marked %d attendance records as ABSENT", updatedAttendanceCount));
            logJobEnd("SessionAutoUpdate", updatedSessionCount);

        } catch (Exception e) {
            logJobError("SessionAutoUpdate", e);
            throw e;
        }
    }

    @Transactional
    public void updatePastSessionsToDoneNow() {
        updatePastSessionsToDone();
    }
}
