package org.fyp.tmssep490be.dtos.classmanagement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectClassResponse {
    private Long classId;
    private String classCode;
    private String message;
    private Boolean success;
    private String rejectionReason;
    private String decidedAt;
    private String decidedBy;

    // Helper methods
    public boolean isSuccess() {
        return success != null && success;
    }
}