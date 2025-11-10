package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.CourseDTO;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.services.CourseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @Operation(
            summary = "Get all courses",
            description = "Retrieve all courses for dropdown selection. Returns id, name, and code."
    )
    public ResponseEntity<ResponseObject<List<CourseDTO>>> getAllCourses() {
        log.info("Getting all courses for dropdown");

        List<CourseDTO> courses = courseService.getAllCourses();

        return ResponseEntity.ok(ResponseObject.<List<CourseDTO>>builder()
                .success(true)
                .message("Courses retrieved successfully")
                .data(courses)
                .build());
    }
}
