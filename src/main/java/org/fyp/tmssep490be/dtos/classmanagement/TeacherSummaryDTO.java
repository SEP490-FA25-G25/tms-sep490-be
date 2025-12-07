package org.fyp.tmssep490be.dtos.classmanagement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherSummaryDTO {

    private Long id;
    private Long teacherId;
    private String fullName;
    private String email;
    private String phone;
    private String employeeCode;
    private Integer sessionCount;
}