package org.fyp.tmssep490be.dtos.studentmanagement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.Gender;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentImportData {
    // From Excel - 7 columns
    private String fullName;
    private String email;
    private String phone;
    private String facebookUrl;
    private String address;
    private Gender gender;
    private LocalDate dob;

    private StudentImportStatus status;  
    private Long existingStudentId;      // Nếu FOUND - student đã tồn tại
    private String existingStudentCode;  // Nếu FOUND - mã sinh viên đã có
    private String errorMessage;         // Nếu ERROR
    private boolean needsBranchSync;    
    private String note;               

    public enum StudentImportStatus {
        FOUND,   // Student đã tồn tại trong hệ thống (tìm theo email)
        CREATE,  // Student mới, sẽ tạo mới
        ERROR    // Có lỗi validation (email invalid, thiếu required field, etc.)
    }
}
