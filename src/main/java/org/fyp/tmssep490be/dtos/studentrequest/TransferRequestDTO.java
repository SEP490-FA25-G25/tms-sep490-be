package org.fyp.tmssep490be.dtos.studentrequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequestDTO {

    @NotNull(message = "Current class ID is required")
    private Long currentClassId;

    @NotNull(message = "Target class ID is required")
    private Long targetClassId;

    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;

    @NotNull(message = "Session ID is required")
    private Long sessionId;

    @NotBlank(message = "Reason is required")
    @Size(min = 10, message = "Reason must be at least 10 characters")
    private String requestReason;

    private String note;

    // For AA on-behalf requests only
    private Long studentId;
}
