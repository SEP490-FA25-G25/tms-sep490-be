package org.fyp.tmssep490be.dtos.studentmanagement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncToBranchRequest {

    @NotNull(message = "Target branch ID is required")
    private Long targetBranchId;

    private String phone;
    private String address; 

    @Valid
    private List<SkillAssessmentInput> newSkillAssessments; 
}
