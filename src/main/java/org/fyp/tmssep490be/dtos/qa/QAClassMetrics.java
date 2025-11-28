package org.fyp.tmssep490be.dtos.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QAClassMetrics {
    /**
     * Tỷ lệ điểm danh trung bình của lớp
     * Formula: (Số buổi có mặt / Tổng số buổi đã điểm danh) * 100
     */
    private Double attendanceRate;

    /**
     * Tỷ lệ hoàn thành bài tập trung bình của lớp
     * Formula: (Số buổi hoàn thành bài tập / Tổng số buổi có bài tập) * 100
     */
    private Double homeworkCompletionRate;

    /**
     * Tổng số buổi học trong lớp
     */
    private Integer totalSessions;

    /**
     * Số buổi học đã hoàn thành (không bị hủy)
     */
    private Integer completedSessions;

    /**
     * Tổng số học sinh trong lớp
     */
    private Integer totalStudents;

    /**
     * Số học sinh có mặt (dùng để tính attendance rate)
     */
    private Integer presentStudents;

    /**
     * Số học sinh hoàn thành bài tập (dùng để tính homework rate)
     */
    private Integer completedHomeworkStudents;

    /**
     * Số buổi học bị hủy
     */
    private Integer cancelledSessions;

    /**
     * Số buổi học sắp tới
     */
    private Integer upcomingSessions;

    /**
     * Số học sinh có rủi ro (attendance < 80% OR homework < 70%)
     */
    private Integer studentsAtRis;

    /**
     * Tổng số lần nghỉ học
     */
    private Integer totalAbsences;

    /**
     * Ngày buổi học tiếp theo
     */
    private String nextSessionDate;
}