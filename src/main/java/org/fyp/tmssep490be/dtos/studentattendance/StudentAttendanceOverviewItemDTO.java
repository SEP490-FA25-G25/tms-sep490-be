package org.fyp.tmssep490be.dtos.studentattendance;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
public class StudentAttendanceOverviewItemDTO {
    private Long classId;
    private String classCode;
    private String className;
    private Long courseId;
    private String courseCode;
    private String courseName;

    private LocalDate startDate;
    private LocalDate actualEndDate;

    private int totalSessions;
    private int attended;
    private int absent;
    private int upcoming;
    private String status;
    private OffsetDateTime lastUpdated;
}




