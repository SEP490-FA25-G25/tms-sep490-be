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
import org.fyp.tmssep490be.entities.Student;
import org.fyp.tmssep490be.entities.enums.StudentRequestType;
import org.fyp.tmssep490be.exceptions.BusinessRuleException;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.StudentRepository;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.StudentRequestService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/students-request")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Student Request Management", description = "APIs for students to manage their absence/makeup/transfer requests")
@SecurityRequirement(name = "Bearer Authentication")
public class StudentRequestController {

    private final StudentRequestService studentRequestService;
    private final StudentRepository studentRepository;

    @GetMapping("/requests")
    @Operation(summary = "Get student's requests", description = "Retrieve all requests submitted by the current student with pagination and filtering")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<Page<StudentRequestResponseDTO>>> getMyRequests(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Filter by request type: ABSENCE, MAKEUP, TRANSFER")
            @RequestParam(required = false) String requestType,
            @Parameter(description = "Filter by status: PENDING, APPROVED, REJECTED, CANCELLED")
            @RequestParam(required = false) String status,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "Sort criter......................................ia: field,direction (e.g., submittedAt,desc)")
            @RequestParam(defaultValue = "submittedAt,desc") String sort) {

        // Get student ID from user principal
        Long studentId = currentUser.getId();

        RequestFilterDTO filter = RequestFilterDTO.builder()
                .requestType(requestType)
                .status(status)
                .page(page)
                .size(size)
                .sort(sort)
                .build();

        Page<StudentRequestResponseDTO> requests = studentRequestService.getMyRequests(studentId, filter);

        return ResponseEntity.ok(ResponseObject.success("Retrieved student requests successfully", requests));
    }

    @GetMapping("/requests/{requestId}")
    @Operation(summary = "Get request details", description = "Retrieve detailed information about a specific request")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<StudentRequestDetailDTO>> getRequestById(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Request ID") @PathVariable Long requestId) {

        Long studentId = currentUser.getId();
        StudentRequestDetailDTO request = studentRequestService.getRequestById(requestId, studentId);

        return ResponseEntity.ok(ResponseObject.success("Retrieved request details successfully", request));
    }

    @PostMapping("/requests/{requestId}/cancel")
    @Operation(summary = "Cancel request", description = "Cancel a pending request submitted by the student")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<StudentRequestResponseDTO>> cancelRequest(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Request ID") @PathVariable Long requestId) {

        Long studentId = currentUser.getId();
        StudentRequestResponseDTO request = studentRequestService.cancelRequest(requestId, studentId);

        return ResponseEntity.ok(ResponseObject.success("Request cancelled successfully", request));
    }

    @GetMapping("/classes/sessions")
    @Operation(summary = "Get available sessions for date", description = "Retrieve all class sessions for a specific date (used in Step 2 of absence request flow)")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<List<SessionAvailabilityDTO>>> getAvailableSessionsForDate(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Date in YYYY-MM-DD format", required = true)
            @RequestParam String date,
            @Parameter(description = "Request type: ABSENCE, MAKEUP, TRANSFER", required = true)
            @RequestParam String requestType) {

        Long studentId = currentUser.getId();
        LocalDate localDate = LocalDate.parse(date);
        StudentRequestType requestTypeEnum = StudentRequestType.valueOf(requestType);

        List<SessionAvailabilityDTO> sessions = studentRequestService.getAvailableSessionsForDate(
                studentId, localDate, requestTypeEnum);

        return ResponseEntity.ok(ResponseObject.success("Retrieved available sessions successfully", sessions));
    }

    // ==================== MAKEUP REQUEST ENDPOINTS ====================

    @GetMapping("/missed-sessions")
    @Operation(summary = "Get missed sessions for makeup", description = "Retrieve all absent sessions that can be made up (within 4 weeks)")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<MissedSessionsResponseDTO>> getMissedSessions(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Number of weeks to look back (default: 4)")
            @RequestParam(required = false, defaultValue = "4") Integer weeksBack,
            @Parameter(description = "Exclude sessions with existing makeup requests")
            @RequestParam(required = false, defaultValue = "false") Boolean excludeRequested) {

        Long userId = currentUser.getId();
        MissedSessionsResponseDTO result = studentRequestService.getMissedSessions(userId, weeksBack, excludeRequested);

        return ResponseEntity.ok(ResponseObject.success("Retrieved missed sessions successfully", result));
    }

    @GetMapping("/makeup-options")
    @Operation(summary = "Get makeup session options", description = "Get available makeup sessions for a specific missed session with smart ranking")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<MakeupOptionsResponseDTO>> getMakeupOptions(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Target session ID (the session student missed)", required = true)
            @RequestParam Long targetSessionId) {

        Long userId = currentUser.getId();
        MakeupOptionsResponseDTO result = studentRequestService.getMakeupOptions(targetSessionId, userId);

        return ResponseEntity.ok(ResponseObject.success("Retrieved makeup options successfully", result));
    }

    // ==================== TRANSFER REQUEST ENDPOINTS ====================

    @GetMapping("/transfer-eligibility")
    @Operation(summary = "Get transfer eligibility", description = "Check if student is eligible for class transfer and get current enrollments")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<TransferEligibilityDTO>> getTransferEligibility(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Long userId = currentUser.getId();
        TransferEligibilityDTO result = studentRequestService.getTransferEligibility(userId);

        return ResponseEntity.ok(ResponseObject.success("Retrieved transfer eligibility successfully", result));
    }

    @GetMapping("/transfer-options")
    @Operation(summary = "Get transfer options", description = "Get available classes to transfer to from a specific current class")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<List<TransferOptionDTO>>> getTransferOptions(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Current class ID to transfer from", required = true)
            @RequestParam Long currentClassId) {

        Long userId = currentUser.getId();
        List<TransferOptionDTO> result = studentRequestService.getTransferOptions(userId, currentClassId);

        return ResponseEntity.ok(ResponseObject.success("Retrieved transfer options successfully", result));
    }

    @PostMapping("/transfer-requests")
    @Operation(summary = "Submit transfer request", description = "Submit a new class transfer request")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<StudentRequestResponseDTO>> submitTransferRequest(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody TransferRequestDTO dto) {

        Long userId = currentUser.getId();
        StudentRequestResponseDTO result = studentRequestService.submitTransferRequest(userId, dto);

        return ResponseEntity.ok(ResponseObject.success("Transfer request submitted successfully", result));
    }
}