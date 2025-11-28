package org.fyp.tmssep490be.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.subject.SubjectDetailDTO;
import org.fyp.tmssep490be.dtos.subject.SubjectSummaryDTO;
import org.fyp.tmssep490be.entities.enums.SubjectStatus;
import org.fyp.tmssep490be.services.SubjectAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/subjects")
@RequiredArgsConstructor
@Slf4j
public class SubjectAdminController {

    private final SubjectAdminService subjectAdminService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseObject<List<SubjectSummaryDTO>>> getSubjects(
            @RequestParam(value = "status", required = false) SubjectStatus status,
            @RequestParam(value = "search", required = false) String search
    ) {
        log.info("Admin requested subject summaries with status={} search={}", status, search);
        List<SubjectSummaryDTO> summaries = subjectAdminService.getSubjectSummaries(status, search);
        return ResponseEntity.ok(ResponseObject.success("Subjects retrieved successfully", summaries));
    }

    @GetMapping("/{subjectId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseObject<SubjectDetailDTO>> getSubjectDetail(
            @PathVariable Long subjectId
    ) {
        log.info("Admin requested subject detail for id={}", subjectId);
        SubjectDetailDTO detailDTO = subjectAdminService.getSubjectDetail(subjectId);
        return ResponseEntity.ok(ResponseObject.success("Subject detail retrieved successfully", detailDTO));
    }
}

