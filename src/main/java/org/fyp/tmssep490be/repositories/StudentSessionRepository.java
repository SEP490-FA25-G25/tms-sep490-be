package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.StudentSession;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

}
