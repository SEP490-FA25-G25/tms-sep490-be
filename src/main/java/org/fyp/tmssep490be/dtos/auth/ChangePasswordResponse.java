package org.fyp.tmssep490be.dtos.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO cho kết quả đổi mật khẩu
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordResponse {

    // Trạng thái thành công
    private boolean success;

    // Thông báo kết quả
    private String message;
}

