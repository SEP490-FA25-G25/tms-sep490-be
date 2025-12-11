package org.fyp.tmssep490be.controllers;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.dtos.attendance.*;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.scheduler.SessionAutoUpdateService;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.AttendanceService;
import org.fyp.tmssep490be.utils.TeacherContextHelper;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final TeacherContextHelper teacherContextHelper;
    private final SessionAutoUpdateService sessionAutoUpdateService;

    @GetMapping("/sessions/today")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<List<SessionTodayDTO>>> getTodaySessions(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<SessionTodayDTO> sessions = attendanceService.getSessionsForDate(teacherId, targetDate);
        return ResponseEntity.ok(
                ResponseObject.<List<SessionTodayDTO>>builder()
                        .success(true)
                        .message("OK")
                        .data(sessions)
                        .build()
        );
    }

    @GetMapping("/sessions/{sessionId}/students")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<StudentsAttendanceResponseDTO>> getSessionStudents(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long sessionId
    ) {
        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
        StudentsAttendanceResponseDTO data = attendanceService.getSessionStudents(teacherId, sessionId);
        return ResponseEntity.ok(
                ResponseObject.<StudentsAttendanceResponseDTO>builder()
                        .success(true)
                        .message("OK")
                        .data(data)
                        .build()
        );
    }

    @PostMapping("/sessions/{sessionId}/mark-all-present")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<MarkAllResponseDTO>> markAllPresent(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long sessionId
    ) {
        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
        MarkAllResponseDTO data = attendanceService.markAllPresent(teacherId, sessionId);
        return ResponseEntity.ok(
                ResponseObject.<MarkAllResponseDTO>builder()
                        .success(true)
                        .message("OK")
                        .data(data)
                        .build()
        );
    }

    @PostMapping("/sessions/{sessionId}/mark-all-absent")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<MarkAllResponseDTO>> markAllAbsent(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long sessionId
    ) {
        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
        MarkAllResponseDTO data = attendanceService.markAllAbsent(teacherId, sessionId);
        return ResponseEntity.ok(
                ResponseObject.<MarkAllResponseDTO>builder()
                        .success(true)
                        .message("OK")
                        .data(data)
                        .build()
        );
    }

    @PostMapping("/sessions/{sessionId}/save")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<AttendanceSaveResponseDTO>> saveAttendance(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long sessionId,
            @RequestBody AttendanceSaveRequestDTO request
    ) {
        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
        AttendanceSaveResponseDTO data = attendanceService.saveAttendance(teacherId, sessionId, request);
        return ResponseEntity.ok(
                ResponseObject.<AttendanceSaveResponseDTO>builder()
                        .success(true)
                        .message("OK")
                        .data(data)
                        .build()
        );
    }

    @GetMapping("/sessions/{sessionId}/report")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<SessionReportResponseDTO>> getSessionReport(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long sessionId
    ) {
        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
        SessionReportResponseDTO data = attendanceService.getSessionReport(teacherId, sessionId);
        return ResponseEntity.ok(
                ResponseObject.<SessionReportResponseDTO>builder()
                        .success(true)
                        .message("OK")
                        .data(data)
                        .build()
        );
    }

    @PostMapping("/sessions/{sessionId}/report")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<SessionReportResponseDTO>> submitSessionReport(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long sessionId,
            @RequestBody SessionReportSubmitDTO request
    ) {
        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
        SessionReportResponseDTO data = attendanceService.submitSessionReport(teacherId, sessionId, request);
        return ResponseEntity.ok(
                ResponseObject.<SessionReportResponseDTO>builder()
                        .success(true)
                        .message("OK")
                        .data(data)
                        .build()
        );
    }

    @PostMapping("/admin/update-past-sessions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CENTER_HEAD')")
    public ResponseEntity<ResponseObject<String>> updatePastSessions() {
        sessionAutoUpdateService.updatePastSessionsToDoneNow();
        return ResponseEntity.ok(
                ResponseObject.<String>builder()
                        .success(true)
                        .message("Past sessions update triggered successfully")
                        .data("Update completed")
                        .build()
        );
    }
}

