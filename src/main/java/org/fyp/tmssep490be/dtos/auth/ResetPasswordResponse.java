package org.fyp.tmssep490be.dtos.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Phản hồi đặt lại mật khẩu")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordResponse {

    @Schema(description = "Thông điệp phản hồi", example = "Mật khẩu đã được đặt lại thành công")
    private String message;

    @Schema(description = "Trạng thái thành công", example = "true")
    private boolean success;
}