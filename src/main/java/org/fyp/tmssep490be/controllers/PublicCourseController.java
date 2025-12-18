package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.publicapi.PublicBranchDTO;
import org.fyp.tmssep490be.dtos.publicapi.PublicCourseCatalogDTO;
import org.fyp.tmssep490be.dtos.publicapi.PublicCourseDTO;
import org.fyp.tmssep490be.dtos.publicapi.PublicCourseSimpleDTO;
import org.fyp.tmssep490be.dtos.publicapi.PublicScheduleDTO;
import org.fyp.tmssep490be.services.PublicCourseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public API Controller for landing page
 * No authentication required
 */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Public API", description = "Public endpoints for landing page - no authentication required")
public class PublicCourseController {

    private final PublicCourseService publicCourseService;

    @GetMapping("/branches")
    @Operation(summary = "Get public branches", description = "Get all active branches for consultation form dropdown")
    public ResponseEntity<ResponseObject<List<PublicBranchDTO>>> getPublicBranches() {
        log.info("Public API: Getting branches for consultation form");
        List<PublicBranchDTO> branches = publicCourseService.getPublicBranches();
        return ResponseEntity.ok(ResponseObject.<List<PublicBranchDTO>>builder()
                .success(true)
                .message("Branches retrieved successfully")
                .data(branches)
                .build());
    }

    @GetMapping("/courses-list")
    @Operation(summary = "Get simple courses list", description = "Get simple active courses list for consultation form dropdown")
    public ResponseEntity<ResponseObject<List<PublicCourseSimpleDTO>>> getSimpleCoursesList() {
        log.info("Public API: Getting simple courses list for consultation form");
        List<PublicCourseSimpleDTO> courses = publicCourseService.getSimpleCourseList();
        return ResponseEntity.ok(ResponseObject.<List<PublicCourseSimpleDTO>>builder()
                .success(true)
                .message("Courses list retrieved successfully")
                .data(courses)
                .build());
    }

    @GetMapping("/courses")
    @Operation(summary = "Get course catalog", description = "Get all active courses grouped by curriculum for landing page")
    public ResponseEntity<ResponseObject<PublicCourseCatalogDTO>> getCourseCatalog() {
        log.info("Public API: Getting course catalog for landing page");
        PublicCourseCatalogDTO catalog = publicCourseService.getCourseCatalog();
        return ResponseEntity.ok(ResponseObject.<PublicCourseCatalogDTO>builder()
                .success(true)
                .message("Course catalog retrieved successfully")
                .data(catalog)
                .build());
    }

    @GetMapping("/courses/{id}")
    @Operation(summary = "Get course detail", description = "Get active course detail by ID for public course detail page")
    public ResponseEntity<ResponseObject<PublicCourseDTO>> getCourseDetail(@PathVariable Long id) {
        log.info("Public API: Getting course detail for ID: {}", id);
        try {
            PublicCourseDTO course = publicCourseService.getCourseDetail(id);
            return ResponseEntity.ok(ResponseObject.<PublicCourseDTO>builder()
                    .success(true)
                    .message("Course detail retrieved successfully")
                    .data(course)
                    .build());
        } catch (RuntimeException e) {
            log.warn("Course not found or not available: {}", id);
            return ResponseEntity.ok(ResponseObject.<PublicCourseDTO>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @GetMapping("/schedules")
    @Operation(summary = "Get upcoming schedules", description = "Get upcoming class schedules for public schedule page")
    public ResponseEntity<ResponseObject<List<PublicScheduleDTO>>> getUpcomingSchedules() {
        log.info("Public API: Getting upcoming schedules for public schedule page");
        List<PublicScheduleDTO> schedules = publicCourseService.getUpcomingSchedules();
        return ResponseEntity.ok(ResponseObject.<List<PublicScheduleDTO>>builder()
                .success(true)
                .message("Schedules retrieved successfully")
                .data(schedules)
                .build());
    }
}
