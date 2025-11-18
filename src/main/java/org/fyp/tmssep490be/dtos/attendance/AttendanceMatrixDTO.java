package org.fyp.tmssep490be.dtos.attendance;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AttendanceMatrixDTO {
    private Long classId;
    private String classCode;
    private String courseName;
    private Double attendanceRate; // Tỷ lệ chuyên cần của cả lớp
    private List<SessionMatrixInfoDTO> sessions;
    private List<StudentAttendanceMatrixDTO> students;
}


