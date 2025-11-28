package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.QAReport;
import org.fyp.tmssep490be.entities.enums.QAReportType;
import org.fyp.tmssep490be.entities.enums.QAReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QAReportRepository extends JpaRepository<QAReport, Long> {

    @Query("SELECT qar FROM QAReport qar " +
           "LEFT JOIN FETCH qar.classEntity c " +
           "LEFT JOIN FETCH qar.session s " +
           "LEFT JOIN FETCH qar.phase p " +
           "LEFT JOIN FETCH qar.reportedBy u " +
           "WHERE (:classId IS NULL OR c.id = :classId) " +
           "AND (:sessionId IS NULL OR s.id = :sessionId) " +
           "AND (:phaseId IS NULL OR p.id = :phaseId) " +
           "AND (:reportType IS NULL OR qar.reportType = :reportType) " +
           "AND (:status IS NULL OR qar.status = :status) " +
           "AND (:reportedBy IS NULL OR u.id = :reportedBy)")
    Page<QAReport> findWithFilters(
        @Param("classId") Long classId,
        @Param("sessionId") Long sessionId,
        @Param("phaseId") Long phaseId,
        @Param("reportType") QAReportType reportType,
        @Param("status") QAReportStatus status,
        @Param("reportedBy") Long reportedBy,
        Pageable pageable
    );

    @Query("SELECT qar FROM QAReport qar " +
           "LEFT JOIN FETCH qar.classEntity " +
           "LEFT JOIN FETCH qar.session " +
           "LEFT JOIN FETCH qar.phase " +
           "LEFT JOIN FETCH qar.reportedBy " +
           "WHERE qar.id = :reportId")
    Optional<QAReport> findByIdWithDetails(@Param("reportId") Long reportId);

    @Query("SELECT COUNT(qar) FROM QAReport qar WHERE qar.classEntity.id = :classId")
    long countByClassEntityId(@Param("classId") Long classId);

    @Query("SELECT qar FROM QAReport qar " +
           "WHERE qar.createdAt >= :startDate " +
           "ORDER BY qar.createdAt DESC")
    List<QAReport> findRecentReports(@Param("startDate") OffsetDateTime startDate, Pageable pageable);

    @Query("SELECT qar FROM QAReport qar " +
           "LEFT JOIN FETCH qar.classEntity " +
           "LEFT JOIN FETCH qar.session " +
           "LEFT JOIN FETCH qar.reportedBy " +
           "WHERE qar.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY qar.createdAt DESC")
    List<QAReport> findReportsByDateRange(@Param("startDate") OffsetDateTime startDate,
                                          @Param("endDate") OffsetDateTime endDate,
                                          Pageable pageable);

    @Query("SELECT qar FROM QAReport qar " +
           "LEFT JOIN FETCH qar.classEntity c " +
           "LEFT JOIN FETCH qar.session " +
           "LEFT JOIN FETCH qar.reportedBy " +
           "WHERE qar.createdAt BETWEEN :startDate AND :endDate " +
           "AND (:branchIds IS NULL OR c.branch.id IN :branchIds) " +
           "ORDER BY qar.createdAt DESC")
    List<QAReport> findReportsByDateRangeAndBranches(@Param("startDate") OffsetDateTime startDate,
                                                      @Param("endDate") OffsetDateTime endDate,
                                                      @Param("branchIds") List<Long> branchIds,
                                                      Pageable pageable);

    List<QAReport> findByClassEntityId(Long classId);
}
