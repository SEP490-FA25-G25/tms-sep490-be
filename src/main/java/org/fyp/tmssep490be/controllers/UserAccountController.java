package org.fyp.tmssep490be.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.user.CreateUserRequest;
import org.fyp.tmssep490be.dtos.user.UserResponse;
import org.fyp.tmssep490be.services.UserAccountService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * User account management controller with role-based access control
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserAccountController {

    private final UserAccountService userAccountService;

    /**
     * Create new user (ADMIN only)
     * POST /api/v1/users
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseObject<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {

        UserResponse userResponse = userAccountService.createUser(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ResponseObject.<UserResponse>builder()
                        .success(true)
                        .message("User created successfully")
                        .data(userResponse)
                        .build()
        );
    }

    /**
     * Get user by ID
     * GET /api/v1/users/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CENTER_HEAD')")
    public ResponseEntity<ResponseObject<UserResponse>> getUserById(@PathVariable Long id) {

        UserResponse userResponse = userAccountService.getUserById(id);

        return ResponseEntity.ok(
                ResponseObject.<UserResponse>builder()
                        .success(true)
                        .message("User retrieved successfully")
                        .data(userResponse)
                        .build()
        );
    }

    /**
     * Get user by email
     * GET /api/v1/users/email/{email}
     */
    @GetMapping("/email/{email}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CENTER_HEAD')")
    public ResponseEntity<ResponseObject<UserResponse>> getUserByEmail(@PathVariable String email) {

        UserResponse userResponse = userAccountService.getUserByEmail(email);

        return ResponseEntity.ok(
                ResponseObject.<UserResponse>builder()
                        .success(true)
                        .message("User retrieved successfully")
                        .data(userResponse)
                        .build()
        );
    }

    /**
     * Get all users with pagination and filters
     * GET /api/v1/users?page=0&size=20&sort=id,desc&search=keyword&role=TEACHER&status=ACTIVE
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CENTER_HEAD')")
    public ResponseEntity<ResponseObject<Page<UserResponse>>> getAllUsers(
            Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {

        Page<UserResponse> users = userAccountService.getAllUsers(pageable, search, role, status);

        return ResponseEntity.ok(
                ResponseObject.<Page<UserResponse>>builder()
                        .success(true)
                        .message("Users retrieved successfully")
                        .data(users)
                        .build()
        );
    }

    /**
     * Update user status
     * PATCH /api/v1/users/{id}/status
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseObject<UserResponse>> updateUserStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        UserResponse userResponse = userAccountService.updateUserStatus(id, status);

        return ResponseEntity.ok(
                ResponseObject.<UserResponse>builder()
                        .success(true)
                        .message("User status updated successfully")
                        .data(userResponse)
                        .build()
        );
    }

    /**
     * Delete user (soft delete)
     * DELETE /api/v1/users/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseObject<Void>> deleteUser(@PathVariable Long id) {

        userAccountService.deleteUser(id);

        return ResponseEntity.ok(
                ResponseObject.<Void>builder()
                        .success(true)
                        .message("User deleted successfully")
                        .build()
        );
    }

    /**
     * Check if email exists
     * GET /api/v1/users/check/email/{email}
     */
    @GetMapping("/check/email/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseObject<Boolean>> checkEmailExists(@PathVariable String email) {

        boolean exists = userAccountService.existsByEmail(email);

        return ResponseEntity.ok(
                ResponseObject.<Boolean>builder()
                        .success(true)
                        .message("Email availability checked")
                        .data(exists)
                        .build()
        );
    }
}
