package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.createclass.AssignTimeSlotsRequest;
import org.fyp.tmssep490be.dtos.createclass.AssignTimeSlotsResponse;
import org.fyp.tmssep490be.dtos.createclass.CreateClassRequest;
import org.fyp.tmssep490be.dtos.createclass.CreateClassResponse;
import org.fyp.tmssep490be.dtos.createclass.RejectClassRequest;
import org.fyp.tmssep490be.dtos.createclass.SubmitClassResponse;
import org.fyp.tmssep490be.dtos.createclass.ValidateClassResponse;
import org.fyp.tmssep490be.dtos.classmanagement.*;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.Modality;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.ClassService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Controller for class management operations
 * Provides endpoints for Academic Affairs staff to view and manage classes
 */
@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Class Management", description = "Class management APIs for Academic Affairs")
@SecurityRequirement(name = "bearerAuth")
public class ClassController {

    private final ClassService classService;

    /**
     * Get list of classes accessible to academic affairs user
     * Filters by user's branch assignments and applies search/filter criteria
     */
    @GetMapping
    @Operation(
            summary = "Get classes list",
            description = "Retrieve paginated list of classes accessible to the user with filtering options. " +
                    "By default, returns all classes regardless of status."
    )
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<Page<ClassListItemDTO>>> getClasses(
            @Parameter(description = "Filter by branch ID(s). If not provided, uses user's accessible branches")
            @RequestParam(required = false) List<Long> branchIds,

            @Parameter(description = "Filter by course ID")
            @RequestParam(required = false) Long courseId,

            @Parameter(description = "Filter by class status (DRAFT, SCHEDULED, ONGOING, COMPLETED, CANCELLED). If not provided, returns all statuses")
            @RequestParam(required = false) ClassStatus status,

            @Parameter(description = "Filter by approval status (PENDING, APPROVED, REJECTED). If not provided, returns all approval statuses")
            @RequestParam(required = false) ApprovalStatus approvalStatus,

            @Parameter(description = "Filter by modality (ONLINE, OFFLINE, HYBRID)")
            @RequestParam(required = false) Modality modality,

            @Parameter(description = "Search term for class code, name, course name, or branch name")
            @RequestParam(required = false) String search,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort field and direction")
            @RequestParam(defaultValue = "startDate") String sort,

            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "asc") String sortDir,

            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requesting classes list with filters: branchIds={}, courseId={}, status={}, approvalStatus={}, modality={}, search={}",
                currentUser.getId(), branchIds, courseId, status, approvalStatus, modality, search);

        // Create pageable with sort
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));

        Page<ClassListItemDTO> classes = classService.getClasses(
                branchIds, courseId, status, approvalStatus, modality, search, pageable, currentUser.getId()
        );

        return ResponseEntity.ok(ResponseObject.<Page<ClassListItemDTO>>builder()
                .success(true)
                .message("Classes retrieved successfully")
                .data(classes)
                .build());
    }

    /**
     * Get detailed information about a specific class
     * Includes enrollment summary and upcoming sessions
     */
    @GetMapping("/{classId}")
    @Operation(
            summary = "Get class details",
            description = "Retrieve comprehensive information about a specific class including enrollment summary and upcoming sessions"
    )
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<ClassDetailDTO>> getClassDetail(
            @Parameter(description = "Class ID")
            @PathVariable Long classId,

            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requesting details for class {}", currentUser.getId(), classId);

        ClassDetailDTO classDetail = classService.getClassDetail(classId, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<ClassDetailDTO>builder()
                .success(true)
                .message("Class details retrieved successfully")
                .data(classDetail)
                .build());
    }

    /**
     * Get list of students currently enrolled in a class
     * Supports search and pagination
     */
    @GetMapping("/{classId}/students")
    @Operation(
            summary = "Get class students",
            description = "Retrieve paginated list of students currently enrolled in a specific class with search functionality"
    )
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<Page<ClassStudentDTO>>> getClassStudents(
            @Parameter(description = "Class ID")
            @PathVariable Long classId,

            @Parameter(description = "Search term for student code, name, email, or phone")
            @RequestParam(required = false) String search,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort field and direction")
            @RequestParam(defaultValue = "enrolledAt") String sort,

            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "desc") String sortDir,

            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requesting students for class {} with search: {}", currentUser.getId(), classId, search);

        // Create pageable with sort
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));

        Page<ClassStudentDTO> students = classService.getClassStudents(
                classId, search, pageable, currentUser.getId()
        );

        return ResponseEntity.ok(ResponseObject.<Page<ClassStudentDTO>>builder()
                .success(true)
                .message("Class students retrieved successfully")
                .data(students)
                .build());
    }

    /**
     * Get quick enrollment summary for a class
     * Lightweight endpoint for capacity checks and list views
     */
    @GetMapping("/{classId}/summary")
    @Operation(
            summary = "Get class enrollment summary",
            description = "Retrieve lightweight enrollment summary with capacity information for quick checks and list views"
    )
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<ClassEnrollmentSummaryDTO>> getClassEnrollmentSummary(
            @Parameter(description = "Class ID")
            @PathVariable Long classId,

            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requesting enrollment summary for class {}", currentUser.getId(), classId);

        ClassEnrollmentSummaryDTO summary = classService.getClassEnrollmentSummary(classId, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<ClassEnrollmentSummaryDTO>builder()
                .success(true)
                .message("Class enrollment summary retrieved successfully")
                .data(summary)
                .build());
    }

    /**
     * Get available students for enrollment in a class with complete assessment data
     * Returns students from the same branch who are not yet enrolled with full replacement skill assessment history
     * Smart sorting based on skill assessment matching
     */
    @GetMapping("/{classId}/available-students")
    @Operation(
            summary = "Get available students for enrollment",
            description = "Retrieve students who are available to enroll in the class with complete replacement skill assessment history, " +
                    "sorted by skill assessment match priority. Priority levels:\n" +
                    "1. Perfect match - Assessment matches both Subject AND Level\n" +
                    "2. Partial match - Assessment matches Subject only\n" +
                    "3. No match - No assessment or different Subject\n\n" +
                    "Response includes complete student assessment data:\n" +
                    "- All replacement skill assessments (READING, WRITING, SPEAKING, LISTENING, GENERAL)\n" +
                    "- Assessment details: scores, dates, types, notes, and assessors\n" +
                    "- Level information with subject context and duration expectations\n" +
                    "- Class match analysis with detailed reasoning for recommendations"
    )
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<Page<AvailableStudentDTO>>> getAvailableStudentsForClass(
            @Parameter(description = "Class ID to enroll students into")
            @PathVariable Long classId,

            @Parameter(description = "Search term for student code, name, email, or phone")
            @RequestParam(required = false) String search,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort field (default: matchPriority for smart sorting)")
            @RequestParam(defaultValue = "matchPriority") String sort,

            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "asc") String sortDir,

            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requesting available students for class {} with search: {}",
                currentUser.getId(), classId, search);

        // Create pageable with sort
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));

        Page<AvailableStudentDTO> availableStudents = classService.getAvailableStudentsForClass(
                classId, search, pageable, currentUser.getId()
        );

        return ResponseEntity.ok(ResponseObject.<Page<AvailableStudentDTO>>builder()
                .success(true)
                .message("Available students retrieved successfully")
                .data(availableStudents)
                .build());
    }

    // Create Class Workflow endpoints (STEP 1, 3, 6, 7)

    /**
     * STEP 1: Create a new class and auto-generate sessions
     */
    @PostMapping
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    @Operation(
            summary = "Create new class",
            description = "Create a new class with DRAFT status and auto-generate sessions based on course template. " +
                    "This is STEP 1 of the 7-step Create Class workflow. Sessions are automatically generated (STEP 2). " +
                    "After creation, proceed to assign time slots (STEP 3), resources (STEP 4), teachers (STEP 5), " +
                    "validate (STEP 6), and submit for approval (STEP 7)."
    )
    public ResponseEntity<ResponseObject<CreateClassResponse>> createClass(
            @Parameter(description = "Class creation request with all required information")
            @RequestBody @Valid CreateClassRequest request,

            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} creating new class with code: {}", currentUser.getId(), request.getCode());

        CreateClassResponse response = classService.createClass(request, currentUser.getId());

        String message = response.isSuccess() ?
                String.format("Class %s created successfully with %d sessions generated",
                        response.getCode(), response.getSessionSummary().getSessionsGenerated()) :
                "Failed to create class";

        return ResponseEntity.ok(ResponseObject.<CreateClassResponse>builder()
                .success(response.isSuccess())
                .message(message)
                .data(response)
                .build());
    }

    /**
     * STEP 3: Assign time slots to class sessions
     */
    @PostMapping("/{classId}/time-slots")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    @Operation(
            summary = "Assign time slots",
            description = "Assign time slots to class sessions based on day of week patterns. " +
                    "This is STEP 3 of the Create Class workflow. All sessions matching the specified day of week " +
                    "will be assigned the corresponding time slot."
    )
    public ResponseEntity<ResponseObject<AssignTimeSlotsResponse>> assignTimeSlots(
            @Parameter(description = "Class ID")
            @PathVariable Long classId,

            @Parameter(description = "Time slot assignment details")
            @RequestBody @Valid AssignTimeSlotsRequest request,

            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} assigning time slots to class {}", currentUser.getId(), classId);

        AssignTimeSlotsResponse response = classService.assignTimeSlots(classId, request, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<AssignTimeSlotsResponse>builder()
                .success(response.isSuccess())
                .message(response.getMessage())
                .data(response)
                .build());
    }

    /**
     * STEP 6: Validate class completeness before submission
     */
    @PostMapping("/{classId}/validate")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    @Operation(
            summary = "Validate class completeness",
            description = "Validate that all required assignments are completed before submission. " +
                    "This is STEP 6 of the Create Class workflow. Checks timeslot, resource, and teacher assignments."
    )
    public ResponseEntity<ResponseObject<ValidateClassResponse>> validateClass(
            @Parameter(description = "Class ID")
            @PathVariable Long classId,

            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} validating class {}", currentUser.getId(), classId);

        ValidateClassResponse response = classService.validateClass(classId, currentUser.getId());

        String message = response.isReadyForSubmission() ?
                "Class is complete and ready for submission" :
                response.getValidationSummary();

        return ResponseEntity.ok(ResponseObject.<ValidateClassResponse>builder()
                .success(response.isValid())
                .message(message)
                .data(response)
                .build());
    }

    /**
     * STEP 7: Submit class for approval
     */
    @PostMapping("/{classId}/submit")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    @Operation(
            summary = "Submit class for approval",
            description = "Submit class for Center Head approval. This is STEP 7 of the Create Class workflow. " +
                    "Class must be validated before submission. After submission, Center Head can approve or reject."
    )
    public ResponseEntity<ResponseObject<SubmitClassResponse>> submitClass(
            @Parameter(description = "Class ID")
            @PathVariable Long classId,

            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} submitting class {} for approval", currentUser.getId(), classId);

        SubmitClassResponse response = classService.submitClass(classId, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<SubmitClassResponse>builder()
                .success(response.isSuccess())
                .message(response.getSubmissionSummary())
                .data(response)
                .build());
    }

    /**
     * STEP 7: Approve submitted class (Center Head only)
     */
    @PostMapping("/{classId}/approve")
    @PreAuthorize("hasRole('CENTER_HEAD')")
    @Operation(
            summary = "Approve class",
            description = "Approve submitted class and change status to SCHEDULED. This is STEP 7 of the workflow. " +
                    "Only users with CENTER_HEAD role can approve classes."
    )
    public ResponseEntity<ResponseObject<String>> approveClass(
            @Parameter(description = "Class ID")
            @PathVariable Long classId,

            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("Center Head {} approving class {}", currentUser.getId(), classId);

        classService.approveClass(classId, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<String>builder()
                .success(true)
                .message("Class approved successfully")
                .data("Class status changed to SCHEDULED")
                .build());
    }

    /**
     * STEP 7: Reject submitted class (Center Head only)
     */
    @PostMapping("/{classId}/reject")
    @PreAuthorize("hasRole('CENTER_HEAD')")
    @Operation(
            summary = "Reject class",
            description = "Reject submitted class and provide rejection reason. This is STEP 7 of the workflow. " +
                    "Only users with CENTER_HEAD role can reject classes. Class status resets to DRAFT."
    )
    public ResponseEntity<ResponseObject<String>> rejectClass(
            @Parameter(description = "Class ID")
            @PathVariable Long classId,

            @Parameter(description = "Rejection reason")
            @RequestBody RejectClassRequest request,

            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("Center Head {} rejecting class {} with reason: {}", currentUser.getId(), classId, request.getReason());

        classService.rejectClass(classId, request.getReason(), currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<String>builder()
                .success(true)
                .message("Class rejected successfully")
                .data("Class status reset to DRAFT")
                .build());
    }
}