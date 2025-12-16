package org.fyp.tmssep490be.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.schedule.WeeklyScheduleResponseDTO;
import org.fyp.tmssep490be.dtos.studentrequest.*;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.exceptions.BusinessRuleException;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.fyp.tmssep490be.repositories.UserBranchesRepository;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.StudentRequestService;
import org.fyp.tmssep490be.services.StudentScheduleService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/academic-requests")
@RequiredArgsConstructor
@Slf4j
public class AcademicAffairsRequestController {

    private final StudentRequestService studentRequestService;
    private final StudentScheduleService studentScheduleService;
    private final UserBranchesRepository userBranchesRepository;
    private final UserAccountRepository userAccountRepository;

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<PagedAARequestResponseDTO>> getPendingRequests(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String requestType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sessionDateFrom,
            @RequestParam(required = false) String sessionDateTo,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "submittedAt,asc") String sort) {

        AARequestFilterDTO filter = AARequestFilterDTO.builder()
                .branchId(branchId)
                .requestType(requestType)
                .keyword(keyword)
                .sessionDateFrom(sessionDateFrom)
                .sessionDateTo(sessionDateTo)
                .page(page)
                .size(size)
                .sort(sort)
                .build();

        Page<AARequestResponseDTO> requests = studentRequestService.getPendingRequests(currentUser.getId(), filter);
        RequestSummaryDTO summary = studentRequestService.getRequestSummary(currentUser.getId(), filter);

        PagedAARequestResponseDTO response = PagedAARequestResponseDTO.builder()
                .content(requests.getContent())
                .pageable(requests.getPageable())
                .totalElements(requests.getTotalElements())
                .totalPages(requests.getTotalPages())
                .first(requests.isFirst())
                .last(requests.isLast())
                .summary(summary)
                .build();

        return ResponseEntity.ok(ResponseObject.success("Retrieved pending requests successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<Page<AARequestResponseDTO>>> getAllRequests(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String requestType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long decidedBy,
            @RequestParam(required = false) String sessionDateFrom,
            @RequestParam(required = false) String sessionDateTo,
            @RequestParam(required = false) String submittedDateFrom,
            @RequestParam(required = false) String submittedDateTo,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "submittedAt,desc") String sort) {

        AARequestFilterDTO filter = AARequestFilterDTO.builder()
                .branchId(branchId)
                .status(status)
                .requestType(requestType)
                .keyword(keyword)
                .decidedBy(decidedBy)
                .sessionDateFrom(sessionDateFrom)
                .sessionDateTo(sessionDateTo)
                .submittedDateFrom(submittedDateFrom)
                .submittedDateTo(submittedDateTo)
                .page(page)
                .size(size)
                .sort(sort)
                .build();

        Page<AARequestResponseDTO> requests = studentRequestService.getAllRequests(currentUser.getId(), filter);

        return ResponseEntity.ok(ResponseObject.success("Retrieved all requests successfully", requests));
    }

    @GetMapping("/staff")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<List<AAStaffDTO>>> getAAStaffForFilters(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        // Get current user's assigned branches
        List<Long> userBranchIds = userBranchesRepository.findBranchIdsByUserId(currentUser.getId());

        if (userBranchIds.isEmpty()) {
            throw new BusinessRuleException("ACCESS_DENIED",
                "User is not assigned to any branch. Contact administrator.");
        }

        // Find all AA users in the same branches (using role code "ACADEMIC_AFFAIR")
        List<UserAccount> aaStaff = userAccountRepository.findByRoleCodeAndBranches("ACADEMIC_AFFAIR", userBranchIds);

        // Map to simplified DTO
        List<AAStaffDTO> staffList = aaStaff.stream()
                .map(user -> AAStaffDTO.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(ResponseObject.success("Retrieved AA staff successfully", staffList));
    }

    @GetMapping("/{requestId}")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<StudentRequestDetailDTO>> getRequestDetails(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long requestId) {

        StudentRequestDetailDTO request = studentRequestService.getRequestDetailsForAA(requestId);

        return ResponseEntity.ok(ResponseObject.success("Retrieved request details successfully", request));
    }

    @PutMapping("/{requestId}/approve")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<StudentRequestResponseDTO>> approveRequest(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long requestId,
            @Valid @RequestBody ApprovalDTO approvalDTO) {

        Long decidedById = currentUser.getId();
        StudentRequestResponseDTO request = studentRequestService.approveRequest(requestId, decidedById, approvalDTO);

        return ResponseEntity.ok(ResponseObject.success("Request approved successfully", request));
    }

    @PutMapping("/{requestId}/reject")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<StudentRequestResponseDTO>> rejectRequest(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long requestId,
            @Valid @RequestBody RejectionDTO rejectionDTO) {

        Long decidedById = currentUser.getId();
        StudentRequestResponseDTO request = studentRequestService.rejectRequest(requestId, decidedById, rejectionDTO);

        return ResponseEntity.ok(ResponseObject.success("Request rejected successfully", request));
    }

    @GetMapping("/students/{studentId}/missed-sessions")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<MissedSessionsResponseDTO>> getStudentMissedSessions(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long studentId,
            @RequestParam(required = false) Integer weeksBack,
            @RequestParam(required = false) Boolean excludeRequested) {

        MissedSessionsResponseDTO response = studentRequestService.getMissedSessionsForStudent(
                studentId, weeksBack, excludeRequested);

        return ResponseEntity.ok(ResponseObject.success("Retrieved missed sessions successfully", response));
    }

    @GetMapping("/makeup-options")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<MakeupOptionsResponseDTO>> getMakeupOptionsForStudent(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam Long studentId,
            @RequestParam Long targetSessionId) {

        MakeupOptionsResponseDTO response = studentRequestService.getMakeupOptionsForStudent(
                studentId, targetSessionId);

        return ResponseEntity.ok(ResponseObject.success("Retrieved makeup options successfully", response));
    }

    @PostMapping("/absence/on-behalf")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<StudentRequestResponseDTO>> submitAbsenceOnBehalf(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody AbsenceRequestDTO dto) {

        StudentRequestResponseDTO response = studentRequestService.submitAbsenceRequestOnBehalf(
                currentUser.getId(), dto);

        return ResponseEntity.ok(ResponseObject.success("Absence request created and auto-approved", response));
    }

    @PostMapping("/makeup-requests/on-behalf")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<StudentRequestResponseDTO>> submitMakeupOnBehalf(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody MakeupRequestDTO dto) {

        StudentRequestResponseDTO response = studentRequestService.submitMakeupRequestOnBehalf(
                currentUser.getId(), dto);

        return ResponseEntity.ok(ResponseObject.success("Makeup request created and auto-approved", response));
    }

    @GetMapping("/students/{studentId}/transfer-eligibility")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<TransferEligibilityDTO>> getStudentTransferEligibility(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long studentId) {

        TransferEligibilityDTO response = studentRequestService.getTransferEligibilityForStudent(studentId);

        return ResponseEntity.ok(ResponseObject.success("Retrieved transfer eligibility successfully", response));
    }

    @GetMapping("/transfer-options")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<TransferOptionsResponseDTO>> getTransferOptions(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam Long currentClassId,
            @RequestParam(required = false) String targetModality,
            @RequestParam(required = false) Boolean scheduleOnly) {

        TransferOptionsResponseDTO response = studentRequestService.getTransferOptionsFlexible(
                currentClassId, targetModality, scheduleOnly);

        return ResponseEntity.ok(ResponseObject.success("Retrieved transfer options successfully", response));
    }

    @PostMapping("/transfer-requests/on-behalf")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<StudentRequestResponseDTO>> submitTransferOnBehalf(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody TransferRequestDTO dto) {

        StudentRequestResponseDTO response = studentRequestService.submitTransferRequestOnBehalf(
                currentUser.getId(), dto);

        return ResponseEntity.ok(ResponseObject.success("Transfer request created and auto-approved", response));
    }

    @GetMapping("/students/{studentId}/schedule")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<WeeklyScheduleResponseDTO>> getStudentSchedule(
            @PathVariable Long studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam(required = false) Long classId) {

        log.info("Academic Affairs requesting schedule for student: {}, week: {}, class: {}",
                studentId, weekStart, classId);

        if (weekStart == null) {
            weekStart = studentScheduleService.getCurrentWeekStart();
            log.debug("Using current week start: {}", weekStart);
        }

        // Validate weekStart is Monday
        if (weekStart.getDayOfWeek() != java.time.DayOfWeek.MONDAY) {
            log.warn("Invalid weekStart provided: {} (not a Monday)", weekStart);
            return ResponseEntity.badRequest().body(
                    ResponseObject.<WeeklyScheduleResponseDTO>builder()
                            .success(false)
                            .message("weekStart must be a Monday (ISO 8601 format: YYYY-MM-DD)")
                            .build()
            );
        }

        WeeklyScheduleResponseDTO schedule = studentScheduleService.getWeeklySchedule(studentId, weekStart, classId);

        return ResponseEntity.ok(
                ResponseObject.<WeeklyScheduleResponseDTO>builder()
                        .success(true)
                        .message("Student schedule retrieved successfully")
                        .data(schedule)
                        .build()
        );
    }
}
