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
    // Additional fields for non-student users
    private boolean isUserAccount; // true if email/phone belongs to existing user (but not student)
    private String userRole; // e.g., "TEACHER", "QA", "ADMIN" for display

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
