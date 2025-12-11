package org.fyp.tmssep490be.dtos.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.QAReportType;
import org.fyp.tmssep490be.entities.enums.QAReportStatus;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QAReportListItemDTO {
    private Long id;
    private QAReportType reportType;
    private String reportLevel;
    private Long classId;
    private String classCode;
    private Long branchId;
    private String branchName;
    private Long sessionId;
    private String sessionDate;
    private Long phaseId;
    private String phaseName;
    private QAReportStatus status;
    private String reportedByName;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String contentPreview;
}
