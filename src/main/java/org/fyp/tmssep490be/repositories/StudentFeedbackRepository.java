package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.StudentFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentFeedbackRepository extends JpaRepository<StudentFeedback, Long> {

    // ============== SCHEDULER JOB METHODS ==============

    /**
     * Find completed classes with students who haven't provided feedback
     * Used by FeedbackCollectionReminderJob to send feedback reminders
     */
    @Query("""
        SELECT new org.fyp.tmssep490be.dtos.FeedbackReminderDTO(
            s.id, u.fullName, cp.id, cp.name, c_course.name, c.id, c.name
        )
        FROM CoursePhase cp
        JOIN cp.course c_course
        JOIN c_course.classes c
        JOIN c.enrollments e
        JOIN e.student s
        JOIN s.userAccount u
        LEFT JOIN StudentFeedback sf ON sf.student.id = s.id
                                  AND sf.phase.id = cp.id
                                  AND sf.classEntity.id = c.id
        WHERE c.status = 'COMPLETED'
        AND e.status = 'COMPLETED'
        AND (sf.id IS NULL OR sf.isFeedback = false)
        """)
    List<org.fyp.tmssep490be.dtos.FeedbackReminderDTO> findPendingFeedbackReminders();
}
