package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.PLOCLOMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PLOCLOMappingRepository extends JpaRepository<PLOCLOMapping, Long> {

    @Query("SELECT pm FROM PLOCLOMapping pm WHERE pm.clo.id = :cloId")
    List<PLOCLOMapping> findByCloId(@Param("cloId") Long cloId);
}
