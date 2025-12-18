package org.fyp.tmssep490be.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.teachergrade.TeacherAssessmentDTO;
import org.fyp.tmssep490be.dtos.teachergrade.TeacherStudentScoreDTO;
import org.fyp.tmssep490be.dtos.teachergrade.ScoreInputDTO;
import org.fyp.tmssep490be.dtos.teachergrade.BatchScoreInputDTO;
import org.fyp.tmssep490be.dtos.teachergrade.GradebookDTO;
import org.fyp.tmssep490be.dtos.teachergrade.ClassGradesSummaryDTO;
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
public class TeacherGradeController {

        private final TeacherGradeService teacherGradeService;
        private final TeacherContextHelper teacherContextHelper;

        // Lấy danh sách bài kiểm tra của lớp
        @GetMapping("/classes/{classId}/assessments")
        @PreAuthorize("hasRole('TEACHER')")
        public ResponseEntity<ResponseObject<List<TeacherAssessmentDTO>>> getClassAssessments(
                        @PathVariable Long classId,
                        @RequestParam(required = false, defaultValue = "all") String filter,
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {
                log.info("User {} requesting assessments for class {} with filter: {}", userPrincipal.getId(), classId,
                                filter);

                Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
                List<TeacherAssessmentDTO> assessments = teacherGradeService.getClassAssessments(teacherId, classId,
                                filter);

                return ResponseEntity.ok(
                                ResponseObject.<List<TeacherAssessmentDTO>>builder()
                                                .success(true)
                                                .message("Assessments retrieved successfully")
                                                .data(assessments)
                                                .build());
        }

        // Lấy điểm của toàn bộ học viên trong bài kiểm tra
        @GetMapping("/assessments/{assessmentId}/scores")
        @PreAuthorize("hasRole('TEACHER')")
        public ResponseEntity<ResponseObject<List<TeacherStudentScoreDTO>>> getAssessmentScores(
                        @PathVariable Long assessmentId,
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {
                Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
                List<TeacherStudentScoreDTO> scores = teacherGradeService.getAssessmentScores(teacherId, assessmentId);
                return ResponseEntity.ok(
                                ResponseObject.<List<TeacherStudentScoreDTO>>builder()
                                                .success(true)
                                                .message("Scores retrieved successfully")
                                                .data(scores)
                                                .build());
        }

        // Lấy điểm của một học viên trong bài kiểm tra
        @GetMapping("/assessments/{assessmentId}/scores/{studentId}")
        @PreAuthorize("hasRole('TEACHER')")
        public ResponseEntity<ResponseObject<TeacherStudentScoreDTO>> getStudentScore(
                        @PathVariable Long assessmentId,
                        @PathVariable Long studentId,
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {
                Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
                TeacherStudentScoreDTO score = teacherGradeService.getStudentScore(teacherId, assessmentId, studentId);
                return ResponseEntity.ok(
                                ResponseObject.<TeacherStudentScoreDTO>builder()
                                                .success(true)
                                                .message("Score retrieved successfully")
                                                .data(score)
                                                .build());
        }

        // Lưu/cập nhật điểm một học viên
        @PostMapping("/assessments/{assessmentId}/scores")
        @PreAuthorize("hasRole('TEACHER')")
        public ResponseEntity<ResponseObject<TeacherStudentScoreDTO>> saveOrUpdateScore(
                        @PathVariable Long assessmentId,
                        @Valid @RequestBody ScoreInputDTO scoreInput,
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {
                Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
                TeacherStudentScoreDTO score = teacherGradeService.saveOrUpdateScore(teacherId, assessmentId,
                                scoreInput);
                return ResponseEntity.ok(
                                ResponseObject.<TeacherStudentScoreDTO>builder()
                                                .success(true)
                                                .message("Score saved successfully")
                                                .data(score)
                                                .build());
        }

        // Lưu/cập nhật điểm hàng loạt
        @PostMapping("/assessments/{assessmentId}/scores/batch")
        @PreAuthorize("hasRole('TEACHER')")
        public ResponseEntity<ResponseObject<List<TeacherStudentScoreDTO>>> batchSaveOrUpdateScores(
                        @PathVariable Long assessmentId,
                        @Valid @RequestBody BatchScoreInputDTO batchInput,
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {
                Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
                List<TeacherStudentScoreDTO> scores = teacherGradeService.batchSaveOrUpdateScores(teacherId,
                                assessmentId, batchInput);
                return ResponseEntity.ok(
                                ResponseObject.<List<TeacherStudentScoreDTO>>builder()
                                                .success(true)
                                                .message("Scores saved successfully")
                                                .data(scores)
                                                .build());
        }

        // Lấy gradebook (ma trận điểm) của lớp
        @GetMapping("/classes/{classId}/gradebook")
        @PreAuthorize("hasRole('TEACHER')")
        public ResponseEntity<ResponseObject<GradebookDTO>> getClassGradebook(
                        @PathVariable Long classId,
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {
                Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
                GradebookDTO gradebook = teacherGradeService.getClassGradebook(teacherId, classId);
                return ResponseEntity.ok(
                                ResponseObject.<GradebookDTO>builder()
                                                .success(true)
                                                .message("Gradebook retrieved successfully")
                                                .data(gradebook)
                                                .build());
        }

        // Lấy tổng quan điểm số của lớp
        @GetMapping("/classes/{classId}/summary")
        @PreAuthorize("hasRole('TEACHER')")
        public ResponseEntity<ResponseObject<ClassGradesSummaryDTO>> getClassGradesSummary(
                        @PathVariable Long classId,
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {
                Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
                ClassGradesSummaryDTO summary = teacherGradeService.getClassGradesSummary(teacherId, classId);
                return ResponseEntity.ok(
                                ResponseObject.<ClassGradesSummaryDTO>builder()
                                                .success(true)
                                                .message("Grades summary retrieved successfully")
                                                .data(summary)
                                                .build());
        }

        // Cập nhật ngày kiểm tra dự kiến/thực tế
        @PutMapping("/assessments/{assessmentId}/dates")
        @PreAuthorize("hasRole('TEACHER')")
        public ResponseEntity<ResponseObject<TeacherAssessmentDTO>> updateAssessmentDates(
                        @PathVariable Long assessmentId,
                        @RequestBody org.fyp.tmssep490be.dtos.teachergrade.UpdateAssessmentDatesDTO request,
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {
                log.info("User {} updating dates for assessment {}", userPrincipal.getId(), assessmentId);

                Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
                TeacherAssessmentDTO updated = teacherGradeService.updateAssessmentDates(
                                teacherId, assessmentId, request.getScheduledDate(), request.getActualDate());

                return ResponseEntity.ok(
                                ResponseObject.<TeacherAssessmentDTO>builder()
                                                .success(true)
                                                .message("Assessment dates updated successfully")
                                                .data(updated)
                                                .build());
        }
}
