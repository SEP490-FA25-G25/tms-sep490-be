package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.PLO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PLORepository extends JpaRepository<PLO, Long> {
}