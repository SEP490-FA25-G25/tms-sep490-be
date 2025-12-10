package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.teachergrade.TeacherAssessmentDTO;
import org.fyp.tmssep490be.dtos.teachergrade.TeacherStudentScoreDTO;
import org.fyp.tmssep490be.dtos.teachergrade.ScoreInputDTO;
import org.fyp.tmssep490be.dtos.teachergrade.BatchScoreInputDTO;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.TeacherGradeService;
import org.fyp.tmssep490be.utils.TeacherContextHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher/grades")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Teacher Grade Management", description = "APIs for teachers to manage student grades and assessments")
@SecurityRequirement(name = "bearerAuth")
public class TeacherGradeController {

    private final TeacherGradeService teacherGradeService;
    private final TeacherContextHelper teacherContextHelper;

    // Lấy danh sách bài kiểm tra của lớp
    @GetMapping("/classes/{classId}/assessments")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(
            summary = "Get class assessments",
            description = "Get list of assessments for a class with optional filter (all, upcoming, graded, overdue)"
    )
    public ResponseEntity<ResponseObject<List<TeacherAssessmentDTO>>> getClassAssessments(
            @Parameter(description = "Class ID") @PathVariable Long classId,
            @Parameter(description = "Filter type: all, upcoming, graded, overdue")
            @RequestParam(required = false, defaultValue = "all") String filter,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("User {} requesting assessments for class {} with filter: {}", userPrincipal.getId(), classId, filter);

        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
        List<TeacherAssessmentDTO> assessments = teacherGradeService.getClassAssessments(teacherId, classId, filter);

        return ResponseEntity.ok(
                ResponseObject.<List<TeacherAssessmentDTO>>builder()
                        .success(true)
                        .message("Assessments retrieved successfully")
                        .data(assessments)
                        .build()
        );
    }

    // Lấy điểm của toàn bộ học viên trong bài kiểm tra
    @GetMapping("/assessments/{assessmentId}/scores")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<List<TeacherStudentScoreDTO>>> getAssessmentScores(
            @PathVariable Long assessmentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
        List<TeacherStudentScoreDTO> scores = teacherGradeService.getAssessmentScores(teacherId, assessmentId);
        return ResponseEntity.ok(
                ResponseObject.<List<TeacherStudentScoreDTO>>builder()
                        .success(true)
                        .message("Scores retrieved successfully")
                        .data(scores)
                        .build()
        );
    }

    // Lấy điểm của một học viên trong bài kiểm tra
    @GetMapping("/assessments/{assessmentId}/scores/{studentId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<TeacherStudentScoreDTO>> getStudentScore(
            @PathVariable Long assessmentId,
            @PathVariable Long studentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
        TeacherStudentScoreDTO score = teacherGradeService.getStudentScore(teacherId, assessmentId, studentId);
        return ResponseEntity.ok(
                ResponseObject.<TeacherStudentScoreDTO>builder()
                        .success(true)
                        .message("Score retrieved successfully")
                        .data(score)
                        .build()
        );
    }

    // Lưu/cập nhật điểm một học viên
    @PostMapping("/assessments/{assessmentId}/scores")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<TeacherStudentScoreDTO>> saveOrUpdateScore(
            @PathVariable Long assessmentId,
            @Valid @RequestBody ScoreInputDTO scoreInput,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
        TeacherStudentScoreDTO score = teacherGradeService.saveOrUpdateScore(teacherId, assessmentId, scoreInput);
        return ResponseEntity.ok(
                ResponseObject.<TeacherStudentScoreDTO>builder()
                        .success(true)
                        .message("Score saved successfully")
                        .data(score)
                        .build()
        );
    }

    // Lưu/cập nhật điểm hàng loạt
    @PostMapping("/assessments/{assessmentId}/scores/batch")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<List<TeacherStudentScoreDTO>>> batchSaveOrUpdateScores(
            @PathVariable Long assessmentId,
            @Valid @RequestBody BatchScoreInputDTO batchInput,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
        List<TeacherStudentScoreDTO> scores = teacherGradeService.batchSaveOrUpdateScores(teacherId, assessmentId, batchInput);
        return ResponseEntity.ok(
                ResponseObject.<List<TeacherStudentScoreDTO>>builder()
                        .success(true)
                        .message("Scores saved successfully")
                        .data(scores)
                        .build()
        );
    }
}

