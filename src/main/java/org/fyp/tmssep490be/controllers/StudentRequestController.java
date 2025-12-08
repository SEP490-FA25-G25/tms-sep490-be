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
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.entities.enums.StudentRequestType;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.StudentRequestService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/students-request")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "Bearer Authentication")
public class StudentRequestController {

    private final StudentRequestService studentRequestService;

    @GetMapping("/requests")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<Page<StudentRequestResponseDTO>>> getMyRequests(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) String requestType,
            @RequestParam(required = false) String requestTypes,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String statuses,
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "submittedAt,desc") String sort,
            @Parameter(description = "Search by request reason, class code, or session title")
            @RequestParam(required = false) String search) {

        // Get student ID from user principal
        Long studentId = currentUser.getId();

        // Process filter parameters - support both single and multiple values
        List<StudentRequestType> requestTypeFilters = null;
        List<RequestStatus> statusFilters = null;

        // Parse request types
        if (requestTypes != null && !requestTypes.trim().isEmpty()) {
            try {
                requestTypeFilters = Arrays.stream(requestTypes.split(","))
                        .map(String::trim)
                        .map(StudentRequestType::valueOf)
                        .collect(java.util.stream.Collectors.toList());
            } catch (IllegalArgumentException e) {
                // Invalid enum value, ignore and use null
            }
        } else if (requestType != null && !requestType.trim().isEmpty()) {
            try {
                requestTypeFilters = List.of(StudentRequestType.valueOf(requestType));
            } catch (IllegalArgumentException e) {
                // Invalid enum value, ignore and use null
            }
        }

        // Parse statuses
        if (statuses != null && !statuses.trim().isEmpty()) {
            try {
                statusFilters = Arrays.stream(statuses.split(","))
                        .map(String::trim)
                        .map(RequestStatus::valueOf)
                        .collect(java.util.stream.Collectors.toList());
            } catch (IllegalArgumentException e) {
                // Invalid enum value, ignore and use null
            }
        } else if (status != null && !status.trim().isEmpty()) {
            try {
                statusFilters = List.of(RequestStatus.valueOf(status));
            } catch (IllegalArgumentException e) {
                // Invalid enum value, ignore and use null
            }
        }

        RequestFilterDTO filter = RequestFilterDTO.builder()
                .requestType(requestType) // Keep for backward compatibility
                .status(status) // Keep for backward compatibility
                .requestTypeFilters(requestTypeFilters)
                .statusFilters(statusFilters)
                .search(search)
                .page(page)
                .size(size)
                .sort(sort)
                .build();

        Page<StudentRequestResponseDTO> requests = studentRequestService.getMyRequests(studentId, filter);

        return ResponseEntity.ok(ResponseObject.success("Retrieved student requests successfully", requests));
    }

    @GetMapping("/requests/{requestId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<StudentRequestDetailDTO>> getRequestById(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Request ID") @PathVariable Long requestId) {

        Long studentId = currentUser.getId();
        StudentRequestDetailDTO request = studentRequestService.getRequestById(requestId, studentId);

        return ResponseEntity.ok(ResponseObject.success("Retrieved request details successfully", request));
    }

    //Step đầu tiên trong luồng tạo makeup request là student phải gọi config từ bảng policy lên
    @GetMapping("/config")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<StudentRequestConfigDTO>> getConfig() {
        StudentRequestConfigDTO config = studentRequestService.getStudentRequestConfig();
        return ResponseEntity.ok(ResponseObject.success("Retrieved configuration successfully", config));
    }

    // Lấy ra những buổi bị missed trong quá khứ cả xin phep lẫn chưa xin phép
    @GetMapping("/missed-sessions")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<MissedSessionsResponseDTO>> getMissedSessions(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) Integer weeksBack,
            @RequestParam(required = false) Boolean excludeRequested) {

        MissedSessionsResponseDTO response = studentRequestService.getMissedSessions(
                currentUser.getId(), weeksBack, excludeRequested);

        return ResponseEntity.ok(ResponseObject.success("Retrieved missed sessions successfully", response));
    }

    // Lấy ra buổi phù hợp
    @GetMapping("/makeup-options")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get makeup session options", description = "Get available makeup sessions for a specific missed session with smart ranking by branch, modality, date, and capacity")
    public ResponseEntity<ResponseObject<MakeupOptionsResponseDTO>> getMakeupOptions(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Target session ID (the missed session to makeup)", required = true)
            @RequestParam Long targetSessionId) {

        MakeupOptionsResponseDTO response = studentRequestService.getMakeupOptions(
                currentUser.getId(), targetSessionId);

        return ResponseEntity.ok(ResponseObject.success("Retrieved makeup options successfully", response));
    }

    @PostMapping("/makeup-requests")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Submit makeup request", description = "Submit a makeup request for a missed session")
    public ResponseEntity<ResponseObject<StudentRequestResponseDTO>> submitMakeupRequest(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody MakeupRequestDTO dto) {

        StudentRequestResponseDTO response = studentRequestService.submitMakeupRequest(
                currentUser.getId(), dto);

        return ResponseEntity.ok(ResponseObject.success("Makeup request submitted successfully", response));
    }
}
