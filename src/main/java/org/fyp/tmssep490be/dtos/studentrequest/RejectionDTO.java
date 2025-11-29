package org.fyp.tmssep490be.dtos.studentrequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @Size(min = 10, message = "Rejection reason must be at least 10 characters")
    private String rejectionReason;
}
