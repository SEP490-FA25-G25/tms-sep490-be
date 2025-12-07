package org.fyp.tmssep490be.dtos.studentmanagement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentImportResult {
    private Long branchId;
    private String branchName;

    private int totalAttempted;
    private int successfulCreations;
    private int skippedExisting;    // Số student đã tồn tại (FOUND) - bỏ qua
    private int failedCreations;

    private List<CreatedStudentInfo> createdStudents;

    private Long importedBy;
    private OffsetDateTime importedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatedStudentInfo {
        private Long studentId;
        private String studentCode;
        private String fullName;
        private String email;
        private String defaultPassword;
    }
}