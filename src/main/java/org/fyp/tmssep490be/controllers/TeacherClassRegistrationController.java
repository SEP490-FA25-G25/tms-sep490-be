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

        TeacherRegistrationResponse response = registrationService.registerForClass(request, userPrincipal.getId());

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

    // ==================== ACADEMIC AFFAIRS APIs ====================

    // AA mở đăng ký cho lớp
    @PostMapping("/open-registration")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIRS')")
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
    @PreAuthorize("hasRole('ACADEMIC_AFFAIRS')")
    public ResponseEntity<ResponseObject<List<ClassRegistrationSummaryDTO>>> getClassesNeedingReview(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<ClassRegistrationSummaryDTO> classes = registrationService.getClassesNeedingReview(userPrincipal.getId());

        return ResponseEntity.ok(ResponseObject.<List<ClassRegistrationSummaryDTO>>builder()
                .success(true)
                .message("Lấy danh sách lớp cần review thành công")
                .data(classes)
                .build());
    }

    // AA xem chi tiết đăng ký của 1 lớp
    @GetMapping("/classes/{classId}/registrations")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIRS')")
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
    @PreAuthorize("hasRole('ACADEMIC_AFFAIRS')")
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
    @PreAuthorize("hasRole('ACADEMIC_AFFAIRS')")
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
}
