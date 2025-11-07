package org.fyp.tmssep490be.validators;

import org.fyp.tmssep490be.dtos.createclass.AssignTimeSlotsRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AssignTimeSlotsRequestValidator {

    /**
     * Validates if the time slots request is properly formatted
     */
    public boolean isValid(AssignTimeSlotsRequest request) {
        return request != null &&
               request.getAssignments() != null &&
               !request.getAssignments().isEmpty() &&
               request.getAssignments().stream().allMatch(this::isValidAssignment);
    }

    /**
     * Validates individual time slot assignment
     */
    public boolean isValidAssignment(AssignTimeSlotsRequest.TimeSlotAssignment assignment) {
        return assignment != null &&
               assignment.getDayOfWeek() != null &&
               assignment.getTimeSlotTemplateId() != null &&
               assignment.getDayOfWeek() >= 0 &&
               assignment.getDayOfWeek() <= 6; // PostgreSQL DOW format: 0=Sunday, 1=Monday, ..., 6=Saturday
    }

    /**
     * Checks if there are duplicate day assignments
     */
    public boolean hasDuplicateDays(AssignTimeSlotsRequest request) {
        if (request.getAssignments() == null || request.getAssignments().isEmpty()) {
            return false;
        }

        long uniqueDays = request.getAssignments().stream()
                .map(AssignTimeSlotsRequest.TimeSlotAssignment::getDayOfWeek)
                .distinct()
                .count();

        return uniqueDays != request.getAssignments().size();
    }

    /**
     * Gets the day of week that has duplicates
     */
    public Short getDuplicateDay(AssignTimeSlotsRequest request) {
        if (request.getAssignments() == null || request.getAssignments().size() < 2) {
            return null;
        }

        return request.getAssignments().stream()
                .map(AssignTimeSlotsRequest.TimeSlotAssignment::getDayOfWeek)
                .filter(day -> request.getAssignments().stream()
                        .map(AssignTimeSlotsRequest.TimeSlotAssignment::getDayOfWeek)
                        .filter(d -> d.equals(day))
                        .count() > 1)
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets validation error messages
     */
    public List<String> getValidationErrors(AssignTimeSlotsRequest request) {
        return java.util.stream.Stream.of(
            isValid(request) ? null : "Invalid time slot assignments",
            hasDuplicateDays(request) ? "Duplicate day assignments found: Day " + getDuplicateDay(request) : null
        ).filter(java.util.Objects::nonNull)
         .toList();
    }

    /**
     * Counts valid assignments
     */
    public int getValidAssignmentCount(AssignTimeSlotsRequest request) {
        if (request.getAssignments() == null) return 0;
        return (int) request.getAssignments().stream()
                .filter(this::isValidAssignment)
                .count();
    }

    /**
     * Counts invalid assignments
     */
    public int getInvalidAssignmentCount(AssignTimeSlotsRequest request) {
        if (request.getAssignments() == null) return 0;
        return (int) request.getAssignments().stream()
                .filter(assignment -> !isValidAssignment(assignment))
                .count();
    }
}