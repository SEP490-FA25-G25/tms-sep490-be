package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Curriculum;
import org.fyp.tmssep490be.entities.enums.CurriculumStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurriculumRepository extends JpaRepository<Curriculum, Long> {

    // Kiểm tra code đã tồn tại chưa
    boolean existsByCode(String code);

    // Tìm theo status, sắp xếp theo code
    List<Curriculum> findByStatusOrderByCode(CurriculumStatus status);
}