package org.fyp.tmssep490be.repositories;

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

     // Tìm các sinh viên thuộc phase đã kết thúc để tạo bản ghi feedback tự động.
     // Returns Object[] with: [studentId, studentName, studentEmail, classId, classCode, className,
     //                         phaseId, phaseName, subjectName, lastSessionDate]
    @Query("""
        SELECT DISTINCT
            st.id,
            ua.fullName,
            ua.email,
            c.id,
            c.code,
            c.name,
            sp.id,
            sp.name,
            s.name,
            (
                SELECT MAX(sess2.date) FROM Session sess2
                JOIN sess2.subjectSession ss2
                WHERE sess2.classEntity.id = c.id AND ss2.phase.id = sp.id
            )
        FROM Session sess
        JOIN sess.subjectSession ss
        JOIN ss.phase sp
        JOIN sess.classEntity c
        JOIN c.subject s
        JOIN c.enrollments e
        JOIN e.student st
        JOIN st.userAccount ua
        WHERE e.status IN ('ENROLLED', 'COMPLETED')
          AND ss.sequenceNo = (
              SELECT MAX(ss3.sequenceNo) FROM SubjectSession ss3 WHERE ss3.phase.id = sp.id
          )
          AND sess.date = (
              SELECT MAX(sess2.date) FROM Session sess2
              JOIN sess2.subjectSession ss2
              WHERE sess2.classEntity.id = c.id AND ss2.phase.id = sp.id
          )
          AND sess.date <= :targetDate
          AND sess.status = 'DONE'
          AND NOT EXISTS (
              SELECT 1 FROM StudentFeedback sf
              WHERE sf.student.id = st.id AND sf.classEntity.id = c.id AND sf.phase.id = sp.id
          )
        """)
    List<Object[]> findFeedbackCreationCandidatesRawData(@Param("targetDate") LocalDate targetDate);

    @Query("""
        SELECT DISTINCT sf FROM StudentFeedback sf
        JOIN FETCH sf.classEntity c
        JOIN FETCH c.subject s
        LEFT JOIN FETCH sf.phase p
        WHERE sf.student.id = :studentId
          AND sf.isFeedback = false
        """)
    List<StudentFeedback> findPendingByStudentId(@Param("studentId") Long studentId);

    @Query("""
        SELECT COUNT(sf) FROM StudentFeedback sf
        WHERE sf.student.id = :studentId AND sf.isFeedback = false
        """)
    long countPendingByStudentId(@Param("studentId") Long studentId);

    @Query("""
        SELECT sf FROM StudentFeedback sf
        JOIN FETCH sf.classEntity c
        JOIN FETCH c.subject s
        LEFT JOIN FETCH sf.phase p
        LEFT JOIN FETCH sf.studentFeedbackResponses r
        LEFT JOIN FETCH r.question q
        WHERE sf.id = :feedbackId AND sf.student.id = :studentId
        """)
    Optional<StudentFeedback> findByIdForStudent(@Param("feedbackId") Long feedbackId, @Param("studentId") Long studentId);

    @Query("""
        SELECT DISTINCT sf FROM StudentFeedback sf
        JOIN FETCH sf.classEntity c
        JOIN FETCH c.subject s
        LEFT JOIN FETCH sf.phase p
        WHERE sf.student.id = :studentId
          AND (:isFeedback IS NULL OR sf.isFeedback = :isFeedback)
          AND (:classId IS NULL OR c.id = :classId)
          AND (:phaseId IS NULL OR sf.phase.id = :phaseId)
          AND (:search IS NULL OR :search = '' OR
               LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY sf.createdAt DESC
        """)
    List<StudentFeedback> findAllByStudentIdWithFilters(
        @Param("studentId") Long studentId,
        @Param("isFeedback") Boolean isFeedback,
        @Param("classId") Long classId,
        @Param("phaseId") Long phaseId,
        @Param("search") String search
    );

    @Query("""
        SELECT sf FROM StudentFeedback sf
        JOIN FETCH sf.student st
        JOIN FETCH st.userAccount ua
        LEFT JOIN FETCH sf.phase p
        WHERE sf.classEntity.id = :classId
          AND (:phaseId IS NULL OR sf.phase.id = :phaseId)
          AND (:isFeedback IS NULL OR sf.isFeedback = :isFeedback)
        """)
    Page<StudentFeedback> findByClassIdWithFilters(
        @Param("classId") Long classId,
        @Param("phaseId") Long phaseId,
        @Param("isFeedback") Boolean isFeedback,
        Pageable pageable
    );

    @Query("""
        SELECT sf FROM StudentFeedback sf
        JOIN FETCH sf.student st
        JOIN FETCH st.userAccount ua
        JOIN FETCH sf.classEntity c
        JOIN FETCH c.subject s
        LEFT JOIN FETCH sf.phase p
        LEFT JOIN FETCH sf.studentFeedbackResponses r
        LEFT JOIN FETCH r.question q
        WHERE sf.id = :feedbackId
        """)
    Optional<StudentFeedback> findByIdWithDetails(@Param("feedbackId") Long feedbackId);

    @Query("""
        SELECT COUNT(sf) FROM StudentFeedback sf
        WHERE sf.classEntity.id = :classId
          AND (:phaseId IS NULL OR sf.phase.id = :phaseId)
          AND sf.isFeedback = true
        """)
    long countSubmittedFeedbacksByClassIdAndPhase(
        @Param("classId") Long classId,
        @Param("phaseId") Long phaseId
    );

    @Query("""
        SELECT COUNT(DISTINCT e.student.id) FROM Enrollment e
        WHERE e.classEntity.id = :classId
          AND e.status IN ('ENROLLED', 'COMPLETED')
        """)
    long countActiveStudentsByClassId(@Param("classId") Long classId);

    @Query("""
        SELECT COUNT(sf) FROM StudentFeedback sf
        WHERE sf.classEntity.id = :classId
        """)
    long countTotalFeedbacksByClassId(@Param("classId") Long classId);
}
