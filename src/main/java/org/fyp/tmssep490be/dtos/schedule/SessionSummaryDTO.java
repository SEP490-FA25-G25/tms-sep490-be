package org.fyp.tmssep490be.dtos.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.Modality;
import org.fyp.tmssep490be.entities.enums.ResourceType;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.entities.enums.SessionType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionSummaryDTO {

    private Long sessionId;

    private Long studentSessionId;

    private Long classId;

    private LocalDate date;

    private DayOfWeek dayOfWeek;

    private Long timeSlotTemplateId;

    private LocalTime startTime;

    private LocalTime endTime;

    private String classCode;

    private String className;

    private Long subjectId;

    private String subjectName;

    private String topic;

    private SessionType sessionType;

    private SessionStatus sessionStatus;

    private Modality modality;

    private String location;

    private String branchName;

    private AttendanceStatus attendanceStatus;

    private Boolean isMakeup;

    private MakeupInfoDTO makeupInfo;

    private String resourceName;

    private ResourceType resourceType;

    private String onlineLink;
}