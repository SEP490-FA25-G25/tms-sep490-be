package org.fyp.tmssep490be.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.subject.SubjectPhaseDTO;
import org.fyp.tmssep490be.services.SubjectPhaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/phases")
@RequiredArgsConstructor
@Slf4j
public class SubjectPhaseController {

    private final SubjectPhaseService subjectPhaseService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CENTER_HEAD', 'SUBJECT_LEADER', 'ACADEMIC_AFFAIR', 'QA')")
    public ResponseEntity<ResponseObject<List<SubjectPhaseDTO>>> getAllPhases() {
        List<SubjectPhaseDTO> phases = subjectPhaseService.getAllPhases();

        return ResponseEntity.ok(ResponseObject.<List<SubjectPhaseDTO>>builder()
                .success(true)
                .message("Lấy danh sách giai đoạn học thành công")
                .data(phases)
                .build());
    }

    @GetMapping("/subject/{subjectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CENTER_HEAD', 'SUBJECT_LEADER', 'ACADEMIC_AFFAIR', 'QA', 'TEACHER')")
    public ResponseEntity<ResponseObject<List<SubjectPhaseDTO>>> getPhasesBySubjectId(@PathVariable Long subjectId) {
        List<SubjectPhaseDTO> phases = subjectPhaseService.getPhasesBySubjectId(subjectId);

        return ResponseEntity.ok(ResponseObject.<List<SubjectPhaseDTO>>builder()
                .success(true)
                .message("Lấy danh sách giai đoạn học theo môn học thành công")
                .data(phases)
                .build());
    }
}
