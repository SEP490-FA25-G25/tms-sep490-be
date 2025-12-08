package org.fyp.tmssep490be.dtos.studentrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.entities.enums.StudentRequestType;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestFilterDTO {

    // Support both single string (for backward compatibility) and list of enums
    @Builder.Default
    private String requestType = null; // ABSENCE, MAKEUP, TRANSFER (backward compatibility)
    @Builder.Default
    private List<StudentRequestType> requestTypeFilters = null; // Multiple request types

    @Builder.Default
    private String status = null; // PENDING, APPROVED, REJECTED, CANCELLED (backward compatibility)
    @Builder.Default
    private List<RequestStatus> statusFilters = null; // Multiple statuses

    @Builder.Default
    private Integer page = 0;
    @Builder.Default
    private Integer size = 10;
    @Builder.Default
    private String sort = "submittedAt,desc";

    @Builder.Default
    private String search = null; // Search by request reason, class code, session title
}
