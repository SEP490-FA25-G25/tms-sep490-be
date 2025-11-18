package org.fyp.tmssep490be.dtos.studentrequest;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for rejecting a request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectionDTO {

    @NotBlank(message = "Rejection reason is required")
    private String rejectionReason;
}