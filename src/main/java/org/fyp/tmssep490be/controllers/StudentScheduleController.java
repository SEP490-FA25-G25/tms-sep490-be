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

@RestController
@RequestMapping("/api/v1/students")
@Slf4j
@RequiredArgsConstructor
public class StudentScheduleController {

    private final StudentScheduleService studentScheduleService;
    private final StudentContextHelper studentContextHelper;

    @GetMapping("/me/current-week")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<String>> getCurrentWeek() {
        LocalDate weekStart = studentScheduleService.getCurrentWeekStart();
        return ResponseEntity.ok(
                ResponseObject.<String>builder()
                        .success(true)
                        .message("Current week start retrieved successfully")
                        .data(weekStart.toString())
                        .build()
        );
    }

    @GetMapping("/me/schedule")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<WeeklyScheduleResponseDTO>> getMySchedule(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate weekStart,
            @RequestParam(required = false)
            Long classId
    ) {
        log.info("Student {} requesting weekly schedule for week: {}, class: {}",
                userPrincipal.getId(), weekStart, classId);

        Long studentId = studentContextHelper.getStudentId(userPrincipal);

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

        WeeklyScheduleResponseDTO schedule = studentScheduleService.getWeeklySchedule(studentId, weekStart);

        return ResponseEntity.ok(
                ResponseObject.<WeeklyScheduleResponseDTO>builder()
                        .success(true)
                        .message("Weekly schedule retrieved successfully")
                        .data(schedule)
                        .build()
        );
    }

    @GetMapping("/me/sessions/{sessionId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<SessionDetailDTO>> getMySessionDetail(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long sessionId
    ) {
        log.info("Student {} requesting details for session: {}", userPrincipal.getId(), sessionId);

        Long studentId = studentContextHelper.getStudentId(userPrincipal);

        SessionDetailDTO sessionDetail = studentScheduleService.getSessionDetail(studentId, sessionId);

        return ResponseEntity.ok(
                ResponseObject.<SessionDetailDTO>builder()
                        .success(true)
                        .message("Session detail retrieved successfully")
                        .data(sessionDetail)
                        .build()
        );
    }
}
