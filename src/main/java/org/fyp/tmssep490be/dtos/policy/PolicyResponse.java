package org.fyp.tmssep490be.dtos.policy;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class PolicyResponse {

    private Long id;
    private String policyKey;
    private String policyCategory;
    private String policyName;
    private String description;
    private String valueType;
    private String defaultValue;
    private String currentValue;
    private String minValue;
    private String maxValue;
    private String unit;
    private String scope;
    private Long branchId;
    private Long courseId;
    private Long classId;
    private boolean active;
    private Integer version;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}


