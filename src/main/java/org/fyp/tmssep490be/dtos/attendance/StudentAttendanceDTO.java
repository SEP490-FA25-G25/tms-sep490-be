package org.fyp.tmssep490be.dtos.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.HomeworkStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAttendanceDTO {
    private Long studentId;
    private String studentCode;
    private String fullName;
    private AttendanceStatus attendanceStatus;
    private HomeworkStatus homeworkStatus;
    private Boolean hasPreviousHomework;
    private String note;
    private Boolean makeup;
    private Long makeupSessionId;
}

