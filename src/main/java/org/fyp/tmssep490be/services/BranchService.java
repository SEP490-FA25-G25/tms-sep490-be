package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.branch.BranchDTO;
import org.fyp.tmssep490be.entities.Branch;

import java.util.List;

public interface BranchService {
    /**
     * Get all active branches
     * @return List of all active branches
     */
    List<BranchDTO> getAllBranches();
}
