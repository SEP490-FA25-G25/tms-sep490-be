package org.fyp.tmssep490be.dtos.attendance;

import lombok.Builder;
import lombok.Data;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.HomeworkStatus;

@Data
@Builder
public class StudentAttendanceDTO {
    private Long studentId;
    private String studentCode;
    private String fullName;
    private AttendanceStatus attendanceStatus;
    private HomeworkStatus homeworkStatus;
    private Boolean hasPreviousHomework; // Indicates if previous session has homework assignment
    private String note;
    private boolean makeup;
    private Long makeupSessionId;
}


