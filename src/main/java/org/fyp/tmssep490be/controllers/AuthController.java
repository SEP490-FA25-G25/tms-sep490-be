package org.fyp.tmssep490be.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.auth.*;
import org.fyp.tmssep490be.services.AuthService;
import org.fyp.tmssep490be.services.ForgotPasswordService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Simplified authentication controller - JWT based, no session management
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final ForgotPasswordService forgotPasswordService;

    /**
     * Login endpoint
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ResponseObject<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);

        return ResponseEntity.ok(
                ResponseObject.<AuthResponse>builder()
                        .success(true)
                        .message("Login successful")
                        .data(authResponse)
                        .build()
        );
    }

    /**
     * Refresh token endpoint - validates JWT and issues new tokens
     * POST /api/v1/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<ResponseObject<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        AuthResponse authResponse = authService.refreshToken(request);

        return ResponseEntity.ok(
                ResponseObject.<AuthResponse>builder()
                        .success(true)
                        .message("Token refreshed successfully")
                        .data(authResponse)
                        .build()
        );
    }

    /**
     * Logout endpoint - clears security context (client should discard tokens)
     * POST /api/v1/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ResponseObject<Void>> logout() {
        log.info("Logout request received");
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(
                ResponseObject.<Void>builder()
                        .success(true)
                        .message("Logout successful - please discard your tokens")
                        .build()
        );
    }

    /**
     * Forgot password endpoint
     * POST /api/v1/auth/forgot-password
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ResponseObject<ForgotPasswordResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Forgot password request received for email: {}", request.getEmail());

        ForgotPasswordResponse response = forgotPasswordService.requestPasswordReset(request.getEmail());

        return ResponseEntity.ok(
                ResponseObject.<ForgotPasswordResponse>builder()
                        .success(true)
                        .message(response.getMessage())
                        .data(response)
                        .build()
        );
    }

    /**
     * Reset password endpoint
     * POST /api/v1/auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ResponseObject<ResetPasswordResponse>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        log.info("Reset password request received");

        // Validate password confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest()
                    .body(ResponseObject.<ResetPasswordResponse>builder()
                            .success(false)
                            .message("Mật khẩu xác nhận không khớp")
                            .build()
                    );
        }

        ResetPasswordResponse response = forgotPasswordService.resetPassword(
                request.getToken(),
                request.getNewPassword()
        );

        return ResponseEntity.ok(
                ResponseObject.<ResetPasswordResponse>builder()
                        .success(response.isSuccess())
                        .message(response.getMessage())
                        .data(response)
                        .build()
        );
    }
}
