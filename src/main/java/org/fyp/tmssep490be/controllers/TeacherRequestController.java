package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.teacherrequest.*;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.TeacherRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for Teacher Request management
 * Supports: SWAP, RESCHEDULE, MODALITY_CHANGE
 */
@RestController
@RequestMapping("/api/v1/teacher-requests")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Teacher Request", description = "Teacher request management APIs (Swap, Reschedule, Modality Change)")
@SecurityRequirement(name = "Bearer Authentication")
public class TeacherRequestController {

    private final TeacherRequestService teacherRequestService;
    private static final String ROLE_ACADEMIC_AFFAIR = "ROLE_ACADEMIC_AFFAIR";

    /**
     * Create a new teacher request
     * POST /api/v1/teacher-requests
     */
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(
            summary = "Create teacher request",
            description = "Create a new request (SWAP, RESCHEDULE, or MODALITY_CHANGE)"
    )
    public ResponseEntity<ResponseObject<TeacherRequestResponseDTO>> createRequest(
            @RequestBody @Valid TeacherRequestCreateDTO createDTO,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("Create request type {} for session {} by user {}", 
                createDTO.getRequestType(), createDTO.getSessionId(), currentUser.getId());

        TeacherRequestResponseDTO response = teacherRequestService.createRequest(createDTO, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<TeacherRequestResponseDTO>builder()
                .success(true)
                .message("Request created successfully")
                .data(response)
                .build());
    }

    /**
     * Get pending teacher requests for Academic Affairs staff
     * GET /api/v1/teacher-requests/staff/pending
     */
    @GetMapping("/staff/pending")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    @Operation(
            summary = "Staff - Get pending teacher requests",
            description = "List pending teacher requests waiting for Academic Affairs review"
    )
    public ResponseEntity<ResponseObject<List<TeacherRequestListDTO>>> getPendingRequestsForStaff() {
        List<TeacherRequestListDTO> requests = teacherRequestService.getPendingRequestsForStaff();

        return ResponseEntity.ok(ResponseObject.<List<TeacherRequestListDTO>>builder()
                .success(true)
                .message("Pending teacher requests loaded successfully")
                .data(requests)
                .build());
    }

    /**
     * Get teacher requests for Academic Affairs staff with optional status filter
     * GET /api/v1/teacher-requests/staff
     */
    @GetMapping("/staff")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    @Operation(
            summary = "Staff - Get teacher requests",
            description = "List teacher requests with optional status filter (PENDING/APPROVED/REJECTED)"
    )
    public ResponseEntity<ResponseObject<List<TeacherRequestListDTO>>> getRequestsForStaff(
            @RequestParam(value = "status", required = false) RequestStatus status
    ) {
        List<TeacherRequestListDTO> requests = teacherRequestService.getRequestsForStaff(status);

        return ResponseEntity.ok(ResponseObject.<List<TeacherRequestListDTO>>builder()
                .success(true)
                .message("Teacher requests loaded successfully")
                .data(requests)
                .build());
    }

    /**
     * Suggest time slots (RESCHEDULE)
     * For TEACHER only: uses sessionId to suggest slots for a session
     */
    @GetMapping("/{id}/reschedule/slots")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(
            summary = "Suggest time slots for reschedule",
            description = "List time slots without conflicts on the selected date for a session. " +
                    "id is sessionId, date is required."
    )
    public ResponseEntity<ResponseObject<List<RescheduleSlotSuggestionDTO>>> suggestSlots(
            @PathVariable Long id,
            @RequestParam(value = "date", required = true) java.time.LocalDate date,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        // Teacher: id is sessionId, date is required
        List<RescheduleSlotSuggestionDTO> items = teacherRequestService.suggestSlots(id, date, currentUser.getId());
        return ResponseEntity.ok(ResponseObject.<List<RescheduleSlotSuggestionDTO>>builder()
                .success(true)
                .message(items.isEmpty() ? "No suitable time slots for the selected date" : "OK")
                .data(items)
                .build());
    }

    /**
     * Suggest resources (RESCHEDULE)
     * For TEACHER: uses sessionId to suggest resources for a session
     * For ACADEMIC_AFFAIR: uses requestId to suggest resources for a request (to override reschedule)
     */
    @GetMapping("/{id}/reschedule/suggestions")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ACADEMIC_AFFAIR')")
    @Operation(
            summary = "Suggest resources for reschedule",
            description = "For TEACHER: List resources without conflicts for date + time slot for a session. " +
                    "For ACADEMIC_AFFAIR: List resources without conflicts for date + time slot for a reschedule request. " +
                    "If date/timeSlotId are not provided for staff, uses the date and timeSlot from the request."
    )
    public ResponseEntity<ResponseObject<List<RescheduleResourceSuggestionDTO>>> suggestResources(
            @PathVariable Long id,
            @RequestParam(value = "date", required = false) java.time.LocalDate date,
            @RequestParam(value = "timeSlotId", required = false) Long timeSlotId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        boolean isAcademicStaff = isAcademicAffair(currentUser);
        
        if (isAcademicStaff) {
            // Staff: id is requestId, date and timeSlotId are optional (if not provided, uses from request)
            List<RescheduleResourceSuggestionDTO> items = teacherRequestService.suggestResourcesForStaff(id, date, timeSlotId);
            return ResponseEntity.ok(ResponseObject.<List<RescheduleResourceSuggestionDTO>>builder()
                    .success(true)
                    .message(items.isEmpty() ? "No suitable resources for the selected slot" : "OK")
                    .data(items)
                    .build());
        } else {
            // Teacher: id is sessionId, date and timeSlotId are required
            if (date == null || timeSlotId == null) {
                throw new IllegalArgumentException("Date and timeSlotId parameters are required for teachers");
            }
            List<RescheduleResourceSuggestionDTO> items = teacherRequestService.suggestResources(id, date, timeSlotId, currentUser.getId());
            return ResponseEntity.ok(ResponseObject.<List<RescheduleResourceSuggestionDTO>>builder()
                    .success(true)
                    .message(items.isEmpty() ? "No suitable resources for the selected slot" : "OK")
                    .data(items)
                    .build());
        }
    }

    /**
     * Suggest resources for modality change
     * For TEACHER: uses sessionId to suggest resources for a session
     * For ACADEMIC_AFFAIR: uses requestId to suggest resources for a request (to override modality change)
     */
    @GetMapping("/{id}/modality/resources")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ACADEMIC_AFFAIR')")
    @Operation(
            summary = "Suggest resources for modality change",
            description = "For TEACHER: List compatible resources for the session's existing schedule (date + time slot). " +
                    "For ACADEMIC_AFFAIR: List compatible resources for the modality change request's session schedule."
    )
    public ResponseEntity<ResponseObject<List<ModalityResourceSuggestionDTO>>> suggestModalityResources(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        boolean isAcademicStaff = isAcademicAffair(currentUser);
        
        if (isAcademicStaff) {
            // Staff: id is requestId
            List<ModalityResourceSuggestionDTO> suggestions = teacherRequestService.suggestModalityResourcesForStaff(id);
            return ResponseEntity.ok(ResponseObject.<List<ModalityResourceSuggestionDTO>>builder()
                    .success(true)
                    .message(suggestions.isEmpty() ? "No compatible resources available" : "Modality resources loaded successfully")
                    .data(suggestions)
                    .build());
        } else {
            // Teacher: id is sessionId
            List<ModalityResourceSuggestionDTO> suggestions = teacherRequestService.suggestModalityResources(id, currentUser.getId());
            return ResponseEntity.ok(ResponseObject.<List<ModalityResourceSuggestionDTO>>builder()
                    .success(true)
                    .message(suggestions.isEmpty() ? "No compatible resources available" : "Modality resources loaded successfully")
                    .data(suggestions)
                    .build());
        }
    }

    /**
     * Suggest swap candidate teachers (SWAP)
     * For TEACHER: uses sessionId to suggest candidates for a session
     * For ACADEMIC_AFFAIR: uses requestId to suggest candidates for a request (to override replacement teacher)
     */
    @GetMapping("/{id}/swap/candidates")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ACADEMIC_AFFAIR')")
    @Operation(
            summary = "Suggest swap candidates",
            description = "For TEACHER: List teachers who can replace the current teacher for the session. " +
                    "For ACADEMIC_AFFAIR: List teachers who can replace the original teacher for the swap request. " +
                    "Sorted by skill priority, availability priority, and name."
    )
    public ResponseEntity<ResponseObject<List<SwapCandidateDTO>>> suggestSwapCandidates(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        boolean isAcademicStaff = isAcademicAffair(currentUser);
        
        if (isAcademicStaff) {
            // Staff: id is requestId
            log.info("Suggest swap candidates for request {} by staff", id);
            List<SwapCandidateDTO> candidates = teacherRequestService.suggestSwapCandidatesForStaff(id);
            return ResponseEntity.ok(ResponseObject.<List<SwapCandidateDTO>>builder()
                    .success(true)
                    .message(candidates.isEmpty() ? "No suitable replacement teachers found" : "Swap candidates loaded successfully")
                    .data(candidates)
                    .build());
        } else {
            // Teacher: id is sessionId
            log.info("Suggest swap candidates for session {} by user {}", id, currentUser.getId());
            List<SwapCandidateDTO> candidates = teacherRequestService.suggestSwapCandidates(id, currentUser.getId());
            return ResponseEntity.ok(ResponseObject.<List<SwapCandidateDTO>>builder()
                    .success(true)
                    .message(candidates.isEmpty() ? "No suitable replacement teachers found" : "Swap candidates loaded successfully")
                    .data(candidates)
                    .build());
        }
    }

    /**
     * Get all requests for current teacher
     * GET /api/v1/teacher-requests/me
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(
            summary = "Get my requests",
            description = "Get all requests for the current teacher"
    )
    public ResponseEntity<ResponseObject<List<TeacherRequestListDTO>>> getMyRequests(
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("Get requests for user {}", currentUser.getId());

        List<TeacherRequestListDTO> requests = teacherRequestService.getMyRequests(currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<List<TeacherRequestListDTO>>builder()
                .success(true)
                .message("Requests loaded successfully")
                .data(requests)
                .build());
    }

    /**
     * Get my future sessions (7 days) or filter by specific date
     * GET /api/v1/teacher-requests/my-sessions
     */
    @GetMapping("/my-sessions")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ROLE_ACADEMIC_AFFAIR')")
    @Operation(
            summary = "Get my future sessions",
            description = "Get teacher's future sessions within 7 days (or filter by specific date). " +
                    "Returns sessions with status PLANNED that teacher is assigned to teach."
    )
    public ResponseEntity<ResponseObject<List<TeacherSessionDTO>>> getMySessions(
            @RequestParam(value = "date", required = false) java.time.LocalDate date,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("Get my sessions for user {} with date filter {}", currentUser.getId(), date);

        List<TeacherSessionDTO> sessions = teacherRequestService.getMySessions(currentUser.getId(), date);

        return ResponseEntity.ok(ResponseObject.<List<TeacherSessionDTO>>builder()
                .success(true)
                .message("Sessions loaded successfully")
                .data(sessions)
                .build());
    }

    /**
     * Approve teacher request (Staff only)
     * PATCH /api/v1/teacher-requests/{id}/approve
     */
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ROLE_ACADEMIC_AFFAIR')")
    @Operation(
            summary = "Approve teacher request",
            description = "Approve a request. Staff can override Teacher's choices."
    )
    public ResponseEntity<ResponseObject<TeacherRequestResponseDTO>> approveRequest(
            @PathVariable Long id,
            @RequestBody(required = false) TeacherRequestApproveDTO approveDTO,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("Approve request {} by user {}", id, currentUser.getId());

        // Handle null body - create empty DTO
        if (approveDTO == null) {
            approveDTO = TeacherRequestApproveDTO.builder().build();
        }

        TeacherRequestResponseDTO response = teacherRequestService.approveRequest(id, approveDTO, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<TeacherRequestResponseDTO>builder()
                .success(true)
                .message("Request approved successfully")
                .data(response)
                .build());
    }

    /**
     * Reject teacher request (Staff only)
     * PATCH /api/v1/teacher-requests/{id}/reject
     */
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ROLE_ACADEMIC_AFFAIR')")
    @Operation(
            summary = "Reject teacher request",
            description = "Reject a request with reason"
    )
    public ResponseEntity<ResponseObject<TeacherRequestResponseDTO>> rejectRequest(
            @PathVariable Long id,
            @RequestBody @Valid TeacherRequestRejectDTO rejectDTO,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("Reject request {} by user {}", id, currentUser.getId());

        TeacherRequestResponseDTO response = teacherRequestService.rejectRequest(
                id, rejectDTO.getReason(), currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<TeacherRequestResponseDTO>builder()
                .success(true)
                .message("Request rejected")
                .data(response)
                .build());
    }

    /**
     * Confirm swap request (Replacement Teacher only)
     * PATCH /api/v1/teacher-requests/{id}/confirm
     */
    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(
            summary = "Confirm swap request",
            description = "Replacement teacher confirms to take over the session. " +
                    "Updates teaching slots: original teacher → ON_LEAVE, replacement teacher → SUBSTITUTED. " +
                    "Request status → APPROVED."
    )
    public ResponseEntity<ResponseObject<TeacherRequestResponseDTO>> confirmSwap(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("Confirm swap request {} by replacement teacher {}", id, currentUser.getId());

        TeacherRequestResponseDTO response = teacherRequestService.confirmSwap(id, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<TeacherRequestResponseDTO>builder()
                .success(true)
                .message("Swap request confirmed successfully")
                .data(response)
                .build());
    }

    /**
     * Decline swap request (Replacement Teacher only)
     * PATCH /api/v1/teacher-requests/{id}/decline
     */
    @PatchMapping("/{id}/decline")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(
            summary = "Decline swap request",
            description = "Replacement teacher declines to take over the session. " +
                    "Request status → PENDING, replacementTeacherId → null. " +
                    "Staff can approve again with a different replacement teacher."
    )
    public ResponseEntity<ResponseObject<TeacherRequestResponseDTO>> declineSwap(
            @PathVariable Long id,
            @RequestBody @Valid TeacherRequestRejectDTO declineDTO,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("Decline swap request {} by replacement teacher {}", id, currentUser.getId());

        TeacherRequestResponseDTO response = teacherRequestService.declineSwap(
                id, declineDTO.getReason(), currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<TeacherRequestResponseDTO>builder()
                .success(true)
                .message("Swap request declined")
                .data(response)
                .build());
    }

    /**
     * Get teacher request by ID
     * GET /api/v1/teacher-requests/{id}
     * Note: Must be after specific routes like /approve, /reject, /confirm, /decline
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ACADEMIC_AFFAIR')")
    @Operation(
            summary = "Get request by ID",
            description = "Get request details (Teacher sees own requests, Staff sees all)"
    )
    public ResponseEntity<ResponseObject<TeacherRequestResponseDTO>> getRequestById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("Get request {} for user {}", id, currentUser.getId());

        boolean isAcademicStaff = isAcademicAffair(currentUser);
        TeacherRequestResponseDTO response = isAcademicStaff
                ? teacherRequestService.getRequestForStaff(id)
                : teacherRequestService.getRequestById(id, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<TeacherRequestResponseDTO>builder()
                .success(true)
                .message("Request loaded successfully")
                .data(response)
                .build());
    }

    private boolean isAcademicAffair(UserPrincipal currentUser) {
        if (currentUser == null || currentUser.getAuthorities() == null) {
            return false;
        }
        return currentUser.getAuthorities().stream()
                .anyMatch(authority -> ROLE_ACADEMIC_AFFAIR.equals(authority.getAuthority()));
    }
}



