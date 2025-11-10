package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.BranchDTO;
import org.fyp.tmssep490be.repositories.BranchRepository;
import org.fyp.tmssep490be.services.BranchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of BranchService
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;

    @Override
    public List<BranchDTO> getAllBranches() {
        log.debug("Getting all branches for dropdown");
        
        return branchRepository.findAll().stream()
                .map(branch -> BranchDTO.builder()
                        .id(branch.getId())
                        .name(branch.getName())
                        .code(branch.getCode())
                        .build())
                .collect(Collectors.toList());
    }
}
