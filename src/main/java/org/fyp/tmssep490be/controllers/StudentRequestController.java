package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.studentrequest.RequestFilterDTO;
import org.fyp.tmssep490be.dtos.studentrequest.StudentRequestResponseDTO;
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
@Tag(name = "Student Request Management", description = "APIs for students to manage their absence/makeup/transfer requests")
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
}
