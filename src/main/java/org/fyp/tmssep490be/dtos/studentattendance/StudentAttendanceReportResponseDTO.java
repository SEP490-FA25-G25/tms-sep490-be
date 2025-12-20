package org.fyp.tmssep490be.dtos.studentattendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAttendanceReportResponseDTO {
    private Long classId;
    private String classCode;
    private String className;
    private Long courseId;
    private String courseCode;
    private String courseName;
    private LocalDate startDate;
    private LocalDate actualEndDate;
    private String status;
    private String enrollmentStatus;  // ENROLLED, TRANSFERRED, COMPLETED

    private Summary summary;
    private List<StudentAttendanceReportSessionDTO> sessions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private int totalSessions;
        private int attended;
        private int absent;
        private int excused;
        private int excusedCompleted;  // Số buổi EXCUSED đã học bù thành công
        private int upcoming;
        private double attendanceRate;
    }
}
