package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.teachergrade.*;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.TeacherGradeService;
import org.fyp.tmssep490be.utils.TeacherContextHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for teacher grade management operations
 */
@RestController
@RequestMapping("/api/v1/teacher/grades")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Teacher Grade Management", description = "APIs for teachers to manage student grades and assessments")
@SecurityRequirement(name = "bearerAuth")
public class TeacherGradeController {

    private final TeacherGradeService teacherGradeService;
    private final TeacherContextHelper teacherContextHelper;

    /**
     * Get list of assessments for a class with optional filter
     * GET /api/v1/teacher/grades/classes/{classId}/assessments
     */
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
        log.info("User {} requesting assessments for class {} with filter: {}", 
                userPrincipal.getId(), classId, filter);
        
        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
        List<TeacherAssessmentDTO> assessments = teacherGradeService.getClassAssessments(teacherId, classId, filter);
        
        return ResponseEntity.ok(ResponseObject.<List<TeacherAssessmentDTO>>builder()
                .success(true)
                .message("Assessments retrieved successfully")
                .data(assessments)
                .build());
    }

    /**
     * Get all student scores for an assessment
     * GET /api/v1/teacher/grades/assessments/{assessmentId}/scores
     */
    @GetMapping("/assessments/{assessmentId}/scores")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(
            summary = "Get assessment scores",
            description = "Get all student scores for an assessment"
    )
    public ResponseEntity<ResponseObject<List<TeacherStudentScoreDTO>>> getAssessmentScores(
            @Parameter(description = "Assessment ID") @PathVariable Long assessmentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("User {} requesting scores for assessment {}", userPrincipal.getId(), assessmentId);
        
        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
        List<TeacherStudentScoreDTO> scores = teacherGradeService.getAssessmentScores(teacherId, assessmentId);
        
        return ResponseEntity.ok(ResponseObject.<List<TeacherStudentScoreDTO>>builder()
                .success(true)
                .message("Scores retrieved successfully")
                .data(scores)
                .build());
    }

    /**
     * Get a specific student's score for an assessment
     * GET /api/v1/teacher/grades/assessments/{assessmentId}/scores/{studentId}
     */
    @GetMapping("/assessments/{assessmentId}/scores/{studentId}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(
            summary = "Get student score",
            description = "Get a specific student's score for an assessment"
    )
    public ResponseEntity<ResponseObject<TeacherStudentScoreDTO>> getStudentScore(
            @Parameter(description = "Assessment ID") @PathVariable Long assessmentId,
            @Parameter(description = "Student ID") @PathVariable Long studentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("User {} requesting score for assessment {} and student {}", 
                userPrincipal.getId(), assessmentId, studentId);
        
        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
        TeacherStudentScoreDTO score = teacherGradeService.getStudentScore(teacherId, assessmentId, studentId);
        
        return ResponseEntity.ok(ResponseObject.<TeacherStudentScoreDTO>builder()
                .success(true)
                .message("Score retrieved successfully")
                .data(score)
                .build());
    }

    /**
     * Save or update a single student score
     * POST /api/v1/teacher/grades/assessments/{assessmentId}/scores
     */
    @PostMapping("/assessments/{assessmentId}/scores")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(
            summary = "Save or update student score",
            description = "Save or update a single student score for an assessment"
    )
    public ResponseEntity<ResponseObject<TeacherStudentScoreDTO>> saveOrUpdateScore(
            @Parameter(description = "Assessment ID") @PathVariable Long assessmentId,
            @Valid @RequestBody ScoreInputDTO scoreInput,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("User {} saving/updating score for assessment {} and student {}", 
                userPrincipal.getId(), assessmentId, scoreInput.getStudentId());
        
        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
        TeacherStudentScoreDTO score = teacherGradeService.saveOrUpdateScore(teacherId, assessmentId, scoreInput);
        
        return ResponseEntity.ok(ResponseObject.<TeacherStudentScoreDTO>builder()
                .success(true)
                .message("Score saved successfully")
                .data(score)
                .build());
    }

    /**
     * Batch save or update multiple student scores
     * POST /api/v1/teacher/grades/assessments/{assessmentId}/scores/batch
     */
    @PostMapping("/assessments/{assessmentId}/scores/batch")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(
            summary = "Batch save or update scores",
            description = "Save or update multiple student scores for an assessment in one request"
    )
    public ResponseEntity<ResponseObject<List<TeacherStudentScoreDTO>>> batchSaveOrUpdateScores(
            @Parameter(description = "Assessment ID") @PathVariable Long assessmentId,
            @Valid @RequestBody BatchScoreInputDTO batchInput,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("User {} batch saving/updating scores for assessment {} ({} scores)", 
                userPrincipal.getId(), assessmentId, 
                batchInput.getScores() != null ? batchInput.getScores().size() : 0);
        
        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
        List<TeacherStudentScoreDTO> scores = teacherGradeService.batchSaveOrUpdateScores(teacherId, assessmentId, batchInput);
        
        return ResponseEntity.ok(ResponseObject.<List<TeacherStudentScoreDTO>>builder()
                .success(true)
                .message("Scores saved successfully")
                .data(scores)
                .build());
    }

    /**
     * Get class grades summary statistics
     * GET /api/v1/teacher/grades/classes/{classId}/summary
     */
    @GetMapping("/classes/{classId}/summary")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(
            summary = "Get class grades summary",
            description = "Get class grades summary statistics including averages, distributions, and top/bottom students"
    )
    public ResponseEntity<ResponseObject<ClassGradesSummaryDTO>> getClassGradesSummary(
            @Parameter(description = "Class ID") @PathVariable Long classId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("User {} requesting grades summary for class {}", userPrincipal.getId(), classId);
        
        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
        ClassGradesSummaryDTO summary = teacherGradeService.getClassGradesSummary(teacherId, classId);
        
        return ResponseEntity.ok(ResponseObject.<ClassGradesSummaryDTO>builder()
                .success(true)
                .message("Grades summary retrieved successfully")
                .data(summary)
                .build());
    }

    /**
     * Get gradebook (matrix view) for a class
     * GET /api/v1/teacher/grades/classes/{classId}/gradebook
     */
    @GetMapping("/classes/{classId}/gradebook")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(
            summary = "Get class gradebook",
            description = "Get gradebook matrix view with all students and their scores for all assessments"
    )
    public ResponseEntity<ResponseObject<GradebookDTO>> getClassGradebook(
            @Parameter(description = "Class ID") @PathVariable Long classId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("User {} requesting gradebook for class {}", userPrincipal.getId(), classId);
        
        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
        GradebookDTO gradebook = teacherGradeService.getClassGradebook(teacherId, classId);
        
        return ResponseEntity.ok(ResponseObject.<GradebookDTO>builder()
                .success(true)
                .message("Gradebook retrieved successfully")
                .data(gradebook)
                .build());
    }
}

