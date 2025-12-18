package org.fyp.tmssep490be.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.classmanagement.AvailableStudentDTO;
import org.fyp.tmssep490be.dtos.classmanagement.ClassDetailDTO;
import org.fyp.tmssep490be.dtos.classmanagement.ClassListItemDTO;
import org.fyp.tmssep490be.dtos.classmanagement.ClassStudentDTO;
import org.fyp.tmssep490be.dtos.classcreation.CreateClassRequest;
import org.fyp.tmssep490be.dtos.classcreation.CreateClassResponse;
import org.fyp.tmssep490be.dtos.classcreation.PreviewClassCodeResponse;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.qa.QASessionListResponse;
import org.fyp.tmssep490be.entities.Branch;
import org.fyp.tmssep490be.entities.Subject;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.Modality;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.BranchRepository;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.repositories.SubjectRepository;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.ClassService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
@Slf4j
public class ClassController {

        private final ClassService classService;
        private final BranchRepository branchRepository;
        private final SubjectRepository subjectRepository;
        private final ClassRepository classRepository;
        private final org.fyp.tmssep490be.services.ApprovalService approvalService;

        /**
         * STEP 1: Create a new class with DRAFT status and auto-generate sessions
         */
        @PostMapping
        @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
        public ResponseEntity<ResponseObject<CreateClassResponse>> createClass(
                        @RequestBody @Valid CreateClassRequest request,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("User {} creating new class with code: {}", currentUser.getId(), request.getCode());

                CreateClassResponse response = classService.createClass(request, currentUser.getId());

                boolean isSuccess = response != null &&
                                response.getClassId() != null &&
                                response.getCode() != null &&
                                response.getSessionSummary() != null &&
                                response.getSessionSummary().getSessionsGenerated() > 0;

                String message = isSuccess
                                ? String.format("Class %s created successfully with %d sessions generated",
                                                response.getCode(), response.getSessionSummary().getSessionsGenerated())
                                : "Failed to create class";

                return ResponseEntity.ok(ResponseObject.<CreateClassResponse>builder()
                                .success(isSuccess)
                                .message(message)
                                .data(response)
                                .build());
        }

        /**
         * Update an existing class (only for DRAFT or REJECTED status)
         */
        @PutMapping("/{classId}")
        @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
        public ResponseEntity<ResponseObject<CreateClassResponse>> updateClass(
                        @PathVariable Long classId,
                        @RequestBody @Valid CreateClassRequest request,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("User {} updating class with id: {}", currentUser.getId(), classId);

                CreateClassResponse response = classService.updateClass(classId, request, currentUser.getId());

                boolean isSuccess = response != null &&
                                response.getClassId() != null &&
                                response.getCode() != null;

                String message = isSuccess
                                ? String.format("Class %s updated successfully", response.getCode())
                                : "Failed to update class";

                return ResponseEntity.ok(ResponseObject.<CreateClassResponse>builder()
                                .success(isSuccess)
                                .message(message)
                                .data(response)
                                .build());
        }

        @GetMapping("/preview-code")
        @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
        public ResponseEntity<ResponseObject<PreviewClassCodeResponse>> previewClassCode(
                        @RequestParam Long branchId,
                        @RequestParam Long subjectId,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("User {} previewing class code for branchId={}, subjectId={}, startDate={}",
                                currentUser.getId(), branchId, subjectId, startDate);

                // Validate Branch
                Branch branch = branchRepository.findById(branchId)
                                .orElseThrow(() -> new CustomException(ErrorCode.BRANCH_NOT_FOUND));

                // Validate Subject
                Subject subject = subjectRepository.findById(subjectId)
                                .orElseThrow(() -> new CustomException(ErrorCode.SUBJECT_NOT_FOUND));

                // Normalize subject code (remove special chars)
                String normalizedSubjectCode = normalizeSubjectCode(subject.getCode());

                // Build prefix: SUBJECTCODE-BRANCHCODE-YY (2-digit year)
                int year = startDate.getYear() % 100;
                String prefix = String.format("%s-%s-%02d", normalizedSubjectCode, branch.getCode(), year);

                // Find next sequence
                int nextSequence = findNextSequenceReadOnly(branchId, prefix);

                // Build preview code
                String previewCode = String.format("%s-%03d", prefix, nextSequence);

                // Warning if approaching limit
                String warning = null;
                if (nextSequence >= 990) {
                        warning = "Sequence number is approaching limit (999). Consider using a different subject or branch.";
                }

                PreviewClassCodeResponse response = PreviewClassCodeResponse.builder()
                                .previewCode(previewCode)
                                .prefix(prefix)
                                .nextSequence(nextSequence)
                                .warning(warning)
                                .build();

                String message = warning != null ? "Class code preview generated with warning"
                                : "Class code preview generated successfully";

                return ResponseEntity.ok(ResponseObject.<PreviewClassCodeResponse>builder()
                                .success(true)
                                .message(message)
                                .data(response)
                                .build());
        }

        private String normalizeSubjectCode(String subjectCode) {
                if (subjectCode == null || subjectCode.trim().isEmpty()) {
                        throw new CustomException(ErrorCode.INVALID_REQUEST);
                }

                // Remove all non-alphanumeric characters and convert to uppercase
                String normalized = subjectCode
                                .replaceAll("[^A-Za-z0-9]", "")
                                .toUpperCase();

                // Try to extract only the subject name part (before year digits if any)
                Pattern yearPattern = Pattern.compile("(\\d{4})");
                Matcher matcher = yearPattern.matcher(normalized);

                if (matcher.find()) {
                        normalized = normalized.substring(0, matcher.start());
                }

                return normalized.isEmpty() ? subjectCode.toUpperCase() : normalized;
        }

        private int findNextSequenceReadOnly(Long branchId, String prefix) {
                String likePattern = prefix + "-%";
                String highestCode = classRepository.findHighestCodeByPrefixReadOnly(branchId, likePattern);

                if (highestCode == null || highestCode.isEmpty()) {
                        return 1;
                }

                // Extract sequence from code (e.g., "IELTSFOUND-HN01-25-005" -> 5)
                try {
                        String sequencePart = highestCode.substring(highestCode.lastIndexOf('-') + 1);
                        return Integer.parseInt(sequencePart) + 1;
                } catch (Exception e) {
                        log.warn("Could not extract sequence from code: {}", highestCode);
                        return 1;
                }
        }

        @GetMapping("/check-name")
        @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
        public ResponseEntity<ResponseObject<java.util.Map<String, Object>>> checkClassNameExists(
                        @RequestParam Long branchId,
                        @RequestParam String name,
                        @RequestParam(required = false) Long excludeId,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("User {} checking if class name '{}' exists in branch {}",
                                currentUser.getId(), name, branchId);

                boolean exists;
                if (excludeId != null) {
                        // Edit case: exclude current class
                        exists = classRepository.existsByBranchIdAndNameIgnoreCaseAndIdNot(branchId, name,
                                        excludeId);
                } else {
                        // Create case
                        exists = classRepository.existsByBranchIdAndNameIgnoreCase(branchId, name);
                }

                java.util.Map<String, Object> data = java.util.Map.of(
                                "exists", exists,
                                "branchId", branchId,
                                "name", name);

                return ResponseEntity.ok(ResponseObject.<java.util.Map<String, Object>>builder()
                                .success(true)
                                .message(exists ? "Class name already exists" : "Class name is available")
                                .data(data)
                                .build());
        }

        @GetMapping("/{classId}/sessions")
        @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
        public ResponseEntity<ResponseObject<org.fyp.tmssep490be.dtos.classcreation.ClassSessionsOverviewDTO>> getClassSessions(
                        @PathVariable Long classId,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("User {} requesting sessions for class {}", currentUser.getId(), classId);

                org.fyp.tmssep490be.dtos.classcreation.ClassSessionsOverviewDTO response = classService
                                .getClassSessions(classId, currentUser.getId());

                return ResponseEntity.ok(
                                ResponseObject.<org.fyp.tmssep490be.dtos.classcreation.ClassSessionsOverviewDTO>builder()
                                                .success(true)
                                                .message("Class sessions retrieved successfully")
                                                .data(response)
                                                .build());
        }

        @PostMapping("/{classId}/time-slots")
        @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
        public ResponseEntity<ResponseObject<org.fyp.tmssep490be.dtos.classcreation.AssignTimeSlotsResponse>> assignTimeSlots(
                        @PathVariable Long classId,
                        @RequestBody @Valid org.fyp.tmssep490be.dtos.classcreation.AssignTimeSlotsRequest request,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("User {} assigning time slots to class {}", currentUser.getId(), classId);

                org.fyp.tmssep490be.dtos.classcreation.AssignTimeSlotsResponse response = classService
                                .assignTimeSlots(classId, request, currentUser.getId());

                return ResponseEntity.ok(
                                ResponseObject.<org.fyp.tmssep490be.dtos.classcreation.AssignTimeSlotsResponse>builder()
                                                .success(response.isSuccess())
                                                .message(response.getMessage())
                                                .data(response)
                                                .build());
        }

        @GetMapping("/{classId}/resources")
        @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
        public ResponseEntity<ResponseObject<List<org.fyp.tmssep490be.dtos.classcreation.AvailableResourceDTO>>> getAvailableResources(
                        @PathVariable Long classId,
                        @RequestParam(required = false) Long timeSlotId,
                        @RequestParam(required = false) Short dayOfWeek,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("User {} requesting available resources for class {}", currentUser.getId(), classId);

                List<org.fyp.tmssep490be.dtos.classcreation.AvailableResourceDTO> resources = classService
                                .getAvailableResources(
                                                classId, timeSlotId, dayOfWeek, currentUser.getId());

                return ResponseEntity.ok(
                                ResponseObject.<List<org.fyp.tmssep490be.dtos.classcreation.AvailableResourceDTO>>builder()
                                                .success(true)
                                                .message("Available resources retrieved successfully")
                                                .data(resources)
                                                .build());
        }

        @PostMapping("/{classId}/resources")
        @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
        public ResponseEntity<ResponseObject<org.fyp.tmssep490be.dtos.classcreation.AssignResourcesResponse>> assignResources(
                        @PathVariable Long classId,
                        @RequestBody @Valid org.fyp.tmssep490be.dtos.classcreation.AssignResourcesRequest request,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("User {} assigning resources to class {}", currentUser.getId(), classId);

                org.fyp.tmssep490be.dtos.classcreation.AssignResourcesResponse response = classService.assignResources(
                                classId, request, currentUser.getId());

                return ResponseEntity.ok(
                                ResponseObject.<org.fyp.tmssep490be.dtos.classcreation.AssignResourcesResponse>builder()
                                                .success(response.getSuccessCount() > 0)
                                                .message(String.format(
                                                                "Resources assigned: %d/%d sessions successful, %d conflicts (%dms)",
                                                                response.getSuccessCount(), response.getTotalSessions(),
                                                                response.getConflictCount(),
                                                                response.getProcessingTimeMs()))
                                                .data(response)
                                                .build());
        }

        @PostMapping("/{classId}/sessions/{sessionId}/resource")
        @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
        public ResponseEntity<ResponseObject<Void>> assignSessionResource(
                        @PathVariable Long classId,
                        @PathVariable Long sessionId,
                        @RequestBody java.util.Map<String, Long> body,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("User {} assigning resource to session {} of class {}",
                                currentUser.getId(), sessionId, classId);

                Long resourceId = body.get("resourceId");
                if (resourceId == null) {
                        throw new CustomException(ErrorCode.INVALID_REQUEST);
                }

                classService.assignResourceToSession(classId, sessionId, resourceId, currentUser.getId());

                return ResponseEntity.ok(
                                ResponseObject.<Void>builder()
                                                .success(true)
                                                .message("Resource assigned to session successfully")
                                                .build());
        }

        @PostMapping("/{classId}/validate")
        @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
        public ResponseEntity<ResponseObject<org.fyp.tmssep490be.dtos.classcreation.ValidateClassResponse>> validateClass(
                        @PathVariable Long classId,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("User {} validating class {}", currentUser.getId(), classId);

                org.fyp.tmssep490be.dtos.classcreation.ValidateClassResponse response = classService.validateClass(
                                classId,
                                currentUser.getId());

                return ResponseEntity.ok(
                                ResponseObject.<org.fyp.tmssep490be.dtos.classcreation.ValidateClassResponse>builder()
                                                .success(response.getValid())
                                                .message(response.getMessage())
                                                .data(response)
                                                .build());
        }

        @PostMapping("/{classId}/submit")
        @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
        public ResponseEntity<ResponseObject<org.fyp.tmssep490be.dtos.classcreation.SubmitClassResponse>> submitClass(
                        @PathVariable Long classId,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("User {} submitting class {} for approval", currentUser.getId(), classId);

                org.fyp.tmssep490be.dtos.classcreation.SubmitClassResponse response = classService.submitClass(classId,
                                currentUser.getId());

                return ResponseEntity
                                .ok(ResponseObject.<org.fyp.tmssep490be.dtos.classcreation.SubmitClassResponse>builder()
                                                .success(response.isSuccess())
                                                .message(response.getMessage())
                                                .data(response)
                                                .build());
        }

        /**
         * Reset wizard step data when user navigates back and makes changes.
         * This clears time slots and/or resources based on fromStep parameter.
         */
        @PostMapping("/{classId}/reset-steps")
        @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
        public ResponseEntity<ResponseObject<java.util.Map<String, Object>>> resetWizardSteps(
                        @PathVariable Long classId,
                        @RequestParam Integer fromStep,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("User {} resetting wizard steps from {} for class {}",
                                currentUser.getId(), fromStep, classId);

                java.util.Map<String, Object> result = classService.resetWizardSteps(
                                classId, fromStep, currentUser.getId());

                return ResponseEntity.ok(ResponseObject.<java.util.Map<String, Object>>builder()
                                .success(true)
                                .message("Wizard steps reset successfully")
                                .data(result)
                                .build());
        }

        @PostMapping("/{classId}/approve")
        @PreAuthorize("hasRole('CENTER_HEAD') or hasRole('MANAGER')")
        public ResponseEntity<ResponseObject<String>> approveClass(
                        @PathVariable Long classId,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("User {} approving class {}", currentUser.getId(), classId);

                approvalService.approveClass(classId, currentUser.getId());

                return ResponseEntity.ok(ResponseObject.<String>builder()
                                .success(true)
                                .message("Lớp học đã được phê duyệt thành công")
                                .data("APPROVED")
                                .build());
        }

        @PostMapping("/{classId}/reject")
        @PreAuthorize("hasRole('CENTER_HEAD') or hasRole('MANAGER')")
        public ResponseEntity<ResponseObject<String>> rejectClass(
                        @PathVariable Long classId,
                        @RequestBody java.util.Map<String, String> body,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("User {} rejecting class {} with reason: {}", currentUser.getId(), classId,
                                body.get("reason"));

                String reason = body.get("reason");
                approvalService.rejectClass(classId, reason, currentUser.getId());

                return ResponseEntity.ok(ResponseObject.<String>builder()
                                .success(true)
                                .message("Lớp học đã bị từ chối")
                                .data("REJECTED")
                                .build());
        }

        // Endpoint để lấy thông tin lớp học theo ID(cho TEACHER, STUDENT,
        // ACADEMIC_AFFAIR, QA, etc.)
        @GetMapping("/{classId}")
        @PreAuthorize("hasRole('TEACHER') or hasRole('STUDENT') or hasRole('ACADEMIC_AFFAIR') or hasRole('CENTER_HEAD') or hasRole('MANAGER') or hasRole('QA')")
        public ResponseEntity<ResponseObject<ClassDetailDTO>> getClassDetail(
                        @PathVariable Long classId,
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {

                log.info("User {} requesting details for class {}", userPrincipal.getId(), classId);

                ClassDetailDTO classDetail = classService.getClassDetail(classId, userPrincipal.getId());

                return ResponseEntity.ok(
                                ResponseObject.<ClassDetailDTO>builder()
                                                .success(true)
                                                .message("Class details retrieved successfully")
                                                .data(classDetail)
                                                .build());
        }

        @GetMapping
        @PreAuthorize("hasRole('ACADEMIC_AFFAIR') or hasRole('CENTER_HEAD') or hasRole('TEACHER') or hasRole('MANAGER')")
        public ResponseEntity<ResponseObject<Page<ClassListItemDTO>>> getClasses(
                        @RequestParam(required = false) List<Long> branchIds,
                        @RequestParam(required = false) Long subjectId,
                        @RequestParam(required = false) ClassStatus status,
                        @RequestParam(required = false) ApprovalStatus approvalStatus,
                        @RequestParam(required = false) Modality modality,
                        @RequestParam(required = false) String search,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @RequestParam(defaultValue = "startDate") String sort,
                        @RequestParam(defaultValue = "asc") String sortDir,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info(
                                "User {} requesting classes list with filters: branchIds={}, subjectId={}, status={}, approvalStatus={}, modality={}, search={}",
                                currentUser.getId(), branchIds, subjectId, status, approvalStatus, modality, search);

                Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
                Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));

                Page<ClassListItemDTO> classes = classService.getClasses(
                                branchIds, subjectId, status, approvalStatus, modality, search, pageable,
                                currentUser.getId());

                return ResponseEntity.ok(ResponseObject.<Page<ClassListItemDTO>>builder()
                                .success(true)
                                .message("Classes retrieved successfully")
                                .data(classes)
                                .build());
        }

        @GetMapping("/{classId}/available-students")
        @PreAuthorize("hasRole('ACADEMIC_AFFAIR') or hasRole('CENTER_HEAD') or hasRole('MANAGER')")
        public ResponseEntity<ResponseObject<Page<AvailableStudentDTO>>> getAvailableStudentsForClass(
                        @PathVariable Long classId,
                        @RequestParam(required = false) String search,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @AuthenticationPrincipal UserPrincipal currentUser) {

                log.info("User {} requesting available students for class {} with search: {}",
                                currentUser.getId(), classId, search);

                Pageable pageable = PageRequest.of(page, size);
                Page<AvailableStudentDTO> availableStudents = classService.getAvailableStudentsForClass(
                                classId, search, pageable, currentUser.getId());

                return ResponseEntity.ok(ResponseObject.<Page<AvailableStudentDTO>>builder()
                                .success(true)
                                .message("Available students retrieved successfully")
                                .data(availableStudents)
                                .build());
        }

        @GetMapping("/{classId}/students")
        @PreAuthorize("hasRole('ACADEMIC_AFFAIR') or hasRole('CENTER_HEAD') or hasRole('MANAGER') or hasRole('TEACHER')")
        public ResponseEntity<ResponseObject<Page<ClassStudentDTO>>> getClassStudents(
                        @PathVariable Long classId,
                        @RequestParam(required = false) String search,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @RequestParam(defaultValue = "enrolledAt") String sort,
                        @RequestParam(defaultValue = "desc") String sortDir,
                        @AuthenticationPrincipal UserPrincipal currentUser) {

                log.info("User {} requesting students for class {} with search: {}", currentUser.getId(), classId,
                                search);

                // Create pageable with sort
                Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
                Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));

                Page<ClassStudentDTO> students = classService.getClassStudents(
                                classId, search, pageable, currentUser.getId());

                return ResponseEntity.ok(ResponseObject.<Page<ClassStudentDTO>>builder()
                                .success(true)
                                .message("Class students retrieved successfully")
                                .data(students)
                                .build());
        }

        @GetMapping("/{classId}/sessions/metrics")
        @PreAuthorize("hasRole('TEACHER') or hasRole('ACADEMIC_AFFAIR') or hasRole('CENTER_HEAD') or hasRole('MANAGER') or hasRole('QA')")
        public ResponseEntity<ResponseObject<QASessionListResponse>> getSessionsWithMetrics(
                        @PathVariable Long classId,
                        @AuthenticationPrincipal UserPrincipal currentUser) {

                log.info("User {} requesting sessions with metrics for class {}", currentUser.getId(), classId);

                QASessionListResponse response = classService.getSessionsWithMetrics(classId, currentUser.getId());

                return ResponseEntity.ok(ResponseObject.<QASessionListResponse>builder()
                                .success(true)
                                .message("Sessions with metrics retrieved successfully")
                                .data(response)
                                .build());
        }
}