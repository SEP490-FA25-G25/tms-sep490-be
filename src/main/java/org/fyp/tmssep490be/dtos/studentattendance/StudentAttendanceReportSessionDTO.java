package org.fyp.tmssep490be.dtos.studentattendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.HomeworkStatus;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAttendanceReportSessionDTO {
    private Long sessionId;
    private Integer sessionNumber;
    private LocalDate date;
    private String status;

    private LocalTime startTime;
    private LocalTime endTime;
    private String classroomName;
    private String teacherName;

    private AttendanceStatus attendanceStatus;
    private HomeworkStatus homeworkStatus;
    private boolean isMakeup;
    private String note;

    private MakeupInfo makeupSessionInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MakeupInfo {
        private Long sessionId;
        private Long classId;
        private String classCode;
        private LocalDate date;
        private Boolean attended;
    }
}
