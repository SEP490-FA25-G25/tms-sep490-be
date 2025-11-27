package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.qa.QAClassDetailDTO;
import org.fyp.tmssep490be.dtos.qa.QAClassListItemDTO;
import org.fyp.tmssep490be.dtos.qa.QADashboardDTO;
import org.fyp.tmssep490be.dtos.qa.QASessionListResponse;
import org.fyp.tmssep490be.dtos.qa.SessionDetailDTO;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.QAService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/qa")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "QA", description = "Quality Assurance APIs - Dashboard and Class Management")
public class QAController {

    private final QAService qaService;

    /**
     * QA Dashboard - KPIs and metrics overview
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('QA')")
    @Operation(
        summary = "Get QA Dashboard",
        description = "Retrieve KPIs and metrics overview for Quality Assurance including: " +
                      "ongoing classes count, QA reports created this month, average attendance rate, " +
                      "average homework completion rate, classes requiring attention, and recent QA reports."
    )
    public ResponseEntity<ResponseObject<QADashboardDTO>> getQADashboard(
        @Parameter(description = "Filter by branch IDs (optional)")
        @RequestParam(required = false) List<Long> branchIds,

        @Parameter(description = "Date range start (optional)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,

        @Parameter(description = "Date range end (optional)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,

        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requesting QA dashboard with filters: branchIds={}, dateFrom={}, dateTo={}",
                 currentUser.getId(), branchIds, dateFrom, dateTo);

        QADashboardDTO dashboard = qaService.getQADashboard(branchIds, dateFrom, dateTo, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<QADashboardDTO>builder()
            .success(true)
            .message("QA dashboard retrieved successfully")
            .data(dashboard)
            .build());
    }

    /**
     * QA Classes List - Classes with QA-specific metrics
     */
    @GetMapping("/classes")
    @PreAuthorize("hasRole('QA')")
    @Operation(
        summary = "Get QA Classes List",
        description = "Retrieve paginated list of classes with QA-specific metrics including: " +
                      "attendance rate, homework completion rate, and QA report count. " +
                      "This endpoint is specifically designed for QA role with different metrics " +
                      "compared to Academic Affair's class list."
    )
    public ResponseEntity<ResponseObject<Page<QAClassListItemDTO>>> getQAClasses(
        @Parameter(description = "Filter by branch IDs (optional)")
        @RequestParam(required = false) List<Long> branchIds,

        @Parameter(description = "Filter by class status: ONGOING, COMPLETED (optional)")
        @RequestParam(required = false) String status,

        @Parameter(description = "Search by class code or course name (optional)")
        @RequestParam(required = false) String search,

        @Parameter(description = "Page number (0-based)")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "Page size")
        @RequestParam(defaultValue = "20") int size,

        @Parameter(description = "Sort field")
        @RequestParam(defaultValue = "startDate") String sort,

        @Parameter(description = "Sort direction (asc/desc)")
        @RequestParam(defaultValue = "desc") String sortDir,

        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requesting QA classes list with filters: branchIds={}, status={}, search={}",
                 currentUser.getId(), branchIds, status, search);

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));

        Page<QAClassListItemDTO> classes = qaService.getQAClasses(branchIds, status, search, pageable, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<Page<QAClassListItemDTO>>builder()
            .success(true)
            .message("QA classes list retrieved successfully")
            .data(classes)
            .build());
    }

    /**
     * Get QA Class Detail
     */
    @GetMapping("/classes/{classId}")
    @PreAuthorize("hasRole('QA')")
    @Operation(
        summary = "Get QA Class Detail",
        description = "Retrieve detailed class information with QA-specific metrics and reports including: " +
                      "basic class info, session summary, performance metrics (attendance rate, homework completion rate), " +
                      "QA reports list, and teacher assignments."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "QA class detail retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Class not found")
    })
    public ResponseEntity<ResponseObject<QAClassDetailDTO>> getQAClassDetail(
        @PathVariable Long classId,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requesting QA class detail for classId={}", currentUser.getId(), classId);

        QAClassDetailDTO classDetail = qaService.getQAClassDetail(classId, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<QAClassDetailDTO>builder()
            .success(true)
            .message("QA class detail retrieved successfully")
            .data(classDetail)
            .build());
    }

    /**
     * Get QA Session List for a Class
     */
    @GetMapping("/classes/{classId}/sessions")
    @PreAuthorize("hasRole('QA')")
    @Operation(
        summary = "Get QA Session List",
        description = "Retrieve session list with attendance metrics and QA report status including: " +
                      "session info (date, time, topic, teacher), attendance statistics (present/absent counts and rates), " +
                      "homework completion statistics, and QA report indicators for each session."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "QA session list retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Class not found")
    })
    public ResponseEntity<ResponseObject<QASessionListResponse>> getQASessionList(
        @PathVariable Long classId,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requesting QA session list for classId={}", currentUser.getId(), classId);

        QASessionListResponse sessions = qaService.getQASessionList(classId, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<QASessionListResponse>builder()
            .success(true)
            .message("QA session list retrieved successfully")
            .data(sessions)
            .build());
    }

    /**
     * Get QA Session Detail
     */
    @GetMapping("/sessions/{sessionId}")
    @PreAuthorize("hasRole('QA')")
    @Operation(
        summary = "Get QA Session Detail",
        description = "Retrieve detailed session information with student data, CLO achievement metrics, and feedback summary including: " +
                      "session basic info (date, time, topic, teacher), student attendance details with homework completion, " +
                      "attendance and homework statistics, CLO information covered in this session, and student feedback summary."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "QA session detail retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ResponseEntity<ResponseObject<SessionDetailDTO>> getQASessionDetail(
        @Parameter(description = "Session ID", example = "123")
        @PathVariable Long sessionId,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requesting QA session detail for sessionId={}", currentUser.getId(), sessionId);

        SessionDetailDTO sessionDetail = qaService.getQASessionDetail(sessionId, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<SessionDetailDTO>builder()
            .success(true)
            .message("QA session detail retrieved successfully")
            .data(sessionDetail)
            .build());
    }
}
