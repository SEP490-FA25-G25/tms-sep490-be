package org.fyp.tmssep490be.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.curriculum.*;
import org.fyp.tmssep490be.services.CurriculumService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/curriculum")
@RequiredArgsConstructor
@Slf4j
public class CurriculumController {

        private final CurriculumService curriculumService;

        // Lấy tất cả curriculum với levels
        @GetMapping("/curriculums-with-levels")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<List<CurriculumWithLevelsDTO>>> getAllCurriculumsWithLevels() {
                log.info("Fetching all curriculums with their levels");

                List<CurriculumWithLevelsDTO> curriculums = curriculumService.getAllCurriculumsWithLevels();

                log.info("Successfully retrieved {} curriculums with levels", curriculums.size());
                return ResponseEntity.ok(ResponseObject.<List<CurriculumWithLevelsDTO>>builder()
                                .success(true)
                                .message("Curriculums with levels retrieved successfully")
                                .data(curriculums)
                                .build());
        }

        // Tạo curriculum mới
        @PostMapping("/curriculums")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<CurriculumResponseDTO>> createCurriculum(
                        @RequestBody CreateCurriculumDTO request) {
                log.info("Creating new curriculum: {}", request.getCode());
                CurriculumResponseDTO curriculum = curriculumService.createCurriculum(request);
                return ResponseEntity.ok(ResponseObject.<CurriculumResponseDTO>builder()
                                .success(true)
                                .message("Curriculum created successfully")
                                .data(curriculum)
                                .build());
        }

        // Lấy chi tiết curriculum
        @GetMapping("/curriculums/{id}")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<CurriculumResponseDTO>> getCurriculum(@PathVariable Long id) {
                log.info("Fetching curriculum details for ID: {}", id);
                CurriculumResponseDTO curriculum = curriculumService.getCurriculum(id);
                return ResponseEntity.ok(ResponseObject.<CurriculumResponseDTO>builder()
                                .success(true)
                                .message("Curriculum details retrieved successfully")
                                .data(curriculum)
                                .build());
        }

        // Cập nhật curriculum
        @PutMapping("/curriculums/{id}")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<CurriculumResponseDTO>> updateCurriculum(
                        @PathVariable Long id,
                        @RequestBody CreateCurriculumDTO request) {
                log.info("Updating curriculum with ID: {}", id);
                CurriculumResponseDTO curriculum = curriculumService.updateCurriculum(id, request);
                return ResponseEntity.ok(ResponseObject.<CurriculumResponseDTO>builder()
                                .success(true)
                                .message("Curriculum updated successfully")
                                .data(curriculum)
                                .build());
        }

        // Deactivate curriculum
        @PatchMapping("/curriculums/{id}/deactivate")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<Void>> deactivateCurriculum(@PathVariable Long id) {
                log.info("Deactivating curriculum with ID: {}", id);
                curriculumService.deactivateCurriculum(id);
                return ResponseEntity.ok(ResponseObject.<Void>builder()
                                .success(true)
                                .message("Curriculum deactivated successfully")
                                .build());
        }

        // Reactivate curriculum
        @PatchMapping("/curriculums/{id}/reactivate")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<Void>> reactivateCurriculum(@PathVariable Long id) {
                log.info("Reactivating curriculum with ID: {}", id);
                curriculumService.reactivateCurriculum(id);
                return ResponseEntity.ok(ResponseObject.<Void>builder()
                                .success(true)
                                .message("Curriculum reactivated successfully")
                                .build());
        }

        // Delete curriculum
        @DeleteMapping("/curriculums/{id}")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<Void>> deleteCurriculum(@PathVariable Long id) {
                log.info("Deleting curriculum with ID: {}", id);
                curriculumService.deleteCurriculum(id);
                return ResponseEntity.ok(ResponseObject.<Void>builder()
                                .success(true)
                                .message("Curriculum deleted successfully")
                                .build());
        }

        // Get Subject-PLO Matrix
        @GetMapping("/curriculums/{id}/subject-plo-matrix")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<SubjectPLOMatrixDTO>> getSubjectPLOMatrix(@PathVariable Long id) {
                log.info("Fetching Subject-PLO matrix for curriculum ID: {}", id);
                SubjectPLOMatrixDTO matrix = curriculumService.getSubjectPLOMatrix(id);
                return ResponseEntity.ok(ResponseObject.<SubjectPLOMatrixDTO>builder()
                                .success(true)
                                .message("Subject-PLO matrix retrieved successfully")
                                .data(matrix)
                                .build());
        }

        // Tạo level mới
        @PostMapping("/levels")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<LevelResponseDTO>> createLevel(@RequestBody CreateLevelDTO request) {
                log.info("Creating new level for curriculum ID: {}", request.getCurriculumId());
                LevelResponseDTO level = curriculumService.createLevel(request);
                return ResponseEntity.ok(ResponseObject.<LevelResponseDTO>builder()
                                .success(true)
                                .message("Level created successfully")
                                .data(level)
                                .build());
        }

        // Lấy danh sách levels
        @GetMapping("/levels")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<List<LevelResponseDTO>>> getLevels(
                        @RequestParam(required = false) Long curriculumId) {
                log.info("Fetching levels with curriculumId: {}", curriculumId);
                List<LevelResponseDTO> levels = curriculumService.getLevels(curriculumId);
                return ResponseEntity.ok(ResponseObject.<List<LevelResponseDTO>>builder()
                                .success(true)
                                .message("Levels retrieved successfully")
                                .data(levels)
                                .build());
        }

        // Lấy chi tiết level
        @GetMapping("/levels/{id}")
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

        // Cập nhật level
        @PutMapping("/levels/{id}")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<LevelResponseDTO>> updateLevel(
                        @PathVariable Long id,
                        @RequestBody CreateLevelDTO request) {
                log.info("Updating level with ID: {}", id);
                LevelResponseDTO level = curriculumService.updateLevel(id, request);
                return ResponseEntity.ok(ResponseObject.<LevelResponseDTO>builder()
                                .success(true)
                                .message("Level updated successfully")
                                .data(level)
                                .build());
        }

        // Deactivate level
        @PatchMapping("/levels/{id}/deactivate")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<Void>> deactivateLevel(@PathVariable Long id) {
                log.info("Deactivating level with ID: {}", id);
                curriculumService.deactivateLevel(id);
                return ResponseEntity.ok(ResponseObject.<Void>builder()
                                .success(true)
                                .message("Level deactivated successfully")
                                .build());
        }

        // Reactivate level
        @PatchMapping("/levels/{id}/reactivate")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<Void>> reactivateLevel(@PathVariable Long id) {
                log.info("Reactivating level with ID: {}", id);
                curriculumService.reactivateLevel(id);
                return ResponseEntity.ok(ResponseObject.<Void>builder()
                                .success(true)
                                .message("Level reactivated successfully")
                                .build());
        }

        // Update level sort order
        @PutMapping("/curriculums/{id}/levels/sort-order")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<Void>> updateLevelSortOrder(
                        @PathVariable Long id,
                        @RequestBody List<Long> levelIds) {
                log.info("Updating level sort order for curriculum ID: {}", id);
                curriculumService.updateLevelSortOrder(id, levelIds);
                return ResponseEntity.ok(ResponseObject.<Void>builder()
                                .success(true)
                                .message("Level sort order updated successfully")
                                .build());
        }

        // Delete level
        @DeleteMapping("/levels/{id}")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<Void>> deleteLevel(@PathVariable Long id) {
                log.info("Deleting level with ID: {}", id);
                curriculumService.deleteLevel(id);
                return ResponseEntity.ok(ResponseObject.<Void>builder()
                                .success(true)
                                .message("Level deleted successfully")
                                .build());
        }

        // Get standard timeslot duration
        @GetMapping("/timeslot-duration")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<BigDecimal>> getStandardTimeslotDuration() {
                log.info("Fetching standard timeslot duration");
                BigDecimal duration = curriculumService.getStandardTimeslotDuration();
                return ResponseEntity.ok(ResponseObject.<BigDecimal>builder()
                                .success(true)
                                .message("Standard timeslot duration retrieved successfully")
                                .data(duration)
                                .build());
        }

        // Get all timeslot durations
        @GetMapping("/timeslot-durations")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<List<BigDecimal>>> getAllTimeslotDurations() {
                log.info("Fetching all unique timeslot durations");
                List<BigDecimal> durations = curriculumService.getAllTimeslotDurations();
                return ResponseEntity.ok(ResponseObject.<List<BigDecimal>>builder()
                                .success(true)
                                .message("Timeslot durations retrieved successfully")
                                .data(durations)
                                .build());
        }
}