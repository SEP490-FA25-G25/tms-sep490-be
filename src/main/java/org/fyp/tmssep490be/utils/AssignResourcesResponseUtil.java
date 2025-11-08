package org.fyp.tmssep490be.utils;

import org.fyp.tmssep490be.dtos.createclass.AssignResourcesResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for processing AssignResourcesResponse
 * <p>
 * Provides helper methods for:
 * <ul>
 *   <li>Checking assignment success</li>
 *   <li>Calculating assignment progress</li>
 *   <li>Analyzing conflicts</li>
 *   <li>Generating summary messages</li>
 * </ul>
 * </p>
 *
 * @see AssignResourcesResponse
 */
@Component
public class AssignResourcesResponseUtil {

    /**
     * Checks if resource assignment was fully successful (no conflicts)
     *
     * @param response the assignment response
     * @return true if all sessions assigned successfully
     */
    public boolean isFullySuccessful(AssignResourcesResponse response) {
        return response != null &&
               response.getConflictCount() == 0 &&
               response.getSuccessCount() > 0;
    }

    /**
     * Checks if there are conflicts requiring manual resolution
     *
     * @param response the assignment response
     * @return true if conflicts exist
     */
    public boolean hasConflicts(AssignResourcesResponse response) {
        return response != null &&
               response.getConflictCount() != null &&
               response.getConflictCount() > 0;
    }

    /**
     * Calculates assignment progress percentage
     *
     * @param response the assignment response
     * @return progress percentage (0-100)
     */
    public double getAssignmentProgress(AssignResourcesResponse response) {
        if (response == null || response.getTotalSessions() == null || response.getTotalSessions() == 0) {
            return 0.0;
        }
        return (double) response.getSuccessCount() / response.getTotalSessions() * 100;
    }

    /**
     * Calculates conflict rate percentage
     *
     * @param response the assignment response
     * @return conflict rate percentage (0-100)
     */
    public double getConflictRate(AssignResourcesResponse response) {
        if (response == null || response.getTotalSessions() == null || response.getTotalSessions() == 0) {
            return 0.0;
        }
        return (double) response.getConflictCount() / response.getTotalSessions() * 100;
    }

    /**
     * Groups conflicts by conflict type
     *
     * @param response the assignment response
     * @return map of conflict type to count
     */
    public Map<AssignResourcesResponse.ConflictType, Long> getConflictsByType(AssignResourcesResponse response) {
        if (response == null || response.getConflicts() == null) {
            return Map.of();
        }

        return response.getConflicts().stream()
                .collect(Collectors.groupingBy(
                        AssignResourcesResponse.ResourceConflictDetail::getConflictType,
                        Collectors.counting()
                ));
    }

    /**
     * Groups conflicts by day of week
     *
     * @param response the assignment response
     * @return map of day of week to conflict count
     */
    public Map<Short, Long> getConflictsByDay(AssignResourcesResponse response) {
        if (response == null || response.getConflicts() == null) {
            return Map.of();
        }

        return response.getConflicts().stream()
                .collect(Collectors.groupingBy(
                        AssignResourcesResponse.ResourceConflictDetail::getDayOfWeek,
                        Collectors.counting()
                ));
    }

    /**
     * Gets list of sessions with CLASS_BOOKING conflicts
     *
     * @param response the assignment response
     * @return list of session IDs with class booking conflicts
     */
    public List<Long> getClassBookingConflicts(AssignResourcesResponse response) {
        if (response == null || response.getConflicts() == null) {
            return List.of();
        }

        return response.getConflicts().stream()
                .filter(c -> c.getConflictType() == AssignResourcesResponse.ConflictType.CLASS_BOOKING)
                .map(AssignResourcesResponse.ResourceConflictDetail::getSessionId)
                .toList();
    }

    /**
     * Gets formatted assignment summary
     *
     * @param response the assignment response
     * @return summary message
     */
    public String getAssignmentSummary(AssignResourcesResponse response) {
        if (response == null) {
            return "No assignment data available";
        }

        double progress = getAssignmentProgress(response);

        if (isFullySuccessful(response)) {
            return String.format("All %d sessions assigned successfully (100%% - %dms)",
                    response.getTotalSessions(), response.getProcessingTimeMs());
        }

        return String.format(
                "Assignment completed: %d/%d sessions assigned (%.1f%%) - %d conflicts requiring manual resolution (%dms)",
                response.getSuccessCount(), response.getTotalSessions(), progress,
                response.getConflictCount(), response.getProcessingTimeMs()
        );
    }

    /**
     * Gets conflict summary by type
     *
     * @param response the assignment response
     * @return formatted conflict summary
     */
    public String getConflictSummary(AssignResourcesResponse response) {
        if (!hasConflicts(response)) {
            return "No conflicts";
        }

        Map<AssignResourcesResponse.ConflictType, Long> conflictsByType = getConflictsByType(response);

        return conflictsByType.entrySet().stream()
                .map(entry -> String.format("%s: %d", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(", "));
    }

    /**
     * Checks if performance target was met
     * <p>
     * Target: <200ms for 36 sessions
     * </p>
     *
     * @param response the assignment response
     * @return true if performance target met
     */
    public boolean meetsPerformanceTarget(AssignResourcesResponse response) {
        return response != null &&
               response.getProcessingTimeMs() != null &&
               response.getProcessingTimeMs() < 200;
    }

    /**
     * Gets performance status message
     *
     * @param response the assignment response
     * @return performance status
     */
    public String getPerformanceStatus(AssignResourcesResponse response) {
        if (response == null || response.getProcessingTimeMs() == null) {
            return "Performance data unavailable";
        }

        if (meetsPerformanceTarget(response)) {
            return String.format("✅ Performance target met: %dms < 200ms", response.getProcessingTimeMs());
        }

        return String.format("⚠️ Performance target exceeded: %dms > 200ms", response.getProcessingTimeMs());
    }

    /**
     * Checks if the assignment is ready for next step (validation)
     * <p>
     * Ready if: all sessions assigned (no conflicts) OR conflicts acknowledged
     * </p>
     *
     * @param response the assignment response
     * @return true if ready for next step
     */
    public boolean isReadyForNextStep(AssignResourcesResponse response) {
        return response != null &&
               response.getTotalSessions() != null &&
               response.getSuccessCount() != null &&
               response.getSuccessCount() > 0;
    }
}
