package org.fyp.tmssep490be.dtos.studentmanagement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.Gender;
import org.fyp.tmssep490be.entities.enums.UserStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncToBranchResponse {

    private Long studentId;
    private String studentCode;
    private Long userAccountId;
    private String email;
    private String fullName;
    private String phone;
    private Gender gender;
    private LocalDate dob;
    private UserStatus status;
    private List<BranchInfo> allBranches; 
    private BranchInfo newlyAddedBranch; 
    private Integer newSkillAssessmentsCreated; 
    private OffsetDateTime syncedAt;
    private SyncedByInfo syncedBy;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchInfo {
        private Long id;
        private String name;
        private String code;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncedByInfo {
        private Long userId;
        private String fullName;
    }
}
