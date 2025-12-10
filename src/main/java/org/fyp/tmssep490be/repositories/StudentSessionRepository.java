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

    List<StudentSession> findBySessionId(Long sessionId);

    @Query("SELECT DISTINCT ss FROM StudentSession ss " +
            "JOIN FETCH ss.student student " +
            "JOIN FETCH student.userAccount " +
            "JOIN FETCH ss.session s " +
            "LEFT JOIN FETCH s.timeSlotTemplate tst " +
            "JOIN FETCH s.classEntity c " +
            "JOIN FETCH c.subject subj " +
            "JOIN FETCH c.branch branch " +
            "LEFT JOIN FETCH s.subjectSession subjSess " +
            "LEFT JOIN FETCH subjSess.subjectMaterials " +
            "LEFT JOIN FETCH s.sessionResources sr " +
            "LEFT JOIN FETCH sr.resource " +
            "LEFT JOIN FETCH s.teachingSlots ts " +
            "LEFT JOIN FETCH ts.teacher teacher " +
            "LEFT JOIN FETCH teacher.userAccount " +
            "LEFT JOIN FETCH ss.makeupSession " +
            "LEFT JOIN FETCH ss.originalSession origSess " +
            "LEFT JOIN FETCH origSess.timeSlotTemplate " +
            "WHERE ss.student.id = :studentId " +
            "AND ss.session.id = :sessionId")
    Optional<StudentSession> findByStudentIdAndSessionId(
            @Param("studentId") Long studentId,
            @Param("sessionId") Long sessionId
    );

    @Query("SELECT ss.attendanceStatus, COUNT(ss) FROM StudentSession ss " +
           "JOIN ss.session s " +
           "WHERE ss.student.id = :studentId " +
           "AND s.classEntity.id = :classId " +
           "GROUP BY ss.attendanceStatus")
    List<Object[]> countAttendanceByStatusForStudentInClass(
            @Param("studentId") Long studentId,
            @Param("classId") Long classId
    );

    @Query("SELECT COUNT(ss) FROM StudentSession ss " +
           "JOIN ss.session s " +
           "WHERE ss.student.id = :studentId " +
           "AND s.classEntity.id = :classId")
    Long countTotalSessionsForStudentInClass(
            @Param("studentId") Long studentId,
            @Param("classId") Long classId
    );

    @Query("SELECT DISTINCT ss FROM StudentSession ss " +
            "JOIN FETCH ss.session s " +
            "LEFT JOIN FETCH s.timeSlotTemplate tst " +
            "JOIN FETCH s.classEntity c " +
            "JOIN FETCH c.subject subj " +
            "JOIN FETCH c.branch branch " +
            "LEFT JOIN FETCH s.subjectSession ss2 " +
            "LEFT JOIN FETCH ss2.subjectMaterials " +
            "LEFT JOIN FETCH s.sessionResources sr " +
            "LEFT JOIN FETCH sr.resource " +
            "WHERE ss.student.id = :studentId " +
            "AND s.date BETWEEN :startDate AND :endDate " +
            "AND s.status != 'CANCELLED' " +
            "AND (ss.isTransferredOut IS NULL OR ss.isTransferredOut = false) " +
            "AND (:classId IS NULL OR s.classEntity.id = :classId) " +
            "ORDER BY s.date ASC, tst.startTime ASC")
    List<StudentSession> findWeeklyScheduleByStudentId(
            @Param("studentId") Long studentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("classId") Long classId
    );

    // Find all student sessions by student and class for absence rate calculation
    @Query("SELECT ss FROM StudentSession ss " +
           "JOIN ss.session s " +
           "WHERE ss.student.id = :studentId " +
           "AND s.classEntity.id = :classId")
    List<StudentSession> findByStudentIdAndClassEntityId(
            @Param("studentId") Long studentId,
            @Param("classId") Long classId
    );

    @Query("SELECT COUNT(ss) FROM StudentSession ss " +
           "WHERE ss.session.id = :sessionId " +
           "AND ss.attendanceStatus != 'CANCELLED'")
    Long countBySessionId(@Param("sessionId") Long sessionId);

    @Query("SELECT ss FROM StudentSession ss " +
           "JOIN FETCH ss.session s " +
           "JOIN FETCH s.classEntity c " +
           "LEFT JOIN FETCH s.subjectSession " +
           "LEFT JOIN FETCH s.timeSlotTemplate " +
           "WHERE ss.student.id = :studentId " +
           "AND s.status != 'CANCELLED'")
    List<StudentSession> findAllByStudentId(@Param("studentId") Long studentId);

    // Check if student attended a specific session
    @Query("SELECT CASE WHEN COUNT(ss) > 0 THEN true ELSE false END " +
           "FROM StudentSession ss " +
           "WHERE ss.student.id = :studentId " +
           "AND ss.session.id = :sessionId " +
           "AND ss.attendanceStatus = :status")
    boolean existsByStudentIdAndSessionIdAndStatus(
            @Param("studentId") Long studentId,
            @Param("sessionId") Long sessionId,
            @Param("status") AttendanceStatus status
    );

    @Query("""
            SELECT ss FROM StudentSession ss
            JOIN FETCH ss.student st
            JOIN FETCH st.userAccount ua
            JOIN FETCH ss.session sess
            WHERE ss.session.id IN :sessionIds
            """)
    List<StudentSession> findBySessionIds(@Param("sessionIds") List<Long> sessionIds);

}
