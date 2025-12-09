package org.fyp.tmssep490be.dtos.studentrequest;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic DTO for submitting student requests (ABSENCE, MAKEUP, TRANSFER)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentRequestSubmissionDTO {

    @NotBlank(message = "Request type is required")
    @Pattern(regexp = "ABSENCE|MAKEUP|TRANSFER", message = "Request type must be ABSENCE, MAKEUP, or TRANSFER")
    private String requestType;

    @NotNull(message = "Class ID is required")
    private Long currentClassId;

    @NotNull(message = "Target session ID is required")
    private Long targetSessionId;

    private Long makeupSessionId; // For MAKEUP requests only

    @NotBlank(message = "Reason is required")
    @Size(min = 10, message = "Reason must be at least 10 characters")
    private String requestReason;

    private String note;

    // Validation helper: makeupSessionId must be present for MAKEUP requests
    @AssertTrue(message = "Makeup session ID is required for MAKEUP requests")
    public boolean isMakeupSessionValid() {
        if ("MAKEUP".equalsIgnoreCase(requestType)) {
            return makeupSessionId != null;
        }
        return true;
    }
}
