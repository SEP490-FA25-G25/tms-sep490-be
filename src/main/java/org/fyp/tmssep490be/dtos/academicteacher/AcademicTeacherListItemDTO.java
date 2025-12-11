package org.fyp.tmssep490be.dtos.academicteacher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicTeacherListItemDTO {
    private Long teacherId;
    private String fullName;
    private String email;
    private String phone;
    private String employeeCode;
    private String avatarUrl;
    private String status;
    
    private Boolean hasSkills;
    private Integer totalSkills;
    private List<String> specializations;
}