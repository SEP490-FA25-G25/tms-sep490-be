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
public class CreateQAReportRequest {
    @NotNull(message = "Class ID is required")
    private Long classId;

    private Long sessionId;
    private Long phaseId;

    @NotNull(message = "Report type is required")
    private QAReportType reportType;

    @NotBlank(message = "Nội dung báo cáo không được để trống")
    @Size(min = 50, message = "Nội dung báo cáo phải có ít nhất 50 ký tự")
    private String content;

    @NotNull(message = "Status is required")
    private QAReportStatus status;
}
