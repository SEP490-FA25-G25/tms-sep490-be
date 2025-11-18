package org.fyp.tmssep490be.dtos.studentrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response wrapper for makeup options with target session context
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MakeupOptionsResponseDTO {

    private TargetSessionInfo targetSession;
    private List<MakeupOptionDTO> makeupOptions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TargetSessionInfo {
        private Long sessionId;
        private Long courseSessionId;
        private Long classId;
        private String classCode;
        private Long branchId;
        private String modality;
    }
}
