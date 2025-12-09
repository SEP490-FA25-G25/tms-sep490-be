package org.fyp.tmssep490be.dtos.classmanagement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.Modality;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassListItemDTO {

    private Long id;
    private String code;
    private String name;
    private String subjectName;
    private String subjectCode;
    private String branchName;
    private String branchCode;

    private Modality modality;
    private LocalDate startDate;
    private LocalDate plannedEndDate;
    private ClassStatus status;
    private ApprovalStatus approvalStatus;

    private Integer maxCapacity;
    private Integer currentEnrolled;
    private Integer availableSlots;
    private Double utilizationRate;

    private List<TeacherSummaryDTO> teachers;

    private String scheduleSummary;

    private Integer completedSessions;
    private Integer totalSessions;

    private Boolean canEnrollStudents;
    private String enrollmentRestrictionReason;
}
