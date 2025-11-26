package org.fyp.tmssep490be.dtos.qa;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeQAReportStatusRequest {
    @NotBlank(message = "Status is required")
    private String status;
}
