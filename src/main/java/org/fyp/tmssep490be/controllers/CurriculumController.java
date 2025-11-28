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

        @GetMapping("/subjects/{id}")
        @Operation(summary = "Get subject details")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<SubjectResponseDTO>> getSubject(@PathVariable Long id) {
                log.info("Fetching subject details for ID: {}", id);
                SubjectResponseDTO subject = curriculumService.getSubject(id);
                return ResponseEntity.ok(ResponseObject.<SubjectResponseDTO>builder()
                                .success(true)
                                .message("Subject details retrieved successfully")
                                .data(subject)
                                .build());
        }

        @PutMapping("/subjects/{id}")
        @Operation(summary = "Update subject")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<SubjectResponseDTO>> updateSubject(@PathVariable Long id,
                        @RequestBody CreateSubjectDTO request) {
                log.info("Updating subject with ID: {}", id);
                SubjectResponseDTO subject = curriculumService.updateSubject(id, request);
                return ResponseEntity.ok(ResponseObject.<SubjectResponseDTO>builder()
                                .success(true)
                                .message("Subject updated successfully")
                                .data(subject)
                                .build());
        }

        @PatchMapping("/subjects/{id}/deactivate")
        @Operation(summary = "Deactivate subject")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<Void>> deactivateSubject(@PathVariable Long id) {
                log.info("Deactivating subject with ID: {}", id);
                curriculumService.deactivateSubject(id);
                return ResponseEntity.ok(ResponseObject.<Void>builder()
                                .success(true)
                                .message("Subject deactivated successfully")
                                .build());
        }

        @PatchMapping("/subjects/{id}/reactivate")
        @Operation(summary = "Reactivate subject")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<Void>> reactivateSubject(@PathVariable Long id) {
                log.info("Reactivating subject with ID: {}", id);
                curriculumService.reactivateSubject(id);
                return ResponseEntity.ok(ResponseObject.<Void>builder()
                                .success(true)
                                .message("Subject reactivated successfully")
                                .build());
        }

        @GetMapping("/levels/{id}")
        @Operation(summary = "Get level details")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<LevelResponseDTO>> getLevel(@PathVariable Long id) {
                log.info("Fetching level details for ID: {}", id);
                LevelResponseDTO level = curriculumService.getLevel(id);
                return ResponseEntity.ok(ResponseObject.<LevelResponseDTO>builder()
                                .success(true)
                                .message("Level details retrieved successfully")
                                .data(level)
                                .build());
        }

        @PutMapping("/levels/{id}")
        @Operation(summary = "Update level")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<LevelResponseDTO>> updateLevel(@PathVariable Long id,
                        @RequestBody CreateLevelDTO request) {
                log.info("Updating level with ID: {}", id);
                LevelResponseDTO level = curriculumService.updateLevel(id, request);
                return ResponseEntity.ok(ResponseObject.<LevelResponseDTO>builder()
                                .success(true)
                                .message("Level updated successfully")
                                .data(level)
                                .build());
        }

        @PatchMapping("/levels/{id}/deactivate")
        @Operation(summary = "Deactivate level")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<Void>> deactivateLevel(@PathVariable Long id) {
                log.info("Deactivating level with ID: {}", id);
                curriculumService.deactivateLevel(id);
                return ResponseEntity.ok(ResponseObject.<Void>builder()
                                .success(true)
                                .message("Level deactivated successfully")
                                .build());
        }

        @PatchMapping("/levels/{id}/reactivate")
        @Operation(summary = "Reactivate level")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<Void>> reactivateLevel(@PathVariable Long id) {
                log.info("Reactivating level with ID: {}", id);
                curriculumService.reactivateLevel(id);
                return ResponseEntity.ok(ResponseObject.<Void>builder()
                                .success(true)
                                .message("Level reactivated successfully")
                                .build());
        }

        @PutMapping("/subjects/{id}/levels/sort-order")
        @Operation(summary = "Update level sort order for a subject")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<Void>> updateLevelSortOrder(
                        @PathVariable Long id,
                        @RequestBody List<Long> levelIds) {
                log.info("Updating level sort order for subject ID: {}", id);
                curriculumService.updateLevelSortOrder(id, levelIds);
                return ResponseEntity.ok(ResponseObject.<Void>builder()
                                .success(true)
                                .message("Level sort order updated successfully")
                                .build());
        }
}