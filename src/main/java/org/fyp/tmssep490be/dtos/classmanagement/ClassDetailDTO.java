package org.fyp.tmssep490be.dtos.classmanagement;

import lombok.*;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.Modality;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassDetailDTO {

    private Long id;
    private String code;
    private String name;

    private SubjectDTO subject;
    private BranchDTO branch;

    private Modality modality;
    private LocalDate startDate;
    private LocalDate plannedEndDate;
    private LocalDate actualEndDate;
    private Short[] scheduleDays;
    private Integer maxCapacity;

    private ClassStatus status;
    private ApprovalStatus approvalStatus;
    private String rejectionReason;

    private LocalDate submittedAt;
    private LocalDate decidedAt;
    private String decidedByName;

    private String createdByName;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    private List<TeacherSummaryDTO> teachers;

    private String scheduleSummary;

    private EnrollmentSummary enrollmentSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurriculumDTO {
        private Long id;
        private String code;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelDTO {
        private Long id;
        private String code;
        private String name;
        private CurriculumDTO curriculum;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectDTO {
        private Long id;
        private String code;
        private String name;
        private String description;
        private Integer totalHours;
        private Integer numberOfSessions;
        private BigDecimal hoursPerSession;
        private String prerequisites;
        private String targetAudience;
        private String teachingMethods;
        // Related level
        private LevelDTO level;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchDTO {
        private Long id;
        private String code;
        private String name;
        private String address;
        private String phone;
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnrollmentSummary {
        private Integer currentEnrolled;
        private Integer maxCapacity;
        private Integer availableSlots;
        private Double utilizationRate;
        private Boolean canEnrollStudents;
        private String enrollmentRestrictionReason;
    }
}
