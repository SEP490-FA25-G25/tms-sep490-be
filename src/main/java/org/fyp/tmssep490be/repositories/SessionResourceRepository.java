package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.SessionResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface SessionResourceRepository extends JpaRepository<SessionResource, SessionResource.SessionResourceId> {

    // Kiểm tra resource có được dùng trong session nào không
    boolean existsByResourceId(Long resourceId);

    // Đếm số lớp đang dùng resource
    @Query("SELECT COUNT(DISTINCT s.classEntity.id) FROM SessionResource sr " +
            "JOIN sr.session s WHERE sr.resource.id = :resourceId AND s.status != 'CANCELLED'")
    Long countDistinctClassesByResourceId(@Param("resourceId") Long resourceId);

    // Đếm tổng số sessions dùng resource
    @Query("SELECT COUNT(sr) FROM SessionResource sr " +
            "JOIN sr.session s WHERE sr.resource.id = :resourceId AND s.status != 'CANCELLED'")
    Long countSessionsByResourceId(@Param("resourceId") Long resourceId);

    // Tìm session tiếp theo của resource
    @Query(value = """
        SELECT s.* FROM session s
        JOIN session_resource sr ON s.id = sr.session_id
        JOIN time_slot_template tst ON s.time_slot_template_id = tst.id
        WHERE sr.resource_id = :resourceId
        AND s.status != 'CANCELLED'
        AND (s.date > :currentDate OR (s.date = :currentDate AND tst.start_time > :currentTime))
        ORDER BY s.date ASC, tst.start_time ASC
        LIMIT 1
        """, nativeQuery = true)
    Session findNextSessionByResourceId(
            @Param("resourceId") Long resourceId,
            @Param("currentDate") LocalDate currentDate,
            @Param("currentTime") LocalTime currentTime);

    // Lấy tất cả sessions của resource
    @Query("SELECT s FROM SessionResource sr " +
            "JOIN sr.session s WHERE sr.resource.id = :resourceId AND s.status != 'CANCELLED' " +
            "ORDER BY s.date DESC")
    List<Session> findSessionsByResourceId(@Param("resourceId") Long resourceId);

    // Tìm sức chứa lớn nhất của các lớp đang dùng resource (để validate khi giảm capacity)
    @Query("SELECT MAX(s.classEntity.maxCapacity) FROM SessionResource sr " +
            "JOIN sr.session s WHERE sr.resource.id = :resourceId")
    Integer findMaxClassCapacityByResourceId(@Param("resourceId") Long resourceId);
}