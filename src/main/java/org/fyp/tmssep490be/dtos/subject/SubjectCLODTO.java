package org.fyp.tmssep490be.dtos.subject;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SubjectCLODTO {
    private String code;
    private String description;
    private List<String> mappedPLOs;
}