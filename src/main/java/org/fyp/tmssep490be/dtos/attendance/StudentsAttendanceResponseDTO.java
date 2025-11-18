package org.fyp.tmssep490be.dtos.attendance;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class StudentsAttendanceResponseDTO {
    private Long sessionId;
    private Long classId;
    private String classCode;
    private String courseCode;
    private String courseName;
    private LocalDate date;
    private String timeSlotName;
    private AttendanceSummaryDTO summary;
    private List<StudentAttendanceDTO> students;
}


