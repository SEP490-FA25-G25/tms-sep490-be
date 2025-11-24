package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.CLO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CLORepository extends JpaRepository<CLO, Long> {

    @Query("SELECT c FROM CLO c WHERE c.course.id = :courseId")
    List<CLO> findByCourseId(@Param("courseId") Long courseId);
}
