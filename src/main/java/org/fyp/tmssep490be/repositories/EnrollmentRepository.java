package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Enrollment;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    int countByClassIdAndStatus(Long classId, EnrollmentStatus status);

    // Kiểm tra xem student đã được enroll vào class chưa
    boolean existsByClassIdAndStudentIdAndStatus(Long classId, Long studentId, EnrollmentStatus status);

}
