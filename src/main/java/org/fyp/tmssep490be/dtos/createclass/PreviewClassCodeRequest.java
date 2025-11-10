package org.fyp.tmssep490be.dtos.createclass;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for previewing class code before actual creation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewClassCodeRequest {

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    @NotNull(message = "Course ID is required")
    private Long courseId;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;
}
