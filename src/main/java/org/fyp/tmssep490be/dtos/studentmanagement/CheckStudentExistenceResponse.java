package org.fyp.tmssep490be.dtos.studentmanagement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckStudentExistenceResponse {

    private boolean exists;
    private Long studentId;
    private String studentCode;
    private String fullName;
    private String email;
    private String phone;
    private List<BranchInfo> currentBranches;
    private boolean canAddToCurrentBranch;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchInfo {
        private Long id;
        private String name;
        private String code;
    }
}
