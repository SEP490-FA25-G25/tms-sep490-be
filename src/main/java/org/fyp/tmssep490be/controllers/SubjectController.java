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
                        @RequestParam(required = false) Long levelId,
                        @RequestParam(required = false) Boolean forClassCreation) {
                log.info("Getting all subjects - curriculumId: {}, levelId: {}, forClassCreation: {}",
                                curriculumId, levelId, forClassCreation);
                List<SubjectDTO> subjects = subjectService.getAllSubjects(curriculumId, levelId, forClassCreation);
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

        @GetMapping("/{id}")
        @Operation(summary = "Get subject by ID", description = "Lấy chi tiết môn học theo ID")
        public ResponseEntity<ResponseObject<SubjectDetailDTO>> getSubjectById(@PathVariable Long id) {
                log.info("Getting subject by ID: {}", id);
                SubjectDetailDTO subject = subjectService.getSubjectDetails(id);
                return ResponseEntity.ok(ResponseObject.<SubjectDetailDTO>builder()
                                .success(true)
                                .message("Subject retrieved successfully")
                                .data(subject)
                                .build());
        }

        @PutMapping("/{id}")
        @Operation(summary = "Update subject", description = "Cập nhật môn học")
        public ResponseEntity<ResponseObject<SubjectDetailDTO>> updateSubject(
                        @PathVariable Long id,
                        @RequestBody CreateSubjectRequestDTO request,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("Updating subject ID: {}", id);
                Long userId = currentUser != null ? currentUser.getId() : null;
                SubjectDetailDTO result = subjectService.updateSubject(id, request, userId);
                return ResponseEntity.ok(ResponseObject.<SubjectDetailDTO>builder()
                                .success(true)
                                .message("Subject updated successfully")
                                .data(result)
                                .build());
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Delete subject", description = "Xóa môn học (chỉ cho phép khi status=DRAFT và chưa từng submit)")
        public ResponseEntity<ResponseObject<Void>> deleteSubject(@PathVariable Long id) {
                log.info("Deleting subject with ID: {}", id);
                subjectService.deleteSubject(id);
                return ResponseEntity.ok(ResponseObject.<Void>builder()
                                .success(true)
                                .message("Đã xóa môn học thành công")
                                .build());
        }

        // ========== APPROVAL WORKFLOW ==========

        @PostMapping("/{id}/submit")
        @Operation(summary = "Submit subject for approval", description = "Gửi môn học để phê duyệt")
        public ResponseEntity<ResponseObject<Void>> submitSubject(@PathVariable Long id) {
                log.info("Submitting subject with ID: {}", id);
                subjectService.submitSubject(id);
                return ResponseEntity.ok(ResponseObject.<Void>builder()
                                .success(true)
                                .message("Đã gửi môn học để phê duyệt thành công")
                                .build());
        }

        @PostMapping("/{id}/approve")
        @Operation(summary = "Approve subject", description = "Phê duyệt môn học")
        public ResponseEntity<ResponseObject<Void>> approveSubject(
                        @PathVariable Long id,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("Approving subject with ID: {}", id);
                Long managerId = currentUser != null ? currentUser.getId() : null;
                subjectService.approveSubject(id, managerId);
                return ResponseEntity.ok(ResponseObject.<Void>builder()
                                .success(true)
                                .message("Đã phê duyệt môn học thành công")
                                .build());
        }

        @PostMapping("/{id}/reject")
        @Operation(summary = "Reject subject", description = "Từ chối môn học")
        public ResponseEntity<ResponseObject<Void>> rejectSubject(
                        @PathVariable Long id,
                        @RequestBody(required = false) String reason,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("Rejecting subject with ID: {}", id);
                Long managerId = currentUser != null ? currentUser.getId() : null;
                subjectService.rejectSubject(id, managerId, reason);
                return ResponseEntity.ok(ResponseObject.<Void>builder()
                                .success(true)
                                .message("Đã từ chối môn học")
                                .build());
        }

        // ========== LIFECYCLE MANAGEMENT ==========

        @PostMapping("/{id}/deactivate")
        @Operation(summary = "Deactivate subject", description = "Vô hiệu hóa môn học")
        public ResponseEntity<ResponseObject<Void>> deactivateSubject(@PathVariable Long id) {
                log.info("Deactivating subject with ID: {}", id);
                subjectService.deactivateSubject(id);
                return ResponseEntity.ok(ResponseObject.<Void>builder()
                                .success(true)
                                .message("Đã vô hiệu hóa môn học thành công")
                                .build());
        }

        @PostMapping("/{id}/reactivate")
        @Operation(summary = "Reactivate subject", description = "Kích hoạt lại môn học")
        public ResponseEntity<ResponseObject<Void>> reactivateSubject(@PathVariable Long id) {
                log.info("Reactivating subject with ID: {}", id);
                subjectService.reactivateSubject(id);
                return ResponseEntity.ok(ResponseObject.<Void>builder()
                                .success(true)
                                .message("Đã kích hoạt lại môn học thành công")
                                .build());
        }

        @PostMapping("/{id}/clone")
        @Operation(summary = "Clone subject", description = "Tạo bản sao môn học với version mới")
        public ResponseEntity<ResponseObject<SubjectDTO>> cloneSubject(
                        @PathVariable Long id,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("Cloning subject with ID: {}", id);
                Long userId = currentUser != null ? currentUser.getId() : null;
                SubjectDTO result = subjectService.cloneSubject(id, userId);
                return ResponseEntity.ok(ResponseObject.<SubjectDTO>builder()
                                .success(true)
                                .message("Đã tạo bản sao môn học thành công")
                                .data(result)
                                .build());
        }

        // ========== SYLLABUS AND MATERIALS ==========

        @GetMapping("/{id}/syllabus")
        @Operation(summary = "Get subject syllabus", description = "Lấy giáo trình của môn học")
        public ResponseEntity<ResponseObject<SubjectDetailDTO>> getSubjectSyllabus(@PathVariable Long id) {
                log.info("Getting syllabus for subject ID: {}", id);
                SubjectDetailDTO syllabus = subjectService.getSubjectSyllabus(id);
                return ResponseEntity.ok(ResponseObject.<SubjectDetailDTO>builder()
                                .success(true)
                                .message("Subject syllabus retrieved successfully")
                                .data(syllabus)
                                .build());
        }

        @GetMapping("/{id}/materials")
        @Operation(summary = "Get subject materials", description = "Lấy tài liệu của môn học")
        public ResponseEntity<ResponseObject<MaterialHierarchyDTO>> getSubjectMaterials(
                        @PathVariable Long id,
                        @RequestParam(required = false) Long studentId) {
                log.info("Getting materials for subject ID: {}, studentId: {}", id, studentId);
                MaterialHierarchyDTO materials = subjectService.getSubjectMaterials(id, studentId);
                return ResponseEntity.ok(ResponseObject.<MaterialHierarchyDTO>builder()
                                .success(true)
                                .message("Subject materials retrieved successfully")
                                .data(materials)
                                .build());
        }

}