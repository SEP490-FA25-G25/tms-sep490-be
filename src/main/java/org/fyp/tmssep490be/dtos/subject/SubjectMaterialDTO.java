package org.fyp.tmssep490be.dtos.subject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectMaterialDTO {
    private Long id;
    private String name; // Changed from 'title' to match frontend
    private String type; // Changed from 'materialType' to match frontend
    private String url;
    private String scope;
    private Long phaseId;
    private Long sessionId;
}
