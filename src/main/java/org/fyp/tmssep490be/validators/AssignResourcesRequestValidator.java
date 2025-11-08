package org.fyp.tmssep490be.validators;

import org.fyp.tmssep490be.dtos.createclass.AssignResourcesRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Validator for AssignResourcesRequest
 * <p>
 * Validates:
 * <ul>
 *   <li>Request structure and required fields</li>
 *   <li>Day of week format (PostgreSQL DOW: 0-6)</li>
 *   <li>Resource ID validity</li>
 *   <li>Duplicate day assignments</li>
 * </ul>
 * </p>
 *
 * @see AssignResourcesRequest
 */
@Component
public class AssignResourcesRequestValidator {

    /**
     * Validates if the resource assignment request is properly formatted
     *
     * @param request the resource assignment request
     * @return true if valid, false otherwise
     */
    public boolean isValid(AssignResourcesRequest request) {
        return request != null &&
               request.getPattern() != null &&
               !request.getPattern().isEmpty() &&
               request.getPattern().stream().allMatch(this::isValidAssignment);
    }

    /**
     * Validates individual resource assignment
     *
     * @param assignment the resource assignment
     * @return true if valid, false otherwise
     */
    public boolean isValidAssignment(AssignResourcesRequest.ResourceAssignment assignment) {
        return assignment != null &&
               assignment.getDayOfWeek() != null &&
               assignment.getResourceId() != null &&
               assignment.getDayOfWeek() >= 0 &&
               assignment.getDayOfWeek() <= 6 && // PostgreSQL DOW format: 0=Sunday, 1=Monday, ..., 6=Saturday
               assignment.getResourceId() > 0; // Resource ID must be positive
    }

    /**
     * Checks if there are duplicate day assignments
     * <p>
     * Each day can only be assigned one resource
     * </p>
     *
     * @param request the resource assignment request
     * @return true if duplicates found, false otherwise
     */
    public boolean hasDuplicateDays(AssignResourcesRequest request) {
        if (request.getPattern() == null || request.getPattern().isEmpty()) {
            return false;
        }

        long uniqueDays = request.getPattern().stream()
                .map(AssignResourcesRequest.ResourceAssignment::getDayOfWeek)
                .distinct()
                .count();

        return uniqueDays != request.getPattern().size();
    }

    /**
     * Gets the day of week that has duplicates
     *
     * @param request the resource assignment request
     * @return the duplicate day of week, or null if none
     */
    public Short getDuplicateDay(AssignResourcesRequest request) {
        if (request.getPattern() == null || request.getPattern().size() < 2) {
            return null;
        }

        return request.getPattern().stream()
                .map(AssignResourcesRequest.ResourceAssignment::getDayOfWeek)
                .filter(day -> request.getPattern().stream()
                        .map(AssignResourcesRequest.ResourceAssignment::getDayOfWeek)
                        .filter(d -> d.equals(day))
                        .count() > 1)
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets validation error messages
     *
     * @param request the resource assignment request
     * @return list of validation error messages
     */
    public List<String> getValidationErrors(AssignResourcesRequest request) {
        return java.util.stream.Stream.of(
            isValid(request) ? null : "Invalid resource assignments",
            hasDuplicateDays(request) ? "Duplicate day assignments found: Day " + getDuplicateDay(request) : null
        ).filter(java.util.Objects::nonNull)
         .toList();
    }

    /**
     * Counts valid assignments
     *
     * @param request the resource assignment request
     * @return number of valid assignments
     */
    public int getValidAssignmentCount(AssignResourcesRequest request) {
        if (request.getPattern() == null) return 0;
        return (int) request.getPattern().stream()
                .filter(this::isValidAssignment)
                .count();
    }

    /**
     * Counts invalid assignments
     *
     * @param request the resource assignment request
     * @return number of invalid assignments
     */
    public int getInvalidAssignmentCount(AssignResourcesRequest request) {
        if (request.getPattern() == null) return 0;
        return (int) request.getPattern().stream()
                .filter(assignment -> !isValidAssignment(assignment))
                .count();
    }
}
