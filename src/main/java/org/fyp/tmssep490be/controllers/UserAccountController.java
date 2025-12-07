package org.fyp.tmssep490be.controllers;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.dtos.user.CreateUserRequest;
import org.fyp.tmssep490be.dtos.user.UserResponse;
import org.fyp.tmssep490be.services.UserAccountService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;

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



}