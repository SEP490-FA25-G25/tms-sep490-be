package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Student;
import org.fyp.tmssep490be.entities.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    /**
     * Find student by student_code
     */
    Optional<Student> findByStudentCode(String studentCode);

    /**
     * Find student by user_id
     */
    Optional<Student> findByUserAccountId(Long userId);

    /**
     * Find students in specific branches with search functionality
     * For Academic Affairs staff to view students in their accessible branches
     */
    @Query("SELECT s FROM Student s " +
           "INNER JOIN s.userAccount u " +
           "INNER JOIN u.userBranches ub " +
           "INNER JOIN ub.branch b " +
           "WHERE (:branchIds IS NULL OR b.id IN :branchIds) " +
           "AND u.status = org.fyp.tmssep490be.entities.enums.UserStatus.ACTIVE " +
           "AND (COALESCE(:search, '') = '' OR " +
           "  LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(COALESCE(u.phone, '')) LIKE LOWER(CONCAT('%', :search, '%'))" +
           ")")
    Page<Student> findStudentsInBranchesWithSearch(
            @Param("branchIds") List<Long> branchIds,
            @Param("search") String search,
            Pageable pageable
    );

    /**
     * Find available students for enrollment in a class
     * Excludes students already enrolled, includes skill assessment for smart sorting
     * Returns students from the same branch as the class
     * 
     * Note: Using COALESCE to avoid Hibernate bytea type inference bug with NULL + OFFSET
     */
    @Query("SELECT DISTINCT s FROM Student s " +
           "INNER JOIN s.userAccount u " +
           "INNER JOIN u.userBranches ub " +
           "INNER JOIN ub.branch b " +
           "WHERE b.id = :branchId " +
           "AND u.status = org.fyp.tmssep490be.entities.enums.UserStatus.ACTIVE " +
           "AND NOT EXISTS (" +
           "  SELECT 1 FROM Enrollment e " +
           "  WHERE e.student.id = s.id " +
           "  AND e.classId = :classId " +
           "  AND e.status = org.fyp.tmssep490be.entities.enums.EnrollmentStatus.ENROLLED" +
           ") " +
           "AND (COALESCE(:search, '') = '' OR " +
           "  LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(COALESCE(u.phone, '')) LIKE LOWER(CONCAT('%', :search, '%'))" +
           ")")
    Page<Student> findAvailableStudentsForClass(
            @Param("classId") Long classId,
            @Param("branchId") Long branchId,
            @Param("search") String search,
            Pageable pageable
    );

    /**
     * Find students who have enrolled in a specific course
     */
    @Query("SELECT DISTINCT s FROM Student s " +
           "INNER JOIN s.enrollments e " +
           "INNER JOIN e.classEntity c " +
           "INNER JOIN c.course co " +
           "WHERE co.id = :courseId " +
           "AND (:branchIds IS NULL OR c.branch.id IN :branchIds)")
    Page<Student> findStudentsByCourse(
            @Param("courseId") Long courseId,
            @Param("branchIds") List<Long> branchIds,
            Pageable pageable
    );

    /**
     * Find available students for enrollment with computed match priority
     * Database-first approach: computes matchPriority in SQL for consistent pagination
     *
     * @param classId Class ID for enrollment
     * @param classSubjectId Subject ID of the class's course level
     * @param classLevelId Level ID of the class's course
     * @param branchId Branch ID to filter students from
     * @param search Search term for student details
     * @param pageable Pagination and sorting parameters
     * @return Page of students with computed match priority
     */
    /**
     * Find all available students for enrollment in a class (no pagination)
     * Hybrid approach: fetch all students for in-memory sorting
     */
    @Query("SELECT s FROM Student s " +
           "INNER JOIN s.userAccount u " +
           "INNER JOIN u.userBranches ub " +
           "INNER JOIN ub.branch b " +
           "WHERE b.id = :branchId " +
           "AND u.status = org.fyp.tmssep490be.entities.enums.UserStatus.ACTIVE " +
           "AND NOT EXISTS (" +
           "  SELECT 1 FROM Enrollment e " +
           "  WHERE e.student.id = s.id " +
           "  AND e.classId = :classId " +
           "  AND e.status = org.fyp.tmssep490be.entities.enums.EnrollmentStatus.ENROLLED" +
           ") " +
           "AND (COALESCE(:search, '') = '' OR " +
           "  LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(COALESCE(u.phone, '')) LIKE LOWER(CONCAT('%', :search, '%'))" +
           ")")
    List<Student> findAllAvailableStudentsForClass(
            @Param("classId") Long classId,
            @Param("branchId") Long branchId,
            @Param("search") String search
    );

    // ==================== ANALYTICS METHODS ====================

    /**
     * Count students by branch ID
     */
    @Query("SELECT COUNT(DISTINCT s) FROM Student s " +
           "INNER JOIN s.userAccount u " +
           "INNER JOIN u.userBranches ub " +
           "WHERE ub.branch.id = :branchId")
    long countByBranchId(@Param("branchId") Long branchId);

    /**
     * Count students in multiple branches
     */
    @Query("SELECT COUNT(DISTINCT s) FROM Student s " +
           "INNER JOIN s.userAccount u " +
           "INNER JOIN u.userBranches ub " +
           "WHERE ub.branch.id IN :branchIds")
    long countByBranchIdIn(@Param("branchIds") List<Long> branchIds);
}
