package org.fyp.tmssep490be.dtos.studentrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for filtering student requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestFilterDTO {

    @Builder.Default
    private String requestType = null; // ABSENCE, MAKEUP, TRANSFER
    @Builder.Default
    private String status = null; // PENDING, APPROVED, REJECTED, CANCELLED
    @Builder.Default
    private Integer page = 0;
    @Builder.Default
    private Integer size = 10;
    @Builder.Default
    private String sort = "submittedAt,desc";
}