package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.StudentSession;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentSessionRepository extends JpaRepository<StudentSession, StudentSession.StudentSessionId> {

    /**
     * Count số học viên trong một session
     * Bao gồm cả học viên học bù và học viên tham gia buổi đó
     */
    @Query("SELECT COUNT(ss) FROM StudentSession ss WHERE ss.session.id = :sessionId")
    long countBySessionId(@Param("sessionId") Long sessionId);

    /**
     * Find weekly schedule for a student with all related data
     * Uses JOIN FETCH to prevent N+1 queries
     */
    @Query("SELECT ss FROM StudentSession ss " +
           "JOIN FETCH ss.session s " +
           "JOIN FETCH s.timeSlotTemplate tst " +
           "JOIN FETCH s.classEntity c " +
           "JOIN FETCH c.course course " +
           "JOIN FETCH c.branch branch " +
           "JOIN FETCH s.courseSession cs " +
           "LEFT JOIN FETCH cs.courseMaterials " +
           "LEFT JOIN FETCH s.sessionResources sr " +
           "LEFT JOIN FETCH sr.resource " +
           "WHERE ss.student.id = :studentId " +
           "AND s.date BETWEEN :startDate AND :endDate " +
           "AND (ss.isTransferredOut IS NULL OR ss.isTransferredOut = false) " +
           "ORDER BY s.date ASC, tst.startTime ASC")
    List<StudentSession> findWeeklyScheduleByStudentId(
            @Param("studentId") Long studentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find weekly schedule for a student filtered by specific class
     * Uses JOIN FETCH to prevent N+1 queries
     */
    @Query("SELECT ss FROM StudentSession ss " +
           "JOIN FETCH ss.session s " +
           "JOIN FETCH s.timeSlotTemplate tst " +
           "JOIN FETCH s.classEntity c " +
           "JOIN FETCH c.course course " +
           "JOIN FETCH c.branch branch " +
           "JOIN FETCH s.courseSession cs " +
           "LEFT JOIN FETCH cs.courseMaterials " +
           "LEFT JOIN FETCH s.sessionResources sr " +
           "LEFT JOIN FETCH sr.resource " +
           "WHERE ss.student.id = :studentId " +
           "AND s.classEntity.id = :classId " +
           "AND s.date BETWEEN :startDate AND :endDate " +
           "AND (ss.isTransferredOut IS NULL OR ss.isTransferredOut = false) " +
           "ORDER BY s.date ASC, tst.startTime ASC")
    List<StudentSession> findWeeklyScheduleByStudentIdAndClassId(
            @Param("studentId") Long studentId,
            @Param("classId") Long classId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find specific session for a student with authorization check
     * Ensures student can only access their own sessions
     */
    @Query("SELECT ss FROM StudentSession ss " +
           "JOIN FETCH ss.session s " +
           "JOIN FETCH s.timeSlotTemplate tst " +
           "JOIN FETCH s.classEntity c " +
           "JOIN FETCH c.course course " +
           "JOIN FETCH c.branch branch " +
           "JOIN FETCH s.courseSession cs " +
           "LEFT JOIN FETCH cs.courseMaterials " +
           "LEFT JOIN FETCH s.sessionResources sr " +
           "LEFT JOIN FETCH sr.resource " +
           "LEFT JOIN FETCH s.teachingSlots ts " +
           "LEFT JOIN FETCH ts.teacher " +
           "WHERE ss.student.id = :studentId " +
           "AND ss.session.id = :sessionId")
    Optional<StudentSession> findByStudentIdAndSessionId(
            @Param("studentId") Long studentId,
            @Param("sessionId") Long sessionId
    );

    /**
     * Check if student is enrolled in a specific session
     */
    boolean existsByStudentIdAndSessionId(Long studentId, Long sessionId);

    /**
     * Find all student sessions for a specific class (used for absence rate calculation)
     */
    @Query("SELECT ss FROM StudentSession ss " +
           "JOIN ss.session s " +
           "WHERE ss.student.id = :studentId " +
           "AND s.classEntity.id = :classId")
    List<StudentSession> findByStudentIdAndClassEntityId(@Param("studentId") Long studentId, @Param("classId") Long classId);

    /**
     * Find all student sessions for a student (used for overview aggregation)
     */
    @Query("SELECT ss FROM StudentSession ss JOIN ss.session s WHERE ss.student.id = :studentId")
    List<StudentSession> findAllByStudentId(@Param("studentId") Long studentId);

    @Query("""
            SELECT ss FROM StudentSession ss
            JOIN FETCH ss.student st
            JOIN FETCH st.userAccount ua
            JOIN FETCH ss.session sess
            LEFT JOIN FETCH sess.courseSession cs
            WHERE ss.session.id = :sessionId
            ORDER BY st.studentCode ASC
            """)
    List<StudentSession> findBySessionId(@Param("sessionId") Long sessionId);

    @Query("""
            SELECT ss FROM StudentSession ss
            JOIN FETCH ss.student st
            JOIN FETCH st.userAccount ua
            JOIN FETCH ss.session sess
            WHERE ss.session.id IN :sessionIds
            """)
    List<StudentSession> findBySessionIds(@Param("sessionIds") List<Long> sessionIds);
    @Query("SELECT ss FROM StudentSession ss " +
           "JOIN ss.session s " +
           "WHERE ss.student.id = :studentId " +
           "AND s.classEntity.id = :classId " +
           "AND s.date > :date")
    List<StudentSession> findByStudentIdAndClassEntityIdAndSessionDateAfter(
            @Param("studentId") Long studentId,
            @Param("classId") Long classId,
            @Param("date") LocalDate date
    );

    /**
     * Find sessions for a student on a specific date (for conflict checking)
     */
    @Query("SELECT s FROM Session s " +
           "JOIN StudentSession ss ON s.id = ss.session.id " +
           "WHERE ss.student.id = :studentId " +
           "AND s.date = :date")
    List<org.fyp.tmssep490be.entities.Session> findSessionsForStudentByDate(
            @Param("studentId") Long studentId,
            @Param("date") LocalDate date
    );

    /**
     * Find student session by student ID and course session
     * Note: StudentSession links to Session, which links to CourseSession
     */
    @Query("SELECT ss FROM StudentSession ss " +
           "JOIN ss.session s " +
           "WHERE ss.student.id = :studentId " +
           "AND s.courseSession.id = :courseSessionId")
    Optional<StudentSession> findByStudentIdAndCourseSessionId(
            @Param("studentId") Long studentId,
            @Param("courseSessionId") Long courseSessionId
    );

    /**
     * Find all student sessions for a student in a specific class
     * Used for enrollment-based progress tracking
     */
    @Query("SELECT ss FROM StudentSession ss " +
           "JOIN ss.session s " +
           "WHERE ss.student.id = :studentId " +
           "AND s.classEntity.id = :classId")
    List<StudentSession> findByStudentIdAndClassId(
            @Param("studentId") Long studentId,
            @Param("classId") Long classId
    );

    /**
     * Count students by session ID and attendance status
     * Used for teacher schedule attendance summary
     */
    @Query("SELECT COUNT(ss) FROM StudentSession ss WHERE ss.session.id = :sessionId AND ss.attendanceStatus = :attendanceStatus")
    long countBySessionIdAndAttendanceStatus(
            @Param("sessionId") Long sessionId,
            @Param("attendanceStatus") AttendanceStatus attendanceStatus
    );

    /**
     * Check if any student session for a session has isMakeup = true
     * Used to determine if a session is a makeup session
     */
    boolean existsBySessionIdAndIsMakeup(Long sessionId, Boolean isMakeup);
}
