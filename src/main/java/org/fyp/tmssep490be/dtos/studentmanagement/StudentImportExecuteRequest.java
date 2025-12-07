package org.fyp.tmssep490be.dtos.studentmanagement;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentImportExecuteRequest {
    @NotNull(message = "Branch ID is required")
    private Long branchId;

    @NotNull(message = "Students list is required")
    private List<StudentImportData> students;

    // Optional: chỉ import những student được select (nếu null thì import tất cả CREATE status)
    private List<Integer> selectedIndices;
}
