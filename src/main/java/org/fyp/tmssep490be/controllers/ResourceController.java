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

    // POST /resources - Tạo mới
    @PostMapping("/resources")
    @PreAuthorize("hasRole('CENTER_HEAD')")
    @Operation(summary = "Create new resource")
    public ResponseEntity<ResourceDTO> createResource(
            @RequestBody ResourceRequestDTO request,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        ResourceDTO saved = resourceService.createResource(request, currentUser.getId());
        return ResponseEntity.ok(saved);
    }

    // PUT /resources/{id} - Cập nhật
    @PutMapping("/resources/{id}")
    @PreAuthorize("hasRole('CENTER_HEAD')")
    @Operation(summary = "Update resource")
    public ResponseEntity<ResourceDTO> updateResource(
            @PathVariable Long id,
            @RequestBody ResourceRequestDTO request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ResourceDTO saved = resourceService.updateResource(id, request, currentUser.getId());
        return ResponseEntity.ok(saved);
    }

    // DELETE /resources/{id} - Xóa
    @DeleteMapping("/resources/{id}")
    @PreAuthorize("hasRole('CENTER_HEAD')")
    @Operation(summary = "Delete resource")
    public ResponseEntity<Void> deleteResource(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        resourceService.deleteResource(id);
        return ResponseEntity.noContent().build();
    }

    // PATCH /resources/{id}/status - Đổi trạng thái
    @PatchMapping("/resources/{id}/status")
    @PreAuthorize("hasRole('CENTER_HEAD')")
    @Operation(summary = "Update resource status")
    public ResponseEntity<ResourceDTO> updateResourceStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        if (!request.containsKey("status")) {
            throw new RuntimeException("Field 'status' is required");
        }
        ResourceStatus status = ResourceStatus.valueOf(request.get("status"));
        ResourceDTO saved = resourceService.updateResourceStatus(id, status);
        return ResponseEntity.ok(saved);
    }

    // GET /resources/{id}/sessions - Lấy sessions đang dùng
    @GetMapping("/resources/{id}/sessions")
    @PreAuthorize("hasAnyRole('CENTER_HEAD', 'ACADEMIC_AFFAIR', 'MANAGER')")
    @Operation(summary = "Get sessions using a resource")
    public ResponseEntity<List<SessionInfoDTO>> getSessionsByResourceId(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<SessionInfoDTO> sessions = resourceService.getSessionsByResourceId(id);
        return ResponseEntity.ok(sessions);
    }
}