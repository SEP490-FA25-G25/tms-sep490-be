package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.QAReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QAReportRepository extends JpaRepository<QAReport, Long> {

    @Query("SELECT COUNT(qar) FROM QAReport qar WHERE qar.classEntity.id = :classId")
    long countByClassEntityId(@Param("classId") Long classId);
}
