package org.fyp.tmssep490be.dtos.attendance;

import lombok.Builder;
import lombok.Data;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;

import java.util.List;

@Data
@Builder
public class StudentAttendanceMatrixDTO {
    private Long studentId;
    private String studentCode;
    private String fullName;
    private Double attendanceRate; // Tỷ lệ chuyên cần của học viên
    private List<Cell> cells;

    @Data
    @Builder
    public static class Cell {
        private Long sessionId;
        private AttendanceStatus attendanceStatus;
        private boolean makeup;
    }
}


