package org.fyp.tmssep490be.dtos.studentrequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequestDTO {

    @NotNull(message = "Current class ID is required")
    private Long currentClassId;

    @NotNull(message = "Target class ID is required")
    private Long targetClassId;

    /**
     * The first session in target class that student will join.
     * This is the single source of truth for transfer timing.
     */
    @NotNull(message = "Target session ID is required")
    private Long targetSessionId;

    @NotBlank(message = "Reason is required")
    @Size(min = 10, message = "Reason must be at least 10 characters")
    private String requestReason;

    private String note;

    // For AA on-behalf requests only
    private Long studentId;
    
    // For capacity override (AA only)
    private Boolean capacityOverride;
    private String overrideReason;
}
