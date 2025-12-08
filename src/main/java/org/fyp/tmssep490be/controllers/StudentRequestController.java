package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "submittedAt,desc") String sort,
            @Parameter(description = "Search by request reason, class code, or session title")
            @RequestParam(required = false) String search) {

        Long studentId = currentUser.getId();

        RequestFilterDTO filter = RequestFilterDTO.builder()
                .requestType(requestType)
                .status(status)
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
