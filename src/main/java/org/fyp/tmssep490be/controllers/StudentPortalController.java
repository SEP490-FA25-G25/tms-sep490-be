package org.fyp.tmssep490be.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.studentportal.StudentClassDTO;
import org.fyp.tmssep490be.dtos.studentportal.StudentTranscriptDTO;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.StudentPortalService;
import org.fyp.tmssep490be.utils.StudentContextHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students/{studentId}")
@RequiredArgsConstructor
@Slf4j
public class StudentPortalController {

    private final StudentPortalService studentPortalService;
    private final StudentContextHelper studentContextHelper;

    @GetMapping("/classes")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<Page<StudentClassDTO>>> getStudentClasses(
            @PathVariable Long studentId,
            @RequestParam(required = false) List<String> enrollmentStatus,
            @RequestParam(required = false) List<String> classStatus,
            @RequestParam(required = false) List<Long> branchId,
            @RequestParam(required = false) List<Long> courseId,
            @RequestParam(required = false) List<String> modality,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "enrollmentDate") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} retrieving classes for student: {}", currentUser.getId(), studentId);

        Long targetStudentId;
        boolean isAcademicAffair = currentUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ACADEMIC_AFFAIR"));

        if (isAcademicAffair) {
            targetStudentId = studentId;
            log.info("Academic Affairs accessing classes for student ID: {}", studentId);
        } else {
            targetStudentId = studentContextHelper.getStudentId(currentUser);
            if (studentId != null && !targetStudentId.equals(studentId)) {
                log.warn("Student {} attempted to access classes of student {}, using own ID instead",
                        targetStudentId, studentId);
            }
        }

        // Create pageable with sorting
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        // Get student classes (courseId treated as subjectId in new schema)
        Page<StudentClassDTO> classes = studentPortalService.getStudentClasses(
                targetStudentId, enrollmentStatus, classStatus, branchId, courseId, modality, pageable
        );

        return ResponseEntity.ok(ResponseObject.success(classes));
    }

    @GetMapping("/transcript")
    @PreAuthorize("hasAnyRole('STUDENT','MANAGER')")
    public ResponseEntity<ResponseObject<List<StudentTranscriptDTO>>> getStudentTranscript(
            @PathVariable Long studentId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} retrieving transcript for student: {}", currentUser.getId(), studentId);

        Long authStudentId = studentContextHelper.getStudentId(currentUser);
        if (!authStudentId.equals(studentId)) {
            log.warn("Overriding requested studentId {} with authenticated studentId {}", studentId, authStudentId);
        }

        List<StudentTranscriptDTO> transcript = studentPortalService.getStudentTranscript(authStudentId);

        return ResponseEntity.ok(ResponseObject.success(transcript));
    }
}
