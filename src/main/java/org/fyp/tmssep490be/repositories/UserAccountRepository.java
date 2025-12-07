package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.UserAccount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    @EntityGraph(attributePaths = { "userRoles", "userRoles.role" })
    Optional<UserAccount> findByEmail(String email);

    @EntityGraph(attributePaths = { "userRoles", "userRoles.role" })
    Optional<UserAccount> findById(Long id);

    @Override
    @EntityGraph(attributePaths = { "userRoles", "userRoles.role", "userBranches", "userBranches.branch" })
    org.springframework.data.domain.Page<UserAccount> findAll(org.springframework.data.domain.Pageable pageable);

    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);

    @Query("SELECT DISTINCT u FROM UserAccount u " +
            "JOIN u.userRoles ur " +
            "JOIN u.userBranches ub " +
            "WHERE ur.role.code = :roleCode " +
            "AND ub.branch.id IN :branchIds")
    List<UserAccount> findByRoleCodeAndBranches(
            @Param("roleCode") String roleCode,
            @Param("branchIds") List<Long> branchIds);

}
