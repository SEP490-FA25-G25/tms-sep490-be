package org.fyp.tmssep490be.dtos.classcreation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewClassCodeResponse {
    private String previewCode;
    private String prefix;
    private Integer nextSequence;
    private String warning;
}
