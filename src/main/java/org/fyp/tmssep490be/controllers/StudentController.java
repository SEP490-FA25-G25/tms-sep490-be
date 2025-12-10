package org.fyp.tmssep490be.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.studentmanagement.*;
import org.fyp.tmssep490be.entities.enums.Gender;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.StudentService;
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
    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";


    @GetMapping
    @PreAuthorize("hasRole('ROLE_ACADEMIC_AFFAIR')")
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
    @PreAuthorize("hasRole('ROLE_ACADEMIC_AFFAIR')")
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
    
    @GetMapping("/{studentId}")
    @PreAuthorize("hasRole('ROLE_ACADEMIC_AFFAIR')")
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

    // Template học viên của lớp khi đưa cho sale, sale lấy template và gửi lại giáo vụ những người đăng ký và add vào hệ thống
    @GetMapping("/import/template")
    @PreAuthorize("hasRole('ROLE_ACADEMIC_AFFAIR')")
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
    @PreAuthorize("hasRole('ROLE_ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<StudentImportPreview>> previewImport(
            @RequestParam Long branchId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} previewing student import for branch {}", currentUser.getId(), branchId);

        if (file.isEmpty()) {
            throw new org.fyp.tmssep490be.exceptions.CustomException(org.fyp.tmssep490be.exceptions.ErrorCode.EXCEL_FILE_EMPTY);
        }

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
    @PreAuthorize("hasRole('ROLE_ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<StudentImportResult>> executeImport(
            @Valid @RequestBody StudentImportExecuteRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} executing student import for branch {} with {} students",
                currentUser.getId(), request.getBranchId(), request.getStudents().size());

        StudentImportResult result = studentService.executeStudentImport(request, currentUser.getId());

        log.info("Student import completed. Created: {}, Failed: {}",
                result.getSuccessfulCreations(), result.getFailedCreations());

        return ResponseEntity.ok(ResponseObject.<StudentImportResult>builder()
                .success(true)
                .message(String.format("Successfully imported %d students", result.getSuccessfulCreations()))
                .data(result)
                .build());
    }

}
