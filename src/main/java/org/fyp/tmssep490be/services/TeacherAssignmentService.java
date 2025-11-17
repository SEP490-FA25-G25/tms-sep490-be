package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.createclass.AssignTeacherRequest;
import org.fyp.tmssep490be.dtos.createclass.AssignTeacherResponse;
import org.fyp.tmssep490be.dtos.createclass.TeacherAvailabilityDTO;
import org.fyp.tmssep490be.dtos.createclass.TeacherDayAvailabilityDTO;

import java.util.List;

/**
 * Service for Teacher Assignment with PRE-CHECK approach
 * <p>
 * PRE-CHECK Philosophy:
 * <ul>
 *   <li>Step 1: Query available teachers with detailed conflict analysis (PRE-CHECK)</li>
 *   <li>Step 2: User selects teacher based on availability info</li>
 *   <li>Step 3: Direct bulk insert (no re-checking needed)</li>
 * </ul>
 * </p>
 * <p>
 * This eliminates trial-and-error assignment and provides transparency
 * </p>
 */
public interface TeacherAssignmentService {

    /**
     * PRE-CHECK: Query available teachers with detailed conflict analysis
     * <p>
     * Executes complex CTE query to check 4 conditions for EVERY session:
     * <ol>
     *   <li>Teacher availability registered</li>
     *   <li>No teaching conflict (not teaching another class)</li>
     *   <li>No leave conflict (not on leave)</li>
     *   <li>No skill mismatch (or has GENERAL skill = universal)</li>
     * </ol>
     * </p>
     *
     * @param classId Class ID
     * @return List of teachers with availability status (sorted by availability DESC)
     */
    List<TeacherAvailabilityDTO> queryAvailableTeachersWithPrecheck(Long classId);

    /**
     * Assign teacher to class sessions with direct bulk insert
     * <p>
     * No re-checking needed - assumes user selected teacher based on PRE-CHECK results
     * </p>
     * <p>
     * Supports two modes:
     * <ul>
     *   <li><b>Full Assignment:</b> Assign teacher to ALL sessions (request.sessionIds = null)</li>
     *   <li><b>Partial Assignment:</b> Assign teacher to specific sessions (request.sessionIds provided)</li>
     * </ul>
     * </p>
     *
     * @param classId Class ID
     * @param request Assignment request (teacherId + optional sessionIds)
     * @return Assignment response with results
     */
    AssignTeacherResponse assignTeacher(Long classId, AssignTeacherRequest request);

    /**
     * Query teachers available by day of week (for multi-teacher assignment mode)
     * <p>
     * Used for "Phân công nhiều giáo viên cho lớp học" where teachers can be assigned
     * to specific days of the week instead of all sessions.
     * </p>
     * <p>
     * A teacher is included if they are available for AT LEAST ONE FULL DAY of the week,
     * meaning they can teach ALL sessions on that specific day of week from course start to end.
     * </p>
     * <p>
     * <b>Example:</b>
     * <ul>
     *   <li>Class has 24 sessions: 8 on Monday, 8 on Wednesday, 8 on Friday</li>
     *   <li>John Smith: Available for all 8 Mondays (no conflicts) → Included with Monday availability</li>
     *   <li>Lisa Chen: Available for all 8 Wednesdays (no conflicts) → Included with Wednesday availability</li>
     *   <li>Anna Martinez: Available for 7/8 Fridays (1 conflict) → NOT included for Friday</li>
     * </ul>
     * </p>
     * <p>
     * <b>Consistency Check:</b> Teacher must be available from first to last session on that day.
     * Partial availability (e.g., 7/8 sessions) is NOT acceptable.
     * </p>
     *
     * @param classId Class ID
     * @return List of teachers with their available days (only fully available days included)
     */
    List<TeacherDayAvailabilityDTO> queryTeachersAvailableByDay(Long classId);
}
