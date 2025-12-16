package org.fyp.tmssep490be.dtos.studentrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferOptionsResponseDTO {

    private CurrentClassInfo currentClass;
    private List<TransferOptionDTO> availableClasses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentClassInfo {
        private Long id;
        private String code;
        private String name;
        private Long subjectId;
        private Long branchId;
        private String branchName;
        private String modality;
        private String scheduleDays;
        private String scheduleTime;
        private Integer currentSession;
    }
}
