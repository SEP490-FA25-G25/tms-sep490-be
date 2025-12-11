package org.fyp.tmssep490be.dtos.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserImportPreview {
    private List<UserImportData> users;
    private int totalCount;
    private int validCount;
    private int errorCount;
}
