package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.dtos.FeedbackReminderDTO;
import org.fyp.tmssep490be.dtos.StudentFeedbackCreationCandidateDTO;
import org.fyp.tmssep490be.entities.StudentFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentFeedbackRepository extends JpaRepository<StudentFeedback, Long> {

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
    List<FeedbackReminderDTO> findPendingFeedbackReminders();

    /**
     * Tìm các sinh viên thuộc phase đã kết thúc để tạo bản ghi feedback tự động.
     * Điều kiện:
     * - Buổi cuối của phase (sequence_no max) đã diễn ra (status != PLANNED) và không còn buổi phase trong tương lai
     * - Chỉ lấy enrollments còn hiệu lực (ENROLLED, COMPLETED)
     * - Chưa tồn tại student_feedback cho student-class-phase đó
     */
    @Query("""
        SELECT DISTINCT new org.fyp.tmssep490be.dtos.StudentFeedbackCreationCandidateDTO(
            st.id,
            ua.fullName,
            ua.email,
            c.id,
            c.code,
            c.name,
            cp.id,
            cp.name,
            course.name,
            (
                SELECT MAX(s2.date) FROM Session s2
                JOIN s2.courseSession cs2
                WHERE s2.classEntity.id = c.id AND cs2.phase.id = cp.id
            )
        )
        FROM Session s
        JOIN s.courseSession cs
        JOIN cs.phase cp
        JOIN s.classEntity c
        JOIN c.course course
        JOIN c.enrollments e
        JOIN e.student st
        JOIN st.userAccount ua
        WHERE e.status IN ('ENROLLED', 'COMPLETED')
          AND cs.sequenceNo = (
              SELECT MAX(cs3.sequenceNo) FROM CourseSession cs3 WHERE cs3.phase.id = cp.id
          )
          AND s.date = (
              SELECT MAX(s2.date) FROM Session s2
              JOIN s2.courseSession cs2
              WHERE s2.classEntity.id = c.id AND cs2.phase.id = cp.id
          )
          AND s.date <= :targetDate
          AND s.status = 'DONE'
          AND NOT EXISTS (
              SELECT 1 FROM StudentFeedback sf
              WHERE sf.student.id = st.id AND sf.classEntity.id = c.id AND sf.phase.id = cp.id
          )
        """)
    List<StudentFeedbackCreationCandidateDTO> findFeedbackCreationCandidates(@Param("targetDate") LocalDate targetDate);

    /**
     * Pending feedbacks for a specific student (student portal).
     */
    @Query("""
        SELECT DISTINCT sf FROM StudentFeedback sf
        JOIN FETCH sf.classEntity c
        JOIN FETCH c.course course
        LEFT JOIN FETCH sf.phase p
        WHERE sf.student.id = :studentId
          AND sf.isFeedback = false
        """)
    List<StudentFeedback> findPendingByStudentId(@Param("studentId") Long studentId);

    /**
     * Feedback detail for submission by student.
     */
    @Query("""
        SELECT sf FROM StudentFeedback sf
        JOIN FETCH sf.classEntity c
        JOIN FETCH c.course course
        LEFT JOIN FETCH sf.phase p
        LEFT JOIN FETCH sf.studentFeedbackResponses r
        LEFT JOIN FETCH r.question q
        WHERE sf.id = :feedbackId AND sf.student.id = :studentId
        """)
    Optional<StudentFeedback> findByIdAndStudentIdWithDetails(@Param("feedbackId") Long feedbackId,
                                                              @Param("studentId") Long studentId);

    @Query("SELECT COUNT(sf) FROM StudentFeedback sf WHERE sf.student.id = :studentId AND sf.isFeedback = false")
    long countPendingByStudentId(@Param("studentId") Long studentId);

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
           "WHERE sf.classEntity.id = :classId " +
           "AND (:phaseId IS NULL OR sf.phase.id = :phaseId) " +
           "AND sf.isFeedback = true")
    long countSubmittedFeedbacksByClassIdAndPhase(@Param("classId") Long classId,
                                                  @Param("phaseId") Long phaseId);

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
