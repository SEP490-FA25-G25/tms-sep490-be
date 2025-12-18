package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.classcreation.SubmitClassResponse;
import org.fyp.tmssep490be.dtos.classcreation.ValidateClassResponse;
import org.fyp.tmssep490be.entities.Assessment;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.SubjectAssessment;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.NotificationType;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.utils.ScheduleUtils;
import org.fyp.tmssep490be.repositories.AssessmentRepository;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.repositories.SubjectAssessmentRepository;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalService {

    private final ClassRepository classRepository;
    private final UserAccountRepository userAccountRepository;
    private final ValidationService validationService;
    private final NotificationService notificationService;
    private final SubjectAssessmentRepository subjectAssessmentRepository;
    private final AssessmentRepository assessmentRepository;

    @Transactional
    public SubmitClassResponse submitForApproval(Long classId, Long submitterUserId) {
        log.info("Submitting class ID: {} for approval by user: {}", classId, submitterUserId);

        try {
            // Validate class exists
            ClassEntity classEntity = classRepository.findById(classId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

            // Validate class is not already submitted
            if (classEntity.getSubmittedAt() != null || classEntity.getStatus() == ClassStatus.SUBMITTED) {
                throw new CustomException(ErrorCode.CLASS_ALREADY_SUBMITTED);
            }

            // Validate class is not already approved
            if (classEntity.getApprovalStatus() == ApprovalStatus.APPROVED) {
                throw new CustomException(ErrorCode.CLASS_ALREADY_APPROVED);
            }

            // Validate class is complete using ValidationService
            ValidateClassResponse validationResponse = validationService.validateClassComplete(classId);
            if (validationResponse == null || !Boolean.TRUE.equals(validationResponse.getCanSubmit())) {
                throw new CustomException(ErrorCode.CLASS_INCOMPLETE_CANNOT_SUBMIT);
            }

            // Update class submission details
            classEntity.setSubmittedAt(OffsetDateTime.now());
            classEntity.setStatus(ClassStatus.SUBMITTED);
            classEntity.setApprovalStatus(ApprovalStatus.PENDING);

            ClassEntity savedClass = classRepository.save(classEntity);

            // Send notification to CENTER_HEAD of the branch
            sendNotificationForClassSubmission(savedClass);

            log.info("Class ID: {} successfully submitted for approval", classId);

            return SubmitClassResponse.builder()
                    .classId(savedClass.getId())
                    .classCode(savedClass.getCode())
                    .message("Lớp học đã được gửi duyệt thành công")
                    .success(true)
                    .submittedAt(savedClass.getSubmittedAt().toString())
                    .approvalStatus(savedClass.getApprovalStatus().toString())
                    .build();

        } catch (CustomException e) {
            log.error("Error submitting class ID: {} for approval: {}", classId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error submitting class ID: {} for approval", classId, e);
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    @Transactional
    public void approveClass(Long classId, Long approverUserId) {
        log.info("Approving class ID: {} by user: {}", classId, approverUserId);

        try {
            ClassEntity classEntity = classRepository.findById(classId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

            // Validate class is submitted
            if (classEntity.getSubmittedAt() == null || classEntity.getStatus() != ClassStatus.SUBMITTED) {
                throw new CustomException(ErrorCode.CLASS_NOT_SUBMITTED);
            }

            // Validate class is not already approved
            if (classEntity.getApprovalStatus() == ApprovalStatus.APPROVED) {
                throw new CustomException(ErrorCode.CLASS_ALREADY_APPROVED);
            }

            // Validate class is pending approval
            if (classEntity.getApprovalStatus() != ApprovalStatus.PENDING) {
                throw new CustomException(ErrorCode.INVALID_APPROVAL_STATUS);
            }

            // Get approver user
            UserAccount approver = userAccountRepository.findById(approverUserId).orElse(null);

            // Update class approval details
            classEntity.setApprovalStatus(ApprovalStatus.APPROVED);
            classEntity.setStatus(ClassStatus.SCHEDULED);
            classEntity.setDecidedAt(OffsetDateTime.now());
            classEntity.setDecidedBy(approver);
            classEntity.setRejectionReason(null); // Clear any previous rejection reason

            ClassEntity savedClass = classRepository.save(classEntity);

            // Create assessments for the class from subject assessment templates
            createAssessmentsForClass(savedClass);

            // Send notification to class creator
            sendNotificationForClassApproval(savedClass);

            log.info("Class ID: {} successfully approved by user: {}", classId, approverUserId);

        } catch (CustomException e) {
            log.error("Error approving class ID: {}: {}", classId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error approving class ID: {}", classId, e);
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    /**
     * Reject a submitted class
     * Changes status back to DRAFT and approvalStatus to REJECTED
     * Sends notification to the class creator with rejection reason
     */
    @Transactional
    public void rejectClass(Long classId, String reason, Long rejecterUserId) {
        log.info("Rejecting class ID: {} by user: {} with reason: {}", classId, rejecterUserId, reason);

        try {
            // Validate rejection reason
            if (reason == null || reason.trim().isEmpty()) {
                throw new CustomException(ErrorCode.CLASS_REJECTION_REASON_REQUIRED);
            }

            ClassEntity classEntity = classRepository.findById(classId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

            // Validate class is submitted
            if (classEntity.getSubmittedAt() == null) {
                throw new CustomException(ErrorCode.CLASS_NOT_SUBMITTED);
            }

            // Validate class is not already rejected
            if (classEntity.getApprovalStatus() == ApprovalStatus.REJECTED) {
                throw new CustomException(ErrorCode.INVALID_APPROVAL_STATUS);
            }

            // Validate class is pending approval
            if (classEntity.getApprovalStatus() != ApprovalStatus.PENDING) {
                throw new CustomException(ErrorCode.INVALID_APPROVAL_STATUS);
            }

            // Get rejecter user
            UserAccount rejecter = userAccountRepository.findById(rejecterUserId).orElse(null);

            // Update class rejection details
            classEntity.setApprovalStatus(ApprovalStatus.REJECTED);
            classEntity.setStatus(ClassStatus.DRAFT);
            classEntity.setDecidedAt(OffsetDateTime.now());
            classEntity.setDecidedBy(rejecter);
            classEntity.setRejectionReason(reason.trim());
            classEntity.setSubmittedAt(null); // Reset - needs to be resubmitted

            classRepository.save(classEntity);

            // Send notification to class creator
            sendNotificationForClassRejection(classEntity, reason.trim());

            log.info("Class ID: {} successfully rejected by user: {}", classId, rejecterUserId);

        } catch (CustomException e) {
            log.error("Error rejecting class ID: {}: {}", classId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error rejecting class ID: {}", classId, e);
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    // ==================== ASSESSMENT CREATION ====================

    /**
     * Creates Assessment records for the class based on SubjectAssessment
     * templates.
     * Scheduled date defaults to planned_end_date - teachers can edit later.
     */
    private void createAssessmentsForClass(ClassEntity classEntity) {
        try {
            if (classEntity.getSubject() == null) {
                log.warn("Class {} has no subject, skipping assessment creation", classEntity.getId());
                return;
            }

            Long subjectId = classEntity.getSubject().getId();
            List<SubjectAssessment> templates = subjectAssessmentRepository.findBySubjectId(subjectId);

            if (templates.isEmpty()) {
                log.info("No SubjectAssessment templates found for subject {}, skipping", subjectId);
                return;
            }

            // Default scheduled date: use planned_end_date or start_date + 30 days
            OffsetDateTime defaultScheduledDate;
            if (classEntity.getPlannedEndDate() != null) {
                defaultScheduledDate = classEntity.getPlannedEndDate()
                        .atStartOfDay()
                        .atOffset(ZoneOffset.UTC);
            } else if (classEntity.getStartDate() != null) {
                defaultScheduledDate = classEntity.getStartDate()
                        .plusDays(30)
                        .atStartOfDay()
                        .atOffset(ZoneOffset.UTC);
            } else {
                defaultScheduledDate = OffsetDateTime.now().plusDays(30);
            }

            int createdCount = 0;
            for (SubjectAssessment template : templates) {
                Assessment assessment = Assessment.builder()
                        .classEntity(classEntity)
                        .subjectAssessment(template)
                        .scheduledDate(defaultScheduledDate)
                        .createdAt(OffsetDateTime.now())
                        .updatedAt(OffsetDateTime.now())
                        .build();
                assessmentRepository.save(assessment);
                createdCount++;
            }

            log.info("Created {} assessments for class {} from subject {} templates",
                    createdCount, classEntity.getId(), subjectId);

        } catch (Exception e) {
            log.error("Error creating assessments for class {}: {}",
                    classEntity.getId(), e.getMessage(), e);
            // Don't throw - assessment creation failure shouldn't block class approval
        }
    }

    // ==================== NOTIFICATION METHODS ====================

    private void sendNotificationForClassSubmission(ClassEntity classEntity) {
        try {
            Long branchId = classEntity.getBranch().getId();

            List<UserAccount> centerHeadUsers = userAccountRepository.findByRoleCodeAndBranches(
                    "CENTER_HEAD", List.of(branchId));

            if (!centerHeadUsers.isEmpty()) {
                String title = String.format("Lớp học chờ duyệt: %s", classEntity.getCode());
                String message = String.format(
                        "Lớp học %s (%s) đã được tạo và chờ được duyệt. Khai giảng: %s, Chi nhánh: %s",
                        classEntity.getCode(),
                        classEntity.getSubject() != null ? classEntity.getSubject().getName() : "N/A",
                        classEntity.getStartDate() != null ? classEntity.getStartDate().toString() : "Chưa xác định",
                        classEntity.getBranch().getName());

                List<Long> recipientIds = centerHeadUsers.stream()
                        .map(UserAccount::getId)
                        .collect(Collectors.toList());

                notificationService.sendBulkNotifications(
                        recipientIds,
                        NotificationType.REQUEST,
                        title,
                        message);

                log.info("Sent notification to {} CENTER_HEAD users for class submission {}",
                        centerHeadUsers.size(), classEntity.getId());
            } else {
                log.warn("No CENTER_HEAD users found for branch {}", branchId);
            }
        } catch (Exception e) {
            log.error("Error sending notification for class submission {}: {}",
                    classEntity.getId(), e.getMessage(), e);
        }
    }

    private void sendNotificationForClassApproval(ClassEntity classEntity) {
        try {
            UserAccount creator = classEntity.getCreatedBy();
            if (creator != null) {
                String title = String.format("Lớp học đã được duyệt: %s", classEntity.getCode());
                String message = String.format(
                        "Lớp học %s đã được phê duyệt và sẽ bắt đầu vào ngày %s. Lịch học: %s",
                        classEntity.getCode(),
                        classEntity.getStartDate() != null ? classEntity.getStartDate().toString() : "sớm",
                        buildScheduleDisplay(classEntity));

                notificationService.sendBulkNotifications(
                        List.of(creator.getId()),
                        NotificationType.NOTIFICATION,
                        title,
                        message);

                log.info("Sent approval notification to creator {} for class {}",
                        creator.getId(), classEntity.getId());
            }
        } catch (Exception e) {
            log.error("Error sending approval notification for class {}: {}",
                    classEntity.getId(), e.getMessage(), e);
        }
    }

    private void sendNotificationForClassRejection(ClassEntity classEntity, String reason) {
        try {
            UserAccount creator = classEntity.getCreatedBy();
            if (creator != null) {
                String title = String.format("Lớp học bị từ chối: %s", classEntity.getCode());
                String message = String.format(
                        "Lớp học %s đã bị từ chối. Lý do: %s. Vui lòng chỉnh sửa và gửi lại.",
                        classEntity.getCode(),
                        reason);

                notificationService.sendBulkNotifications(
                        List.of(creator.getId()),
                        NotificationType.SYSTEM,
                        title,
                        message);

                log.info("Sent rejection notification to creator {} for class {}",
                        creator.getId(), classEntity.getId());
            }
        } catch (Exception e) {
            log.error("Error sending rejection notification for class {}: {}",
                    classEntity.getId(), e.getMessage(), e);
        }
    }

    private String buildScheduleDisplay(ClassEntity classEntity) {
        return ScheduleUtils.generateScheduleDisplayFromMetadata(classEntity);
    }
}
