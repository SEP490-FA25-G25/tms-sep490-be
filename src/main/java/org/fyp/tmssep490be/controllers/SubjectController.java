package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.subject.*;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.SubjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Subject Management", description = "Subject APIs")
@SecurityRequirement(name = "bearerAuth")
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    @Operation(summary = "Get all subjects", description = "Lấy danh sách môn học")
    public ResponseEntity<ResponseObject<List<SubjectDTO>>> getAllSubjects(
            @RequestParam(required = false) Long curriculumId,
            @RequestParam(required = false) Long levelId) {
        log.info("Getting all subjects - curriculumId: {}, levelId: {}", curriculumId, levelId);
        List<SubjectDTO> subjects = subjectService.getAllSubjects(curriculumId, levelId);
        return ResponseEntity.ok(ResponseObject.<List<SubjectDTO>>builder()
                .success(true)
                .message("Subjects retrieved successfully")
                .data(subjects)
                .build());
    }

    @PostMapping
    @Operation(summary = "Create subject", description = "Tạo môn học mới")
    public ResponseEntity<ResponseObject<SubjectDetailDTO>> createSubject(
            @RequestBody CreateSubjectRequestDTO request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("Creating new subject: {}", request.getBasicInfo().getName());
        Long userId = currentUser != null ? currentUser.getId() : null;
        SubjectDetailDTO result = subjectService.createSubject(request, userId);
        return ResponseEntity.ok(ResponseObject.<SubjectDetailDTO>builder()
                .success(true)
                .message("Subject created successfully")
                .data(result)
                .build());
    }

    @GetMapping("/next-version")
    @Operation(summary = "Get next version number", description = "Get the next available version number for a subject code pattern")
    public ResponseEntity<ResponseObject<Integer>> getNextVersion(
            @RequestParam String subjectCode,
            @RequestParam String levelCode,
            @RequestParam Integer year) {
        log.info("Getting next version for {}-{}-{}", subjectCode, levelCode, year);
        Integer nextVersion = subjectService.getNextVersionNumber(subjectCode, levelCode, year);
        return ResponseEntity.ok(ResponseObject.<Integer>builder()
                .success(true)
                .message("Next version number retrieved")
                .data(nextVersion)
                .build());
    }

}