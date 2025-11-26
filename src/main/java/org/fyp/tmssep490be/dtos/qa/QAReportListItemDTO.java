package org.fyp.tmssep490be.dtos.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QAReportListItemDTO {
    private Long id;
    private String reportType;
    private String reportLevel; // "Class", "Session", "Phase"
    private Long classId;
    private String classCode;
    private Long sessionId;
    private String sessionDate;
    private Long phaseId;
    private String phaseName;
    private String status;
    private String reportedByName;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String findingsPreview;
}
