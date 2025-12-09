package org.fyp.tmssep490be.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.studentportal.AssessmentDTO;
import org.fyp.tmssep490be.dtos.studentportal.ClassSessionsResponseDTO;
import org.fyp.tmssep490be.dtos.studentportal.StudentAssessmentScoreDTO;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.StudentPortalService;
import org.fyp.tmssep490be.utils.StudentContextHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/student-portal/classes/{classId}")
@RequiredArgsConstructor
@Slf4j
public class StudentClassController {

    private final StudentPortalService studentPortalService;
    private final StudentContextHelper studentContextHelper;

    @GetMapping("/sessions")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<ClassSessionsResponseDTO>> getClassSessions(
             @PathVariable Long classId,
             @RequestParam(required = false) Long studentId,
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

    @GetMapping("/assessments")
    @PreAuthorize("hasAnyRole('STUDENT','ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<List<AssessmentDTO>>> getClassAssessments(
            @PathVariable Long classId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} retrieving assessments for class: {}", currentUser.getId(), classId);

        List<AssessmentDTO> assessments = studentPortalService.getClassAssessments(classId);

        return ResponseEntity.ok(ResponseObject.success(assessments));
    }

    @GetMapping("/students/{studentId}/assessment-scores")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<List<StudentAssessmentScoreDTO>>> getStudentAssessmentScores(
            @PathVariable Long classId,
            @PathVariable Long studentId,
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
}
