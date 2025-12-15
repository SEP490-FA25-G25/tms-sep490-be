package org.fyp.tmssep490be.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.managerteacher.ManagerTeacherBranchUpdateRequest;
import org.fyp.tmssep490be.dtos.managerteacher.ManagerTeacherListItemDTO;
import org.fyp.tmssep490be.dtos.schedule.WeeklyScheduleResponseDTO;
import org.fyp.tmssep490be.dtos.teacher.TeacherProfileDTO;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.ManagerTeacherService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/manager/teachers")
@RequiredArgsConstructor
@Slf4j
public class ManagerTeacherController {

    private final ManagerTeacherService managerTeacherService;

    // Lấy danh sách giáo viên trong phạm vi quản lý
    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ResponseObject<List<ManagerTeacherListItemDTO>>> getManagedTeachers(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("Manager {} requesting managed teachers list", currentUser.getId());
        List<ManagerTeacherListItemDTO> teachers = managerTeacherService.getManagedTeachers(currentUser.getId());
        return ResponseEntity.ok(ResponseObject.success(teachers));
    }

    // Gán giáo viên vào chi nhánh trong phạm vi quản lý
    @PostMapping("/{teacherId}/branches/{branchId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ResponseObject<Void>> assignTeacherToBranch(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long teacherId,
            @PathVariable Long branchId) {
        log.info("Manager {} assigning teacher {} to branch {}", currentUser.getId(), teacherId, branchId);
        managerTeacherService.assignTeacherToBranch(currentUser.getId(), teacherId, branchId);
        return ResponseEntity.ok(ResponseObject.success("Gán giáo viên vào chi nhánh thành công", null));
    }

    // Cập nhật danh sách chi nhánh của giáo viên trong phạm vi quản lý
    @PutMapping("/{teacherId}/branches")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ResponseObject<Void>> updateTeacherBranches(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long teacherId,
            @RequestBody @Valid ManagerTeacherBranchUpdateRequest request) {
        log.info("Manager {} updating branches for teacher {}", currentUser.getId(), teacherId);
        managerTeacherService.updateTeacherBranches(currentUser.getId(), teacherId, request.getBranchIds());
        return ResponseEntity.ok(ResponseObject.success("Cập nhật chi nhánh cho giáo viên thành công", null));
    }

    // Lấy chi tiết hồ sơ giáo viên trong phạm vi quản lý
    @GetMapping("/{teacherId}/profile")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ResponseObject<TeacherProfileDTO>> getTeacherProfile(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long teacherId) {
        log.info("Manager {} requesting profile for teacher {}", currentUser.getId(), teacherId);
        TeacherProfileDTO profile = managerTeacherService.getTeacherProfile(currentUser.getId(), teacherId);
        return ResponseEntity.ok(ResponseObject.success(profile));
    }

    // Lấy lịch dạy theo tuần của giáo viên trong phạm vi quản lý
    @GetMapping("/{teacherId}/schedule")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ResponseObject<WeeklyScheduleResponseDTO>> getTeacherWeeklySchedule(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long teacherId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        log.info("Manager {} requesting schedule for teacher {} on week {}", currentUser.getId(), teacherId, weekStart);
        WeeklyScheduleResponseDTO schedule = managerTeacherService.getTeacherWeeklySchedule(
                currentUser.getId(), teacherId, weekStart);
        return ResponseEntity.ok(ResponseObject.success(schedule));
    }
}

