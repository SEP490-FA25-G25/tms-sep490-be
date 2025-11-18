package org.fyp.tmssep490be.dtos.studentrequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for submitting makeup request (student self-service or AA on-behalf)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MakeupRequestDTO {

    @NotNull(message = "Current class ID is required")
    private Long currentClassId;

    @NotNull(message = "Target session ID is required (the session student missed)")
    private Long targetSessionId;

    @NotNull(message = "Makeup session ID is required (the session to make up)")
    private Long makeupSessionId;

    @NotBlank(message = "Reason is required")
    @Size(min = 10, message = "Reason must be at least 10 characters")
    private String requestReason;

    private String note;

    // For AA on-behalf requests
    private Long studentId;
}
