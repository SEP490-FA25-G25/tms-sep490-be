package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.studentportal.ClassSessionsResponseDTO;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.StudentPortalService;
import org.fyp.tmssep490be.utils.StudentContextHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/student-portal/classes/{classId}")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")
public class StudentClassController {

    private final StudentPortalService studentPortalService;
    private final StudentContextHelper studentContextHelper;

    @GetMapping("/sessions")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<ClassSessionsResponseDTO>> getClassSessions(
             @PathVariable Long classId,
             @RequestParam(required = false) Long studentId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        Long authStudentId = studentContextHelper.getStudentId(currentUser);
        if (studentId != null && !authStudentId.equals(studentId)) {
            log.warn("Overriding requested studentId {} with authenticated studentId {}", studentId, authStudentId);
        }
        log.info("User {} retrieving sessions for class: {} and student: {}", currentUser.getId(), classId, authStudentId);

        ClassSessionsResponseDTO sessions = studentPortalService.getClassSessions(classId, authStudentId);

        return ResponseEntity.ok(ResponseObject.success(sessions));
    }
}
