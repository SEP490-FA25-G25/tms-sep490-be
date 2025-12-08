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
public class AbsenceRequestDTO {

    private Long studentId;

    @NotNull(message = "Class ID is required")
    private Long currentClassId;

    @NotNull(message = "Session ID is required")
    private Long targetSessionId;

    @NotBlank(message = "Reason is required")
    @Size(min = 10, message = "Reason must be at least 10 characters")
    private String requestReason;

    private String note;
}
