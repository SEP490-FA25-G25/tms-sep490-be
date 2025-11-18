package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Enrollment;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    /**
     * Count enrolled students của một class với status cụ thể
     */
    int countByClassIdAndStatus(Long classId, EnrollmentStatus status);

    /**
     * Check xem student đã enrolled vào class chưa
     */
    boolean existsByClassIdAndStudentIdAndStatus(Long classId, Long studentId, EnrollmentStatus status);

    /**
     * Find enrolled students for a class with search and pagination
     * Note: Search parameter should include wildcards (e.g., "%search%")
     */
    @Query("SELECT e FROM Enrollment e " +
           "INNER JOIN e.student s " +
           "INNER JOIN s.userAccount u " +
           "WHERE e.classId = :classId " +
           "AND e.status = :status " +
           "AND (:search IS NULL OR :search = '' OR " +
           "  UPPER(s.studentCode) LIKE UPPER(:search) OR " +
           "  UPPER(u.fullName) LIKE UPPER(:search) OR " +
           "  UPPER(u.email) LIKE UPPER(:search) OR " +
           "  UPPER(u.phone) LIKE UPPER(:search)" +
           ")")
    Page<Enrollment> findEnrolledStudentsByClass(
            @Param("classId") Long classId,
            @Param("status") EnrollmentStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    /**
     * Get student enrollment history with pagination
     */
    @Query("SELECT e FROM Enrollment e " +
           "INNER JOIN e.student s " +
           "INNER JOIN s.userAccount u " +
           "INNER JOIN e.classEntity c " +
           "INNER JOIN c.branch b " +
           "INNER JOIN c.course co " +
           "WHERE e.studentId = :studentId " +
           "AND (:branchIds IS NULL OR b.id IN :branchIds) " +
           "ORDER BY e.enrolledAt DESC")
    Page<Enrollment> findStudentEnrollmentHistory(
            @Param("studentId") Long studentId,
            @Param("branchIds") List<Long> branchIds,
            Pageable pageable
    );

    /**
     * Count active enrollments for a student
     */
    int countByStudentIdAndStatus(Long studentId, EnrollmentStatus status);

    /**
     * Find latest enrollment for a student
     */
    @Query("SELECT e FROM Enrollment e " +
           "WHERE e.studentId = :studentId " +
           "ORDER BY e.enrolledAt DESC " +
           "LIMIT 1")
    Enrollment findLatestEnrollmentByStudent(@Param("studentId") Long studentId);

    /**
     * Find specific enrollment for validation
     */
    Enrollment findByStudentIdAndClassIdAndStatus(Long studentId, Long classId, EnrollmentStatus status);

    List<Enrollment> findByClassIdAndStatus(Long classId, EnrollmentStatus status);
    /**
     * Find enrollments by student ID and status list
     */
    List<Enrollment> findByStudentIdAndStatusIn(Long studentId, List<EnrollmentStatus> statuses);

    /**
     * Find enrollments by class ID and status list
     */
    List<Enrollment> findByClassIdAndStatusIn(Long classId, List<EnrollmentStatus> statuses);

    /**
     * Check if student is enrolled in class with specified statuses
     */
    boolean existsByStudentIdAndClassIdAndStatusIn(Long studentId, Long classId, List<EnrollmentStatus> statuses);

    /**
     * Find enrollment by student and class (without status filter)
     */
    Enrollment findByStudentIdAndClassId(Long studentId, Long classId);
}
