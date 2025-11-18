package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.studentattendance.StudentAttendanceOverviewResponseDTO;
import org.fyp.tmssep490be.dtos.studentattendance.StudentAttendanceReportResponseDTO;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.StudentAttendanceService;
import org.fyp.tmssep490be.utils.StudentContextHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Tag(name = "Student Attendance", description = "APIs for students to view their attendance")
@SecurityRequirement(name = "bearerAuth")
public class StudentAttendanceController {

    private final StudentAttendanceService studentAttendanceService;
    private final StudentContextHelper studentContextHelper;

    @GetMapping("/attendance/overview")
    @Operation(summary = "Get attendance overview for student")
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = StudentAttendanceOverviewResponseDTO.class)))
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<StudentAttendanceOverviewResponseDTO>> getOverview(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long studentId = studentContextHelper.getStudentId(userPrincipal);
        StudentAttendanceOverviewResponseDTO data = studentAttendanceService.getOverview(studentId);
        return ResponseEntity.ok(
                ResponseObject.<StudentAttendanceOverviewResponseDTO>builder()
                        .success(true)
                        .message("OK")
                        .data(data)
                        .build()
        );
    }

    @GetMapping("/attendance/report")
    @Operation(summary = "Get attendance report for a class")
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = StudentAttendanceReportResponseDTO.class)))
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<StudentAttendanceReportResponseDTO>> getReport(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam Long classId
    ) {
        Long studentId = studentContextHelper.getStudentId(userPrincipal);
        StudentAttendanceReportResponseDTO data = studentAttendanceService.getReport(studentId, classId);
        return ResponseEntity.ok(
                ResponseObject.<StudentAttendanceReportResponseDTO>builder()
                        .success(true)
                        .message("OK")
                        .data(data)
                        .build()
        );
    }
}




