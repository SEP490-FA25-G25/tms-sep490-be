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
public class QAReportDetailDTO {
    private Long id;
    private QAReportType reportType;
    private QAReportStatus status;
    private Long classId;
    private String classCode;    // Maps to ClassEntity.code (Mã lớp học)
    private String className;    // Maps to ClassEntity.name (Tên lớp học)
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
    private QAClassMetrics classMetrics;
}
