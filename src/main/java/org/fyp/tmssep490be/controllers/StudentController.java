package org.fyp.tmssep490be.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.studentmanagement.*;
import org.fyp.tmssep490be.dtos.studentportal.StudentClassDTO;
import org.fyp.tmssep490be.entities.enums.Gender;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.StudentService;
import org.fyp.tmssep490be.services.StudentPortalService;
import org.fyp.tmssep490be.utils.StudentContextHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Slf4j
public class StudentController {

    private final StudentService studentService;
    private final StudentPortalService studentPortalService;
    private final StudentContextHelper studentContextHelper;
    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB


    @GetMapping
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<Page<StudentListItemDTO>>> getStudents(
            @RequestParam(required = false) List<Long> branchIds,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Gender gender,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "studentCode") String sort,
            @RequestParam(defaultValue = "asc") String sortDir,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requesting students list with filters: branchIds={}, search={}, status={}, gender={}",
                currentUser.getId(), branchIds, search, status, gender);

        // Create pageable with sort
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));

        Page<StudentListItemDTO> students = studentService.getStudents(
                branchIds, search, status, gender, pageable, currentUser.getId()
        );

        return ResponseEntity.ok(ResponseObject.<Page<StudentListItemDTO>>builder()
                .success(true)
                .message("Students retrieved successfully")
                .data(students)
                .build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<CreateStudentResponse>> createStudent(
            @Valid @RequestBody CreateStudentRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        // For testing purposes, we'll use a mock user ID when currentUser is null
        Long userId = currentUser != null ? currentUser.getId() : 1L;
        log.info("User {} creating new student with email: {}", userId, request.getEmail());

        CreateStudentResponse response = studentService.createStudent(request, userId);

        log.info("Successfully created student with code: {} by user: {}",
                response.getStudentCode(), userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseObject.<CreateStudentResponse>builder()
                        .success(true)
                        .message("Student created successfully")
                        .data(response)
                        .build());
    }
    
    @GetMapping("/check-existence")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<CheckStudentExistenceResponse>> checkStudentExistence(
            @RequestParam String type, // EMAIL or PHONE
            @RequestParam String value,
            @RequestParam Long currentBranchId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} checking student existence: type={}, value={}, branchId={}",
                currentUser.getId(), type, value, currentBranchId);

        CheckStudentExistenceResponse response = studentService.checkStudentExistence(
                type, value, currentBranchId, currentUser.getId()
        );

        return ResponseEntity.ok(ResponseObject.<CheckStudentExistenceResponse>builder()
                .success(true)
                .message("Check completed")
                .data(response)
                .build());
    }

    @PostMapping("/{studentId}/sync-to-branch")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<SyncToBranchResponse>> syncStudentToBranch(
            @PathVariable Long studentId,
            @Valid @RequestBody SyncToBranchRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} syncing student {} to branch {}",
                currentUser.getId(), studentId, request.getTargetBranchId());

        SyncToBranchResponse response = studentService.syncStudentToBranch(
                studentId, request, currentUser.getId()
        );

        log.info("Successfully synced student {} to branch {}", studentId, request.getTargetBranchId());

        return ResponseEntity.ok(ResponseObject.<SyncToBranchResponse>builder()
                .success(true)
                .message("Student synced to branch successfully")
                .data(response)
                .build());
    }

    @GetMapping("/{studentId}")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<StudentDetailDTO>> getStudentDetail(
            @PathVariable Long studentId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requesting details for student {}", currentUser.getId(), studentId);

        StudentDetailDTO studentDetail = studentService.getStudentDetail(studentId, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<StudentDetailDTO>builder()
                .success(true)
                .message("Student details retrieved successfully")
                .data(studentDetail)
                .build());
    }

    @PutMapping("/{studentId}")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<StudentDetailDTO>> updateStudent(
            @PathVariable Long studentId,
            @Valid @RequestBody UpdateStudentRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} updating student {} with email: {}", currentUser.getId(), studentId, request.getEmail());

        StudentDetailDTO updatedStudent = studentService.updateStudent(studentId, request, currentUser.getId());

        log.info("Successfully updated student {} by user {}", studentId, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<StudentDetailDTO>builder()
                .success(true)
                .message("Cập nhật thông tin học viên thành công")
                .data(updatedStudent)
                .build());
    }

    @DeleteMapping("/{studentId}/skill-assessments/{assessmentId}")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<Void>> deleteSkillAssessment(
            @PathVariable Long studentId,
            @PathVariable Long assessmentId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} deleting skill assessment {} for student {}", currentUser.getId(), assessmentId, studentId);

        studentService.deleteSkillAssessment(studentId, assessmentId, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<Void>builder()
                .success(true)
                .message("Xóa đánh giá kỹ năng thành công")
                .build());
    }

    // Template học viên của lớp khi đưa cho sale, sale lấy template và gửi lại giáo vụ những người đăng ký và add vào hệ thống
    @GetMapping("/import/template")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<org.springframework.core.io.Resource> downloadImportTemplate(
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requested student import template", currentUser.getId());

        byte[] templateData = studentService.generateStudentImportTemplate();
        org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(templateData);

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(XLSX_CONTENT_TYPE))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mau-nhap-hoc-vien.xlsx")
                .body(resource);
    }

    // Trước khi được add vào hệ thống, thi giáo vụ phải xem preview student đã có ở trong hệ thống hay chưa
    @PostMapping(value = "/import/preview", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<StudentImportPreview>> previewImport(
            @RequestParam Long branchId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} previewing student import for branch {}", currentUser.getId(), branchId);

        // Validate file is not empty
        if (file.isEmpty()) {
            throw new org.fyp.tmssep490be.exceptions.CustomException(org.fyp.tmssep490be.exceptions.ErrorCode.EXCEL_FILE_EMPTY);
        }

        // Validate file size (10MB max)
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new org.fyp.tmssep490be.exceptions.CustomException(org.fyp.tmssep490be.exceptions.ErrorCode.FILE_SIZE_EXCEEDED);
        }

        // Validate file extension
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new org.fyp.tmssep490be.exceptions.CustomException(org.fyp.tmssep490be.exceptions.ErrorCode.INVALID_FILE_TYPE_XLSX);
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals(XLSX_CONTENT_TYPE)) {
            throw new org.fyp.tmssep490be.exceptions.CustomException(org.fyp.tmssep490be.exceptions.ErrorCode.INVALID_FILE_TYPE_XLSX);
        }

        StudentImportPreview preview = studentService.previewStudentImport(branchId, file, currentUser.getId());

        log.info("Import preview completed for branch {}. Create: {}, Found: {}, Errors: {}",
                branchId, preview.getCreateCount(), preview.getFoundCount(), preview.getErrorCount());

        return ResponseEntity.ok(ResponseObject.<StudentImportPreview>builder()
                .success(true)
                .message("Import preview ready")
                .data(preview)
                .build());
    }

    @PostMapping("/import/execute")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<StudentImportResult>> executeImport(
            @Valid @RequestBody StudentImportExecuteRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("=== EXECUTE IMPORT STARTED ===");
        log.info("User {} executing student import for branch {} with {} students, selectedIndices: {}",
                currentUser.getId(), request.getBranchId(), request.getStudents().size(), request.getSelectedIndices());

        StudentImportResult result = studentService.executeStudentImport(request, currentUser.getId());

        log.info("Student import completed. Created: {}, Synced: {}, Failed: {}",
                result.getSuccessfulCreations(), result.getSyncedToBranch(), result.getFailedCreations());

        int totalProcessed = result.getSuccessfulCreations() + result.getSyncedToBranch();
        String message;
        if (result.getSuccessfulCreations() > 0 && result.getSyncedToBranch() > 0) {
            message = String.format("Đã nhập thành công %d học viên (%d tạo mới, %d đồng bộ)", 
                    totalProcessed, result.getSuccessfulCreations(), result.getSyncedToBranch());
        } else if (result.getSyncedToBranch() > 0) {
            message = String.format("Đã đồng bộ thành công %d học viên vào chi nhánh", result.getSyncedToBranch());
        } else {
            message = String.format("Đã tạo mới thành công %d học viên", result.getSuccessfulCreations());
        }

        return ResponseEntity.ok(ResponseObject.<StudentImportResult>builder()
                .success(true)
                .message(message)
                .data(result)
                .build());
    }

    // Student tự xem profile của mình
    @GetMapping("/me/profile")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<StudentDetailDTO>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("Student {} đang lấy thông tin profile", currentUser.getId());
        StudentDetailDTO profile = studentService.getStudentProfileByUserId(currentUser.getId());
        return ResponseEntity.ok(ResponseObject.<StudentDetailDTO>builder()
                .success(true)
                .message("Lấy thông tin profile thành công")
                .data(profile)
                .build());
    }

    // Student tự cập nhật profile của mình
    @PutMapping("/me/profile")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<StudentDetailDTO>> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody org.fyp.tmssep490be.dtos.user.UpdateProfileRequest request
    ) {
        log.info("Student {} đang cập nhật profile", currentUser.getId());
        StudentDetailDTO profile = studentService.updateStudentProfileByUserId(currentUser.getId(), request);
        return ResponseEntity.ok(ResponseObject.<StudentDetailDTO>builder()
                .success(true)
                .message("Cập nhật profile thành công")
                .data(profile)
                .build());
    }

    // Student tự lấy danh sách lớp của mình
    @GetMapping("/me/classes")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<List<StudentClassDTO>>> getMyClasses(
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        Long studentId = studentContextHelper.getStudentId(currentUser);
        log.info("Student {} retrieving own classes", studentId);

        // Get ALL student classes (no pagination - realistic: 15-30 classes max)
        List<StudentClassDTO> classes = studentPortalService.getStudentClasses(studentId);

        return ResponseEntity.ok(ResponseObject.<List<StudentClassDTO>>builder()
                .success(true)
                .message("Lấy danh sách lớp thành công")
                .data(classes)
                .build());
    }

    @GetMapping("/assessors")
    @PreAuthorize("hasAnyRole('STUDENT', 'ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<List<AssessorDTO>>> getAssessors(
            @RequestParam Long branchId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("Student {} requesting assessors for branch {}", currentUser.getId(), branchId);

        List<AssessorDTO> assessors = studentService.getAssessorsByBranch(branchId);

        return ResponseEntity.ok(ResponseObject.<List<AssessorDTO>>builder()
                .success(true)
                .message("Lấy danh sách giảng viên thành công")
                .data(assessors)
                .build());
    }

}
