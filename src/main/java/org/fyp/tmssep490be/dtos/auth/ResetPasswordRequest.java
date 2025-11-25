package org.fyp.tmssep490be.dtos.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Yêu cầu đặt lại mật khẩu với token")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {

    @Schema(description = "Token đặt lại mật khẩu", example = "eyJhbGciOiJIUzUxMiJ9...")
    @NotBlank(message = "Token không được để trống")
    private String token;

    @Schema(description = "Mật khẩu mới", example = "NewPassword123!")
    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
             message = "Mật khẩu phải có ít nhất 1 chữ hoa, 1 chữ thường và 1 số")
    private String newPassword;

    @Schema(description = "Xác nhận mật khẩu mới", example = "NewPassword123!")
    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    private String confirmPassword;
}