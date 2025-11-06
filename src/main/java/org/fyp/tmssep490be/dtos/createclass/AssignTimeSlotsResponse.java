package org.fyp.tmssep490be.dtos.createclass;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignTimeSlotsResponse {

    private boolean success;
    private String message;
    private Long classId;
    private String classCode;
    private int totalSessions;
    private int sessionsUpdated;
    private OffsetDateTime updatedAt;
    private List<AssignmentDetail> assignmentDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentDetail {
        private Short dayOfWeek;
        private String dayName;
        private Long timeSlotTemplateId;
        private String timeSlotName;
        private String startTime;
        private String endTime;
        private int sessionsAffected;
        private boolean successful;
        private String errorMessage;
    }

    // Helper methods
    public boolean isSuccess() {
        return success && sessionsUpdated > 0;
    }

    public double getUpdateProgress() {
        if (totalSessions == 0) return 0.0;
        return (double) sessionsUpdated / totalSessions * 100;
    }

    public boolean isFullyUpdated() {
        return isSuccess() && sessionsUpdated == totalSessions;
    }

    /**
     * Get count of successful assignments
     */
    public int getSuccessfulAssignments() {
        if (assignmentDetails == null) return 0;
        return (int) assignmentDetails.stream()
                .filter(AssignmentDetail::isSuccessful)
                .count();
    }

    /**
     * Get count of failed assignments
     */
    public int getFailedAssignments() {
        if (assignmentDetails == null) return 0;
        return (int) assignmentDetails.stream()
                .filter(detail -> !detail.isSuccessful())
                .count();
    }

    /**
     * Check if all assignments were successful
     */
    public boolean areAllAssignmentsSuccessful() {
        return assignmentDetails != null &&
               !assignmentDetails.isEmpty() &&
               assignmentDetails.stream().allMatch(AssignmentDetail::isSuccessful);
    }
}