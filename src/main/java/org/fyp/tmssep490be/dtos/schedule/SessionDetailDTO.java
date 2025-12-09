package org.fyp.tmssep490be.dtos.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionDetailDTO {

    private Long sessionId;

    private Long studentSessionId;

    private LocalDate date;

    private DayOfWeek dayOfWeek;

    private LocalTime startTime;

    private LocalTime endTime;

    private String timeSlotName;

    private ClassInfoDTO classInfo;

    private SessionInfoDTO sessionInfo;

    private StudentStatusDTO studentStatus;

    private List<MaterialDTO> materials;

    private ResourceDTO classroomResource;

    private MakeupInfoDTO makeupInfo;
}
