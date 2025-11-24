package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.studentportal.*;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.StudentPortalService;
import org.fyp.tmssep490be.utils.StudentContextHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for Student Portal operations
 * Provides endpoints for students to view their classes and related information
 */
@RestController
@RequestMapping("/api/v1/students/{studentId}")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Student Portal", description = "Student portal APIs for viewing classes and progress")
@SecurityRequirement(name = "bearerAuth")
public class StudentPortalController {

    private final StudentPortalService studentPortalService;
    private final StudentContextHelper studentContextHelper;

    /**
     * Get classes enrolled by a student with filtering and pagination
     * GET /api/v1/students/{studentId}/classes
     */
    @GetMapping("/classes")
    @Operation(
            summary = "Get student's enrolled classes",
            description = "Get all classes enrolled by the student with optional filtering by status, branch, course, and modality"
    )
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<Page<StudentClassDTO>>> getStudentClasses(
            @Parameter(description = "Student ID (ignored, derived từ token)") @PathVariable Long studentId,
            @Parameter(description = "Class status filters") @RequestParam(required = false) List<String> status,
            @Parameter(description = "Branch ID filters") @RequestParam(required = false) List<Long> branchId,
            @Parameter(description = "Course ID filters") @RequestParam(required = false) List<Long> courseId,
            @Parameter(description = "Modality filters") @RequestParam(required = false) List<String> modality,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "enrollmentDate") String sort,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} retrieving classes for student: {}", currentUser.getId(), studentId);

        // Luôn lấy studentId từ principal để tránh client truyền userId/nhầm ID
        Long authStudentId = studentContextHelper.getStudentId(currentUser);
        if (studentId != null && !authStudentId.equals(studentId)) {
            log.warn("Overriding requested studentId {} with authenticated studentId {}", studentId, authStudentId);
        }

        // Create pageable with sorting
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        // Get student classes
        Page<StudentClassDTO> classes = studentPortalService.getStudentClasses(
                authStudentId, status, branchId, courseId, modality, pageable
        );

        return ResponseEntity.ok(ResponseObject.success(classes));
    }

    /**
     * Get transcript for a student
     * GET /api/v1/students/{studentId}/transcript
     */
    @GetMapping("/transcript")
    @Operation(
            summary = "Get student transcript",
            description = "Get complete academic transcript including all classes, scores, and progress"
    )
    @PreAuthorize("hasRole('STUDENT') or hasRole('MANAGER')")
    public ResponseEntity<ResponseObject<List<StudentTranscriptDTO>>> getStudentTranscript(
            @Parameter(description = "Student ID (ignored, derived từ token)") @PathVariable Long studentId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} retrieving transcript for student: {}", currentUser.getId(), studentId);

        // Luôn lấy studentId từ principal để tránh client truyền userId/nhầm ID
        Long authStudentId = studentContextHelper.getStudentId(currentUser);
        if (studentId != null && !authStudentId.equals(studentId)) {
            log.warn("Overriding requested studentId {} with authenticated studentId {}", studentId, authStudentId);
        }

        List<StudentTranscriptDTO> transcript = studentPortalService.getStudentTranscript(authStudentId);

        return ResponseEntity.ok(ResponseObject.success(transcript));
    }
}
