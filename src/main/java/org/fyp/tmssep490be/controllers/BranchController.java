package org.fyp.tmssep490be.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.branch.BranchDTO;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.entities.Branch;
import org.fyp.tmssep490be.entities.enums.BranchStatus;
import org.fyp.tmssep490be.repositories.BranchRepository;
import org.fyp.tmssep490be.repositories.UserBranchesRepository;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
@Slf4j
public class BranchController {

    private final BranchRepository branchRepository;
    private final UserBranchesRepository userBranchesRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CENTER_HEAD', 'ACADEMIC_AFFAIR', 'QA')")
    public ResponseEntity<ResponseObject<List<BranchDTO>>> getAllBranches() {
        log.info("Request to get all active branches");

        List<BranchDTO> branches = branchRepository.findAll().stream()
                .filter(branch -> branch.getStatus() == BranchStatus.ACTIVE)
                .map(this::mapToBranchDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ResponseObject.<List<BranchDTO>>builder()
                .success(true)
                .message("Branches retrieved successfully")
                .data(branches)
                .build());
    }

 
    @GetMapping("/my-branches")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResponseObject<List<BranchDTO>>> getMyBranches(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("Request to get branches for user ID: {}", currentUser.getId());

        List<Long> branchIds = userBranchesRepository.findBranchIdsByUserId(currentUser.getId());

        if (branchIds.isEmpty()) {
            log.warn("No branches assigned to user ID: {}", currentUser.getId());
            return ResponseEntity.ok(ResponseObject.<List<BranchDTO>>builder()
                    .success(true)
                    .message("No branches assigned")
                    .data(List.of())
                    .build());
        }

        List<BranchDTO> branches = branchRepository.findAllById(branchIds).stream()
                .filter(branch -> branch.getStatus() == BranchStatus.ACTIVE)
                .map(this::mapToBranchDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ResponseObject.<List<BranchDTO>>builder()
                .success(true)
                .message("My branches retrieved successfully")
                .data(branches)
                .build());
    }

    private BranchDTO mapToBranchDTO(Branch branch) {
        return BranchDTO.builder()
                .id(branch.getId())
                .centerId(branch.getCenter() != null ? branch.getCenter().getId() : null)
                .centerName(branch.getCenter() != null ? branch.getCenter().getName() : null)
                .code(branch.getCode())
                .name(branch.getName())
                .address(branch.getAddress())
                .city(branch.getCity())
                .district(branch.getDistrict())
                .phone(branch.getPhone())
                .email(branch.getEmail())
                .status(branch.getStatus() != null ? branch.getStatus().name() : null)
                .openingDate(branch.getOpeningDate())
                .createdAt(branch.getCreatedAt())
                .updatedAt(branch.getUpdatedAt())
                .build();
    }
}
