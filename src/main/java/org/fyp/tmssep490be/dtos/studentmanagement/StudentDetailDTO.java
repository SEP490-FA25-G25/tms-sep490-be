package org.fyp.tmssep490be.dtos.studentmanagement;

import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDetailDTO {
    private Long id;
    private String studentCode;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String facebookUrl;
    private String avatarUrl;
    private String gender;
    private LocalDate dateOfBirth;
    private String status;
    private OffsetDateTime lastLoginAt;
    private OffsetDateTime createdAt;
    private String branchName;
    private Long branchId;

    // Current Active Classes
    private List<StudentActiveClassDTO> currentClasses;

    // Skill Assessments
    private List<SkillAssessmentDetailDTO> skillAssessments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillAssessmentDetailDTO {
        private Long id;
        private String skill;
        private String levelCode;
        private String levelName;
        private String score;
        private LocalDate assessmentDate;
        private String assessmentType;
        private String note;
        private AssessedByDTO assessedBy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssessedByDTO {
        private Long userId;
        private String fullName;
    }
}
