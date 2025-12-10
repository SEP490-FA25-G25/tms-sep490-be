package org.fyp.tmssep490be.dtos.subject;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SubjectPLODTO {
    private Long id;
    private String code;
    private String description;
    private String programName;
}