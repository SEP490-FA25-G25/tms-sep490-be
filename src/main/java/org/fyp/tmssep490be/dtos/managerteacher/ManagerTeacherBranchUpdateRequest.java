package org.fyp.tmssep490be.dtos.managerteacher;

import lombok.Data;

import java.util.List;

@Data
public class ManagerTeacherBranchUpdateRequest {
    /**
     * Target list of branch IDs that the teacher should belong to
     * (only branches within the manager's scope will be applied).
     */
    private List<Long> branchIds;
}

