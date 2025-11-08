package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Teacher entity
 */
@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    
    /**
     * Find teacher by user account ID
     */
    Optional<Teacher> findByUserAccountId(Long userAccountId);

    // ==================== CREATE CLASS WORKFLOW - PHASE 2.3: TEACHER ASSIGNMENT (PRE-CHECK) ====================

    /**
     * <h2>PRE-CHECK Query: Find Available Teachers with Detailed Conflict Analysis</h2>
     * <p>
     * This is a <b>complex CTE query</b> that performs 3-condition checks for EVERY session:
     * <ol>
     *   <li><b>Teacher Availability:</b> Teacher must have registered availability (teacher_availability table)</li>
     *   <li><b>Teaching Conflict:</b> Teacher must NOT be teaching another class at the same time</li>
     *   <li><b>Leave Conflict:</b> Teacher must NOT be on leave (teaching_slot.status = 'ON_LEAVE')</li>
     * </ol>
     * </p>
     *
     * <h3>CRITICAL FEATURE: 'GENERAL' Skill = UNIVERSAL</h3>
     * <p>
     * If teacher has <code>Skill.GENERAL</code> → Can teach ANY session type (skill check bypassed)
     * </p>
     *
     * <h3>Performance Target:</h3>
     * <ul>
     *   <li>Query execution: &lt;100ms for 10 teachers × 36 sessions = 360 checks</li>
     *   <li>Uses CTEs for optimization</li>
     *   <li>Uses LEFT JOINs to detect conflicts</li>
     * </ul>
     *
     * <h3>Result Format:</h3>
     * <pre>
     * [
     *   {
     *     "teacherId": 5,
     *     "fullName": "Jane Smith",
     *     "email": "jane@example.com",
     *     "skills": "GENERAL,SPEAKING",
     *     "hasGeneralSkill": true,
     *     "totalSessions": 10,
     *     "availableSessions": 10,
     *     "noAvailabilityCount": 0,
     *     "teachingConflictCount": 0,
     *     "leaveConflictCount": 0,
     *     "skillMismatchCount": 0
     *   },
     *   ...
     * ]
     * </pre>
     *
     * @param classId Class ID to check teachers for
     * @return List of Object[] with teacher availability data
     * <p>
     * Object[] structure:
     * [0] Long teacherId
     * [1] String fullName
     * [2] String email
     * [3] String skills (comma-separated)
     * [4] Boolean hasGeneralSkill
     * [5] Integer totalSessions
     * [6] Integer availableSessions
     * [7] Integer noAvailabilityCount
     * [8] Integer teachingConflictCount
     * [9] Integer leaveConflictCount
     * [10] Integer skillMismatchCount
     * </p>
     */
    @Query(value = """
        WITH
        -- Step 1: Get all sessions for the class with their requirements
        class_sessions AS (
            SELECT
                s.id AS session_id,
                s.date,
                s.time_slot_template_id,
                cs.skill_set,
                EXTRACT(DOW FROM s.date) AS day_of_week
            FROM session s
            JOIN course_session cs ON s.course_session_id = cs.id
            WHERE s.class_id = :classId
        ),
        
        -- Step 2: Get all teachers with their skills
        teachers_with_skills AS (
            SELECT
                t.id AS teacher_id,
                ua.full_name,
                ua.email,
                STRING_AGG(DISTINCT ts.skill::TEXT, ',' ORDER BY ts.skill::TEXT) AS skills,
                BOOL_OR(ts.skill = 'GENERAL') AS has_general_skill
            FROM teacher t
            JOIN user_account ua ON t.user_account_id = ua.id
            LEFT JOIN teacher_skill ts ON ts.teacher_id = t.id
            GROUP BY t.id, ua.full_name, ua.email
        ),
        
        -- Step 3: Check each teacher against each session
        teacher_session_checks AS (
            SELECT
                tws.teacher_id,
                tws.full_name,
                tws.email,
                tws.skills,
                tws.has_general_skill,
                cs.session_id,
                cs.time_slot_template_id,
                cs.day_of_week,
                cs.date,
                cs.skill_set,
                
                -- Condition 1: Teacher availability check
                CASE WHEN ta.teacher_id IS NULL THEN 1 ELSE 0 END AS no_availability,
                
                -- Condition 2: Teaching conflict check (teaching another class at same time)
                CASE WHEN EXISTS (
                    SELECT 1
                    FROM teaching_slot ts_conflict
                    JOIN session s_conflict ON ts_conflict.session_id = s_conflict.id
                    WHERE ts_conflict.teacher_id = tws.teacher_id
                      AND s_conflict.date = cs.date
                      AND s_conflict.time_slot_template_id = cs.time_slot_template_id
                      AND s_conflict.id != cs.session_id
                      AND ts_conflict.status != 'ON_LEAVE'
                ) THEN 1 ELSE 0 END AS teaching_conflict,
                
                -- Condition 3: Leave conflict check (teacher on leave for this session)
                CASE WHEN EXISTS (
                    SELECT 1
                    FROM teaching_slot ts_leave
                    WHERE ts_leave.teacher_id = tws.teacher_id
                      AND ts_leave.session_id = cs.session_id
                      AND ts_leave.status = 'ON_LEAVE'
                ) THEN 1 ELSE 0 END AS leave_conflict,
                
                -- Condition 4: Skill mismatch check (unless has GENERAL skill)
                CASE
                    WHEN tws.has_general_skill THEN 0
                    WHEN cs.skill_set IS NULL THEN 0
                    WHEN NOT EXISTS (
                        SELECT 1
                        FROM teacher_skill ts_skill
                        WHERE ts_skill.teacher_id = tws.teacher_id
                          AND ts_skill.skill = ANY(cs.skill_set)
                    ) THEN 1
                    ELSE 0
                END AS skill_mismatch
                
            FROM teachers_with_skills tws
            CROSS JOIN class_sessions cs
            LEFT JOIN teacher_availability ta
                ON ta.teacher_id = tws.teacher_id
                AND ta.time_slot_template_id = cs.time_slot_template_id
                AND ta.day_of_week = cs.day_of_week
        ),
        
        -- Step 4: Aggregate results per teacher
        teacher_summary AS (
            SELECT
                teacher_id,
                full_name,
                email,
                skills,
                has_general_skill,
                COUNT(*) AS total_sessions,
                SUM(CASE WHEN (no_availability + teaching_conflict + leave_conflict + skill_mismatch) = 0 THEN 1 ELSE 0 END) AS available_sessions,
                SUM(no_availability) AS no_availability_count,
                SUM(teaching_conflict) AS teaching_conflict_count,
                SUM(leave_conflict) AS leave_conflict_count,
                SUM(skill_mismatch) AS skill_mismatch_count
            FROM teacher_session_checks
            GROUP BY teacher_id, full_name, email, skills, has_general_skill
        )
        
        -- Step 5: Return results sorted by availability (best matches first)
        SELECT
            teacher_id,
            full_name,
            email,
            COALESCE(skills, '') AS skills,
            COALESCE(has_general_skill, false) AS has_general_skill,
            total_sessions,
            available_sessions,
            no_availability_count,
            teaching_conflict_count,
            leave_conflict_count,
            skill_mismatch_count
        FROM teacher_summary
        ORDER BY available_sessions DESC, teacher_id ASC
        """, nativeQuery = true)
    List<Object[]> findAvailableTeachersWithPrecheck(@Param("classId") Long classId);
}
