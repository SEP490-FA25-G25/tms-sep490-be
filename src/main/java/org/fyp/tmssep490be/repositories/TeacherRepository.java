package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Teacher;
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

    // Tìm tất cả giáo viên trong các branch được chỉ định
    @Query("SELECT DISTINCT t FROM Teacher t JOIN t.userAccount.userBranches ub WHERE ub.branch.id IN :branchIds")
    List<Teacher> findByBranchIds(@Param("branchIds") List<Long> branchIds);
}
