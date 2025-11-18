package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service to automatically update session status when date passes
 * Runs daily at 1:00 AM to mark past sessions as DONE
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionAutoUpdateService {

    private final SessionRepository sessionRepository;

    /**
     * Automatically mark sessions as DONE if their date has passed
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

        int updatedCount = 0;
        for (Session session : pastSessions) {
            session.setStatus(SessionStatus.DONE);
            updatedCount++;
        }

        log.info("Automatically updated {} past sessions to DONE status (date < {})", updatedCount, today);
    }
}


