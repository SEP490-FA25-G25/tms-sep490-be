package org.fyp.tmssep490be.dtos.qa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateQAReportRequest {
    @NotNull(message = "Class ID is required")
    private Long classId;

    private Long sessionId;
    private Long phaseId;

    @NotBlank(message = "Report type is required")
    @Size(max = 100)
    private String reportType;

    @NotBlank(message = "Findings are required")
    @Size(min = 50, message = "Findings must be at least 50 characters")
    private String findings;

    private String actionItems;

    @NotBlank(message = "Status is required")
    private String status;
}
