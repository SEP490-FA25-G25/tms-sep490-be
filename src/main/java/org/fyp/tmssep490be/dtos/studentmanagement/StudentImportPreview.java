package org.fyp.tmssep490be.dtos.studentmanagement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentImportPreview {
    private Long branchId;
    private String branchName;

    private List<StudentImportData> students;

    // Counts
    private int foundCount;      // Số student đã có trong hệ thống
    private int createCount;     // Số student sẽ tạo mới
    private int errorCount;      // Số student có lỗi
    private int totalValid;      // foundCount + createCount (nhưng chỉ createCount sẽ được tạo mới)

    private List<String> warnings;
    private List<String> errors;
}
