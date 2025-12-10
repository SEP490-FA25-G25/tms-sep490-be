package org.fyp.tmssep490be.controllers;

import org.fyp.tmssep490be.dtos.attendance.AttendanceMatrixDTO;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.qa.QASessionListResponse;
import org.fyp.tmssep490be.dtos.subject.SubjectDetailDTO;
import org.fyp.tmssep490be.dtos.teacher.TeacherProfileDTO;
import org.fyp.tmssep490be.dtos.teacherclass.ClassStudentDTO;
import org.fyp.tmssep490be.dtos.teacherclass.TeacherClassListItemDTO;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.AttendanceService;
import org.fyp.tmssep490be.services.TeacherClassService;
import org.fyp.tmssep490be.services.TeacherService;
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
    private final AttendanceService attendanceService;
    private final TeacherService teacherService;

    public TeacherController(TeacherClassService teacherClassService, TeacherContextHelper teacherContextHelper, AttendanceService attendanceService, TeacherService teacherService) {
        this.teacherClassService = teacherClassService;
        this.teacherContextHelper = teacherContextHelper;
        this.attendanceService = attendanceService;
        this.teacherService = teacherService;
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

    // Endpoint để lấy ma trận điểm danh cho lớp học của giáo viên
    // Sử dụng cho tab "Ma trận điểm danh" trong trang chi tiết lớp học
    @GetMapping("/classes/{classId}/matrix")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<AttendanceMatrixDTO>> getClassAttendanceMatrix(
            @PathVariable Long classId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        // Lấy ID giáo viên từ JWT token
        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
        
        // Lấy ma trận điểm danh
        AttendanceMatrixDTO data = attendanceService.getClassAttendanceMatrix(teacherId, classId);
        
        return ResponseEntity.ok(
                ResponseObject.<AttendanceMatrixDTO>builder()
                        .success(true)
                        .message("Attendance matrix retrieved successfully")
                        .data(data)
                        .build());
    }

    // Endpoint để lấy giáo trình (curriculum/syllabus) cho lớp học của giáo viên
    // Sử dụng cho tab "Giáo trình" trong trang chi tiết lớp học
    @GetMapping("/classes/{classId}/curriculum")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<SubjectDetailDTO>> getClassCurriculum(
            @PathVariable Long classId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        // Lấy ID giáo viên từ JWT token
        Long teacherId = teacherContextHelper.getTeacherId(userPrincipal);
        
        // Lấy giáo trình từ classId (lấy subjectId từ class rồi gọi SubjectService.getSubjectSyllabus)
        SubjectDetailDTO data = teacherClassService.getClassCurriculum(classId, teacherId);
        
        return ResponseEntity.ok(
                ResponseObject.<SubjectDetailDTO>builder()
                        .success(true)
                        .message("Class curriculum retrieved successfully")
                        .data(data)
                        .build());
    }

    // Endpoint để lấy thông tin profile của giáo viên hiện tại
    @GetMapping("/me/profile")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<TeacherProfileDTO>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        TeacherProfileDTO profile = teacherService.getMyProfile(userPrincipal.getId());
        return ResponseEntity.ok(
                ResponseObject.<TeacherProfileDTO>builder()
                        .success(true)
                        .message("Teacher profile retrieved successfully")
                        .data(profile)
                        .build());
    }
}
