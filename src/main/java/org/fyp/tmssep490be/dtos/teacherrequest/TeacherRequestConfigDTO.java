package org.fyp.tmssep490be.dtos.teacherrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherRequestConfigDTO {

    /**
     * Nếu true: giáo viên bắt buộc phải chọn resource khi tạo RESCHEDULE.
     * Nếu false: giáo viên có thể bỏ qua bước chọn resource, staff sẽ chọn khi duyệt.
     */
    private boolean requireResourceAtRescheduleCreate;

    /**
     * Nếu true: giáo viên bắt buộc phải chọn resource khi tạo MODALITY_CHANGE.
     * Nếu false: giáo viên có thể bỏ qua bước chọn resource, staff sẽ chọn khi duyệt.
     */
    private boolean requireResourceAtModalityChangeCreate;
}


