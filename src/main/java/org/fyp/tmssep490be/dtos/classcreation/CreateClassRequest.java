package org.fyp.tmssep490be.dtos.classcreation;

import jakarta.validation.constraints.*;
import lombok.*;
import org.fyp.tmssep490be.entities.enums.Modality;

import java.time.LocalDate;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateClassRequest {

    @NotNull(message = "Yêu cầu branch ID")
    private Long branchId;

    @NotNull(message = "Yêu cầu subject ID")
    private Long subjectId;

    // Mã lớp tùy chọn - sinh tự động nếu không có
    @Size(max = 50, message = "Mã lớp tối đa 50 ký tự")
    @Pattern(regexp = "^[A-Z0-9\\-]*$", message = "Mã lớp chỉ chứa chữ in hoa, số và gạch ngang")
    private String code;

    @NotBlank(message = "Yêu cầu tên lớp")
    @Size(max = 255, message = "Tên lớp tối đa 255 ký tự")
    private String name;

    @NotNull(message = "Yêu cầu hình thức học")
    private Modality modality;

    @NotNull(message = "Yêu cầu ngày bắt đầu")
    @Future(message = "Ngày bắt đầu phải trong tương lai")
    private LocalDate startDate;

    @NotEmpty(message = "Yêu cầu lịch học")
    @Size(min = 1, max = 7, message = "Lịch học phải có 1-7 ngày")
    private List<@NotNull Short> scheduleDays;

    @NotNull(message = "Yêu cầu sĩ số tối đa")
    @Min(value = 1, message = "Sĩ số tối thiểu là 1")
    @Max(value = 1000, message = "Sĩ số tối đa là 1000")
    private Integer maxCapacity;
}
