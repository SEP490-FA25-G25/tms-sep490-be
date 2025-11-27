package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.branch.BranchDTO;
import org.fyp.tmssep490be.dtos.branch.BranchRequestDTO;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.services.BranchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for branch operations
 * Provides endpoints for CRUD operations on branches
 */
@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Branch Management", description = "Branch management APIs")
@SecurityRequirement(name = "bearerAuth")
public class BranchController {

    private final BranchService branchService;

    /**
     * Get all active branches
     * Used for dropdown filters and branch selection in transfer requests
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CENTER_HEAD', 'ACADEMIC_AFFAIR')")
    @Operation(
            summary = "Get all active branches",
            description = "Retrieve list of all active branches for dropdown filters and selection"
    )
    public ResponseEntity<ResponseObject<List<BranchDTO>>> getAllBranches() {
        log.info("Request to get all active branches");

        List<BranchDTO> branches = branchService.getAllBranches();

        return ResponseEntity.ok(ResponseObject.<List<BranchDTO>>builder()
                .success(true)
                .message("Branches retrieved successfully")
                .data(branches)
                .build());
    }

    /**
     * Get all branches by center ID
     */
    @GetMapping("/center/{centerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CENTER_HEAD')")
    @Operation(
            summary = "Get branches by center ID",
            description = "Retrieve list of all branches belonging to a specific center"
    )
    public ResponseEntity<ResponseObject<List<BranchDTO>>> getBranchesByCenterId(@PathVariable Long centerId) {
        log.info("Request to get branches for center ID: {}", centerId);

        List<BranchDTO> branches = branchService.getBranchesByCenterId(centerId);

        return ResponseEntity.ok(ResponseObject.<List<BranchDTO>>builder()
                .success(true)
                .message("Branches retrieved successfully")
                .data(branches)
                .build());
    }

    /**
     * Get branch by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CENTER_HEAD')")
    @Operation(
            summary = "Get branch by ID",
            description = "Retrieve branch details by ID"
    )
    public ResponseEntity<ResponseObject<BranchDTO>> getBranchById(@PathVariable Long id) {
        log.info("Request to get branch with ID: {}", id);

        BranchDTO branch = branchService.getBranchById(id);

        return ResponseEntity.ok(ResponseObject.<BranchDTO>builder()
                .success(true)
                .message("Branch retrieved successfully")
                .data(branch)
                .build());
    }

    /**
     * Create a new branch
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Create branch",
            description = "Create a new branch under a center"
    )
    public ResponseEntity<ResponseObject<BranchDTO>> createBranch(@Valid @RequestBody BranchRequestDTO request) {
        log.info("Request to create branch with code: {}", request.getCode());

        BranchDTO branch = branchService.createBranch(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseObject.<BranchDTO>builder()
                        .success(true)
                        .message("Branch created successfully")
                        .data(branch)
                        .build());
    }

    /**
     * Update branch
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update branch",
            description = "Update branch information"
    )
    public ResponseEntity<ResponseObject<BranchDTO>> updateBranch(
            @PathVariable Long id,
            @Valid @RequestBody BranchRequestDTO request) {
        log.info("Request to update branch with ID: {}", id);

        BranchDTO branch = branchService.updateBranch(id, request);

        return ResponseEntity.ok(ResponseObject.<BranchDTO>builder()
                .success(true)
                .message("Branch updated successfully")
                .data(branch)
                .build());
    }

    /**
     * Delete branch
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Delete branch",
            description = "Delete a branch (only if it has no associated classes or resources)"
    )
    public ResponseEntity<ResponseObject<Void>> deleteBranch(@PathVariable Long id) {
        log.info("Request to delete branch with ID: {}", id);

        branchService.deleteBranch(id);

        return ResponseEntity.ok(ResponseObject.<Void>builder()
                .success(true)
                .message("Branch deleted successfully")
                .build());
    }
}
