package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.subject.SubjectDTO;
import org.fyp.tmssep490be.services.SubjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    public ResponseEntity<ResponseObject<List<SubjectDTO>>> getAllSubjects(
            @RequestParam(required = false) Long curriculumId,
            @RequestParam(required = false) Long levelId) {
        log.info("Getting all subjects for dropdown with filters - curriculumId: {}, levelId: {}", curriculumId, levelId);

        List<SubjectDTO> subjects = subjectService.getAllSubjects(curriculumId, levelId);

        return ResponseEntity.ok(ResponseObject.<List<SubjectDTO>>builder()
                .success(true)
                .message("Subjects retrieved successfully")
                .data(subjects)
                .build());
    }
}
