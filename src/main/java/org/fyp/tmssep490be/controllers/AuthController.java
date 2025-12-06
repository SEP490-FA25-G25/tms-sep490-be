package org.fyp.tmssep490be.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.auth.*;
import org.fyp.tmssep490be.services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
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

}
