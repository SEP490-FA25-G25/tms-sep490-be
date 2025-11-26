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
public class QAReportDetailDTO {
    private Long id;
    private String reportType;
    private String status;
    private Long classId;
    private String classCode;
    private String className;
    private Long sessionId;
    private String sessionDate;
    private Long phaseId;
    private String phaseName;
    private String findings;
    private String actionItems;
    private Long reportedById;
    private String reportedByName;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
