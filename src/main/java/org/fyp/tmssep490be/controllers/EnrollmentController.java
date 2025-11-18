package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * Controller cho enrollment management
 * Primary workflow: Class-specific student enrollment via Excel import
 */
@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Enrollment", description = "Enrollment management APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final EnrollmentTemplateService enrollmentTemplateService;

    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /**
     * Download generic Excel template for student enrollment
     * GET /api/v1/enrollments/template
     *
     * Simplified 7-column format: full_name, email, phone, facebook_url, address, gender, dob
     */
    @GetMapping("/template")
    @PreAuthorize("hasRole('ROLE_ACADEMIC_AFFAIR')")
    @Operation(
            summary = "Download Excel enrollment template",
            description = "Download a generic Excel template with 7 columns for student enrollment"
    )
    public ResponseEntity<Resource> downloadGenericTemplate(
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requested generic enrollment template", currentUser.getId());

        byte[] templateData = enrollmentTemplateService.generateExcelTemplate();
        ByteArrayResource resource = new ByteArrayResource(templateData);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(XLSX_CONTENT_TYPE))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=student-enrollment-template.xlsx")
                .body(resource);
    }

    /**
     * Download class-specific Excel template
     * GET /api/v1/enrollments/classes/{classId}/template
     *
     * Includes class information and sample data in the template
     */
    @GetMapping("/classes/{classId}/template")
    @PreAuthorize("hasRole('ROLE_ACADEMIC_AFFAIR')")
    @Operation(
            summary = "Download class-specific Excel template",
            description = "Download a class-specific Excel template with class information and sample data"
    )
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

    /**
     * Preview import Excel cho class enrollment
     * POST /api/v1/enrollments/classes/{classId}/import/preview
     *
     * Workflow:
     * 1. Parse Excel file
     * 2. Resolve students (FOUND/CREATE/ERROR)
     * 3. Calculate capacity
     * 4. Return preview với recommendation
     */
    @PostMapping(value = "/classes/{classId}/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ROLE_ACADEMIC_AFFAIR')")
    @Operation(
            summary = "Preview Excel import for class enrollment",
            description = "Parse Excel file, resolve students, calculate capacity, and provide recommendations"
    )
    public ResponseEntity<ResponseObject> previewImport(
            @PathVariable Long classId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("Preview import request for class {} by user {}", classId, currentUser.getId());

        // Validate file type
        if (file.isEmpty()) {
            throw new CustomException(ErrorCode.EXCEL_FILE_EMPTY);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals(XLSX_CONTENT_TYPE)) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE_XLSX);
        }

        ClassEnrollmentImportPreview preview = enrollmentService.previewClassEnrollmentImport(
                classId, file, currentUser.getId()
        );

        log.info("Preview completed for class {}. Valid students: {}, Errors: {}",
                classId, preview.getTotalValid(), preview.getErrorCount());

        return ResponseEntity.ok(ResponseObject.builder()
                .success(true)
                .message("Import preview ready")
                .data(preview)
                .build());
    }

    /**
     * Execute import sau khi preview và confirm
     * POST /api/v1/enrollments/classes/{classId}/import/execute
     *
     * Workflow:
     * 1. Lock class (pessimistic)
     * 2. Filter students theo strategy (ALL/PARTIAL/OVERRIDE)
     * 3. Create new students if needed
     * 4. Create enrollment records
     * 5. Auto-generate student_session records
     * 6. Send welcome emails (async) - commented out
     */
    @PostMapping("/classes/{classId}/import/execute")
    @PreAuthorize("hasRole('ROLE_ACADEMIC_AFFAIR')")
    @Operation(
            summary = "Execute enrollment import",
            description = "Create enrollments and auto-generate student sessions based on strategy"
    )
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

    /**
     * Enroll existing students vào class (Tab 1: Select Existing Students)
     * POST /api/v1/enrollments/classes/{classId}/students
     *
     * Use case: Manual multi-select enrollment từ danh sách students đã có trong DB
     * Workflow:
     * 1. Validate class status và capacity
     * 2. Validate all students exist
     * 3. Check for duplicate enrollments
     * 4. Handle capacity override nếu cần
     * 5. Create enrollment records
     * 6. Auto-generate student_session records
     */
    @PostMapping("/classes/{classId}/students")
    @PreAuthorize("hasRole('ROLE_ACADEMIC_AFFAIR')")
    @Operation(
            summary = "Enroll existing students into class",
            description = "Multi-select enrollment for existing students. Supports capacity override with reason."
    )
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
