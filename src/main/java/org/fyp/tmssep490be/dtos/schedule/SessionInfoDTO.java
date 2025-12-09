package org.fyp.tmssep490be.dtos.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.entities.enums.SessionType;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionInfoDTO {

    private String topic;

    private String description;

    private SessionType sessionType;

    private SessionStatus sessionStatus;

    private String location;

    private String onlineLink;

    private List<String> skills;

    private Integer sequenceNo;
}
