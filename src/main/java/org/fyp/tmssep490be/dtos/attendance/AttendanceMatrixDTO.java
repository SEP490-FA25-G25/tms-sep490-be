package org.fyp.tmssep490be.dtos.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceMatrixDTO {
    private Long classId;
    private String classCode;
    private String className;
    private String subjectName;
    private Double attendanceRate;
    private List<SessionMatrixInfoDTO> sessions;
    private List<StudentAttendanceMatrixDTO> students;
}

