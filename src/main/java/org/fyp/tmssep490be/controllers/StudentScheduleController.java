package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.schedule.SessionDetailDTO;
import org.fyp.tmssep490be.dtos.schedule.WeeklyScheduleResponseDTO;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.StudentScheduleService;
import org.fyp.tmssep490be.utils.StudentContextHelper;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller for student schedule operations
 * Provides endpoints for students to view their class schedules
 */
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Student Schedule", description = "APIs for students to view their class schedules")
@SecurityRequirement(name = "bearerAuth")
public class StudentScheduleController {

    private final StudentScheduleService studentScheduleService;
    private final StudentContextHelper studentContextHelper;

    /**
     * Get weekly schedule for the authenticated student
     */
    @GetMapping("/me/schedule")
    @Operation(
            summary = "Get weekly schedule",
            description = "Get the authenticated student's class schedule for a specific week. " +
                    "Returns sessions organized by day and timeslot. " +
                    "Can filter by specific class if classId is provided."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Schedule retrieved successfully",
                    content = @Content(schema = @Schema(implementation = WeeklyScheduleResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid weekStart parameter - must be a Monday"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User is not a student"
            )
    })
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<WeeklyScheduleResponseDTO>> getMySchedule(
            @AuthenticationPrincipal UserPrincipal userPrincipal,

            @Parameter(
                    description = "Monday of the target week in ISO 8601 format (YYYY-MM-DD). " +
                            "If not provided, defaults to current week Monday.",
                    example = "2025-11-04"
            )
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate weekStart,

            @Parameter(
                    description = "Filter by specific class ID (optional). If provided, only returns sessions for this class.",
                    example = "2"
            )
            @RequestParam(required = false)
            Long classId
    ) {
        log.info("Student {} requesting weekly schedule for week: {}, class: {}",
                userPrincipal.getId(), weekStart, classId);

        // 1. Extract student ID from JWT
        Long studentId = studentContextHelper.getStudentId(userPrincipal);

        // 2. Default to current week if not specified
        if (weekStart == null) {
            weekStart = studentScheduleService.getCurrentWeekStart();
            log.debug("Using current week start: {}", weekStart);
        }

        // 3. Validate weekStart is Monday
        if (weekStart.getDayOfWeek() != java.time.DayOfWeek.MONDAY) {
            log.warn("Invalid weekStart provided: {} (not a Monday)", weekStart);
            return ResponseEntity.badRequest().body(
                    ResponseObject.<WeeklyScheduleResponseDTO>builder()
                            .success(false)
                            .message("weekStart must be a Monday (ISO 8601 format: YYYY-MM-DD)")
                            .build()
            );
        }

        // 4. Fetch schedule (with optional class filter)
        WeeklyScheduleResponseDTO schedule;
        if (classId != null) {
            schedule = studentScheduleService.getWeeklyScheduleByClass(studentId, classId, weekStart);
        } else {
            schedule = studentScheduleService.getWeeklySchedule(studentId, weekStart);
        }

        // 5. Return response
        return ResponseEntity.ok(
                ResponseObject.<WeeklyScheduleResponseDTO>builder()
                        .success(true)
                        .message("Weekly schedule retrieved successfully")
                        .data(schedule)
                        .build()
        );
    }

    /**
     * Get detailed information for a specific session
     */
    @GetMapping("/me/sessions/{sessionId}")
    @Operation(
            summary = "Get session details",
            description = "Get detailed information for a specific session including materials, " +
                    "homework, and attendance status. Student can only view their own enrolled sessions."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Session details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = SessionDetailDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User is not a student"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Session not found or student not enrolled in this session"
            )
    })
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<SessionDetailDTO>> getMySessionDetail(
            @AuthenticationPrincipal UserPrincipal userPrincipal,

            @Parameter(
                    description = "ID of the session to retrieve",
                    example = "1001"
            )
            @PathVariable Long sessionId
    ) {
        log.info("Student {} requesting details for session: {}", userPrincipal.getId(), sessionId);

        // 1. Extract student ID from JWT
        Long studentId = studentContextHelper.getStudentId(userPrincipal);

        // 2. Fetch session detail (with authorization check inside service)
        SessionDetailDTO sessionDetail = studentScheduleService.getSessionDetail(studentId, sessionId);

        // 3. Return response
        return ResponseEntity.ok(
                ResponseObject.<SessionDetailDTO>builder()
                        .success(true)
                        .message("Session details retrieved successfully")
                        .data(sessionDetail)
                        .build()
        );
    }

    /**
     * Get current week start (Monday) for quick reference
     */
    @GetMapping("/me/current-week")
    @Operation(
            summary = "Get current week start",
            description = "Returns the Monday date of the current week. Useful for frontend navigation."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Current week start retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User is not a student"
            )
    })
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<LocalDate>> getCurrentWeek() {
        LocalDate currentWeekStart = studentScheduleService.getCurrentWeekStart();

        return ResponseEntity.ok(
                ResponseObject.<LocalDate>builder()
                        .success(true)
                        .message("Current week start retrieved successfully")
                        .data(currentWeekStart)
                        .build()
        );
    }
}