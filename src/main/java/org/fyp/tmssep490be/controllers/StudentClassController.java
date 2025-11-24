package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.studentportal.*;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.StudentPortalService;
import org.fyp.tmssep490be.utils.StudentContextHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for Class details in Student Portal
 * Provides endpoints for viewing detailed class information
 */
@RestController
@RequestMapping("/api/v1/student-portal/classes/{classId}")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Student Class Details", description = "Class detail APIs for student portal")
@SecurityRequirement(name = "bearerAuth")
public class StudentClassController {

    private final StudentPortalService studentPortalService;
    private final StudentContextHelper studentContextHelper;

    /**
     * Get detailed information about a specific class
     * GET /api/v1/classes/{classId}
     */
    @GetMapping
    @Operation(
            summary = "Get class details",
            description = "Get detailed information about a specific class including teachers, schedule, and enrollment summary"
    )
    @PreAuthorize("hasAnyRole('STUDENT', 'ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<ClassDetailDTO>> getClassDetail(
            @Parameter(description = "Class ID") @PathVariable Long classId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} retrieving details for class: {}", currentUser.getId(), classId);

        ClassDetailDTO classDetail = studentPortalService.getClassDetail(classId);

        return ResponseEntity.ok(ResponseObject.success(classDetail));
    }

    /**
     * Get sessions for a class including student attendance data
     * GET /api/v1/classes/{classId}/sessions
     */
    @GetMapping("/sessions")
    @Operation(
            summary = "Get class sessions",
            description = "Get all sessions for a class including upcoming, past sessions, and student attendance information"
    )
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<ClassSessionsResponseDTO>> getClassSessions(
            @Parameter(description = "Class ID") @PathVariable Long classId,
            @Parameter(description = "Student ID for attendance data (ignored, lấy từ token)") @RequestParam(required = false) Long studentId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        Long authStudentId = studentContextHelper.getStudentId(currentUser);
        if (studentId != null && !authStudentId.equals(studentId)) {
            log.warn("Overriding requested studentId {} with authenticated studentId {}", studentId, authStudentId);
        }
        log.info("User {} retrieving sessions for class: {} and student: {}", currentUser.getId(), classId, authStudentId);

        ClassSessionsResponseDTO sessions = studentPortalService.getClassSessions(classId, authStudentId);

        return ResponseEntity.ok(ResponseObject.success(sessions));
    }

    /**
     * Get assessments for a class
     * GET /api/v1/classes/{classId}/assessments
     */
    @GetMapping("/assessments")
    @Operation(
            summary = "Get class assessments",
            description = "Get all assessments for a class including tests, assignments, and exams"
    )
    @PreAuthorize("hasAnyRole('STUDENT', 'ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<List<AssessmentDTO>>> getClassAssessments(
            @Parameter(description = "Class ID") @PathVariable Long classId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} retrieving assessments for class: {}", currentUser.getId(), classId);

        List<AssessmentDTO> assessments = studentPortalService.getClassAssessments(classId);

        return ResponseEntity.ok(ResponseObject.success(assessments));
    }

    /**
     * Get assessment scores for a specific student in a class
     * GET /api/v1/classes/{classId}/students/{studentId}/assessment-scores
     */
    @GetMapping("/students/{studentId}/assessment-scores")
    @Operation(
            summary = "Get student assessment scores",
            description = "Get assessment scores for a specific student in a class including grades and feedback"
    )
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<List<StudentAssessmentScoreDTO>>> getStudentAssessmentScores(
            @Parameter(description = "Class ID") @PathVariable Long classId,
            @Parameter(description = "Student ID (ignored, lấy từ token)") @PathVariable Long studentId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        Long authStudentId = studentContextHelper.getStudentId(currentUser);
        if (!authStudentId.equals(studentId)) {
            log.warn("Overriding requested studentId {} with authenticated studentId {}", studentId, authStudentId);
        }
        log.info("User {} retrieving assessment scores for student: {} in class: {}", currentUser.getId(), authStudentId, classId);

        List<StudentAssessmentScoreDTO> scores = studentPortalService.getStudentAssessmentScores(classId, authStudentId);

        return ResponseEntity.ok(ResponseObject.success(scores));
    }

    /**
     * Get classmates for a class
     * GET /api/v1/classes/{classId}/classmates
     */
    @GetMapping("/classmates")
    @Operation(
            summary = "Get classmates",
            description = "Get list of classmates enrolled in the same class"
    )
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<List<ClassmateDTO>>> getClassmates(
            @Parameter(description = "Class ID") @PathVariable Long classId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} retrieving classmates for class: {}", currentUser.getId(), classId);

        // Note: We don't need to verify specific student ID here since this shows general class information
        // The student should already be enrolled in the class (verified in service layer)
        List<ClassmateDTO> classmates = studentPortalService.getClassmates(classId);

        return ResponseEntity.ok(ResponseObject.success(classmates));
    }
}
