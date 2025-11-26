package org.fyp.tmssep490be.dtos.qa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.QAReportType;
import org.fyp.tmssep490be.entities.enums.QAReportStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateQAReportRequest {
    @NotNull
    private QAReportType reportType;

    @NotBlank
    @Size(min = 50)
    private String findings;

    private String actionItems;

    @NotNull
    private QAReportStatus status;
}
