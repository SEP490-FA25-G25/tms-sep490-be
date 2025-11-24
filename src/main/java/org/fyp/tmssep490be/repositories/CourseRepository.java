package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findBySubjectId(Long subjectId);

    List<Course> findByLevelId(Long levelId);

    List<Course> findBySubjectIdAndLevelId(Long subjectId, Long levelId);
}
