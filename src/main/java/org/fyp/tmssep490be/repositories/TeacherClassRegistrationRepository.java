package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.TeacherClassRegistration;
import org.fyp.tmssep490be.entities.enums.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherClassRegistrationRepository extends JpaRepository<TeacherClassRegistration, Long> {

    // Tìm đăng ký theo teacher và class
    Optional<TeacherClassRegistration> findByTeacherIdAndClassEntityId(Long teacherId, Long classId);

    // Kiểm tra teacher đã đăng ký lớp chưa
    boolean existsByTeacherIdAndClassEntityId(Long teacherId, Long classId);

    // Lấy tất cả đăng ký của 1 lớp
    @Query("SELECT tcr FROM TeacherClassRegistration tcr " +
           "LEFT JOIN FETCH tcr.teacher t " +
           "LEFT JOIN FETCH t.userAccount ua " +
           "WHERE tcr.classEntity.id = :classId " +
           "ORDER BY tcr.registeredAt ASC")
    List<TeacherClassRegistration> findByClassEntityIdOrderByRegisteredAtAsc(@Param("classId") Long classId);

    // Lấy đăng ký của lớp theo status
    @Query("SELECT tcr FROM TeacherClassRegistration tcr " +
           "LEFT JOIN FETCH tcr.teacher t " +
           "LEFT JOIN FETCH t.userAccount ua " +
           "WHERE tcr.classEntity.id = :classId AND tcr.status = :status " +
           "ORDER BY tcr.registeredAt ASC")
    List<TeacherClassRegistration> findByClassEntityIdAndStatusOrderByRegisteredAtAsc(
            @Param("classId") Long classId,
            @Param("status") RegistrationStatus status);

    // Lấy tất cả đăng ký của 1 giáo viên
    @Query("SELECT tcr FROM TeacherClassRegistration tcr " +
           "LEFT JOIN FETCH tcr.classEntity c " +
           "LEFT JOIN FETCH c.subject s " +
           "LEFT JOIN FETCH c.branch b " +
           "WHERE tcr.teacher.id = :teacherId " +
           "ORDER BY tcr.registeredAt DESC")
    List<TeacherClassRegistration> findByTeacherIdOrderByRegisteredAtDesc(@Param("teacherId") Long teacherId);

    // Lấy đăng ký của giáo viên theo status
    List<TeacherClassRegistration> findByTeacherIdAndStatusOrderByRegisteredAtDesc(
            Long teacherId, RegistrationStatus status);

    // Đếm số đăng ký pending của 1 lớp
    long countByClassEntityIdAndStatus(Long classId, RegistrationStatus status);

    // Lấy tất cả đăng ký PENDING theo chi nhánh (cho AA)
    @Query("SELECT tcr FROM TeacherClassRegistration tcr " +
           "LEFT JOIN FETCH tcr.teacher t " +
           "LEFT JOIN FETCH t.userAccount ua " +
           "LEFT JOIN FETCH tcr.classEntity c " +
           "LEFT JOIN FETCH c.subject s " +
           "WHERE c.branch.id = :branchId AND tcr.status = 'PENDING' " +
           "ORDER BY c.registrationCloseDate ASC, tcr.registeredAt ASC")
    List<TeacherClassRegistration> findPendingRegistrationsByBranchId(@Param("branchId") Long branchId);

    // Lấy danh sách class có đăng ký pending (cho AA dashboard)
    @Query("SELECT DISTINCT tcr.classEntity.id FROM TeacherClassRegistration tcr " +
           "WHERE tcr.classEntity.branch.id = :branchId AND tcr.status = 'PENDING'")
    List<Long> findClassIdsWithPendingRegistrationsByBranchId(@Param("branchId") Long branchId);

    // Lấy đăng ký pending kèm thông tin teacher (cho AA review)
    @Query("SELECT tcr FROM TeacherClassRegistration tcr " +
           "LEFT JOIN FETCH tcr.teacher t " +
           "LEFT JOIN FETCH t.userAccount ua " +
           "LEFT JOIN FETCH t.teacherSkills ts " +
           "WHERE tcr.classEntity.id = :classId AND tcr.status = 'PENDING' " +
           "ORDER BY tcr.registeredAt ASC")
    List<TeacherClassRegistration> findPendingRegistrationsWithTeacherDetailsForClass(
            @Param("classId") Long classId);
}
