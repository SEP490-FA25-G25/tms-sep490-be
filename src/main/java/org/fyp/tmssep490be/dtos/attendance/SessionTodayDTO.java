package org.fyp.tmssep490be.dtos.attendance;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class SessionTodayDTO {
    private Long sessionId;
    private Long classId;
    private String classCode;
    private String className;
    private String courseCode;
    private String courseName;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private boolean attendanceSubmitted;
    private int totalStudents;
    private int presentCount;
    private int absentCount;
}


