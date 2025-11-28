package org.fyp.tmssep490be.dtos.subject;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SubjectDetailDTO {

    private SubjectSummaryDTO summary;
    private List<LevelSummaryDTO> levels;
    private List<CourseSummaryDTO> courses;
}

