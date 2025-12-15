package org.fyp.tmssep490be.scheduler;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.Enrollment;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.repositories.EnrollmentRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Scheduled job to automatically complete enrollments when their class is completed.
 *
 * Functionality:
 * - Updates ENROLLED enrollments to COMPLETED when class status is COMPLETED
 * - Only processes enrollments for classes that are actually COMPLETED
 *
 * Runs daily at 2:30 AM (configurable via application.yml)
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "tms.scheduler.jobs.enrollment-auto-complete",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class EnrollmentAutoCompleteJob extends BaseScheduledJob {

    private final ClassRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Scheduled(cron = "${tms.scheduler.jobs.enrollment-auto-complete.cron:0 30 2 * * ?}")
    @Transactional
    public void autoCompleteEnrollments() {
        try {
            logJobStart("EnrollmentAutoComplete");

            // Find all COMPLETED classes
            List<ClassEntity> completedClasses = classRepository.findByStatus(ClassStatus.COMPLETED);

            if (completedClasses.isEmpty()) {
                logJobEnd("EnrollmentAutoComplete", "No completed classes found");
                return;
            }

            int totalCompleted = 0;

            for (ClassEntity classEntity : completedClasses) {
                // Find all ENROLLED enrollments for this class
                List<Enrollment> enrolledStudents = enrollmentRepository.findByClassIdAndStatus(
                    classEntity.getId(), EnrollmentStatus.ENROLLED);

                if (enrolledStudents.isEmpty()) {
                    continue;
                }

                int completedCount = 0;
                for (Enrollment enrollment : enrolledStudents) {
                    enrollment.setStatus(EnrollmentStatus.COMPLETED);
                    enrollmentRepository.save(enrollment);
                    completedCount++;
                }

                if (completedCount > 0) {
                    logJobInfo(String.format("Completed %d enrollments for class '%s' (ID: %d)",
                        completedCount, classEntity.getCode(), classEntity.getId()));
                    totalCompleted += completedCount;
                }
            }

            logJobEnd("EnrollmentAutoComplete", totalCompleted);

        } catch (Exception e) {
            logJobError("EnrollmentAutoComplete", e);
            throw e;
        }
    }
}

