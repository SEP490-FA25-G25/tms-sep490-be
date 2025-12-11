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

        Optional<UserAccount> findByPhone(String phone);

        @EntityGraph(attributePaths = { "userRoles", "userRoles.role", "userBranches", "userBranches.branch" })
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

        // Tìm kiếm theo tên, email, hoặc số điện thoại (không phân biệt hoa thường)
        @Query(value = "SELECT DISTINCT u.* FROM user_account u " +
                        "LEFT JOIN user_role ur ON u.id = ur.user_id " +
                        "LEFT JOIN role r ON ur.role_id = r.id " +
                        "LEFT JOIN user_branches ub ON u.id = ub.user_id " +
                        "WHERE (:search IS NULL OR u.full_name ILIKE CONCAT('%', :search, '%') OR u.email ILIKE CONCAT('%', :search, '%') OR u.phone ILIKE CONCAT('%', :search, '%')) "
                        +
                        "AND (:role IS NULL OR r.code = :role) " +
                        "AND (CAST(:status AS VARCHAR) IS NULL OR u.status = :status) " +
                        "AND (:branchId IS NULL OR ub.branch_id = :branchId)", countQuery = "SELECT COUNT(DISTINCT u.id) FROM user_account u "
                                        +
                                        "LEFT JOIN user_role ur ON u.id = ur.user_id " +
                                        "LEFT JOIN role r ON ur.role_id = r.id " +
                                        "LEFT JOIN user_branches ub ON u.id = ub.user_id " +
                                        "WHERE (:search IS NULL OR u.full_name ILIKE CONCAT('%', :search, '%') OR u.email ILIKE CONCAT('%', :search, '%') OR u.phone ILIKE CONCAT('%', :search, '%')) "
                                        +
                                        "AND (:role IS NULL OR r.code = :role) " +
                                        "AND (CAST(:status AS VARCHAR) IS NULL OR u.status = :status) " +
                                        "AND (:branchId IS NULL OR ub.branch_id = :branchId)", nativeQuery = true)
        Page<UserAccount> findAllWithFilters(
                        @Param("search") String search,
                        @Param("role") String role,
                        @Param("status") String status,
                        @Param("branchId") Long branchId,
                        Pageable pageable);

        /**
         * Find all users with a specific role (for sending bulk notifications)
         */
        @Query("SELECT DISTINCT u FROM UserAccount u " +
                        "JOIN u.userRoles ur " +
                        "WHERE ur.role.code = :roleCode")
        List<UserAccount> findUsersByRole(@Param("roleCode") String roleCode);

        // Đếm user theo trạng thái
        Long countByStatus(UserStatus status);

        // Đếm user theo từng Role
        @Query("SELECT r.name, COUNT(ur) FROM UserRole ur JOIN ur.role r GROUP BY r.name ORDER BY COUNT(ur) DESC")
        List<Object[]> countUsersByRole();

        // Đếm user theo từng Branch
        @Query("SELECT b.name, COUNT(ub) FROM UserBranches ub JOIN ub.branch b GROUP BY b.name ORDER BY COUNT(ub) DESC")
        List<Object[]> countUsersByBranch();

        // Đếm user mới theo ngày (dùng cho biểu đồ)
        @Query(value = "SELECT DATE(created_at) as date, COUNT(*) as count " +
                        "FROM user_account " +
                        "WHERE DATE(created_at) BETWEEN :startDate AND :endDate " +
                        "GROUP BY DATE(created_at) " +
                        "ORDER BY DATE(created_at)", nativeQuery = true)
        List<Object[]> countNewUsersByDay(@Param("startDate") java.time.LocalDate startDate,
                        @Param("endDate") java.time.LocalDate endDate);
}
