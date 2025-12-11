package org.fyp.tmssep490be.dtos.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {

    // Token đặt lại mật khẩu
    @NotBlank(message = "Token đặt lại mật khẩu không được để trống")
    private String token;

    // Mật khẩu mới
    @NotBlank(message = "Mật khẩu mới không được để trống")
    private String newPassword;

    // Nhập lại mật khẩu mới để xác nhận
    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    private String confirmPassword;
}

