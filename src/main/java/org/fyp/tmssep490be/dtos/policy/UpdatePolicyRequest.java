package org.fyp.tmssep490be.dtos.policy;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdatePolicyRequest {

    @NotBlank
    private String newValue;

    private String reason;
}


