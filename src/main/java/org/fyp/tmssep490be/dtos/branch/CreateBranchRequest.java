package org.fyp.tmssep490be.dtos.branch;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBranchRequest {

    @NotBlank(message = "Mã chi nhánh không được để trống")
    private String code;

    @NotBlank(message = "Tên chi nhánh không được để trống")
    private String name;

    private String address;
    private String city;
    private String district;
    private String phone;
    private String email;
    private LocalDate openingDate;
}
