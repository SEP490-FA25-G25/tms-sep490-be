package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.TeacherSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for TeacherSkill entity
 * <p>
 * Manages teacher skill assignments with composite primary key (teacher_id, skill)
 * </p>
 */
@Repository
public interface TeacherSkillRepository extends JpaRepository<TeacherSkill, TeacherSkill.TeacherSkillId> {

    /**
     * Find all skills for a teacher
     *
     * @param teacherId Teacher ID
     * @return List of teacher skills
     */
    List<TeacherSkill> findByTeacherId(Long teacherId);
}
