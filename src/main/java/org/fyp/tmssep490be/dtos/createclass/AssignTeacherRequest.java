package org.fyp.tmssep490be.dtos.createclass;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

/**
 * Request DTO for assigning teacher to class sessions (STEP 5)
 * <p>
 * Two assignment modes:
 * <ul>
 *   <li><b>Full Assignment:</b> Assign teacher to ALL sessions (sessionIds = null)</li>
 *   <li><b>Partial Assignment:</b> Assign teacher to specific sessions (sessionIds provided)</li>
 * </ul>
 * </p>
 * <p>
 * Example (Full Assignment):
 * <pre>
 * {
 *   "teacherId": 5
 * }
 * </pre>
 * </p>
 * <p>
 * Example (Partial Assignment):
 * <pre>
 * {
 *   "teacherId": 5,
 *   "sessionIds": [101, 102, 103]
 * }
 * </pre>
 * </p>
 *
 * @see AssignTeacherResponse
 * @see org.fyp.tmssep490be.validators.AssignTeacherRequestValidator
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignTeacherRequest {

    /**
     * Teacher ID to assign
     */
    @NotNull(message = "Teacher ID is required")
    @Positive(message = "Teacher ID must be positive")
    private Long teacherId;

    /**
     * Optional: Specific session IDs to assign (for partial assignment)
     * <p>
     * If null or empty → Full Assignment (all sessions)
     * If provided → Partial Assignment (only these sessions)
     * </p>
     */
    @Size(min = 1, message = "If provided, session IDs list must not be empty")
    private List<Long> sessionIds;
}
