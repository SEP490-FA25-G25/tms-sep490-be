package org.fyp.tmssep490be.dtos.branch;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBranchRequest {

    @NotBlank(message = "Mã chi nhánh không được để trống")
    private String code;

    @NotBlank(message = "Tên chi nhánh không được để trống")
    private String name;

    private String address;
    private String city;
    private String district;
    
    @Pattern(regexp = "^$|^(0[2-9])[0-9]{8,9}$", message = "Số điện thoại phải bắt đầu bằng 0 và có 10-11 chữ số")
    private String phone;
    
    @Email(message = "Email không hợp lệ")
    private String email;
    
    private String status;
    private LocalDate openingDate;
}
