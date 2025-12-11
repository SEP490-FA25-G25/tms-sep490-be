package org.fyp.tmssep490be.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.academicteacher.*;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.AcademicTeacherService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Controller cho Academic Affairs để quản lý giáo viên và skills của họ
// Trách nhiệm chính:
// - Xem danh sách giáo viên trong các branch có thể truy cập
// - Quản lý skills của giáo viên (thêm/cập nhật/xóa)
@RestController
@RequestMapping("/api/v1/academic/teachers")
@RequiredArgsConstructor
@Slf4j
public class AcademicTeacherController {

    private final AcademicTeacherService academicTeacherService;

    // Lấy danh sách giáo viên trong các branch mà user có quyền truy cập
    // Hỗ trợ lọc theo từ khóa tìm kiếm và trạng thái có skills
    @GetMapping
    @PreAuthorize("hasRole('ROLE_ACADEMIC_AFFAIR') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<ResponseObject<List<AcademicTeacherListItemDTO>>> getTeachers(
            @RequestParam(required = false) String search, // Từ khóa tìm kiếm theo tên, email, mã nhân viên, hoặc số điện thoại
            @RequestParam(required = false) Boolean hasSkills, // Lọc theo trạng thái có skills: true = có skills, false = không có skills, null = tất cả
            @RequestParam(required = false) Long branchId, // Lọc theo branch cụ thể (nếu null thì lấy tất cả branches mà user có quyền)
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requesting teachers list with search='{}', hasSkills={}, branchId={}",
                currentUser.getId(), search, hasSkills, branchId);

        List<AcademicTeacherListItemDTO> teachers = academicTeacherService.getTeachers(
                search, hasSkills, branchId, currentUser.getId()
        );

        return ResponseEntity.ok(ResponseObject.<List<AcademicTeacherListItemDTO>>builder()
                .success(true)
                .message("Lấy danh sách giáo viên thành công")
                .data(teachers)
                .build());
    }

    // Lấy thông tin chi tiết giáo viên bao gồm tất cả skills
    @GetMapping("/{teacherId}")
    @PreAuthorize("hasRole('ROLE_ACADEMIC_AFFAIR') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<ResponseObject<AcademicTeacherDetailDTO>> getTeacherDetail(
            @PathVariable Long teacherId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requesting detail for teacher {}", currentUser.getId(), teacherId);

        AcademicTeacherDetailDTO teacher = academicTeacherService.getTeacherDetail(
                teacherId, currentUser.getId()
        );

        return ResponseEntity.ok(ResponseObject.<AcademicTeacherDetailDTO>builder()
                .success(true)
                .message("Lấy thông tin chi tiết giáo viên thành công")
                .data(teacher)
                .build());
    }

    // Lấy danh sách skills của giáo viên
    @GetMapping("/{teacherId}/skills")
    @PreAuthorize("hasRole('ROLE_ACADEMIC_AFFAIR') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<ResponseObject<List<TeacherSkillDTO>>> getTeacherSkills(
            @PathVariable Long teacherId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requesting skills for teacher {}", currentUser.getId(), teacherId);

        List<TeacherSkillDTO> skills = academicTeacherService.getTeacherSkills(
                teacherId, currentUser.getId()
        );

        return ResponseEntity.ok(ResponseObject.<List<TeacherSkillDTO>>builder()
                .success(true)
                .message("Lấy danh sách skills của giáo viên thành công")
                .data(skills)
                .build());
    }

    // Cập nhật skills của giáo viên (thay thế toàn bộ)
    // Thay thế tất cả skills hiện tại bằng danh sách mới được cung cấp
    @PostMapping("/{teacherId}/skills")
    @PreAuthorize("hasRole('ROLE_ACADEMIC_AFFAIR') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<ResponseObject<List<TeacherSkillDTO>>> updateTeacherSkills(
            @PathVariable Long teacherId,
            @Valid @RequestBody UpdateTeacherSkillsRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} updating skills for teacher {} with {} skills",
                currentUser.getId(), teacherId, request.getSkills().size());

        List<TeacherSkillDTO> updatedSkills = academicTeacherService.updateTeacherSkills(
                teacherId, request, currentUser.getId()
        );

        return ResponseEntity.ok(ResponseObject.<List<TeacherSkillDTO>>builder()
                .success(true)
                .message("Cập nhật skills của giáo viên thành công")
                .data(updatedSkills)
                .build());
    }

    // Xóa tất cả skills theo specialization cụ thể
    // Ví dụ: DELETE /api/v1/academic/teachers/1/skills?specialization=IELTS
    @DeleteMapping("/{teacherId}/skills")
    @PreAuthorize("hasRole('ROLE_ACADEMIC_AFFAIR') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<ResponseObject<Void>> deleteTeacherSkillsBySpecialization(
            @PathVariable Long teacherId,
            @RequestParam String specialization, // Specialization cần xóa (ví dụ: IELTS, TOEIC, JLPT)
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} deleting skills with specialization '{}' for teacher {}",
                currentUser.getId(), specialization, teacherId);

        academicTeacherService.deleteTeacherSkillsBySpecialization(
                teacherId, specialization, currentUser.getId()
        );

        return ResponseEntity.ok(ResponseObject.<Void>builder()
                .success(true)
                .message("Xóa skills của giáo viên thành công")
                .build());
    }
}

