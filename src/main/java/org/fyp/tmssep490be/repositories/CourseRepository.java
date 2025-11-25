package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Course;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.CourseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findBySubjectId(Long subjectId);

    List<Course> findByLevelId(Long levelId);

    List<Course> findBySubjectIdAndLevelId(Long subjectId, Long levelId);

    // ==================== SCHEDULER JOB METHODS ====================

    /**
     * Find courses ready for activation on their effective date
     * Used by CourseActivationJob to activate DRAFT + APPROVED courses
     */
    @Query("SELECT c FROM Course c " +
           "WHERE c.effectiveDate <= :date " +
           "AND c.status = :status " +
           "AND c.approvalStatus = :approvalStatus " +
           "ORDER BY c.effectiveDate ASC")
    List<Course> findByEffectiveDateBeforeOrEqualAndStatusAndApprovalStatus(
        @Param("date") LocalDate date,
        @Param("status") CourseStatus status,
        @Param("approvalStatus") ApprovalStatus approvalStatus
    );
}
