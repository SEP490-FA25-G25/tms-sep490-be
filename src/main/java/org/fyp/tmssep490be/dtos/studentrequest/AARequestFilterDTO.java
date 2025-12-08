package org.fyp.tmssep490be.dtos.studentrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AARequestFilterDTO {

    @Builder.Default
    private Long branchId = null;
    @Builder.Default
    private String requestType = null; // ABSENCE, MAKEUP, TRANSFER
    @Builder.Default
    private String keyword = null; // Search by student name, student code, or class code
    @Builder.Default
    private String studentName = null; // Search by student name (deprecated, use keyword)
    @Builder.Default
    private String classCode = null; // Search by class code (deprecated, use keyword)
    @Builder.Default
    private String sessionDateFrom = null; // YYYY-MM-DD
    @Builder.Default
    private String sessionDateTo = null; // YYYY-MM-DD
    @Builder.Default
    private String status = null; // PENDING, APPROVED, REJECTED, CANCELLED
    @Builder.Default
    private Long decidedBy = null; // Filter by who decided
    @Builder.Default
    private String submittedDateFrom = null; // YYYY-MM-DD
    @Builder.Default
    private String submittedDateTo = null; // YYYY-MM-DD
    @Builder.Default
    private Integer page = 0;
    @Builder.Default
    private Integer size = 20;
    @Builder.Default
    private String sort = "submittedAt,asc";
}
