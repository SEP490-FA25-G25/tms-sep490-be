package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.StudentRequest;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.entities.enums.StudentRequestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface StudentRequestRepository extends JpaRepository<StudentRequest, Long> {

    // Find requests by student
    Page<StudentRequest> findByStudentIdAndStatusIn(Long studentId, List<RequestStatus> statuses, Pageable pageable);

    // Check for duplicate requests
    boolean existsByStudentIdAndTargetSessionIdAndRequestTypeAndStatusIn(
            Long studentId, Long sessionId, StudentRequestType requestType, List<RequestStatus> statuses);

    @Query("SELECT sr FROM StudentRequest sr " +
           "JOIN sr.currentClass c " +
           "JOIN c.branch b " +
           "WHERE sr.status = :status " +
           "AND b.id IN :branchIds")
    Page<StudentRequest> findPendingRequestsByBranches(
            @Param("status") RequestStatus status,
            @Param("branchIds") List<Long> branchIds,
            Pageable pageable);


    @Query("SELECT sr FROM StudentRequest sr " +
           "JOIN sr.currentClass c " +
           "JOIN c.branch b " +
           "WHERE b.id IN :branchIds")
    Page<StudentRequest> findAllRequestsByBranches(
            @Param("branchIds") List<Long> branchIds,
            Pageable pageable);


    @Query("SELECT sr FROM StudentRequest sr " +
           "JOIN sr.currentClass c " +
           "JOIN c.branch b " +
           "WHERE b.id IN :branchIds " +
           "AND sr.decidedBy.id = :decidedBy")
    Page<StudentRequest> findAllRequestsByBranchesAndDecidedBy(
            @Param("branchIds") List<Long> branchIds,
            @Param("decidedBy") Long decidedBy,
            Pageable pageable);

    @Query("SELECT COUNT(sr) FROM StudentRequest sr " +
           "JOIN sr.currentClass c " +
           "JOIN c.branch b " +
           "WHERE sr.status = :status " +
           "AND b.id IN :branchIds")
    long countByStatusAndBranches(
            @Param("status") RequestStatus status,
            @Param("branchIds") List<Long> branchIds);

    @Query("SELECT COUNT(sr) FROM StudentRequest sr " +
           "JOIN sr.currentClass c " +
           "JOIN c.branch b " +
           "WHERE sr.requestType = :requestType " +
           "AND sr.status = :status " +
           "AND b.id IN :branchIds")
    long countByRequestTypeAndStatusAndBranches(
            @Param("requestType") StudentRequestType requestType,
            @Param("status") RequestStatus status,
            @Param("branchIds") List<Long> branchIds);

    // Find pending requests for AA review - simplified query
    @Query("SELECT sr FROM StudentRequest sr WHERE sr.status = :status")
    Page<StudentRequest> findPendingRequestsForAA(@Param("status") RequestStatus status, Pageable pageable);

    // Find all requests by status
    Page<StudentRequest> findByStatus(RequestStatus status, Pageable pageable);

    // Find all requests by status with sort only (for in-memory filtering)
    List<StudentRequest> findByStatus(RequestStatus status, org.springframework.data.domain.Sort sort);

    // Find request by student and ID (for student access control)
    boolean existsByIdAndStudentId(Long requestId, Long studentId);

    // Count requests by status for summary
    long countByStatus(RequestStatus status);

    // Count requests by type and status for summary
    long countByRequestTypeAndStatus(StudentRequestType requestType, RequestStatus status);

    // Find all requests by student (for previous requests calculation)
    List<StudentRequest> findByStudentId(Long studentId);

    boolean existsByStudentIdAndTargetSessionIdAndRequestTypeAndMakeupSessionIdAndStatusIn(
        Long studentId,
        Long targetSessionId,
        StudentRequestType requestType,
        Long makeupSessionId,
        List<RequestStatus> statuses
    );

    @Query("SELECT COUNT(sr) > 0 FROM StudentRequest sr " +
           "WHERE sr.makeupSession.id = :makeupSessionId " +
           "AND sr.status IN :statuses")
    boolean existsPendingMakeupForSession(
        @Param("makeupSessionId") Long makeupSessionId,
        @Param("statuses") List<RequestStatus> statuses
    );

    @Query("SELECT COUNT(sr) > 0 FROM StudentRequest sr " +
           "WHERE sr.student.id = :studentId " +
           "AND sr.currentClass.id = :currentClassId " +
           "AND sr.targetClass.id = :targetClassId " +
           "AND sr.requestType = :requestType " +
           "AND sr.status IN :statuses")
    boolean existsByStudentIdAndCurrentClassIdAndTargetClassIdAndRequestTypeAndStatusIn(
            @Param("studentId") Long studentId,
            @Param("currentClassId") Long currentClassId,
            @Param("targetClassId") Long targetClassId,
            @Param("requestType") StudentRequestType requestType,
            @Param("statuses") List<RequestStatus> statuses);

    @Query("SELECT COUNT(sr) FROM StudentRequest sr " +
           "JOIN sr.targetClass tc " +
           "JOIN tc.subject c " +
           "WHERE sr.student.id = :studentId " +
           "AND sr.requestType = :requestType " +
           "AND sr.status = :status " +
           "AND c.id = :courseId")
    long countByStudentIdAndRequestTypeAndStatusAndTargetClassCourseId(
            @Param("studentId") Long studentId,
            @Param("requestType") StudentRequestType requestType,
            @Param("status") RequestStatus status,
            @Param("courseId") Long courseId
    );

    long countByStudentIdAndRequestTypeAndStatus(Long studentId, StudentRequestType requestType, RequestStatus status);

    @Query("SELECT COUNT(sr) > 0 FROM StudentRequest sr " +
           "WHERE sr.student.id = :studentId " +
           "AND sr.currentClass.id = :currentClassId " +
           "AND sr.requestType = :requestType " +
           "AND sr.status = :status")
    boolean existsPendingTransferFromClass(
            @Param("studentId") Long studentId,
            @Param("currentClassId") Long currentClassId,
            @Param("requestType") StudentRequestType requestType,
            @Param("status") RequestStatus status);

    @Query("""
        SELECT sr FROM StudentRequest sr
        WHERE sr.student.id = :studentId
        AND (:search IS NULL OR :search = '' OR 
             LOWER(sr.requestReason) LIKE LOWER(CONCAT('%', :search, '%')) OR
             LOWER(sr.currentClass.code) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:requestTypes IS NULL OR sr.requestType IN :requestTypes)
        AND (:statuses IS NULL OR sr.status IN :statuses)
        """)
    Page<StudentRequest> findStudentRequestsWithFilters(
            @Param("studentId") Long studentId,
            @Param("search") String search,
            @Param("requestTypes") List<StudentRequestType> requestTypes,
            @Param("statuses") List<RequestStatus> statuses,
            Pageable pageable);

    @Query("SELECT sr FROM StudentRequest sr " +
           "JOIN FETCH sr.student " +
           "JOIN FETCH sr.currentClass " +
           "JOIN FETCH sr.targetClass " +
           "JOIN FETCH sr.effectiveSession " +
           "LEFT JOIN FETCH sr.decidedBy " +
           "WHERE sr.requestType = :requestType " +
           "AND sr.status = :status " +
           "AND sr.effectiveDate = :effectiveDate " +
           "ORDER BY sr.id ASC")
    List<StudentRequest> findApprovedTransferRequestsByEffectiveDate(
        @Param("requestType") StudentRequestType requestType,
        @Param("status") RequestStatus status,
        @Param("effectiveDate") LocalDate effectiveDate
    );

    @Query("SELECT sr FROM StudentRequest sr " +
           "WHERE sr.student.id = :studentId " +
           "AND sr.targetSession.id = :targetSessionId " +
           "AND sr.requestType = :requestType")
    List<StudentRequest> findByStudentIdAndTargetSessionIdAndRequestType(
            @Param("studentId") Long studentId,
            @Param("targetSessionId") Long targetSessionId,
            @Param("requestType") StudentRequestType requestType);

    /**
     * Find requests by status and submitted before cutoff date
     * Used by RequestExpiryJob to expire old PENDING requests
     */
    @Query("SELECT sr FROM StudentRequest sr " +
           "WHERE sr.status = :status " +
           "AND sr.submittedAt < :cutoffDate " +
           "ORDER BY sr.submittedAt ASC")
    List<StudentRequest> findByStatusAndSubmittedAtBefore(
            @Param("status") RequestStatus status,
            @Param("cutoffDate") OffsetDateTime cutoffDate);
}
