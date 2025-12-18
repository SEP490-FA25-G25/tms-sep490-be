package org.fyp.tmssep490be.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.studentportal.StudentClassDTO;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/student-portal")
@RequiredArgsConstructor
@Slf4j
public class StudentPortalStudentController {

    private final StudentPortalService studentPortalService;
    private final StudentContextHelper studentContextHelper;

    @GetMapping("/classes")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<List<StudentClassDTO>>> getMyClasses(
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        Long studentId = studentContextHelper.getStudentId(currentUser);
        log.info("Student {} retrieving own classes", studentId);

        // Get ALL student classes (no pagination - realistic: 15-30 classes max)
        // Frontend will handle filtering and UI pagination
        List<StudentClassDTO> classes = studentPortalService.getStudentClasses(studentId);

        return ResponseEntity.ok(ResponseObject.success(classes));
    }
}
