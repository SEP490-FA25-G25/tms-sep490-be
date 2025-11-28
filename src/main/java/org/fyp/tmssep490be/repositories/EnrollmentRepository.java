package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Enrollment;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    
    /**
     * Count enrollments created between two dates
     */
    long countByEnrolledAtBetween(OffsetDateTime start, OffsetDateTime end);
    
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

    /**
     * Find enrollment by student and course and status
     */
    @Query("SELECT e FROM Enrollment e " +
           "WHERE e.studentId = :studentId " +
           "AND e.classEntity.course.id = :courseId " +
           "AND e.status = :status")
    Enrollment findByStudentIdAndCourseIdAndStatus(
            @Param("studentId") Long studentId,
            @Param("courseId") Long courseId,
            @Param("status") EnrollmentStatus status
    );

    /**
     * Find enrollments by student ID and status
     */
    List<Enrollment> findByStudentIdAndStatus(Long studentId, EnrollmentStatus status);

    /**
     * Find all enrollments by student ID with eager fetch of Class and Course
     * Excludes CANCELLED enrollments to show only active/historical classes
     */
    @Query("""
        SELECT e FROM Enrollment e
        JOIN FETCH e.classEntity c
        JOIN FETCH c.course
        WHERE e.studentId = :studentId
        AND e.status != 'CANCELLED'
        """)
    List<Enrollment> findByStudentIdWithClassAndCourse(@Param("studentId") Long studentId);

    // ============== SCHEDULER JOB METHODS ==============

    /**
     * Find enrollments where class is completed but enrollment status is still ENROLLED
     * Used by EnrollmentStatusUpdateJob to auto-complete enrollments
     */
    @Query("SELECT e FROM Enrollment e JOIN e.classEntity c WHERE c.status = :classStatus AND e.status = :enrollmentStatus")
    List<Enrollment> findByClassEntityStatusAndEnrollmentStatus(
        @Param("classStatus") String classStatus,
        @Param("enrollmentStatus") String enrollmentStatus
    );

    /**
     * Generate weekly attendance report by class
     * Used by WeeklyAttendanceReportJob for class-level attendance statistics
     */
    @Query("""
        SELECT new org.fyp.tmssep490be.dtos.WeeklyAttendanceReportDTO(
            c.id, c.name, COUNT(ss.id),
            COUNT(CASE WHEN ss.attendanceStatus = 'PRESENT' THEN 1 END),
            COUNT(CASE WHEN ss.attendanceStatus = 'ABSENT' THEN 1 END),
            ROUND(COUNT(CASE WHEN ss.attendanceStatus = 'PRESENT' THEN 1 END) * 100.0 / NULLIF(COUNT(ss.id), 0), 2)
        )
        FROM ClassEntity c
        JOIN c.sessions s
        JOIN s.studentSessions ss
        WHERE s.date BETWEEN :weekStart AND :weekEnd
        AND s.status != 'CANCELLED'
        GROUP BY c.id, c.name
        """)
    List<org.fyp.tmssep490be.dtos.WeeklyAttendanceReportDTO> generateWeeklyAttendanceReport(
        @Param("weekStart") java.time.LocalDate weekStart,
        @Param("weekEnd") java.time.LocalDate weekEnd
    );

    /**
     * Find students with low attendance rate below threshold
     * Used by WeeklyAttendanceReportJob to identify students needing attention
     */
    @Query("""
        SELECT new org.fyp.tmssep490be.dtos.StudentAttendanceAlertDTO(
            u.id, u.fullName, u.email, c.name,
            COUNT(CASE WHEN ss.attendanceStatus = 'PRESENT' THEN 1 END),
            COUNT(ss.id),
            ROUND(COUNT(CASE WHEN ss.attendanceStatus = 'PRESENT' THEN 1 END) * 100.0 / COUNT(ss.id), 2)
        )
        FROM UserAccount u
        JOIN u.student s
        JOIN s.enrollments e
        JOIN e.classEntity c
        JOIN c.sessions sess
        JOIN sess.studentSessions ss
        WHERE sess.date BETWEEN :weekStart AND :weekEnd
        AND sess.status != 'CANCELLED'
        AND e.status = 'ENROLLED'
        GROUP BY u.id, u.fullName, u.email, c.id, c.name
        HAVING COUNT(CASE WHEN ss.attendanceStatus = 'PRESENT' THEN 1 END) * 100.0 / COUNT(ss.id) < :threshold
        """)
    List<org.fyp.tmssep490be.dtos.StudentAttendanceAlertDTO> findStudentsWithLowAttendance(
        @Param("weekStart") java.time.LocalDate weekStart,
        @Param("weekEnd") java.time.LocalDate weekEnd,
        @Param("threshold") Double threshold
    );

    /**
     * Count enrollments by date range in specific branches
     */
    @Query("SELECT COUNT(e) FROM Enrollment e " +
           "INNER JOIN e.classEntity c " +
           "WHERE e.enrolledAt BETWEEN :start AND :end " +
           "AND c.branch.id IN :branchIds")
    long countByEnrolledAtBetweenAndBranchIdIn(
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end,
            @Param("branchIds") List<Long> branchIds);

    /**
     * Count enrollments in specific branches
     */
    @Query("SELECT COUNT(e) FROM Enrollment e " +
           "INNER JOIN e.classEntity c " +
           "WHERE c.branch.id IN :branchIds")
    long countByBranchIdIn(@Param("branchIds") List<Long> branchIds);

    /**
     * Calculate average enrollment rate for branches
     */
    @Query("SELECT COALESCE(AVG(CAST(e.classEntity.maxCapacity AS DOUBLE) / NULLIF(e.classEntity.maxCapacity, 0)), 0.0) " +
           "FROM Enrollment e " +
           "INNER JOIN e.classEntity c " +
           "WHERE c.branch.id IN :branchIds " +
           "AND e.status = org.fyp.tmssep490be.entities.enums.EnrollmentStatus.ENROLLED")
    double calculateAverageEnrollmentRateForBranches(@Param("branchIds") List<Long> branchIds);
}
