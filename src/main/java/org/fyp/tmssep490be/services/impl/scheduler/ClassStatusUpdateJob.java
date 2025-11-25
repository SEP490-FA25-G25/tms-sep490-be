package org.fyp.tmssep490be.services.impl.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduled job to automatically update class status based on dates.
 *
 * Status Transitions:
 * 1. SCHEDULED -> ONGOING: When startDate is reached
 * 2. ONGOING -> COMPLETED: When plannedEndDate is reached
 *
 * Runs daily at 12:30 AM (configurable via application.yml)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    prefix = "tms.scheduler.jobs.class-status-update",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class ClassStatusUpdateJob extends BaseScheduledJob {

    private final ClassRepository classRepository;

    /**
     * Update class status from SCHEDULED to ONGOING when startDate is reached.
     * Runs daily at 12:30 AM.
     */
    @Scheduled(cron = "${tms.scheduler.jobs.class-status-update.cron:0 30 0 * * ?}")
    @Transactional
    public void updateScheduledToOngoing() {
        try {
            logJobStart("ClassStatusUpdate: SCHEDULED->ONGOING");

            LocalDate today = LocalDate.now();
            logJobInfo("Checking classes with startDate <= " + today + " and status = SCHEDULED");

            List<ClassEntity> scheduledClasses = classRepository
                .findByStartDateBeforeOrEqualAndStatus(today, ClassStatus.SCHEDULED);

            if (scheduledClasses.isEmpty()) {
                logJobEnd("ClassStatusUpdate: SCHEDULED->ONGOING", "No classes to update");
                return;
            }

            int updatedCount = 0;
            for (ClassEntity classEntity : scheduledClasses) {
                logJobInfo(String.format("Updating class %s (ID: %d) from SCHEDULED to ONGOING",
                    classEntity.getCode(), classEntity.getId()));
                classEntity.setStatus(ClassStatus.ONGOING);
                classRepository.save(classEntity);
                updatedCount++;
            }

            logJobEnd("ClassStatusUpdate: SCHEDULED->ONGOING", updatedCount);

        } catch (Exception e) {
            logJobError("ClassStatusUpdate: SCHEDULED->ONGOING", e);
            throw e;
        }
    }

    /**
     * Update class status from ONGOING to COMPLETED when plannedEndDate is reached.
     * Runs daily at 12:30 AM.
     */
    @Scheduled(cron = "${tms.scheduler.jobs.class-status-update.cron:0 30 0 * * ?}")
    @Transactional
    public void updateOngoingToCompleted() {
        try {
            logJobStart("ClassStatusUpdate: ONGOING->COMPLETED");

            LocalDate today = LocalDate.now();
            logJobInfo("Checking classes with plannedEndDate <= " + today + " and status = ONGOING");

            List<ClassEntity> ongoingClasses = classRepository
                .findByPlannedEndDateBeforeOrEqualAndStatus(today, ClassStatus.ONGOING);

            if (ongoingClasses.isEmpty()) {
                logJobEnd("ClassStatusUpdate: ONGOING->COMPLETED", "No classes to update");
                return;
            }

            int updatedCount = 0;
            for (ClassEntity classEntity : ongoingClasses) {
                logJobInfo(String.format("Updating class %s (ID: %d) from ONGOING to COMPLETED",
                    classEntity.getCode(), classEntity.getId()));
                classEntity.setStatus(ClassStatus.COMPLETED);
                classEntity.setActualEndDate(today); // Record actual end date
                classRepository.save(classEntity);
                updatedCount++;
            }

            logJobEnd("ClassStatusUpdate: ONGOING->COMPLETED", updatedCount);

        } catch (Exception e) {
            logJobError("ClassStatusUpdate: ONGOING->COMPLETED", e);
            throw e;
        }
    }
}
