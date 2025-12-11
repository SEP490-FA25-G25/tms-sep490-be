package org.fyp.tmssep490be.dtos.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordResponse {

    // Thông điệp phản hồi
    private String message;

    // Thành công hay thất bại
    private boolean success;
}

