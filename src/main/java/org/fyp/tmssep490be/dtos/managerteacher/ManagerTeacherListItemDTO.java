package org.fyp.tmssep490be.dtos.managerteacher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.UserStatus;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerTeacherListItemDTO {
    private Long teacherId;
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String employeeCode;
    private UserStatus status;
    /**
     * Names of branches this teacher belongs to (limited to manager's branch scope).
     */
    private List<String> branchNames;
}

