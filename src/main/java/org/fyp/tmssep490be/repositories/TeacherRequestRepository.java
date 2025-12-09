package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.TeacherRequest;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherRequestRepository extends JpaRepository<TeacherRequest, Long> {

    //Lấy tất cả yêu cầu của giáo viên (bao gồm cả yêu cầu mà giáo viên là người thay thế)
    //Sắp xếp theo thời gian submit giảm dần
    @Query("SELECT tr FROM TeacherRequest tr " +
           "LEFT JOIN FETCH tr.teacher t " +
           "LEFT JOIN FETCH t.userAccount ua " +
           "LEFT JOIN FETCH tr.replacementTeacher rt " +
           "LEFT JOIN FETCH rt.userAccount rua " +
           "LEFT JOIN FETCH tr.session s " +
           "LEFT JOIN FETCH s.classEntity c " +
           "WHERE tr.teacher.id = :teacherId OR (tr.replacementTeacher IS NOT NULL AND tr.replacementTeacher.id = :teacherId) " +
           "ORDER BY tr.submittedAt DESC")
    List<TeacherRequest> findByTeacherIdOrReplacementTeacherIdOrderBySubmittedAtDesc(@Param("teacherId") Long teacherId);

    //Lất tất cả yêu cầu của giáo viên
    //Sắp xếp theo thời gian submit giảm dần
    List<TeacherRequest> findByTeacherIdOrderBySubmittedAtDesc(Long teacherId);
    
    //Lất tất cả yêu cầu của giáo viên mà giáo viên là người dạy thay
    //Sắp xếp theo thời gian submit giảm dần
    List<TeacherRequest> findByReplacementTeacherIdOrderBySubmittedAtDesc(Long replacementTeacherId);

    //Lất tất cả yêu cầu
    //Sắp xếp theo thời gian submit giảm dần
    @Query("SELECT tr FROM TeacherRequest tr " +
           "LEFT JOIN FETCH tr.teacher t " +
           "LEFT JOIN FETCH t.userAccount ua " +
           "LEFT JOIN FETCH tr.session s " +
           "LEFT JOIN FETCH s.classEntity c " +
           "LEFT JOIN FETCH tr.decidedBy db " +
           "ORDER BY tr.submittedAt DESC")
    List<TeacherRequest> findAllByOrderBySubmittedAtDesc();

    //Lất tất cả yêu cầu theo trạng thái
    //Sắp xếp theo thời gian submit giảm dần
    @Query("SELECT tr FROM TeacherRequest tr " +
           "LEFT JOIN FETCH tr.teacher t " +
           "LEFT JOIN FETCH t.userAccount ua " +
           "LEFT JOIN FETCH tr.session s " +
           "LEFT JOIN FETCH s.classEntity c " +
           "LEFT JOIN FETCH tr.decidedBy db " +
           "WHERE tr.status = :status " +
           "ORDER BY tr.submittedAt DESC")
    List<TeacherRequest> findByStatusOrderBySubmittedAtDesc(@Param("status") RequestStatus status);

    //Lấy đầy đủ thông tin yêu cầu theo ID
    @Query("SELECT tr FROM TeacherRequest tr " +
           "LEFT JOIN FETCH tr.teacher t " +
           "LEFT JOIN FETCH t.userAccount ua " +
           "LEFT JOIN FETCH tr.replacementTeacher rt " +
           "LEFT JOIN FETCH rt.userAccount rua " +
           "LEFT JOIN FETCH tr.session s " +
           "LEFT JOIN FETCH s.classEntity c " +
           "LEFT JOIN FETCH s.subjectSession ss " +
           "LEFT JOIN FETCH s.timeSlotTemplate tst " +
           "LEFT JOIN FETCH tr.newResource nr " +
           "LEFT JOIN FETCH tr.newTimeSlot nts " +
           "LEFT JOIN FETCH tr.newSession ns " +
           "LEFT JOIN FETCH tr.decidedBy db " +
           "WHERE tr.id = :id")
    java.util.Optional<TeacherRequest> findByIdWithTeacherAndSession(@Param("id") Long id);

    //Kiểm tra xem session có pending request không
    boolean existsBySessionIdAndStatus(Long sessionId, RequestStatus status);
}

