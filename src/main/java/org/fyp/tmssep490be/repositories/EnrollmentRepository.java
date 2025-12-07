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

    //Find enrolled students for a class with search and pagination
    @Query("""
        SELECT e FROM Enrollment e
        INNER JOIN FETCH e.student s
        INNER JOIN FETCH s.userAccount u
        WHERE e.classId = :classId
        AND e.status = :status
        AND (:search IS NULL OR :search = '' OR
          UPPER(s.studentCode) LIKE UPPER(CONCAT('%', :search, '%')) OR
          UPPER(u.fullName) LIKE UPPER(CONCAT('%', :search, '%')) OR
          UPPER(u.email) LIKE UPPER(CONCAT('%', :search, '%')) OR
          UPPER(u.phone) LIKE UPPER(CONCAT('%', :search, '%'))
        )
        ORDER BY e.enrolledAt DESC
        """)
    Page<Enrollment> findEnrolledStudentsByClass(
            @Param("classId") Long classId,
            @Param("status") EnrollmentStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    //Find all enrollments for a class with specific status
    List<Enrollment> findByClassIdAndStatus(Long classId, EnrollmentStatus status);
}

