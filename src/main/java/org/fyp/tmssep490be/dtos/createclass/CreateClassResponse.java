package org.fyp.tmssep490be.dtos.createclass;

import lombok.*;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateClassResponse {

    private Long classId;
    private String code;
    private String name;
    private ClassStatus status;
    private ApprovalStatus approvalStatus;
    private OffsetDateTime createdAt;

    // Session generation summary
    private SessionGenerationSummary sessionSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionGenerationSummary {
        private int sessionsGenerated;
        private int totalSessionsInCourse;
        private String courseCode;
        private String courseName;
        private LocalDate startDate;
        private LocalDate endDate;
        private Short[] scheduleDays;

        // Helper methods
        public boolean isSessionGenerationSuccessful() {
            return sessionsGenerated > 0;
        }

        public double getGenerationProgress() {
            if (totalSessionsInCourse == 0) return 0.0;
            return (double) sessionsGenerated / totalSessionsInCourse * 100;
        }

        public boolean hasFullCourseCoverage() {
            return sessionsGenerated == totalSessionsInCourse;
        }
    }

    // Helper methods for CreateClassResponse
    public boolean isSuccess() {
        return classId != null && code != null &&
               sessionSummary != null && sessionSummary.isSessionGenerationSuccessful();
    }

    public boolean hasSessions() {
        return sessionSummary != null && sessionSummary.getSessionsGenerated() > 0;
    }

    public boolean isReadyForNextStep() {
        return isSuccess() && hasSessions() &&
               (status == ClassStatus.DRAFT) &&
               (approvalStatus == ApprovalStatus.PENDING);
    }

    /**
     * Returns workflow progress percentage (0-100%)
     * STEP 1 completed = 14.3% (1/7)
     */
    public double getWorkflowProgress() {
        if (!isSuccess()) return 0.0;
        return 14.3; // STEP 1 of 7 steps completed
    }

    /**
     * Returns next workflow step description
     */
    public String getNextStepDescription() {
        if (!isReadyForNextStep()) {
            return "Fix validation errors before proceeding";
        }
        return "STEP 2: Assign time slots to sessions";
    }
}