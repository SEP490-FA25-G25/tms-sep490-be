package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.StudentSession;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StudentSessionRepository extends JpaRepository<StudentSession, StudentSession.StudentSessionId> {

    List<StudentSession> findBySessionId(Long sessionId);

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
            "ORDER BY s.date ASC, tst.startTime ASC")
    List<StudentSession> findWeeklyScheduleByStudentId(
            @Param("studentId") Long studentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

}
