package org.fyp.tmssep490be.controllers;

import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestConfigDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestListDTO;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.PolicyService;
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
    private final PolicyService policyService;

    public TeacherRequestController(TeacherRequestService teacherRequestService, PolicyService policyService) {
        this.teacherRequestService = teacherRequestService;
        this.policyService = policyService;
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

    //Endpoint để lấy cấu hình request cho giáo viên (từ policies)
    @GetMapping("/config")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<TeacherRequestConfigDTO>> getTeacherRequestConfig() {
        boolean requireResourceAtRescheduleCreate = policyService.getGlobalBoolean(
                "teacher.reschedule.require_resource_at_create", true);
        boolean requireResourceAtModalityChangeCreate = policyService.getGlobalBoolean(
                "teacher.modality_change.require_resource", true);
        int minDaysBeforeSession = policyService.getGlobalInt(
                "teacher.request.min_days_before_session", 1);
        int reasonMinLength = policyService.getGlobalInt(
                "teacher.request.reason_min_length", 15);
        int timeWindowDays = policyService.getGlobalInt(
                "teacher.session.suggestion.max_days", 14);

        TeacherRequestConfigDTO config = TeacherRequestConfigDTO.builder()
                .requireResourceAtRescheduleCreate(requireResourceAtRescheduleCreate)
                .requireResourceAtModalityChangeCreate(requireResourceAtModalityChangeCreate)
                .minDaysBeforeSession(minDaysBeforeSession)
                .reasonMinLength(reasonMinLength)
                .timeWindowDays(timeWindowDays)
                .build();

        return ResponseEntity.ok(ResponseObject.<TeacherRequestConfigDTO>builder()
                .success(true)
                .message("Teacher request configuration loaded successfully")
                .data(config)
                .build());
    }
}

