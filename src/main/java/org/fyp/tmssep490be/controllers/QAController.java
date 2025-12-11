package org.fyp.tmssep490be.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.qa.QAClassListItemDTO;
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
}
