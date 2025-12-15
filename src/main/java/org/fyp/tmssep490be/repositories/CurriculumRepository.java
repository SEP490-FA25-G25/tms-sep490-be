package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Curriculum;
import org.fyp.tmssep490be.entities.enums.CurriculumStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurriculumRepository extends JpaRepository<Curriculum, Long> {

    // Kiểm tra code đã tồn tại chưa
    boolean existsByCode(String code);

    // Tìm theo status, sắp xếp theo code
    List<Curriculum> findByStatusOrderByCode(CurriculumStatus status);

    // Fetch all curriculums with PLOs eagerly loaded
    @Query("SELECT DISTINCT c FROM Curriculum c LEFT JOIN FETCH c.plos ORDER BY c.updatedAt DESC")
    List<Curriculum> findAllWithPlos();
}