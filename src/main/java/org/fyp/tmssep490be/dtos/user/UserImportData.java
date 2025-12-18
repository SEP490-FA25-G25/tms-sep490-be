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
public class UserImportData {
    private String fullName;
    private String email;
    private String phone;
    private String role; // Role code: TEACHER, ACADEMIC_AFFAIR, etc.
    private String branchCode; // Branch code if applicable
    private String dob; // Date of birth (DD/MM/YYYY format)
    
    // Status fields for preview
    private String status; // CREATE, ERROR, SKIP
    private String errorMessage;
    
    // For mapping after preview
    @Builder.Default
    private boolean valid = false;
}
