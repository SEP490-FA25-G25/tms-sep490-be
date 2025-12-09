package org.fyp.tmssep490be.controllers;

import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestApproveDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestConfigDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestCreateDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestListDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.MySessionDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.ModalityResourceSuggestionDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestRejectDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestResponseDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherListDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.ReplacementCandidateDTO;
import org.fyp.tmssep490be.dtos.schedule.TimeSlotDTO;
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
import org.springframework.web.bind.annotation.PostMapping;
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

    //Endpoint để giáo viên tạo yêu cầu
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<TeacherRequestResponseDTO>> createRequest(
            @RequestBody @Valid TeacherRequestCreateDTO createDTO,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Teacher {} creating request {}", userPrincipal.getId(), createDTO.getRequestType());

        TeacherRequestResponseDTO response = teacherRequestService.createRequest(createDTO, userPrincipal.getId());

        return ResponseEntity.ok(ResponseObject.<TeacherRequestResponseDTO>builder()
                .success(true)
                .message("Request created successfully")
                .data(response)
                .build());
    }

    //Gợi ý resource khả dụng cho MODALITY_CHANGE
    @GetMapping("/sessions/{sessionId}/modality-resources")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<List<ModalityResourceSuggestionDTO>>> suggestModalityResources(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<ModalityResourceSuggestionDTO> suggestions = teacherRequestService.suggestModalityResources(
                sessionId, userPrincipal.getId());

        return ResponseEntity.ok(ResponseObject.<List<ModalityResourceSuggestionDTO>>builder()
                .success(true)
                .message("Resources loaded successfully")
                .data(suggestions)
                .build());
    }

    // Gợi ý resource cho giáo vụ dựa trên session ID và teacher ID (MODALITY_CHANGE) - khi tạo request mới
    @GetMapping("/sessions/{sessionId}/modality/resources/staff")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<List<ModalityResourceSuggestionDTO>>> suggestModalityResourcesForStaffBySession(
            @PathVariable Long sessionId,
            @RequestParam Long teacherId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<ModalityResourceSuggestionDTO> suggestions = teacherRequestService.suggestModalityResourcesForStaffBySession(
                sessionId, teacherId, userPrincipal.getId());

        return ResponseEntity.ok(ResponseObject.<List<ModalityResourceSuggestionDTO>>builder()
                .success(true)
                .message("Resources loaded successfully")
                .data(suggestions)
                .build());
    }

    // Gợi ý resource cho giáo vụ dựa trên request ID (MODALITY_CHANGE)
    @GetMapping("/{requestId}/modality/resources/staff")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<List<ModalityResourceSuggestionDTO>>> suggestModalityResourcesForStaff(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<ModalityResourceSuggestionDTO> suggestions = teacherRequestService.suggestModalityResourcesForStaff(
                requestId, userPrincipal.getId());

        return ResponseEntity.ok(ResponseObject.<List<ModalityResourceSuggestionDTO>>builder()
                .success(true)
                .message("Resources loaded successfully")
                .data(suggestions)
                .build());
    }

    //Endpoint để gợi ý resource khả dụng cho MODALITY_CHANGE
    @GetMapping("/{sessionId}/modality/resources")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<List<ModalityResourceSuggestionDTO>>> suggestModalityResourcesAlias(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return suggestModalityResources(sessionId, userPrincipal);
    }

    //Endpoint để lấy cấu hình request cho giáo viên
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

    //Endpoint để lấy danh sách teachers cho academic staff
    @GetMapping("/staff/teachers")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<List<TeacherListDTO>>> getTeachersForStaff(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<TeacherListDTO> teachers = teacherRequestService.getTeachersForStaff(userPrincipal.getId());

        return ResponseEntity.ok(ResponseObject.<List<TeacherListDTO>>builder()
                .success(true)
                .message("Teachers loaded successfully")
                .data(teachers)
                .build());
    }

    //Endpoint để lấy danh sách sessions của teacher cho academic staff
    @GetMapping("/staff/teachers/{teacherId}/sessions")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<List<MySessionDTO>>> getTeacherSessionsForStaff(
            @PathVariable Long teacherId,
            @RequestParam(value = "days", required = false) Integer days,
            @RequestParam(value = "classId", required = false) Long classId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<MySessionDTO> sessions = teacherRequestService.getSessionsForTeacherByStaff(
                teacherId, days, classId, userPrincipal.getId());

        return ResponseEntity.ok(ResponseObject.<List<MySessionDTO>>builder()
                .success(true)
                .message("Sessions loaded successfully")
                .data(sessions)
                .build());
    }

    //Endpoint để tạo request cho teacher bởi academic staff (tự động approve)
    @PostMapping("/staff/create")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<TeacherRequestResponseDTO>> createRequestForTeacher(
            @Valid @RequestBody TeacherRequestCreateDTO createDTO,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        TeacherRequestResponseDTO request = teacherRequestService.createRequestForTeacherByStaff(
                createDTO, userPrincipal.getId());

        return ResponseEntity.ok(ResponseObject.<TeacherRequestResponseDTO>builder()
                .success(true)
                .message("Request created and approved successfully")
                .data(request)
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

    //Endpoint để lấy slots khả dụng cho RESCHEDULE (cho teacher)
    @GetMapping("/sessions/{sessionId}/reschedule/slots")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<List<TimeSlotDTO>>> getRescheduleSlots(
            @PathVariable Long sessionId,
            @RequestParam String date,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<TimeSlotDTO> slots = teacherRequestService.getRescheduleSlots(
                sessionId, date, userPrincipal.getId());

        return ResponseEntity.ok(ResponseObject.<List<TimeSlotDTO>>builder()
                .success(true)
                .message("Reschedule slots loaded successfully")
                .data(slots)
                .build());
    }

    //Endpoint để lấy slots khả dụng cho RESCHEDULE (cho academic staff - từ sessionId)
    @GetMapping("/sessions/{sessionId}/reschedule/slots/staff")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<List<TimeSlotDTO>>> getRescheduleSlotsForStaff(
            @PathVariable Long sessionId,
            @RequestParam String date,
            @RequestParam Long teacherId) {

        List<TimeSlotDTO> slots = teacherRequestService.getRescheduleSlotsForStaff(
                sessionId, date, teacherId);

        return ResponseEntity.ok(ResponseObject.<List<TimeSlotDTO>>builder()
                .success(true)
                .message("Reschedule slots loaded successfully")
                .data(slots)
                .build());
    }

    //Endpoint để lấy slots khả dụng cho RESCHEDULE (cho academic staff - từ requestId)
    @GetMapping("/{requestId}/reschedule/slots/staff")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<List<TimeSlotDTO>>> getRescheduleSlotsForStaffFromRequest(
            @PathVariable Long requestId) {

        List<TimeSlotDTO> slots = teacherRequestService.getRescheduleSlotsForStaffFromRequest(requestId);

        return ResponseEntity.ok(ResponseObject.<List<TimeSlotDTO>>builder()
                .success(true)
                .message("Reschedule slots loaded successfully")
                .data(slots)
                .build());
    }

    //Endpoint để lấy resources khả dụng cho RESCHEDULE (cho teacher)
    @GetMapping("/sessions/{sessionId}/reschedule/suggestions")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<List<ModalityResourceSuggestionDTO>>> getRescheduleResources(
            @PathVariable Long sessionId,
            @RequestParam String date,
            @RequestParam Long timeSlotId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<ModalityResourceSuggestionDTO> resources = teacherRequestService.getRescheduleResources(
                sessionId, date, timeSlotId, userPrincipal.getId());

        return ResponseEntity.ok(ResponseObject.<List<ModalityResourceSuggestionDTO>>builder()
                .success(true)
                .message("Reschedule resources loaded successfully")
                .data(resources)
                .build());
    }

    //Endpoint để lấy resources khả dụng cho RESCHEDULE (cho academic staff - từ sessionId)
    @GetMapping("/sessions/{sessionId}/reschedule/suggestions/staff")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<List<ModalityResourceSuggestionDTO>>> getRescheduleResourcesForStaff(
            @PathVariable Long sessionId,
            @RequestParam String date,
            @RequestParam Long timeSlotId,
            @RequestParam Long teacherId) {

        List<ModalityResourceSuggestionDTO> resources = teacherRequestService.getRescheduleResourcesForStaff(
                sessionId, date, timeSlotId, teacherId);

        return ResponseEntity.ok(ResponseObject.<List<ModalityResourceSuggestionDTO>>builder()
                .success(true)
                .message("Reschedule resources loaded successfully")
                .data(resources)
                .build());
    }

    //Endpoint để lấy resources khả dụng cho RESCHEDULE (cho academic staff - từ requestId)
    @GetMapping("/{requestId}/reschedule/suggestions/staff")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<List<ModalityResourceSuggestionDTO>>> getRescheduleResourcesForStaffFromRequest(
            @PathVariable Long requestId) {

        List<ModalityResourceSuggestionDTO> resources = teacherRequestService.getRescheduleResourcesForStaffFromRequest(requestId);

        return ResponseEntity.ok(ResponseObject.<List<ModalityResourceSuggestionDTO>>builder()
                .success(true)
                .message("Reschedule resources loaded successfully")
                .data(resources)
                .build());
    }

    //Endpoint để gợi ý giáo viên dạy thay cho REPLACEMENT request (cho teacher)
    @GetMapping("/{sessionId}/replacement/candidates")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<List<ReplacementCandidateDTO>>> suggestReplacementCandidates(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<ReplacementCandidateDTO> candidates = teacherRequestService.suggestReplacementCandidates(
                sessionId, userPrincipal.getId());

        return ResponseEntity.ok(ResponseObject.<List<ReplacementCandidateDTO>>builder()
                .success(true)
                .message("Replacement candidates loaded successfully")
                .data(candidates)
                .build());
    }

    //Endpoint để gợi ý giáo viên dạy thay cho REPLACEMENT request (cho academic staff)
    //Loại trừ các teacher đã từ chối request này
    @GetMapping("/staff/{requestId}/replacement/candidates")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<List<ReplacementCandidateDTO>>> suggestReplacementCandidatesForStaff(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<ReplacementCandidateDTO> candidates = teacherRequestService.suggestReplacementCandidatesForStaff(
                requestId, userPrincipal.getId());

        return ResponseEntity.ok(ResponseObject.<List<ReplacementCandidateDTO>>builder()
                .success(true)
                .message("Replacement candidates loaded successfully")
                .data(candidates)
                .build());
    }

    //Endpoint để giáo viên dạy thay xác nhận đồng ý dạy thay
    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<TeacherRequestResponseDTO>> confirmReplacementRequest(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        log.info("Confirm replacement request {} by teacher {}", id, userPrincipal.getId());

        String note = body != null ? body.get("note") : null;
        TeacherRequestResponseDTO response = teacherRequestService.confirmReplacementRequest(id, userPrincipal.getId(), note);

        return ResponseEntity.ok(ResponseObject.<TeacherRequestResponseDTO>builder()
                .success(true)
                .message("Replacement request confirmed successfully")
                .data(response)
                .build());
    }

    //Endpoint để giáo viên dạy thay từ chối dạy thay
    @PatchMapping("/{id}/decline")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<TeacherRequestResponseDTO>> declineReplacementRequest(
            @PathVariable Long id,
            @RequestBody @Valid TeacherRequestRejectDTO rejectDTO,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        log.info("Decline replacement request {} by teacher {}", id, userPrincipal.getId());

        TeacherRequestResponseDTO response = teacherRequestService.declineReplacementRequest(id, rejectDTO.getReason(), userPrincipal.getId());

        return ResponseEntity.ok(ResponseObject.<TeacherRequestResponseDTO>builder()
                .success(true)
                .message("Replacement request declined")
                .data(response)
                .build());
    }
}

