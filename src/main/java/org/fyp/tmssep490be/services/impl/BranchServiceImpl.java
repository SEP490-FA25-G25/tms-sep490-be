package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.branch.BranchDTO;
import org.fyp.tmssep490be.entities.Branch;
import org.fyp.tmssep490be.entities.enums.BranchStatus;
import org.fyp.tmssep490be.repositories.BranchRepository;
import org.fyp.tmssep490be.services.BranchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;

    @Override
    public List<BranchDTO> getAllBranches() {
        log.info("Fetching all active branches");
        List<Branch> branches = branchRepository.findAll();

        return branches.stream()
                .filter(branch -> branch.getStatus() == BranchStatus.ACTIVE)
                .map(this::mapToBranchDTO)
                .collect(Collectors.toList());
    }

    private BranchDTO mapToBranchDTO(Branch branch) {
        return BranchDTO.builder()
                .id(branch.getId())
                .code(branch.getCode())
                .name(branch.getName())
                .address(branch.getAddress())
                .city(branch.getCity())
                .district(branch.getDistrict())
                .status(branch.getStatus().name())
                .build();
    }
}
