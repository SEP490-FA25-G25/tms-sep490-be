package org.fyp.tmssep490be.dtos.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDetailDTO {
    // Common fields
    private Long id;
    private String name;
    private String code;
    private String description;

    // HEAD fields (Admin/Creator view)
    private CourseBasicInfoDTO basicInfo;
    private CourseStructureDTO structure;

    // Main fields (Student/Viewer view)
    private Long subjectId;
    private String subjectName;
    private Long levelId;
    private String levelName;
    private String logicalCourseCode;
    private Integer version;
    private Integer totalHours;

    private BigDecimal hoursPerSession;
    private String scoreScale;
    private String prerequisites;
    private String targetAudience;
    private String teachingMethods;
    private LocalDate effectiveDate;
    private String status;
    private String approvalStatus;
    private Integer totalSessions;
    private Integer totalMaterials;

    // Lists (Merged)
    private List<CoursePhaseDTO> phases;
    private List<CourseMaterialDTO> materials;
    private List<CourseCLODTO> clos;
    private List<CourseAssessmentDTO> assessments;
}
