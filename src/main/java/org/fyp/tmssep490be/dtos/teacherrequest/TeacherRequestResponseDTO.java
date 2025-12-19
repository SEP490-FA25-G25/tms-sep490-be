package org.fyp.tmssep490be.dtos.teacherrequest;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.entities.enums.TeacherRequestType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeacherRequestResponseDTO {

    private Long id;
    private TeacherRequestType requestType;
    private RequestStatus status;
    private Long sessionId;
    private String classCode;
    private String className;
    private String subjectName;
    private LocalDate sessionDate;
    private LocalTime sessionStartTime;
    private LocalTime sessionEndTime;
    private String sessionTopic;
    private Long teacherId;
    private String teacherName;
    private String teacherEmail;
    private String currentModality;
    private String newModality;
    private String currentResourceName;
    private Long replacementTeacherId;
    private String replacementTeacherName;
    private String replacementTeacherEmail;
    private LocalDate newDate;
    private LocalTime newTimeSlotStartTime;
    private LocalTime newTimeSlotEndTime;
    private String newTimeSlotName;
    private Long newResourceId;
    private String newResourceName;
    private String newResourceType;
    private Long newSessionId;
    private LocalDate newSessionDate;
    private LocalTime newSessionStartTime;
    private LocalTime newSessionEndTime;
    private String newSessionResourceName;
    private String newSessionClassCode;
    private String requestReason;
    private String note;
    private OffsetDateTime submittedAt;
    private OffsetDateTime decidedAt;
    private Long decidedById;
    private String decidedByName;
    private String decidedByEmail;
    
    // Session information for displaying badges
    private String sessionStatus;
    private List<String> pendingRequestTypes;
    private Boolean attendanceSubmitted;
    private Boolean isMakeup;
    private String sessionModality; // ONLINE or OFFLINE based on resource type
    private Boolean reportSubmitted; // Whether QA report has been submitted for the session
}

