package org.fyp.tmssep490be.dtos.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummaryDTO {
    private int totalStudents;
    private int presentCount;
    private int absentCount;
    private Integer completedHomeworkCount; // Số học viên đã làm bài tập về nhà (null nếu không có bài tập)
}

