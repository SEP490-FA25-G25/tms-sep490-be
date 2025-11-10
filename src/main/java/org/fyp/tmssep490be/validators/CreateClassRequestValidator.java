package org.fyp.tmssep490be.validators;

import org.fyp.tmssep490be.dtos.createclass.CreateClassRequest;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.List;

@Component
public class CreateClassRequestValidator {

    /**
     * Validates if all required fields are present and valid
     * Note: code is OPTIONAL - will be auto-generated if not provided
     */
    public boolean isValid(CreateClassRequest request) {
        return request != null &&
               request.getBranchId() != null &&
               request.getCourseId() != null &&
               // code is OPTIONAL - removed from validation
               request.getName() != null && !request.getName().isBlank() &&
               request.getModality() != null &&
               request.getStartDate() != null &&
               request.getScheduleDays() != null && !request.getScheduleDays().isEmpty() &&
               request.getMaxCapacity() != null && request.getMaxCapacity() > 0;
    }

    /**
     * Gets the primary schedule day (first day in the list)
     */
    public Short getPrimaryScheduleDay(CreateClassRequest request) {
        return request.getScheduleDays() != null && !request.getScheduleDays().isEmpty()
               ? request.getScheduleDays().get(0)
               : null;
    }

    /**
     * Checks if schedule includes weekend days (Saturday or Sunday)
     */
    public boolean includesWeekends(CreateClassRequest request) {
        if (request.getScheduleDays() == null) return false;
        // Note: Using PostgreSQL DOW format: 0=Sunday, 1=Monday, ..., 6=Saturday
        return request.getScheduleDays().contains((short) 0) || request.getScheduleDays().contains((short) 6);
    }

    /**
     * Validates if start date matches one of the schedule days
     */
    public boolean isStartDateInScheduleDays(CreateClassRequest request) {
        if (request.getStartDate() == null || request.getScheduleDays() == null || request.getScheduleDays().isEmpty()) {
            return false;
        }

        // LocalDate.getDayOfWeek() returns 1-7 (Monday-Sunday) matching ISODOW
        // Convert to PostgreSQL DOW format: 0=Sunday, 1=Monday, ..., 6=Saturday
        DayOfWeek dayOfWeek = request.getStartDate().getDayOfWeek();
        short postgresDayOfWeek = (short) ((dayOfWeek.getValue() % 7)); // Convert ISODOW to DOW

        return request.getScheduleDays().contains(postgresDayOfWeek);
    }

    /**
     * Checks if there are duplicate day assignments in schedule
     */
    public boolean hasDuplicateDays(CreateClassRequest request) {
        if (request.getScheduleDays() == null || request.getScheduleDays().isEmpty()) {
            return false;
        }

        long uniqueDays = request.getScheduleDays().stream()
                .distinct()
                .count();

        return uniqueDays != request.getScheduleDays().size();
    }

    /**
     * Gets validation error messages
     */
    public List<String> getValidationErrors(CreateClassRequest request) {
        return java.util.stream.Stream.of(
            isValid(request) ? null : "Invalid or missing required fields",
            isStartDateInScheduleDays(request) ? null : "Start date does not match scheduled days",
            hasDuplicateDays(request) ? "Duplicate schedule days found" : null
        ).filter(java.util.Objects::nonNull)
         .toList();
    }
}