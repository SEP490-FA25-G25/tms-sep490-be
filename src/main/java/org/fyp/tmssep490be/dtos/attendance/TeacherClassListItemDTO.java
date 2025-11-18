package org.fyp.tmssep490be.dtos.attendance;

import lombok.*;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.Modality;

import java.time.LocalDate;

/**
 * Class information for teacher's class list view
 * Used in GET /api/v1/attendance/classes endpoint
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherClassListItemDTO {

    private Long id;
    private String code;
    private String name;
    private String courseName;
    private String courseCode;
    private String branchName;
    private String branchCode;

    private Modality modality;
    private LocalDate startDate;
    private LocalDate plannedEndDate;
    private ClassStatus status;
    private Integer totalSessions;
    private Double attendanceRate;
}





