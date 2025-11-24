package org.fyp.tmssep490be.dtos.studentportal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO for classmate information in student portal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassmateDTO {
    private Long studentId;
    private String fullName;
    private String avatar;
    private String email;
    private String studentCode;
    private Long enrollmentId;
    private OffsetDateTime enrollmentDate;
    private String enrollmentStatus;
    private BigDecimal attendanceRate;
}