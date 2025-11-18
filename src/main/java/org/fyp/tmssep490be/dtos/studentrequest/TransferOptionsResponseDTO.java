
package org.fyp.tmssep490be.dtos.studentrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for flexible transfer options (AA use case)
 * Wraps transfer options with current class info and transfer criteria
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferOptionsResponseDTO {

    /**
     * Current class information
     */
    private CurrentClassInfo currentClass;

    /**
     * Transfer criteria applied (what dimensions are changing)
     */
    private TransferCriteria transferCriteria;

    /**
     * Available target classes matching criteria
     */
    private List<TransferOptionDTO> availableClasses;

    /**
     * Current class information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentClassInfo {
        private Long id;
        private String code;
        private String name;
        private String branchName;
        private String modality;
        private String scheduleDays;
        private String scheduleTime;
        private Integer currentSession;
    }

    /**
     * Transfer criteria - what dimensions are being changed
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferCriteria {
        private boolean branchChange;
        private boolean modalityChange;
        private boolean scheduleChange;
    }
}
