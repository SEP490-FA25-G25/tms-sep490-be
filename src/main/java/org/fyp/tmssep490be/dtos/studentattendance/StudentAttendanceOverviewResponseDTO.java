package org.fyp.tmssep490be.dtos.studentattendance;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StudentAttendanceOverviewResponseDTO {
    private List<StudentAttendanceOverviewItemDTO> classes;
}




