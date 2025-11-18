package org.fyp.tmssep490be.dtos.studentrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for transfer eligibility information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferEligibilityDTO {

    private boolean eligibleForTransfer;
    private String ineligibilityReason;
    private List<CurrentClassInfo> currentClasses;
    private TransferPolicyInfo policyInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentClassInfo {
        private Long enrollmentId;
        private Long classId;
        private String classCode;
        private String className;
        private Long courseId;
        private String courseName;
        private String branchName;
        private String learningMode;
        private String scheduleInfo;
        private LocalDate enrollmentDate;
        private boolean canTransfer;
        private boolean hasPendingTransfer;
        private TransferQuotaInfo transferQuota;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferQuotaInfo {
        private int used;
        private int limit;
        private int remaining;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferPolicyInfo {
        private int maxTransfersPerCourse;
        private int usedTransfers;
        private int remainingTransfers;
        private boolean requiresAAApproval;
        private String policyDescription;
    }
}