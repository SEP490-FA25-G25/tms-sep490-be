package org.fyp.tmssep490be.dtos.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Phản hồi yêu cầu đặt lại mật khẩu")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordResponse {

    @Schema(description = "Thông điệp phản hồi", example = "Hướng dẫn đặt lại mật khẩu đã được gửi đến email của bạn")
    private String message;

    @Schema(description = "Trạng thái gửi email", example = "true")
    private boolean emailSent;

    @Schema(description = "Email đã gửi (đã che)", example = "nguy****@example.com")
    private String maskedEmail;
}