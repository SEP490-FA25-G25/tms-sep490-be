package org.fyp.tmssep490be.dtos.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QAClassScoresDTO {

    private Long classId;
    private String classCode;
    private ScoreSummary scoreSummary;
    private List<StudentScoresSummary> students;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreSummary {
        private Double classAverage;
        private Integer totalStudents;
        private Integer studentsWithScores;
        private Integer totalAssessments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentScoresSummary {
        private Long studentId;
        private String studentCode;
        private String studentName;
        private String email;
        private String phone;
        private String address;
        private String avatarUrl;
        private OffsetDateTime enrolledAt;
        private Double averageScore;
        private Integer totalAssessments;
        private Integer completedAssessments;
        private List<AssessmentScore> scores;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssessmentScore {
        private Long assessmentId;
        private String assessmentName;
        private String assessmentKind;
        private String skill;
        private Double maxScore;
        private Integer durationMinutes;
        private OffsetDateTime scheduledDate;
        private OffsetDateTime actualDate;
        private Long scoreId;
        private Double score;
        private Double scorePercentage;
        private String feedback;
        private String gradedByName;
        private OffsetDateTime gradedAt;
    }
}
