package org.fyp.tmssep490be.dtos.studentfeedback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentFeedbackSubmitResponse {
    private Long feedbackId;
    private Boolean isFeedback;
    private OffsetDateTime submittedAt;
    private Double averageRating;
}
