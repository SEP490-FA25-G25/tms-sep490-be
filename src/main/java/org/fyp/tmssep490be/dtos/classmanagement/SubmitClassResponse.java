package org.fyp.tmssep490be.dtos.classmanagement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitClassResponse {
    private Long classId;
    private String classCode;
    private String message;
    private Boolean success;
    private String submittedAt;
    private String approvalStatus;

    // Helper methods
    public boolean isSuccess() {
        return success != null && success;
    }
}