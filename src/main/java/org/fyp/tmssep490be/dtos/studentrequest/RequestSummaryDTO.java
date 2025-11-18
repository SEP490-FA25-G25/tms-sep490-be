package org.fyp.tmssep490be.dtos.studentrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for request summary statistics (for AA dashboard)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestSummaryDTO {

    private Integer totalPending;
    private Integer needsUrgentReview; // Sessions happening in next 2 days
    private Integer absenceRequests;
    private Integer makeupRequests;
    private Integer transferRequests;
}