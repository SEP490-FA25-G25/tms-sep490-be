package org.fyp.tmssep490be.dtos.attendance;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.HomeworkStatus;

@Data
@Builder
public class AttendanceRecordDTO {

    @NotNull
    private Long studentId;

    @NotNull
    private AttendanceStatus attendanceStatus;

    private HomeworkStatus homeworkStatus;

    private String note;
}


