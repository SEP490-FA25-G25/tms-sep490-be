package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.studentmanagement.*;
import org.fyp.tmssep490be.dtos.studentrequest.StudentClassDTO;
import org.fyp.tmssep490be.entities.Student;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.fyp.tmssep490be.exceptions.BusinessRuleException;
import org.fyp.tmssep490be.repositories.StudentRepository;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.StudentService;
import org.fyp.tmssep490be.services.StudentRequestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for student management operations
 * Provides endpoints for Academic Affairs staff to view and manage students
 */
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Student Management", description = "Student management APIs for Academic Affairs")
@SecurityRequirement(name = "bearerAuth")
public class StudentController {

    private final StudentService studentService;
    private final StudentRequestService studentRequestService;
    private final StudentRepository studentRepository;

    /**
     * Create a new student with auto-generated student code
     * POST /api/v1/students
     */
    @PostMapping
    @Operation(
            summary = "Create a new student",
            description = "Create a new student with auto-generated student code. Academic Affairs can add students to their accessible branches."
    )
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

    /**
     * Get list of students accessible to academic affairs user
     * Filters by user's branch assignments and applies search/filter criteria
     */
    @GetMapping
    @Operation(
            summary = "Get students list",
            description = "Retrieve paginated list of students accessible to the user with filtering options"
    )
    @PreAuthorize("hasRole('ROLE_ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<Page<StudentListItemDTO>>> getStudents(
            @Parameter(description = "Filter by branch ID(s). If not provided, uses user's accessible branches")
            @RequestParam(required = false) List<Long> branchIds,

            @Parameter(description = "Search term for student code, name, email, or phone")
            @RequestParam(required = false) String search,

            @Parameter(description = "Filter by user status (ACTIVE, INACTIVE, SUSPENDED)")
            @RequestParam(required = false) UserStatus status,

            @Parameter(description = "Filter by course ID - students who have enrolled in this course")
            @RequestParam(required = false) Long courseId,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort field and direction")
            @RequestParam(defaultValue = "fullName") String sort,

            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "asc") String sortDir,

            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requesting students list with filters: branchIds={}, search={}, status={}, courseId={}",
                currentUser.getId(), branchIds, search, status, courseId);

        // Create pageable with sort
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));

        Page<StudentListItemDTO> students = studentService.getStudents(
                branchIds, search, status, courseId, pageable, currentUser.getId()
        );

        return ResponseEntity.ok(ResponseObject.<Page<StudentListItemDTO>>builder()
                .success(true)
                .message("Students retrieved successfully")
                .data(students)
                .build());
    }

  /**
     * Get current student's own profile information
     * For students to view their personal academic information and statistics
     */
    @GetMapping("/me/profile")
    @Operation(
            summary = "Get my student profile",
            description = "Retrieve the current student's profile information including personal details, academic statistics, and current classes"
    )
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public ResponseEntity<ResponseObject<StudentProfileDTO>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("Student {} requesting their profile information", currentUser.getId());

        StudentProfileDTO studentProfile = studentService.getMyProfile(currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<StudentProfileDTO>builder()
                .success(true)
                .message("Student profile retrieved successfully")
                .data(studentProfile)
                .build());
    }

    /**
     * Get detailed information about a specific student
     * Includes enrollment history and current active classes
     */
    @GetMapping("/{studentId}")
    @Operation(
            summary = "Get student details",
            description = "Retrieve comprehensive information about a specific student including enrollment history and current classes"
    )
    @PreAuthorize("hasRole('ROLE_ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<StudentDetailDTO>> getStudentDetail(
            @Parameter(description = "Student ID")
            @PathVariable Long studentId,

            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requesting details for student {}", currentUser.getId(), studentId);

        StudentDetailDTO studentDetail = studentService.getStudentDetail(studentId, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<StudentDetailDTO>builder()
                .success(true)
                .message("Student details retrieved successfully")
                .data(studentDetail)
                .build());
    }

    /**
     * Get enrollment history for a specific student
     * Shows all classes the student has been enrolled in with status and progress
     */
    @GetMapping("/{studentId}/enrollments")
    @Operation(
            summary = "Get student enrollment history",
            description = "Retrieve comprehensive enrollment history for a specific student including current and past enrollments"
    )
    @PreAuthorize("hasRole('ROLE_ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<Page<StudentEnrollmentHistoryDTO>>> getStudentEnrollmentHistory(
            @Parameter(description = "Student ID")
            @PathVariable Long studentId,

            @Parameter(description = "Filter by branch ID(s)")
            @RequestParam(required = false) List<Long> branchIds,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort field and direction")
            @RequestParam(defaultValue = "enrolledAt") String sort,

            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "desc") String sortDir,

            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requesting enrollment history for student {}", currentUser.getId(), studentId);

        // Create pageable with sort
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));

        Page<StudentEnrollmentHistoryDTO> enrollmentHistory = studentService.getStudentEnrollmentHistory(
                studentId, branchIds, pageable, currentUser.getId()
        );

        return ResponseEntity.ok(ResponseObject.<Page<StudentEnrollmentHistoryDTO>>builder()
                .success(true)
                .message("Student enrollment history retrieved successfully")
                .data(enrollmentHistory)
                .build());
}
}