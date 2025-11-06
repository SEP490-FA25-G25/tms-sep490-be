package org.fyp.tmssep490be.dtos.createclass;

import lombok.*;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitClassResponse {

    private boolean success;
    private String message;
    private Long classId;
    private String classCode;
    private ClassStatus status;
    private ApprovalStatus approvalStatus;
    private OffsetDateTime submittedAt;
    private String submittedBy;
    private OffsetDateTime submittedAtTimestamp;

    // Helper methods
    public boolean isSuccess() {
        return success && classId != null && submittedAt != null;
    }

    /**
     * Check if class is ready for Center Head approval
     */
    public boolean isPendingApproval() {
        return isSuccess() &&
               status == ClassStatus.DRAFT &&
               approvalStatus == ApprovalStatus.PENDING;
    }

    /**
     * Get formatted submission summary
     */
    public String getSubmissionSummary() {
        if (!isSuccess()) {
            return "Submission failed";
        }
        return String.format("Class %s submitted for approval at %s",
                classCode, submittedAt);
    }

    /**
     * Get next step description
     */
    public String getNextStepDescription() {
        if (!isSuccess()) {
            return "Fix errors and resubmit";
        }
        return "Waiting for Center Head approval";
    }

    /**
     * Get workflow progress percentage (after STEP 7)
     */
    public double getWorkflowProgress() {
        if (!isSuccess()) return 0.0;
        return 100.0; // All 7 steps completed
    }
}