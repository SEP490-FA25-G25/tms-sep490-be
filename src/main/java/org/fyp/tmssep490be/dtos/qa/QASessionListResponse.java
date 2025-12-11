package org.fyp.tmssep490be.dtos.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QASessionListResponse {

    private Long classId;
    private String classCode;
    private Integer totalSessions;
    private List<QASessionItemDTO> sessions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QASessionItemDTO {
        private Long sessionId;
        private Integer sequenceNumber;
        private LocalDate date;
        private String dayOfWeek;
        private String timeSlot;  
        private LocalTime startTime;  
        private LocalTime endTime;   
        private String topic;
        private String status;
        private String teacherName;

        private Integer totalStudents;
        private Integer presentCount;
        private Integer absentCount;
        private Double attendanceRate;
        private Integer homeworkCompletedCount;
        private Double homeworkCompletionRate;

        private Boolean hasQAReport;
        private Integer qaReportCount;
    }
}
