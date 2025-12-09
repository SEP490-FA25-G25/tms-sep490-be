package org.fyp.tmssep490be.controllers;

import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.qa.QASessionListResponse;
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

    //Endpoint để lấy tất cả lớp học mà giáo viên được phân công
    @GetMapping("/classes")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<List<TeacherClassListItemDTO>>> getTeacherClasses(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        // Lấy ID giáo viên từ JWT token
        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
        
        // Lấy lớp học được phân công cho giáo viên theo teacherId
        List<TeacherClassListItemDTO> classes = teacherClassService.getTeacherClasses(teacherId);
        
        // Trả về response
        return ResponseEntity.ok(
                ResponseObject.<List<TeacherClassListItemDTO>>builder()
                        .success(true)
                        .message("Classes retrieved successfully")
                        .data(classes)
                        .build());
    }

    //Endpoint để lấy danh sách sinh viên đăng ký lớp học theo ID lớp học
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

        // Tạo pageable với sort
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));

        // Lấy sinh viên đăng ký lớp học theo ID lớp học
        Page<ClassStudentDTO> students = teacherClassService.getClassStudents(classId, search, pageable);

        return ResponseEntity.ok(
                ResponseObject.<Page<ClassStudentDTO>>builder()
                        .success(true)
                        .message("Class students retrieved successfully")
                        .data(students)
                        .build());
    }

    // Endpoint để lấy danh sách buổi học với metrics điểm danh và bài tập về nhà cho lớp học của giáo viên
    // Sử dụng cho tab "Buổi học" trong trang chi tiết lớp học
    @GetMapping("/classes/{classId}/sessions/metrics")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<QASessionListResponse>> getClassSessions(
            @PathVariable Long classId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        // Lấy ID giáo viên từ JWT token
        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
        
        // Lấy danh sách buổi học với metrics
        QASessionListResponse response = teacherClassService.getSessionsWithMetrics(classId, teacherId);
        
        return ResponseEntity.ok(
                ResponseObject.<QASessionListResponse>builder()
                        .success(true)
                        .message("Sessions with metrics retrieved successfully")
                        .data(response)
                        .build());
    }
}
