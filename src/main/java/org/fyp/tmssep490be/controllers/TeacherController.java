package org.fyp.tmssep490be.controllers;

import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.teacherclass.ClassStudentDTO;
import org.fyp.tmssep490be.dtos.teacherclass.TeacherClassListItemDTO;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.TeacherClassService;
import org.fyp.tmssep490be.utils.TeacherContextHelper;
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

    //Endpoint to retrieve a paginated list of students enrolled in a specific class
    @GetMapping("/classes/{classId}/students")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<Page<ClassStudentDTO>>> getClassStudents(
            @PathVariable Long classId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "enrolledAt") String sort,
            @RequestParam(defaultValue = "desc") String sortDir,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        // Create pageable with sort
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));

        // Get students enrolled in the class
        Page<ClassStudentDTO> students = teacherClassService.getClassStudents(classId, search, pageable);

        return ResponseEntity.ok(
                ResponseObject.<Page<ClassStudentDTO>>builder()
                        .success(true)
                        .message("Class students retrieved successfully")
                        .data(students)
                        .build());
    }
}
