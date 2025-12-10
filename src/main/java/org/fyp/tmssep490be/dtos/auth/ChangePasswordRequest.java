package org.fyp.tmssep490be.dtos.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO cho yêu cầu đổi mật khẩu khi người dùng đã đăng nhập
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    // Mật khẩu hiện tại
    @NotBlank(message = "Mật khẩu hiện tại không được để trống")
    private String currentPassword;

    // Mật khẩu mới
    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
             message = "Mật khẩu phải có ít nhất 1 chữ hoa, 1 chữ thường và 1 số")
    private String newPassword;

    // Xác nhận mật khẩu mới
    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    private String confirmPassword;
}

