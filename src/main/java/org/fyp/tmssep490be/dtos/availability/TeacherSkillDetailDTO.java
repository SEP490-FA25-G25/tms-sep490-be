package org.fyp.tmssep490be.dtos.availability;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherSkillDetailDTO {
    
    private String skill;
    
    private String specialization;
    
    private Integer level;
}

