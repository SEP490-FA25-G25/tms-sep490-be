package org.fyp.tmssep490be.dtos.createclass;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for per-session resource assignment (Quick Fix).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignSessionResourceResponse {

    private Long classId;
    private Long sessionId;
    private String sessionDate;
    private Long resourceId;
    private String resourceCode;
    private String resourceName;
    private Boolean resolved;
    private String message;
}
