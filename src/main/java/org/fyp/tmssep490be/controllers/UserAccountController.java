package org.fyp.tmssep490be.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.user.CreateUserRequest;
import org.fyp.tmssep490be.dtos.user.UpdateUserRequest;
import org.fyp.tmssep490be.dtos.user.UserResponse;
import org.fyp.tmssep490be.services.UserAccountService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserAccountController {

    private final UserAccountService userAccountService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse userResponse = userAccountService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long userId, @Valid @RequestBody UpdateUserRequest request) {
        UserResponse userResponse = userAccountService.updateUser(userId, request);
        return ResponseEntity.ok(userResponse);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        UserResponse userResponse = userAccountService.getUserById(userId);
        return ResponseEntity.ok(userResponse);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ResponseObject<Page<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long branch) {

        String[] sortParams = sort.split(",");
        Sort.Direction sortDirection = sortParams.length > 1
                ? Sort.Direction.fromString(sortParams[1])
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortParams[0]));

        Page<UserResponse> users = userAccountService.getAllUsers(pageable, search, role, status, branch);
        return ResponseEntity.ok(new ResponseObject<>(true, "Success", users));
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        UserResponse userResponse = userAccountService.getUserByEmail(email);
        return ResponseEntity.ok(userResponse);
    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUserStatus(@PathVariable Long userId, @RequestParam String status) {
        UserResponse userResponse = userAccountService.updateUserStatus(userId, status);
        return ResponseEntity.ok(userResponse);
    }

    @GetMapping("/check/email/{email}")
    public ResponseEntity<ResponseObject<Boolean>> checkEmailExists(@PathVariable String email) {
        boolean exists = userAccountService.checkEmailExists(email);
        return ResponseEntity.ok(new ResponseObject<>(true, "Success", exists));
    }

    @GetMapping("/check/phone/{phone}")
    public ResponseEntity<ResponseObject<Boolean>> checkPhoneExists(@PathVariable String phone) {
        boolean exists = userAccountService.checkPhoneExists(phone);
        return ResponseEntity.ok(new ResponseObject<>(true, "Success", exists));
    }
}
