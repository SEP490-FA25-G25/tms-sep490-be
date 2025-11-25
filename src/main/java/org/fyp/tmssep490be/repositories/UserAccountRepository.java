package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    // ============== SCHEDULER JOB METHODS ==============

    /**
     * Find all users with specific role (no branch filtering)
     * Used by scheduler jobs to send notifications to specific user roles
     */
    @Query("SELECT u FROM UserAccount u JOIN u.userRoles ur WHERE ur.role.code = :roleCode")
    List<UserAccount> findUsersByRole(@Param("roleCode") String roleCode);

    // ============== PASSWORD RESET METHODS ==============

    /**
     * Find active user by email for password reset
     * Only returns users with ACTIVE status
     */
    Optional<UserAccount> findActiveUserByEmail(String email);

    /**
     * Update password hash by user ID
     * Used for password reset functionality
     */
    @Modifying
    @Transactional
    @Query("UPDATE UserAccount u SET u.passwordHash = :passwordHash WHERE u.id = :userId")
    int updatePasswordById(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);

    /**
     * Find all users with roles and branches eagerly fetched
     * Used for admin user management listing
     * Override default findAll to include roles and branches
     */
    @Override
    @EntityGraph(attributePaths = {"userRoles", "userRoles.role", "userBranches", "userBranches.branch"})
    org.springframework.data.domain.Page<UserAccount> findAll(org.springframework.data.domain.Pageable pageable);

    /**
     * Find all users with filtering support
     * Supports search by email, fullName, phone and filtering by role and status
     */
    @EntityGraph(attributePaths = {"userRoles", "userRoles.role", "userBranches", "userBranches.branch"})
    @Query("SELECT DISTINCT u FROM UserAccount u " +
           "LEFT JOIN u.userRoles ur " +
           "LEFT JOIN u.userBranches ub " +
           "WHERE (:search IS NULL OR :search = '' OR " +
           "       LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "       LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "       (u.phone IS NOT NULL AND LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%')))) " +
           "AND (:role IS NULL OR :role = '' OR ur.role.code = :role) " +
           "AND (:statusEnum IS NULL OR u.status = :statusEnum)")
    org.springframework.data.domain.Page<UserAccount> findAllWithFilters(
            @Param("search") String search,
            @Param("role") String role,
            @Param("statusEnum") UserStatus statusEnum,
            org.springframework.data.domain.Pageable pageable);
}
