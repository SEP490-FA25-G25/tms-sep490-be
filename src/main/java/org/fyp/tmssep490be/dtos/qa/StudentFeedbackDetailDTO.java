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
public class StudentFeedbackDetailDTO {
    private Long feedbackId;
    private Long studentId;
    private String studentName;
    private Long classId;
    private String classCode;
    private Long phaseId;
    private String phaseName;
    private Boolean isFeedback;
    private OffsetDateTime submittedAt;
    private String response;
    private Double rating;
    private String sentiment;
    private List<FeedbackResponseItem> detailedResponses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedbackResponseItem {
        private Long questionId;
        private String questionText;
        private String answerText;
    }
}
