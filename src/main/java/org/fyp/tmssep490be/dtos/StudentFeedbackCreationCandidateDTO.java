package org.fyp.tmssep490be.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO dùng cho job tạo sẵn bản ghi feedback sau khi phase kết thúc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentFeedbackCreationCandidateDTO {
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private Long classId;
    private String classCode;
    private String className;
    private Long phaseId;
    private String phaseName;
    private String courseName;
    private LocalDate lastSessionDate;
}
