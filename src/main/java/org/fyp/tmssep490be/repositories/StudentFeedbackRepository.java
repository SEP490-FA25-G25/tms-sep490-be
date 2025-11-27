package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.StudentFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentFeedbackRepository extends JpaRepository<StudentFeedback, Long> {

    /**
     * Find all feedbacks for a specific session
     * Used for QA session detail functionality
     */
    @Query("SELECT sf FROM StudentFeedback sf " +
           "JOIN FETCH sf.student s " +
           "JOIN FETCH s.userAccount ua " +
           "JOIN sf.classEntity c " +
           "JOIN c.sessions sess " +
           "WHERE sess.id = :sessionId " +
           "ORDER BY s.userAccount.fullName")
    List<StudentFeedback> findBySessionIdWithDetails(@Param("sessionId") Long sessionId);

    /**
     * Count feedback submissions for a session
     */
    @Query("SELECT COUNT(sf) FROM StudentFeedback sf " +
           "JOIN sf.classEntity c " +
           "JOIN c.sessions sess " +
           "WHERE sess.id = :sessionId")
    long countBySessionId(@Param("sessionId") Long sessionId);

    // ============== SCHEDULER JOB METHODS ==============

    /**
     * Find completed classes with students who haven't provided feedback
     * Used by FeedbackCollectionReminderJob to send feedback reminders
     */
    @Query("""
        SELECT new org.fyp.tmssep490be.dtos.FeedbackReminderDTO(
            s.id, u.fullName, cp.id, cp.name, c_course.name, c.id, c.name
        )
        FROM CoursePhase cp
        JOIN cp.course c_course
        JOIN c_course.classes c
        JOIN c.enrollments e
        JOIN e.student s
        JOIN s.userAccount u
        LEFT JOIN StudentFeedback sf ON sf.student.id = s.id
                                  AND sf.phase.id = cp.id
                                  AND sf.classEntity.id = c.id
        WHERE c.status = 'COMPLETED'
        AND e.status = 'COMPLETED'
        AND (sf.id IS NULL OR sf.isFeedback = false)
        """)
    List<org.fyp.tmssep490be.dtos.FeedbackReminderDTO> findPendingFeedbackReminders();

    // ============== QA METHODS ==============

    @Query("SELECT sf FROM StudentFeedback sf " +
           "JOIN FETCH sf.student s " +
           "JOIN FETCH s.userAccount u " +
           "LEFT JOIN FETCH sf.phase p " +
           "WHERE sf.classEntity.id = :classId " +
           "AND (:phaseId IS NULL OR p.id = :phaseId) " +
           "AND (:isFeedback IS NULL OR sf.isFeedback = :isFeedback)")
    Page<StudentFeedback> findByClassIdWithFilters(
        @Param("classId") Long classId,
        @Param("phaseId") Long phaseId,
        @Param("isFeedback") Boolean isFeedback,
        Pageable pageable
    );

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.classEntity.id = :classId AND e.status = 'ACTIVE'")
    long countActiveStudentsByClassId(@Param("classId") Long classId);

    @Query("SELECT COUNT(sf) FROM StudentFeedback sf " +
           "WHERE sf.classEntity.id = :classId AND sf.isFeedback = true")
    long countSubmittedFeedbacksByClassId(@Param("classId") Long classId);

    @Query("SELECT sf FROM StudentFeedback sf " +
           "JOIN FETCH sf.student s " +
           "JOIN FETCH s.userAccount u " +
           "LEFT JOIN FETCH sf.phase p " +
           "LEFT JOIN FETCH sf.studentFeedbackResponses sfr " +
           "WHERE sf.id = :feedbackId")
    Optional<StudentFeedback> findByIdWithDetails(@Param("feedbackId") Long feedbackId);
}
