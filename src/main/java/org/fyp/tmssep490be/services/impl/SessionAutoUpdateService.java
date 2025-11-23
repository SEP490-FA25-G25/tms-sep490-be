package org.fyp.tmssep490be.services.impl;

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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to automatically update session status and attendance when date passes
 * Runs daily at 1:00 AM to mark past sessions as DONE and auto-absent students
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionAutoUpdateService {

    private final SessionRepository sessionRepository;
    private final StudentSessionRepository studentSessionRepository;

    /**
     * Automatically mark sessions as DONE if their date has passed
     * Also marks students with PLANNED attendance as ABSENT
     * Runs daily at 1:00 AM
     */
    @Scheduled(cron = "0 0 1 * * ?") // Every day at 1:00 AM
    @Transactional
    public void updatePastSessionsToDone() {
        LocalDate today = LocalDate.now();
        List<Session> pastSessions = sessionRepository.findPastSessionsByStatus(today, SessionStatus.PLANNED);
        
        if (pastSessions.isEmpty()) {
            log.debug("No past sessions to update to DONE status");
            return;
        }

        int updatedSessionCount = 0;
        int updatedAttendanceCount = 0;
        List<StudentSession> studentSessionsToSave = new ArrayList<>();
        
        for (Session session : pastSessions) {
            // Update session status to DONE
            session.setStatus(SessionStatus.DONE);
            sessionRepository.save(session); // Explicitly save to ensure changes are flushed
            updatedSessionCount++;
            
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

        log.info("Automatically updated {} past sessions to DONE status and {} attendance records to ABSENT (date < {})", 
                updatedSessionCount, updatedAttendanceCount, today);
    }

    /**
     * Manually trigger the update of past sessions to DONE status and auto-mark absent students.
     * This method can be called manually (e.g., from admin endpoints) if immediate update is needed.
     * Note: The scheduled task runs daily at 1:00 AM, so manual calls are usually not necessary.
     */
    @Transactional
    public void updatePastSessionsToDoneNow() {
        updatePastSessionsToDone();
    }
}


