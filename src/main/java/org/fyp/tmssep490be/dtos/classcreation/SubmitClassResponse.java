package org.fyp.tmssep490be.dtos.classcreation;

import lombok.*;

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

    public boolean isSuccess() {
        return success != null && success;
    }
}
