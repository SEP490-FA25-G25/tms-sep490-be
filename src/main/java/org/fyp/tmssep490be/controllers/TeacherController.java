package org.fyp.tmssep490be.controllers;

import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.teacherclass.TeacherClassListItemDTO;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.TeacherClassService;
import org.fyp.tmssep490be.utils.TeacherContextHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher")
public class TeacherController {

    private final TeacherClassService teacherClassService;
    private final TeacherContextHelper teacherContextHelper;

    public TeacherController(TeacherClassService teacherClassService, TeacherContextHelper teacherContextHelper) {
        this.teacherClassService = teacherClassService;
        this.teacherContextHelper = teacherContextHelper;
    }

    //Endpoint to get all classes that the teacher is assigned
    @GetMapping("/classes")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<List<TeacherClassListItemDTO>>> getTeacherClasses(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        // Extract teacher ID from JWT token
        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
        
        // Get classes assigned to teacher by teacherId
        List<TeacherClassListItemDTO> classes = teacherClassService.getTeacherClasses(teacherId);
        
        // Return response
        return ResponseEntity.ok(
                ResponseObject.<List<TeacherClassListItemDTO>>builder()
                        .success(true)
                        .message("Classes retrieved successfully")
                        .data(classes)
                        .build());
    }
}
