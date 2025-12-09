package org.fyp.tmssep490be.dtos.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
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
public class StudentStatusDTO {

    private AttendanceStatus attendanceStatus;

    private HomeworkStatus homeworkStatus;

    private String homeworkDescription;
}
