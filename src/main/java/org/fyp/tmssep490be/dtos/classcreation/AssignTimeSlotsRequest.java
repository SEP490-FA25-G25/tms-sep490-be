package org.fyp.tmssep490be.dtos.classcreation;

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
        private Short dayOfWeek;

        @NotNull(message = "Time slot ID is required")
        private Long timeSlotTemplateId;
    }
}
