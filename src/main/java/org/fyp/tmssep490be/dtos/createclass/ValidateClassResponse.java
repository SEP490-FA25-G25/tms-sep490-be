package org.fyp.tmssep490be.dtos.createclass;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateClassResponse {

    private boolean valid;
    private boolean canSubmit;
    private Long classId;
    private String classCode;
    private String message;
    private ValidationChecks checks;
    private List<String> errors;
    private List<String> warnings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationChecks {
        private int totalSessions;
        private int sessionsWithTimeslot;
        private int sessionsWithResource;
        private int sessionsWithTeacher;
        private int completionPercentage;
        private boolean hasTimeslotConflicts;
        private boolean hasResourceConflicts;
        private boolean hasTeacherConflicts;
        private boolean startDateInPast;
        private boolean hasMultipleTeachersPerSkillGroup;
        private LocalDate earliestSessionDate;
        private LocalDate latestSessionDate;
    }

    // Helper methods
    public boolean isValid() {
        return valid && (errors == null || errors.isEmpty());
    }

    public boolean canSubmit() {
        return canSubmit && isValid();
    }

    /**
     * Get completion percentage for validation
     */
    public double getCompletionPercentage() {
        if (checks == null) return 0.0;
        return checks.getCompletionPercentage();
    }

    /**
     * Check if validation shows class is ready for submission
     */
    public boolean isReadyForSubmission() {
        return canSubmit() && getCompletionPercentage() == 100.0;
    }

    /**
     * Get critical issues that prevent submission
     */
    public List<String> getCriticalIssues() {
        return errors != null ? errors : List.of();
    }

    /**
     * Get warnings that don't prevent submission
     */
    public List<String> getWarnings() {
        return warnings != null ? warnings : List.of();
    }

    /**
     * Get formatted validation summary
     */
    public String getValidationSummary() {
        if (isReadyForSubmission()) {
            return "Class is complete and ready for submission";
        } else if (canSubmit()) {
            return String.format("Class is %d%% complete - ready for submission", (int) getCompletionPercentage());
        } else {
            return String.format("Class has %d critical issues that must be resolved", getCriticalIssues().size());
        }
    }
}