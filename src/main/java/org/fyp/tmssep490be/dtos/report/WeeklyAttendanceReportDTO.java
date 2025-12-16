package org.fyp.tmssep490be.dtos.report;

import lombok.AllArgsConstructor;
import lombok.Data;

// DTO dùng cho báo cáo chuyên cần theo lớp trong tuần
@Data
@AllArgsConstructor
public class WeeklyAttendanceReportDTO {

    private Long classId;
    private String className;
    private Long totalSessions;
    private Long presentSessions;
    private Long absentSessions;
    private Double attendanceRate;
}


