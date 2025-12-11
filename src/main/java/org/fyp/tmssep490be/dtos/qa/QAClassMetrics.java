package org.fyp.tmssep490be.dtos.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QAClassMetrics {
    private Double attendanceRate;
    private Double homeworkCompletionRate;
    private Integer totalSessions;
    private Integer completedSessions;
    private Integer totalStudents;
    private Integer presentStudents;
    private Integer completedHomeworkStudents;
    private Integer totalSessionsWithAttendance;
    private Integer totalSessionsWithHomework;
}
