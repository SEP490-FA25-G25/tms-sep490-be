package org.fyp.tmssep490be.utils;

import org.fyp.tmssep490be.dtos.classmanagement.ValidateClassResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ValidateClassResponseUtil {

    /**
     * Checks if validation response is valid
     */
    public boolean isValid(ValidateClassResponse response) {
        return response != null && response.getValid() != null && response.getValid();
    }

    /**
     * Checks if class can be submitted for approval
     */
    public boolean canSubmit(ValidateClassResponse response) {
        return response != null && response.getCanSubmit() != null && response.getCanSubmit();
    }

    /**
     * Checks if all time slots are assigned
     */
    public boolean areTimeSlotsComplete(ValidateClassResponse response) {
        if (response == null || response.getChecks() == null) {
            return false;
        }
        return response.getChecks().getAllSessionsHaveTimeSlots() != null &&
               response.getChecks().getAllSessionsHaveTimeSlots();
    }

    /**
     * Checks if all resources are assigned
     */
    public boolean areResourcesComplete(ValidateClassResponse response) {
        if (response == null || response.getChecks() == null) {
            return false;
        }
        return response.getChecks().getAllSessionsHaveResources() != null &&
               response.getChecks().getAllSessionsHaveResources();
    }

    /**
     * Checks if all teachers are assigned
     */
    public boolean areTeachersComplete(ValidateClassResponse response) {
        if (response == null || response.getChecks() == null) {
            return false;
        }
        return response.getChecks().getAllSessionsHaveTeachers() != null &&
               response.getChecks().getAllSessionsHaveTeachers();
    }

    /**
     * Checks if class is fully complete (time slots, resources, teachers)
     */
    public boolean isFullyComplete(ValidateClassResponse response) {
        return areTimeSlotsComplete(response) &&
               areResourcesComplete(response) &&
               areTeachersComplete(response);
    }


    /**
     * Gets formatted completion breakdown
     */
    public String getCompletionBreakdown(ValidateClassResponse response) {
        if (response == null || response.getChecks() == null) {
            return "No completion data available";
        }

        ValidateClassResponse.ValidationChecks checks = response.getChecks();
        Integer completionPct = checks.getCompletionPercentage() != null ? checks.getCompletionPercentage() : 0;
        Long totalSessions = checks.getTotalSessions() != null ? checks.getTotalSessions() : 0L;

        return String.format(
            "Completion: %d%% (%d total sessions) - Time slots: %s, Resources: %s, Teachers: %s",
            completionPct,
            totalSessions,
            areTimeSlotsComplete(response) ? "✅ Complete" : "❌ Incomplete",
            areResourcesComplete(response) ? "✅ Complete" : "❌ Incomplete",
            areTeachersComplete(response) ? "✅ Complete" : "❌ Incomplete"
        );
    }

    /**
     * Gets detailed session breakdown
     */
    public String getSessionBreakdown(ValidateClassResponse response) {
        if (response == null || response.getChecks() == null) {
            return "No session data available";
        }

        ValidateClassResponse.ValidationChecks checks = response.getChecks();
        Long total = checks.getTotalSessions() != null ? checks.getTotalSessions() : 0L;
        Long withoutTimeSlots = checks.getSessionsWithoutTimeSlots() != null ? checks.getSessionsWithoutTimeSlots() : 0L;
        Long withoutResources = checks.getSessionsWithoutResources() != null ? checks.getSessionsWithoutResources() : 0L;
        Long withoutTeachers = checks.getSessionsWithoutTeachers() != null ? checks.getSessionsWithoutTeachers() : 0L;

        return String.format(
            "Sessions: %d total, %d without time slots, %d without resources, %d without teachers",
            total, withoutTimeSlots, withoutResources, withoutTeachers
        );
    }

    /**
     * Gets validation status summary
     */
    public String getStatusSummary(ValidateClassResponse response) {
        if (response == null) {
            return "Validation status unavailable";
        }

        StringBuilder summary = new StringBuilder();

        // Overall status
        if (canSubmit(response) && isFullyComplete(response)) {
            summary.append("✅ Ready for submission - Fully complete");
        } else if (canSubmit(response)) {
            summary.append("⚠️ Ready for submission but has warnings");
        } else {
            summary.append("❌ Not ready for submission");
        }

        // Completion percentage
        Integer completionPct = (response.getChecks() != null && response.getChecks().getCompletionPercentage() != null)
            ? response.getChecks().getCompletionPercentage() : 0;
        summary.append(String.format(" (%d%% complete)", completionPct));

        // Critical issues count
        int criticalIssues = (response.getErrors() != null) ? response.getErrors().size() : 0;
        if (criticalIssues > 0) {
            summary.append(String.format(" - %d critical issues", criticalIssues));
        }

        // Warnings count
        int warnings = (response.getWarnings() != null) ? response.getWarnings().size() : 0;
        if (warnings > 0) {
            summary.append(String.format(" - %d warnings", warnings));
        }

        return summary.toString();
    }
}