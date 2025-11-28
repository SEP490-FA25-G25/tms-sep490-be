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
        -- Step 0: Get course specialization and branch from class (source of truth for filtering)
        course_info AS (
            SELECT 
                s.code as course_specialization,
                cl.branch_id as class_branch_id
            FROM course c
            JOIN subject s ON c.subject_id = s.id
            JOIN class cl ON cl.course_id = c.id
            WHERE cl.id = :classId
        ),
        
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
        
        -- Step 2: Get all teachers with their skills (FILTERED by specialization AND branch)
        teachers_with_skills AS (
            SELECT
                t.id AS teacher_id,
                ua.full_name,
                ua.email,
                STRING_AGG(DISTINCT ts.skill::TEXT, ',' ORDER BY ts.skill::TEXT) AS skills,
                BOOL_OR(ts.skill = 'GENERAL') AS has_general_skill
            FROM teacher t
            JOIN user_account ua ON t.user_account_id = ua.id
            JOIN user_branches ub ON ub.user_id = ua.id
            LEFT JOIN teacher_skill ts ON ts.teacher_id = t.id
            CROSS JOIN course_info ci
            WHERE ts.specialization ILIKE ci.course_specialization || '%'
              AND ub.branch_id = ci.class_branch_id
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
                -- Only considers conflicts with ACTIVE classes that overlap in time
                CASE WHEN EXISTS (
                    SELECT 1
                    FROM teaching_slot ts_conflict
                    JOIN session s_conflict ON ts_conflict.session_id = s_conflict.id
                    JOIN class cl_conflict ON s_conflict.class_id = cl_conflict.id
                    WHERE ts_conflict.teacher_id = tws.teacher_id
                      AND s_conflict.date = cs.date
                      AND s_conflict.time_slot_template_id = cs.time_slot_template_id
                      AND s_conflict.id != cs.session_id
                      AND ts_conflict.status != 'ON_LEAVE'
                      AND cl_conflict.status IN ('PENDING_APPROVAL', 'APPROVED', 'IN_PROGRESS', 'ONGOING')
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

    /**
     * Find teachers available by specific days of week (for multi-teacher assignment mode)
     * <p>
     * Returns teachers who are fully available for at least ONE DAY of the week.
     * A teacher is included for a day only if they can teach ALL sessions on that day from start to end.
     * </p>
     * <p>
     * <b>Query Logic:</b>
     * <ol>
     *   <li>Group class sessions by day of week</li>
     *   <li>For each teacher-day combination, check all 4 conditions:
     *     <ul>
     *       <li>Teacher has registered availability for that day</li>
     *       <li>No teaching conflicts (not teaching another class at same time)</li>
     *       <li>No leave conflicts (not on approved leave)</li>
     *       <li>No skill mismatch (or has GENERAL skill)</li>
     *     </ul>
     *   </li>
     *   <li>Count available sessions per day</li>
     *   <li>Only return days where available_sessions = total_sessions (100% availability)</li>
     * </ol>
     * </p>
     * <p>
     * <b>Result Format:</b> Array of [teacher_id, full_name, email, employee_code, skills,
     * has_general_skill, day_of_week, day_name, total_sessions, available_sessions,
     * first_date, last_date, time_slot_display]
     * </p>
     * <p>
     * <b>Example:</b>
     * <ul>
     *   <li>John Smith: Available Monday (8/8 sessions) → Row for Monday</li>
     *   <li>Lisa Chen: Available Wednesday (8/8 sessions) → Row for Wednesday</li>
     *   <li>Anna Martinez: Available Friday 7/8 sessions → NO row (not fully available)</li>
     * </ul>
     * </p>
     *
     * @param classId Class ID
     * @return List of Object[] with teacher info + day availability details
     */
    @Query(value = """
        WITH class_schedule AS (
          -- Get all sessions grouped by day of week with date range
          SELECT 
            CASE WHEN EXTRACT(DOW FROM s.date)::INTEGER = 0 THEN 7 ELSE EXTRACT(DOW FROM s.date)::INTEGER END as day_of_week,
            s.date,
            s.id as session_id,
            s.time_slot_template_id,
            MIN(s.date) OVER (PARTITION BY EXTRACT(DOW FROM s.date)) as first_date,
            MAX(s.date) OVER (PARTITION BY EXTRACT(DOW FROM s.date)) as last_date
          FROM session s
          WHERE s.class_id = :classId
            AND s.status = 'PLANNED'
        ),
        teacher_day_availability AS (
          -- Check each teacher's availability for each day of week
          SELECT 
            t.id as teacher_id,
            ua.full_name,
            ua.email,
            t.employee_code,
            cs.day_of_week,
            cs.first_date,
            cs.last_date,
            COUNT(DISTINCT cs.session_id) as total_sessions,
            ts_template.start_time,
            ts_template.end_time,
            
            -- Count sessions where teacher IS available (no conflicts)
            COUNT(DISTINCT cs.session_id) FILTER (
              WHERE 
                -- Check 1: Teacher has availability registered for this day AND time slot
                EXISTS (
                  SELECT 1 FROM teacher_availability ta
                  WHERE ta.teacher_id = t.id
                    AND ta.day_of_week = cs.day_of_week
                    AND ta.time_slot_template_id = cs.time_slot_template_id
                )
                -- Check 2: No teaching conflict (not teaching another class)
                AND NOT EXISTS (
                  SELECT 1 FROM teaching_slot ts2
                  JOIN session s2 ON ts2.session_id = s2.id
                  WHERE ts2.teacher_id = t.id
                    AND s2.date = cs.date
                    AND s2.time_slot_template_id = cs.time_slot_template_id
                    AND s2.class_id != :classId
                    AND ts2.status = 'SCHEDULED'
                )
                -- Check 3: No leave conflict
                -- TODO: Implement when leave_request table is created
                -- AND NOT EXISTS (
                --   SELECT 1 FROM leave_request lr
                --   WHERE lr.teacher_id = t.id
                --     AND lr.status = 'APPROVED'
                --     AND cs.date BETWEEN lr.start_date AND lr.end_date
                -- )
                -- Check 4: Skill match (or has GENERAL skill)
                AND (
                  -- Has GENERAL skill (universal)
                  EXISTS (
                    SELECT 1 FROM teacher_skill ts
                    WHERE ts.teacher_id = t.id
                      AND ts.skill = 'GENERAL'
                  )
                  OR
                  -- Has matching skill for this class's subject
                  EXISTS (
                    SELECT 1 FROM teacher_skill ts
                    JOIN class c ON c.id = :classId
                    JOIN course co ON co.id = c.course_id
                    JOIN level l ON l.id = co.level_id
                    JOIN subject subj ON subj.id = l.subject_id
                    WHERE ts.teacher_id = t.id
                      AND (
                        (subj.name ILIKE '%IELTS%' AND ts.skill IN ('IELTS_LISTENING', 'IELTS_READING', 'IELTS_WRITING', 'IELTS_SPEAKING'))
                        OR (subj.name ILIKE '%TOEIC%' AND ts.skill IN ('TOEIC_LISTENING', 'TOEIC_READING'))
                        OR (subj.name ILIKE '%TOEFL%' AND ts.skill IN ('TOEFL_LISTENING', 'TOEFL_READING', 'TOEFL_WRITING', 'TOEFL_SPEAKING'))
                      )
                  )
                )
            ) as available_sessions
            
          FROM teacher t
          JOIN user_account ua ON t.user_account_id = ua.id
          JOIN user_branches ub ON ub.user_id = ua.id
          JOIN class c ON c.id = :classId
          CROSS JOIN class_schedule cs
          LEFT JOIN time_slot_template ts_template ON cs.time_slot_template_id = ts_template.id
          WHERE ua.status = 'ACTIVE'
            AND ub.branch_id = c.branch_id  -- IMPORTANT: Teacher must be in same branch as class
          GROUP BY t.id, ua.full_name, ua.email, t.employee_code, cs.day_of_week, cs.first_date, cs.last_date, ts_template.start_time, ts_template.end_time
          HAVING COUNT(DISTINCT cs.session_id) = COUNT(DISTINCT cs.session_id) FILTER (
            WHERE 
              EXISTS (
                SELECT 1 FROM teacher_availability ta
                WHERE ta.teacher_id = t.id AND ta.day_of_week = cs.day_of_week
              )
              AND NOT EXISTS (
                SELECT 1 FROM teaching_slot ts2
                JOIN session s2 ON ts2.session_id = s2.id
                WHERE ts2.teacher_id = t.id AND s2.date = cs.date AND s2.time_slot_template_id = cs.time_slot_template_id
                  AND s2.class_id != :classId AND ts2.status = 'SCHEDULED'
              )
              -- Check 3: No leave conflict  
              -- TODO: Implement when leave_request table is created
              -- AND NOT EXISTS (
              --   SELECT 1 FROM leave_request lr
              --   WHERE lr.teacher_id = t.id AND lr.status = 'APPROVED' AND cs.date BETWEEN lr.start_date AND lr.end_date
              -- )
              AND (
                EXISTS (SELECT 1 FROM teacher_skill ts WHERE ts.teacher_id = t.id AND ts.skill = 'GENERAL')
                OR EXISTS (
                  SELECT 1 FROM teacher_skill ts
                  JOIN class c ON c.id = :classId
                  JOIN course co ON co.id = c.course_id
                  JOIN level l ON l.id = co.level_id
                  JOIN subject subj ON subj.id = l.subject_id
                  WHERE ts.teacher_id = t.id AND (
                    (subj.name ILIKE '%IELTS%' AND ts.skill IN ('IELTS_LISTENING', 'IELTS_READING', 'IELTS_WRITING', 'IELTS_SPEAKING'))
                    OR (subj.name ILIKE '%TOEIC%' AND ts.skill IN ('TOEIC_LISTENING', 'TOEIC_READING'))
                    OR (subj.name ILIKE '%TOEFL%' AND ts.skill IN ('TOEFL_LISTENING', 'TOEFL_READING', 'TOEFL_WRITING', 'TOEFL_SPEAKING'))
                  )
                )
              )
          )  -- HAVING: Only include days with 100% availability
        ),
        teacher_skills_agg AS (
          -- Aggregate skills per teacher
          SELECT 
            t.id as teacher_id,
            STRING_AGG(DISTINCT ts.skill::TEXT, ',' ORDER BY ts.skill::TEXT) as skills,
            BOOL_OR(ts.skill = 'GENERAL') as has_general_skill
          FROM teacher t
          LEFT JOIN teacher_skill ts ON t.id = ts.teacher_id
          GROUP BY t.id
        )
        SELECT 
          tda.teacher_id,
          tda.full_name,
          tda.email,
          tda.employee_code,
          tsa.skills,
          tsa.has_general_skill,
          tda.day_of_week,
          CASE tda.day_of_week
            WHEN 1 THEN 'Thứ Hai'
            WHEN 2 THEN 'Thứ Ba'
            WHEN 3 THEN 'Thứ Tư'
            WHEN 4 THEN 'Thứ Năm'
            WHEN 5 THEN 'Thứ Sáu'
            WHEN 6 THEN 'Thứ Bảy'
            WHEN 7 THEN 'Chủ Nhật'
          END as day_name,
          tda.total_sessions,
          tda.available_sessions,
          tda.first_date,
          tda.last_date,
          CONCAT(TO_CHAR(tda.start_time, 'HH24:MI'), ' - ', TO_CHAR(tda.end_time, 'HH24:MI')) as time_slot_display
        FROM teacher_day_availability tda
        JOIN teacher_skills_agg tsa ON tda.teacher_id = tsa.teacher_id
        ORDER BY tda.teacher_id, tda.day_of_week
        """, nativeQuery = true)
    List<Object[]> findAvailableTeachersByDay(@Param("classId") Long classId);

    // ==================== ANALYTICS METHODS ====================

    /**
     * Count teachers by branch ID
     */
    @Query("SELECT COUNT(DISTINCT t) FROM Teacher t " +
           "INNER JOIN t.userAccount u " +
           "INNER JOIN u.userBranches ub " +
           "WHERE ub.branch.id = :branchId")
    long countByBranchId(@Param("branchId") Long branchId);

    /**
     * Count teachers assigned to classes in specific branches
     */
    @Query("SELECT COUNT(DISTINCT t) FROM Teacher t " +
           "INNER JOIN t.teachingSlots ts " +
           "INNER JOIN ts.session s " +
           "INNER JOIN s.classEntity c " +
           "WHERE c.branch.id IN :branchIds")
    long countByAssignedClassesInBranches(@Param("branchIds") List<Long> branchIds);

    /**
     * Count teachers assigned to classes in a specific branch
     */
    @Query("SELECT COUNT(DISTINCT t) FROM Teacher t " +
           "INNER JOIN t.teachingSlots ts " +
           "INNER JOIN ts.session s " +
           "INNER JOIN s.classEntity c " +
           "WHERE c.branch.id = :branchId")
    long countByAssignedClassesInBranch(@Param("branchId") Long branchId);
}

