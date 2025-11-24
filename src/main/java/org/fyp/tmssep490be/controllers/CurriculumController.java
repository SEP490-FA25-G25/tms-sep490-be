package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.curriculum.SubjectWithLevelsDTO;
import org.fyp.tmssep490be.services.CurriculumService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.fyp.tmssep490be.dtos.curriculum.*;
import java.util.List;

/**
 * Controller for curriculum-related operations
 * Provides subject and level information for dropdown/select components
 */
@RestController
@RequestMapping("/api/v1/curriculum")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Curriculum Management", description = "Curriculum APIs for subjects and levels")
@SecurityRequirement(name = "bearerAuth")
public class CurriculumController {

        private final CurriculumService curriculumService;

        /**
         * Get all subjects with their levels
         * Used for dropdown/select components when creating student skill assessments
         */
        @GetMapping("/subjects-with-levels")
        @Operation(summary = "Get all subjects with their levels", description = "Retrieve list of subjects with their levels. Used for selecting levels in student skill assessments.")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<List<SubjectWithLevelsDTO>>> getAllSubjectsWithLevels() {
                log.info("Fetching all subjects with their levels");
                log.info("User authorities: {}",
                                org.springframework.security.core.context.SecurityContextHolder.getContext()
                                                .getAuthentication().getAuthorities());

                List<SubjectWithLevelsDTO> subjects = curriculumService.getAllSubjectsWithLevels();

                log.info("Successfully retrieved {} subjects with levels", subjects.size());

                return ResponseEntity.ok(ResponseObject.<List<SubjectWithLevelsDTO>>builder()
                                .success(true)
                                .message("Subjects with levels retrieved successfully")
                                .data(subjects)
                                .build());
        }

        @PostMapping("/subjects")
        @Operation(summary = "Create a new subject")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<SubjectResponseDTO>> createSubject(@RequestBody CreateSubjectDTO request) {
                log.info("Creating new subject: {}", request.getCode());
                SubjectResponseDTO subject = curriculumService.createSubject(request);
                return ResponseEntity.ok(ResponseObject.<SubjectResponseDTO>builder()
                                .success(true)
                                .message("Subject created successfully")
                                .data(subject)
                                .build());
        }

        @PostMapping("/levels")
        @Operation(summary = "Create a new level")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<LevelResponseDTO>> createLevel(@RequestBody CreateLevelDTO request) {
                log.info("Creating new level for subject ID: {}", request.getSubjectId());
                LevelResponseDTO level = curriculumService.createLevel(request);
                return ResponseEntity.ok(ResponseObject.<LevelResponseDTO>builder()
                                .success(true)
                                .message("Level created successfully")
                                .data(level)
                                .build());
        }

        @GetMapping("/levels")
        @Operation(summary = "Get all levels", description = "Retrieve all levels, optionally filtered by subject.")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<List<LevelResponseDTO>>> getLevels(
                        @RequestParam(required = false) Long subjectId) {
                log.info("Fetching levels with subjectId: {}", subjectId);
                List<LevelResponseDTO> levels = curriculumService.getLevels(subjectId);
                return ResponseEntity.ok(ResponseObject.<List<LevelResponseDTO>>builder()
                                .success(true)
                                .message("Levels retrieved successfully")
                                .data(levels)
                                .build());
        }

        @GetMapping("/timeslot-duration")
        @Operation(summary = "Get standard timeslot duration", description = "Retrieve standard timeslot duration in hours.")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<java.math.BigDecimal>> getStandardTimeslotDuration() {
                log.info("Fetching standard timeslot duration");
                java.math.BigDecimal duration = curriculumService.getStandardTimeslotDuration();
                return ResponseEntity.ok(ResponseObject.<java.math.BigDecimal>builder()
                                .success(true)
                                .message("Standard timeslot duration retrieved successfully")
                                .data(duration)
                                .build());
        }
}