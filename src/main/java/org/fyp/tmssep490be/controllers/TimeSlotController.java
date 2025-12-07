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

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Quản lý Khung giờ")
@SecurityRequirement(name = "bearerAuth")
public class TimeSlotController {

    private final TimeSlotTemplateService timeSlotTemplateService;

    @GetMapping("/time-slots")
    @PreAuthorize("hasAnyRole('CENTER_HEAD', 'ACADEMIC_AFFAIR', 'TEACHER', 'MANAGER')")
    @Operation(summary = "Get all time slots")
    public ResponseEntity<List<TimeSlotResponseDTO>> getAllTimeSlots(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        boolean isCenterHead = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CENTER_HEAD"));
        boolean isTeacher = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));

        List<TimeSlotResponseDTO> timeSlots = timeSlotTemplateService.getAllTimeSlots(
                branchId, search, currentUser.getId(), isCenterHead, isTeacher);
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



}