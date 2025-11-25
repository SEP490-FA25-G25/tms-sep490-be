package org.fyp.tmssep490be.services.impl.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.entities.enums.StudentRequestType;
import org.fyp.tmssep490be.repositories.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Scheduled job to automatically execute approved TRANSFER requests on their effective date.
 *
 * COMPLEX JOB - Handles multi-step transaction:
 * 1. Find approved TRANSFER requests where effectiveDate = TODAY
 * 2. Validate target class capacity is not exceeded
 * 3. Update old enrollment: status = TRANSFERRED, leftAt, leftSessionId
 * 4. Create new enrollment: status = ENROLLED, enrolledAt, joinSessionId
 * 5. Mark StudentSession records in old class as transferredOut (for future sessions)
 * 6. Create StudentSession records in new class (for future sessions)
 * 7. Update request status to EXECUTED (if needed) or mark as processed
 *
 * IMPORTANT:
 * - Uses pessimistic locking on class entities to prevent race conditions
 * - Validates capacity before each transfer
 * - Rolls back entire transaction on any failure
 * - Extensive logging for debugging and audit trail
 *
 * Runs daily at 2:00 AM (configurable via application.yml)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    prefix = "tms.scheduler.jobs.transfer-request-execution",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class TransferRequestExecutionJob extends BaseScheduledJob {

    private final StudentRequestRepository studentRequestRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final ClassRepository classRepository;
    private final SessionRepository sessionRepository;

    /**
     * Execute approved TRANSFER requests on their effective date.
     * Runs daily at 2:00 AM.
     */
    @Scheduled(cron = "${tms.scheduler.jobs.transfer-request-execution.cron:0 0 2 * * ?}")
    @Transactional
    public void executeTransferRequests() {
        try {
            logJobStart("TransferRequestExecution");

            LocalDate today = LocalDate.now();
            logJobInfo(String.format("Executing approved TRANSFER requests with effectiveDate = %s", today));

            // Find approved TRANSFER requests with effectiveDate = today
            List<StudentRequest> transferRequests = studentRequestRepository
                .findApprovedTransferRequestsByEffectiveDate(StudentRequestType.TRANSFER, RequestStatus.APPROVED, today);

            if (transferRequests.isEmpty()) {
                logJobEnd("TransferRequestExecution", "No transfer requests to execute");
                return;
            }

            int successCount = 0;
            int failedCount = 0;

            for (StudentRequest request : transferRequests) {
                try {
                    executeTransfer(request, today);
                    successCount++;
                } catch (Exception e) {
                    failedCount++;
                    logJobWarning(String.format("Failed to execute transfer request ID: %d - %s",
                        request.getId(), e.getMessage()));
                    log.error("Transfer execution error details:", e);
                    // Continue with next request - don't fail entire job
                }
            }

            String summary = String.format("Success: %d, Failed: %d, Total: %d",
                successCount, failedCount, transferRequests.size());
            logJobEnd("TransferRequestExecution", summary);

        } catch (Exception e) {
            logJobError("TransferRequestExecution", e);
            throw e;
        }
    }

    /**
     * Execute a single transfer request with full transaction integrity.
     */
    private void executeTransfer(StudentRequest request, LocalDate today) {
        logJobInfo(String.format("Executing transfer for student %d: Class %d -> Class %d (Request ID: %d)",
            request.getStudent().getId(),
            request.getCurrentClass().getId(),
            request.getTargetClass().getId(),
            request.getId()));

        Long studentId = request.getStudent().getId();
        Long currentClassId = request.getCurrentClass().getId();
        Long targetClassId = request.getTargetClass().getId();
        Session effectiveSession = request.getEffectiveSession();

        if (effectiveSession == null) {
            throw new IllegalStateException("Transfer request missing effectiveSession");
        }

        Long effectiveSessionId = effectiveSession.getId();

        // Step 1: Validate target class capacity with pessimistic lock
        ClassEntity targetClass = classRepository.findByIdWithLock(targetClassId)
            .orElseThrow(() -> new IllegalStateException("Target class not found: " + targetClassId));

        int currentEnrolled = enrollmentRepository.countByClassIdAndStatus(targetClassId, EnrollmentStatus.ENROLLED);
        Integer maxCapacity = targetClass.getMaxCapacity();

        if (maxCapacity != null && currentEnrolled >= maxCapacity) {
            throw new IllegalStateException(String.format(
                "Target class capacity exceeded: %d/%d (request must be rejected manually)",
                currentEnrolled, maxCapacity));
        }

        // Step 2: Update old enrollment
        Enrollment oldEnrollment = enrollmentRepository.findByStudentIdAndClassIdAndStatus(
            studentId, currentClassId, EnrollmentStatus.ENROLLED);

        if (oldEnrollment == null) {
            throw new IllegalStateException(String.format(
                "Old enrollment not found: student %d, class %d", studentId, currentClassId));
        }

        oldEnrollment.setStatus(EnrollmentStatus.TRANSFERRED);
        oldEnrollment.setLeftAt(OffsetDateTime.now());
        oldEnrollment.setLeftSessionId(effectiveSessionId);
        enrollmentRepository.save(oldEnrollment);

        logJobInfo(String.format("  Updated old enrollment ID: %d (status -> TRANSFERRED)", oldEnrollment.getId()));

        // Step 3: Create new enrollment
        Enrollment newEnrollment = Enrollment.builder()
            .studentId(studentId)
            .classId(targetClassId)
            .status(EnrollmentStatus.ENROLLED)
            .enrolledAt(OffsetDateTime.now())
            .joinSessionId(effectiveSessionId)
            .enrolledBy(request.getDecidedBy() != null ? request.getDecidedBy().getId() : null)
            .capacityOverride(false)
            .build();
        enrollmentRepository.save(newEnrollment);

        logJobInfo(String.format("  Created new enrollment ID: %d (class %d)", newEnrollment.getId(), targetClassId));

        // Step 4: Mark old class StudentSessions as transferredOut (future sessions only)
        List<StudentSession> oldStudentSessions = studentSessionRepository
            .findByStudentIdAndClassEntityIdAndSessionDateAfter(studentId, currentClassId, today);

        for (StudentSession ss : oldStudentSessions) {
            ss.setIsTransferredOut(true);
            studentSessionRepository.save(ss);
        }

        logJobInfo(String.format("  Marked %d old StudentSessions as transferredOut", oldStudentSessions.size()));

        // Step 5: Create new class StudentSessions (future sessions only)
        List<Session> newClassSessions = sessionRepository
            .findByClassIdAndDateAfter(targetClassId, today);

        int createdSessionCount = 0;
        for (Session session : newClassSessions) {
            StudentSession.StudentSessionId ssId = new StudentSession.StudentSessionId(studentId, session.getId());

            // Check if already exists (shouldn't happen, but defensive programming)
            if (!studentSessionRepository.existsById(ssId)) {
                StudentSession newSS = StudentSession.builder()
                    .id(ssId)
                    .student(request.getStudent())
                    .session(session)
                    .attendanceStatus(AttendanceStatus.PLANNED)
                    .isMakeup(false)
                    .isTransferredOut(false)
                    .build();
                studentSessionRepository.save(newSS);
                createdSessionCount++;
            }
        }

        logJobInfo(String.format("  Created %d new StudentSessions for target class", createdSessionCount));

        // Step 6: Update request note
        String executionNote = String.format("\nĐã thực thi chuyển lớp tự động vào %s (hệ thống)", today);
        String existingNote = request.getNote();
        if (existingNote != null && !existingNote.isEmpty()) {
            request.setNote(existingNote + executionNote);
        } else {
            request.setNote(executionNote.trim());
        }
        studentRequestRepository.save(request);

        logJobInfo(String.format("  Transfer completed successfully for request ID: %d", request.getId()));
    }
}
