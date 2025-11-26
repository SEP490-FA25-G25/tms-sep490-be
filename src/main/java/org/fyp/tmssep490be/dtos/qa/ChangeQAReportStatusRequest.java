package org.fyp.tmssep490be.dtos.qa;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.QAReportStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeQAReportStatusRequest {
    @NotNull(message = "Status is required")
    private QAReportStatus status;
}
