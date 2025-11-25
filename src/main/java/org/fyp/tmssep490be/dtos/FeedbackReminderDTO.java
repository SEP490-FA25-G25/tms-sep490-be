package org.fyp.tmssep490be.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for feedback collection reminder
 * Used by FeedbackCollectionReminderJob to send reminders to students
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackReminderDTO {
    private Long studentId;
    private String studentName;
    private Long phaseId;
    private String phaseName;
    private String courseName;
    private Long classId;
    private String className;
}