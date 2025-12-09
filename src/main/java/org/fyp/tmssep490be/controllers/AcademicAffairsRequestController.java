package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.studentrequest.*;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.StudentRequestService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/academic-requests")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Academic Affairs Request Management", description = "APIs for Academic Affairs staff to manage student requests")
@SecurityRequirement(name = "Bearer Authentication")
public class AcademicAffairsRequestController {

    private final StudentRequestService studentRequestService;

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<PagedAARequestResponseDTO>> getPendingRequests(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String requestType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sessionDateFrom,
            @RequestParam(required = false) String sessionDateTo,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "submittedAt,asc") String sort) {

        AARequestFilterDTO filter = AARequestFilterDTO.builder()
                .branchId(branchId)
                .requestType(requestType)
                .keyword(keyword)
                .sessionDateFrom(sessionDateFrom)
                .sessionDateTo(sessionDateTo)
                .page(page)
                .size(size)
                .sort(sort)
                .build();

        Page<AARequestResponseDTO> requests = studentRequestService.getPendingRequests(currentUser.getId(), filter);
        RequestSummaryDTO summary = studentRequestService.getRequestSummary(currentUser.getId(), filter);

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
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<Page<AARequestResponseDTO>>> getAllRequests(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String requestType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long decidedBy,
            @RequestParam(required = false) String sessionDateFrom,
            @RequestParam(required = false) String sessionDateTo,
            @RequestParam(required = false) String submittedDateFrom,
            @RequestParam(required = false) String submittedDateTo,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "submittedAt,desc") String sort) {

        AARequestFilterDTO filter = AARequestFilterDTO.builder()
                .branchId(branchId)
                .status(status)
                .requestType(requestType)
                .keyword(keyword)
                .decidedBy(decidedBy)
                .sessionDateFrom(sessionDateFrom)
                .sessionDateTo(sessionDateTo)
                .submittedDateFrom(submittedDateFrom)
                .submittedDateTo(submittedDateTo)
                .page(page)
                .size(size)
                .sort(sort)
                .build();

        Page<AARequestResponseDTO> requests = studentRequestService.getAllRequests(currentUser.getId(), filter);

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

    @GetMapping("/students/{studentId}/missed-sessions")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    @Operation(summary = "Get student missed sessions (AA)", description = "Get missed sessions for a specific student for on-behalf makeup request creation")
    public ResponseEntity<ResponseObject<MissedSessionsResponseDTO>> getStudentMissedSessions(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Student ID", required = true) @PathVariable Long studentId,
            @Parameter(description = "Number of weeks to look back (default: 4)")
            @RequestParam(required = false) Integer weeksBack,
            @Parameter(description = "Exclude sessions with existing makeup requests (default: true)")
            @RequestParam(required = false) Boolean excludeRequested) {

        MissedSessionsResponseDTO response = studentRequestService.getMissedSessionsForStudent(
                studentId, weeksBack, excludeRequested);

        return ResponseEntity.ok(ResponseObject.success("Retrieved missed sessions successfully", response));
    }

    @GetMapping("/makeup-options")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    @Operation(summary = "Get makeup options for student (AA)", description = "Get available makeup sessions for a specific missed session (for AA to create makeup requests on behalf)")
    public ResponseEntity<ResponseObject<MakeupOptionsResponseDTO>> getMakeupOptionsForStudent(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Student ID", required = true) @RequestParam Long studentId,
            @Parameter(description = "Target session ID (the missed session to makeup)", required = true)
            @RequestParam Long targetSessionId) {

        MakeupOptionsResponseDTO response = studentRequestService.getMakeupOptionsForStudent(
                studentId, targetSessionId);

        return ResponseEntity.ok(ResponseObject.success("Retrieved makeup options successfully", response));
    }

    @PostMapping("/makeup-requests/on-behalf")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    @Operation(summary = "Submit makeup request on-behalf", description = "Create makeup request for student (auto-approved)")
    public ResponseEntity<ResponseObject<StudentRequestResponseDTO>> submitMakeupOnBehalf(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody MakeupRequestDTO dto) {

        StudentRequestResponseDTO response = studentRequestService.submitMakeupRequestOnBehalf(
                currentUser.getId(), dto);

        return ResponseEntity.ok(ResponseObject.success("Makeup request created and auto-approved", response));
    }
}
