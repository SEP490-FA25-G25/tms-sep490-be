package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.CLO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CLORepository extends JpaRepository<CLO, Long> {

    List<CLO> findBySubjectId(Long subjectId);

    void deleteBySubjectId(Long subjectId);

    List<CLO> findByCodeIn(List<String> codes);
}