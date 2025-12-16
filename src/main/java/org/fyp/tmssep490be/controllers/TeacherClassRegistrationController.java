package org.fyp.tmssep490be.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.teacherregistration.*;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.TeacherClassRegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher-registrations")
@RequiredArgsConstructor
@Slf4j
public class TeacherClassRegistrationController {

        private final TeacherClassRegistrationService registrationService;

        // ==================== TEACHER APIs ====================

        // Giáo viên xem danh sách lớp có thể đăng ký
        @GetMapping("/available-classes")
        @PreAuthorize("hasRole('TEACHER')")
        public ResponseEntity<ResponseObject<List<AvailableClassDTO>>> getAvailableClasses(
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {

                List<AvailableClassDTO> classes = registrationService.getAvailableClasses(userPrincipal.getId());

                return ResponseEntity.ok(ResponseObject.<List<AvailableClassDTO>>builder()
                                .success(true)
                                .message("Lấy danh sách lớp thành công")
                                .data(classes)
                                .build());
        }

        // Giáo viên đăng ký dạy lớp
        @PostMapping
        @PreAuthorize("hasRole('TEACHER')")
        public ResponseEntity<ResponseObject<TeacherRegistrationResponse>> registerForClass(
                        @RequestBody @Valid TeacherRegistrationRequest request,
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {

                log.info("Teacher {} registering for class {}", userPrincipal.getId(), request.getClassId());

                TeacherRegistrationResponse response = registrationService.registerForClass(request,
                                userPrincipal.getId());

                return ResponseEntity.ok(ResponseObject.<TeacherRegistrationResponse>builder()
                                .success(true)
                                .message("Đăng ký dạy lớp thành công")
                                .data(response)
                                .build());
        }

        // Giáo viên xem danh sách đăng ký của mình
        @GetMapping("/me")
        @PreAuthorize("hasRole('TEACHER')")
        public ResponseEntity<ResponseObject<List<MyRegistrationDTO>>> getMyRegistrations(
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {

                List<MyRegistrationDTO> registrations = registrationService.getMyRegistrations(userPrincipal.getId());

                return ResponseEntity.ok(ResponseObject.<List<MyRegistrationDTO>>builder()
                                .success(true)
                                .message("Lấy danh sách đăng ký thành công")
                                .data(registrations)
                                .build());
        }

        // Giáo viên hủy đăng ký
        @DeleteMapping("/{registrationId}")
        @PreAuthorize("hasRole('TEACHER')")
        public ResponseEntity<ResponseObject<Void>> cancelRegistration(
                        @PathVariable Long registrationId,
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {

                log.info("Teacher {} cancelling registration {}", userPrincipal.getId(), registrationId);

                registrationService.cancelRegistration(registrationId, userPrincipal.getId());

                return ResponseEntity.ok(ResponseObject.<Void>builder()
                                .success(true)
                                .message("Hủy đăng ký thành công")
                                .build());
        }

        // Kiểm tra xung đột lịch trước khi đăng ký
        @GetMapping("/check-conflict/{classId}")
        @PreAuthorize("hasRole('TEACHER')")
        public ResponseEntity<ResponseObject<ScheduleConflictDTO>> checkScheduleConflict(
                        @PathVariable Long classId,
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {

                log.info("Teacher {} checking schedule conflict for class {}", userPrincipal.getId(), classId);

                ScheduleConflictDTO result = registrationService.checkScheduleConflict(classId, userPrincipal.getId());

                return ResponseEntity.ok(ResponseObject.<ScheduleConflictDTO>builder()
                                .success(true)
                                .message(result.isHasConflict() ? "Phát hiện xung đột lịch" : "Không có xung đột lịch")
                                .data(result)
                                .build());
        }

        // ==================== ACADEMIC AFFAIRS APIs ====================

        // AA mở đăng ký cho lớp
        @PostMapping("/open-registration")
        @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
        public ResponseEntity<ResponseObject<Void>> openRegistration(
                        @RequestBody @Valid OpenRegistrationRequest request,
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {

                log.info("AA {} opening registration for class {}", userPrincipal.getId(), request.getClassId());

                registrationService.openRegistration(request, userPrincipal.getId());

                return ResponseEntity.ok(ResponseObject.<Void>builder()
                                .success(true)
                                .message("Mở đăng ký thành công")
                                .build());
        }

        // AA xem danh sách lớp cần review
        @GetMapping("/classes-needing-review")
        @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
        public ResponseEntity<ResponseObject<List<ClassRegistrationSummaryDTO>>> getClassesNeedingReview(
                        @AuthenticationPrincipal UserPrincipal userPrincipal,
                        @RequestParam(required = false) Long branchId) {

                List<ClassRegistrationSummaryDTO> classes = registrationService
                                .getClassesNeedingReview(userPrincipal.getId(), branchId);

                return ResponseEntity.ok(ResponseObject.<List<ClassRegistrationSummaryDTO>>builder()
                                .success(true)
                                .message("Lấy danh sách lớp cần review thành công")
                                .data(classes)
                                .build());
        }

        // AA xem danh sách lớp cần gán giáo viên (APPROVED, chưa có teacher)
        @GetMapping("/classes-needing-teacher")
        @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
        public ResponseEntity<ResponseObject<List<ClassNeedingTeacherDTO>>> getClassesNeedingTeacher(
                        @AuthenticationPrincipal UserPrincipal userPrincipal,
                        @RequestParam(required = false) Long branchId) {

                List<ClassNeedingTeacherDTO> classes = registrationService
                                .getClassesNeedingTeacher(userPrincipal.getId(), branchId);

                return ResponseEntity.ok(ResponseObject.<List<ClassNeedingTeacherDTO>>builder()
                                .success(true)
                                .message("Lấy danh sách lớp cần gán giáo viên thành công")
                                .data(classes)
                                .build());
        }

        // AA xem chi tiết đăng ký của 1 lớp
        @GetMapping("/classes/{classId}/registrations")
        @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
        public ResponseEntity<ResponseObject<ClassRegistrationSummaryDTO>> getClassRegistrations(
                        @PathVariable Long classId) {

                ClassRegistrationSummaryDTO summary = registrationService.getClassRegistrationSummary(classId);

                return ResponseEntity.ok(ResponseObject.<ClassRegistrationSummaryDTO>builder()
                                .success(true)
                                .message("Lấy chi tiết đăng ký thành công")
                                .data(summary)
                                .build());
        }

        // AA duyệt chọn giáo viên
        @PostMapping("/approve")
        @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
        public ResponseEntity<ResponseObject<Void>> approveRegistration(
                        @RequestBody @Valid ApproveRegistrationRequest request,
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {

                log.info("AA {} approving registration {}", userPrincipal.getId(), request.getRegistrationId());

                registrationService.approveRegistration(request, userPrincipal.getId());

                return ResponseEntity.ok(ResponseObject.<Void>builder()
                                .success(true)
                                .message("Duyệt chọn giáo viên thành công")
                                .build());
        }

        // AA gán trực tiếp giáo viên
        @PostMapping("/direct-assign")
        @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
        public ResponseEntity<ResponseObject<Void>> directAssignTeacher(
                        @RequestBody @Valid DirectAssignRequest request,
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {

                log.info("AA {} directly assigning teacher {} to class {}",
                                userPrincipal.getId(), request.getTeacherId(), request.getClassId());

                registrationService.directAssignTeacher(request, userPrincipal.getId());

                return ResponseEntity.ok(ResponseObject.<Void>builder()
                                .success(true)
                                .message("Gán giáo viên thành công")
                                .build());
        }

        // AA lấy danh sách giáo viên phù hợp để gán trực tiếp
        @GetMapping("/classes/{classId}/qualified-teachers")
        @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
        public ResponseEntity<ResponseObject<List<QualifiedTeacherDTO>>> getQualifiedTeachers(
                        @PathVariable Long classId,
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {

                log.info("AA {} getting qualified teachers for class {}", userPrincipal.getId(), classId);

                List<QualifiedTeacherDTO> teachers = registrationService.getQualifiedTeachersForClass(
                                classId, userPrincipal.getId());

                return ResponseEntity.ok(ResponseObject.<List<QualifiedTeacherDTO>>builder()
                                .success(true)
                                .message("Lấy danh sách giáo viên phù hợp thành công")
                                .data(teachers)
                                .build());
        }
}
