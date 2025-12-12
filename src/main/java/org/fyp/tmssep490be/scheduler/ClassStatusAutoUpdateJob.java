package org.fyp.tmssep490be.scheduler;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduled job to automatically update class status based on dates and session completion.
 *
 * Functionality:
 * - Updates SCHEDULED classes to ONGOING when startDate is reached
 * - Updates ONGOING classes to COMPLETED when:
 *   1. plannedEndDate has passed
 *   2. All sessions are DONE or CANCELLED (no PLANNED or ONGOING sessions remain)
 * - Sets actualEndDate when class is marked as COMPLETED
 *
 * Runs daily at 2:00 AM (configurable via application.yml)
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "tms.scheduler.jobs.class-status-update",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class ClassStatusAutoUpdateJob extends BaseScheduledJob {

    private final ClassRepository classRepository;
    private final SessionRepository sessionRepository;

    @Scheduled(cron = "${tms.scheduler.jobs.class-status-update.cron:0 0 2 * * ?}")
    @Transactional
    public void updateClassStatuses() {
        try {
            logJobStart("ClassStatusAutoUpdate");

            LocalDate today = LocalDate.now();
            int updatedToOngoingCount = 0;
            int updatedToCompletedCount = 0;

            // 1. Update SCHEDULED → ONGOING when startDate is reached
            List<ClassEntity> scheduledClasses = classRepository
                .findByStatus(ClassStatus.SCHEDULED)
                .stream()
                .filter(c -> c.getStartDate() != null && !c.getStartDate().isAfter(today))
                .toList();

            for (ClassEntity classEntity : scheduledClasses) {
                classEntity.setStatus(ClassStatus.ONGOING);
                classRepository.save(classEntity);
                logJobInfo(String.format("Updated class '%s' (ID: %d) from SCHEDULED to ONGOING (startDate: %s)",
                    classEntity.getCode(), classEntity.getId(), classEntity.getStartDate()));
                updatedToOngoingCount++;
            }

            // 2. Update ONGOING → COMPLETED when plannedEndDate passed and all sessions are done
            List<ClassEntity> ongoingClasses = classRepository
                .findByStatus(ClassStatus.ONGOING)
                .stream()
                .filter(c -> c.getPlannedEndDate() != null && c.getPlannedEndDate().isBefore(today))
                .toList();

            for (ClassEntity classEntity : ongoingClasses) {
                // Check if all non-cancelled sessions are DONE
                Long totalNonCancelledSessions = sessionRepository.countNonCancelledSessionsByClassId(
                    classEntity.getId());
                Long doneSessions = sessionRepository.countByClassEntityIdAndStatus(
                    classEntity.getId(), SessionStatus.DONE);

                // If all non-cancelled sessions are done, mark class as COMPLETED
                if (totalNonCancelledSessions != null && totalNonCancelledSessions > 0 
                    && doneSessions != null 
                    && doneSessions.equals(totalNonCancelledSessions)) {
                    
                    classEntity.setStatus(ClassStatus.COMPLETED);
                    if (classEntity.getActualEndDate() == null) {
                        classEntity.setActualEndDate(today);
                    }
                    classRepository.save(classEntity);
                    logJobInfo(String.format("Updated class '%s' (ID: %d) from ONGOING to COMPLETED (plannedEndDate: %s, actualEndDate: %s)",
                        classEntity.getCode(), classEntity.getId(), 
                        classEntity.getPlannedEndDate(), classEntity.getActualEndDate()));
                    updatedToCompletedCount++;
                } else {
                    logJobWarning(String.format("Class '%s' (ID: %d) has plannedEndDate passed but still has active sessions (total non-cancelled: %d, done: %d)",
                        classEntity.getCode(), classEntity.getId(), totalNonCancelledSessions, doneSessions));
                }
            }

            int totalUpdated = updatedToOngoingCount + updatedToCompletedCount;
            if (totalUpdated > 0) {
                logJobInfo(String.format("Updated %d classes to ONGOING", updatedToOngoingCount));
                logJobInfo(String.format("Updated %d classes to COMPLETED", updatedToCompletedCount));
            }

            logJobEnd("ClassStatusAutoUpdate", totalUpdated);

        } catch (Exception e) {
            logJobError("ClassStatusAutoUpdate", e);
            throw e;
        }
    }
}

