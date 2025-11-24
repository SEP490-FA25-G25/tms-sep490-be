package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.createclass.AssignResourcesRequest;
import org.fyp.tmssep490be.dtos.createclass.AssignResourcesResponse;
import org.fyp.tmssep490be.dtos.createclass.AssignSessionResourceRequest;
import org.fyp.tmssep490be.dtos.createclass.AssignSessionResourceResponse;
import org.fyp.tmssep490be.dtos.createclass.AssignTeacherRequest;
import org.fyp.tmssep490be.dtos.createclass.AssignTeacherResponse;
import org.fyp.tmssep490be.dtos.createclass.AssignTimeSlotsRequest;
import org.fyp.tmssep490be.dtos.createclass.AssignTimeSlotsResponse;
import org.fyp.tmssep490be.dtos.createclass.AvailableResourceDTO;
import org.fyp.tmssep490be.dtos.createclass.CreateClassRequest;
import org.fyp.tmssep490be.dtos.createclass.CreateClassResponse;
import org.fyp.tmssep490be.dtos.createclass.PreviewClassCodeRequest;
import org.fyp.tmssep490be.dtos.createclass.PreviewClassCodeResponse;
import org.fyp.tmssep490be.dtos.createclass.SessionListResponse;
import org.fyp.tmssep490be.dtos.createclass.TeacherAvailabilityDTO;
import org.fyp.tmssep490be.dtos.createclass.TeacherDayAvailabilityDTO;
import org.fyp.tmssep490be.dtos.createclass.UpdateClassRequest;
// import org.fyp.tmssep490be.dtos.createclass.RejectClassRequest; // Removed - now using classmanagement package
// import org.fyp.tmssep490be.dtos.createclass.SubmitClassResponse; // Removed - now using classmanagement package
// import org.fyp.tmssep490be.dtos.createclass.ValidateClassResponse; // Removed - now using classmanagement package
import org.fyp.tmssep490be.dtos.classmanagement.*;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.entities.Resource;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.Modality;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.ClassCodeGeneratorService;
import org.fyp.tmssep490be.services.ClassService;
import org.fyp.tmssep490be.services.ResourceAssignmentService;
import org.fyp.tmssep490be.utils.AssignResourcesResponseUtil;
import org.fyp.tmssep490be.utils.ValidateClassResponseUtil;
import org.fyp.tmssep490be.utils.CreateClassResponseUtil;
import org.fyp.tmssep490be.utils.AssignTimeSlotsResponseUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
  private final ResourceAssignmentService resourceAssignmentService;
  private final ClassCodeGeneratorService classCodeGeneratorService;
  private final ValidateClassResponseUtil validateClassResponseUtil;
  private final CreateClassResponseUtil createClassResponseUtil;
  private final AssignTimeSlotsResponseUtil assignTimeSlotsResponseUtil;
  private final AssignResourcesResponseUtil assignResourcesResponseUtil;

  /**
   * Get list of classes accessible to user
   * Filters by user's branch assignments and applies search/filter criteria
   * Accessible by: ACADEMIC_AFFAIR (create/manage classes), CENTER_HEAD
   * (review/approve classes)
   */
  @GetMapping
  @Operation(summary = "Get classes list", description = "Retrieve paginated list of classes accessible to the user with filtering options. "
      +
      "By default, returns all classes regardless of status. " +
      "Academic Affairs can create and manage classes. Center Head can review and approve classes.")
  @PreAuthorize("hasRole('ACADEMIC_AFFAIR') or hasRole('CENTER_HEAD')")
  public ResponseEntity<ResponseObject<Page<ClassListItemDTO>>> getClasses(
      @Parameter(description = "Filter by branch ID(s). If not provided, uses user's accessible branches") @RequestParam(required = false) List<Long> branchIds,

      @Parameter(description = "Filter by course ID") @RequestParam(required = false) Long courseId,

      @Parameter(description = "Filter by class status (DRAFT, SCHEDULED, ONGOING, COMPLETED, CANCELLED). If not provided, returns all statuses") @RequestParam(required = false) ClassStatus status,

      @Parameter(description = "Filter by approval status (PENDING, APPROVED, REJECTED). If not provided, returns all approval statuses") @RequestParam(required = false) ApprovalStatus approvalStatus,

      @Parameter(description = "Filter by modality (ONLINE, OFFLINE, HYBRID)") @RequestParam(required = false) Modality modality,

      @Parameter(description = "Search term for class code, name, course name, or branch name") @RequestParam(required = false) String search,

      @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,

      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,

      @Parameter(description = "Sort field and direction") @RequestParam(defaultValue = "startDate") String sort,

      @Parameter(description = "Sort direction") @RequestParam(defaultValue = "asc") String sortDir,

      @AuthenticationPrincipal UserPrincipal currentUser) {
    log.info(
        "User {} requesting classes list with filters: branchIds={}, courseId={}, status={}, approvalStatus={}, modality={}, search={}",
        currentUser.getId(), branchIds, courseId, status, approvalStatus, modality, search);

    // Create pageable with sort
    Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));

    Page<ClassListItemDTO> classes = classService.getClasses(
        branchIds, courseId, status, approvalStatus, modality, search, pageable, currentUser.getId());

    return ResponseEntity.ok(ResponseObject.<Page<ClassListItemDTO>>builder()
        .success(true)
        .message("Classes retrieved successfully")
        .data(classes)
        .build());
  }

  /**
   * STEP 0 (Optional): Preview class code before creation
   * This endpoint allows users to preview what the class code will be without
   * actually creating the class
   * Supports both GET (with query params) and POST (with request body)
   * IMPORTANT: This endpoint MUST be defined BEFORE /{classId} to avoid routing
   * conflicts
   */
  @GetMapping("/preview-code")
  @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
  @Operation(summary = "Preview class code (GET)", description = "Preview the next class code that will be generated based on branch, course, and start date. "
      +
      "Note: The previewed code may change if another class is created in the meantime. " +
      "This is an optional helper endpoint before STEP 1 (Create Class). " +
      "Use GET with query params or POST with request body.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Preview generated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseObject.class), examples = @ExampleObject(name = "Preview Success", value = """
          {
            "success": true,
            "message": "Class code preview generated successfully",
            "data": {
              "previewCode": "IELTSFOUND-HN01-25-005",
              "prefix": "IELTSFOUND-HN01-25",
              "nextSequence": 5,
              "warning": null
            }
          }
          """))),
      @ApiResponse(responseCode = "400", description = "Invalid request or sequence limit reached", content = @Content(mediaType = "application/json", examples = {
          @ExampleObject(name = "Validation Error", value = """
              {
                "success": false,
                "message": "Validation failed",
                "data": {
                  "branchId": "must not be null",
                  "courseId": "must not be null"
                }
              }
              """),
          @ExampleObject(name = "Sequence Limit Warning", value = """
              {
                "success": true,
                "message": "Class code preview generated successfully",
                "data": {
                  "previewCode": "IELTSFOUND-HN01-25-999",
                  "prefix": "IELTSFOUND-HN01-25",
                  "nextSequence": 999,
                  "warning": "Sequence limit reached (999). Cannot create more classes with this prefix."
                }
              }
              """)
      }))
  })
  public ResponseEntity<ResponseObject<PreviewClassCodeResponse>> previewClassCode(
      @Parameter(description = "Branch ID", required = true) @RequestParam Long branchId,

      @Parameter(description = "Course ID", required = true) @RequestParam Long courseId,

      @Parameter(description = "Start date (YYYY-MM-DD)", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

      @AuthenticationPrincipal UserPrincipal currentUser) {
    log.info("User {} previewing class code for branchId={}, courseId={}, startDate={}",
        currentUser.getId(), branchId, courseId, startDate);

    PreviewClassCodeResponse response = classService.previewClassCode(
        branchId,
        courseId,
        startDate,
        currentUser.getId());

    String message = response.getWarning() != null ? "Class code preview generated with warning"
        : "Class code preview generated successfully";

    return ResponseEntity.ok(ResponseObject.<PreviewClassCodeResponse>builder()
        .success(true)
        .message(message)
        .data(response)
        .build());
  }

  /**
   * STEP 2: Get list of generated sessions for review
   * Used for "Xem lại buổi học" step in frontend
   * Accessible by: ACADEMIC_AFFAIR (manage sessions), CENTER_HEAD (review for
   * approval)
   */
  @GetMapping("/{classId}/sessions")
  @PreAuthorize("hasRole('ACADEMIC_AFFAIR') or hasRole('CENTER_HEAD')")
  @Operation(summary = "Get list of sessions for review", description = """
      Retrieve all generated sessions for a class after creation (STEP 2).
      This endpoint is used for the "Xem lại buổi học" (Review Sessions) step in the frontend.

      **Response Structure:**
      - Class information (ID, code)
      - Total sessions count
      - Date range (first to last session)
      - Flat list of all sessions with details
      - Week-grouped view for better visualization
      - Assignment status per session (time slot, resource, teacher)

      **Session Details Include:**
      - Session ID, sequence number, date
      - Vietnamese day name (Thứ Hai, Thứ Ba, etc.)
      - Course session name from template
      - Assignment status flags (hasTimeSlot, hasResource, hasTeacher)
      - Time slot information (if assigned)

      **Week Grouping:**
      - Sessions grouped into 7-day week periods
      - Week number, date range, session count per week
      - Useful for timeline/calendar displays

      **Use Cases:**
      - Review sessions after creation before assigning time slots
      - Display session timeline/calendar in frontend
      - Show assignment progress overview
      - Verify session generation correctness
      """)
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Sessions retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseObject.class), examples = @ExampleObject(name = "Sessions List", value = """
          {
            "success": true,
            "message": "Sessions retrieved successfully",
            "data": {
              "classId": 6,
              "classCode": "IELTSFOUND-HN01-25-005",
              "totalSessions": 24,
              "dateRange": {
                "startDate": "2025-01-06",
                "endDate": "2025-03-31"
              },
              "sessions": [
                {
                  "sessionId": 101,
                  "sequenceNumber": 1,
                  "date": "2025-01-06",
                  "dayOfWeek": "Thứ Hai",
                  "courseSessionName": "Introduction to IELTS",
                  "status": "PLANNED",
                  "hasTimeSlot": false,
                  "hasResource": false,
                  "hasTeacher": false,
                  "timeSlotInfo": null
                }
              ],
              "groupedByWeek": [
                {
                  "weekNumber": 1,
                  "weekRange": "2025-01-06 - 2025-01-12",
                  "sessionCount": 3,
                  "sessionIds": [101, 102, 103]
                }
              ],
              "warnings": []
            }
          }
          """))),
      @ApiResponse(responseCode = "404", description = "Class not found", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
          {
            "success": false,
            "message": "Class with ID 999 not found",
            "data": null
          }
          """))),
      @ApiResponse(responseCode = "403", description = "Forbidden - User does not have access to this class's branch", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
          {
            "success": false,
            "message": "Access denied - user does not have access to this class's branch",
            "data": null
          }
          """)))
  })
  public ResponseEntity<ResponseObject<SessionListResponse>> listSessions(
      @Parameter(description = "Class ID") @PathVariable Long classId,

      @AuthenticationPrincipal UserPrincipal currentUser) {
    Long userId = currentUser != null ? currentUser.getId() : null;
    log.info("User {} requesting session list for class {}", userId, classId);

    SessionListResponse response = classService.listSessions(classId, userId);

    return ResponseEntity.ok(ResponseObject.<SessionListResponse>builder()
        .success(true)
        .message("Sessions retrieved successfully")
        .data(response)
        .build());
  }

  /**
   * Get detailed information about a specific class
   * Includes enrollment summary and upcoming sessions
   * Accessible by: ACADEMIC_AFFAIR, CENTER_HEAD (for approval review), STUDENT
   */
  @GetMapping("/{classId}")
  @Operation(summary = "Get class details", description = "Retrieve comprehensive information about a specific class including enrollment summary and upcoming sessions. "
      +
      "Center Head can access this to review class details before approval.")
  @PreAuthorize("hasRole('ACADEMIC_AFFAIR') or hasRole('CENTER_HEAD') or hasRole('STUDENT')")
  public ResponseEntity<ResponseObject<ClassDetailDTO>> getClassDetail(
      @Parameter(description = "Class ID") @PathVariable Long classId,

      @AuthenticationPrincipal UserPrincipal currentUser) {
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
  @Operation(summary = "Get class students", description = "Retrieve paginated list of students currently enrolled in a specific class with search functionality")
  @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
  public ResponseEntity<ResponseObject<Page<ClassStudentDTO>>> getClassStudents(
      @Parameter(description = "Class ID") @PathVariable Long classId,

      @Parameter(description = "Search term for student code, name, email, or phone") @RequestParam(required = false) String search,

      @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,

      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,

      @Parameter(description = "Sort field and direction") @RequestParam(defaultValue = "enrolledAt") String sort,

      @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDir,

      @AuthenticationPrincipal UserPrincipal currentUser) {
    log.info("User {} requesting students for class {} with search: {}", currentUser.getId(), classId, search);

    // Create pageable with sort
    Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));

    Page<ClassStudentDTO> students = classService.getClassStudents(
        classId, search, pageable, currentUser.getId());

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
  @Operation(summary = "Get class enrollment summary", description = "Retrieve lightweight enrollment summary with capacity information for quick checks and list views")
  @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
  public ResponseEntity<ResponseObject<ClassEnrollmentSummaryDTO>> getClassEnrollmentSummary(
      @Parameter(description = "Class ID") @PathVariable Long classId,

      @AuthenticationPrincipal UserPrincipal currentUser) {
    log.info("User {} requesting enrollment summary for class {}", currentUser.getId(), classId);

    ClassEnrollmentSummaryDTO summary = classService.getClassEnrollmentSummary(classId, currentUser.getId());

    return ResponseEntity.ok(ResponseObject.<ClassEnrollmentSummaryDTO>builder()
        .success(true)
        .message("Class enrollment summary retrieved successfully")
        .data(summary)
        .build());
  }

  /**
   * Get available students for enrollment in a class with complete assessment
   * data
   * Returns students from the same branch who are not yet enrolled with full
   * replacement skill assessment history
   * Smart sorting based on skill assessment matching
   */
  @GetMapping("/{classId}/available-students")
  @Operation(summary = "Get available students for enrollment", description = "Retrieve students who are available to enroll in the class with complete replacement skill assessment history, "
      +
      "sorted by skill assessment match priority. Priority levels:\n" +
      "1. Perfect match - Assessment matches both Subject AND Level\n" +
      "2. Partial match - Assessment matches Subject only\n" +
      "3. No match - No assessment or different Subject\n\n" +
      "Response includes complete student assessment data:\n" +
      "- All replacement skill assessments (READING, WRITING, SPEAKING, LISTENING, GENERAL)\n" +
      "- Assessment details: scores, dates, types, notes, and assessors\n" +
      "- Level information with subject context and duration expectations\n" +
      "- Class match analysis with detailed reasoning for recommendations")
  @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
  public ResponseEntity<ResponseObject<Page<AvailableStudentDTO>>> getAvailableStudentsForClass(
      @Parameter(description = "Class ID to enroll students into") @PathVariable Long classId,

      @Parameter(description = "Search term for student code, name, email, or phone") @RequestParam(required = false) String search,

      @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,

      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,

      @Parameter(description = "Sort field (default: matchPriority for smart sorting)") @RequestParam(defaultValue = "matchPriority") String sort,

      @Parameter(description = "Sort direction") @RequestParam(defaultValue = "asc") String sortDir,

      @AuthenticationPrincipal UserPrincipal currentUser) {
    log.info("User {} requesting available students for class {} with search: {}",
        currentUser.getId(), classId, search);

    // Create pageable with sort
    Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));

    Page<AvailableStudentDTO> availableStudents = classService.getAvailableStudentsForClass(
        classId, search, pageable, currentUser.getId());

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
  @Operation(summary = "Create new class", description = "Create a new class with DRAFT status and auto-generate sessions based on course template. "
      +
      "This is STEP 1 of the 7-step Create Class workflow. Sessions are automatically generated (STEP 2). " +
      "After creation, proceed to assign time slots (STEP 3), resources (STEP 4), teachers (STEP 5), " +
      "validate (STEP 6), and submit for approval (STEP 7).")
  public ResponseEntity<ResponseObject<CreateClassResponse>> createClass(
      @Parameter(description = "Class creation request with all required information") @RequestBody @Valid CreateClassRequest request,

      @AuthenticationPrincipal UserPrincipal currentUser) {
    log.info("User {} creating new class with code: {}", currentUser.getId(), request.getCode());

    CreateClassResponse response = classService.createClass(request, currentUser.getId());

    String message = createClassResponseUtil.isSuccess(response)
        ? String.format("Class %s created successfully with %d sessions generated",
            response.getCode(), response.getSessionSummary().getSessionsGenerated())
        : "Failed to create class";

    return ResponseEntity.ok(ResponseObject.<CreateClassResponse>builder()
        .success(createClassResponseUtil.isSuccess(response))
        .message(message)
        .data(response)
        .build());
  }

  /**
   * STEP 1 (Edit): Update basic class information while draft/rejected
   */
  @PutMapping("/{classId}")
  @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
  @Operation(summary = "Update class basic info", description = "Update draft/rejected class metadata for Edit Class step 1.")
  public ResponseEntity<ResponseObject<CreateClassResponse>> updateClass(
      @Parameter(description = "Class ID") @PathVariable Long classId,
      @Parameter(description = "Class update payload (same shape as create)") @RequestBody @Valid UpdateClassRequest request,
      @AuthenticationPrincipal UserPrincipal currentUser) {
    log.info("User {} updating class {}", currentUser.getId(), classId);

    CreateClassResponse response = classService.updateClass(classId, request, currentUser.getId());

    return ResponseEntity.ok(ResponseObject.<CreateClassResponse>builder()
        .success(true)
        .message("Class updated successfully")
        .data(response)
        .build());
  }

  /**
   * STEP 3: Assign time slots to class sessions
   */
  @PostMapping("/{classId}/time-slots")
  @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
  @Operation(summary = "Assign time slots", description = "Assign time slots to class sessions based on day of week patterns. "
      +
      "This is STEP 3 of the Create Class workflow. All sessions matching the specified day of week " +
      "will be assigned the corresponding time slot.")
  public ResponseEntity<ResponseObject<AssignTimeSlotsResponse>> assignTimeSlots(
      @Parameter(description = "Class ID") @PathVariable Long classId,

      @Parameter(description = "Time slot assignment details") @RequestBody @Valid AssignTimeSlotsRequest request,

      @AuthenticationPrincipal UserPrincipal currentUser) {
    log.info("User {} assigning time slots to class {}", currentUser.getId(), classId);

    AssignTimeSlotsResponse response = classService.assignTimeSlots(classId, request, currentUser.getId());

    return ResponseEntity.ok(ResponseObject.<AssignTimeSlotsResponse>builder()
        .success(assignTimeSlotsResponseUtil.isSuccess(response))
        .message(response.getMessage())
        .data(response)
        .build());
  }

  /**
   * STEP 4A: Get available resources for time slot and day of week
   */
  @GetMapping("/{classId}/resources")
  @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
  @Operation(summary = "Get available resources for Step 4", description = """
      Query available resources (rooms/online accounts) for a specific time slot and day of week.
      Used in Step 4 of Create Class workflow before resource assignment.

      Returns resources with:
      - Basic info (id, code, name, capacity)
      - Availability metadata (conflict count, availability rate)
      - Recommendation flag based on suitability

      Frontend expects:
      - timeSlotId: Time slot template ID from Step 3
      - dayOfWeek: Day of week (0=Sunday, 1=Monday, ..., 6=Saturday)
      """)
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successfully retrieved available resources", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseObject.class))),
      @ApiResponse(responseCode = "404", description = "Class not found", content = @Content(mediaType = "application/json"))
  })
  public ResponseEntity<ResponseObject<List<AvailableResourceDTO>>> getAvailableResources(
      @Parameter(description = "Class ID") @PathVariable Long classId,

      @Parameter(description = "Time slot template ID from Step 3", required = true) @RequestParam Long timeSlotId,

      @Parameter(description = "Day of week (0=Sunday, 1=Monday, ..., 6=Saturday)", required = true) @RequestParam Short dayOfWeek,

      @AuthenticationPrincipal UserPrincipal currentUser) {
    log.info("User {} requesting available resources for class {} on day {} at timeSlot {}",
        currentUser.getId(), classId, dayOfWeek, timeSlotId);

    // Query available resources with conflict metadata
    Map<Resource, Integer> resourceConflicts = resourceAssignmentService
        .queryAvailableResourcesWithConflicts(classId, timeSlotId, dayOfWeek);

    // Get total sessions for this day of week
    int totalSessions = resourceAssignmentService.getTotalSessionsForDayOfWeek(classId, dayOfWeek);

    if (totalSessions == 0) {
      log.warn("No sessions found for class {} on day {}", classId, dayOfWeek);
      return ResponseEntity.ok(ResponseObject.<List<AvailableResourceDTO>>builder()
          .success(true)
          .message("No sessions found for this day")
          .data(List.of())
          .build());
    }

    // Convert to DTOs with real conflict counts
    List<AvailableResourceDTO> resourceDTOs = resourceConflicts.entrySet().stream()
        .map(entry -> AvailableResourceDTO.fromEntity(
            entry.getKey(),
            entry.getValue(),
            totalSessions))
        .toList();

    log.info("Found {} available resources for class {} on day {} (total {} sessions)",
        resourceDTOs.size(), classId, dayOfWeek, totalSessions);

    return ResponseEntity.ok(ResponseObject.<List<AvailableResourceDTO>>builder()
        .success(true)
        .message("Available resources retrieved successfully")
        .data(resourceDTOs)
        .build());
  }

  /**
   * STEP 4B: Assign resources to class sessions
   */
  @PostMapping("/{classId}/resources")
  @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
  @Operation(summary = "Assign resources using HYBRID approach", description = """
      Assign resources (rooms/online accounts) to class sessions using HYBRID approach.
      This is STEP 4 of the Create Class workflow.

      **HYBRID Approach:**
      - Phase 1 (SQL Bulk Insert): Fast assignment for ~90% of sessions without conflicts
      - Phase 2 (Java Analysis): Detailed conflict analysis for remaining ~10% sessions

      **Performance:** Target <200ms for 36 sessions

      **Benefits:**
      - Fast execution with detailed conflict reporting
      - Academic Staff can resolve conflicts manually
      - Shows conflicting class names and reasons
      """)
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Resources assigned successfully (with or without conflicts)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseObject.class), examples = {
          @ExampleObject(name = "Full Success", description = "All sessions assigned without conflicts", value = """
              {
                "success": true,
                "message": "All 36 sessions assigned successfully in 142ms",
                "data": {
                  "successCount": 36,
                  "conflictCount": 0,
                  "totalSessions": 36,
                  "conflicts": [],
                  "processingTimeMs": 142
                }
              }
              """),
          @ExampleObject(name = "Partial Success with Conflicts", description = "Some sessions assigned, some have conflicts requiring manual resolution", value = """
              {
                "success": true,
                "message": "Resources assigned: 32/36 sessions successful, 4 conflicts requiring manual resolution (158ms)",
                "data": {
                  "successCount": 32,
                  "conflictCount": 4,
                  "totalSessions": 36,
                  "conflicts": [
                    {
                      "sessionId": 5,
                      "sessionDate": "2025-01-15",
                      "dayOfWeek": "MONDAY",
                      "conflictReason": "CAPACITY_EXCEEDED",
                      "requestedCapacity": 30,
                      "availableCapacity": 25,
                      "resourceId": 101,
                      "resourceName": "Room A101",
                      "conflictingClasses": ["CS102-B", "CS103-A"]
                    }
                  ],
                  "processingTimeMs": 158
                }
              }
              """)
      })),
      @ApiResponse(responseCode = "404", description = "Class or Resource not found", content = @Content(mediaType = "application/json", examples = {
          @ExampleObject(name = "Class Not Found", value = """
              {
                "success": false,
                "message": "Class with ID 999 not found",
                "data": null
              }
              """),
          @ExampleObject(name = "Resource Not Found", value = """
              {
                "success": false,
                "message": "Resource with ID 123 not found",
                "data": null
              }
              """)
      })),
      @ApiResponse(responseCode = "400", description = "Bad request - Validation error or business logic error", content = @Content(mediaType = "application/json", examples = {
          @ExampleObject(name = "Validation Error", description = "Request body validation failed", value = """
              {
                "success": false,
                "message": "Validation failed",
                "data": {
                  "resourceAssignments": "must not be empty"
                }
              }
              """),
          @ExampleObject(name = "Invalid Status", description = "Class not in DRAFT status", value = """
              {
                "success": false,
                "message": "Cannot assign resources: class status must be DRAFT",
                "data": null
              }
              """),
          @ExampleObject(name = "Branch Mismatch", description = "Resource does not belong to class's branch", value = """
              {
                "success": false,
                "message": "Resource 123 does not belong to class's branch",
                "data": null
              }
              """)
      })),
      @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
          {
            "success": false,
            "message": "Invalid username or password",
            "data": null
          }
          """))),
      @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions (requires ACADEMIC_AFFAIR role)", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
          {
            "success": false,
            "message": "Access denied - insufficient permissions",
            "data": null
          }
          """)))
  })
  public ResponseEntity<ResponseObject<AssignResourcesResponse>> assignResources(
      @Parameter(description = "Class ID") @PathVariable Long classId,

      @Parameter(description = "Resource assignment pattern (day of week → resource ID mapping)") @RequestBody @Valid AssignResourcesRequest request,

      @AuthenticationPrincipal UserPrincipal currentUser) {
    log.info("User {} assigning resources to class {}", currentUser.getId(), classId);

    AssignResourcesResponse response = classService.assignResources(classId, request, currentUser.getId());

    // Build success message based on conflicts
    String message;
    if (assignResourcesResponseUtil.isFullySuccessful(response)) {
      message = String.format("All %d sessions assigned successfully in %dms",
          response.getTotalSessions(), response.getProcessingTimeMs());
    } else {
      message = String.format(
          "Resources assigned: %d/%d sessions successful, %d conflicts requiring manual resolution (%dms)",
          response.getSuccessCount(), response.getTotalSessions(),
          response.getConflictCount(), response.getProcessingTimeMs());
    }

    return ResponseEntity.ok(ResponseObject.<AssignResourcesResponse>builder()
        .success(response.getSuccessCount() > 0)
        .message(message)
        .data(response)
        .build());
  }

  /**
   * STEP 4C: Assign resource manually to a single session (Quick Fix)
   */
  @PostMapping("/{classId}/sessions/{sessionId}/resource")
  @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
  public ResponseEntity<ResponseObject<AssignSessionResourceResponse>> assignResourceToSession(
      @PathVariable Long classId,
      @PathVariable Long sessionId,
      @RequestBody @Valid AssignSessionResourceRequest request,
      @AuthenticationPrincipal UserPrincipal currentUser) {
    AssignSessionResourceResponse response = classService.assignResourceToSession(
        classId,
        sessionId,
        request,
        currentUser.getId());

    return ResponseEntity.ok(ResponseObject.<AssignSessionResourceResponse>builder()
        .success(Boolean.TRUE.equals(response.getResolved()))
        .message(response.getMessage())
        .data(response)
        .build());
  }

  /**
   * STEP 4C: Get available resources for a specific session (Quick Fix
   * suggestions)
   */
  @GetMapping("/{classId}/sessions/{sessionId}/resources")
  @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
  public ResponseEntity<ResponseObject<List<AvailableResourceDTO>>> getSessionResources(
      @PathVariable Long classId,
      @PathVariable Long sessionId,
      @AuthenticationPrincipal UserPrincipal currentUser) {
    List<AvailableResourceDTO> resources = classService.getAvailableResourcesForSession(
        classId,
        sessionId,
        currentUser.getId());

    return ResponseEntity.ok(ResponseObject.<List<AvailableResourceDTO>>builder()
        .success(true)
        .message("Available resources retrieved successfully")
        .data(resources)
        .build());
  }

  /**
   * STEP 5A: Get available teachers with PRE-CHECK (Query before assignment)
   */
  @GetMapping("/{classId}/available-teachers")
  @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
  @Operation(summary = "Get available teachers using PRE-CHECK approach", description = """
      Query available teachers for class sessions using PRE-CHECK approach.
      This is STEP 5A of the Create Class workflow (query before assignment).

      **PRE-CHECK Approach:**
      - Execute complex CTE query showing ALL teachers with availability status
      - Academic Staff sees detailed conflict breakdown BEFORE selecting teacher
      - Direct bulk assignment without re-checking (PRE-CHECK already done)

      **4 Conflict Checks Per Session:**
      1. Teacher availability registered (teacher_availability table)
      2. No teaching conflict (not teaching another class at same time)
      3. No leave conflict (teaching_slot.status != 'ON_LEAVE')
      4. No skill mismatch (or has GENERAL skill = universal bypass)

      **Performance:** Target <100ms for 10 teachers × 36 sessions

      **Benefits:**
      - Transparency: Show ALL teachers (not just available ones)
      - Detailed breakdown: Exactly which sessions conflict and why
      - GENERAL skill handling: Universal skill bypasses all skill checks
      - No trial-and-error: One query shows everything

      **Response Structure:**
      - Teacher details (ID, name, skills, experience)
      - Availability status (FULLY_AVAILABLE, PARTIALLY_AVAILABLE, UNAVAILABLE)
      - Conflict breakdown (noAvailability, teachingConflict, leaveConflict, skillMismatch)
      - Available sessions count and percentage
      - Conflicting sessions with reasons
      - Sorted by availability percentage (descending)
      """)
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successfully retrieved teacher availability data", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseObject.class), examples = @ExampleObject(name = "Teachers with Availability Breakdown", description = "List of all teachers with detailed availability metrics", value = """
          {
            "success": true,
            "message": "Found 5 teachers. Use POST /classes/{classId}/teachers to assign.",
            "data": [
              {
                "teacherId": 101,
                "teacherName": "John Smith",
                "skills": ["JAVA", "SPRING_BOOT"],
                "yearsExperience": 5,
                "availabilityStatus": "FULLY_AVAILABLE",
                "availableSessions": 36,
                "totalSessions": 36,
                "availabilityPercentage": 100.0,
                "conflictBreakdown": {
                  "noAvailability": 0,
                  "teachingConflict": 0,
                  "leaveConflict": 0,
                  "skillMismatch": 0
                },
                "conflictingSessions": []
              },
              {
                "teacherId": 102,
                "teacherName": "Jane Doe",
                "skills": ["GENERAL"],
                "yearsExperience": 8,
                "availabilityStatus": "PARTIALLY_AVAILABLE",
                "availableSessions": 32,
                "totalSessions": 36,
                "availabilityPercentage": 88.9,
                "conflictBreakdown": {
                  "noAvailability": 0,
                  "teachingConflict": 4,
                  "leaveConflict": 0,
                  "skillMismatch": 0
                },
                "conflictingSessions": [
                  {
                    "sessionId": 5,
                    "sessionDate": "2025-01-15",
                    "conflictReason": "TEACHING_CONFLICT",
                    "conflictDetails": "Teaching CS102-B"
                  }
                ]
              }
            ]
          }
          """))),
      @ApiResponse(responseCode = "404", description = "Class not found", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
          {
            "success": false,
            "message": "Class with ID 999 not found",
            "data": null
          }
          """))),
      @ApiResponse(responseCode = "400", description = "Bad request - Invalid class status", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
          {
            "success": false,
            "message": "Cannot query teachers: class status must be DRAFT",
            "data": null
          }
          """))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
          {
            "success": false,
            "message": "Invalid username or password",
            "data": null
          }
          """))),
      @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions (requires ACADEMIC_AFFAIR role)", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
          {
            "success": false,
            "message": "Access denied - insufficient permissions",
            "data": null
          }
          """)))
  })
  public ResponseEntity<ResponseObject<List<TeacherAvailabilityDTO>>> getAvailableTeachers(
      @Parameter(description = "Class ID") @PathVariable Long classId,

      @AuthenticationPrincipal UserPrincipal currentUser) {
    // Handle null currentUser when security is disabled
    Long userId = (currentUser != null) ? currentUser.getId() : 1L; // Default to user ID 1 for testing
    log.info("User {} querying available teachers for class {}", userId, classId);

    List<TeacherAvailabilityDTO> teachers = classService.getAvailableTeachers(classId, userId);

    String message = String.format("Found %d teachers. Use POST /classes/{classId}/teachers to assign.",
        teachers.size());

    return ResponseEntity.ok(ResponseObject.<List<TeacherAvailabilityDTO>>builder()
        .success(true)
        .message(message)
        .data(teachers)
        .build());
  }

  /**
   * STEP 5A-ALTERNATE: Get teachers available by day of week (for multi-teacher
   * mode)
   */
  @GetMapping("/{classId}/teachers/available-by-day")
  @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
  @Operation(summary = "Get teachers available by specific days of week", description = """
      Query teachers who are available for at least ONE FULL DAY of the week consistently.
      This endpoint is used for "Phân công nhiều giáo viên" mode where different
      teachers teach different days (e.g., John on Mondays, Lisa on Wednesdays).

      **Use Case:**
      - User selects "Phân công nhiều giáo viên cho lớp học" option
      - Frontend calls this endpoint instead of regular /teachers endpoint
      - Returns teachers with day-level availability breakdown

      **Availability Criteria (per day):**
      - Teacher must be available for ALL sessions on that day of week
      - No teaching conflicts (not teaching another class at same time)
      - No leave conflicts (not on approved leave on any session date)
      - Has required skills or GENERAL skill
      - Covers from first session to last session on that day (consistency check)

      **Difference from regular /teachers endpoint:**
      - Regular: Returns teachers available for ALL 24 sessions (100% availability)
      - This: Returns teachers available for specific days (e.g., 8/24 sessions = 33% but 100% on Mondays)

      **Frontend Usage:**
      1. Display teachers grouped by available days
      2. Show "Có thể dạy: Thứ Hai (8 buổi)" for each teacher
      3. Allow user to select teacher + specific days to assign
      4. Call POST /teachers with sessionIds filtered by selected days
      """)
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Teachers with day availability retrieved successfully"),
      @ApiResponse(responseCode = "404", description = "Class not found")
  })
  public ResponseEntity<ResponseObject<List<TeacherDayAvailabilityDTO>>> getTeachersAvailableByDay(
      @Parameter(description = "Class ID") @PathVariable Long classId,

      @AuthenticationPrincipal UserPrincipal currentUser) {
    Long userId = (currentUser != null) ? currentUser.getId() : 1L;
    log.info("User {} querying teachers available by day for class {}", userId, classId);

    List<TeacherDayAvailabilityDTO> teachers = classService.getTeachersAvailableByDay(classId, userId);

    int teacherCount = teachers.size();
    int totalDays = teachers.stream()
        .mapToInt(t -> t.getAvailableDays().size())
        .sum();

    String message = String.format("Found %d teachers with availability on %d day(s)",
        teacherCount, totalDays);

    return ResponseEntity.ok(ResponseObject.<List<TeacherDayAvailabilityDTO>>builder()
        .success(true)
        .message(message)
        .data(teachers)
        .build());
  }

  /**
   * STEP 5B: Assign teacher to class sessions
   */
  @PostMapping("/{classId}/teachers")
  @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
  @Operation(summary = "Assign teacher to class sessions", description = """
      Assign a teacher to class sessions using PRE-CHECK approach.
      This is STEP 5B of the Create Class workflow (assignment after PRE-CHECK).

      **Assignment Modes:**

      1. **Full Assignment** (sessionIds = null):
         - Assign teacher to ALL unassigned sessions
         - Example: Assign teacher to entire class

      2. **Partial Assignment** (sessionIds provided):
         - Assign teacher only to specified sessions
         - Example: Assign teacher to specific weeks or days
         - Use case: Handle substitute teachers or split assignments

      **Request Body:**
      ```json
      {
        "teacherId": 123,
        "sessionIds": [1, 2, 3] // Optional - null = full assignment
      }
      ```

      **Validation:**
      - Class must be in DRAFT status
      - Teacher must exist and have required skills (or GENERAL skill)
      - Sessions must exist and belong to the class
      - Sessions must not already have teachers assigned

      **Performance:** Target <50ms for 36 sessions (bulk INSERT)

      **Response Structure:**
      - Success count and assigned session IDs
      - needsSubstitute flag (true if partial assignment)
      - remainingSessions count and IDs
      - Processing time in milliseconds

      **Example Use Cases:**
      - Main teacher for entire class (full assignment)
      - Substitute teacher for specific weeks (partial assignment)
      - Split teaching between multiple teachers (partial assignment)
      """)
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Teacher assigned successfully (full or partial)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseObject.class), examples = {
          @ExampleObject(name = "Full Assignment", description = "Teacher assigned to all sessions", value = """
              {
                "success": true,
                "message": "Teacher assigned to all 36 sessions successfully in 42ms",
                "data": {
                  "assignedCount": 36,
                  "assignedSessionIds": [1, 2, 3, 4, 5, "..."],
                  "needsSubstitute": false,
                  "remainingSessions": 0,
                  "remainingSessionIds": [],
                  "processingTimeMs": 42
                }
              }
              """),
          @ExampleObject(name = "Partial Assignment", description = "Teacher assigned to specific sessions only", value = """
              {
                "success": true,
                "message": "Teacher assigned to 12 sessions. 24 sessions still need assignment (38ms)",
                "data": {
                  "assignedCount": 12,
                  "assignedSessionIds": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12],
                  "needsSubstitute": true,
                  "remainingSessions": 24,
                  "remainingSessionIds": [13, 14, 15, "..."],
                  "processingTimeMs": 38
                }
              }
              """)
      })),
      @ApiResponse(responseCode = "404", description = "Class or Teacher not found", content = @Content(mediaType = "application/json", examples = {
          @ExampleObject(name = "Class Not Found", value = """
              {
                "success": false,
                "message": "Class with ID 999 not found",
                "data": null
              }
              """),
          @ExampleObject(name = "Teacher Not Found", value = """
              {
                "success": false,
                "message": "Teacher with ID 123 not found",
                "data": null
              }
              """)
      })),
      @ApiResponse(responseCode = "400", description = "Bad request - Validation error or business logic error", content = @Content(mediaType = "application/json", examples = {
          @ExampleObject(name = "Validation Error", description = "Request body validation failed", value = """
              {
                "success": false,
                "message": "Validation failed",
                "data": {
                  "teacherId": "must not be null"
                }
              }
              """),
          @ExampleObject(name = "Invalid Status", description = "Class not in DRAFT status", value = """
              {
                "success": false,
                "message": "Cannot assign teacher: class status must be DRAFT",
                "data": null
              }
              """),
          @ExampleObject(name = "Skill Mismatch", description = "Teacher does not have required skills", value = """
              {
                "success": false,
                "message": "Teacher does not have required skills for this class",
                "data": null
              }
              """),
          @ExampleObject(name = "Session Already Assigned", description = "Some sessions already have teachers", value = """
              {
                "success": false,
                "message": "Session 5 already has a teacher assigned",
                "data": null
              }
              """)
      })),
      @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
          {
            "success": false,
            "message": "Invalid username or password",
            "data": null
          }
          """))),
      @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions (requires ACADEMIC_AFFAIR role)", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
          {
            "success": false,
            "message": "Access denied - insufficient permissions",
            "data": null
          }
          """)))
  })
  public ResponseEntity<ResponseObject<AssignTeacherResponse>> assignTeacher(
      @Parameter(description = "Class ID") @PathVariable Long classId,

      @Parameter(description = "Teacher assignment request (teacherId + optional sessionIds)") @RequestBody @Valid AssignTeacherRequest request,

      @AuthenticationPrincipal UserPrincipal currentUser) {
    log.info("User {} assigning teacher {} to class {}", currentUser.getId(), request.getTeacherId(), classId);

    AssignTeacherResponse response = classService.assignTeacher(classId, request, currentUser.getId());

    // Build success message based on assignment mode
    String message;
    if (response.getNeedsSubstitute()) {
      message = String.format("Teacher assigned to %d sessions. %d sessions still need assignment (%dms)",
          response.getAssignedCount(), response.getRemainingSessions(),
          response.getProcessingTimeMs());
    } else {
      message = String.format("Teacher assigned to all %d sessions successfully in %dms",
          response.getAssignedCount(), response.getProcessingTimeMs());
    }

    return ResponseEntity.ok(ResponseObject.<AssignTeacherResponse>builder()
        .success(response.getAssignedCount() > 0)
        .message(message)
        .data(response)
        .build());
  }

  /**
   * STEP 6: Validate class completeness before submission
   */
  @PostMapping("/{classId}/validate")
  @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
  @Operation(summary = "Validate class completeness", description = "Validate that all required assignments are completed before submission. "
      +
      "This is STEP 6 of the Create Class workflow. Checks timeslot, resource, and teacher assignments.")
  public ResponseEntity<ResponseObject<ValidateClassResponse>> validateClass(
      @Parameter(description = "Class ID") @PathVariable Long classId,

      @AuthenticationPrincipal UserPrincipal currentUser) {
    log.info("User {} validating class {}", currentUser.getId(), classId);

    ValidateClassResponse response = classService.validateClass(classId, currentUser.getId());

    String message = validateClassResponseUtil.canSubmit(response) ? "Class is complete and ready for submission"
        : response.getMessage();

    return ResponseEntity.ok(ResponseObject.<ValidateClassResponse>builder()
        .success(validateClassResponseUtil.isValid(response))
        .message(message)
        .data(response)
        .build());
  }

  /**
   * STEP 7: Submit class for approval
   */
  @PostMapping("/{classId}/submit")
  @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
  @Operation(summary = "Submit class for approval", description = "Submit class for Center Head approval. This is STEP 7 of the Create Class workflow. "
      +
      "Class must be validated before submission. After submission, Center Head can approve or reject.")
  public ResponseEntity<ResponseObject<SubmitClassResponse>> submitClass(
      @Parameter(description = "Class ID") @PathVariable Long classId,

      @AuthenticationPrincipal UserPrincipal currentUser) {
    log.info("User {} submitting class {} for approval", currentUser.getId(), classId);

    SubmitClassResponse response = classService.submitClass(classId, currentUser.getId());

    return ResponseEntity.ok(ResponseObject.<SubmitClassResponse>builder()
        .success(response.isSuccess())
        .message(response.getMessage())
        .data(response)
        .build());
  }

  /**
   * STEP 7: Approve submitted class (Center Head only)
   */
  @PostMapping("/{classId}/approve")
  @PreAuthorize("hasRole('CENTER_HEAD')")
  @Operation(summary = "Approve class", description = "Approve submitted class and change status to SCHEDULED. This is STEP 7 of the workflow. "
      +
      "Only users with CENTER_HEAD role can approve classes.")
  public ResponseEntity<ResponseObject<String>> approveClass(
      @Parameter(description = "Class ID") @PathVariable Long classId,

      @AuthenticationPrincipal UserPrincipal currentUser) {
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
  @Operation(summary = "Reject class", description = "Reject submitted class and provide rejection reason. This is STEP 7 of the workflow. "
      +
      "Only users with CENTER_HEAD role can reject classes. Class status resets to DRAFT.")
  public ResponseEntity<ResponseObject<RejectClassResponse>> rejectClass(
      @Parameter(description = "Class ID") @PathVariable Long classId,

      @Parameter(description = "Rejection reason") @RequestBody RejectClassRequest request,

      @AuthenticationPrincipal UserPrincipal currentUser) {
    log.info("Center Head {} rejecting class {} with reason: {}", currentUser.getId(), classId, request.getReason());

    RejectClassResponse response = classService.rejectClass(classId, request.getReason(), currentUser.getId());

    return ResponseEntity.ok(ResponseObject.<RejectClassResponse>builder()
        .success(response.isSuccess())
        .message(response.getMessage())
        .data(response)
        .build());
  }

  /**
   * DELETE a DRAFT class
   * Only classes in DRAFT status can be deleted
   */
  @DeleteMapping("/{classId}")
  @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
  @Operation(summary = "Delete DRAFT class", description = "Delete a class that is in DRAFT status. " +
      "Only classes that have not been submitted for approval can be deleted. " +
      "Cascades delete to sessions, teaching slots, and session resources.")
  public ResponseEntity<ResponseObject<String>> deleteClass(
      @Parameter(description = "Class ID") @PathVariable Long classId,

      @AuthenticationPrincipal UserPrincipal currentUser) {
    log.info("User {} deleting class {}", currentUser.getId(), classId);

    classService.deleteClass(classId, currentUser.getId());

    return ResponseEntity.ok(ResponseObject.<String>builder()
        .success(true)
        .message("Class deleted successfully")
        .data("Class and all related data removed")
        .build());
  }
}
