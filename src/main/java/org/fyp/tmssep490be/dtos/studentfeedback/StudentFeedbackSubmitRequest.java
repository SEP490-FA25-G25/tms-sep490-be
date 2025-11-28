package org.fyp.tmssep490be.dtos.studentfeedback;

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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentFeedbackSubmitRequest {

    @NotEmpty
    private List<ResponseItem> responses;

    private String comment;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseItem {
        @NotNull
        private Long questionId;

        @NotNull
        @Min(1)
        @Max(5)
        private Short rating;
    }
}
