package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.classcreation.CreateClassRequest;
import org.fyp.tmssep490be.dtos.classcreation.CreateClassResponse;
import org.fyp.tmssep490be.dtos.classcreation.SessionListResponse;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.ClassCreationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/class-creation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Class Creation", description = "API tạo lớp học mới")
public class ClassCreationController {

        private final ClassCreationService classCreationService;

        // Step 1: Tạo lớp mới
        @PostMapping("/classes")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD')")
        @Operation(summary = "Tạo lớp mới", description = "Tạo lớp và sinh sessions tự động từ subject template")
        public ResponseEntity<ResponseObject<CreateClassResponse>> createClass(
                        @RequestBody @Valid CreateClassRequest request,
                        @AuthenticationPrincipal UserPrincipal currentUser) {

                log.info("User {} tạo lớp mới với subject {}", currentUser.getId(), request.getSubjectId());

                CreateClassResponse response = classCreationService.createClass(request, currentUser.getId());

                String message = String.format("Đã tạo lớp %s với %d buổi học",
                                response.getCode(), response.getSessionSummary().getSessionsGenerated());

                return ResponseEntity.ok(ResponseObject.<CreateClassResponse>builder()
                                .success(true)
                                .message(message)
                                .data(response)
                                .build());
        }

        // Step 2: Xem danh sách sessions
        @GetMapping("/classes/{classId}/sessions")
        @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'TEACHER', 'MANAGER')")
        @Operation(summary = "Lấy danh sách sessions", description = "Xem lại các buổi học đã sinh cho lớp")
        public ResponseEntity<ResponseObject<SessionListResponse>> listSessions(
                        @PathVariable Long classId,
                        @AuthenticationPrincipal UserPrincipal currentUser) {

                Long userId = currentUser != null ? currentUser.getId() : null;
                log.info("User {} xem sessions cho lớp {}", userId, classId);

                SessionListResponse response = classCreationService.listSessions(classId, userId);

                return ResponseEntity.ok(ResponseObject.<SessionListResponse>builder()
                                .success(true)
                                .message("Lấy danh sách buổi học thành công")
                                .data(response)
                                .build());
        }
}
