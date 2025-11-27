package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Center;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CenterRepository extends JpaRepository<Center, Long> {

    boolean existsByCode(String code);

    Optional<Center> findByCode(String code);

    /**
     * Count distinct centers that have branches in the given list
     */
    @Query("SELECT COUNT(DISTINCT b.center.id) FROM Branch b WHERE b.id IN :branchIds")
    long countDistinctByBranches(@Param("branchIds") List<Long> branchIds);
}
