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
    private String message;
    private boolean emailSent;
    private String maskedEmail;
}

