package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.subject.MaterialHierarchyDTO;
import org.fyp.tmssep490be.dtos.subject.SubjectDetailDTO;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.SubjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Course Management", description = "Course APIs (tương thích với frontend - Course = Subject trong DB mới)")
@SecurityRequirement(name = "bearerAuth")
public class CourseController {

    private final SubjectService subjectService;

    // Endpoint để lấy giáo trình của course
    @GetMapping("/{courseId}/syllabus")
    @PreAuthorize("hasRole('TEACHER') or hasRole('STUDENT') or hasRole('ACADEMIC_AFFAIR')")
    @Operation(summary = "Get course syllabus", description = "Lấy giáo trình của course (courseId = subjectId trong DB mới)")
    public ResponseEntity<ResponseObject<SubjectDetailDTO>> getCourseSyllabus(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} requesting syllabus for course (subjectId) {}", 
                currentUser != null ? currentUser.getId() : null, courseId);

        SubjectDetailDTO syllabus = subjectService.getSubjectSyllabus(courseId);

        return ResponseEntity.ok(ResponseObject.<SubjectDetailDTO>builder()
                .success(true)
                .message("Course syllabus retrieved successfully")
                .data(syllabus)
                .build());
    }

    // Endpoint để lấy materials hierarchy của course
    @GetMapping("/{courseId}/materials")
    @PreAuthorize("hasRole('TEACHER') or hasRole('STUDENT') or hasRole('ACADEMIC_AFFAIR')")
    @Operation(summary = "Get course materials hierarchy", description = "Lấy danh sách materials của course theo hierarchy (courseId = subjectId trong DB mới)")
    public ResponseEntity<ResponseObject<MaterialHierarchyDTO>> getCourseMaterials(
            @PathVariable Long courseId,
            @RequestParam(required = false) Long studentId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} requesting materials for course (subjectId) {}, studentId: {}", 
                currentUser != null ? currentUser.getId() : null, courseId, studentId);

        MaterialHierarchyDTO materials = subjectService.getSubjectMaterials(courseId, studentId);

        return ResponseEntity.ok(ResponseObject.<MaterialHierarchyDTO>builder()
                .success(true)
                .message("Course materials retrieved successfully")
                .data(materials)
                .build());
    }
}

