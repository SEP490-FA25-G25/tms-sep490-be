package org.fyp.tmssep490be.dtos.createclass;

import jakarta.validation.constraints.*;
import lombok.*;
import org.fyp.tmssep490be.entities.enums.Modality;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateClassRequest {

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    @NotNull(message = "Course ID is required")
    private Long courseId;

    @NotBlank(message = "Class code is required")
    @Size(max = 50, message = "Class code must not exceed 50 characters")
    @Pattern(regexp = "^[A-Z0-9\\-]+$", message = "Class code must contain only uppercase letters, numbers, and hyphens")
    private String code;

    @NotBlank(message = "Class name is required")
    @Size(max = 255, message = "Class name must not exceed 255 characters")
    private String name;

    @NotNull(message = "Modality is required")
    private Modality modality;

    @NotNull(message = "Start date is required")
    @Future(message = "Start date must be in the future")
    private LocalDate startDate;

    @NotEmpty(message = "Schedule days are required")
    @Size(min = 1, max = 7, message = "Schedule days must contain 1-7 days")
    private List<@NotNull Short> scheduleDays;

    @NotNull(message = "Max capacity is required")
    @Min(value = 1, message = "Max capacity must be at least 1")
    @Max(value = 1000, message = "Max capacity must not exceed 1000")
    private Integer maxCapacity;

    // Additional validation methods
    public boolean isValid() {
        return branchId != null && courseId != null && code != null && !code.isBlank() &&
               name != null && !name.isBlank() && modality != null && startDate != null &&
               scheduleDays != null && !scheduleDays.isEmpty() && maxCapacity != null && maxCapacity > 0;
    }

    public Short getPrimaryScheduleDay() {
        return scheduleDays != null && !scheduleDays.isEmpty() ? scheduleDays.get(0) : null;
    }

    public boolean includesWeekends() {
        if (scheduleDays == null) return false;
        return scheduleDays.contains((short) 6) || scheduleDays.contains((short) 7); // Saturday or Sunday
    }

    /**
     * Validates if start date matches one of the schedule days
     * @return true if start date day of week is in scheduleDays
     */
    public boolean isStartDateInScheduleDays() {
        if (startDate == null || scheduleDays == null || scheduleDays.isEmpty()) {
            return false;
        }
        // LocalDate.getDayOfWeek() returns 1-7 (Monday-Sunday) matching ISODOW
        int startDayOfWeek = startDate.getDayOfWeek().getValue();
        return scheduleDays.contains((short) startDayOfWeek);
    }

    /**
     * Check if any duplicate day assignments exist
     */
    public boolean hasDuplicateDays() {
        if (scheduleDays == null || scheduleDays.isEmpty()) return false;
        long uniqueDays = scheduleDays.stream()
                .distinct()
                .count();
        return uniqueDays != scheduleDays.size();
    }
}