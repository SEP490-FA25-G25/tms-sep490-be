package org.fyp.tmssep490be.dtos.adminanalytic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Statistics for a single branch
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchStatDTO {
    private Long branchId;
    private String branchName;
    private Long centerId;
    private String centerName;
    private Long studentCount;
    private Long teacherCount;
    private Long classCount;
    private Long activeClassCount;
}

