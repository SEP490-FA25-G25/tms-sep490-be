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

    //Tìm kiếm sinh viên đăng ký lớp học theo ID lớp học với tìm kiếm và phân trang
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

    //Tìm kiếm tất cả đăng ký lớp học theo ID lớp học và trạng thái
    List<Enrollment> findByClassIdAndStatus(Long classId, EnrollmentStatus status);
}

