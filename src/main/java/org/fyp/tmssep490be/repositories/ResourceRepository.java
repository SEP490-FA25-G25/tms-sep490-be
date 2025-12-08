package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Resource;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.entities.enums.ResourceStatus;
import org.fyp.tmssep490be.entities.enums.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    // Lấy resources theo branch
    List<Resource> findByBranchIdOrderByNameAsc(Long branchId);

    // Kiểm tra trùng code trong cùng branch
    boolean existsByBranchIdAndCodeIgnoreCase(Long branchId, String code);

    // Kiểm tra trùng code (loại trừ chính nó - dùng khi UPDATE)
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM Resource r " +
            "WHERE r.branch.id = :branchId " +
            "AND LOWER(r.code) = LOWER(:code) " +
            "AND r.id <> :excludeId")
    boolean existsByBranchIdAndCodeIgnoreCaseAndIdNot(
            @Param("branchId") Long branchId,
            @Param("code") String code,
            @Param("excludeId") Long excludeId);

    // Kiểm tra trùng tên trong cùng branch
    boolean existsByBranchIdAndNameIgnoreCase(Long branchId, String name);

    // Kiểm tra trùng tên (loại trừ chính nó)
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM Resource r " +
            "WHERE r.branch.id = :branchId " +
            "AND LOWER(r.name) = LOWER(:name) " +
            "AND r.id <> :excludeId")
    boolean existsByBranchIdAndNameIgnoreCaseAndIdNot(
            @Param("branchId") Long branchId,
            @Param("name") String name,
            @Param("excludeId") Long excludeId);

    // Đếm resources theo branch
    long countByBranchId(Long branchId);

    // Đếm resources theo branch và status
    long countByBranchIdAndStatus(Long branchId, ResourceStatus status);

    // Lấy resources VIRTUAL có ngày hết hạn (cho scheduler job)
    @Query("SELECT r FROM Resource r " +
            "WHERE r.resourceType = :resourceType " +
            "AND r.expiryDate IS NOT NULL " +
            "ORDER BY r.expiryDate ASC")
    List<Resource> findByResourceTypeAndExpiryDateIsNotNull(
            @Param("resourceType") ResourceType resourceType);

    // Tìm resource khả dụng cho buổi học (cùng branch, không bị trùng lịch)
    @Query("""
        SELECT r FROM Resource r
        WHERE r.branch.id = :branchId
          AND r.status = org.fyp.tmssep490be.entities.enums.ResourceStatus.ACTIVE
          AND NOT EXISTS (
              SELECT 1 FROM SessionResource sr
              JOIN sr.session s
              WHERE sr.resource.id = r.id
                AND s.status != org.fyp.tmssep490be.entities.enums.SessionStatus.CANCELLED
                AND s.date = :sessionDate
                AND s.timeSlotTemplate.id = :timeSlotId
                AND s.id <> :sessionId
          )
        ORDER BY r.name ASC
        """)
    List<Resource> findAvailableResourcesForSession(@Param("branchId") Long branchId,
                                                    @Param("sessionDate") LocalDate sessionDate,
                                                    @Param("timeSlotId") Long timeSlotId,
                                                    @Param("sessionId") Long sessionId);
}