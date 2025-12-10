package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @EntityGraph(attributePaths = { "userRoles", "userRoles.role", "userBranches", "userBranches.branch" })
    @Query("SELECT DISTINCT u FROM UserAccount u " +
            "LEFT JOIN u.userRoles ur " +
            "LEFT JOIN ur.role r " +
            "WHERE (:search IS NULL OR u.fullName LIKE %:search% OR u.email LIKE %:search%) " +
            "AND (:role IS NULL OR r.code = :role) " +
            "AND (:status IS NULL OR u.status = :status)")
    Page<UserAccount> findAllWithFilters(
            @Param("search") String search,
            @Param("role") String role,
            @Param("status") UserStatus status,
            Pageable pageable);
}
