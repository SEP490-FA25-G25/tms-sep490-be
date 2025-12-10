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

     // Nếu true: giáo viên bắt buộc phải chọn resource khi tạo RESCHEDULE.
     // Nếu false: giáo viên có thể bỏ qua bước chọn resource, staff sẽ chọn khi duyệt.
    private boolean requireResourceAtRescheduleCreate;

     // Nếu true: giáo viên bắt buộc phải chọn resource khi tạo MODALITY_CHANGE.
     // Nếu false: giáo viên có thể bỏ qua bước chọn resource, staff sẽ chọn khi duyệt.
    private boolean requireResourceAtModalityChangeCreate;

     // Độ dài tối thiểu của lý do yêu cầu (ký tự).
     // Giáo viên phải nhập lý do có độ dài tối thiểu X ký tự.
    private int reasonMinLength;

     // Số ngày tối đa cho time window (khoảng thời gian cho phép).
     // Session phải nằm trong vòng X ngày từ hôm nay.
    private int timeWindowDays;
}
