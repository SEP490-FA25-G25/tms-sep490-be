package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.resource.TimeSlotTemplateDTO;
import org.fyp.tmssep490be.entities.TimeSlotTemplate;
import org.fyp.tmssep490be.repositories.TimeSlotTemplateRepository;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for resource management operations
 * Provides endpoints for querying available resources and time slots
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Resource Management", description = "APIs for managing resources and time slots")
@SecurityRequirement(name = "bearerAuth")
public class ResourceController {

    private final TimeSlotTemplateRepository timeSlotTemplateRepository;

    /**
     * Get available time slot templates for a branch
     * Returns all time slots ordered by start time for selection in class creation
     *
     * @param branchId Branch ID to get time slots for
     * @param currentUser Current authenticated user
     * @return List of time slot templates ordered by start time
     */
    @GetMapping("/branches/{branchId}/time-slot-templates")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    @Operation(
            summary = "Get branch time slot templates",
            description = "Get all available time slot templates for a specific branch, " +
                    "ordered by start time. Used in Phase 1.3: Assign Time Slots (STEP 3) " +
                    "to show available time slots for selection."
    )
    public ResponseEntity<ResponseObject<List<TimeSlotTemplateDTO>>> getBranchTimeSlotTemplates(
            @Parameter(description = "Branch ID")
            @PathVariable Long branchId,

            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        Long userId = currentUser != null ? currentUser.getId() : null;
        log.info("User {} requesting time slot templates for branch {}", userId, branchId);

        List<TimeSlotTemplate> timeSlots = timeSlotTemplateRepository.findByBranchIdOrderByStartTimeAsc(branchId);

        // Convert to DTOs to avoid lazy loading issues
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        List<TimeSlotTemplateDTO> timeSlotDTOs = timeSlots.stream()
                .map(ts -> TimeSlotTemplateDTO.builder()
                        .id(ts.getId())
                        .name(ts.getName())
                        .startTime(ts.getStartTime().toString())
                        .endTime(ts.getEndTime().toString())
                        .displayName(ts.getStartTime().format(formatter) + " - " + ts.getEndTime().format(formatter))
                        .build())
                .collect(Collectors.toList());

        log.info("Found {} time slot templates for branch {}", timeSlotDTOs.size(), branchId);

        return ResponseEntity.ok(ResponseObject.<List<TimeSlotTemplateDTO>>builder()
                .success(true)
                .message("Time slot templates retrieved successfully")
                .data(timeSlotDTOs)
                .build());
    }
}