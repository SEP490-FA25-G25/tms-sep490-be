package org.fyp.tmssep490be.dtos.studentfeedback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentFeedbackListItemDTO {
    private Long feedbackId;
    private Long classId;
    private String classCode;
    private String className;
    private String subjectName;
    private Long phaseId;
    private String phaseName;
    private Boolean isFeedback;        // false = pending, true = submitted
    private OffsetDateTime submittedAt;
    private Double averageRating;      // Average rating 1-5
    private String comment;
    private List<FeedbackResponseItem> responses;  // Detailed responses (only when submitted)
    private OffsetDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FeedbackResponseItem {
        private Long questionId;
        private String questionText;
        private Integer rating;
        private Integer displayOrder;
    }
}
