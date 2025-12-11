package org.fyp.tmssep490be.dtos.studentmanagement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.Gender;
import org.fyp.tmssep490be.entities.enums.UserStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStudentResponse {

    private Long studentId;
    private String studentCode;
    private Long userAccountId;
    private String email;
    private String fullName;
    private String phone;
    private Gender gender;
    private LocalDate dob;
    private Long branchId;
    private String branchName;
    private UserStatus status;
    private String defaultPassword; 
    private Integer skillAssessmentsCreated;
    private OffsetDateTime createdAt;
    private CreatedByInfo createdBy;
    
    @Builder.Default
    private boolean isExistingStudent = false;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatedByInfo {
        private Long userId;
        private String fullName;
    }
}
