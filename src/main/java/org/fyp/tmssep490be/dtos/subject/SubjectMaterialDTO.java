package org.fyp.tmssep490be.dtos.subject;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SubjectMaterialDTO {
    private Long id;
    private String title;
    private String materialType; // MaterialType enum value
    private String url;
    private String scope; // SUBJECT, PHASE, SESSION
    private Long phaseId;
    private Long sessionId;
}