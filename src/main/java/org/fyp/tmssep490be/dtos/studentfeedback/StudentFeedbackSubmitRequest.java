package org.fyp.tmssep490be.dtos.studentfeedback;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentFeedbackSubmitRequest {
    
    @NotEmpty(message = "Vui lòng trả lời ít nhất một câu hỏi")
    @Valid
    private List<ResponseItem> responses;
    
    private String comment;  // Optional comment

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResponseItem {
        @NotNull(message = "Question ID không được để trống")
        private Long questionId;
        
        @NotNull(message = "Rating không được để trống")
        @Min(value = 1, message = "Rating tối thiểu là 1")
        @Max(value = 5, message = "Rating tối đa là 5")
        private Short rating;
    }
}
