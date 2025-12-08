package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Student;
import org.fyp.tmssep490be.entities.enums.Gender;
import org.fyp.tmssep490be.entities.enums.UserStatus;
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

    Optional<Student> findByStudentCode(String studentCode);

    Optional<Student> findByUserAccountId(Long userId);

    /**
     * Find all available students for enrollment in a class
     * Students must: be from same branch, ACTIVE status, not already enrolled
     */
    @Query("SELECT s FROM Student s " +
           "INNER JOIN s.userAccount u " +
           "INNER JOIN u.userBranches ub " +
           "WHERE ub.branch.id = :branchId " +
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

    /**
     * Find students with filters for AA student search (for on-behalf request creation)
     * Filters by branch access, search term, user status, and gender
     */
    @Query("SELECT DISTINCT s FROM Student s " +
           "INNER JOIN s.userAccount u " +
           "INNER JOIN u.userBranches ub " +
           "WHERE ub.branch.id IN :branchIds " +
           "AND (:status IS NULL OR u.status = :status) " +
           "AND (:gender IS NULL OR u.gender = :gender) " +
           "AND (COALESCE(:search, '') = '' OR " +
           "  LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(COALESCE(u.phone, '')) LIKE LOWER(CONCAT('%', :search, '%'))" +
           ")")
    Page<Student> findStudentsWithFilters(
            @Param("branchIds") List<Long> branchIds,
            @Param("search") String search,
            @Param("status") UserStatus status,
            @Param("gender") Gender gender,
            Pageable pageable
    );

}