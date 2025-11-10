package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.BranchDTO;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.services.BranchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for branch operations
 * Provides endpoints for retrieving branch information for dropdowns
 */
@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Branch Management", description = "Branch APIs for dropdown selection")
@SecurityRequirement(name = "bearerAuth")
public class BranchController {

    private final BranchService branchService;

    /**
     * Get all branches for dropdown selection
     * GET /api/v1/branches
     */
    @GetMapping
    @Operation(
            summary = "Get all branches",
            description = "Retrieve all branches for dropdown selection. Returns id, name, and code."
    )
    public ResponseEntity<ResponseObject<List<BranchDTO>>> getAllBranches() {
        log.info("Getting all branches for dropdown");

        List<BranchDTO> branches = branchService.getAllBranches();

        return ResponseEntity.ok(ResponseObject.<List<BranchDTO>>builder()
                .success(true)
                .message("Branches retrieved successfully")
                .data(branches)
                .build());
    }
}
