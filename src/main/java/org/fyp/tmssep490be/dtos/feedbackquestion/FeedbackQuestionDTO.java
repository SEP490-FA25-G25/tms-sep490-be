package org.fyp.tmssep490be.dtos.feedbackquestion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackQuestionDTO {
    private Long id;
    private String questionText;
    private Integer displayOrder;
    private String status;
    private Integer usageCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
