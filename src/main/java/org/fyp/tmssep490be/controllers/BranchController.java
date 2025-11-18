package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.branch.BranchDTO;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.services.BranchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for branch operations
 * Provides endpoints for listing branches
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
}
