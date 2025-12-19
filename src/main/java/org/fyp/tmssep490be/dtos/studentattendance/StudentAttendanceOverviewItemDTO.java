package org.fyp.tmssep490be.dtos.studentattendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAttendanceOverviewItemDTO {
    private Long classId;
    private String classCode;
    private String className;
    private Long courseId;
    private String courseCode;
    private String courseName;

    private LocalDate startDate;
    private LocalDate expectedEndDate;
    private LocalDate actualEndDate;

    private int totalSessions;
    private int attended;
    private int absent;
    private int excused;
    private int upcoming;
    private String status;
    private String enrollmentStatus;  // ENROLLED, TRANSFERRED, COMPLETED
    private OffsetDateTime lastUpdated;
}
