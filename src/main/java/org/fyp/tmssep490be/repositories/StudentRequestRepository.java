package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.StudentRequest;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.entities.enums.StudentRequestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StudentRequestRepository extends JpaRepository<StudentRequest, Long>, JpaSpecificationExecutor<StudentRequest> {

    // Find requests by student
    Page<StudentRequest> findByStudentIdAndStatusIn(Long studentId, List<RequestStatus> statuses, Pageable pageable);

    // Check for duplicate requests
    boolean existsByStudentIdAndTargetSessionIdAndRequestTypeAndStatusIn(
            Long studentId, Long sessionId, StudentRequestType requestType, List<RequestStatus> statuses);

    // ============== ACADEMIC AFFAIRS BRANCH-FILTERED QUERIES ==============

    /**
     * Find pending requests filtered by AA user's assigned branches
     * Used for secure branch-level filtering at database level
     */
    @Query("SELECT sr FROM StudentRequest sr " +
           "WHERE sr.status = :status " +
           "AND sr.currentClass.branch.id IN :branchIds " +
           "ORDER BY sr.submittedAt ASC")
    Page<StudentRequest> findPendingRequestsByBranches(
            @Param("status") RequestStatus status,
            @Param("branchIds") List<Long> branchIds,
            Pageable pageable);

    /**
     * Find all requests (any status) filtered by AA user's assigned branches
     * Used for history view with branch-level security
     */
    @Query("SELECT sr FROM StudentRequest sr " +
           "WHERE sr.currentClass.branch.id IN :branchIds " +
           "ORDER BY sr.submittedAt DESC")
    Page<StudentRequest> findAllRequestsByBranches(
            @Param("branchIds") List<Long> branchIds,
            Pageable pageable);

    /**
     * Find all requests filtered by AA user's assigned branches and decided by user
     * Used for history view with decidedBy filtering and branch-level security
     */
    @Query("SELECT sr FROM StudentRequest sr " +
           "WHERE sr.currentClass.branch.id IN :branchIds " +
           "AND sr.decidedBy.id = :decidedBy " +
           "ORDER BY sr.submittedAt DESC")
    Page<StudentRequest> findAllRequestsByBranchesAndDecidedBy(
            @Param("branchIds") List<Long> branchIds,
            @Param("decidedBy") Long decidedBy,
            Pageable pageable);

    /**
     * Count pending requests by branch IDs for summary statistics
     */
    @Query("SELECT COUNT(sr) FROM StudentRequest sr " +
           "WHERE sr.status = :status " +
           "AND sr.currentClass.branch.id IN :branchIds")
    long countByStatusAndBranches(
            @Param("status") RequestStatus status,
            @Param("branchIds") List<Long> branchIds);

    /**
     * Count requests by type, status, and branch IDs for summary statistics
     */
    @Query("SELECT COUNT(sr) FROM StudentRequest sr " +
           "WHERE sr.requestType = :requestType " +
           "AND sr.status = :status " +
           "AND sr.currentClass.branch.id IN :branchIds")
    long countByRequestTypeAndStatusAndBranches(
            @Param("requestType") StudentRequestType requestType,
            @Param("status") RequestStatus status,
            @Param("branchIds") List<Long> branchIds);

    // Find pending requests for AA review - simplified query
    @Query("SELECT sr FROM StudentRequest sr WHERE sr.status = :status ORDER BY sr.submittedAt ASC")
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

    /**
     * Check if student has existing makeup request for a target session
     * (to prevent duplicate makeup requests for same session)
     */
    boolean existsByStudentIdAndTargetSessionIdAndRequestTypeAndMakeupSessionIdAndStatusIn(
        Long studentId,
        Long targetSessionId,
        StudentRequestType requestType,
        Long makeupSessionId,
        List<RequestStatus> statuses
    );

    /**
     * Check if makeup session already has a pending/approved request
     * (to prevent conflicts when multiple students request same makeup session)
     */
    @Query("SELECT COUNT(sr) > 0 FROM StudentRequest sr " +
           "WHERE sr.makeupSession.id = :makeupSessionId " +
           "AND sr.status IN :statuses")
    boolean existsPendingMakeupForSession(
        @Param("makeupSessionId") Long makeupSessionId,
        @Param("statuses") List<RequestStatus> statuses
    );

    // ============== TRANSFER REQUEST METHODS ==============

    /**
     * Check for duplicate transfer requests between same classes
     */
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

    /**
     * Count approved transfers for a student in a specific course
     */
    @Query("SELECT COUNT(sr) FROM StudentRequest sr " +
           "JOIN sr.targetClass tc " +
           "JOIN tc.course c " +
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

    /**
     * Count approved transfers by student and request type
     */
    long countByStudentIdAndRequestTypeAndStatus(Long studentId, StudentRequestType requestType, RequestStatus status);

    /**
     * Check if student has pending transfer request from a specific class
     * Used to prevent duplicate transfer requests from same source class
     */
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

    // ============== DYNAMIC FILTERING SPECIFICATIONS ==============

    /**
     * Specification for filtering by student ID
     */
    static Specification<StudentRequest> hasStudentId(Long studentId) {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("student").get("id"), studentId);
    }

    /**
     * Specification for filtering by request types (multiple values)
     */
    static Specification<StudentRequest> hasRequestTypes(List<StudentRequestType> requestTypes) {
        return (root, query, criteriaBuilder) -> {
            if (requestTypes == null || requestTypes.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("requestType").in(requestTypes);
        };
    }

    /**
     * Specification for filtering by statuses (multiple values)
     */
    static Specification<StudentRequest> hasStatuses(List<RequestStatus> statuses) {
        return (root, query, criteriaBuilder) -> {
            if (statuses == null || statuses.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("status").in(statuses);
        };
    }

    /**
     * Specification for filtering by single request type (backward compatibility)
     */
    static Specification<StudentRequest> hasRequestType(StudentRequestType requestType) {
        return (root, query, criteriaBuilder) -> {
            if (requestType == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("requestType"), requestType);
        };
    }

    /**
     * Specification for filtering by single status (backward compatibility)
     */
    static Specification<StudentRequest> hasStatus(RequestStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    /**
     * Specification for searching by request reason, class code, or session title
     */
    static Specification<StudentRequest> hasSearchTerm(String searchTerm) {
        return (root, query, criteriaBuilder) -> {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            String searchLower = searchTerm.toLowerCase().trim();

            return criteriaBuilder.or(
                // Search in request reason
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("requestReason")),
                    "%" + searchLower + "%"
                ),
                // Search in current class code
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("currentClass").get("code")),
                    "%" + searchLower + "%"
                )
            );
        };
    }
}
