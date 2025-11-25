package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.classmanagement.RejectClassResponse;
import org.fyp.tmssep490be.dtos.classmanagement.SubmitClassResponse;
import org.fyp.tmssep490be.dtos.classmanagement.ValidateClassResponse;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.Enrollment;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;
import org.fyp.tmssep490be.entities.enums.NotificationType;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.repositories.EnrollmentRepository;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.fyp.tmssep490be.services.ApprovalService;
import org.fyp.tmssep490be.services.NotificationService;
import org.fyp.tmssep490be.services.ValidationService;
import org.fyp.tmssep490be.utils.ValidateClassResponseUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalServiceImpl implements ApprovalService {

    private final ClassRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserAccountRepository userAccountRepository;
    private final ValidationService validationService;
    private final NotificationService notificationService;
    private final ValidateClassResponseUtil validateClassResponseUtil;

    @Override
    @Transactional
    public SubmitClassResponse submitForApproval(Long classId, Long submitterUserId) {
        log.info("Submitting class ID: {} for approval by user: {}", classId, submitterUserId);

        try {
            // Validate class exists and get user info
            ClassEntity classEntity = classRepository.findById(classId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

            // Validate class is not already submitted
            if (classEntity.getSubmittedAt() != null || classEntity.getStatus() == ClassStatus.SUBMITTED) {
                throw new CustomException(ErrorCode.CLASS_NOT_SUBMITTED);
            }

            // Validate class is not already approved
            if (classEntity.getApprovalStatus() == ApprovalStatus.APPROVED) {
                throw new CustomException(ErrorCode.CLASS_ALREADY_APPROVED);
            }

            // Validate class is complete using ValidationService
            ValidateClassResponse validationResponse = validationService.validateClassComplete(classId);
            if (!validateClassResponseUtil.canSubmit(validationResponse)) {
                throw new CustomException(ErrorCode.CLASS_INCOMPLETE_CANNOT_SUBMIT);
            }

            // Get submitter user details
            UserAccount submitter = classEntity.getCreatedBy();
            if (submitter == null || !submitter.getId().equals(submitterUserId)) {
                log.warn("Submitter user ID mismatch. Expected: {}, Actual: {}",
                        submitter != null ? submitter.getId() : "null", submitterUserId);
            }

            // Update class submission details
            classEntity.setSubmittedAt(OffsetDateTime.now());
            classEntity.setStatus(ClassStatus.SUBMITTED);
            classEntity.setApprovalStatus(ApprovalStatus.PENDING);

            ClassEntity savedClass = classRepository.save(classEntity);

            // Gửi notification cho Academic Affairs khi class được submit
            sendNotificationForClassSubmission(savedClass);

            log.info("Class ID: {} successfully submitted for approval", classId);

            return SubmitClassResponse.builder()
                    .classId(savedClass.getId())
                    .classCode(savedClass.getCode())
                    .message("Class successfully submitted for approval")
                    .success(true)
                    .submittedAt(savedClass.getSubmittedAt().toString())
                    .approvalStatus(savedClass.getApprovalStatus().toString())
                    .build();

        } catch (CustomException e) {
            log.error("Error submitting class ID: {} for approval: {}", classId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error submitting class ID: {} for approval", classId, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public void approveClass(Long classId, Long approverUserId) {
        log.info("Approving class ID: {} by user: {}", classId, approverUserId);

        try {
            // Validate class exists
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

            // TODO: Validate approver has CENTER_HEAD role
            // This would involve checking the user's role through UserAccount or UserService

            // Get approver user details
            UserAccount approver = classEntity.getDecidedBy();
            if (approver == null || !approver.getId().equals(approverUserId)) {
                log.warn("Approver user ID mismatch. Expected: {}, Actual: {}",
                        approver != null ? approver.getId() : "null", approverUserId);
            }

            // Update class approval details
            classEntity.setApprovalStatus(ApprovalStatus.APPROVED);
            classEntity.setStatus(ClassStatus.SCHEDULED);
            classEntity.setDecidedAt(OffsetDateTime.now());
            classEntity.setRejectionReason(null); // Clear any previous rejection reason

            ClassEntity savedClass = classRepository.save(classEntity);

            // Gửi notification cho enrolled students khi class được approve
            sendNotificationForClassApproval(savedClass);

            log.info("Class ID: {} successfully approved by user: {}", classId, approverUserId);

        } catch (CustomException e) {
            log.error("Error approving class ID: {}: {}", classId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error approving class ID: {}", classId, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public RejectClassResponse rejectClass(Long classId, String reason, Long rejecterUserId) {
        log.info("Rejecting class ID: {} by user: {} with reason: {}", classId, rejecterUserId, reason);

        try {
            // Validate rejection reason
            if (reason == null || reason.trim().isEmpty()) {
                throw new CustomException(ErrorCode.CLASS_REJECTION_REASON_REQUIRED);
            }

            // Validate class exists
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

            // TODO: Validate rejecter has CENTER_HEAD role
            // This would involve checking the user's role through UserAccount or UserService

            // Get rejecter user details
            UserAccount rejecter = classEntity.getDecidedBy();
            if (rejecter == null || !rejecter.getId().equals(rejecterUserId)) {
                log.warn("Rejecter user ID mismatch. Expected: {}, Actual: {}",
                        rejecter != null ? rejecter.getId() : "null", rejecterUserId);
            }

            // Update class rejection details
            classEntity.setApprovalStatus(ApprovalStatus.REJECTED);
            classEntity.setStatus(ClassStatus.DRAFT);
            classEntity.setDecidedAt(OffsetDateTime.now());
            classEntity.setRejectionReason(reason.trim());
            classEntity.setSubmittedAt(null); // Reset submission - needs to be resubmitted

            ClassEntity savedClass = classRepository.save(classEntity);

            log.info("Class ID: {} successfully rejected by user: {}", classId, rejecterUserId);

            return RejectClassResponse.builder()
                    .classId(savedClass.getId())
                    .classCode(savedClass.getCode())
                    .message("Class has been rejected and returned to draft status")
                    .success(true)
                    .rejectionReason(savedClass.getRejectionReason())
                    .decidedAt(savedClass.getDecidedAt().toString())
                    .decidedBy(rejecter != null ? rejecter.getFullName() : "Unknown")
                    .build();

        } catch (CustomException e) {
            log.error("Error rejecting class ID: {}: {}", classId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error rejecting class ID: {}", classId, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== NOTIFICATION METHODS ====================

    /**
     * Gửi notification cho Academic Affairs khi class được submit để duyệt
     */
    private void sendNotificationForClassSubmission(ClassEntity classEntity) {
        try {
            // Lấy branch của class để gửi cho Academic Affairs tương ứng
            Long branchId = classEntity.getBranch().getId();

            List<UserAccount> academicAffairsUsers = userAccountRepository.findByRoleCodeAndBranches(
                    "ACADEMIC_AFFAIRS", List.of(branchId));

            if (!academicAffairsUsers.isEmpty()) {
                String title = String.format("Lớp học chờ duyệt: %s", classEntity.getCode());
                // Lấy số lượng enrolled students hiện tại
                Integer currentEnrolled = enrollmentRepository.countByClassIdAndStatus(
                        classEntity.getId(), EnrollmentStatus.ENROLLED);

                String message = String.format(
                        "Lớp học %s (%s) đã được tạo và chờ được duyệt. Khai giảng: %s, Sĩ số: %d/%d",
                        classEntity.getCode(),
                        classEntity.getCourse().getName(),
                        classEntity.getStartDate() != null ? classEntity.getStartDate().toString() : "Chưa xác định",
                        currentEnrolled != null ? currentEnrolled : 0,
                        classEntity.getMaxCapacity()
                );

                List<Long> recipientIds = academicAffairsUsers.stream()
                        .map(UserAccount::getId)
                        .collect(Collectors.toList());

                notificationService.sendBulkNotificationsWithReference(
                        recipientIds,
                        NotificationType.REQUEST_APPROVAL,
                        title,
                        message,
                        "Class",
                        classEntity.getId()
                );

                log.info("Đã gửi notification cho {} Academic Affairs users về class submission {}",
                        academicAffairsUsers.size(), classEntity.getId());
            } else {
                log.warn("Không tìm thấy Academic Affairs user nào cho branch {}", branchId);
            }
        } catch (Exception e) {
            log.error("Lỗi khi gửi notification cho Academic Affairs về class submission {}: {}",
                    classEntity.getId(), e.getMessage(), e);
        }
    }

    /**
     * Gửi notification cho enrolled students khi class được duyệt
     */
    private void sendNotificationForClassApproval(ClassEntity classEntity) {
        try {
            // Lấy danh sách enrolled students của class
            List<Enrollment> enrolledStudents = enrollmentRepository
                    .findByClassIdAndStatus(classEntity.getId(), EnrollmentStatus.ENROLLED);

            if (!enrolledStudents.isEmpty()) {
                List<Long> studentUserIds = enrolledStudents.stream()
                        .map(enrollment -> enrollment.getStudent().getUserAccount().getId())
                        .collect(Collectors.toList());

                String title = String.format("Lớp học đã được duyệt: %s", classEntity.getCode());
                String message = String.format(
                        "Lớp học %s của bạn đã được duyệt và sẽ bắt đầu vào ngày %s. Lịch học: %s",
                        classEntity.getCode(),
                        classEntity.getStartDate() != null ? classEntity.getStartDate().toString() : "sớm",
                        buildScheduleDisplay(classEntity)
                );

                notificationService.sendBulkNotificationsWithReference(
                        studentUserIds,
                        NotificationType.CLASS_REMINDER,
                        title,
                        message,
                        "Class",
                        classEntity.getId()
                );

                log.info("Đã gửi notification cho {} students về class approval {}",
                        enrolledStudents.size(), classEntity.getId());
            } else {
                log.info("Không có enrolled students nào cho class {} để gửi notification", classEntity.getId());
            }
        } catch (Exception e) {
            log.error("Lỗi khi gửi notification cho students về class approval {}: {}",
                    classEntity.getId(), e.getMessage(), e);
        }
    }

    /**
     * Xây dựng chuỗi hiển thị lịch học từ class entity
     */
    private String buildScheduleDisplay(ClassEntity classEntity) {
        if (classEntity.getScheduleDays() == null || classEntity.getScheduleDays().length == 0) {
            return "Chưa có lịch cụ thể";
        }

        // Convert schedule days to day names
        String[] dayNames = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};
        StringBuilder schedule = new StringBuilder();

        for (int i = 0; i < classEntity.getScheduleDays().length; i++) {
            if (i > 0) schedule.append(", ");
            int dayIndex = classEntity.getScheduleDays()[i];
            if (dayIndex >= 0 && dayIndex < dayNames.length) {
                schedule.append(dayNames[dayIndex]);
            }
        }

        return schedule.toString();
    }
}
