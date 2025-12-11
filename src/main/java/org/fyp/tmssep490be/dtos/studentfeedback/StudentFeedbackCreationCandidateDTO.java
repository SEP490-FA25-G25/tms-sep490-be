package org.fyp.tmssep490be.dtos.studentfeedback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentFeedbackCreationCandidateDTO {
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private Long classId;
    private String classCode;
    private String className;
    private Long phaseId;
    private String phaseName;
    private String subjectName;
    private LocalDate lastSessionDate;
}
