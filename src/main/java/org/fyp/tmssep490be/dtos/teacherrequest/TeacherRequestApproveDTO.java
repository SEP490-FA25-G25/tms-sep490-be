package org.fyp.tmssep490be.dtos.teacherrequest;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherRequestApproveDTO {

    // Giáo vụ có thể chọn lại giáo viên dạy thay
    private Long replacementTeacherId;

    // Giáo vụ có thể chọn lại ngày, giờ, phòng học
    private java.time.LocalDate newDate;
    private Long newTimeSlotId;
    
    // Giáo vụ có thể chọn lại phòng học
    private Long newResourceId;

    // Ghi chú của giáo vụ(Nếu cần)
    @Size(max = 1000, message = "Note must not exceed 1000 characters")
    private String note;
}

