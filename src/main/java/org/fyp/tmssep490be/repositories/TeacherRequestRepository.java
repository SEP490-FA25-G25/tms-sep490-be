package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.TeacherRequest;
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
           "WHERE tr.teacher.id = :teacherId OR tr.replacementTeacher.id = :teacherId " +
           "ORDER BY tr.submittedAt DESC")
    List<TeacherRequest> findByTeacherIdOrReplacementTeacherIdOrderBySubmittedAtDesc(@Param("teacherId") Long teacherId);
}

