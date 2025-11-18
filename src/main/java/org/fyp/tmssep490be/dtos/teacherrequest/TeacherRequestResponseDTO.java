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

/**
 * DTO for Teacher Request response
 * Fields with null value will be excluded from JSON response
 */
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
    private LocalDate sessionDate;
    private LocalTime sessionStartTime;
    private LocalTime sessionEndTime;
    private String sessionTopic;
    private Long teacherId;
    private String teacherName;
    private String teacherEmail;
    private Long replacementTeacherId;
    private String replacementTeacherName;
    private String replacementTeacherEmail;
    private LocalDate newDate;
    private LocalTime newTimeSlotStartTime;
    private LocalTime newTimeSlotEndTime;
    private String newTimeSlotName;
    private Long newResourceId;
    private String newResourceName;
    private String requestReason;
    private String note;
    private OffsetDateTime submittedAt;
    private OffsetDateTime decidedAt;
    private Long decidedById;
    private String decidedByName;
    private String decidedByEmail;
    private Long newSessionId;
}



