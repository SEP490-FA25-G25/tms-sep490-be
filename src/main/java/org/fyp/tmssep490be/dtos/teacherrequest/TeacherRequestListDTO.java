package org.fyp.tmssep490be.dtos.teacherrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.Modality;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.entities.enums.TeacherRequestType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

/**
 * DTO for listing Teacher Requests (teacher & staff views)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherRequestListDTO {

    private Long id;
    private TeacherRequestType requestType;
    private RequestStatus status;

    // Original session info
    private Long sessionId;
    private LocalDate sessionDate;
    private LocalTime sessionStartTime;
    private LocalTime sessionEndTime;
    private String className;
    private String classCode;
    private String sessionTopic;

    // Request owner info
    private Long teacherId;
    private String teacherName;
    private String teacherEmail;

    // Replacement teacher (for SWAP)
    private String replacementTeacherName;

    // Proposed schedule (for RESCHEDULE)
    private LocalDate newSessionDate;
    private LocalTime newSessionStartTime;
    private LocalTime newSessionEndTime;

    private String requestReason;
    private OffsetDateTime submittedAt;
    private OffsetDateTime decidedAt;
    private Long decidedById;
    private String decidedByName;
    private String decidedByEmail;

    // Modality (for MODALITY_CHANGE)
    private Modality currentModality;
    private Modality newModality;
}



