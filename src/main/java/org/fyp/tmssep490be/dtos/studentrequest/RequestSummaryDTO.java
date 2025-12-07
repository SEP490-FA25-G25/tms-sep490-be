package org.fyp.tmssep490be.dtos.studentrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestSummaryDTO {

    private Integer totalPending;
    private Integer absenceRequests;
    private Integer makeupRequests;
    private Integer transferRequests;
}
