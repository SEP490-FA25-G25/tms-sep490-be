package org.fyp.tmssep490be.dtos.createclass;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

/**
 * Request DTO for assigning resources to class sessions (STEP 4)
 * <p>
 * Pattern-based assignment: Assigns same resource to all sessions on specific day of week
 * </p>
 * <p>
 * Example:
 * <pre>
 * {
 *   "pattern": [
 *     {"dayOfWeek": 1, "resourceId": 6},  // Monday -> Room 203
 *     {"dayOfWeek": 3, "resourceId": 6},  // Wednesday -> Room 203
 *     {"dayOfWeek": 5, "resourceId": 7}   // Friday -> Room 205
 *   ]
 * }
 * </pre>
 * </p>
 *
 * @see AssignResourcesResponse
 * @see org.fyp.tmssep490be.validators.AssignResourcesRequestValidator
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignResourcesRequest {

    @NotEmpty(message = "Resource assignment pattern is required")
    @Size(min = 1, max = 7, message = "Must provide 1-7 resource assignments")
    @Valid
    private List<ResourceAssignment> pattern;

    /**
     * Resource assignment for a specific day of week
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceAssignment {

        /**
         * Day of week using PostgreSQL DOW format
         * <p>
         * 0 = Sunday, 1 = Monday, 2 = Tuesday, 3 = Wednesday,
         * 4 = Thursday, 5 = Friday, 6 = Saturday
         * </p>
         */
        @NotNull(message = "Day of week is required")
        @Min(value = 0, message = "Day of week must be between 0 (Sunday) and 6 (Saturday)")
        @Max(value = 6, message = "Day of week must be between 0 (Sunday) and 6 (Saturday)")
        private Short dayOfWeek;

        /**
         * Resource ID (room or online account)
         */
        @NotNull(message = "Resource ID is required")
        @Positive(message = "Resource ID must be positive")
        private Long resourceId;
    }
}
