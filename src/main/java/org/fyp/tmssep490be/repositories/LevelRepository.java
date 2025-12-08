package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Level;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LevelRepository extends JpaRepository<Level, Long> {

    // Tìm level theo code
    java.util.Optional<Level> findByCodeIgnoreCase(String code);

    // Tìm levels theo curriculum ID, sắp xếp theo sort order
    List<Level> findByCurriculumIdOrderBySortOrderAsc(Long curriculumId);

    // Tìm levels theo curriculum ID, sắp xếp theo updatedAt DESC (mới nhất trước)
    List<Level> findByCurriculumIdOrderByUpdatedAtDesc(Long curriculumId);

    // Lấy max sort order để tính sortOrder cho level mới
    @Query("SELECT MAX(l.sortOrder) FROM Level l WHERE l.curriculum.id = :curriculumId")
    Integer findMaxSortOrderByCurriculumId(@Param("curriculumId") Long curriculumId);

    // Đếm số level thuộc curriculum
    long countByCurriculumId(Long curriculumId);
}
