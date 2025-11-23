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
import org.fyp.tmssep490be.dtos.schedule.TeacherSessionDetailDTO;
import org.fyp.tmssep490be.dtos.schedule.TeacherWeeklyScheduleResponseDTO;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.TeacherScheduleService;
import org.fyp.tmssep490be.utils.TeacherContextHelper;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller for teacher schedule operations
 * Provides endpoints for teachers to view their teaching schedules
 */
@RestController
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Teacher Schedule", description = "APIs for teachers to view their teaching schedules")
@SecurityRequirement(name = "bearerAuth")
public class TeacherScheduleController {

    private final TeacherScheduleService teacherScheduleService;
    private final TeacherContextHelper teacherContextHelper;

    /**
     * Get weekly schedule for the authenticated teacher
     */
    @GetMapping("/schedule")
    @Operation(
            summary = "Get weekly schedule",
            description = "Get the authenticated teacher's teaching schedule for a specific week. " +
                    "Returns sessions organized by day and timeslot. " +
                    "Can filter by specific class if classId is provided."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Schedule retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TeacherWeeklyScheduleResponseDTO.class))
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
                    description = "Forbidden - User is not a teacher"
            )
    })
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<TeacherWeeklyScheduleResponseDTO>> getMySchedule(
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
        log.info("Teacher {} requesting weekly schedule for week: {}, class: {}",
                userPrincipal.getId(), weekStart, classId);

        // 1. Extract teacher ID from JWT
        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);

        // 2. Default to current week if not specified
        if (weekStart == null) {
            weekStart = teacherScheduleService.getCurrentWeekStart();
            log.debug("Using current week start: {}", weekStart);
        }

        // 3. Validate weekStart is Monday
        if (weekStart.getDayOfWeek() != java.time.DayOfWeek.MONDAY) {
            log.warn("Invalid weekStart provided: {} (not a Monday)", weekStart);
            return ResponseEntity.badRequest().body(
                    ResponseObject.<TeacherWeeklyScheduleResponseDTO>builder()
                            .success(false)
                            .message("weekStart must be a Monday (ISO 8601 format: YYYY-MM-DD)")
                            .build()
            );
        }

        // 4. Fetch schedule (with optional class filter)
        TeacherWeeklyScheduleResponseDTO schedule;
        if (classId != null) {
            schedule = teacherScheduleService.getWeeklyScheduleByClass(teacherId, classId, weekStart);
        } else {
            schedule = teacherScheduleService.getWeeklySchedule(teacherId, weekStart);
        }

        // 5. Return response
        return ResponseEntity.ok(
                ResponseObject.<TeacherWeeklyScheduleResponseDTO>builder()
                        .success(true)
                        .message("Weekly schedule retrieved successfully")
                        .data(schedule)
                        .build()
        );
    }

    /**
     * Get detailed information for a specific session
     */
    @GetMapping("/sessions/{sessionId}")
    @Operation(
            summary = "Get session details",
            description = "Get detailed information for a specific session including materials, " +
                    "attendance summary, and classroom resources. Teacher can only view sessions they are assigned to teach."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Session details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TeacherSessionDetailDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User is not a teacher"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Session not found or teacher not assigned to this session"
            )
    })
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<TeacherSessionDetailDTO>> getMySessionDetail(
            @AuthenticationPrincipal UserPrincipal userPrincipal,

            @Parameter(
                    description = "ID of the session to retrieve",
                    example = "1001"
            )
            @PathVariable Long sessionId
    ) {
        log.info("Teacher {} requesting details for session: {}", userPrincipal.getId(), sessionId);

        // 1. Extract teacher ID from JWT
        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);

        // 2. Fetch session detail (with authorization check inside service)
        TeacherSessionDetailDTO sessionDetail = teacherScheduleService.getSessionDetail(teacherId, sessionId);

        // 3. Return response
        return ResponseEntity.ok(
                ResponseObject.<TeacherSessionDetailDTO>builder()
                        .success(true)
                        .message("Session details retrieved successfully")
                        .data(sessionDetail)
                        .build()
        );
    }

    /**
     * Get current week start (Monday) for quick reference
     */
    @GetMapping("/current-week")
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
                    description = "Forbidden - User is not a teacher"
            )
    })
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<LocalDate>> getCurrentWeek() {
        LocalDate currentWeekStart = teacherScheduleService.getCurrentWeekStart();

        return ResponseEntity.ok(
                ResponseObject.<LocalDate>builder()
                        .success(true)
                        .message("Current week start retrieved successfully")
                        .data(currentWeekStart)
                        .build()
        );
    }
}

