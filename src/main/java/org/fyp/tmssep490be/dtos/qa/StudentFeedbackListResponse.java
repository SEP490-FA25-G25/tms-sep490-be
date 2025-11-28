package org.fyp.tmssep490be.dtos.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentFeedbackListResponse {
    private FeedbackStatistics statistics;
    private Page<StudentFeedbackItemDTO> feedbacks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedbackStatistics {
        private Integer totalStudents;
        private Integer submittedCount;
        private Integer notSubmittedCount;
        private Double submissionRate;
        private Double averageRating;
        private Integer positiveFeedbackCount;
        private Integer negativeFeedbackCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentFeedbackItemDTO {
        private Long feedbackId;
        private Long studentId;
        private String studentName;
        private Long phaseId;
        private String phaseName;
        private Boolean isFeedback;
        private OffsetDateTime submittedAt;
        private String responsePreview;
        private Double rating;
        private String sentiment;
    }
}
