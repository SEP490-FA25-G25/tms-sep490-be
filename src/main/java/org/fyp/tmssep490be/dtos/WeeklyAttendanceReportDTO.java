package org.fyp.tmssep490be.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for weekly attendance report by class
 * Used by WeeklyAttendanceReportJob to generate attendance summaries
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyAttendanceReportDTO {
    private Long classId;
    private String className;
    private Long totalSessions;
    private Long presentSessions;
    private Long absentSessions;
    private Double attendanceRate;
}