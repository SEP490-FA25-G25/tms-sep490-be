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
@Schema(description = "Class information for session details")
public class ClassInfoDTO {

    @Schema(description = "Class ID", example = "10")
    private Long classId;

    @Schema(description = "Class code", example = "HN-FOUND-O1")
    private String classCode;

    @Schema(description = "Class name", example = "IELTS Foundation - Oct 2025")
    private String className;

    @Schema(description = "Course ID", example = "1")
    private Long courseId;

    @Schema(description = "Course name", example = "IELTS Foundation")
    private String courseName;

    @Schema(description = "Teacher ID", example = "50")
    private Long teacherId;

    @Schema(description = "Teacher name", example = "Mr. Nguyen Van A")
    private String teacherName;

    @Schema(description = "Branch ID", example = "1")
    private Long branchId;

    @Schema(description = "Branch name", example = "Hanoi Branch")
    private String branchName;

    @Schema(description = "Class modality", example = "OFFLINE")
    private Modality modality;
}