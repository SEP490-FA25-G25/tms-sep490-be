package org.fyp.tmssep490be.dtos.curriculum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LevelResponseDTO {
    private String id;
    private String code;
    private String name;
    private String description;
    private Integer durationHours;
    private String subjectName;
    private String subjectCode;
    private String status;
    private Integer sortOrder;
}
