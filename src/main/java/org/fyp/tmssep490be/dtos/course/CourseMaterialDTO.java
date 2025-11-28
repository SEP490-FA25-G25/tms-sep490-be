package org.fyp.tmssep490be.dtos.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseMaterialDTO {
    private Long id;
    private String name;
    private String type;
    private String url;
    private String scope;
    private Long phaseId;
    private Long sessionId;
}
