package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    
    List<Branch> findByCenterId(Long centerId);
}
