package org.fyp.tmssep490be.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for student attendance alert notification
 * Used by WeeklyAttendanceReportJob to identify students with low attendance
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentAttendanceAlertDTO {
    private Long studentId;
    private String studentName;
    private String email;
    private String className;
    private Long presentCount;
    private Long totalCount;
    private Double attendanceRate;
}