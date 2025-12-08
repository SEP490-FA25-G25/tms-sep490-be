package org.fyp.tmssep490be.controllers;

import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestApproveDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestConfigDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestListDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.MySessionDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestRejectDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestResponseDTO;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.PolicyService;
import org.fyp.tmssep490be.services.TeacherRequestService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher-requests")
@Slf4j
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

    //Endpoint để lấy danh sách yêu cầu giáo viên cho Academic Staff với optional status filter
    @GetMapping("/staff")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<List<TeacherRequestListDTO>>> getRequestsForStaff(
            @RequestParam(value = "status", required = false) RequestStatus status,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        List<TeacherRequestListDTO> requests = teacherRequestService.getRequestsForStaff(status, userPrincipal.getId());
        
        return ResponseEntity.ok(
                ResponseObject.<List<TeacherRequestListDTO>>builder()
                        .success(true)
                        .message("Teacher requests loaded successfully")
                        .data(requests)
                        .build());
    }

    //Endpoint để lấy chi tiết request theo ID (cho cả TEACHER và ACADEMIC_AFFAIR)
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<TeacherRequestResponseDTO>> getRequestById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        log.info("Get request {} for user {}", id, userPrincipal.getId());

        // Kiểm tra quyền truy cập
        boolean isAcademicStaff = userPrincipal.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ACADEMIC_AFFAIR".equals(authority.getAuthority()));

        TeacherRequestResponseDTO response = isAcademicStaff
                ? teacherRequestService.getRequestForStaff(id)
                : teacherRequestService.getRequestById(id, userPrincipal.getId());

        return ResponseEntity.ok(ResponseObject.<TeacherRequestResponseDTO>builder()
                .success(true)
                .message("Request loaded successfully")
                .data(response)
                .build());
    }

    //Endpoint để duyệt yêu cầu giáo viên (Academic Staff)
    //Tất cả thông tin bắt buộc phải được điền khi approve
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<TeacherRequestResponseDTO>> approveRequest(
            @PathVariable Long id,
            @RequestBody @Valid TeacherRequestApproveDTO approveDTO,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        log.info("Approve request {} by academic staff {}", id, userPrincipal.getId());

        TeacherRequestResponseDTO response = teacherRequestService.approveRequest(id, approveDTO, userPrincipal.getId());

        return ResponseEntity.ok(ResponseObject.<TeacherRequestResponseDTO>builder()
                .success(true)
                .message("Request approved successfully")
                .data(response)
                .build());
    }

    //Endpoint để từ chối yêu cầu giáo viên (Academic Staff)
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<TeacherRequestResponseDTO>> rejectRequest(
            @PathVariable Long id,
            @RequestBody @Valid TeacherRequestRejectDTO rejectDTO,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        log.info("Reject request {} by academic staff {}", id, userPrincipal.getId());

        TeacherRequestResponseDTO response = teacherRequestService.rejectRequest(id, rejectDTO.getReason(), userPrincipal.getId());

        return ResponseEntity.ok(ResponseObject.<TeacherRequestResponseDTO>builder()
                .success(true)
                .message("Request rejected")
                .data(response)
                .build());
    }

    //Endpoint lấy buổi sắp tới của giáo viên
    @GetMapping("/my-sessions")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<List<MySessionDTO>>> getMyFutureSessions(
            @RequestParam(value = "days", required = false) Integer days,
            @RequestParam(value = "classId", required = false) Long classId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<MySessionDTO> sessions = teacherRequestService.getFutureSessionsForTeacher(
                userPrincipal.getId(), days, classId);

        return ResponseEntity.ok(ResponseObject.<List<MySessionDTO>>builder()
                .success(true)
                .message("Future sessions loaded successfully")
                .data(sessions)
                .build());
    }
}

