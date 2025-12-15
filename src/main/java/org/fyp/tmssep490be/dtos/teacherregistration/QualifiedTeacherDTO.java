package org.fyp.tmssep490be.dtos.teacherregistration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for qualified teacher candidates for direct assignment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualifiedTeacherDTO {
    private Long teacherId;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String employeeCode;

    // Skills info
    private List<TeacherSkillInfo> skills;
    private Integer totalSkills;

    // Match info
    private Boolean isMatch;
    private String matchReason;
    private Integer matchScore; // Higher = better match

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherSkillInfo {
        private String skill; // READING, WRITING, etc.
        private String specialization; // IELTS, TOEIC, etc.
        private String language; // "English", "Japanese"
        private Short level;
    }
}
