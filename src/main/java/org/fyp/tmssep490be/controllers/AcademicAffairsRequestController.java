package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.schedule.WeeklyScheduleResponseDTO;
import org.fyp.tmssep490be.dtos.studentrequest.*;
import org.fyp.tmssep490be.entities.Student;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.StudentRepository;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.StudentRequestService;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/academic-requests")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Academic Affairs Request Management", description = "APIs for Academic Affairs staff to manage student requests")
@SecurityRequirement(name = "Bearer Authentication")
public class AcademicAffairsRequestController {

    private final StudentRequestService studentRequestService;
    private final StudentRepository studentRepository;

    @GetMapping("/pending")
    @Operation(summary = "Get pending requests for review", description = "Retrieve all pending requests that need Academic Affairs review with filtering and pagination")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<PagedAARequestResponseDTO>> getPendingRequests(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Filter by branch ID")
            @RequestParam(required = false) Long branchId,
            @Parameter(description = "Filter by request type: ABSENCE, MAKEUP, TRANSFER")
            @RequestParam(required = false) String requestType,
            @Parameter(description = "Search by student name or student code")
            @RequestParam(required = false) String studentName,
            @Parameter(description = "Search by class code")
            @RequestParam(required = false) String classCode,
            @Parameter(description = "Filter session date from (YYYY-MM-DD)")
            @RequestParam(required = false) String sessionDateFrom,
            @Parameter(description = "Filter session date to (YYYY-MM-DD)")
            @RequestParam(required = false) String sessionDateTo,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "Sort criteria: field,direction (e.g., submittedAt,asc)")
            @RequestParam(defaultValue = "submittedAt,asc") String sort) {

        AARequestFilterDTO filter = AARequestFilterDTO.builder()
                .branchId(branchId)
                .requestType(requestType)
                .studentName(studentName)
                .classCode(classCode)
                .sessionDateFrom(sessionDateFrom)
                .sessionDateTo(sessionDateTo)
                .page(page)
                .size(size)
                .sort(sort)
                .build();

        Page<AARequestResponseDTO> requests = studentRequestService.getPendingRequests(filter);
        RequestSummaryDTO summary = studentRequestService.getRequestSummary(filter);

        PagedAARequestResponseDTO response = PagedAARequestResponseDTO.builder()
                .content(requests.getContent())
                .pageable(requests.getPageable())
                .totalElements(requests.getTotalElements())
                .totalPages(requests.getTotalPages())
                .first(requests.isFirst())
                .last(requests.isLast())
                .summary(summary)
                .build();

        return ResponseEntity.ok(ResponseObject.success("Retrieved pending requests successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get all requests history", description = "Retrieve all requests (approved/rejected/cancelled) with comprehensive filtering")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<Page<AARequestResponseDTO>>> getAllRequests(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Filter by branch ID")
            @RequestParam(required = false) Long branchId,
            @Parameter(description = "Filter by status: PENDING, APPROVED, REJECTED, CANCELLED")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by request type: ABSENCE, MAKEUP, TRANSFER")
            @RequestParam(required = false) String requestType,
            @Parameter(description = "Search by student name or student code")
            @RequestParam(required = false) String studentName,
            @Parameter(description = "Search by class code")
            @RequestParam(required = false) String classCode,
            @Parameter(description = "Filter by who decided (user ID)")
            @RequestParam(required = false) Long decidedBy,
            @Parameter(description = "Filter session date from (YYYY-MM-DD)")
            @RequestParam(required = false) String sessionDateFrom,
            @Parameter(description = "Filter session date to (YYYY-MM-DD)")
            @RequestParam(required = false) String sessionDateTo,
            @Parameter(description = "Filter submitted date from (YYYY-MM-DD)")
            @RequestParam(required = false) String submittedDateFrom,
            @Parameter(description = "Filter submitted date to (YYYY-MM-DD)")
            @RequestParam(required = false) String submittedDateTo,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "Sort criteria: field,direction (e.g., submittedAt,desc)")
            @RequestParam(defaultValue = "submittedAt,desc") String sort) {

        AARequestFilterDTO filter = AARequestFilterDTO.builder()
                .branchId(branchId)
                .status(status)
                .requestType(requestType)
                .studentName(studentName)
                .classCode(classCode)
                .decidedBy(decidedBy)
                .sessionDateFrom(sessionDateFrom)
                .sessionDateTo(sessionDateTo)
                .submittedDateFrom(submittedDateFrom)
                .submittedDateTo(submittedDateTo)
                .page(page)
                .size(size)
                .sort(sort)
                .build();

        Page<AARequestResponseDTO> requests = studentRequestService.getAllRequests(filter);

        return ResponseEntity.ok(ResponseObject.success("Retrieved all requests successfully", requests));
    }

    @GetMapping("/{requestId}")
    @Operation(summary = "Get request details for review", description = "Retrieve detailed information about a specific request for Academic Affairs review")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<StudentRequestDetailDTO>> getRequestDetails(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Request ID") @PathVariable Long requestId) {

        StudentRequestDetailDTO request = studentRequestService.getRequestDetailsForAA(requestId);

        return ResponseEntity.ok(ResponseObject.success("Retrieved request details successfully", request));
    }

    @PutMapping("/{requestId}/approve")
    @Operation(summary = "Approve request", description = "Approve a pending student request")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<StudentRequestResponseDTO>> approveRequest(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Request ID") @PathVariable Long requestId,
            @Valid @RequestBody ApprovalDTO approvalDTO) {

        Long decidedById = currentUser.getId();
        StudentRequestResponseDTO request = studentRequestService.approveRequest(requestId, decidedById, approvalDTO);

        return ResponseEntity.ok(ResponseObject.success("Request approved successfully", request));
    }

    @PutMapping("/{requestId}/reject")
    @Operation(summary = "Reject request", description = "Reject a pending student request")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<StudentRequestResponseDTO>> rejectRequest(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Request ID") @PathVariable Long requestId,
            @Valid @RequestBody RejectionDTO rejectionDTO) {

        Long decidedById = currentUser.getId();
        StudentRequestResponseDTO request = studentRequestService.rejectRequest(requestId, decidedById, rejectionDTO);

        return ResponseEntity.ok(ResponseObject.success("Request rejected successfully", request));
    }

    // DTO for combining paginated results with summary
    public static class PagedAARequestResponseDTO {
        private Object content;
        private Object pageable;
        private long totalElements;
        private int totalPages;
        private boolean first;
        private boolean last;
        private RequestSummaryDTO summary;

        // Getters and setters
        public Object getContent() { return content; }
        public void setContent(Object content) { this.content = content; }
        public Object getPageable() { return pageable; }
        public void setPageable(Object pageable) { this.pageable = pageable; }
        public long getTotalElements() { return totalElements; }
        public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
        public boolean isFirst() { return first; }
        public void setFirst(boolean first) { this.first = first; }
        public boolean isLast() { return last; }
        public void setLast(boolean last) { this.last = last; }
        public RequestSummaryDTO getSummary() { return summary; }
        public void setSummary(RequestSummaryDTO summary) { this.summary = summary; }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private PagedAARequestResponseDTO instance = new PagedAARequestResponseDTO();

            public Builder content(Object content) {
                instance.setContent(content);
                return this;
            }

            public Builder pageable(Object pageable) {
                instance.setPageable(pageable);
                return this;
            }

            public Builder totalElements(long totalElements) {
                instance.setTotalElements(totalElements);
                return this;
            }

            public Builder totalPages(int totalPages) {
                instance.setTotalPages(totalPages);
                return this;
            }

            public Builder first(boolean first) {
                instance.setFirst(first);
                return this;
            }

            public Builder last(boolean last) {
                instance.setLast(last);
                return this;
            }

            public Builder summary(RequestSummaryDTO summary) {
                instance.setSummary(summary);
                return this;
            }

            public PagedAARequestResponseDTO build() {
                return instance;
            }
        }
    }

    // ==================== STUDENT SCHEDULE FOR AA ENDPOINTS ====================

    @GetMapping("/students/{studentId}/schedule")
    @Operation(
        summary = "Get student weekly schedule (AA)",
        description = "Get weekly schedule for a specific student. Used by AA for absence request workflow. " +
                      "Can filter by specific class if classId is provided."
    )
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<WeeklyScheduleResponseDTO>> getStudentWeeklySchedule(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Student ID", required = true)
            @PathVariable Long studentId,
            @Parameter(
                description = "Monday of the target week in ISO 8601 format (YYYY-MM-DD). " +
                        "If not provided, defaults to current week Monday.",
                example = "2025-11-10"
            )
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate weekStart,
            @Parameter(description = "Filter by specific class ID (optional). If provided, only returns sessions for this class.")
            @RequestParam(required = false) Long classId) {

        log.info("AA user {} requesting weekly schedule for student: {}, class: {}, week: {}",
                currentUser.getId(), studentId, classId, weekStart);

        // 1. Default to current week if not specified
        if (weekStart == null) {
            weekStart = studentRequestService.getCurrentWeekStart();
            log.debug("Using current week start: {}", weekStart);
        }

        // 2. Fetch schedule (with optional class filter)
        WeeklyScheduleResponseDTO schedule;
        if (classId != null) {
            schedule = studentRequestService.getWeeklyScheduleByClass(studentId, classId, weekStart);
        } else {
            schedule = studentRequestService.getWeeklySchedule(studentId, weekStart);
        }

        return ResponseEntity.ok(ResponseObject.success("Retrieved student weekly schedule successfully", schedule));
    }

    // ==================== ABSENCE REQUEST ON-BEHALF ENDPOINTS ====================

    @PostMapping("/absence/on-behalf")
    @Operation(
        summary = "Submit absence request on behalf of student",
        description = "Academic Affairs submits an absence request on behalf of a student. " +
                      "The request is auto-approved and the student session is automatically marked as EXCUSED."
    )
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<StudentRequestResponseDTO>> submitAbsenceRequestOnBehalf(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody AbsenceRequestDTO absenceRequest) {

        log.info("AA user {} submitting absence request on-behalf for student {} session {}",
                currentUser.getId(), absenceRequest.getStudentId(), absenceRequest.getTargetSessionId());

        Long decidedById = currentUser.getId();
        StudentRequestResponseDTO response = studentRequestService.submitAbsenceRequestOnBehalf(decidedById, absenceRequest);

        return ResponseEntity.ok(ResponseObject.success("Absence request created and auto-approved", response));
    }

    // ==================== MAKEUP REQUEST ON-BEHALF ENDPOINTS ====================

    @GetMapping("/students/{studentId}/missed-sessions")
    @Operation(summary = "Get missed sessions for student (AA)", description = "Get all missed sessions for a specific student (for AA to create makeup requests on behalf)")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<MissedSessionsResponseDTO>> getMissedSessionsForStudent(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Student ID", required = true)
            @PathVariable Long studentId,
            @Parameter(description = "Number of weeks to look back (default: 4)")
            @RequestParam(required = false, defaultValue = "4") Integer weeksBack) {

        MissedSessionsResponseDTO result = studentRequestService.getMissedSessionsForStudent(studentId, weeksBack);

        return ResponseEntity.ok(ResponseObject.success("Retrieved missed sessions successfully", result));
    }

    @GetMapping("/makeup-options")
    @Operation(summary = "Get makeup options for student (AA)", description = "Get available makeup sessions for a specific missed session (for AA to create makeup requests on behalf)")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<MakeupOptionsResponseDTO>> getMakeupOptionsForStudent(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Target session ID", required = true)
            @RequestParam Long targetSessionId,
            @Parameter(description = "Student ID", required = true)
            @RequestParam Long studentId) {

        MakeupOptionsResponseDTO result = studentRequestService.getMakeupOptionsForStudent(targetSessionId, studentId);

        return ResponseEntity.ok(ResponseObject.success("Retrieved makeup options successfully", result));
    }

    @PostMapping("/on-behalf")
    @Operation(summary = "Submit request on behalf of student", description = "Academic Affairs submits a makeup request on behalf of a student (auto-approved)")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<StudentRequestResponseDTO>> submitRequestOnBehalf(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody MakeupRequestDTO makeupRequest) {

        Long decidedById = currentUser.getId();
        StudentRequestResponseDTO response = studentRequestService.submitMakeupRequestOnBehalf(decidedById, makeupRequest);

        return ResponseEntity.ok(ResponseObject.success("Makeup request created and auto-approved", response));
    }

    // ==================== TRANSFER REQUEST ON-BEHALF ENDPOINTS ====================

    @GetMapping("/students/{studentId}/transfer-eligibility")
    @Operation(
        summary = "Get student transfer eligibility (AA)",
        description = "Check if student is eligible for class transfer with quota and policy info"
    )
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<TransferEligibilityDTO>> getStudentTransferEligibility(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Student ID", required = true)
            @PathVariable Long studentId) {

        log.info("AA user {} checking transfer eligibility for student {}", currentUser.getId(), studentId);

        // Get student's user account ID
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + studentId));

        Long userId = student.getUserAccount().getId();

        // Reuse existing service logic
        TransferEligibilityDTO result = studentRequestService.getTransferEligibility(userId);

        return ResponseEntity.ok(ResponseObject.success("Retrieved transfer eligibility successfully", result));
    }

    @PostMapping("/transfer/on-behalf")
    @Operation(summary = "Submit transfer request on behalf of student", description = "Academic Affairs submits a transfer request on behalf of a student (auto-approved)")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<StudentRequestResponseDTO>> submitTransferRequestOnBehalf(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody TransferRequestDTO transferRequest) {

        Long decidedById = currentUser.getId();
        StudentRequestResponseDTO response = studentRequestService.submitTransferRequestOnBehalf(decidedById, transferRequest);

        return ResponseEntity.ok(ResponseObject.success("Transfer request created and auto-approved", response));
    }

    @GetMapping("/transfer-options")
    @Operation(
        summary = "Get flexible transfer options (AA)",
        description = "Get transfer options with flexible branch/modality filtering for Academic Affairs. " +
                      "Supports: schedule-only change, branch change, modality change, or combined changes."
    )
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<TransferOptionsResponseDTO>> getFlexibleTransferOptions(
            @AuthenticationPrincipal UserPrincipal currentUser,

            @Parameter(description = "Current class ID", required = true)
            @RequestParam Long currentClassId,

            @Parameter(description = "Target branch ID (optional - for branch change)")
            @RequestParam(required = false) Long targetBranchId,

            @Parameter(description = "Target modality (optional - for modality change): OFFLINE, ONLINE, HYBRID")
            @RequestParam(required = false) String targetModality,

            @Parameter(description = "Schedule change only (same branch & modality, different time slot)")
            @RequestParam(required = false) Boolean scheduleOnly) {

        log.info("AA user {} getting flexible transfer options for class {} (targetBranch: {}, targetModality: {}, scheduleOnly: {})",
                currentUser.getId(), currentClassId, targetBranchId, targetModality, scheduleOnly);

        TransferOptionsResponseDTO result = studentRequestService.getTransferOptionsFlexible(
                currentClassId, targetBranchId, targetModality, scheduleOnly);

        return ResponseEntity.ok(ResponseObject.success("Retrieved transfer options successfully", result));
    }
}