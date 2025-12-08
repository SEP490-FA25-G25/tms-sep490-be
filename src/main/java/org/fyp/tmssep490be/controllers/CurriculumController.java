package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Quản lý Chương trình đào tạo", description = "APIs cho Curriculum và Level")
@SecurityRequirement(name = "bearerAuth")
public class CurriculumController {

    private final CurriculumService curriculumService;

    // Lấy tất cả curriculum với levels
    @GetMapping("/curriculums-with-levels")
    @Operation(summary = "Get all curriculums with their levels",
            description = "Retrieve list of curriculums with their levels. Used for dropdowns.")
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
    @Operation(summary = "Create a new curriculum")
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
    @Operation(summary = "Get curriculum details")
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
}