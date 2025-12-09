package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Subject;
import org.fyp.tmssep490be.entities.enums.SubjectStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findAll(Sort sort);

    List<Subject> findByCurriculumIdOrderByUpdatedAtDesc(Long curriculumId);

    List<Subject> findByLevelIdOrderByUpdatedAtDesc(Long levelId);

    List<Subject> findByCurriculumIdAndLevelIdOrderByUpdatedAtDesc(Long curriculumId, Long levelId);

    // Kiểm tra có Subject nào đang ACTIVE thuộc Level không
    boolean existsByLevelIdAndStatus(Long levelId, SubjectStatus status);

    // Đếm số Subject thuộc Level
    long countByLevelId(Long levelId);

    // Kiểm tra code đã tồn tại
    boolean existsByCode(String code);

    // Tìm theo curriculum ID và status
    boolean existsByCurriculumIdAndStatus(Long curriculumId, SubjectStatus status);
}

