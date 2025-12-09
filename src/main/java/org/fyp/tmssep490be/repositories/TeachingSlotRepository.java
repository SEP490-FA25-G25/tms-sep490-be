package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.TeachingSlot;
import org.fyp.tmssep490be.entities.enums.TeachingSlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeachingSlotRepository extends JpaRepository<TeachingSlot, TeachingSlot.TeachingSlotId> {

    //Tìm kiếm tất cả lớp học được phân công cho giáo viên theo teacherId
    @Query("""
        SELECT DISTINCT c FROM TeachingSlot ts
        JOIN ts.session s
        JOIN s.classEntity c
        JOIN FETCH c.subject
        JOIN FETCH c.branch
        WHERE ts.teacher.id = :teacherId
          AND ts.status IN ('SCHEDULED', 'SUBSTITUTED')
        ORDER BY c.code ASC
        """)
    List<org.fyp.tmssep490be.entities.ClassEntity> findDistinctClassesByTeacherId(
        @Param("teacherId") Long teacherId);

    @Query("SELECT ts FROM TeachingSlot ts WHERE ts.session.classEntity.id = :classId AND ts.status = :status")
    List<TeachingSlot> findByClassEntityIdAndStatus(@Param("classId") Long classId,
                                                    @Param("status") TeachingSlotStatus status);

    /**
     * Check teacher ownership on session (active slot)
     */
    boolean existsByIdSessionIdAndIdTeacherIdAndStatusIn(
        Long sessionId,
        Long teacherId,
        List<TeachingSlotStatus> statuses);

    /**
     * Find teaching slots by teacher and date (exclude cancelled sessions)
     */
    @Query("""
        SELECT ts FROM TeachingSlot ts
        JOIN FETCH ts.session s
        JOIN FETCH s.timeSlotTemplate tst
        JOIN FETCH s.classEntity c
        JOIN FETCH c.subject subj
        JOIN FETCH c.branch branch
        LEFT JOIN FETCH s.subjectSession ss
        WHERE ts.teacher.id = :teacherId
          AND ts.status IN ('SCHEDULED', 'SUBSTITUTED')
          AND s.date = :date
          AND s.status <> 'CANCELLED'
        ORDER BY tst.startTime ASC
        """)
    List<TeachingSlot> findByTeacherIdAndDate(
        @Param("teacherId") Long teacherId,
        @Param("date") java.time.LocalDate date);

    /**
     * Get teaching slots of a session with teacher/user loaded
     */
    @Query("""
        SELECT ts FROM TeachingSlot ts
        JOIN FETCH ts.teacher t
        JOIN FETCH t.userAccount ua
        WHERE ts.session.id = :sessionId
          AND ts.status IN ('SCHEDULED', 'SUBSTITUTED')
        """)
    List<TeachingSlot> findBySessionIdWithTeacher(@Param("sessionId") Long sessionId);
}
