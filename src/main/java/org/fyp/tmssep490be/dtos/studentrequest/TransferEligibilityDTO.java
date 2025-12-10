package org.fyp.tmssep490be.dtos.studentrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferEligibilityDTO {

    private Boolean eligibleForTransfer;
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
        private Long subjectId;
        private String subjectName;
        private Long branchId;
        private String branchName;
        private String modality;
        private String enrollmentStatus;
        private String enrollmentDate;
        private String scheduleInfo;
        private TransferQuota transferQuota;
        private Boolean hasPendingTransfer;
        private Boolean canTransfer;
        private List<SessionInfo> allSessions; // All sessions for timeline view
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionInfo {
        private Long sessionId;
        private java.time.LocalDate date;
        private Integer subjectSessionNumber;
        private String subjectSessionTitle;
        private String timeSlot;
        private String status; // PLANNED, DONE, CANCELLED
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferQuota {
        private Integer used;
        private Integer limit;
        private Integer remaining;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferPolicyInfo {
        private Integer maxTransfersPerSubject;
        private Boolean requiresAAApproval;
        private String policyDescription;
    }
}
