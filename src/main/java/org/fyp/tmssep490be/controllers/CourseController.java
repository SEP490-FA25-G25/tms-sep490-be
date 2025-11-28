package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.CourseDTO;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.course.*;
import org.fyp.tmssep490be.repositories.StudentRepository;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.CourseService;
import org.fyp.tmssep490be.services.MaterialAccessService;
import org.fyp.tmssep490be.services.StudentProgressService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for course operations
 * Provides endpoints for retrieving course information for dropdowns, student
 * views, and course management.
 */
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Course Management", description = "Course APIs for management and student information")
@SecurityRequirement(name = "bearerAuth")
public class CourseController {

        private final CourseService courseService;
        private final MaterialAccessService materialAccessService;
        private final StudentProgressService studentProgressService;
        private final StudentRepository studentRepository;

        /**
         * Get all courses for dropdown selection
         * GET /api/v1/courses
         */
        @GetMapping
        @Operation(summary = "Get all courses", description = "Retrieve all courses for dropdown selection. Returns id, name, and code.")
        public ResponseEntity<ResponseObject<List<CourseDTO>>> getAllCourses(
                        @RequestParam(required = false) Long subjectId,
                        @RequestParam(required = false) Long levelId) {
                log.info("Getting all courses for dropdown with filters - subjectId: {}, levelId: {}", subjectId,
                                levelId);

                List<CourseDTO> courses = courseService.getAllCourses(subjectId, levelId);

                return ResponseEntity.ok(ResponseObject.<List<CourseDTO>>builder()
                                .success(true)
                                .message("Courses retrieved successfully")
                                .data(courses)
                                .build());
        }

        @GetMapping("/student/{userId}")
        @Operation(summary = "Get student's enrolled courses", description = "Retrieve all courses that a student is currently enrolled in with progress information. Note: The path parameter is userId which will be used to find the corresponding student.")
        @PreAuthorize("hasRole('STUDENT') or hasRole('ROLE_ACADEMIC_AFFAIR')")
        public ResponseEntity<ResponseObject<List<StudentCourseDTO>>> getStudentCourses(
                        @Parameter(description = "User ID (will be used to find corresponding Student)") @PathVariable Long userId,

                        @AuthenticationPrincipal UserPrincipal currentUser) {
                // Require valid authentication
                if (currentUser == null) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body(ResponseObject.<List<StudentCourseDTO>>builder()
                                                        .success(false)
                                                        .message("Authentication required")
                                                        .build());
                }

                Long currentUserId = currentUser.getId();
                boolean isStudent = currentUser.getAuthorities().stream()
                                .anyMatch(auth -> auth.getAuthority().equals("ROLE_STUDENT"));

                // Students can only view their own courses
                if (isStudent && !currentUserId.equals(userId)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(ResponseObject.<List<StudentCourseDTO>>builder()
                                                        .success(false)
                                                        .message("Students can only view their own courses")
                                                        .build());
                }

                // Convert userId to studentId
                Long studentId = null;
                try {
                        studentId = studentRepository.findByUserAccountId(userId)
                                        .map(student -> student.getId())
                                        .orElse(null);
                } catch (Exception e) {
                        log.error("Error finding student for userId {}", userId, e);
                }

                if (studentId == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(ResponseObject.<List<StudentCourseDTO>>builder()
                                                        .success(false)
                                                        .message("Student not found for user ID: " + userId)
                                                        .build());
                }

                log.info("User {} requesting courses for student {} (userId: {})", currentUserId, studentId, userId);

                List<StudentCourseDTO> courses = courseService.getStudentCoursesByUserId(userId);

                return ResponseEntity.ok(ResponseObject.<List<StudentCourseDTO>>builder()
                                .success(true)
                                .message("Student courses retrieved successfully")
                                .data(courses)
                                .build());
        }

        @PostMapping
        @Operation(summary = "Create a new course (Syllabus)")
        @PreAuthorize("hasRole('SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<CourseDTO>> createCourse(@RequestBody CreateCourseRequestDTO request,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("Creating new course: {}", request.getBasicInfo().getName());
                Long userId = currentUser != null ? currentUser.getId() : null;
                CourseDTO createdCourse = courseService.createCourse(request, userId);
                return ResponseEntity.ok(ResponseObject.<CourseDTO>builder()
                                .success(true)
                                .message("Course created successfully")
                                .data(createdCourse)
                                .build());
        }

        @PutMapping("/{id}")
        @Operation(summary = "Update course")
        @PreAuthorize("hasRole('SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<CourseDetailDTO>> updateCourse(@PathVariable Long id,
                        @RequestBody CreateCourseRequestDTO request,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                log.info("Updating course with ID: {}", id);
                Long userId = currentUser != null ? currentUser.getId() : null;
                CourseDetailDTO updatedCourse = courseService.updateCourse(id, request, userId);
                return ResponseEntity.ok(ResponseObject.<CourseDetailDTO>builder()
                                .success(true)
                                .message("Course updated successfully")
                                .data(updatedCourse)
                                .build());
        }

        @PostMapping("/{id}/submit")
        @Operation(summary = "Submit course for approval")
        @PreAuthorize("hasRole('SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<Void>> submitCourse(@PathVariable Long id) {
                log.info("Submitting course with ID: {}", id);
                courseService.submitCourse(id);
                return ResponseEntity.ok(ResponseObject.<Void>builder()
                                .success(true)
                                .message("Course submitted for approval successfully")
                                .build());
        }

        @PostMapping("/{id}/approve")
        @Operation(summary = "Approve course")
        @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
        public ResponseEntity<ResponseObject<Void>> approveCourse(@PathVariable Long id) {
                log.info("Approving course with ID: {}", id);
                courseService.approveCourse(id);
                return ResponseEntity.ok(ResponseObject.<Void>builder()
                                .success(true)
                                .message("Course approved successfully")
                                .build());
        }

        @PostMapping("/{id}/reject")
        @Operation(summary = "Reject course")
        @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
        public ResponseEntity<ResponseObject<Void>> rejectCourse(@PathVariable Long id,
                        @RequestBody(required = false) String reason) {
                log.info("Rejecting course with ID: {}", id);
                courseService.rejectCourse(id, reason);
                return ResponseEntity.ok(ResponseObject.<Void>builder()
                                .success(true)
                                .message("Course rejected successfully")
                                .build());
        }

        @PatchMapping("/{id}/deactivate")
        @Operation(summary = "Deactivate course")
        @PreAuthorize("hasRole('SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<Void>> deactivateCourse(@PathVariable Long id) {
                log.info("Deactivating course with ID: {}", id);
                courseService.deactivateCourse(id);
                return ResponseEntity.ok(ResponseObject.<Void>builder()
                                .success(true)
                                .message("Course deactivated successfully")
                                .build());
        }

        @PatchMapping("/{id}/reactivate")
        @Operation(summary = "Reactivate course")
        @PreAuthorize("hasRole('SUBJECT_LEADER')")
        public ResponseEntity<ResponseObject<Void>> reactivateCourse(@PathVariable Long id) {
                log.info("Reactivating course with ID: {}", id);
                courseService.reactivateCourse(id);
                return ResponseEntity.ok(ResponseObject.<Void>builder()
                                .success(true)
                                .message("Course reactivated successfully")
                                .build());
        }

        @GetMapping("/{id}")
        @Operation(summary = "Get course details (Admin/Manager view)")
        @PreAuthorize("hasRole('SUBJECT_LEADER') or hasRole('MANAGER') or hasRole('ADMIN')")
        public ResponseEntity<ResponseObject<CourseDetailDTO>> getCourseDetails(@PathVariable Long id) {
                log.info("Getting course details for id: {}", id);
                CourseDetailDTO course = courseService.getCourseDetails(id);
                return ResponseEntity.ok(ResponseObject.<CourseDetailDTO>builder()
                                .success(true)
                                .message("Course details retrieved successfully")
                                .data(course)
                                .build());
        }

        @GetMapping("/{courseId}/detail")
        @Operation(summary = "Get course detail information (Student/Teacher view)", description = "Retrieve comprehensive course information including phases, sessions, materials, and assessments")
        @PreAuthorize("hasRole('STUDENT') or hasRole('ROLE_ACADEMIC_AFFAIR') or hasRole('TEACHER')")
        public ResponseEntity<ResponseObject<CourseDetailDTO>> getCourseDetail(
                        @Parameter(description = "Course ID") @PathVariable Long courseId,

                        @AuthenticationPrincipal UserPrincipal currentUser) {
                Long currentUserId = currentUser != null ? currentUser.getId() : 1L;
                log.info("User {} requesting details for course {}", currentUserId, courseId);

                CourseDetailDTO courseDetail = courseService.getCourseDetail(courseId);

                return ResponseEntity.ok(ResponseObject.<CourseDetailDTO>builder()
                                .success(true)
                                .message("Course details retrieved successfully")
                                .data(courseDetail)
                                .build());
        }

        @GetMapping("/{courseId}/syllabus")
        @Operation(summary = "Get course syllabus", description = "Retrieve course structure with phases and sessions")
        @PreAuthorize("hasRole('STUDENT') or hasRole('ROLE_ACADEMIC_AFFAIR') or hasRole('TEACHER')")
        public ResponseEntity<ResponseObject<CourseDetailDTO>> getCourseSyllabus(
                        @Parameter(description = "Course ID") @PathVariable Long courseId,

                        @AuthenticationPrincipal UserPrincipal currentUser) {
                Long currentUserId = currentUser != null ? currentUser.getId() : 1L;
                log.info("User {} requesting syllabus for course {}", currentUserId, courseId);

                CourseDetailDTO syllabus = courseService.getCourseSyllabus(courseId);

                return ResponseEntity.ok(ResponseObject.<CourseDetailDTO>builder()
                                .success(true)
                                .message("Course syllabus retrieved successfully")
                                .data(syllabus)
                                .build());
        }

        @GetMapping("/{courseId}/materials")
        @Operation(summary = "Get course materials hierarchy", description = "Retrieve hierarchical materials structure organized by course, phase, and session levels")
        @PreAuthorize("hasRole('STUDENT') or hasRole('ROLE_ACADEMIC_AFFAIR') or hasRole('TEACHER')")
        public ResponseEntity<ResponseObject<MaterialHierarchyDTO>> getCourseMaterials(
                        @Parameter(description = "Course ID") @PathVariable Long courseId,

                        @Parameter(description = "Student ID for access control (required for students)") @RequestParam(required = false) Long studentId,

                        @AuthenticationPrincipal UserPrincipal currentUser) {
                Long currentUserId = currentUser != null ? currentUser.getId() : 1L;
                boolean isStudent = currentUser != null &&
                                currentUser.getAuthorities().stream()
                                                .anyMatch(auth -> auth.getAuthority().equals("ROLE_STUDENT"));

                // Students must provide their own ID for access control
                if (isStudent && (studentId == null || !currentUserId.equals(studentId))) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(ResponseObject.<MaterialHierarchyDTO>builder()
                                                        .success(false)
                                                        .message("Student ID required for material access")
                                                        .build());
                }

                log.info("User {} requesting materials for course {}", currentUserId, courseId);

                MaterialHierarchyDTO materials = courseService.getCourseMaterials(courseId,
                                isStudent ? studentId : null);

                return ResponseEntity.ok(ResponseObject.<MaterialHierarchyDTO>builder()
                                .success(true)
                                .message("Course materials retrieved successfully")
                                .data(materials)
                                .build());
        }

        @GetMapping("/{courseId}/plos")
        @Operation(summary = "Get course program learning outcomes", description = "Retrieve PLOs associated with this course")
        @PreAuthorize("hasRole('STUDENT') or hasRole('ROLE_ACADEMIC_AFFAIR') or hasRole('TEACHER')")
        public ResponseEntity<ResponseObject<List<CoursePLODTO>>> getCoursePLOs(
                        @Parameter(description = "Course ID") @PathVariable Long courseId,

                        @AuthenticationPrincipal UserPrincipal currentUser) {
                Long currentUserId = currentUser != null ? currentUser.getId() : 1L;
                log.info("User {} requesting PLOs for course {}", currentUserId, courseId);

                List<CoursePLODTO> plos = courseService.getCoursePLOs(courseId);

                return ResponseEntity.ok(ResponseObject.<List<CoursePLODTO>>builder()
                                .success(true)
                                .message("Course PLOs retrieved successfully")
                                .data(plos)
                                .build());
        }

        @GetMapping("/{courseId}/clos")
        @Operation(summary = "Get course learning outcomes", description = "Retrieve CLOs associated with this course")
        @PreAuthorize("hasRole('STUDENT') or hasRole('ROLE_ACADEMIC_AFFAIR') or hasRole('TEACHER')")
        public ResponseEntity<ResponseObject<List<CourseCLODTO>>> getCourseCLOs(
                        @Parameter(description = "Course ID") @PathVariable Long courseId,

                        @AuthenticationPrincipal UserPrincipal currentUser) {
                Long currentUserId = currentUser != null ? currentUser.getId() : 1L;
                log.info("User {} requesting CLOs for course {}", currentUserId, courseId);

                List<CourseCLODTO> clos = courseService.getCourseCLOs(courseId);

                return ResponseEntity.ok(ResponseObject.<List<CourseCLODTO>>builder()
                                .success(true)
                                .message("Course CLOs retrieved successfully")
                                .data(clos)
                                .build());
        }

        @GetMapping("/student/{userId}/progress")
        @Operation(summary = "Get student course progress", description = "Retrieve comprehensive progress information for a student in a specific course. Note: The path parameter is userId which will be automatically mapped to studentId.")
        @PreAuthorize("hasRole('STUDENT') or hasRole('ROLE_ACADEMIC_AFFAIR')")
        public ResponseEntity<ResponseObject<CourseProgressDTO>> getStudentCourseProgress(
                        @Parameter(description = "User ID (will be mapped to Student ID)") @PathVariable Long userId,

                        @Parameter(description = "Course ID") @RequestParam Long courseId,

                        @AuthenticationPrincipal UserPrincipal currentUser) {
                Long currentUserId = currentUser != null ? currentUser.getId() : 1L;
                boolean isStudent = currentUser != null &&
                                currentUser.getAuthorities().stream()
                                                .anyMatch(auth -> auth.getAuthority().equals("ROLE_STUDENT"));

                // Students can only view their own progress
                if (currentUser != null && isStudent && !currentUserId.equals(userId)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(ResponseObject.<CourseProgressDTO>builder()
                                                        .success(false)
                                                        .message("Students can only view their own progress")
                                                        .build());
                }

                // Convert userId to studentId
                Long studentId = null;
                try {
                        studentId = studentRepository.findByUserAccountId(userId)
                                        .map(student -> student.getId())
                                        .orElse(null);
                } catch (Exception e) {
                        log.error("Error finding student for userId {}", userId, e);
                }

                if (studentId == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(ResponseObject.<CourseProgressDTO>builder()
                                                        .success(false)
                                                        .message("Student not found for user ID: " + userId)
                                                        .build());
                }

                log.info("User {} requesting progress for student {} (userId: {}) in course {}", currentUserId,
                                studentId, userId, courseId);

                CourseProgressDTO progress = studentProgressService.calculateProgress(studentId, courseId);

                return ResponseEntity.ok(ResponseObject.<CourseProgressDTO>builder()
                                .success(true)
                                .message("Student progress retrieved successfully")
                                .data(progress)
                                .build());
        }

        @GetMapping("/{courseId}/materials/{materialId}/accessible")
        @Operation(summary = "Check material access", description = "Check if a student can access a specific material")
        @PreAuthorize("hasRole('STUDENT')")
        public ResponseEntity<ResponseObject<Boolean>> checkMaterialAccess(
                        @Parameter(description = "Course ID") @PathVariable Long courseId,

                        @Parameter(description = "Material ID") @PathVariable Long materialId,

                        @Parameter(description = "Student ID") @RequestParam Long studentId,

                        @AuthenticationPrincipal UserPrincipal currentUser) {
                Long currentUserId = currentUser != null ? currentUser.getId() : 1L;

                // Students can only check their own access
                if (!currentUserId.equals(studentId)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(ResponseObject.<Boolean>builder()
                                                        .success(false)
                                                        .message("Students can only check their own material access")
                                                        .build());
                }

                log.info("User {} checking access to material {} for course {}", currentUserId, materialId, courseId);

                boolean hasAccess = materialAccessService.canAccessMaterial(studentId, materialId);

                return ResponseEntity.ok(ResponseObject.<Boolean>builder()
                                .success(true)
                                .message("Material access check completed")
                                .data(hasAccess)
                                .build());
        }
}
