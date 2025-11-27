package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.branch.BranchDTO;
import org.fyp.tmssep490be.dtos.branch.BranchRequestDTO;

import java.util.List;

public interface BranchService {
    /**
     * Get all active branches
     * @return List of all active branches
     */
    List<BranchDTO> getAllBranches();

    /**
     * Get all branches by center ID
     * @param centerId Center ID
     * @return List of branches belonging to the center
     */
    List<BranchDTO> getBranchesByCenterId(Long centerId);

    /**
     * Get branch by ID
     * @param id Branch ID
     * @return Branch DTO
     */
    BranchDTO getBranchById(Long id);

    /**
     * Create a new branch
     * @param request Branch request
     * @return Created branch DTO
     */
    BranchDTO createBranch(BranchRequestDTO request);

    /**
     * Update branch
     * @param id Branch ID
     * @param request Branch request
     * @return Updated branch DTO
     */
    BranchDTO updateBranch(Long id, BranchRequestDTO request);

    /**
     * Delete branch
     * @param id Branch ID
     */
    void deleteBranch(Long id);
}
