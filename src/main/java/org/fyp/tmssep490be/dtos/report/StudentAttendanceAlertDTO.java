package org.fyp.tmssep490be.dtos.report;

import lombok.AllArgsConstructor;
import lombok.Data;

// DTO dùng cho cảnh báo học viên có tỷ lệ chuyên cần thấp trong tuần
@Data
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


