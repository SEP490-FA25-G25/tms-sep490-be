package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.studentrequest.AARequestFilterDTO;
import org.fyp.tmssep490be.dtos.studentrequest.AARequestResponseDTO;
import org.fyp.tmssep490be.dtos.studentrequest.PagedAARequestResponseDTO;
import org.fyp.tmssep490be.dtos.studentrequest.RequestSummaryDTO;
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
}
