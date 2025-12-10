package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.UserBranches;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBranchesRepository extends JpaRepository<UserBranches, UserBranches.UserBranchesId> {

    @Query("SELECT ub.branch.id FROM UserBranches ub WHERE ub.id.userId = :userId")
    List<Long> findBranchIdsByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM UserBranches ub WHERE ub.userAccount = :userAccount")
    void deleteByUserAccount(@Param("userAccount") UserAccount userAccount);

    @Query("SELECT CASE WHEN COUNT(ub) > 0 THEN true ELSE false END FROM UserBranches ub WHERE ub.id.userId = :userId AND ub.id.branchId = :branchId")
    boolean existsByUserAccountIdAndBranchId(@Param("userId") Long userId, @Param("branchId") Long branchId);
}
