package org.fyp.tmssep490be.controllers;

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
public class TeacherScheduleController {

    private final TeacherScheduleService teacherScheduleService;
    private final TeacherContextHelper teacherContextHelper;

    @GetMapping("/current-week")
    @PreAuthorize("hasRole('TEACHER')")
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
    public ResponseEntity<ResponseObject<WeeklyScheduleResponseDTO>> getWeeklySchedule(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam LocalDate weekStart,
            @RequestParam(required = false)
            Long classId
    ) {
        log.info("Giáo viên {} yêu cầu lịch dạy tuần: {}, lớp: {}",
                userPrincipal.getId(), weekStart, classId);

        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);

        if (weekStart == null) {
            weekStart = teacherScheduleService.getCurrentWeekStart();
            log.debug("Sử dụng tuần hiện tại bắt đầu từ: {}", weekStart);
        }

        // Validate weekStart is Monday
        if (weekStart.getDayOfWeek() != java.time.DayOfWeek.MONDAY) {
            log.warn("weekStart không hợp lệ: {} (không phải thứ Hai)", weekStart);
            return ResponseEntity.badRequest().body(
                    ResponseObject.<WeeklyScheduleResponseDTO>builder()
                            .success(false)
                            .message("weekStart must be a Monday (ISO 8601 format: YYYY-MM-DD)")
                            .build()
            );
        }

        WeeklyScheduleResponseDTO schedule = teacherScheduleService.getWeeklySchedule(teacherId, weekStart, classId, null);

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
    public ResponseEntity<ResponseObject<TeacherSessionDetailDTO>> getSessionDetail(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long sessionId
    ) {
        log.info("Giáo viên {} yêu cầu chi tiết buổi học: {}", userPrincipal.getId(), sessionId);

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

