package org.fyp.tmssep490be.dtos.studentrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response wrapper for missed sessions list
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissedSessionsResponseDTO {

    private Integer totalCount;
    private List<MissedSessionDTO> sessions;
}
