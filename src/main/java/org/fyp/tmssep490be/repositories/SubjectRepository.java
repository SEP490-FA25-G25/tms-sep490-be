package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.dtos.subject.projections.SubjectSummaryProjection;
import org.fyp.tmssep490be.entities.Subject;
import org.fyp.tmssep490be.entities.enums.SubjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    /**
     * Find subjects by status, ordered by code
     */
    List<Subject> findByStatusOrderByCode(SubjectStatus status);

    boolean existsByCode(String code);

    @Query("""
            SELECT 
                s.id AS id,
                s.code AS code,
                s.name AS name,
                s.description AS description,
                s.status AS status,
                s.createdAt AS createdAt,
                s.updatedAt AS updatedAt,
                COALESCE(cb.fullName, cb.email) AS ownerName,
                COALESCE(COUNT(DISTINCT l.id), 0) AS levelCount,
                COALESCE(COUNT(DISTINCT c.id), 0) AS courseCount,
                COALESCE(SUM(CASE WHEN c.approvalStatus = org.fyp.tmssep490be.entities.enums.ApprovalStatus.PENDING THEN 1 ELSE 0 END), 0) AS pendingCourseCount,
                COALESCE(SUM(CASE WHEN c.approvalStatus = org.fyp.tmssep490be.entities.enums.ApprovalStatus.APPROVED THEN 1 ELSE 0 END), 0) AS approvedCourseCount,
                COALESCE(SUM(CASE WHEN c.status = org.fyp.tmssep490be.entities.enums.CourseStatus.DRAFT THEN 1 ELSE 0 END), 0) AS draftCourseCount
            FROM Subject s
            LEFT JOIN s.levels l
            LEFT JOIN s.courses c
            LEFT JOIN s.createdBy cb
            WHERE (:status IS NULL OR s.status = :status)
            AND (:searchTerm IS NULL OR LOWER(s.code) LIKE :searchTerm 
                 OR LOWER(s.name) LIKE :searchTerm)
            GROUP BY s.id, s.code, s.name, s.description, s.status, s.createdAt, s.updatedAt, cb.fullName, cb.email
            ORDER BY s.code ASC
            """)
    List<SubjectSummaryProjection> findSubjectSummaries(
            @Param("status") SubjectStatus status,
            @Param("searchTerm") String searchTerm
    );

    @Query("""
            SELECT 
                s.id AS id,
                s.code AS code,
                s.name AS name,
                s.description AS description,
                s.status AS status,
                s.createdAt AS createdAt,
                s.updatedAt AS updatedAt,
                COALESCE(cb.fullName, cb.email) AS ownerName,
                COALESCE(COUNT(DISTINCT l.id), 0) AS levelCount,
                COALESCE(COUNT(DISTINCT c.id), 0) AS courseCount,
                COALESCE(SUM(CASE WHEN c.approvalStatus = org.fyp.tmssep490be.entities.enums.ApprovalStatus.PENDING THEN 1 ELSE 0 END), 0) AS pendingCourseCount,
                COALESCE(SUM(CASE WHEN c.approvalStatus = org.fyp.tmssep490be.entities.enums.ApprovalStatus.APPROVED THEN 1 ELSE 0 END), 0) AS approvedCourseCount,
                COALESCE(SUM(CASE WHEN c.status = org.fyp.tmssep490be.entities.enums.CourseStatus.DRAFT THEN 1 ELSE 0 END), 0) AS draftCourseCount
            FROM Subject s
            LEFT JOIN s.levels l
            LEFT JOIN s.courses c
            LEFT JOIN s.createdBy cb
            WHERE s.id = :subjectId
            GROUP BY s.id, s.code, s.name, s.description, s.status, s.createdAt, s.updatedAt, cb.fullName, cb.email
            """)
    Optional<SubjectSummaryProjection> findSubjectSummaryById(@Param("subjectId") Long subjectId);
}
