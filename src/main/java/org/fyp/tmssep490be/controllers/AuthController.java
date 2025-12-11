package org.fyp.tmssep490be.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.auth.*;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

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

    @PostMapping("/logout")
    public ResponseEntity<ResponseObject<Void>> logout() {
        log.info("Logout request received");

        return ResponseEntity.ok(
                ResponseObject.<Void>builder()
                        .success(true)
                        .message("Logout successful - please discard your tokens")
                        .build()
        );
    }

    // Endpoint đổi mật khẩu (cho người dùng đã đăng nhập)
    // POST /api/v1/auth/change-password
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResponseObject<ChangePasswordResponse>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("Yêu cầu đổi mật khẩu cho user: {}", currentUser.getId());

        // Xác minh mật khẩu xác nhận
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest()
                    .body(ResponseObject.<ChangePasswordResponse>builder()
                            .success(false)
                            .message("Mật khẩu xác nhận không khớp")
                            .build()
                    );
        }

        ChangePasswordResponse response = authService.changePassword(currentUser.getId(), request);

        if (!response.isSuccess()) {
            return ResponseEntity.badRequest()
                    .body(ResponseObject.<ChangePasswordResponse>builder()
                            .success(false)
                            .message(response.getMessage())
                            .data(response)
                            .build()
                    );
        }

        return ResponseEntity.ok(
                ResponseObject.<ChangePasswordResponse>builder()
                        .success(true)
                        .message(response.getMessage())
                        .data(response)
                        .build()
        );
    }

    // Endpoint quên mật khẩu
    // POST /api/v1/auth/forgot-password
    @PostMapping("/forgot-password")
    public ResponseEntity<ResponseObject<ForgotPasswordResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Yêu cầu quên mật khẩu cho email: {}", request.getEmail());

        ForgotPasswordResponse response = authService.requestPasswordReset(request);

        return ResponseEntity.ok(
                ResponseObject.<ForgotPasswordResponse>builder()
                        .success(true)
                        .message(response.getMessage())
                        .data(response)
                        .build()
        );
    }

    // Endpoint đặt lại mật khẩu
    // POST /api/v1/auth/reset-password
    @PostMapping("/reset-password")
    public ResponseEntity<ResponseObject<ResetPasswordResponse>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        log.info("Yêu cầu đặt lại mật khẩu");

        ResetPasswordResponse response = authService.resetPassword(request);

        if (!response.isSuccess()) {
            return ResponseEntity.badRequest()
                    .body(ResponseObject.<ResetPasswordResponse>builder()
                            .success(false)
                            .message(response.getMessage())
                            .data(response)
                            .build()
                    );
        }

        return ResponseEntity.ok(
                ResponseObject.<ResetPasswordResponse>builder()
                        .success(true)
                        .message(response.getMessage())
                        .data(response)
                        .build()
        );
    }

}
