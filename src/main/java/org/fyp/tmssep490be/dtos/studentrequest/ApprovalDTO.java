package org.fyp.tmssep490be.dtos.studentrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for approving a request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalDTO {

    private String note; // Optional approval note
}