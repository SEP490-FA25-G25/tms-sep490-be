package org.fyp.tmssep490be.dtos.teachergrade;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAssessmentDatesDTO {
    private OffsetDateTime scheduledDate;
    private OffsetDateTime actualDate;
}
