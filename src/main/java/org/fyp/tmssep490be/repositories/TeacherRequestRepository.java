package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.TeacherRequest;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.entities.enums.TeacherRequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherRequestRepository extends JpaRepository<TeacherRequest, Long> {
    
    /**
     * Find all requests by teacher ID, ordered by submitted date descending
     */
    List<TeacherRequest> findByTeacherIdOrderBySubmittedAtDesc(Long teacherId);
    
    /**
     * Find requests where teacher is replacement teacher, ordered by submitted date descending
     */
    List<TeacherRequest> findByReplacementTeacherIdOrderBySubmittedAtDesc(Long replacementTeacherId);
    
    /**
     * Find all requests for a teacher (both created by teacher and where teacher is replacement)
     * Ordered by submitted date descending
     */
    @Query("SELECT tr FROM TeacherRequest tr " +
           "LEFT JOIN FETCH tr.teacher t " +
           "LEFT JOIN FETCH t.userAccount ua " +
           "LEFT JOIN FETCH tr.replacementTeacher rt " +
           "LEFT JOIN FETCH rt.userAccount rua " +
           "LEFT JOIN FETCH tr.session s " +
           "LEFT JOIN FETCH s.classEntity c " +
           "WHERE tr.teacher.id = :teacherId OR tr.replacementTeacher.id = :teacherId " +
           "ORDER BY tr.submittedAt DESC")
    List<TeacherRequest> findByTeacherIdOrReplacementTeacherIdOrderBySubmittedAtDesc(@Param("teacherId") Long teacherId);
    
    /**
     * Check if a pending request exists for the same session and request type
     * Used to prevent duplicate requests
     */
    boolean existsBySessionIdAndRequestTypeAndStatus(
            Long sessionId, 
            TeacherRequestType requestType, 
            RequestStatus status
    );
    
    /**
     * Find pending request by session ID and request type
     */
    Optional<TeacherRequest> findBySessionIdAndRequestTypeAndStatus(
            Long sessionId,
            TeacherRequestType requestType,
            RequestStatus status
    );
    
    /**
     * Find request by ID with all relationships loaded for detail view
     */
    @Query("SELECT tr FROM TeacherRequest tr " +
           "LEFT JOIN FETCH tr.teacher t " +
           "LEFT JOIN FETCH t.userAccount ua " +
           "LEFT JOIN FETCH tr.replacementTeacher rt " +
           "LEFT JOIN FETCH rt.userAccount rua " +
           "LEFT JOIN FETCH tr.session s " +
           "LEFT JOIN FETCH s.classEntity c " +
           "LEFT JOIN FETCH s.courseSession cs " +
           "LEFT JOIN FETCH s.timeSlotTemplate tst " +
           "LEFT JOIN FETCH tr.newResource nr " +
           "LEFT JOIN FETCH tr.newTimeSlot nts " +
           "LEFT JOIN FETCH tr.decidedBy db " +
           "WHERE tr.id = :id")
    Optional<TeacherRequest> findByIdWithTeacherAndSession(@Param("id") Long id);
    
    /**
     * Find requests by teacher ID and status
     */
    List<TeacherRequest> findByTeacherIdAndStatusOrderBySubmittedAtDesc(
            Long teacherId, 
            RequestStatus status
    );

    /**
     * Find all requests ordered by submitted time (newest first)
     * With teacher and userAccount loaded for staff view
     */
    @Query("SELECT tr FROM TeacherRequest tr " +
           "LEFT JOIN FETCH tr.teacher t " +
           "LEFT JOIN FETCH t.userAccount ua " +
           "LEFT JOIN FETCH tr.session s " +
           "LEFT JOIN FETCH s.classEntity c " +
           "LEFT JOIN FETCH tr.decidedBy db " +
           "ORDER BY tr.submittedAt DESC")
    List<TeacherRequest> findAllByOrderBySubmittedAtDesc();

    /**
     * Find requests by status ordered by submitted time (newest first)
     * With teacher and userAccount loaded for staff view
     */
    @Query("SELECT tr FROM TeacherRequest tr " +
           "LEFT JOIN FETCH tr.teacher t " +
           "LEFT JOIN FETCH t.userAccount ua " +
           "LEFT JOIN FETCH tr.session s " +
           "LEFT JOIN FETCH s.classEntity c " +
           "LEFT JOIN FETCH tr.decidedBy db " +
           "WHERE tr.status = :status " +
           "ORDER BY tr.submittedAt DESC")
    List<TeacherRequest> findByStatusOrderBySubmittedAtDesc(@Param("status") RequestStatus status);

    /**
     * Find requests by session IDs and statuses
     * Used to check if sessions have pending requests
     */
    List<TeacherRequest> findBySessionIdInAndStatusIn(List<Long> sessionIds, List<RequestStatus> statuses);
}
