package org.fyp.tmssep490be.dtos.studentrequest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MissedSessionsResponseDTO {

    private Long studentId;

    private Integer totalCount;

    private List<MissedSessionDTO> missedSessions;
}
