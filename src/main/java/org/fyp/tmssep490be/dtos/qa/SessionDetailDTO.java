package org.fyp.tmssep490be.dtos.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionDetailDTO {
    private Long sessionId;
    private Long classId;
    private String classCode;
    private String subjectName;
    private LocalDate date;
    private String timeSlot;
    private String topic;
    private String studentTask;
    private String status;
    private String teacherName;
    private String teacherNote;
    private AttendanceStats attendanceStats;
    private List<StudentAttendanceDTO> students;
    private List<CLOInfo> closCovered;
    
    // New fields
    private Integer sequenceNo;
    private List<String> skills;
    private PhaseInfo phase;
    private List<ResourceInfo> resources;
    private List<MaterialInfo> materials;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceStats {
        private Integer totalStudents;
        private Integer presentCount;
        private Integer absentCount;
        private Integer homeworkCompletedCount;
        private Boolean hasHomework;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentAttendanceDTO {
        private Long studentId;
        private String studentCode;
        private String studentName;
        private String avatarUrl;
        private String attendanceStatus;
        private String homeworkStatus;
        private Boolean isMakeup;
        private String note;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CLOInfo {
        private Long cloId;
        private String cloCode;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceInfo {
        private Long resourceId;
        private String resourceType; // "ROOM" | "VIRTUAL"
        private String code;
        private String name;
        
        // For ROOM
        private String branchName;
        private String branchAddress;
        private Integer capacity;
        private String equipment;
        
        // For VIRTUAL
        private String meetingUrl;
        private String meetingId;
        private String meetingPasscode;
        private String accountEmail;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhaseInfo {
        private Long phaseId;
        private Integer phaseNumber;
        private String phaseName;
        private String learningFocus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaterialInfo {
        private Long materialId;
        private String title;
        private String description;
        private String materialType; // "DOCUMENT" | "MEDIA" | "ARCHIVE" | "LINK" | "OTHER"
        private String url;
    }
}
