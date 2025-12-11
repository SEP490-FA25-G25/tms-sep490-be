package org.fyp.tmssep490be.dtos.studentfeedback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentFeedbackQuestionDTO {
    private Long id;
    private String questionText;
    private String questionType;  // 'rating' or 'text'
    private String[] options;
    private Integer displayOrder;
}
