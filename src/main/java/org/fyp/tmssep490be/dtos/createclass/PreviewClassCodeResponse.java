package org.fyp.tmssep490be.dtos.createclass;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for class code preview
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewClassCodeResponse {

    /**
     * Preview of the next class code
     * Note: This may change when actually submitting if another class is created in the meantime
     */
    private String previewCode;

    /**
     * The prefix used (COURSECODE-BRANCHCODE-YY)
     */
    private String prefix;

    /**
     * The next sequence number
     */
    private Integer nextSequence;

    /**
     * Warning message if sequence is approaching limit
     */
    private String warning;
}
