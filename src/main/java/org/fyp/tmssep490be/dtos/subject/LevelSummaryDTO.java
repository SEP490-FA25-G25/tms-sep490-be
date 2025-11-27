package org.fyp.tmssep490be.dtos.subject;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LevelSummaryDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Integer expectedDurationHours;
    private Integer sortOrder;
    private long courseCount;
}

