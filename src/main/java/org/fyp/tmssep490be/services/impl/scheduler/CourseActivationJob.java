package org.fyp.tmssep490be.services.impl.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.Course;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.CourseStatus;
import org.fyp.tmssep490be.repositories.CourseRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduled job to automatically activate courses on their effective date.
 *
 * Functionality:
 * - Activates courses when effectiveDate is reached
 * - Only activates courses that are:
 *   1. status = DRAFT
 *   2. approvalStatus = APPROVED
 *   3. effectiveDate = TODAY or past
 * - Updates status from DRAFT to ACTIVE
 *
 * Runs daily at 1:00 AM (configurable via application.yml)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    prefix = "tms.scheduler.jobs.course-activation",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class CourseActivationJob extends BaseScheduledJob {

    private final CourseRepository courseRepository;

    /**
     * Activate courses on their effective date.
     * Runs daily at 1:00 AM.
     */
    @Scheduled(cron = "${tms.scheduler.jobs.course-activation.cron:0 0 1 * * ?}")
    @Transactional
    public void activateCourses() {
        try {
            logJobStart("CourseActivation");

            LocalDate today = LocalDate.now();
            logJobInfo(String.format("Checking courses with effectiveDate <= %s, status = DRAFT, approvalStatus = APPROVED",
                today));

            List<Course> coursesToActivate = courseRepository
                .findByEffectiveDateBeforeOrEqualAndStatusAndApprovalStatus(
                    today,
                    CourseStatus.DRAFT,
                    ApprovalStatus.APPROVED
                );

            if (coursesToActivate.isEmpty()) {
                logJobEnd("CourseActivation", "No courses to activate");
                return;
            }

            int activatedCount = 0;
            for (Course course : coursesToActivate) {
                logJobInfo(String.format("Activating course '%s' (ID: %d, code: %s, effectiveDate: %s)",
                    course.getName(), course.getId(), course.getCode(), course.getEffectiveDate()));

                course.setStatus(CourseStatus.ACTIVE);
                courseRepository.save(course);
                activatedCount++;
            }

            logJobEnd("CourseActivation", activatedCount);

        } catch (Exception e) {
            logJobError("CourseActivation", e);
            throw e;
        }
    }
}
