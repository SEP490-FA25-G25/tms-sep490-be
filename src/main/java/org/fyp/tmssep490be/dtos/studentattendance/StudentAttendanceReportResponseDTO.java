package org.fyp.tmssep490be.dtos.studentattendance;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StudentAttendanceReportResponseDTO {
    private Long classId;
    private String classCode;
    private String className;
    private Long courseId;
    private String courseCode;
    private String courseName;

    @Data
    @Builder
    public static class Summary {
        private int totalSessions;
        private int attended;
        private int absent;
        private int upcoming;
        private double attendanceRate;
    }

    private Summary summary;
    private List<StudentAttendanceReportSessionDTO> sessions;
}




