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
public class AttendanceRecordDTO {
    private Long studentId;
    private AttendanceStatus attendanceStatus;
    private HomeworkStatus homeworkStatus;
    private String note;
}

