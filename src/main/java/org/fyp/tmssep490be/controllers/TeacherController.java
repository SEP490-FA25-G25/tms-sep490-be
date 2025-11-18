package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.dtos.attendance.AttendanceMatrixDTO;
import org.fyp.tmssep490be.dtos.attendance.TeacherClassListItemDTO;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.AttendanceService;
import org.fyp.tmssep490be.utils.TeacherContextHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
@Tag(name = "Teacher", description = "Teacher management APIs")
@SecurityRequirement(name = "bearerAuth")
public class TeacherController {

    private final AttendanceService attendanceService;
    private final TeacherContextHelper teacherContextHelper;

    @GetMapping("/classes")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Get list of classes for teacher")
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = TeacherClassListItemDTO.class)))
    public ResponseEntity<ResponseObject<List<TeacherClassListItemDTO>>> getTeacherClasses(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
        List<TeacherClassListItemDTO> classes = attendanceService.getTeacherClasses(teacherId);
        return ResponseEntity.ok(
                ResponseObject.<List<TeacherClassListItemDTO>>builder()
                        .success(true)
                        .message("OK")
                        .data(classes)
                        .build()
        );
    }

    @GetMapping("/classes/{classId}/matrix")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Get attendance matrix for a class")
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = AttendanceMatrixDTO.class)))
    public ResponseEntity<ResponseObject<AttendanceMatrixDTO>> getClassAttendanceMatrix(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long classId
    ) {
        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
        AttendanceMatrixDTO data = attendanceService.getClassAttendanceMatrix(teacherId, classId);
        return ResponseEntity.ok(
                ResponseObject.<AttendanceMatrixDTO>builder()
                        .success(true)
                        .message("OK")
                        .data(data)
                        .build()
        );
    }
}



