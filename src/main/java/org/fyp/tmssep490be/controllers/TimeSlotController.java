package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.resource.*;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.TimeSlotTemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.fyp.tmssep490be.entities.enums.ResourceStatus;
import java.util.Map;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Quản lý Khung giờ")
@SecurityRequirement(name = "bearerAuth")
public class TimeSlotController {

    private final TimeSlotTemplateService timeSlotTemplateService;

    // Lấy danh sách khung giờ
    @GetMapping("/time-slots")
    @PreAuthorize("hasAnyRole('CENTER_HEAD', 'ACADEMIC_AFFAIR', 'TEACHER', 'MANAGER')")
    @Operation(summary = "Get all time slots")
    public ResponseEntity<List<TimeSlotResponseDTO>> getAllTimeSlots(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        List<TimeSlotResponseDTO> timeSlots = timeSlotTemplateService.getAllTimeSlots(
                branchId, search, currentUser.getId());
        return ResponseEntity.ok(timeSlots);
    }

    // Lấy chi tiết 1 khung giờ
    @GetMapping("/time-slots/{id}")
    @PreAuthorize("hasAnyRole('CENTER_HEAD', 'ACADEMIC_AFFAIR', 'MANAGER')")
    @Operation(summary = "Get time slot by ID")
    public ResponseEntity<TimeSlotResponseDTO> getTimeSlotById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        TimeSlotResponseDTO timeSlot = timeSlotTemplateService.getTimeSlotById(id);
        return ResponseEntity.ok(timeSlot);
    }

    // Tạo khung giờ mới
    @PostMapping("/time-slots")
    @PreAuthorize("hasRole('CENTER_HEAD')")
    @Operation(summary = "Create new time slot")
    public ResponseEntity<TimeSlotResponseDTO> createTimeSlot(
            @RequestBody TimeSlotRequestDTO request,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        TimeSlotResponseDTO saved = timeSlotTemplateService.createTimeSlot(request, currentUser.getId(), null);
        return ResponseEntity.ok(saved);
    }

    // Cập nhật khung giờ
    @PutMapping("/time-slots/{id}")
    @PreAuthorize("hasRole('CENTER_HEAD')")
    @Operation(summary = "Update time slot")
    public ResponseEntity<TimeSlotResponseDTO> updateTimeSlot(
            @PathVariable Long id,
            @RequestBody TimeSlotRequestDTO request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        TimeSlotResponseDTO saved = timeSlotTemplateService.updateTimeSlot(id, request, currentUser.getId());
        return ResponseEntity.ok(saved);
    }

    // Xóa khung giờ
    @DeleteMapping("/time-slots/{id}")
    @PreAuthorize("hasRole('CENTER_HEAD')")
    @Operation(summary = "Delete time slot")
    public ResponseEntity<Void> deleteTimeSlot(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        timeSlotTemplateService.deleteTimeSlot(id);
        return ResponseEntity.noContent().build();
    }

    // Đổi trạng thái
    @PatchMapping("/time-slots/{id}/status")
    @PreAuthorize("hasRole('CENTER_HEAD')")
    @Operation(summary = "Update time slot status")
    public ResponseEntity<TimeSlotResponseDTO> updateTimeSlotStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        if (!request.containsKey("status")) {
            throw new RuntimeException("Field 'status' is required");
        }
        ResourceStatus status = ResourceStatus.valueOf(request.get("status"));
        TimeSlotResponseDTO saved = timeSlotTemplateService.updateTimeSlotStatus(id, status);
        return ResponseEntity.ok(saved);
    }

    // Lấy sessions đang dùng khung giờ
    @GetMapping("/time-slots/{id}/sessions")
    @PreAuthorize("hasAnyRole('CENTER_HEAD', 'ACADEMIC_AFFAIR', 'MANAGER')")
    @Operation(summary = "Get sessions using a time slot")
    public ResponseEntity<List<SessionInfoDTO>> getSessionsByTimeSlotId(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<SessionInfoDTO> sessions = timeSlotTemplateService.getSessionsByTimeSlotId(id);
        return ResponseEntity.ok(sessions);
    }

    // Lấy khung giờ cho dropdown
    @GetMapping("/branches/{branchId}/time-slot-templates")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    @Operation(summary = "Get branch time slot templates for dropdown")
    public ResponseEntity<ResponseObject<List<TimeSlotTemplateDTO>>> getBranchTimeSlotTemplates(
            @Parameter(description = "Branch ID") @PathVariable Long branchId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<TimeSlotTemplateDTO> timeSlotDTOs = timeSlotTemplateService.getBranchTimeSlotTemplates(branchId);
        return ResponseEntity.ok(ResponseObject.<List<TimeSlotTemplateDTO>>builder()
                .success(true)
                .message("Time slot templates retrieved successfully")
                .data(timeSlotDTOs)
                .build());
    }

}