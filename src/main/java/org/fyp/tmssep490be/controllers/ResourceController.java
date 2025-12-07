package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.resource.*;
import org.fyp.tmssep490be.entities.enums.ResourceStatus;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.ResourceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Quản lý Tài nguyên")
@SecurityRequirement(name = "bearerAuth")
public class ResourceController {

    private final ResourceService resourceService;

    // GET /resources - Lấy danh sách
    @GetMapping("/resources")
    @PreAuthorize("hasAnyRole('CENTER_HEAD', 'ACADEMIC_AFFAIR', 'MANAGER')")
    @Operation(summary = "Get all resources")
    public ResponseEntity<List<ResourceDTO>> getAllResources(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        List<ResourceDTO> resources = resourceService.getAllResources(
                branchId, resourceType, search, currentUser.getId());
        return ResponseEntity.ok(resources);
    }

    // GET /resources/{id} - Lấy chi tiết
    @GetMapping("/resources/{id}")
    @PreAuthorize("hasAnyRole('CENTER_HEAD', 'ACADEMIC_AFFAIR', 'MANAGER')")
    @Operation(summary = "Get resource by ID")
    public ResponseEntity<ResourceDTO> getResourceById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ResourceDTO resource = resourceService.getResourceById(id);
        return ResponseEntity.ok(resource);
    }
}