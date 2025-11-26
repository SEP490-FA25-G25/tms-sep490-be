package org.fyp.tmssep490be.dtos.qa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateQAReportRequest {
    @NotBlank
    private String reportType;

    @NotBlank
    @Size(min = 50)
    private String findings;

    private String actionItems;

    @NotBlank
    private String status;
}
