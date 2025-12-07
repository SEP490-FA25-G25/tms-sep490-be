package org.fyp.tmssep490be.dtos.enrollment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentRecommendation {
    private RecommendationType type;
    private String message;
    private Integer suggestedEnrollCount;  // Nếu type = PARTIAL_SUGGESTED
}
