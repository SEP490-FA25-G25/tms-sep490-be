package org.fyp.tmssep490be.controllers;

import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestListDTO;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.TeacherRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher-requests")
public class TeacherRequestController {

    private final TeacherRequestService teacherRequestService;

    public TeacherRequestController(TeacherRequestService teacherRequestService) {
        this.teacherRequestService = teacherRequestService;
    }

    //Endpoint để lấy tất cả yêu cầu của giáo viên hiện tại
    @GetMapping("/me")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<List<TeacherRequestListDTO>>> getMyRequests(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        // Lấy danh sách yêu cầu của giáo viên
        List<TeacherRequestListDTO> requests = teacherRequestService.getMyRequests(userPrincipal.getId());
        
        return ResponseEntity.ok(
                ResponseObject.<List<TeacherRequestListDTO>>builder()
                        .success(true)
                        .message("Requests retrieved successfully")
                        .data(requests)
                        .build());
    }
}

