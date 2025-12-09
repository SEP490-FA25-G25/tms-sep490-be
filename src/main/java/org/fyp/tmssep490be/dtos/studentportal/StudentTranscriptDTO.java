package org.fyp.tmssep490be.dtos.studentportal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentTranscriptDTO {
    private Long classId;
    private String classCode;
    private String className;
    private String subjectName;
    private String teacherName;
    private String status;
    private BigDecimal averageScore;
    private Map<String, BigDecimal> componentScores;
    private LocalDate completedDate;
    private Integer totalSessions;
    private Integer completedSessions;
}
