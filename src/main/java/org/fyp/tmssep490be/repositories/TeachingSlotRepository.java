package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.TeachingSlot;
import org.fyp.tmssep490be.entities.enums.TeachingSlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TeachingSlotRepository extends JpaRepository<TeachingSlot, TeachingSlot.TeachingSlotId> {

    @Query("SELECT ts FROM TeachingSlot ts WHERE ts.session.classEntity.id = :classId AND ts.status = :status")
    List<TeachingSlot> findByClassEntityIdAndStatus(@Param("classId") Long classId, @Param("status") TeachingSlotStatus status);
    
    /**
     * Check if teacher owns (is assigned to) a session
     * Teacher owns session if there's a teaching_slot with status SCHEDULED or SUBSTITUTED
     */
    boolean existsByIdSessionIdAndIdTeacherIdAndStatusIn(
            Long sessionId,
            Long teacherId,
            List<TeachingSlotStatus> statuses
    );

    @Query("""
            SELECT ts FROM TeachingSlot ts
            JOIN FETCH ts.session s
            JOIN FETCH s.timeSlotTemplate tst
            JOIN FETCH s.classEntity c
            JOIN FETCH c.course course
            LEFT JOIN FETCH s.courseSession cs
            WHERE ts.teacher.id = :teacherId
              AND ts.status IN ('SCHEDULED', 'SUBSTITUTED')
              AND s.date = :date
              AND s.status <> 'CANCELLED'
            ORDER BY tst.startTime ASC
            """)
    List<TeachingSlot> findByTeacherIdAndDate(
            @Param("teacherId") Long teacherId,
            @Param("date") LocalDate date
    );

    /**
     * Find teacher's future sessions within date range
     * Returns sessions with status PLANNED, within 7 days from today (or specific date range)
     */
    @Query("""
            SELECT ts FROM TeachingSlot ts
            JOIN FETCH ts.session s
            JOIN FETCH s.timeSlotTemplate tst
            JOIN FETCH s.classEntity c
            JOIN FETCH c.course course
            LEFT JOIN FETCH s.courseSession cs
            WHERE ts.teacher.id = :teacherId
              AND ts.status = 'SCHEDULED'
              AND s.status = 'PLANNED'
              AND s.date >= :fromDate
              AND s.date <= :toDate
            ORDER BY s.date ASC, tst.startTime ASC
            """)
    List<TeachingSlot> findByTeacherIdAndDateRange(
            @Param("teacherId") Long teacherId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    /**
     * Find teaching slot by session ID with teacher loaded
     * Used to get teacher from session when request.teacher is null
     */
    @Query("""
            SELECT ts FROM TeachingSlot ts
            JOIN FETCH ts.teacher t
            JOIN FETCH t.userAccount ua
            WHERE ts.session.id = :sessionId
              AND ts.status IN ('SCHEDULED', 'SUBSTITUTED')
            """)
    List<TeachingSlot> findBySessionIdWithTeacher(@Param("sessionId") Long sessionId);

    /**
     * Find distinct classes that a teacher is teaching
     * Returns classes where teacher has at least one teaching slot with status SCHEDULED or SUBSTITUTED
     */
    @Query("""
            SELECT DISTINCT c FROM TeachingSlot ts
            JOIN ts.session s
            JOIN s.classEntity c
            JOIN FETCH c.course
            JOIN FETCH c.branch
            WHERE ts.teacher.id = :teacherId
              AND ts.status = 'SCHEDULED'
            ORDER BY c.code ASC
            """)
    List<org.fyp.tmssep490be.entities.ClassEntity> findDistinctClassesByTeacherId(@Param("teacherId") Long teacherId);

    /**
     * Find weekly schedule for a teacher with all related data
     * Uses JOIN FETCH to prevent N+1 queries
     * Includes sessions with status SCHEDULED or SUBSTITUTED
     * Includes all session statuses (PLANNED, DONE) but excludes CANCELLED
     */
    @Query("""
            SELECT ts FROM TeachingSlot ts
            JOIN FETCH ts.session s
            JOIN FETCH s.timeSlotTemplate tst
            JOIN FETCH s.classEntity c
            JOIN FETCH c.course course
            JOIN FETCH c.branch branch
            LEFT JOIN FETCH s.courseSession cs
            LEFT JOIN FETCH cs.courseMaterials
            LEFT JOIN FETCH s.sessionResources sr
            LEFT JOIN FETCH sr.resource
            WHERE ts.teacher.id = :teacherId
              AND ts.status IN ('SCHEDULED', 'SUBSTITUTED')
              AND s.date BETWEEN :startDate AND :endDate
              AND s.status <> 'CANCELLED'
            ORDER BY s.date ASC, tst.startTime ASC
            """)
    List<TeachingSlot> findWeeklyScheduleByTeacherId(
            @Param("teacherId") Long teacherId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find weekly schedule for a teacher filtered by specific class
     * Uses JOIN FETCH to prevent N+1 queries
     */
    @Query("""
            SELECT ts FROM TeachingSlot ts
            JOIN FETCH ts.session s
            JOIN FETCH s.timeSlotTemplate tst
            JOIN FETCH s.classEntity c
            JOIN FETCH c.course course
            JOIN FETCH c.branch branch
            LEFT JOIN FETCH s.courseSession cs
            LEFT JOIN FETCH cs.courseMaterials
            LEFT JOIN FETCH s.sessionResources sr
            LEFT JOIN FETCH sr.resource
            WHERE ts.teacher.id = :teacherId
              AND ts.status IN ('SCHEDULED', 'SUBSTITUTED')
              AND s.classEntity.id = :classId
              AND s.date BETWEEN :startDate AND :endDate
              AND s.status <> 'CANCELLED'
            ORDER BY s.date ASC, tst.startTime ASC
            """)
    List<TeachingSlot> findWeeklyScheduleByTeacherIdAndClassId(
            @Param("teacherId") Long teacherId,
            @Param("classId") Long classId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find teaching slot by teacher ID and session ID
     * Used for authorization check and detail retrieval
     */
    @Query("""
            SELECT ts FROM TeachingSlot ts
            JOIN FETCH ts.session s
            JOIN FETCH s.timeSlotTemplate tst
            JOIN FETCH s.classEntity c
            JOIN FETCH c.course course
            JOIN FETCH c.branch branch
            LEFT JOIN FETCH s.courseSession cs
            LEFT JOIN FETCH cs.courseMaterials
            LEFT JOIN FETCH s.sessionResources sr
            LEFT JOIN FETCH sr.resource
            WHERE ts.teacher.id = :teacherId
              AND ts.session.id = :sessionId
              AND ts.status IN ('SCHEDULED', 'SUBSTITUTED')
            """)
    java.util.Optional<TeachingSlot> findByTeacherIdAndSessionId(
            @Param("teacherId") Long teacherId,
            @Param("sessionId") Long sessionId
    );
}
