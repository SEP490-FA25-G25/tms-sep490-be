package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Teacher;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

        // Tìm kiếm giáo viên theo ID user account
        Optional<Teacher> findByUserAccountId(Long userAccountId);

        // Tìm tất cả giáo viên ACTIVE trong các branch được chỉ định
        @Query("SELECT DISTINCT t FROM Teacher t " +
                        "JOIN t.userAccount u " +
                        "JOIN u.userBranches ub " +
                        "LEFT JOIN FETCH t.assignedClasses " +
                        "WHERE ub.branch.id IN :branchIds " +
                        "AND u.status = org.fyp.tmssep490be.entities.enums.UserStatus.ACTIVE")
        List<Teacher> findByBranchIds(@Param("branchIds") List<Long> branchIds);

        // Đếm số lớp đang được gán cho giáo viên theo status
        @Query("SELECT COUNT(c) FROM ClassEntity c " +
                        "WHERE c.assignedTeacher.id = :teacherId " +
                        "AND c.status IN :statuses")
        int countAssignedClassesByTeacherIdAndStatuses(
                        @Param("teacherId") Long teacherId,
                        @Param("statuses") List<ClassStatus> statuses);
}
