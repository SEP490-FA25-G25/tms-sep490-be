package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.publicapi.PublicBranchDTO;
import org.fyp.tmssep490be.dtos.publicapi.PublicSubjectCatalogDTO;
import org.fyp.tmssep490be.dtos.publicapi.PublicSubjectDTO;
import org.fyp.tmssep490be.dtos.publicapi.PublicSubjectSimpleDTO;
import org.fyp.tmssep490be.dtos.publicapi.PublicScheduleDTO;
import org.fyp.tmssep490be.services.PublicSubjectService;
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
public class PublicSubjectController {

    private final PublicSubjectService publicSubjectService;

    @GetMapping("/branches")
    @Operation(summary = "Get public branches", description = "Get all active branches for consultation form dropdown")
    public ResponseEntity<ResponseObject<List<PublicBranchDTO>>> getPublicBranches() {
        log.info("Public API: Getting branches for consultation form");
        List<PublicBranchDTO> branches = publicSubjectService.getPublicBranches();
        return ResponseEntity.ok(ResponseObject.<List<PublicBranchDTO>>builder()
                .success(true)
                .message("Branches retrieved successfully")
                .data(branches)
                .build());
    }

    @GetMapping("/subjects-list")
    @Operation(summary = "Get simple subjects list", description = "Get simple active subjects list for consultation form dropdown")
    public ResponseEntity<ResponseObject<List<PublicSubjectSimpleDTO>>> getSimpleSubjectsList() {
        log.info("Public API: Getting simple subjects list for consultation form");
        List<PublicSubjectSimpleDTO> subjects = publicSubjectService.getSimpleSubjectList();
        return ResponseEntity.ok(ResponseObject.<List<PublicSubjectSimpleDTO>>builder()
                .success(true)
                .message("Subjects list retrieved successfully")
                .data(subjects)
                .build());
    }

    @GetMapping("/subjects")
    @Operation(summary = "Get subject catalog", description = "Get all active subjects grouped by curriculum for landing page")
    public ResponseEntity<ResponseObject<PublicSubjectCatalogDTO>> getSubjectCatalog() {
        log.info("Public API: Getting subject catalog for landing page");
        PublicSubjectCatalogDTO catalog = publicSubjectService.getSubjectCatalog();
        return ResponseEntity.ok(ResponseObject.<PublicSubjectCatalogDTO>builder()
                .success(true)
                .message("Subject catalog retrieved successfully")
                .data(catalog)
                .build());
    }

    @GetMapping("/subjects/{id}")
    @Operation(summary = "Get subject detail", description = "Get active subject detail by ID for public course detail page")
    public ResponseEntity<ResponseObject<PublicSubjectDTO>> getSubjectDetail(@PathVariable Long id) {
        log.info("Public API: Getting subject detail for ID: {}", id);
        try {
            PublicSubjectDTO subject = publicSubjectService.getSubjectDetail(id);
            return ResponseEntity.ok(ResponseObject.<PublicSubjectDTO>builder()
                    .success(true)
                    .message("Subject detail retrieved successfully")
                    .data(subject)
                    .build());
        } catch (RuntimeException e) {
            log.warn("Subject not found or not available: {}", id);
            return ResponseEntity.ok(ResponseObject.<PublicSubjectDTO>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @GetMapping("/schedules")
    @Operation(summary = "Get upcoming schedules", description = "Get upcoming class schedules for public schedule page")
    public ResponseEntity<ResponseObject<List<PublicScheduleDTO>>> getUpcomingSchedules() {
        log.info("Public API: Getting upcoming schedules for public schedule page");
        List<PublicScheduleDTO> schedules = publicSubjectService.getUpcomingSchedules();
        return ResponseEntity.ok(ResponseObject.<List<PublicScheduleDTO>>builder()
                .success(true)
                .message("Schedules retrieved successfully")
                .data(schedules)
                .build());
    }
}
