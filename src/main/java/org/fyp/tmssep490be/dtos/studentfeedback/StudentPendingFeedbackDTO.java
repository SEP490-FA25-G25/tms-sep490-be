package org.fyp.tmssep490be.dtos.studentfeedback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentPendingFeedbackDTO {
    private Long feedbackId;
    private Long classId;
    private String classCode;
    private String className;
    private String subjectName;
    private Long phaseId;
    private String phaseName;
    private OffsetDateTime createdAt;
}
