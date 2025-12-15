package org.fyp.tmssep490be.dtos.classmanagement;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableStudentDTO {
    private Long id;
    private String studentCode;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;

    private String accountStatus;

    private List<SkillAssessmentDTO> replacementSkillAssessments;

    private ClassMatchInfoDTO classMatchInfo;

    private Integer activeEnrollments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillAssessmentDTO {
        private Long id;
        private String skill;
        private LevelInfoDTO level;
        private String score;
        private LocalDate assessmentDate;
        private String assessmentType;
        private String note;
        private AssessorDTO assessedBy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelInfoDTO {
        private Long id;
        private String code;
        private String name;
        private SubjectInfoDTO subject;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectInfoDTO {
        private Long id;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssessorDTO {
        private Long id;
        private String fullName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassMatchInfoDTO {
        private Integer matchPriority; // 1: Perfect match, 2: Subject match, 3: No match
        private String matchingSkill;
        private LevelInfoDTO matchingLevel;
        private String matchReason;
    }
}
