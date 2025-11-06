package org.fyp.tmssep490be.dtos.createclass;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectClassRequest {

    @NotBlank(message = "Rejection reason is required")
    @Size(min = 10, max = 500, message = "Rejection reason must be between 10 and 500 characters")
    private String reason;

    // Helper methods
    public boolean isValid() {
        return reason != null && !reason.isBlank() &&
               reason.length() >= 10 && reason.length() <= 500;
    }

    public String getFormattedReason() {
        return reason != null ? reason.trim() : "";
    }
}