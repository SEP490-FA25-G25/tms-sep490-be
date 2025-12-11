package org.fyp.tmssep490be.dtos.teacherrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherRequestConfigDTO {

    private boolean requireResourceAtRescheduleCreate;

    private boolean requireResourceAtModalityChangeCreate;

    private int reasonMinLength;

    private int timeWindowDays;
}
