package org.fyp.tmssep490be.dtos.subject;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SubjectSessionDTO {
    private Long id;
    private Integer sequenceNo;
    private String topic;
    private String studentTask;
    private List<String> skills;
    private List<String> mappedCLOs;
    private List<SubjectMaterialDTO> materials;
}