package org.fyp.tmssep490be.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.enrollment.ClassEnrollmentImportExecuteRequest;
import org.fyp.tmssep490be.dtos.enrollment.ClassEnrollmentImportPreview;
import org.fyp.tmssep490be.dtos.enrollment.EnrollExistingStudentsRequest;
import org.fyp.tmssep490be.dtos.enrollment.EnrollmentResult;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.EnrollmentService;
import org.fyp.tmssep490be.services.EnrollmentTemplateService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
@Slf4j
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final EnrollmentTemplateService enrollmentTemplateService;
    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @GetMapping("/classes/{classId}/template")
    @PreAuthorize("hasRole('ROLE_ACADEMIC_AFFAIR')")
    public ResponseEntity<Resource> downloadClassTemplate(
            @PathVariable Long classId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requested class-specific enrollment template for class {}",
                currentUser.getId(), classId);

        byte[] templateData = enrollmentTemplateService.generateExcelTemplateWithClassInfo(classId);
        ByteArrayResource resource = new ByteArrayResource(templateData);

        // Get class name for filename (optional enhancement)
        String filename = "class-" + classId + "-enrollment-template.xlsx";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(XLSX_CONTENT_TYPE))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .body(resource);
    }

    @PostMapping(value = "/classes/{classId}/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ROLE_ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject> previewImport(
            @PathVariable Long classId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("Preview import request for class {} by user {}", classId, currentUser.getId());

        if (file.isEmpty()) {
            throw new CustomException(ErrorCode.EXCEL_FILE_EMPTY);
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        // Validate file extension
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE_XLSX);
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals(XLSX_CONTENT_TYPE)) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE_XLSX);
        }

        ClassEnrollmentImportPreview preview = enrollmentService.previewClassEnrollmentImport(
                classId, file, currentUser.getId()
        );

        log.info("Preview completed for class {}. Total students: {}",
                classId, preview.getStudents().size());

        return ResponseEntity.ok(ResponseObject.builder()
                .success(true)
                .message("Import preview ready")
                .data(preview)
                .build());
    }


    @PostMapping("/classes/{classId}/import/execute")
    @PreAuthorize("hasRole('ROLE_ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject> executeImport(
            @PathVariable Long classId,
            @RequestBody @Valid ClassEnrollmentImportExecuteRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("Execute import request for class {} with strategy {} by user {}",
                classId, request.getStrategy(), currentUser.getId());

        // Validate classId match
        if (!classId.equals(request.getClassId())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        EnrollmentResult result = enrollmentService.executeClassEnrollmentImport(
                request,
                currentUser.getId()
        );

        log.info("Enrollment completed for class {}. Enrolled: {}, Created: {}, Total sessions: {}",
                classId, result.getEnrolledCount(), result.getStudentsCreated(),
                result.getTotalStudentSessionsCreated());

        return ResponseEntity.ok(ResponseObject.builder()
                .success(true)
                .message(String.format("Successfully enrolled %d students", result.getEnrolledCount()))
                .data(result)
                .build());
    }

    @PostMapping("/classes/{classId}/students")
    @PreAuthorize("hasRole('ROLE_ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<EnrollmentResult>> enrollExistingStudents(
            @PathVariable Long classId,
            @RequestBody @Valid EnrollExistingStudentsRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("Enroll existing students request for class {} with {} students by user {}",
                classId, request.getStudentIds().size(), currentUser.getId());

        // Validate classId match
        if (!classId.equals(request.getClassId())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        EnrollmentResult result = enrollmentService.enrollExistingStudents(
                request,
                currentUser.getId()
        );

        log.info("Enrollment completed for class {}. Enrolled: {}, Total sessions: {}",
                classId, result.getEnrolledCount(), result.getTotalStudentSessionsCreated());

        return ResponseEntity.ok(ResponseObject.<EnrollmentResult>builder()
                .success(true)
                .message(String.format("Successfully enrolled %d students into class", result.getEnrolledCount()))
                .data(result)
                .build());
    }
}
