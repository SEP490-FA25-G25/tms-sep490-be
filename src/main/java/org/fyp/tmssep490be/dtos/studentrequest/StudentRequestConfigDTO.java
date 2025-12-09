package org.fyp.tmssep490be.dtos.studentrequest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentRequestConfigDTO {

    private Integer makeupLookbackWeeks;

    private Integer makeupWeeksLimit;

    private Integer maxTransfersPerCourse;

    private Integer absenceLeadTimeDays;

    private Integer reasonMinLength;
}
