package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.PLO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PLORepository extends JpaRepository<PLO, Long> {
    List<PLO> findByCodeIn(List<String> codes);
}