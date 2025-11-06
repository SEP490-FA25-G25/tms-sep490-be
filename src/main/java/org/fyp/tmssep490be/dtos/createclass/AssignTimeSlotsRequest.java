package org.fyp.tmssep490be.dtos.createclass;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignTimeSlotsRequest {

    @NotEmpty(message = "Time slot assignments are required")
    @Size(min = 1, max = 7, message = "Must provide 1-7 time slot assignments")
    @Valid
    private List<TimeSlotAssignment> assignments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSlotAssignment {

        @NotNull(message = "Day of week is required")
        @Min(value = 0, message = "Day of week must be between 0 (Sunday) and 6 (Saturday)")
        @Max(value = 6, message = "Day of week must be between 0 (Sunday) and 6 (Saturday)")
        private Short dayOfWeek; // 0=Sunday, 1=Monday, ..., 6=Saturday (PostgreSQL DOW format)

        @NotNull(message = "Time slot ID is required")
        private Long timeSlotTemplateId;
    }

    // Validation methods
    public boolean isValid() {
        return assignments != null && !assignments.isEmpty() &&
               assignments.stream().allMatch(assignment ->
                   assignment != null &&
                   assignment.getDayOfWeek() != null &&
                   assignment.getTimeSlotTemplateId() != null &&
                   assignment.getDayOfWeek() >= 1 &&
                   assignment.getDayOfWeek() <= 7);
    }

    /**
     * Check if any duplicate day assignments exist
     */
    public boolean hasDuplicateDays() {
        if (assignments == null || assignments.isEmpty()) return false;
        long uniqueDays = assignments.stream()
                .map(TimeSlotAssignment::getDayOfWeek)
                .distinct()
                .count();
        return uniqueDays != assignments.size();
    }

    /**
     * Get day of week that has duplicates
     */
    public Short getDuplicateDay() {
        if (assignments == null || assignments.size() < 2) return null;
        return assignments.stream()
                .map(TimeSlotAssignment::getDayOfWeek)
                .filter(day -> assignments.stream()
                        .map(TimeSlotAssignment::getDayOfWeek)
                        .filter(d -> d.equals(day))
                        .count() > 1)
                .findFirst()
                .orElse(null);
    }
}