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
    private String courseName;
    private String courseCode;
    private String branchName;
    private String branchCode;

    private Modality modality;
    private LocalDate startDate;
    private LocalDate plannedEndDate;
    private ClassStatus status;
    private ApprovalStatus approvalStatus;

    // Capacity information
    private Integer maxCapacity;
    private Integer currentEnrolled;
    private Integer availableSlots;
    private Double utilizationRate;

    // Teacher information - all teachers teaching this class
    private List<TeacherSummaryDTO> teachers;

    // Schedule summary
    private String scheduleSummary;

    // Session progress
    private Integer completedSessions;
    private Integer totalSessions;

    // Quick checks for UI
    private Boolean canEnrollStudents;
    private String enrollmentRestrictionReason;
}
