package org.fyp.tmssep490be.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.classcreation.CreateClassRequest;
import org.fyp.tmssep490be.dtos.classcreation.CreateClassResponse;
import org.fyp.tmssep490be.dtos.classcreation.SessionListResponse;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.ClassCreationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

// Controller xử lý workflow tạo lớp học
@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
@Slf4j
public class ClassCreationController {

        private final ClassCreationService classCreationService;

        // Step 1: Tạo lớp mới
        @PostMapping
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER')")
        public ResponseEntity<ResponseObject<CreateClassResponse>> createClass(
                        @Valid @RequestBody CreateClassRequest request,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("User {} đang tạo lớp mới", currentUser.getId());

                CreateClassResponse response = classCreationService.createClass(request, currentUser.getId());

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ResponseObject.<CreateClassResponse>builder()
                                                .success(true)
                                                .message("Tạo lớp thành công")
                                                .data(response)
                                                .build());
        }

        // Step 1: Cập nhật lớp (DRAFT/REJECTED)
        @PutMapping("/{classId}")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER')")
        public ResponseEntity<ResponseObject<CreateClassResponse>> updateClass(
                        @PathVariable Long classId,
                        @Valid @RequestBody CreateClassRequest request,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("User {} đang cập nhật lớp {}", currentUser.getId(), classId);

                CreateClassResponse response = classCreationService.updateClass(classId, request, currentUser.getId());

                return ResponseEntity.ok(ResponseObject.<CreateClassResponse>builder()
                                .success(true)
                                .message("Cập nhật lớp thành công")
                                .data(response)
                                .build());
        }

        // Preview mã lớp dựa trên branchId, subjectId, và startDate
        @GetMapping("/preview-code")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER')")
        public ResponseEntity<ResponseObject<Map<String, String>>> previewClassCode(
                        @RequestParam Long branchId,
                        @RequestParam Long subjectId,
                        @RequestParam LocalDate startDate) {
                log.info("Preview mã lớp: branchId={}, subjectId={}, startDate={}", branchId, subjectId, startDate);

                String previewCode = classCreationService.previewClassCode(branchId, subjectId, startDate);

                return ResponseEntity.ok(ResponseObject.<Map<String, String>>builder()
                                .success(true)
                                .message("Preview mã lớp")
                                .data(Map.of("previewCode", previewCode))
                                .build());
        }

        // Kiểm tra tên lớp trùng
        @GetMapping("/check-name")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER')")
        public ResponseEntity<ResponseObject<Map<String, Object>>> checkClassName(
                        @RequestParam Long branchId,
                        @RequestParam String name,
                        @RequestParam(required = false) Long excludeId) {
                log.info("Kiểm tra tên lớp: branchId={}, name={}, excludeId={}", branchId, name, excludeId);

                boolean exists = classCreationService.checkClassNameExists(branchId, name, excludeId);

                return ResponseEntity.ok(ResponseObject.<Map<String, Object>>builder()
                                .success(true)
                                .message(exists ? "Tên lớp đã tồn tại" : "Tên lớp hợp lệ")
                                .data(Map.of(
                                                "exists", exists,
                                                "branchId", branchId,
                                                "name", name))
                                .build());
        }

        // Lấy thông tin lớp để edit
        @GetMapping("/{classId}/edit")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER')")
        public ResponseEntity<ResponseObject<CreateClassResponse>> getClassForEdit(
                        @PathVariable Long classId,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("User {} lấy thông tin lớp {} để edit", currentUser.getId(), classId);

                CreateClassResponse response = classCreationService.getClassForEdit(classId, currentUser.getId());

                return ResponseEntity.ok(ResponseObject.<CreateClassResponse>builder()
                                .success(true)
                                .message("Lấy thông tin lớp thành công")
                                .data(response)
                                .build());
        }

        // Step 2: Lấy danh sách sessions
        @GetMapping("/{classId}/sessions")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER')")
        public ResponseEntity<ResponseObject<SessionListResponse>> getClassSessions(
                        @PathVariable Long classId,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("User {} lấy sessions của lớp {}", currentUser.getId(), classId);

                SessionListResponse response = classCreationService.listSessions(classId, currentUser.getId());

                return ResponseEntity.ok(ResponseObject.<SessionListResponse>builder()
                                .success(true)
                                .message("Lấy sessions thành công")
                                .data(response)
                                .build());
        }
}
