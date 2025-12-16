package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Enrollment;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    //Tìm kiếm sinh viên đăng ký lớp học theo ID lớp học với tìm kiếm và phân trang
    @Query("""
        SELECT e FROM Enrollment e
        INNER JOIN FETCH e.student s
        INNER JOIN FETCH s.userAccount u
        WHERE e.classId = :classId
        AND e.status = :status
        AND (:search IS NULL OR :search = '' OR
          UPPER(s.studentCode) LIKE UPPER(CONCAT('%', :search, '%')) OR
          UPPER(u.fullName) LIKE UPPER(CONCAT('%', :search, '%')) OR
          UPPER(u.email) LIKE UPPER(CONCAT('%', :search, '%')) OR
          UPPER(u.phone) LIKE UPPER(CONCAT('%', :search, '%'))
        )
        ORDER BY e.enrolledAt DESC
        """)
    Page<Enrollment> findEnrolledStudentsByClass(
            @Param("classId") Long classId,
            @Param("status") EnrollmentStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    int countByClassIdAndStatus(Long classId, EnrollmentStatus status);

    int countByStudentIdAndStatus(Long studentId, EnrollmentStatus status);

    boolean existsByClassIdAndStudentIdAndStatus(Long classId, Long studentId, EnrollmentStatus status);

    List<Enrollment> findByClassIdAndStatus(Long classId, EnrollmentStatus status);

    List<Enrollment> findByStudentIdAndStatus(Long studentId, EnrollmentStatus status);

    // Find enrollment by studentId, classId and status for request validation
    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId AND e.classId = :classId AND e.status = :status")
    Enrollment findByStudentIdAndClassIdAndStatus(
            @Param("studentId") Long studentId,
            @Param("classId") Long classId,
            @Param("status") EnrollmentStatus status
    );

    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId ORDER BY e.enrolledAt DESC LIMIT 1")
    Enrollment findLatestEnrollmentByStudent(@Param("studentId") Long studentId);

    @Query("SELECT e FROM Enrollment e " +
           "JOIN FETCH e.classEntity c " +
           "JOIN FETCH c.subject sub " +
           "WHERE e.student.id = :studentId")
    List<Enrollment> findByStudentIdWithClassAndCourse(@Param("studentId") Long studentId);

    @Query("SELECT COUNT(e) > 0 FROM Enrollment e " +
           "WHERE e.student.id = :studentId " +
           "AND e.classId = :classId " +
           "AND e.status IN :statuses")
    boolean existsByStudentIdAndClassIdAndStatusIn(
            @Param("studentId") Long studentId,
            @Param("classId") Long classId,
            @Param("statuses") List<EnrollmentStatus> statuses
    );

    List<Enrollment> findByStudentIdAndStatusIn(Long studentId, List<EnrollmentStatus> statuses);

    // Find enrollment by studentId and classId (any status) for attendance report filtering
    @Query("SELECT e FROM Enrollment e " +
           "LEFT JOIN FETCH e.joinSession " +
           "LEFT JOIN FETCH e.leftSession " +
           "WHERE e.student.id = :studentId " +
           "AND e.classId = :classId")
    Enrollment findByStudentIdAndClassId(
            @Param("studentId") Long studentId,
            @Param("classId") Long classId
    );

    // ========= SUPPORT FOR WEEKLY ATTENDANCE REPORT =========

    // Raw data cho báo cáo chuyên cần theo lớp trong tuần
    @Query(value = """
            SELECT c.id AS class_id,
                   c.name AS class_name,
                   COUNT(DISTINCT s.id) AS total_sessions,
                   SUM(CASE WHEN ss.attendance_status = 'PRESENT' THEN 1 ELSE 0 END) AS present_sessions,
                   SUM(CASE WHEN ss.attendance_status = 'ABSENT' THEN 1 ELSE 0 END) AS absent_sessions,
                   CASE WHEN COUNT(DISTINCT s.id) = 0 THEN NULL
                        ELSE ROUND(
                             100.0 * SUM(CASE WHEN ss.attendance_status = 'PRESENT' THEN 1 ELSE 0 END)
                             / GREATEST(COUNT(DISTINCT s.id), 1)
                        , 1) END AS attendance_rate
            FROM enrollment e
            JOIN class c ON e.class_id = c.id
            JOIN session s ON s.class_id = c.id
            LEFT JOIN student_session ss ON ss.session_id = s.id
            WHERE s.date BETWEEN :weekStart AND :weekEnd
              AND s.status != 'CANCELLED'
            GROUP BY c.id, c.name
            ORDER BY c.name ASC
            """, nativeQuery = true)
    List<Object[]> findWeeklyAttendanceRawData(
            @Param("weekStart") java.time.LocalDate weekStart,
            @Param("weekEnd") java.time.LocalDate weekEnd);

    // Raw data cho cảnh báo học viên chuyên cần thấp trong tuần
    @Query(value = """
            SELECT st.id AS student_id,
                   ua.full_name AS student_name,
                   ua.email AS email,
                   c.name AS class_name,
                   SUM(CASE WHEN ss.attendance_status = 'PRESENT' THEN 1 ELSE 0 END) AS present_count,
                   COUNT(DISTINCT s.id) AS total_count,
                   CASE WHEN COUNT(DISTINCT s.id) = 0 THEN NULL
                        ELSE ROUND(
                             100.0 * SUM(CASE WHEN ss.attendance_status = 'PRESENT' THEN 1 ELSE 0 END)
                             / GREATEST(COUNT(DISTINCT s.id), 1)
                        , 1) END AS attendance_rate
            FROM enrollment e
            JOIN student st ON e.student_id = st.id
            JOIN user_account ua ON st.user_account_id = ua.id
            JOIN class c ON e.class_id = c.id
            JOIN session s ON s.class_id = c.id
            LEFT JOIN student_session ss ON ss.session_id = s.id AND ss.student_id = st.id
            WHERE s.date BETWEEN :weekStart AND :weekEnd
              AND s.status != 'CANCELLED'
            GROUP BY st.id, ua.full_name, ua.email, c.name
            HAVING COUNT(DISTINCT s.id) > 0
               AND (
                    100.0 * SUM(CASE WHEN ss.attendance_status = 'PRESENT' THEN 1 ELSE 0 END)
                    / GREATEST(COUNT(DISTINCT s.id), 1)
               ) < :threshold
            ORDER BY attendance_rate ASC
            """, nativeQuery = true)
    List<Object[]> findStudentsWithLowAttendanceRawData(
            @Param("weekStart") java.time.LocalDate weekStart,
            @Param("weekEnd") java.time.LocalDate weekEnd,
            @Param("threshold") double lowAttendanceThreshold);
}