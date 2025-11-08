package org.fyp.tmssep490be.dtos.createclass;

import lombok.*;

import java.util.List;

/**
 * Response DTO for teacher assignment with PRE-CHECK approach
 * <p>
 * Contains assignment results after direct bulk insert (no re-checking needed)
 * </p>
 *
 * @see AssignTeacherRequest
 * @see org.fyp.tmssep490be.utils.AssignTeacherResponseUtil
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignTeacherResponse {

    /**
     * Class ID
     */
    private Long classId;

    /**
     * Assigned teacher ID
     */
    private Long teacherId;

    /**
     * Assigned teacher's full name
     */
    private String teacherName;

    /**
     * Total number of sessions in the class
     */
    private Integer totalSessions;

    /**
     * Number of sessions successfully assigned to this teacher
     */
    private Integer assignedCount;

    /**
     * List of assigned session IDs
     */
    @Builder.Default
    private List<Long> assignedSessionIds = List.of();

    /**
     * Whether this teacher needs a substitute for some sessions
     * <p>
     * true = Partial assignment (assignedCount < totalSessions)
     * false = Full assignment (assignedCount == totalSessions)
     * </p>
     */
    private Boolean needsSubstitute;

    /**
     * Number of remaining sessions without teacher
     * <p>
     * 0 = All sessions have teacher
     * > 0 = Some sessions still need teacher assignment
     * </p>
     */
    private Integer remainingSessions;

    /**
     * List of remaining session IDs without teacher (for substitute assignment)
     */
    @Builder.Default
    private List<Long> remainingSessionIds = List.of();

    /**
     * Processing time in milliseconds
     * <p>
     * Target: <120ms for PRE-CHECK + assignment
     * </p>
     */
    private Long processingTimeMs;
}
