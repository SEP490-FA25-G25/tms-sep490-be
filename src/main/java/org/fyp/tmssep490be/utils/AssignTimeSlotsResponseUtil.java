package org.fyp.tmssep490be.utils;

import org.fyp.tmssep490be.dtos.createclass.AssignTimeSlotsResponse;
import org.springframework.stereotype.Component;

@Component
public class AssignTimeSlotsResponseUtil {

    /**
     * Checks if time slot assignment was successful
     */
    public boolean isSuccess(AssignTimeSlotsResponse response) {
        return response != null && response.isSuccess() && response.getSessionsUpdated() > 0;
    }

    /**
     * Calculates update progress percentage
     */
    public double getUpdateProgress(AssignTimeSlotsResponse response) {
        if (response == null || response.getTotalSessions() == 0) {
            return 0.0;
        }
        return (double) response.getSessionsUpdated() / response.getTotalSessions() * 100;
    }

    /**
     * Checks if all sessions were updated
     */
    public boolean isFullyUpdated(AssignTimeSlotsResponse response) {
        return isSuccess(response) &&
               response.getSessionsUpdated() == response.getTotalSessions();
    }

    /**
     * Gets count of successful assignments
     */
    public int getSuccessfulAssignments(AssignTimeSlotsResponse response) {
        if (response == null || response.getAssignmentDetails() == null) {
            return 0;
        }
        return (int) response.getAssignmentDetails().stream()
                .filter(AssignTimeSlotsResponse.AssignmentDetail::isSuccessful)
                .count();
    }

    /**
     * Gets count of failed assignments
     */
    public int getFailedAssignments(AssignTimeSlotsResponse response) {
        if (response == null || response.getAssignmentDetails() == null) {
            return 0;
        }
        return (int) response.getAssignmentDetails().stream()
                .filter(detail -> !detail.isSuccessful())
                .count();
    }

    /**
     * Checks if all assignments were successful
     */
    public boolean areAllAssignmentsSuccessful(AssignTimeSlotsResponse response) {
        return response != null &&
               response.getAssignmentDetails() != null &&
               !response.getAssignmentDetails().isEmpty() &&
               response.getAssignmentDetails().stream()
                       .allMatch(AssignTimeSlotsResponse.AssignmentDetail::isSuccessful);
    }

    /**
     * Gets assignment success rate
     */
    public double getSuccessRate(AssignTimeSlotsResponse response) {
        if (response == null || response.getAssignmentDetails() == null ||
            response.getAssignmentDetails().isEmpty()) {
            return 0.0;
        }

        int total = response.getAssignmentDetails().size();
        int successful = getSuccessfulAssignments(response);
        return (double) successful / total * 100;
    }

    /**
     * Gets formatted assignment summary
     */
    public String getAssignmentSummary(AssignTimeSlotsResponse response) {
        if (response == null) {
            return "No assignment data available";
        }

        int successful = getSuccessfulAssignments(response);
        int failed = getFailedAssignments(response);
        double progress = getUpdateProgress(response);

        return String.format(
            "Assignment completed: %d successful, %d failed (%.1f%% progress)",
            successful, failed, progress
        );
    }

    /**
     * Gets detailed status message
     */
    public String getStatusMessage(AssignTimeSlotsResponse response) {
        if (response == null) {
            return "Status unavailable";
        }

        if (!response.isSuccess()) {
            return "Assignment failed: " + response.getMessage();
        }

        if (isFullyUpdated(response)) {
            return "All sessions updated successfully";
        }

        return String.format("Partially updated: %d/%d sessions",
                           response.getSessionsUpdated(), response.getTotalSessions());
    }
}