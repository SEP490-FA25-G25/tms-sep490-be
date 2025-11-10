package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.common.BranchDTO;
import java.util.List;

/**
 * Service interface for branch operations
 */
public interface BranchService {
    
    /**
     * Get all active branches for dropdown selection
     * 
     * @return List of BranchDTO containing id, name, and code
     */
    List<BranchDTO> getAllBranches();
}
