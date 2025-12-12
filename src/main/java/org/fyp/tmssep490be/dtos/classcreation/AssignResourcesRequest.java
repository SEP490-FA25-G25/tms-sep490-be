package org.fyp.tmssep490be.dtos.classcreation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignResourcesRequest {

    @NotEmpty(message = "Resource assignment pattern is required")
    @Size(min = 1, max = 7, message = "Must provide 1-7 resource assignments")
    @Valid
    private List<ResourceAssignment> pattern;

    // If true, skip conflict checking and force assign (for retry after user
    // confirmation)
    @Builder.Default
    private Boolean skipConflictCheck = false;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceAssignment {

        @NotNull(message = "Day of week is required")
        @Min(value = 0, message = "Day of week must be between 0 (Sunday) and 6 (Saturday)")
        @Max(value = 6, message = "Day of week must be between 0 (Sunday) and 6 (Saturday)")
        private Short dayOfWeek;

        @NotNull(message = "Resource ID is required")
        @Positive(message = "Resource ID must be positive")
        private Long resourceId;
    }
}
