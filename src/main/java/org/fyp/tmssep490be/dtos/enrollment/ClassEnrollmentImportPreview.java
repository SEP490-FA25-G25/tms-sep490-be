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

    // Students data
    private List<StudentEnrollmentData> students;
    private int foundCount;      // Số students đã có trong DB
    private int createCount;     // Số students sẽ tạo mới
    private int errorCount;      // Số students có lỗi
    private int totalValid;      // found + create

    // Capacity info
    private int currentEnrolled;
    private int maxCapacity;
    private int availableSlots;
    private boolean exceedsCapacity;
    private int exceededBy;  // Số lượng vượt quá (0 nếu không vượt)

    // Warnings
    private List<String> warnings;
    private List<String> errors;

    // Recommendation
    private EnrollmentRecommendation recommendation;
}

