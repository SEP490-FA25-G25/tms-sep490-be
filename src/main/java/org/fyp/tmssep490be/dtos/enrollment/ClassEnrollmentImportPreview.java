package org.fyp.tmssep490be.dtos.enrollment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassEnrollmentImportPreview {
    // Class info
    private Long classId;
    private String classCode;
    private String className;

    // Students data (FE sẽ tự filter và đếm)
    private List<StudentEnrollmentData> students;

    // Capacity info (chỉ giữ thông tin cơ bản, FE tự tính các giá trị khác)
    private int currentEnrolled;
    private int maxCapacity;
}

