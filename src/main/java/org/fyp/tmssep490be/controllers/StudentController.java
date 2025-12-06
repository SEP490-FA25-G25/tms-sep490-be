package org.fyp.tmssep490be.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.studentmanagement.*;
import org.fyp.tmssep490be.repositories.StudentRepository;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Slf4j
public class StudentController {

    private final StudentService studentService;
    private final StudentRepository studentRepository;

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<CreateStudentResponse>> createStudent(
            @Valid @RequestBody CreateStudentRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        // For testing purposes, we'll use a mock user ID when currentUser is null
        Long userId = currentUser != null ? currentUser.getId() : 1L;
        log.info("User {} creating new student with email: {}", userId, request.getEmail());

        CreateStudentResponse response = studentService.createStudent(request, userId);

        log.info("Successfully created student with code: {} by user: {}",
                response.getStudentCode(), userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseObject.<CreateStudentResponse>builder()
                        .success(true)
                        .message("Student created successfully")
                        .data(response)
                        .build());
    }

}
