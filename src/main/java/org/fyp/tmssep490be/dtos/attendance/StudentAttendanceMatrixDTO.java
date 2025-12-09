package org.fyp.tmssep490be.dtos.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.HomeworkStatus;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAttendanceMatrixDTO {
    private Long studentId;
    private String studentCode;
    private String fullName;
    private Double attendanceRate;
    private List<Cell> cells;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Cell {
        private Long sessionId;
        private AttendanceStatus attendanceStatus;
        private HomeworkStatus homeworkStatus;
        private Boolean makeup;
    }
}

