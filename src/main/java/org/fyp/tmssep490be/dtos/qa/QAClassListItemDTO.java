package org.fyp.tmssep490be.dtos.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QAClassListItemDTO {
    private Long classId;
    private String classCode;
    private String className;
    private Long subjectId;
    private String subjectName;
    private String branchName;
    private String modality;
    private String status;
    private LocalDate startDate;
    private Integer totalSessions;
    private Integer completedSessions;
    private Double attendanceRate;
    private Double homeworkCompletionRate;
    private Integer qaReportCount;
}
