package org.fyp.tmssep490be.dtos.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordResponse {

    // Thông điệp phản hồi cho người dùng
    private String message;

    // Đánh dấu đã cố gắng gửi email
    private boolean emailSent;

    // Email đã được che bớt để bảo mật
    private String maskedEmail;
}

