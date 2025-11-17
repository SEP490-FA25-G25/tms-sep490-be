package org.fyp.tmssep490be.dtos.createclass;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for assigning a specific resource to a single session
 * as part of the Quick Fix flow in Step 4.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignSessionResourceRequest {

    /**
     * ID của tài nguyên muốn gán cho session
     */
    @NotNull(message = "Resource ID is required")
    @Positive(message = "Resource ID must be positive")
    private Long resourceId;
}
