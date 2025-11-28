package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.course.CoursePhaseDTO;
import org.fyp.tmssep490be.services.CoursePhaseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for course phase operations
 * Provides endpoints for retrieving course phase information for QA reports and other management tasks.
 */
@RestController
@RequestMapping("/api/v1/phases")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Course Phase Management", description = "Course Phase APIs for QA reports and curriculum management")
@SecurityRequirement(name = "bearerAuth")
public class CoursePhaseController {

    private final CoursePhaseService coursePhaseService;

    /**
     * Get all phases across all courses (for QA reports dropdown)
     * GET /api/v1/phases
     */
    @GetMapping
    @Operation(summary = "Get all course phases", description = "Retrieve all course phases across all courses for QA reports and dropdown selection.")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CENTER_HEAD', 'SUBJECT_LEADER', 'ACADEMIC_AFFAIR', 'QA')")
    public ResponseEntity<ResponseObject<List<CoursePhaseDTO>>> getAllPhases() {
        try {
            List<CoursePhaseDTO> phases = coursePhaseService.getAllPhases();

            ResponseObject<List<CoursePhaseDTO>> response = ResponseObject.<List<CoursePhaseDTO>>builder()
                    .success(true)
                    .message("Lấy danh sách giai đoạn học thành công")
                    .data(phases)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving all course phases", e);

            ResponseObject<List<CoursePhaseDTO>> response = ResponseObject.<List<CoursePhaseDTO>>builder()
                    .success(false)
                    .message("Lỗi khi lấy danh sách giai đoạn học: " + e.getMessage())
                    .data(null)
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get phases for a specific course
     * GET /api/v1/phases/course/{courseId}
     */
    @GetMapping("/course/{courseId}")
    @Operation(summary = "Get phases by course", description = "Retrieve all phases for a specific course, ordered by phase number.")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CENTER_HEAD', 'SUBJECT_LEADER', 'ACADEMIC_AFFAIR', 'QA', 'TEACHER')")
    public ResponseEntity<ResponseObject<List<CoursePhaseDTO>>> getPhasesByCourseId(
            @Parameter(description = "ID of the course") @PathVariable Long courseId) {
        try {
            List<CoursePhaseDTO> phases = coursePhaseService.getPhasesByCourseId(courseId);

            ResponseObject<List<CoursePhaseDTO>> response = ResponseObject.<List<CoursePhaseDTO>>builder()
                    .success(true)
                    .message("Lấy danh sách giai đoạn học theo khóa học thành công")
                    .data(phases)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving phases for course {}", courseId, e);

            ResponseObject<List<CoursePhaseDTO>> response = ResponseObject.<List<CoursePhaseDTO>>builder()
                    .success(false)
                    .message("Lỗi khi lấy danh sách giai đoạn học theo khóa học: " + e.getMessage())
                    .data(null)
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}