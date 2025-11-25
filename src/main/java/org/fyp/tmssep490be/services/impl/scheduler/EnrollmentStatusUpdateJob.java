package org.fyp.tmssep490be.services.impl.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.Enrollment;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;
import org.fyp.tmssep490be.repositories.EnrollmentRepository;
import org.fyp.tmssep490be.services.NotificationService;
import org.fyp.tmssep490be.entities.enums.NotificationType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Job 6: Enrollment Status Update Job
 * Auto-complete enrollments when classes finish
 * Schedule: Daily 1:00 AM (after CourseActivationJob)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    prefix = "tms.scheduler.jobs.enrollment-status-update",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class EnrollmentStatusUpdateJob extends BaseScheduledJob {

    private final EnrollmentRepository enrollmentRepository;
    private final NotificationService notificationService;

    /**
     * Main scheduled method to update enrollment statuses
     * Runs daily at 1:00 AM by default
     */
    @Scheduled(cron = "${tms.scheduler.jobs.enrollment-status-update.cron:0 0 1 * * ?}")
    @Transactional
    public void updateEnrollmentsForCompletedClasses() {
        logJobStart("EnrollmentStatusUpdateJob");

        try {
            // Find enrollments where class is completed but enrollment status is still ENROLLED
            List<Enrollment> enrollmentsToUpdate = enrollmentRepository
                .findByClassEntityStatusAndEnrollmentStatus("COMPLETED", "ENROLLED");

            if (enrollmentsToUpdate.isEmpty()) {
                logJobEnd("EnrollmentStatusUpdateJob", "No enrollments to update");
                return;
            }

            logJobInfo(String.format("Found %d enrollments to update", enrollmentsToUpdate.size()));

            int updatedCount = 0;
            for (Enrollment enrollment : enrollmentsToUpdate) {
                try {
                    // Update enrollment status
                    enrollment.setStatus(EnrollmentStatus.COMPLETED);

                    // Set leftAt if not already set
                    if (enrollment.getLeftAt() == null) {
                        enrollment.setLeftAt(OffsetDateTime.now());
                    }

                    enrollmentRepository.save(enrollment);
                    updatedCount++;

                    // Create notification for student
                    if (enrollment.getStudent() != null && enrollment.getStudent().getUserAccount() != null) {
                        notificationService.createNotificationWithReference(
                            enrollment.getStudent().getUserAccount().getId(),
                            NotificationType.SYSTEM_ALERT,
                            "Hoàn thành khóa học",
                            String.format("Bạn đã hoàn thành lớp %s. Chúc mừng!",
                                enrollment.getClassEntity() != null ? enrollment.getClassEntity().getName() : "không xác định"),
                            "ENROLLMENT",
                            enrollment.getId()
                        );

                        logJobInfo(String.format("Created completion notification for student %d",
                            enrollment.getStudent().getUserAccount().getId()));
                    }

                } catch (Exception e) {
                    logJobWarning(String.format("Failed to update enrollment %d: %s",
                        enrollment.getId(), e.getMessage()));
                }
            }

            logJobEnd("EnrollmentStatusUpdateJob",
                String.format("Updated %d/%d enrollments", updatedCount, enrollmentsToUpdate.size()));

        } catch (Exception e) {
            logJobError("EnrollmentStatusUpdateJob", e);
            throw e; // Re-throw to prevent silent failures
        }
    }
}