package org.fyp.tmssep490be.dtos.attendance;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttendanceSummaryDTO {
    private int totalStudents;
    private int presentCount;
    private int absentCount;
}


