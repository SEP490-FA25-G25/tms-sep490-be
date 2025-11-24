package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.CourseDTO;
import org.fyp.tmssep490be.dtos.course.CourseDetailDTO;
import org.fyp.tmssep490be.dtos.course.CreateCourseRequestDTO;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.services.CourseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for course operations
 * Provides endpoints for retrieving course information for dropdowns
 */
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Course Management", description = "Course APIs for dropdown selection")
@SecurityRequirement(name = "bearerAuth")
public class CourseController {

    private final CourseService courseService;

    /**
     * Get all courses for dropdown selection
     * GET /api/v1/courses
     */
    @GetMapping
    @Operation(summary = "Get all courses", description = "Retrieve all courses for dropdown selection. Returns id, name, and code.")
    public ResponseEntity<ResponseObject<List<CourseDTO>>> getAllCourses(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long levelId) {
        log.info("Getting all courses for dropdown with filters - subjectId: {}, levelId: {}", subjectId, levelId);

        List<CourseDTO> courses = courseService.getAllCourses(subjectId, levelId);

        return ResponseEntity.ok(ResponseObject.<List<CourseDTO>>builder()
                .success(true)
                .message("Courses retrieved successfully")
                .data(courses)
                .build());
    }

    @PostMapping
    @Operation(summary = "Create a new course (Syllabus)")
    @PreAuthorize("hasRole('SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<CourseDTO>> createCourse(@RequestBody CreateCourseRequestDTO request) {
        log.info("Creating new course: {}", request.getBasicInfo().getName());
        CourseDTO createdCourse = courseService.createCourse(request);
        return ResponseEntity.ok(ResponseObject.<CourseDTO>builder()
                .success(true)
                .message("Course created successfully")
                .data(createdCourse)
                .build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get course details")
    @PreAuthorize("hasRole('SUBJECT_LEADER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ResponseObject<CourseDetailDTO>> getCourseDetails(@PathVariable Long id) {
        log.info("Getting course details for id: {}", id);
        CourseDetailDTO course = courseService.getCourseDetails(id);
        return ResponseEntity.ok(ResponseObject.<CourseDetailDTO>builder()
                .success(true)
                .message("Course details retrieved successfully")
                .data(course)
                .build());
    }
}
