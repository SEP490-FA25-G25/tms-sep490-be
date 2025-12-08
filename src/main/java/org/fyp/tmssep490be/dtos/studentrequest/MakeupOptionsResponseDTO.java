package org.fyp.tmssep490be.dtos.studentrequest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MakeupOptionsResponseDTO {

    private Long targetSessionId;

    private TargetSessionInfo targetSession;

    private List<MakeupOptionDTO> makeupOptions;

    private Integer totalOptions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TargetSessionInfo {
        private Long sessionId;

        private Long subjectSessionId;

        private Long classId;

        private String classCode;

        private Long branchId;

        private String branchName;

        private String modality;
    }
}
