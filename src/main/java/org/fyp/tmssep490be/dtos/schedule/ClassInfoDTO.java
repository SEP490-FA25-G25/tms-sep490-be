package org.fyp.tmssep490be.dtos.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.Modality;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassInfoDTO {

    private Long classId;

    private String classCode;

    private String className;

    private Long subjectId;

    private String subjectName;

    private Long teacherId;

    private String teacherName;

    private Long branchId;

    private String branchName;

    private String branchAddress;

    private Modality modality;
}
