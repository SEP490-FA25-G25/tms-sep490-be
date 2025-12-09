package org.fyp.tmssep490be.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.studentportal.StudentTranscriptDTO;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.StudentPortalService;
import org.fyp.tmssep490be.utils.StudentContextHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students/{studentId}")
@RequiredArgsConstructor
@Slf4j
public class StudentPortalController {

    private final StudentPortalService studentPortalService;
    private final StudentContextHelper studentContextHelper;

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
