package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.TeachingSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeachingSlotRepository extends JpaRepository<TeachingSlot, TeachingSlot.TeachingSlotId> {

    //Tìm kiếm tất cả lớp học được phân công cho giáo viên theo teacherId
    @Query("""
        SELECT DISTINCT c FROM TeachingSlot ts
        JOIN ts.session s
        JOIN s.classEntity c
        JOIN FETCH c.subject
        JOIN FETCH c.branch
        WHERE ts.teacher.id = :teacherId
          AND ts.status = 'SCHEDULED'
        ORDER BY c.code ASC
        """)
    List<org.fyp.tmssep490be.entities.ClassEntity> findDistinctClassesByTeacherId(
        @Param("teacherId") Long teacherId);
}
