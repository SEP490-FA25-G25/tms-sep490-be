package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.qa.QAClassDetailDTO;
import org.fyp.tmssep490be.dtos.qa.QAClassListItemDTO;
import org.fyp.tmssep490be.dtos.qa.QAClassScoresDTO;
import org.fyp.tmssep490be.dtos.qa.QASessionListResponse;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.QAService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/qa")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "QA", description = "Quality Assurance APIs")
public class QAController {

    private final QAService qaService;


    @GetMapping("/classes")
    @PreAuthorize("hasAnyRole('QA','MANAGER','ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<Page<QAClassListItemDTO>>> getQAClasses(
        @RequestParam(required = false) List<Long> branchIds,

        @RequestParam(required = false) String status,

        @RequestParam(required = false) String search,

        @RequestParam(defaultValue = "0") int page,

        @RequestParam(defaultValue = "20") int size,

        @RequestParam(defaultValue = "startDate") String sort,

        @RequestParam(defaultValue = "desc") String sortDir,

        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requesting QA classes: branchIds={}, status={}, search={}",
                 currentUser.getId(), branchIds, status, search);

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));

        Page<QAClassListItemDTO> classes = qaService.getQAClasses(
                branchIds, status, search, pageable, currentUser.getId()
        );

        return ResponseEntity.ok(ResponseObject.<Page<QAClassListItemDTO>>builder()
            .success(true)
            .message("Lấy danh sách lớp học thành công")
            .data(classes)
            .build());
    }

    @GetMapping("/classes/{classId}")
    @PreAuthorize("hasRole('QA')")
    public ResponseEntity<ResponseObject<QAClassDetailDTO>> getQAClassDetail(
        @PathVariable Long classId,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requesting QA class detail for classId={}", currentUser.getId(), classId);

        QAClassDetailDTO classDetail = qaService.getQAClassDetail(classId, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<QAClassDetailDTO>builder()
            .success(true)
            .message("Lấy chi tiết lớp học thành công")
            .data(classDetail)
            .build());
    }

    @GetMapping("/classes/{classId}/sessions")
    @PreAuthorize("hasRole('QA')")
    public ResponseEntity<ResponseObject<QASessionListResponse>> getQASessionList(
        @PathVariable Long classId,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requesting QA session list for classId={}", currentUser.getId(), classId);

        QASessionListResponse sessions = qaService.getQASessionList(classId, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<QASessionListResponse>builder()
            .success(true)
            .message("Lấy danh sách buổi học thành công")
            .data(sessions)
            .build());
    }

    @GetMapping("/classes/{classId}/scores")
    @PreAuthorize("hasRole('QA')")
    public ResponseEntity<ResponseObject<QAClassScoresDTO>> getQAClassScores(
        @Parameter(description = "Class ID") @PathVariable Long classId,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requesting QA class scores for classId={}", currentUser.getId(), classId);

        QAClassScoresDTO classScores = qaService.getQAClassScores(classId, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<QAClassScoresDTO>builder()
            .success(true)
            .message("Lấy điểm số lớp học thành công")
            .data(classScores)
            .build());
    }
}
