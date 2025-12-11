package org.fyp.tmssep490be.dtos.curriculum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for Subject-PLO Matrix view.
 * Shows which PLOs each Subject in a Curriculum addresses (through CLO
 * mappings).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectPLOMatrixDTO {

    private Long curriculumId;
    private String curriculumCode;
    private String curriculumName;

    /**
     * List of PLOs for this curriculum (column headers)
     */
    private List<PLOInfo> plos;

    /**
     * List of subjects with their PLO mappings (rows)
     */
    private List<SubjectPLORow> subjects;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PLOInfo {
        private Long id;
        private String code;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectPLORow {
        private Long subjectId;
        private String subjectCode;
        private String subjectName;
        private String levelName;
        private String status;

        /**
         * Boolean array indicating if this subject maps to each PLO.
         * Index corresponds to the PLO at the same index in the plos list.
         * true = at least one CLO of this subject maps to the PLO
         */
        private List<Boolean> ploMappings;
    }
}
