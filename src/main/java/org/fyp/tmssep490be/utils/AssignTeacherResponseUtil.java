package org.fyp.tmssep490be.utils;

import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.createclass.AssignTeacherResponse;
import org.fyp.tmssep490be.dtos.createclass.TeacherAvailabilityDTO;
import org.fyp.tmssep490be.entities.Teacher;
import org.fyp.tmssep490be.entities.enums.Skill;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for building AssignTeacherResponse
 */
@Component
@Slf4j
public class AssignTeacherResponseUtil {

    /**
     * Build response for successful teacher assignment
     *
     * @param classId Class ID
     * @param teacher Assigned teacher
     * @param totalSessions Total sessions in class
     * @param assignedSessionIds IDs of assigned sessions
     * @param remainingSessionIds IDs of remaining sessions without teacher
     * @param processingTimeMs Processing time in milliseconds
     * @return AssignTeacherResponse
     */
    public AssignTeacherResponse buildSuccessResponse(
            Long classId,
            Teacher teacher,
            int totalSessions,
            List<Long> assignedSessionIds,
            List<Long> remainingSessionIds,
            long processingTimeMs) {

        int assignedCount = assignedSessionIds.size();
        int remainingCount = remainingSessionIds.size();
        boolean needsSubstitute = remainingCount > 0;

        log.info("Building assign teacher response: {} assigned, {} remaining",
                assignedCount, remainingCount);

        return AssignTeacherResponse.builder()
                .classId(classId)
                .teacherId(teacher.getId())
                .teacherName(teacher.getUserAccount().getFullName())
                .totalSessions(totalSessions)
                .assignedCount(assignedCount)
                .assignedSessionIds(assignedSessionIds)
                .needsSubstitute(needsSubstitute)
                .remainingSessions(remainingCount)
                .remainingSessionIds(remainingSessionIds)
                .processingTimeMs(processingTimeMs)
                .build();
    }

    /**
     * Calculate availability status based on percentage
     *
     * @param availableCount Number of available sessions
     * @param totalCount Total number of sessions
     * @return AvailabilityStatus
     */
    public TeacherAvailabilityDTO.AvailabilityStatus calculateAvailabilityStatus(
            int availableCount, int totalCount) {

        if (availableCount == totalCount) {
            return TeacherAvailabilityDTO.AvailabilityStatus.FULLY_AVAILABLE;
        } else if (availableCount > 0) {
            return TeacherAvailabilityDTO.AvailabilityStatus.PARTIALLY_AVAILABLE;
        } else {
            return TeacherAvailabilityDTO.AvailabilityStatus.UNAVAILABLE;
        }
    }

    /**
     * Calculate availability percentage
     *
     * @param availableCount Number of available sessions
     * @param totalCount Total number of sessions
     * @return Percentage (0-100)
     */
    public double calculateAvailabilityPercentage(int availableCount, int totalCount) {
        if (totalCount == 0) {
            return 0.0;
        }
        return (availableCount * 100.0) / totalCount;
    }

    /**
     * Check if teacher has GENERAL skill (universal)
     *
     * @param skills List of teacher's skills
     * @return true if has GENERAL skill
     */
    public boolean hasGeneralSkill(List<Skill> skills) {
        return skills != null && skills.contains(Skill.GENERAL);
    }

    /**
     * Format skill list for display
     *
     * @param skills List of skills
     * @return Comma-separated skill names
     */
    public String formatSkills(List<Skill> skills) {
        if (skills == null || skills.isEmpty()) {
            return "No skills";
        }
        return skills.stream()
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }

    /**
     * Build conflict breakdown with all zeros
     *
     * @return Empty ConflictBreakdown
     */
    public TeacherAvailabilityDTO.ConflictBreakdown buildEmptyConflictBreakdown() {
        return TeacherAvailabilityDTO.ConflictBreakdown.builder()
                .noAvailability(0)
                .teachingConflict(0)
                .leaveConflict(0)
                .skillMismatch(0)
                .totalConflicts(0)
                .build();
    }

    /**
     * Update total conflicts in breakdown
     *
     * @param breakdown Conflict breakdown
     */
    public void updateTotalConflicts(TeacherAvailabilityDTO.ConflictBreakdown breakdown) {
        if (breakdown != null) {
            int total = breakdown.getNoAvailability() +
                       breakdown.getTeachingConflict() +
                       breakdown.getLeaveConflict() +
                       breakdown.getSkillMismatch();
            breakdown.setTotalConflicts(total);
        }
    }
}
