# Subject Module Migration - Copy Paste Guide (Chính xác từ Backend Deprecated)

> **📌 MỤC ĐÍCH**: Copy-paste chính xác code từ backend deprecated, chỉ đổi tên Course → Subject
> 
> **Quy tắc đổi tên**:
> - `Course` → `Subject`
> - `course` → `subject`  
> - `Subject` (deprecated - Curriculum cũ) → `Curriculum`
> - `CoursePhase` → `SubjectPhase`
> - `CourseSession` → `SubjectSession`
> - `CourseMaterial` → `SubjectMaterial`
> - `CourseAssessment` → `SubjectAssessment`
> - `CourseStatus` → `SubjectStatus`
> - "khóa học" → "môn học"
> - "Môn học" (deprecated context) → "Khung chương trình"

---

## 1. SUBJECTREPOSITORY - COPY CHÍNH XÁC TỪ CourseRepository

```java
// File: src/main/java/org/fyp/tmssep490be/repositories/SubjectRepository.java
package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Subject;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.SubjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    
    // Find by Curriculum (deprecated: Subject)
    List<Subject> findByCurriculumId(Long curriculumId);
    List<Subject> findByLevelId(Long levelId);
    List<Subject> findByCurriculumIdAndLevelId(Long curriculumId, Long levelId);
    
    // Ordered by updatedAt DESC
    List<Subject> findByCurriculumIdOrderByUpdatedAtDesc(Long curriculumId);
    List<Subject> findByLevelIdOrderByUpdatedAtDesc(Long levelId);
    List<Subject> findByCurriculumIdAndLevelIdOrderByUpdatedAtDesc(Long curriculumId, Long levelId);
    
    long countByLevelId(Long levelId);

    // ==================== SCHEDULER JOB METHODS ====================

    /**
     * Tìm môn học sẵn sàng kích hoạt vào ngày hiệu lực
     * Dùng bởi SubjectActivationJob để kích hoạt môn học APPROVED khi đến effectiveDate
     */
    @Query("SELECT s FROM Subject s " +
            "WHERE s.effectiveDate <= :date " +
            "AND s.status = :status " +
            "AND s.approvalStatus = :approvalStatus " +
            "ORDER BY s.effectiveDate ASC")
    List<Subject> findByEffectiveDateBeforeOrEqualAndStatusAndApprovalStatus(
            @Param("date") LocalDate date,
            @Param("status") SubjectStatus status,
            @Param("approvalStatus") ApprovalStatus approvalStatus);

    /**
     * Đếm số môn học có lớp trong các chi nhánh
     */
    @Query("SELECT COUNT(DISTINCT s) FROM Subject s " +
            "INNER JOIN s.classes cl " +
            "WHERE cl.branch.id IN :branchIds")
    long countDistinctByClassesInBranches(@Param("branchIds") List<Long> branchIds);

    /**
     * Kiểm tra curriculum có môn học với status cụ thể
     * Dùng để auto-activate curriculum khi môn học được approve
     */
    boolean existsByCurriculumIdAndStatus(Long curriculumId, SubjectStatus status);

    /**
     * Kiểm tra level có môn học với status cụ thể  
     * Dùng để auto-activate level khi môn học được approve
     */
    boolean existsByLevelIdAndStatus(Long levelId, SubjectStatus status);

    /**
     * Tìm version lớn nhất cho logical subject code
     * Dùng khi clone môn học để xác định version tiếp theo
     */
    @Query("SELECT MAX(s.version) FROM Subject s WHERE s.logicalSubjectCode = :logicalSubjectCode")
    Integer findMaxVersionByLogicalSubjectCode(@Param("logicalSubjectCode") String logicalSubjectCode);

    /**
     * Tìm tất cả môn học cùng logical code (tất cả versions)
     */
    List<Subject> findByLogicalSubjectCodeOrderByVersionDesc(String logicalSubjectCode);

    /**
     * Kiểm tra code đã tồn tại
     */
    boolean existsByCode(String code);
}
```

---

## 2. SUBJECTCONTROLLER - COPY CHÍNH XÁC TỪ CourseController

```java
// File: src/main/java/org/fyp/tmssep490be/controllers/SubjectController.java
package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.SubjectDTO;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.subject.*;
import org.fyp.tmssep490be.repositories.StudentRepository;
import org.fyp.tmssep490be.common.security.UserPrincipal;
import org.fyp.tmssep490be.services.SubjectService;
import org.fyp.tmssep490be.services.MaterialAccessService;
import org.fyp.tmssep490be.services.StudentProgressService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller cho quản lý môn học
 */
@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Subject Management", description = "Subject APIs for management and student information")
@SecurityRequirement(name = "bearerAuth")
public class SubjectController {

    private final SubjectService subjectService;
    private final MaterialAccessService materialAccessService;
    private final StudentProgressService studentProgressService;
    private final StudentRepository studentRepository;

    // ==================== LIST & GET ====================

    @GetMapping
    @Operation(summary = "Get all subjects", description = "Lấy danh sách môn học cho dropdown")
    public ResponseEntity<ResponseObject<List<SubjectDTO>>> getAllSubjects(
            @RequestParam(required = false) Long curriculumId,
            @RequestParam(required = false) Long levelId) {
        log.info("Getting all subjects with filters - curriculumId: {}, levelId: {}", curriculumId, levelId);
        List<SubjectDTO> subjects = subjectService.getAllSubjects(curriculumId, levelId);
        return ResponseEntity.ok(ResponseObject.<List<SubjectDTO>>builder()
                .success(true)
                .message("Subjects retrieved successfully")
                .data(subjects)
                .build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get subject details (Admin/Manager view)")
    @PreAuthorize("hasRole('SUBJECT_LEADER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ResponseObject<SubjectDetailDTO>> getSubjectDetails(@PathVariable Long id) {
        log.info("Getting subject details for id: {}", id);
        SubjectDetailDTO subject = subjectService.getSubjectDetails(id);
        return ResponseEntity.ok(ResponseObject.<SubjectDetailDTO>builder()
                .success(true)
                .message("Subject details retrieved successfully")
                .data(subject)
                .build());
    }

    @GetMapping("/{subjectId}/detail")
    @Operation(summary = "Get subject detail (Student/Teacher view)")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ROLE_ACADEMIC_AFFAIR') or hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<SubjectDetailDTO>> getSubjectDetail(
            @Parameter(description = "Subject ID") @PathVariable Long subjectId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Long currentUserId = currentUser != null ? currentUser.getId() : 1L;
        log.info("User {} requesting details for subject {}", currentUserId, subjectId);
        SubjectDetailDTO subjectDetail = subjectService.getSubjectDetail(subjectId);
        return ResponseEntity.ok(ResponseObject.<SubjectDetailDTO>builder()
                .success(true)
                .message("Subject details retrieved successfully")
                .data(subjectDetail)
                .build());
    }

    // ==================== CRUD ====================

    @PostMapping
    @Operation(summary = "Create a new subject (Syllabus)")
    @PreAuthorize("hasRole('SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<SubjectDetailDTO>> createSubject(
            @RequestBody CreateSubjectRequestDTO request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("Creating new subject: {}", request.getBasicInfo().getName());
        Long userId = currentUser != null ? currentUser.getId() : null;
        SubjectDetailDTO createdSubject = subjectService.createSubject(request, userId);
        return ResponseEntity.ok(ResponseObject.<SubjectDetailDTO>builder()
                .success(true)
                .message("Subject created successfully")
                .data(createdSubject)
                .build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update subject")
    @PreAuthorize("hasRole('SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<SubjectDetailDTO>> updateSubject(
            @PathVariable Long id,
            @RequestBody CreateSubjectRequestDTO request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("Updating subject with ID: {}", id);
        Long userId = currentUser != null ? currentUser.getId() : null;
        SubjectDetailDTO updatedSubject = subjectService.updateSubject(id, request, userId);
        return ResponseEntity.ok(ResponseObject.<SubjectDetailDTO>builder()
                .success(true)
                .message("Subject updated successfully")
                .data(updatedSubject)
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete subject")
    @PreAuthorize("hasRole('SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<Void>> deleteSubject(@PathVariable Long id) {
        log.info("Deleting subject with ID: {}", id);
        subjectService.deleteSubject(id);
        return ResponseEntity.ok(ResponseObject.<Void>builder()
                .success(true)
                .message("Subject deleted successfully")
                .build());
    }

    // ==================== APPROVAL WORKFLOW ====================

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit subject for approval")
    @PreAuthorize("hasRole('SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<Void>> submitSubject(@PathVariable Long id) {
        log.info("Submitting subject with ID: {}", id);
        subjectService.submitSubject(id);
        return ResponseEntity.ok(ResponseObject.<Void>builder()
                .success(true)
                .message("Subject submitted for approval successfully")
                .build());
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve subject")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ResponseObject<Void>> approveSubject(@PathVariable Long id) {
        log.info("Approving subject with ID: {}", id);
        subjectService.approveSubject(id);
        return ResponseEntity.ok(ResponseObject.<Void>builder()
                .success(true)
                .message("Subject approved successfully")
                .build());
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject subject")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ResponseObject<Void>> rejectSubject(
            @PathVariable Long id,
            @RequestBody(required = false) String reason) {
        log.info("Rejecting subject with ID: {}", id);
        subjectService.rejectSubject(id, reason);
        return ResponseEntity.ok(ResponseObject.<Void>builder()
                .success(true)
                .message("Subject rejected successfully")
                .build());
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate subject")
    @PreAuthorize("hasRole('SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<Void>> deactivateSubject(@PathVariable Long id) {
        log.info("Deactivating subject with ID: {}", id);
        subjectService.deactivateSubject(id);
        return ResponseEntity.ok(ResponseObject.<Void>builder()
                .success(true)
                .message("Subject deactivated successfully")
                .build());
    }

    @PatchMapping("/{id}/reactivate")
    @Operation(summary = "Reactivate subject")
    @PreAuthorize("hasRole('SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<Void>> reactivateSubject(@PathVariable Long id) {
        log.info("Reactivating subject with ID: {}", id);
        subjectService.reactivateSubject(id);
        return ResponseEntity.ok(ResponseObject.<Void>builder()
                .success(true)
                .message("Subject reactivated successfully")
                .build());
    }

    // ==================== SYLLABUS & MATERIALS ====================

    @GetMapping("/{subjectId}/syllabus")
    @Operation(summary = "Get subject syllabus")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ROLE_ACADEMIC_AFFAIR') or hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<SubjectDetailDTO>> getSubjectSyllabus(
            @Parameter(description = "Subject ID") @PathVariable Long subjectId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Long currentUserId = currentUser != null ? currentUser.getId() : 1L;
        log.info("User {} requesting syllabus for subject {}", currentUserId, subjectId);
        SubjectDetailDTO syllabus = subjectService.getSubjectSyllabus(subjectId);
        return ResponseEntity.ok(ResponseObject.<SubjectDetailDTO>builder()
                .success(true)
                .message("Subject syllabus retrieved successfully")
                .data(syllabus)
                .build());
    }

    @GetMapping("/{subjectId}/materials")
    @Operation(summary = "Get subject materials hierarchy")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ROLE_ACADEMIC_AFFAIR') or hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<MaterialHierarchyDTO>> getSubjectMaterials(
            @Parameter(description = "Subject ID") @PathVariable Long subjectId,
            @Parameter(description = "Student ID for access control") @RequestParam(required = false) Long studentId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Long currentUserId = currentUser != null ? currentUser.getId() : 1L;
        boolean isStudent = currentUser != null &&
                currentUser.getAuthorities().stream()
                        .anyMatch(auth -> auth.getAuthority().equals("ROLE_STUDENT"));

        if (isStudent && (studentId == null || !currentUserId.equals(studentId))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseObject.<MaterialHierarchyDTO>builder()
                            .success(false)
                            .message("Student ID required for material access")
                            .build());
        }

        log.info("User {} requesting materials for subject {}", currentUserId, subjectId);
        MaterialHierarchyDTO materials = subjectService.getSubjectMaterials(subjectId,
                isStudent ? studentId : null);
        return ResponseEntity.ok(ResponseObject.<MaterialHierarchyDTO>builder()
                .success(true)
                .message("Subject materials retrieved successfully")
                .data(materials)
                .build());
    }

    // ==================== PLO & CLO ====================

    @GetMapping("/{subjectId}/plos")
    @Operation(summary = "Get subject PLOs")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ROLE_ACADEMIC_AFFAIR') or hasRole('TEACHER') or hasRole('SUBJECT_LEADER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ResponseObject<List<SubjectPLODTO>>> getSubjectPLOs(
            @Parameter(description = "Subject ID") @PathVariable Long subjectId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Long currentUserId = currentUser != null ? currentUser.getId() : 1L;
        log.info("User {} requesting PLOs for subject {}", currentUserId, subjectId);
        List<SubjectPLODTO> plos = subjectService.getSubjectPLOs(subjectId);
        return ResponseEntity.ok(ResponseObject.<List<SubjectPLODTO>>builder()
                .success(true)
                .message("Subject PLOs retrieved successfully")
                .data(plos)
                .build());
    }

    @GetMapping("/{subjectId}/clos")
    @Operation(summary = "Get subject CLOs")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ROLE_ACADEMIC_AFFAIR') or hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<List<SubjectCLODTO>>> getSubjectCLOs(
            @Parameter(description = "Subject ID") @PathVariable Long subjectId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Long currentUserId = currentUser != null ? currentUser.getId() : 1L;
        log.info("User {} requesting CLOs for subject {}", currentUserId, subjectId);
        List<SubjectCLODTO> clos = subjectService.getSubjectCLOs(subjectId);
        return ResponseEntity.ok(ResponseObject.<List<SubjectCLODTO>>builder()
                .success(true)
                .message("Subject CLOs retrieved successfully")
                .data(clos)
                .build());
    }

    // ==================== CLONE & VERSIONING ====================

    @PostMapping("/{id}/clone")
    @Operation(summary = "Clone subject", description = "Tạo version mới từ môn học hiện có")
    @PreAuthorize("hasRole('SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<SubjectDTO>> cloneSubject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} cloning subject with ID: {}", currentUser.getId(), id);
        SubjectDTO clonedSubject = subjectService.cloneSubject(id, currentUser.getId());
        return ResponseEntity.ok(ResponseObject.<SubjectDTO>builder()
                .success(true)
                .message("Subject cloned successfully. New version created.")
                .data(clonedSubject)
                .build());
    }

    @GetMapping("/next-version")
    @Operation(summary = "Get next version number")
    @PreAuthorize("hasRole('SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<Integer>> getNextVersion(
            @RequestParam String curriculumCode,
            @RequestParam String levelCode,
            @RequestParam Integer year) {
        log.info("Getting next version for {}-{}-{}", curriculumCode, levelCode, year);
        Integer nextVersion = subjectService.getNextVersionNumber(curriculumCode, levelCode, year);
        return ResponseEntity.ok(ResponseObject.<Integer>builder()
                .success(true)
                .message("Next version number retrieved")
                .data(nextVersion)
                .build());
    }
}
```

---

## 3. SUBJECTSERVICE - APPROVAL WORKFLOW (COPY CHÍNH XÁC)

```java
// Thêm vào SubjectService.java - METHODS APPROVAL WORKFLOW

// ==================== SUBMIT ====================

@Override
@Transactional
public void submitSubject(Long id) {
    log.info("Submitting subject for approval: {}", id);
    Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));

    // Chỉ cho phép submit khi status là DRAFT hoặc REJECTED
    if (subject.getStatus() != SubjectStatus.DRAFT && subject.getStatus() != SubjectStatus.REJECTED) {
        throw new IllegalStateException("Chỉ có thể gửi môn học ở trạng thái NHÁP hoặc BỊ TỪ CHỐI");
    }

    // Set updatedAt khi re-submit sau rejection
    if (subject.getApprovalStatus() == ApprovalStatus.REJECTED) {
        subject.setUpdatedAt(OffsetDateTime.now());
        log.info("Subject {} is being re-submitted after rejection", id);
    }

    subject.setStatus(SubjectStatus.SUBMITTED);
    subject.setApprovalStatus(ApprovalStatus.PENDING);
    subject.setSubmittedAt(OffsetDateTime.now());
    subjectRepository.save(subject);

    // Gửi thông báo cho Managers
    sendNotificationToManagers(subject);

    log.info("Subject {} submitted successfully", id);
}

// ==================== APPROVE ====================

@Override
@Transactional
public void approveSubject(Long id) {
    log.info("Approving subject with ID: {}", id);
    Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));

    if (subject.getStatus() != SubjectStatus.SUBMITTED) {
        throw new IllegalStateException("Chỉ có thể phê duyệt môn học ở trạng thái ĐÃ GỬI");
    }

    // Chuyển sang PENDING_ACTIVATION (chờ đến ngày hiệu lực)
    // Sẽ được kích hoạt bởi SubjectActivationJob khi đến effectiveDate
    subject.setStatus(SubjectStatus.PENDING_ACTIVATION);
    subject.setApprovalStatus(ApprovalStatus.APPROVED);
    subjectRepository.save(subject);

    // Gửi thông báo cho Subject Leader (người tạo)
    sendApprovalNotificationToSubjectLeader(subject, true, null);

    // Note: Curriculum/Level sẽ tự động kích hoạt khi Subject ACTIVE (via SubjectActivationJob)

    log.info("Subject {} approved successfully. Will be activated on effective date: {}",
            id, subject.getEffectiveDate());
}

// ==================== REJECT ====================

@Override
@Transactional
public void rejectSubject(Long id, String reason) {
    log.info("Rejecting subject with ID: {}", id);
    Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));

    if (subject.getStatus() != SubjectStatus.SUBMITTED) {
        throw new IllegalStateException("Chỉ có thể từ chối môn học ở trạng thái ĐÃ GỬI");
    }

    subject.setStatus(SubjectStatus.DRAFT);
    subject.setApprovalStatus(ApprovalStatus.REJECTED);
    subject.setRejectionReason(reason);

    subjectRepository.save(subject);

    // Gửi thông báo cho Subject Leader với lý do từ chối
    sendApprovalNotificationToSubjectLeader(subject, false, reason);

    log.info("Subject {} rejected. Reason: {}", id, reason);
}

// ==================== DEACTIVATE ====================

@Override
@Transactional
public void deactivateSubject(Long id) {
    log.info("Deactivating subject with ID: {}", id);
    Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));

    if (subject.getStatus() != SubjectStatus.ACTIVE) {
        throw new IllegalStateException("Chỉ có thể hủy kích hoạt môn học đang ở trạng thái HOẠT ĐỘNG");
    }

    // Kiểm tra có lớp học nào đang sử dụng môn học này còn session tương lai
    boolean hasFutureSessions = subject.getClasses().stream()
            .flatMap(classEntity -> classEntity.getSessions().stream())
            .anyMatch(session -> !session.getDate().isBefore(java.time.LocalDate.now()));

    if (hasFutureSessions) {
        throw new IllegalStateException("Không thể hủy kích hoạt vì còn lớp học đang giảng dạy môn học này");
    }

    subject.setStatus(SubjectStatus.INACTIVE);
    subjectRepository.save(subject);

    // Chuyển Curriculum/Level về DRAFT nếu không còn môn học ACTIVE
    deactivateCurriculumAndLevelIfNeeded(subject);
}

// ==================== REACTIVATE ====================

@Override
@Transactional
public void reactivateSubject(Long id) {
    log.info("Reactivating subject with ID: {}", id);
    Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));

    // Kiểm tra môn học có đủ dữ liệu để active
    if (subject.getClos() != null && !subject.getClos().isEmpty() &&
            subject.getSubjectPhases() != null && !subject.getSubjectPhases().isEmpty()) {
        subject.setStatus(SubjectStatus.ACTIVE);
    } else {
        subject.setStatus(SubjectStatus.DRAFT);
    }
    subjectRepository.save(subject);
}

// ==================== DELETE ====================

@Override
@Transactional
public void deleteSubject(Long id) {
    log.info("Deleting subject with ID: {}", id);
    Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học với ID: " + id));

    // Validation: Chỉ xóa được khi status = DRAFT và approvalStatus = null
    if (subject.getStatus() != SubjectStatus.DRAFT || subject.getApprovalStatus() != null) {
        throw new IllegalStateException(
                "Không thể xóa môn học. Môn học phải ở trạng thái NHÁP và chưa được gửi phê duyệt.");
    }

    subjectRepository.delete(subject);
    log.info("Subject deleted successfully: {}", id);
}
```

---

## 4. NOTIFICATION HELPER METHODS (COPY CHÍNH XÁC)

```java
// Thêm vào SubjectService.java - NOTIFICATION METHODS

/**
 * Gửi thông báo cho tất cả Managers khi môn học được submit
 */
private void sendNotificationToManagers(Subject subject) {
    try {
        // Tìm tất cả users có role MANAGER (không phân biệt branch vì môn học là toàn hệ thống)
        List<UserAccount> managers = userAccountRepository.findUsersByRole("MANAGER");

        if (!managers.isEmpty()) {
            String title = "Môn học mới cần phê duyệt";
            String message = String.format(
                    "Môn học \"%s\" (Mã: %s) đã được gửi để phê duyệt. Khung chương trình: %s, Cấp độ: %s.",
                    subject.getName(),
                    subject.getCode(),
                    subject.getCurriculum() != null ? subject.getCurriculum().getName() : "N/A",
                    subject.getLevel() != null ? subject.getLevel().getName() : "N/A");

            List<Long> recipientIds = managers.stream()
                    .map(UserAccount::getId)
                    .collect(Collectors.toList());

            notificationService.sendBulkNotificationsWithReference(
                    recipientIds,
                    NotificationType.REQUEST_APPROVAL,
                    title,
                    message,
                    "Subject",
                    subject.getId());

            log.info("Sent notification to {} managers about subject {} submission",
                    managers.size(), subject.getId());
        } else {
            log.warn("No managers found to notify about subject {} submission", subject.getId());
        }
    } catch (Exception e) {
        log.error("Error sending notification to managers for subject {}: {}",
                subject.getId(), e.getMessage(), e);
        // Không throw exception - lỗi notification không block submission
    }
}

/**
 * Gửi thông báo cho Subject Leader khi môn học được approve hoặc reject
 */
private void sendApprovalNotificationToSubjectLeader(Subject subject, boolean isApproved, String rejectionReason) {
    try {
        UserAccount subjectLeader = subject.getCreatedBy();
        if (subjectLeader == null) {
            log.warn("No creator found for subject {} - cannot send approval notification", subject.getId());
            return;
        }

        String title;
        String message;
        NotificationType notificationType;

        if (isApproved) {
            title = "Môn học đã được phê duyệt";
            message = String.format(
                    "Môn học \"%s\" (Mã: %s) đã được phê duyệt và sẽ kích hoạt vào ngày %s.",
                    subject.getName(),
                    subject.getCode(),
                    subject.getEffectiveDate() != null ? subject.getEffectiveDate().toString() : "N/A");
            notificationType = NotificationType.REQUEST_APPROVAL;
        } else {
            title = "Môn học bị từ chối";
            message = String.format(
                    "Môn học \"%s\" (Mã: %s) đã bị từ chối. Lý do: %s",
                    subject.getName(),
                    subject.getCode(),
                    rejectionReason != null ? rejectionReason : "Không có lý do cụ thể");
            notificationType = NotificationType.REQUEST_APPROVAL;
        }

        notificationService.sendBulkNotificationsWithReference(
                List.of(subjectLeader.getId()),
                notificationType,
                title,
                message,
                "Subject",
                subject.getId());

        log.info("Sent {} notification to Subject Leader (user {}) for subject {}",
                isApproved ? "approval" : "rejection", subjectLeader.getId(), subject.getId());
    } catch (Exception e) {
        log.error("Error sending approval notification for subject {}: {}",
                subject.getId(), e.getMessage(), e);
        // Không throw exception - lỗi notification không block approval/rejection
    }
}
```

---

## 5. ACTIVATE/DEACTIVATE CURRICULUM & LEVEL (COPY CHÍNH XÁC)

```java
// Thêm vào SubjectService.java

/**
 * Tự động kích hoạt Curriculum và Level khi Subject trở thành ACTIVE.
 * Được gọi bởi SubjectActivationJob khi môn học đến ngày hiệu lực.
 */
public void activateCurriculumAndLevelIfNeeded(Subject subject) {
    // Kích hoạt Curriculum nếu đang ở trạng thái DRAFT
    Curriculum curriculum = subject.getCurriculum();
    if (curriculum != null && curriculum.getStatus() == CurriculumStatus.DRAFT) {
        curriculum.setStatus(CurriculumStatus.ACTIVE);
        curriculumRepository.save(curriculum);
        log.info("Curriculum {} auto-activated due to subject activation", curriculum.getId());
    }

    // Kích hoạt Level nếu đang ở trạng thái DRAFT
    Level level = subject.getLevel();
    if (level != null && level.getStatus() == LevelStatus.DRAFT) {
        level.setStatus(LevelStatus.ACTIVE);
        levelRepository.save(level);
        log.info("Level {} auto-activated due to subject activation", level.getId());
    }
}

/**
 * Chuyển Curriculum và Level về DRAFT khi môn học ACTIVE cuối cùng bị deactivate.
 */
private void deactivateCurriculumAndLevelIfNeeded(Subject subject) {
    // Kiểm tra Curriculum - chuyển về DRAFT nếu không còn môn học ACTIVE
    Curriculum curriculum = subject.getCurriculum();
    if (curriculum != null && curriculum.getStatus() == CurriculumStatus.ACTIVE) {
        boolean hasOtherActiveSubject = subjectRepository.existsByCurriculumIdAndStatus(
                curriculum.getId(), SubjectStatus.ACTIVE);
        if (!hasOtherActiveSubject) {
            curriculum.setStatus(CurriculumStatus.DRAFT);
            curriculumRepository.save(curriculum);
            log.info("Curriculum {} reverted to DRAFT - no more active subjects", curriculum.getId());
        }
    }

    // Kiểm tra Level - chuyển về DRAFT nếu không còn môn học ACTIVE
    Level level = subject.getLevel();
    if (level != null && level.getStatus() == LevelStatus.ACTIVE) {
        boolean hasOtherActiveSubject = subjectRepository.existsByLevelIdAndStatus(
                level.getId(), SubjectStatus.ACTIVE);
        if (!hasOtherActiveSubject) {
            level.setStatus(LevelStatus.DRAFT);
            levelRepository.save(level);
            log.info("Level {} reverted to DRAFT - no more active subjects", level.getId());
        }
    }
}
```

---

## 6. CLONE & VERSIONING (COPY CHÍNH XÁC)

```java
// Thêm vào SubjectService.java

@Override
public Integer getNextVersionNumber(String curriculumCode, String levelCode, Integer year) {
    String logicalSubjectCode = String.format("%s-%s-%d", curriculumCode, levelCode, year);
    Integer maxVersion = subjectRepository.findMaxVersionByLogicalSubjectCode(logicalSubjectCode);
    return (maxVersion == null) ? 1 : maxVersion + 1;
}

@Override
@Transactional
public SubjectDTO cloneSubject(Long id, Long userId) {
    log.info("Cloning subject with ID: {} by user: {}", id, userId);

    Subject originalSubject = subjectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học với ID: " + id));

    // Lấy curriculum và level codes
    String curriculumCode = originalSubject.getCurriculum().getCode();
    String levelCode = originalSubject.getLevel().getCode();
    int year = originalSubject.getEffectiveDate() != null
            ? originalSubject.getEffectiveDate().getYear()
            : java.time.LocalDate.now().getYear();

    // Tính logical subject code và next version
    String logicalSubjectCode = String.format("%s-%s-%d", curriculumCode, levelCode, year);
    Integer nextVersion = getNextVersionNumber(curriculumCode, levelCode, year);
    String newCode = String.format("%s-V%d", logicalSubjectCode, nextVersion);

    // Kiểm tra code đã tồn tại
    while (subjectRepository.existsByCode(newCode)) {
        nextVersion++;
        newCode = String.format("%s-V%d", logicalSubjectCode, nextVersion);
    }

    // Tạo môn học mới
    Subject newSubject = new Subject();
    newSubject.setCurriculum(originalSubject.getCurriculum());
    newSubject.setLevel(originalSubject.getLevel());
    newSubject.setLogicalSubjectCode(logicalSubjectCode);
    newSubject.setVersion(nextVersion);
    newSubject.setCode(newCode);
    newSubject.setName(originalSubject.getName() + " (V" + nextVersion + ")");
    newSubject.setDescription(originalSubject.getDescription());
    newSubject.setScoreScale(originalSubject.getScoreScale());
    newSubject.setTotalHours(originalSubject.getTotalHours());
    newSubject.setNumberOfSessions(originalSubject.getNumberOfSessions());
    newSubject.setHoursPerSession(originalSubject.getHoursPerSession());
    newSubject.setPrerequisites(originalSubject.getPrerequisites());
    newSubject.setTargetAudience(originalSubject.getTargetAudience());
    newSubject.setTeachingMethods(originalSubject.getTeachingMethods());
    newSubject.setEffectiveDate(originalSubject.getEffectiveDate());
    newSubject.setStatus(SubjectStatus.DRAFT);
    newSubject.setApprovalStatus(null);

    // Set created by
    UserAccount creator = userAccountRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    newSubject.setCreatedBy(creator);

    // Save môn học mới trước
    newSubject = subjectRepository.save(newSubject);
    final Subject savedNewSubject = newSubject;

    // Clone CLOs
    Map<Long, CLO> oldToNewCloMap = new java.util.HashMap<>();
    for (CLO oldClo : originalSubject.getClos()) {
        CLO newClo = new CLO();
        newClo.setSubject(savedNewSubject);
        newClo.setCode(oldClo.getCode());
        newClo.setDescription(oldClo.getDescription());
        cloRepository.save(newClo);
        oldToNewCloMap.put(oldClo.getId(), newClo);

        // Clone PLO-CLO mappings
        List<PLOCLOMapping> oldMappings = ploCloMappingRepository.findByCloId(oldClo.getId());
        for (PLOCLOMapping oldMapping : oldMappings) {
            PLOCLOMapping newMapping = new PLOCLOMapping();
            newMapping.setClo(newClo);
            newMapping.setPlo(oldMapping.getPlo());
            ploCloMappingRepository.save(newMapping);
        }
    }

    // Clone Subject Phases và Sessions
    for (SubjectPhase oldPhase : originalSubject.getSubjectPhases()) {
        SubjectPhase newPhase = new SubjectPhase();
        newPhase.setSubject(savedNewSubject);
        newPhase.setName(oldPhase.getName());
        newPhase.setDescription(oldPhase.getDescription());
        newPhase.setPhaseNumber(oldPhase.getPhaseNumber());
        subjectPhaseRepository.save(newPhase);

        // Clone Sessions
        for (SubjectSession oldSession : oldPhase.getSubjectSessions()) {
            SubjectSession newSession = new SubjectSession();
            newSession.setPhase(newPhase);
            newSession.setTopic(oldSession.getTopic());
            newSession.setStudentTask(oldSession.getStudentTask());
            newSession.setSkills(
                    oldSession.getSkills() != null ? new ArrayList<>(oldSession.getSkills()) : new ArrayList<>());
            newSession.setSequenceNo(oldSession.getSequenceNo());
            subjectSessionRepository.save(newSession);

            // Clone Session-CLO mappings
            List<SubjectSessionCLOMapping> oldSessionMappings = subjectSessionCLOMappingRepository
                    .findBySubjectSessionId(oldSession.getId());
            for (SubjectSessionCLOMapping oldMapping : oldSessionMappings) {
                CLO newClo = oldToNewCloMap.get(oldMapping.getClo().getId());
                if (newClo != null) {
                    SubjectSessionCLOMapping newMapping = new SubjectSessionCLOMapping();
                    newMapping.setSubjectSession(newSession);
                    newMapping.setClo(newClo);
                    subjectSessionCLOMappingRepository.save(newMapping);
                }
            }
        }
    }

    // Clone Subject Assessments
    for (SubjectAssessment oldAssessment : originalSubject.getSubjectAssessments()) {
        SubjectAssessment newAssessment = new SubjectAssessment();
        newAssessment.setSubject(savedNewSubject);
        newAssessment.setName(oldAssessment.getName());
        newAssessment.setDescription(oldAssessment.getDescription());
        newAssessment.setKind(oldAssessment.getKind());
        newAssessment.setMaxScore(oldAssessment.getMaxScore());
        newAssessment.setDurationMinutes(oldAssessment.getDurationMinutes());
        newAssessment.setSkills(new ArrayList<>(oldAssessment.getSkills()));
        newAssessment.setNote(oldAssessment.getNote());
        subjectAssessmentRepository.save(newAssessment);

        // Clone Assessment-CLO mappings
        List<SubjectAssessmentCLOMapping> oldAssessmentMappings = subjectAssessmentCLOMappingRepository
                .findBySubjectAssessmentId(oldAssessment.getId());
        for (SubjectAssessmentCLOMapping oldMapping : oldAssessmentMappings) {
            CLO newClo = oldToNewCloMap.get(oldMapping.getClo().getId());
            if (newClo != null) {
                SubjectAssessmentCLOMapping newMapping = new SubjectAssessmentCLOMapping();
                newMapping.setSubjectAssessment(newAssessment);
                newMapping.setClo(newClo);
                subjectAssessmentCLOMappingRepository.save(newMapping);
            }
        }
    }

    // Clone Subject Materials
    for (SubjectMaterial oldMaterial : originalSubject.getSubjectMaterials()) {
        SubjectMaterial newMaterial = new SubjectMaterial();
        newMaterial.setSubject(savedNewSubject);
        newMaterial.setTitle(oldMaterial.getTitle());
        newMaterial.setDescription(oldMaterial.getDescription());
        newMaterial.setMaterialType(oldMaterial.getMaterialType());
        newMaterial.setUrl(oldMaterial.getUrl());
        subjectMaterialRepository.save(newMaterial);
    }

    log.info("Successfully cloned subject {} to new subject {} with version {}", id, savedNewSubject.getId(),
            nextVersion);

    return SubjectDTO.builder()
            .id(savedNewSubject.getId())
            .name(savedNewSubject.getName())
            .code(savedNewSubject.getCode())
            .status(savedNewSubject.getStatus().name())
            .build();
}
```

---

## 7. SUBJECTACTIVATIONJOB (COPY CHÍNH XÁC TỪ CourseActivationJob)

```java
// File: src/main/java/org/fyp/tmssep490be/services/scheduler/SubjectActivationJob.java
package org.fyp.tmssep490be.services.scheduler;

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
 * Scheduled job tự động kích hoạt môn học vào ngày hiệu lực.
 *
 * Chức năng:
 * - Kích hoạt môn học khi đến effectiveDate
 * - Chỉ kích hoạt môn học có:
 *   1. status = PENDING_ACTIVATION
 *   2. approvalStatus = APPROVED
 *   3. effectiveDate = HÔM NAY hoặc đã qua
 * - Cập nhật status từ PENDING_ACTIVATION sang ACTIVE
 * - Tự động kích hoạt Curriculum và Level khi môn học ACTIVE
 *
 * Chạy hàng ngày lúc 1:00 AM (configurable via application.yml)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    prefix = "tms.scheduler.jobs.subject-activation",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class SubjectActivationJob extends BaseScheduledJob {

    private final SubjectRepository subjectRepository;
    private final SubjectService subjectService;

    /**
     * Kích hoạt môn học vào ngày hiệu lực.
     * Chạy hàng ngày lúc 1:00 AM.
     */
    @Scheduled(cron = "${tms.scheduler.jobs.subject-activation.cron:0 0 1 * * ?}")
    @Transactional
    public void activateSubjects() {
        try {
            logJobStart("SubjectActivation");

            LocalDate today = LocalDate.now();
            logJobInfo(String.format("Checking subjects with effectiveDate <= %s, status = PENDING_ACTIVATION, approvalStatus = APPROVED",
                today));

            List<Subject> subjectsToActivate = subjectRepository
                .findByEffectiveDateBeforeOrEqualAndStatusAndApprovalStatus(
                    today,
                    SubjectStatus.PENDING_ACTIVATION,
                    ApprovalStatus.APPROVED
                );

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
                
                // Tự động kích hoạt Curriculum và Level khi môn học ACTIVE
                subjectService.activateCurriculumAndLevelIfNeeded(subject);
                
                activatedCount++;
            }

            logJobEnd("SubjectActivation", activatedCount);

        } catch (Exception e) {
            logJobError("SubjectActivation", e);
            throw e;
        }
    }
}
```

---

## 📋 CHECKLIST MIGRATION

- [ ] Copy SubjectRepository với @Query annotations chính xác
- [ ] Copy SubjectController với @PreAuthorize permissions chính xác
- [ ] Copy SubjectService approval workflow methods
- [ ] Copy notification helper methods
- [ ] Copy activate/deactivate Curriculum & Level methods
- [ ] Copy clone và versioning methods
- [ ] Copy SubjectActivationJob scheduler
- [ ] Thêm config scheduler trong application.yml
- [ ] **Tạo các DTOs còn thiếu (xem Section 8)**
- [ ] Kiểm tra tất cả imports
- [ ] Test các endpoints qua Swagger

---

## 8. DTOs CÒN THIẾU (CẦN TẠO MỚI)

### 8.1 SubjectPLODTO.java

```java
// File: src/main/java/org/fyp/tmssep490be/dtos/subject/SubjectPLODTO.java
package org.fyp.tmssep490be.dtos.subject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectPLODTO {
    private Long id;
    private String code;
    private String description;
    private String programName;
}
```

### 8.2 SubjectProgressDTO.java

```java
// File: src/main/java/org/fyp/tmssep490be/dtos/subject/SubjectProgressDTO.java
package org.fyp.tmssep490be.dtos.subject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectProgressDTO {
    private Long subjectId;
    private Long studentId;
    private Integer totalSessions;
    private Integer completedSessions;
    private Integer totalMaterials;
    private Integer accessibleMaterials;
    private Double progressPercentage;
    private Double attendanceRate;
    private List<CLOProgressDTO> cloProgress;
    private List<AssessmentProgressDTO> assessmentProgress;
    private String currentPhase;
    private String nextSession;
    private Long estimatedCompletionDate;
}
```

### 8.3 AssessmentProgressDTO.java

```java
// File: src/main/java/org/fyp/tmssep490be/dtos/subject/AssessmentProgressDTO.java
package org.fyp.tmssep490be.dtos.subject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentProgressDTO {
    private Long assessmentId;
    private String name;
    private String assessmentType;
    private BigDecimal weight;
    private BigDecimal maxScore;
    private BigDecimal achievedScore;
    private Boolean isCompleted;
    private String completedAt;
    private Double percentageScore;
}
```

### 8.4 CLOProgressDTO.java

```java
// File: src/main/java/org/fyp/tmssep490be/dtos/subject/CLOProgressDTO.java
package org.fyp.tmssep490be.dtos.subject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CLOProgressDTO {
    private Long cloId;
    private String cloCode;
    private String description;
    private Double achievementRate;
    private Boolean isAchieved;
    private Integer totalAssessments;
    private Integer completedAssessments;
    private Double averageScore;
}
```

### 8.5 MaterialHierarchyDTO.java

```java
// File: src/main/java/org/fyp/tmssep490be/dtos/subject/MaterialHierarchyDTO.java
package org.fyp.tmssep490be.dtos.subject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialHierarchyDTO {
    private List<SubjectMaterialDTO> subjectLevel;
    private List<PhaseMaterialDTO> phases;
    private Integer totalMaterials;
    private Integer accessibleMaterials;
}
```

### 8.6 PhaseMaterialDTO.java

```java
// File: src/main/java/org/fyp/tmssep490be/dtos/subject/PhaseMaterialDTO.java
package org.fyp.tmssep490be.dtos.subject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhaseMaterialDTO {
    private Long id;
    private Integer phaseNumber;
    private String name;
    private List<SubjectMaterialDTO> materials;
    private List<SessionMaterialDTO> sessions;
    private Integer totalMaterials;
}
```

### 8.7 SessionMaterialDTO.java

```java
// File: src/main/java/org/fyp/tmssep490be/dtos/subject/SessionMaterialDTO.java
package org.fyp.tmssep490be.dtos.subject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionMaterialDTO {
    private Long id;
    private Integer sequenceNo;
    private String topic;
    private List<SubjectMaterialDTO> materials;
    private List<String> skills;
    private Integer totalMaterials;
}
```

### 8.8 StudentSubjectDTO.java

```java
// File: src/main/java/org/fyp/tmssep490be/dtos/subject/StudentSubjectDTO.java
package org.fyp.tmssep490be.dtos.subject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentSubjectDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String curriculumName;  // đổi từ subjectName
    private String levelName;
    private String logicalSubjectCode;  // đổi từ logicalCourseCode
    private Integer totalHours;

    private String targetAudience;
    private String teachingMethods;
    private LocalDate effectiveDate;
    private String status;
    private String approvalStatus;
    private Long classId;
    private String classCode;
    private String centerName;
    private String roomName;
    private String modality;
    private LocalDate classStartDate;
    private LocalDate classEndDate;
    private String teacherName;
    private String enrollmentStatus;
    private LocalDate enrolledAt;
    private Double progressPercentage;
    private Integer completedSessions;
    private Integer totalSessions;
    private String attendanceRate;
}
```

---

## 📊 TỔNG KẾT DTOs

| # | Deprecated (Course) | New (Subject) | Trạng thái |
|---|---------------------|---------------|------------|
| 1 | CourseDTO | SubjectDTO | ✅ Đã có |
| 2 | CreateCourseRequestDTO | CreateSubjectRequestDTO | ✅ Đã có |
| 3 | CourseDetailDTO | SubjectDetailDTO | ✅ Đã có |
| 4 | CourseBasicInfoDTO | SubjectBasicInfoDTO | ✅ Đã có |
| 5 | CourseCLODTO | SubjectCLODTO | ✅ Đã có |
| 6 | CourseStructureDTO | SubjectStructureDTO | ✅ Đã có |
| 7 | CoursePhaseDTO | SubjectPhaseDTO | ✅ Đã có |
| 8 | CourseSessionDTO | SubjectSessionDTO | ✅ Đã có |
| 9 | CourseMaterialDTO | SubjectMaterialDTO | ✅ Đã có |
| 10 | CourseAssessmentDTO | SubjectAssessmentDTO | ✅ Đã có |
| 11 | CoursePLODTO | **SubjectPLODTO** | 🆕 Section 8.1 |
| 12 | CourseProgressDTO | **SubjectProgressDTO** | 🆕 Section 8.2 |
| 13 | AssessmentProgressDTO | **AssessmentProgressDTO** | 🆕 Section 8.3 |
| 14 | CLOProgressDTO | **CLOProgressDTO** | 🆕 Section 8.4 |
| 15 | MaterialHierarchyDTO | **MaterialHierarchyDTO** | 🆕 Section 8.5 |
| 16 | PhaseMaterialDTO | **PhaseMaterialDTO** | 🆕 Section 8.6 |
| 17 | SessionMaterialDTO | **SessionMaterialDTO** | 🆕 Section 8.7 |
| 18 | StudentCourseDTO | **StudentSubjectDTO** | 🆕 Section 8.8 |
