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

    /**
     * Find user account by email (email is used for login)
     * Eagerly fetch userRoles and their associated roles for authentication
     */
    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Optional<UserAccount> findByEmail(String email);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find user account by ID with roles eagerly fetched
     * Override default findById to include roles for token refresh
     */
    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Optional<UserAccount> findById(Long id);

    /**
     * Find all users with a specific role in given branches
     * Used for AA staff listing in request filters
     */
    @Query("SELECT DISTINCT u FROM UserAccount u " +
           "JOIN u.userRoles ur " +
           "JOIN u.userBranches ub " +
           "WHERE ur.role.code = :roleCode " +
           "AND ub.branch.id IN :branchIds")
    List<UserAccount> findByRoleCodeAndBranches(
            @Param("roleCode") String roleCode,
            @Param("branchIds") List<Long> branchIds);
}
