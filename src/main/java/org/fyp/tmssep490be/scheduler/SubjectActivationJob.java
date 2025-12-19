package org.fyp.tmssep490be.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.Subject;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.SubjectStatus;
import org.fyp.tmssep490be.repositories.SubjectRepository;
import org.fyp.tmssep490be.services.SubjectService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduled job to automatically activate subjects on their effective date.
 *
 * Functionality:
 * - Activates subjects when effectiveDate is reached
 * - Only activates subjects that are:
 * 1. status = PENDING_ACTIVATION
 * 2. approvalStatus = APPROVED
 * 3. effectiveDate <= TODAY
 * - Updates status from PENDING_ACTIVATION to ACTIVE
 * - Cascades ACTIVE status to Level and Curriculum
 *
 * Runs daily at 1:00 AM (configurable via application.yml)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "tms.scheduler.jobs.subject-activation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SubjectActivationJob extends BaseScheduledJob {

    private final SubjectRepository subjectRepository;
    private final SubjectService subjectService;

    /**
     * Activate subjects on their effective date.
     * Runs daily at 1:00 AM.
     */
    @Scheduled(cron = "${tms.scheduler.jobs.subject-activation.cron:0 0 1 * * ?}")
    @Transactional
    public void activateSubjects() {
        try {
            logJobStart("SubjectActivation");

            LocalDate today = LocalDate.now();
            logJobInfo(String.format(
                    "Checking subjects with effectiveDate <= %s, status = PENDING_ACTIVATION, approvalStatus = APPROVED",
                    today));

            List<Subject> subjectsToActivate = subjectRepository
                    .findByEffectiveDateBeforeOrEqualAndStatusAndApprovalStatus(
                            today,
                            SubjectStatus.PENDING_ACTIVATION,
                            ApprovalStatus.APPROVED);

            if (subjectsToActivate.isEmpty()) {
                logJobEnd("SubjectActivation", "No subjects to activate");
                return;
            }

            int activatedCount = 0;
            for (Subject subject : subjectsToActivate) {
                logJobInfo(String.format("Activating subject '%s' (ID: %d, code: %s, effectiveDate: %s)",
                        subject.getName(), subject.getId(), subject.getCode(), subject.getEffectiveDate()));

                subject.setStatus(SubjectStatus.ACTIVE);
                subjectRepository.save(subject);

                // Cascade ACTIVE status to Level and Curriculum
                subjectService.activateLevelAndCurriculumIfNeeded(subject);

                activatedCount++;
            }

            logJobEnd("SubjectActivation", activatedCount);

        } catch (Exception e) {
            logJobError("SubjectActivation", e);
            throw e;
        }
    }
}
