package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.schedule.TeacherSessionDetailDTO;
import org.fyp.tmssep490be.dtos.schedule.WeeklyScheduleResponseDTO;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.TeacherScheduleService;
import org.fyp.tmssep490be.utils.TeacherContextHelper;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/teacher")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Teacher Schedule", description = "APIs for teacher to view their weekly schedule and session details")
public class TeacherScheduleController {

    private final TeacherScheduleService teacherScheduleService;
    private final TeacherContextHelper teacherContextHelper;

    @GetMapping("/current-week")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Get current week start date", description = "Returns the Monday of the current week in ISO 8601 format (YYYY-MM-DD)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Current week start retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ResponseObject.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ResponseObject<String>> getCurrentWeek() {
        LocalDate weekStart = teacherScheduleService.getCurrentWeekStart();
        return ResponseEntity.ok(
                ResponseObject.<String>builder()
                        .success(true)
                        .message("Current week start retrieved successfully")
                        .data(weekStart.toString())
                        .build()
        );
    }

    @GetMapping("/schedule")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Get weekly schedule", description = "Returns the teacher's weekly schedule for the specified week")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Weekly schedule retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ResponseObject.class))),
            @ApiResponse(responseCode = "400", description = "Invalid weekStart (must be a Monday)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ResponseObject<WeeklyScheduleResponseDTO>> getWeeklySchedule(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "Monday of the week in ISO 8601 format (YYYY-MM-DD)", required = true)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam LocalDate weekStart,
            @Parameter(description = "Optional class ID to filter sessions")
            @RequestParam(required = false)
            Long classId
    ) {
        log.info("Teacher {} requesting weekly schedule for week: {}, class: {}",
                userPrincipal.getId(), weekStart, classId);

        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);

        if (weekStart == null) {
            weekStart = teacherScheduleService.getCurrentWeekStart();
            log.debug("Using current week start: {}", weekStart);
        }

        // Validate weekStart is Monday
        if (weekStart.getDayOfWeek() != java.time.DayOfWeek.MONDAY) {
            log.warn("Invalid weekStart provided: {} (not a Monday)", weekStart);
            return ResponseEntity.badRequest().body(
                    ResponseObject.<WeeklyScheduleResponseDTO>builder()
                            .success(false)
                            .message("weekStart must be a Monday (ISO 8601 format: YYYY-MM-DD)")
                            .build()
            );
        }

        WeeklyScheduleResponseDTO schedule = teacherScheduleService.getWeeklySchedule(teacherId, weekStart, classId);

        return ResponseEntity.ok(
                ResponseObject.<WeeklyScheduleResponseDTO>builder()
                        .success(true)
                        .message("Weekly schedule retrieved successfully")
                        .data(schedule)
                        .build()
        );
    }

    @GetMapping("/sessions/{sessionId}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Get session detail", description = "Returns detailed information about a specific session")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Session detail retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ResponseObject.class))),
            @ApiResponse(responseCode = "403", description = "Teacher is not assigned to this session"),
            @ApiResponse(responseCode = "404", description = "Session not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ResponseObject<TeacherSessionDetailDTO>> getSessionDetail(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "Session ID", required = true)
            @PathVariable Long sessionId
    ) {
        log.info("Teacher {} requesting details for session: {}", userPrincipal.getId(), sessionId);

        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);

        TeacherSessionDetailDTO sessionDetail = teacherScheduleService.getSessionDetail(teacherId, sessionId);

        return ResponseEntity.ok(
                ResponseObject.<TeacherSessionDetailDTO>builder()
                        .success(true)
                        .message("Session detail retrieved successfully")
                        .data(sessionDetail)
                        .build()
        );
    }
}

