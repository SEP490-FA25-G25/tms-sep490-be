package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.resource.TimeSlotTemplateDTO;
import org.fyp.tmssep490be.entities.Branch;
import org.fyp.tmssep490be.entities.Resource;
import org.fyp.tmssep490be.entities.TimeSlotTemplate;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.enums.ResourceType;
import org.fyp.tmssep490be.repositories.BranchRepository;
import org.fyp.tmssep490be.repositories.ResourceRepository;
import org.fyp.tmssep490be.repositories.TimeSlotTemplateRepository;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        private final ResourceRepository resourceRepository;
        private final BranchRepository branchRepository;
        private final UserAccountRepository userAccountRepository;

        // ==================== RESOURCE CRUD ENDPOINTS ====================

        /**
         * Get all resources with optional filters
         */
        @GetMapping("/resources")
        @PreAuthorize("hasAnyRole('CENTER_HEAD', 'ACADEMIC_AFFAIR')")
        @Operation(summary = "Get all resources", description = "Get all resources with optional filters for branch, type, and search")
        public ResponseEntity<List<Map<String, Object>>> getAllResources(
                        @RequestParam(required = false) Long branchId,
                        @RequestParam(required = false) String resourceType,
                        @RequestParam(required = false) String search,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("User {} requesting resources - branchId: {}, type: {}, search: {}",
                                currentUser.getId(), branchId, resourceType, search);

                List<Resource> resources = resourceRepository.findAll();

                // Apply filters
                if (branchId != null) {
                        resources = resources.stream()
                                        .filter(r -> r.getBranch().getId().equals(branchId))
                                        .collect(Collectors.toList());
                }
                if (resourceType != null && !resourceType.isEmpty()) {
                        ResourceType type = ResourceType.valueOf(resourceType);
                        resources = resources.stream()
                                        .filter(r -> r.getResourceType() == type)
                                        .collect(Collectors.toList());
                }
                if (search != null && !search.trim().isEmpty()) {
                        String searchLower = search.toLowerCase().trim();
                        resources = resources.stream()
                                        .filter(r -> r.getName().toLowerCase().contains(searchLower) ||
                                                        r.getCode().toLowerCase().contains(searchLower))
                                        .collect(Collectors.toList());
                }

                List<Map<String, Object>> resourceDTOs = resources.stream()
                                .map(this::convertResourceToDTO)
                                .collect(Collectors.toList());

                log.info("Found {} resources", resourceDTOs.size());
                return ResponseEntity.ok(resourceDTOs);
        }

        /**
         * Get resource by ID
         */
        @GetMapping("/resources/{id}")
        @PreAuthorize("hasAnyRole('CENTER_HEAD', 'ACADEMIC_AFFAIR')")
        @Operation(summary = "Get resource by ID")
        public ResponseEntity<Map<String, Object>> getResourceById(
                        @PathVariable Long id,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("User {} requesting resource {}", currentUser.getId(), id);

                Resource resource = resourceRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Resource not found with id: " + id));

                return ResponseEntity.ok(convertResourceToDTO(resource));
        }

        /**
         * Create new resource
         */
        @PostMapping("/resources")
        @PreAuthorize("hasRole('CENTER_HEAD')")
        @Operation(summary = "Create new resource")
        public ResponseEntity<Map<String, Object>> createResource(
                        @RequestBody Map<String, Object> request,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("User {} creating resource: {}", currentUser.getId(), request);

                Resource resource = new Resource();
                updateResourceFromRequest(resource, request, currentUser);
                resource.setCreatedAt(OffsetDateTime.now());
                resource.setUpdatedAt(OffsetDateTime.now());

                Resource saved = resourceRepository.save(resource);
                log.info("Created resource with ID: {}", saved.getId());

                return ResponseEntity.ok(convertResourceToDTO(saved));
        }

        /**
         * Update resource
         */
        @PutMapping("/resources/{id}")
        @PreAuthorize("hasRole('CENTER_HEAD')")
        @Operation(summary = "Update resource")
        public ResponseEntity<Map<String, Object>> updateResource(
                        @PathVariable Long id,
                        @RequestBody Map<String, Object> request,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("User {} updating resource {}: {}", currentUser.getId(), id, request);

                Resource resource = resourceRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Resource not found with id: " + id));

                updateResourceFromRequest(resource, request, currentUser);
                resource.setUpdatedAt(OffsetDateTime.now());

                Resource saved = resourceRepository.save(resource);
                log.info("Updated resource with ID: {}", saved.getId());

                return ResponseEntity.ok(convertResourceToDTO(saved));
        }

        /**
         * Delete resource
         */
        @DeleteMapping("/resources/{id}")
        @PreAuthorize("hasRole('CENTER_HEAD')")
        @Operation(summary = "Delete resource")
        public ResponseEntity<Void> deleteResource(
                        @PathVariable Long id,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("User {} deleting resource {}", currentUser.getId(), id);

                if (!resourceRepository.existsById(id)) {
                        throw new RuntimeException("Resource not found with id: " + id);
                }

                resourceRepository.deleteById(id);
                log.info("Deleted resource with ID: {}", id);

                return ResponseEntity.noContent().build();
        }

        // ==================== TIME SLOT CRUD ENDPOINTS ====================

        /**
         * Get all time slots with optional filters
         */
        @GetMapping("/time-slots")
        @PreAuthorize("hasAnyRole('CENTER_HEAD', 'ACADEMIC_AFFAIR')")
        @Operation(summary = "Get all time slots", description = "Get all time slot templates with optional filters")
        public ResponseEntity<List<Map<String, Object>>> getAllTimeSlots(
                        @RequestParam(required = false) Long branchId,
                        @RequestParam(required = false) String search,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("User {} requesting time slots - branchId: {}, search: {}",
                                currentUser.getId(), branchId, search);

                List<TimeSlotTemplate> timeSlots = timeSlotTemplateRepository.findAll();

                // Apply filters
                if (branchId != null) {
                        timeSlots = timeSlots.stream()
                                        .filter(ts -> ts.getBranch().getId().equals(branchId))
                                        .collect(Collectors.toList());
                }
                if (search != null && !search.trim().isEmpty()) {
                        String searchLower = search.toLowerCase().trim();
                        timeSlots = timeSlots.stream()
                                        .filter(ts -> ts.getName().toLowerCase().contains(searchLower))
                                        .collect(Collectors.toList());
                }

                List<Map<String, Object>> timeSlotDTOs = timeSlots.stream()
                                .map(this::convertTimeSlotToDTO)
                                .collect(Collectors.toList());

                log.info("Found {} time slots", timeSlotDTOs.size());
                return ResponseEntity.ok(timeSlotDTOs);
        }

        /**
         * Get time slot by ID
         */
        @GetMapping("/time-slots/{id}")
        @PreAuthorize("hasAnyRole('CENTER_HEAD', 'ACADEMIC_AFFAIR')")
        @Operation(summary = "Get time slot by ID")
        public ResponseEntity<Map<String, Object>> getTimeSlotById(
                        @PathVariable Long id,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("User {} requesting time slot {}", currentUser.getId(), id);

                TimeSlotTemplate timeSlot = timeSlotTemplateRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Time slot not found with id: " + id));

                return ResponseEntity.ok(convertTimeSlotToDTO(timeSlot));
        }

        /**
         * Create new time slot
         */
        @PostMapping("/time-slots")
        @PreAuthorize("hasRole('CENTER_HEAD')")
        @Operation(summary = "Create new time slot")
        public ResponseEntity<Map<String, Object>> createTimeSlot(
                        @RequestBody Map<String, Object> request,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("User {} creating time slot: {}", currentUser.getId(), request);

                TimeSlotTemplate timeSlot = new TimeSlotTemplate();
                updateTimeSlotFromRequest(timeSlot, request);
                timeSlot.setCreatedAt(OffsetDateTime.now());
                timeSlot.setUpdatedAt(OffsetDateTime.now());

                TimeSlotTemplate saved = timeSlotTemplateRepository.save(timeSlot);
                log.info("Created time slot with ID: {}", saved.getId());

                return ResponseEntity.ok(convertTimeSlotToDTO(saved));
        }

        /**
         * Update time slot
         */
        @PutMapping("/time-slots/{id}")
        @PreAuthorize("hasRole('CENTER_HEAD')")
        @Operation(summary = "Update time slot")
        public ResponseEntity<Map<String, Object>> updateTimeSlot(
                        @PathVariable Long id,
                        @RequestBody Map<String, Object> request,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("User {} updating time slot {}: {}", currentUser.getId(), id, request);

                TimeSlotTemplate timeSlot = timeSlotTemplateRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Time slot not found with id: " + id));

                updateTimeSlotFromRequest(timeSlot, request);
                timeSlot.setUpdatedAt(OffsetDateTime.now());

                TimeSlotTemplate saved = timeSlotTemplateRepository.save(timeSlot);
                log.info("Updated time slot with ID: {}", saved.getId());

                return ResponseEntity.ok(convertTimeSlotToDTO(saved));
        }

        /**
         * Delete time slot
         */
        @DeleteMapping("/time-slots/{id}")
        @PreAuthorize("hasRole('CENTER_HEAD')")
        @Operation(summary = "Delete time slot")
        public ResponseEntity<Void> deleteTimeSlot(
                        @PathVariable Long id,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("User {} deleting time slot {}", currentUser.getId(), id);

                if (!timeSlotTemplateRepository.existsById(id)) {
                        throw new RuntimeException("Time slot not found with id: " + id);
                }

                timeSlotTemplateRepository.deleteById(id);
                log.info("Deleted time slot with ID: {}", id);

                return ResponseEntity.noContent().build();
        }

        // ==================== EXISTING ENDPOINT FOR ACADEMIC AFFAIR
        // ====================

        /**
         * Get available time slot templates for a branch
         * Returns all time slots ordered by start time for selection in class creation
         *
         * @param branchId    Branch ID to get time slots for
         * @param currentUser Current authenticated user
         * @return List of time slot templates ordered by start time
         */
        @GetMapping("/branches/{branchId}/time-slot-templates")
        @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
        @Operation(summary = "Get branch time slot templates", description = "Get all available time slot templates for a specific branch, "
                        +
                        "ordered by start time. Used in Phase 1.3: Assign Time Slots (STEP 3) " +
                        "to show available time slots for selection.")
        public ResponseEntity<ResponseObject<List<TimeSlotTemplateDTO>>> getBranchTimeSlotTemplates(
                        @Parameter(description = "Branch ID") @PathVariable Long branchId,

                        @AuthenticationPrincipal UserPrincipal currentUser) {
                Long userId = currentUser != null ? currentUser.getId() : null;
                log.info("User {} requesting time slot templates for branch {}", userId, branchId);

                List<TimeSlotTemplate> timeSlots = timeSlotTemplateRepository
                                .findByBranchIdOrderByStartTimeAsc(branchId);

                // Convert to DTOs to avoid lazy loading issues
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                List<TimeSlotTemplateDTO> timeSlotDTOs = timeSlots.stream()
                                .map(ts -> TimeSlotTemplateDTO.builder()
                                                .id(ts.getId())
                                                .name(ts.getName())
                                                .startTime(ts.getStartTime().toString())
                                                .endTime(ts.getEndTime().toString())
                                                .displayName(ts.getStartTime().format(formatter) + " - "
                                                                + ts.getEndTime().format(formatter))
                                                .build())
                                .collect(Collectors.toList());

                log.info("Found {} time slot templates for branch {}", timeSlotDTOs.size(), branchId);

                return ResponseEntity.ok(ResponseObject.<List<TimeSlotTemplateDTO>>builder()
                                .success(true)
                                .message("Time slot templates retrieved successfully")
                                .data(timeSlotDTOs)
                                .build());
        }

        // ==================== HELPER METHODS ====================

        private Map<String, Object> convertResourceToDTO(Resource resource) {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", resource.getId());
                dto.put("branchId", resource.getBranch().getId());
                dto.put("branchName", resource.getBranch().getName());
                dto.put("resourceType", resource.getResourceType().toString());
                dto.put("code", resource.getCode());
                dto.put("name", resource.getName());
                dto.put("description", resource.getDescription());
                dto.put("capacity", resource.getCapacity());
                dto.put("capacityOverride", resource.getCapacityOverride());
                dto.put("equipment", resource.getEquipment());
                dto.put("meetingUrl", resource.getMeetingUrl());
                dto.put("meetingId", resource.getMeetingId());
                dto.put("meetingPasscode", resource.getMeetingPasscode());
                dto.put("accountEmail", resource.getAccountEmail());
                dto.put("licenseType", resource.getLicenseType());
                dto.put("expiryDate", resource.getExpiryDate() != null ? resource.getExpiryDate().toString() : null);
                dto.put("renewalDate", resource.getRenewalDate() != null ? resource.getRenewalDate().toString() : null);
                dto.put("createdAt", resource.getCreatedAt().toString());
                dto.put("updatedAt", resource.getUpdatedAt().toString());
                return dto;
        }

        private Map<String, Object> convertTimeSlotToDTO(TimeSlotTemplate timeSlot) {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", timeSlot.getId());
                dto.put("branchId", timeSlot.getBranch().getId());
                dto.put("branchName", timeSlot.getBranch().getName());
                dto.put("name", timeSlot.getName());
                dto.put("startTime", timeSlot.getStartTime().toString());
                dto.put("endTime", timeSlot.getEndTime().toString());
                dto.put("createdAt", timeSlot.getCreatedAt().toString());
                dto.put("updatedAt", timeSlot.getUpdatedAt().toString());
                return dto;
        }

        private void updateResourceFromRequest(Resource resource, Map<String, Object> request,
                        UserPrincipal currentUser) {
                if (request.containsKey("branchId")) {
                        Long branchId = Long.valueOf(request.get("branchId").toString());
                        Branch branch = branchRepository.findById(branchId)
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Branch not found with id: " + branchId));
                        resource.setBranch(branch);
                }

                if (request.containsKey("resourceType")) {
                        resource.setResourceType(ResourceType.valueOf(request.get("resourceType").toString()));
                }
                if (request.containsKey("code")) {
                        resource.setCode(request.get("code").toString());
                }
                if (request.containsKey("name")) {
                        resource.setName(request.get("name").toString());
                }
                if (request.containsKey("description")) {
                        resource.setDescription(request.get("description").toString());
                }
                if (request.containsKey("capacity")) {
                        resource.setCapacity(Integer.valueOf(request.get("capacity").toString()));
                }
                if (request.containsKey("capacityOverride")) {
                        resource.setCapacityOverride(Integer.valueOf(request.get("capacityOverride").toString()));
                }
                if (request.containsKey("equipment")) {
                        resource.setEquipment(request.get("equipment").toString());
                }
                if (request.containsKey("meetingUrl")) {
                        resource.setMeetingUrl(request.get("meetingUrl").toString());
                }
                if (request.containsKey("meetingId")) {
                        resource.setMeetingId(request.get("meetingId").toString());
                }
                if (request.containsKey("meetingPasscode")) {
                        resource.setMeetingPasscode(request.get("meetingPasscode").toString());
                }
                if (request.containsKey("accountEmail")) {
                        resource.setAccountEmail(request.get("accountEmail").toString());
                }
                if (request.containsKey("accountPassword")) {
                        resource.setAccountPassword(request.get("accountPassword").toString());
                }
                if (request.containsKey("licenseType")) {
                        resource.setLicenseType(request.get("licenseType").toString());
                }
                if (request.containsKey("expiryDate")) {
                        resource.setExpiryDate(LocalDate.parse(request.get("expiryDate").toString()));
                }
                if (request.containsKey("renewalDate")) {
                        resource.setRenewalDate(LocalDate.parse(request.get("renewalDate").toString()));
                }

                if (resource.getCreatedBy() == null && currentUser != null) {
                        UserAccount user = userAccountRepository.findById(currentUser.getId())
                                        .orElse(null);
                        resource.setCreatedBy(user);
                }
        }

        private void updateTimeSlotFromRequest(TimeSlotTemplate timeSlot, Map<String, Object> request) {
                if (request.containsKey("branchId")) {
                        Long branchId = Long.valueOf(request.get("branchId").toString());
                        Branch branch = branchRepository.findById(branchId)
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Branch not found with id: " + branchId));
                        timeSlot.setBranch(branch);
                }

                if (request.containsKey("name")) {
                        timeSlot.setName(request.get("name").toString());
                }
                if (request.containsKey("startTime")) {
                        timeSlot.setStartTime(LocalTime.parse(request.get("startTime").toString()));
                }
                if (request.containsKey("endTime")) {
                        timeSlot.setEndTime(LocalTime.parse(request.get("endTime").toString()));
                }
        }
}