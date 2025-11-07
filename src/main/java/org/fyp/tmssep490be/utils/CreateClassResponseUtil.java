package org.fyp.tmssep490be.utils;

import org.fyp.tmssep490be.dtos.createclass.CreateClassResponse;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.springframework.stereotype.Component;

@Component
public class CreateClassResponseUtil {

    /**
     * Checks if class creation was successful
     */
    public boolean isSuccess(CreateClassResponse response) {
        return response != null &&
               response.getClassId() != null &&
               response.getCode() != null &&
               response.getSessionSummary() != null &&
               response.getSessionSummary().getSessionsGenerated() > 0;
    }

    /**
     * Checks if class is ready for next step (time slot assignment)
     */
    public boolean isReadyForNextStep(CreateClassResponse response) {
        return isSuccess(response) &&
               response.getStatus() == ClassStatus.DRAFT &&
               response.getApprovalStatus() == ApprovalStatus.PENDING;
    }

    /**
     * Returns workflow progress percentage (0-100%)
     * STEP 1 completed = 14.3% (1/7)
     */
    public double getWorkflowProgress(CreateClassResponse response) {
        if (!isSuccess(response)) return 0.0;
        return 14.3; // STEP 1 of 7 steps completed
    }

    /**
     * Returns next workflow step description
     */
    public String getNextStepDescription(CreateClassResponse response) {
        if (!isReadyForNextStep(response)) {
            return "Fix validation errors before proceeding";
        }
        return "STEP 2: Assign time slots to sessions";
    }

    /**
     * Calculates session generation progress percentage
     */
    public double getGenerationProgress(CreateClassResponse.SessionGenerationSummary summary) {
        if (summary == null || summary.getTotalSessionsInCourse() == 0) {
            return 0.0;
        }
        return (double) summary.getSessionsGenerated() / summary.getTotalSessionsInCourse() * 100;
    }

    /**
     * Checks if course has full session coverage
     */
    public boolean hasFullCourseCoverage(CreateClassResponse.SessionGenerationSummary summary) {
        return summary != null &&
               summary.getSessionsGenerated() == summary.getTotalSessionsInCourse();
    }

    /**
     * Gets session generation status message
     */
    public String getSessionGenerationStatus(CreateClassResponse response) {
        if (response == null || response.getSessionSummary() == null) {
            return "No session generation data available";
        }

        CreateClassResponse.SessionGenerationSummary summary = response.getSessionSummary();
        int generated = summary.getSessionsGenerated();
        int total = summary.getTotalSessionsInCourse();
        double progress = getGenerationProgress(summary);

        if (generated == 0) {
            return "No sessions generated";
        } else if (generated == total) {
            return String.format("All %d sessions generated successfully", total);
        } else {
            return String.format("Partially generated: %d/%d sessions (%.1f%%)",
                               generated, total, progress);
        }
    }

    /**
     * Gets formatted class creation summary
     */
    public String getClassCreationSummary(CreateClassResponse response) {
        if (response == null) {
            return "No class creation data available";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Class: ").append(response.getCode()).append(" - ").append(response.getName());

        if (response.getSessionSummary() != null) {
            CreateClassResponse.SessionGenerationSummary sessionSummary = response.getSessionSummary();
            summary.append("\nSessions: ").append(sessionSummary.getSessionsGenerated())
                   .append("/").append(sessionSummary.getTotalSessionsInCourse())
                   .append(" generated");

            if (sessionSummary.getCourseCode() != null) {
                summary.append(" for course ").append(sessionSummary.getCourseCode());
                if (sessionSummary.getCourseName() != null) {
                    summary.append(" (").append(sessionSummary.getCourseName()).append(")");
                }
            }
        }

        summary.append("\nStatus: ").append(response.getStatus())
               .append(" | Approval: ").append(response.getApprovalStatus());

        if (isReadyForNextStep(response)) {
            summary.append("\n✅ Ready for time slot assignment");
        }

        return summary.toString();
    }

    /**
     * Gets workflow completion percentage
     */
    public double getWorkflowCompletionPercentage(CreateClassResponse response) {
        double progress = 0.0;

        if (isSuccess(response)) {
            progress += 14.3; // STEP 1 completed
        }

        // Additional steps would be checked here when implemented
        // STEP 2: Time slot assignment (14.3%)
        // STEP 3: Resource assignment (14.3%)
        // STEP 4: Teacher assignment (14.3%)
        // STEP 5: Validation (14.3%)
        // STEP 6: Submission (14.3%)
        // STEP 7: Approval (14.3%)

        return progress;
    }

    /**
     * Gets next actionable step for the class
     */
    public String getNextActionableStep(CreateClassResponse response) {
        if (!isSuccess(response)) {
            return "Fix class creation errors";
        }

        if (!isReadyForNextStep(response)) {
            if (response.getStatus() != ClassStatus.DRAFT) {
                return "Class status must be DRAFT to proceed";
            }
            if (response.getApprovalStatus() != ApprovalStatus.PENDING) {
                return "Class approval status must be PENDING to proceed";
            }
        }

        return "Assign time slots to sessions";
    }
}