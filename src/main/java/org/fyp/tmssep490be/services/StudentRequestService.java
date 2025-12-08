package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.studentrequest.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.entities.enums.StudentRequestType;
import org.fyp.tmssep490be.exceptions.BusinessRuleException;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudentRequestService {

    private static final int LEAD_TIME_DAYS = 2;
    private static final double ABSENCE_THRESHOLD_PERCENT = 20.0;

    private final StudentRequestRepository studentRequestRepository;
    private final StudentRepository studentRepository;
    private final UserBranchesRepository userBranchesRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final SessionRepository sessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassRepository classRepository;
    private final UserAccountRepository userAccountRepository;

    public Page<StudentRequestResponseDTO> getMyRequests(Long userId, RequestFilterDTO filter) {
        log.debug("Getting requests for student user {}", userId);

        Student student = studentRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, 
                    "Student not found for user ID: " + userId));

        String searchTerm = (filter.getSearch() != null && !filter.getSearch().trim().isEmpty()) 
            ? filter.getSearch().trim() : null;
        
        List<StudentRequestType> requestTypes = prepareRequestTypeFilter(filter);
        List<RequestStatus> statuses = prepareStatusFilter(filter);

        Sort sort = buildSortOrder(filter, statuses);
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        Page<StudentRequest> requests = studentRequestRepository.findStudentRequestsWithFilters(
            student.getId(), searchTerm, requestTypes, statuses, pageable);

        List<StudentRequestResponseDTO> dtoList = requests.getContent().stream()
                .map(this::mapToStudentRequestResponseDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, requests.getTotalElements());
    }

    public StudentRequestDetailDTO getRequestById(Long requestId, Long userId) {
        Student student = studentRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, 
                    "Student not found for user ID: " + userId));

        StudentRequest request = studentRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + requestId));

        if (!request.getStudent().getId().equals(student.getId())) {
            throw new BusinessRuleException("ACCESS_DENIED", "You can only view your own requests");
        }

        return mapToDetailDTO(request);
    }

    public StudentRequestDetailDTO getRequestDetailsForAA(Long requestId) {
        StudentRequest request = studentRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + requestId));

        StudentRequestDetailDTO detailDTO = mapToDetailDTO(request);

        if (request.getTargetSession() != null) {
            long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), request.getTargetSession().getDate());

            StudentRequestDetailDTO.AdditionalInfoDTO additionalInfo = StudentRequestDetailDTO.AdditionalInfoDTO.builder()
                    .daysUntilSession(daysUntil)
                    .studentAbsenceStats(calculateAbsenceStats(request.getStudent().getId(), request.getCurrentClass().getId()))
                    .previousRequests(calculatePreviousRequests(request.getStudent().getId()))
                    .build();

            detailDTO.setAdditionalInfo(additionalInfo);
        }

        return detailDTO;
    }

    private List<StudentRequestType> prepareRequestTypeFilter(RequestFilterDTO filter) {
        if (filter.getRequestTypeFilters() != null && !filter.getRequestTypeFilters().isEmpty()) {
            return filter.getRequestTypeFilters();
        }
        
        if (filter.getRequestType() != null) {
            try {
                return List.of(StudentRequestType.valueOf(filter.getRequestType()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid request type: {}", filter.getRequestType());
            }
        }
        
        return null;
    }

    private List<RequestStatus> prepareStatusFilter(RequestFilterDTO filter) {
        if (filter.getStatusFilters() != null && !filter.getStatusFilters().isEmpty()) {
            return filter.getStatusFilters();
        }
        
        if (filter.getStatus() != null) {
            try {
                return List.of(RequestStatus.valueOf(filter.getStatus()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status: {}", filter.getStatus());
            }
        }
        
        return null;
    }

    private Sort buildSortOrder(RequestFilterDTO filter, List<RequestStatus> statusFilter) {
        boolean hasStatusFilter = (statusFilter != null && !statusFilter.isEmpty());
        
        try {
            String[] sortParts = filter.getSort().split(",");
            String field = sortParts[0];
            String direction = sortParts.length > 1 ? sortParts[1] : "desc";
            Sort.Direction sortDirection = Sort.Direction.fromString(direction);
            
            if (!hasStatusFilter) {
                return Sort.by(
                    Sort.Order.asc("status"),  
                    new Sort.Order(sortDirection, field)
                );
            }
            
            return Sort.by(sortDirection, field);
            
        } catch (Exception e) {
            log.warn("Invalid sort format: {}, using default", filter.getSort());
            return !hasStatusFilter 
                ? Sort.by(Sort.Order.asc("status"), Sort.Order.desc("submittedAt"))
                : Sort.by(Sort.Direction.DESC, "submittedAt");
        }
    }

    public Page<AARequestResponseDTO> getPendingRequests(Long currentUserId, AARequestFilterDTO filter) {
        List<Long> userBranchIds = userBranchesRepository.findBranchIdsByUserId(currentUserId);
        if (userBranchIds.isEmpty()) {
            throw new CustomException(ErrorCode.CLASS_NO_BRANCH_ACCESS, "User is not assigned to any branch. Contact administrator.");
        }
        if (filter.getBranchId() != null && !userBranchIds.contains(filter.getBranchId())) {
            throw new CustomException(ErrorCode.BRANCH_ACCESS_DENIED, "Access denied to branch ID: " + filter.getBranchId());
        }
        List<Long> targetBranchIds = filter.getBranchId() != null ?
            List.of(filter.getBranchId()) : userBranchIds;

        Sort sort = Sort.by(Sort.Direction.fromString(filter.getSort().split(",")[1]),
                filter.getSort().split(",")[0]);
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        Page<StudentRequest> requests = studentRequestRepository.findPendingRequestsByBranches(
                RequestStatus.PENDING, targetBranchIds, pageable);
        
        List<StudentRequest> filteredRequests = requests.getContent().stream()
                .filter(request -> applyAdditionalFilters(request, filter))
                .toList();

        List<AARequestResponseDTO> dtoList = filteredRequests.stream()
                .map(this::mapToAAResponseDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(
                dtoList,
                pageable,
                requests.getTotalElements() // Keep original total for pagination
        );
    }

    public Page<AARequestResponseDTO> getAllRequests(Long currentUserId, AARequestFilterDTO filter) {
        log.debug("AA user {} getting all requests history", currentUserId);

        List<Long> userBranchIds = userBranchesRepository.findBranchIdsByUserId(currentUserId);
        if (userBranchIds.isEmpty()) {
            throw new CustomException(ErrorCode.CLASS_NO_BRANCH_ACCESS, "User is not assigned to any branch. Contact administrator.");
        }
        if (filter.getBranchId() != null && !userBranchIds.contains(filter.getBranchId())) {
            throw new CustomException(ErrorCode.BRANCH_ACCESS_DENIED, "Access denied to branch ID: " + filter.getBranchId());
        }
        List<Long> targetBranchIds = filter.getBranchId() != null ?
            List.of(filter.getBranchId()) : userBranchIds;

        Sort sort = Sort.by(Sort.Direction.fromString(filter.getSort().split(",")[1]),
                filter.getSort().split(",")[0]);
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        Page<StudentRequest> requests;
        if (filter.getDecidedBy() != null) {
            requests = studentRequestRepository.findAllRequestsByBranchesAndDecidedBy(
                    targetBranchIds, filter.getDecidedBy(), pageable);
        } else {
            requests = studentRequestRepository.findAllRequestsByBranches(
                    targetBranchIds, pageable);
        }

        List<StudentRequest> filteredRequests = requests.getContent().stream()
                .filter(request -> applyAdditionalFilters(request, filter))
                .toList();

        List<AARequestResponseDTO> dtoList = filteredRequests.stream()
                .map(this::mapToAAResponseDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(
                dtoList,
                pageable,
                requests.getTotalElements()
        );
    }

    public RequestSummaryDTO getRequestSummary(Long currentUserId, AARequestFilterDTO filter) {
        List<Long> userBranchIds = userBranchesRepository.findBranchIdsByUserId(currentUserId);

        if (userBranchIds.isEmpty()) {
            throw new CustomException(ErrorCode.CLASS_NO_BRANCH_ACCESS, "User is not assigned to any branch. Contact administrator.");
        }

        List<Long> targetBranchIds = filter.getBranchId() != null ?
            List.of(filter.getBranchId()) : userBranchIds;

        long totalPending = studentRequestRepository.countByStatusAndBranches(
                RequestStatus.PENDING, targetBranchIds);

        long absenceCount = studentRequestRepository.countByRequestTypeAndStatusAndBranches(
                StudentRequestType.ABSENCE, RequestStatus.PENDING, targetBranchIds);
        long makeupCount = studentRequestRepository.countByRequestTypeAndStatusAndBranches(
                StudentRequestType.MAKEUP, RequestStatus.PENDING, targetBranchIds);
        long transferCount = studentRequestRepository.countByRequestTypeAndStatusAndBranches(
                StudentRequestType.TRANSFER, RequestStatus.PENDING, targetBranchIds);

        return RequestSummaryDTO.builder()
                .totalPending((int) totalPending)
                .absenceRequests((int) absenceCount)
                .makeupRequests((int) makeupCount)
                .transferRequests((int) transferCount)
                .build();
    }

    private boolean applyAdditionalFilters(StudentRequest request, AARequestFilterDTO filter) {
        if (filter.getRequestType() != null) {
            StudentRequestType requestType = StudentRequestType.valueOf(filter.getRequestType());
            if (!request.getRequestType().equals(requestType)) {
                return false;
            }
        }

        if (filter.getStatus() != null) {
            RequestStatus status = RequestStatus.valueOf(filter.getStatus());
            if (!request.getStatus().equals(status)) {
                return false;
            }
        }

        if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
            String searchLower = filter.getKeyword().toLowerCase();
            boolean matchesStudentName = request.getStudent().getUserAccount().getFullName() != null &&
                    request.getStudent().getUserAccount().getFullName().toLowerCase().contains(searchLower);
            boolean matchesStudentCode = request.getStudent().getStudentCode() != null &&
                    request.getStudent().getStudentCode().toLowerCase().contains(searchLower);
            boolean matchesClassCode = request.getCurrentClass() != null && 
                    request.getCurrentClass().getCode() != null &&
                    request.getCurrentClass().getCode().toLowerCase().contains(searchLower);
            if (!matchesStudentName && !matchesStudentCode && !matchesClassCode) {
                return false;
            }
        }

        if (filter.getSessionDateFrom() != null && request.getTargetSession() != null) {
            LocalDate sessionDateFrom = LocalDate.parse(filter.getSessionDateFrom());
            if (request.getTargetSession().getDate().isBefore(sessionDateFrom)) {
                return false;
            }
        }

        if (filter.getSessionDateTo() != null && request.getTargetSession() != null) {
            LocalDate sessionDateTo = LocalDate.parse(filter.getSessionDateTo());
            if (request.getTargetSession().getDate().isAfter(sessionDateTo)) {
                return false;
            }
        }

        if (filter.getSubmittedDateFrom() != null && request.getSubmittedAt() != null) {
            LocalDate submittedDateFrom = LocalDate.parse(filter.getSubmittedDateFrom());
            if (request.getSubmittedAt().toLocalDate().isBefore(submittedDateFrom)) {
                return false;
            }
        }

        if (filter.getSubmittedDateTo() != null && request.getSubmittedAt() != null) {
            LocalDate submittedDateTo = LocalDate.parse(filter.getSubmittedDateTo());
            if (request.getSubmittedAt().toLocalDate().isAfter(submittedDateTo)) {
                return false;
            }
        }

        return true;
    }

    private StudentRequestResponseDTO mapToStudentRequestResponseDTO(StudentRequest request) {
        return StudentRequestResponseDTO.builder()
                .id(request.getId())
                .requestType(request.getRequestType() != null ? request.getRequestType().toString() : null)
                .status(request.getStatus() != null ? request.getStatus().toString() : null)
                .currentClass(mapClassToSummary(request.getCurrentClass()))
                .targetClass(mapClassToSummary(request.getTargetClass()))
                .targetSession(mapSessionToSummary(request.getTargetSession()))
                .makeupSession(mapSessionToSummary(request.getMakeupSession()))
                .effectiveDate(request.getEffectiveDate() != null ? request.getEffectiveDate().toString() : null)
                .requestReason(request.getRequestReason())
                .note(request.getNote())
                .submittedAt(request.getSubmittedAt())
                .submittedBy(mapUserToSummary(request.getSubmittedBy()))
                .decidedAt(request.getDecidedAt())
                .decidedBy(mapUserToSummary(request.getDecidedBy()))
                .build();
    }

    private AARequestResponseDTO mapToAAResponseDTO(StudentRequest request) {
        return AARequestResponseDTO.builder()
                .id(request.getId())
                .requestType(request.getRequestType() != null ? request.getRequestType().toString() : null)
                .status(request.getStatus() != null ? request.getStatus().toString() : null)
                .student(mapStudentToSummary(request.getStudent()))
                .currentClass(mapClassToSummaryWithBranch(request.getCurrentClass()))
                .targetClass(mapClassToSummaryWithBranch(request.getTargetClass()))
                .targetSession(mapSessionToSummaryWithTeacher(request.getTargetSession()))
                .makeupSession(mapSessionToSummaryWithTeacher(request.getMakeupSession()))
                .effectiveDate(request.getEffectiveDate() != null ? request.getEffectiveDate().toString() : null)
                .requestReason(request.getRequestReason())
                .note(request.getNote())
                .submittedAt(request.getSubmittedAt())
                .submittedBy(mapUserToAAUserSummary(request.getSubmittedBy()))
                .decidedAt(request.getDecidedAt())
                .decidedBy(mapUserToAAUserSummary(request.getDecidedBy()))
                .daysUntilSession(calculateDaysUntilSession(request.getTargetSession()))
                .attendanceStats(calculateAttendanceStats(request))
                .build();
    }

    private AARequestResponseDTO.AttendanceStatsDTO calculateAttendanceStats(StudentRequest request) {
        if (request.getStudent() == null || request.getCurrentClass() == null) {
            return null;
        }

        Long studentId = request.getStudent().getId();
        Long classId = request.getCurrentClass().getId();

        // Get total sessions count
        Long totalSessions = studentSessionRepository.countTotalSessionsForStudentInClass(studentId, classId);

        // Get attendance breakdown by status
        List<Object[]> attendanceCounts = studentSessionRepository.countAttendanceByStatusForStudentInClass(studentId, classId);

        // Convert to map for easy lookup
        Map<AttendanceStatus, Long> countByStatus = attendanceCounts.stream()
                .collect(Collectors.toMap(
                        row -> (AttendanceStatus) row[0],
                        row -> (Long) row[1]
                ));

        return AARequestResponseDTO.AttendanceStatsDTO.builder()
                .totalSessions(totalSessions != null ? totalSessions.intValue() : 0)
                .presentCount(countByStatus.getOrDefault(AttendanceStatus.PRESENT, 0L).intValue())
                .absentCount(countByStatus.getOrDefault(AttendanceStatus.ABSENT, 0L).intValue())
                .excusedCount(countByStatus.getOrDefault(AttendanceStatus.EXCUSED, 0L).intValue())
                .build();
    }

    private StudentRequestResponseDTO.ClassSummaryDTO mapClassToSummary(ClassEntity classEntity) {
        if (classEntity == null) return null;
        return StudentRequestResponseDTO.ClassSummaryDTO.builder()
                .id(classEntity.getId())
                .code(classEntity.getCode())
                .name(classEntity.getName())
                .build();
    }

    private AARequestResponseDTO.ClassSummaryDTO mapClassToSummaryWithBranch(ClassEntity classEntity) {
        if (classEntity == null) return null;
        return AARequestResponseDTO.ClassSummaryDTO.builder()
                .id(classEntity.getId())
                .code(classEntity.getCode())
                .name(classEntity.getName())
                .branch(classEntity.getBranch() != null ? 
                    AARequestResponseDTO.BranchSummaryDTO.builder()
                        .id(classEntity.getBranch().getId())
                        .name(classEntity.getBranch().getName())
                        .build()
                    : null)
                .build();
    }

    private StudentRequestResponseDTO.SessionSummaryDTO mapSessionToSummary(Session session) {
        if (session == null) return null;
        return StudentRequestResponseDTO.SessionSummaryDTO.builder()
                .id(session.getId())
                .date(session.getDate() != null ? session.getDate().toString() : null)
                .courseSessionNumber(session.getSubjectSession() != null ? session.getSubjectSession().getSequenceNo() : null)
                .courseSessionTitle(session.getSubjectSession() != null ? session.getSubjectSession().getTopic() : null)
                .timeSlot(session.getTimeSlotTemplate() != null ?
                    StudentRequestResponseDTO.TimeSlotSummaryDTO.builder()
                        .startTime(session.getTimeSlotTemplate().getStartTime() != null ? session.getTimeSlotTemplate().getStartTime().toString() : null)
                        .endTime(session.getTimeSlotTemplate().getEndTime() != null ? session.getTimeSlotTemplate().getEndTime().toString() : null)
                        .build()
                    : null)
                .build();
    }

    private AARequestResponseDTO.SessionSummaryDTO mapSessionToSummaryWithTeacher(Session session) {
        if (session == null) return null;
        
        // Get teaching slot info if available
        Teacher teacher = null;
        if (session.getTeachingSlots() != null && !session.getTeachingSlots().isEmpty()) {
            teacher = session.getTeachingSlots().iterator().next().getTeacher();
        }
        
        return AARequestResponseDTO.SessionSummaryDTO.builder()
                .id(session.getId())
                .date(session.getDate() != null ? session.getDate().toString() : null)
                .dayOfWeek(session.getDate() != null ? session.getDate().getDayOfWeek().toString() : null)
                .courseSessionNumber(session.getSubjectSession() != null ? session.getSubjectSession().getSequenceNo() : null)
                .courseSessionTitle(session.getSubjectSession() != null ? session.getSubjectSession().getTopic() : null)
                .timeSlot(session.getTimeSlotTemplate() != null ?
                    AARequestResponseDTO.TimeSlotSummaryDTO.builder()
                        .startTime(session.getTimeSlotTemplate().getStartTime() != null ? session.getTimeSlotTemplate().getStartTime().toString() : null)
                        .endTime(session.getTimeSlotTemplate().getEndTime() != null ? session.getTimeSlotTemplate().getEndTime().toString() : null)
                        .build()
                    : null)
                .status(session.getStatus() != null ? session.getStatus().toString() : null)
                .teacher(teacher != null ?
                    AARequestResponseDTO.TeacherSummaryDTO.builder()
                        .id(teacher.getId())
                        .fullName(teacher.getUserAccount() != null ? teacher.getUserAccount().getFullName() : null)
                        .email(teacher.getUserAccount() != null ? teacher.getUserAccount().getEmail() : null)
                        .build()
                    : null)
                .build();
    }

    private AARequestResponseDTO.StudentSummaryDTO mapStudentToSummary(Student student) {
        if (student == null) return null;
        return AARequestResponseDTO.StudentSummaryDTO.builder()
                .id(student.getId())
                .studentCode(student.getStudentCode())
                .fullName(student.getUserAccount() != null ? student.getUserAccount().getFullName() : null)
                .email(student.getUserAccount() != null ? student.getUserAccount().getEmail() : null)
                .phone(student.getUserAccount() != null ? student.getUserAccount().getPhone() : null)
                .build();
    }

    private StudentRequestResponseDTO.UserSummaryDTO mapUserToSummary(UserAccount user) {
        if (user == null) return null;
        return StudentRequestResponseDTO.UserSummaryDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }

    private AARequestResponseDTO.UserSummaryDTO mapUserToAAUserSummary(UserAccount user) {
        if (user == null) return null;
        return AARequestResponseDTO.UserSummaryDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }

    private Long calculateDaysUntilSession(Session session) {
        if (session == null || session.getDate() == null) return null;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), session.getDate());
    }

    @Transactional
    public StudentRequestResponseDTO submitAbsenceRequest(Long userId, AbsenceRequestDTO dto) {
        log.info("Submitting absence request for user {} and session {}", userId, dto.getTargetSessionId());

        Student student = studentRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user ID: " + userId));

        log.info("Resolved user ID {} to student ID {}", userId, student.getId());

        Session session = sessionRepository.findById(dto.getTargetSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        if (session.getClassEntity() == null || !session.getClassEntity().getId().equals(dto.getCurrentClassId())) {
            throw new BusinessRuleException("SESSION_CLASS_MISMATCH", "Session does not belong to the selected class");
        }

        if (session.getDate().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("PAST_SESSION", "Cannot request absence for past sessions");
        }

        if (!session.getStatus().equals(SessionStatus.PLANNED)) {
            throw new BusinessRuleException("INVALID_SESSION_STATUS", "Session must be in PLANNED status");
        }

        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndClassIdAndStatus(student.getId(), dto.getCurrentClassId(), EnrollmentStatus.ENROLLED);

        if (enrollment == null) {
            throw new BusinessRuleException("NOT_ENROLLED", "You are not enrolled in this class");
        }

        StudentSession.StudentSessionId studentSessionId = new StudentSession.StudentSessionId(student.getId(), dto.getTargetSessionId());
        boolean hasStudentSession = studentSessionRepository.findById(studentSessionId).isPresent();
        if (!hasStudentSession) {
            throw new BusinessRuleException("SESSION_NOT_ASSIGNED", "You are not assigned to this session");
        }

        if (hasDuplicateRequest(student.getId(), dto.getTargetSessionId(), StudentRequestType.ABSENCE)) {
            throw new BusinessRuleException("DUPLICATE_REQUEST", "Duplicate absence request for this session");
        }

        long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), session.getDate());
        if (daysUntil < LEAD_TIME_DAYS) {
            log.warn("Absence request submitted with insufficient lead time: {} days", daysUntil);
        }

        double absenceRate = calculateAbsenceRate(student.getId(), dto.getCurrentClassId());
        if (absenceRate > ABSENCE_THRESHOLD_PERCENT) {
            log.warn("Student absence rate {}% exceeds threshold", absenceRate);
        }

        ClassEntity classEntity = classRepository.findById(dto.getCurrentClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        UserAccount submittedBy = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        StudentRequest request = StudentRequest.builder()
                .student(student)
                .requestType(StudentRequestType.ABSENCE)
                .currentClass(classEntity)
                .targetSession(session)
                .requestReason(dto.getRequestReason())
                .note(dto.getNote())
                .status(RequestStatus.PENDING)
                .submittedBy(submittedBy)
                .submittedAt(OffsetDateTime.now())
                .build();

        request = studentRequestRepository.save(request);
        log.info("Absence request created successfully with id: {}", request.getId());

        return mapToStudentRequestResponseDTO(request);
    }

    @Transactional
    public StudentRequestResponseDTO approveRequest(Long requestId, Long decidedById, ApprovalDTO dto) {
        StudentRequest request = studentRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (!request.getStatus().equals(RequestStatus.PENDING)) {
            throw new BusinessRuleException("INVALID_STATUS", "Only pending requests can be approved");
        }

        UserAccount decidedBy = userAccountRepository.findById(decidedById)
                .orElseThrow(() -> new ResourceNotFoundException("Deciding user not found"));

        request.setStatus(RequestStatus.APPROVED);
        request.setDecidedBy(decidedBy);
        request.setDecidedAt(OffsetDateTime.now());
        request.setNote(dto.getNote());
        request = studentRequestRepository.save(request);

        if (request.getRequestType().equals(StudentRequestType.ABSENCE) && request.getTargetSession() != null) {
            markSessionAsExcused(
                    request.getStudent(),
                    request.getTargetSession(),
                    String.format("Vắng có phép được duyệt lúc %s. Request ID: %d",
                            OffsetDateTime.now(), requestId)
            );
        }

        log.info("Request {} approved by user {}", requestId, decidedById);

        return mapToStudentRequestResponseDTO(request);
    }

    @Transactional
    public StudentRequestResponseDTO rejectRequest(Long requestId, Long decidedById, RejectionDTO dto) {
        StudentRequest request = studentRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (!request.getStatus().equals(RequestStatus.PENDING)) {
            throw new BusinessRuleException("INVALID_STATUS", "Only pending requests can be rejected");
        }

        UserAccount decidedBy = userAccountRepository.findById(decidedById)
                .orElseThrow(() -> new ResourceNotFoundException("Deciding user not found"));

        request.setStatus(RequestStatus.REJECTED);
        request.setDecidedBy(decidedBy);
        request.setDecidedAt(OffsetDateTime.now());
        request.setNote(dto.getNote());
        request = studentRequestRepository.save(request);

        log.info("Request {} rejected by user {}", requestId, decidedById);

        return mapToStudentRequestResponseDTO(request);
    }

    private StudentRequestDetailDTO mapToDetailDTO(StudentRequest request) {
        // For TRANSFER requests, use effectiveSession instead of targetSession
        Session sessionToMap = request.getRequestType() == StudentRequestType.TRANSFER
                ? request.getEffectiveSession()
                : request.getTargetSession();

        String effectiveDateStr = request.getEffectiveDate() != null
                ? request.getEffectiveDate().toString()
                : null;

        return StudentRequestDetailDTO.builder()
                .id(request.getId())
                .requestType(request.getRequestType().toString())
                .status(request.getStatus().toString())
                .student(mapToDetailStudentSummary(request.getStudent()))
                .currentClass(mapToDetailClassDTO(request.getCurrentClass()))
                .targetClass(mapToDetailClassDTO(request.getTargetClass())) // For TRANSFER only
                .targetSession(mapToDetailSessionDTO(sessionToMap))
                .makeupSession(mapToDetailSessionDTO(request.getMakeupSession())) // For MAKEUP only
                .effectiveDate(effectiveDateStr) // For TRANSFER only
                .requestReason(request.getRequestReason())
                .note(request.getNote())
                .submittedAt(request.getSubmittedAt())
                .submittedBy(mapToDetailUserSummary(request.getSubmittedBy()))
                .decidedAt(request.getDecidedAt())
                .decidedBy(mapToDetailUserSummary(request.getDecidedBy()))
                .decisionNote(request.getNote()) // AA's decision note (used for both approve/reject)
                .build();
    }

    private StudentRequestDetailDTO.StudentSummaryDTO mapToDetailStudentSummary(Student student) {
        if (student == null) return null;
        return StudentRequestDetailDTO.StudentSummaryDTO.builder()
                .id(student.getId())
                .studentCode(student.getStudentCode())
                .fullName(student.getUserAccount() != null ? student.getUserAccount().getFullName() : null)
                .email(student.getUserAccount() != null ? student.getUserAccount().getEmail() : null)
                .phone(student.getUserAccount() != null ? student.getUserAccount().getPhone() : null)
                .build();
    }

    private StudentRequestDetailDTO.ClassDetailDTO mapToDetailClassDTO(ClassEntity classEntity) {
        if (classEntity == null) return null;
        
        return StudentRequestDetailDTO.ClassDetailDTO.builder()
                .id(classEntity.getId())
                .code(classEntity.getCode())
                .name(classEntity.getName())
                .branch(classEntity.getBranch() != null ?
                    StudentRequestDetailDTO.BranchSummaryDTO.builder()
                        .id(classEntity.getBranch().getId())
                        .name(classEntity.getBranch().getName())
                        .build()
                    : null)
                .teacher(null) // Teacher mapping to be implemented via TeachingSlot relationship
                .build();
    }

    private StudentRequestDetailDTO.SessionDetailDTO mapToDetailSessionDTO(Session session) {
        if (session == null) return null;
        
        // Get teacher from teaching slots if available
        Teacher teacher = null;
        if (session.getTeachingSlots() != null && !session.getTeachingSlots().isEmpty()) {
            teacher = session.getTeachingSlots().iterator().next().getTeacher();
        }
        
        return StudentRequestDetailDTO.SessionDetailDTO.builder()
                .id(session.getId())
                .date(session.getDate() != null ? session.getDate().toString() : null)
                .dayOfWeek(session.getDate() != null ? session.getDate().getDayOfWeek().toString() : null)
                .courseSessionNumber(session.getSubjectSession() != null ? session.getSubjectSession().getSequenceNo() : null)
                .courseSessionTitle(session.getSubjectSession() != null ? session.getSubjectSession().getTopic() : null)
                .timeSlot(session.getTimeSlotTemplate() != null ?
                    StudentRequestDetailDTO.TimeSlotDTO.builder()
                        .startTime(session.getTimeSlotTemplate().getStartTime() != null ? session.getTimeSlotTemplate().getStartTime().toString() : null)
                        .endTime(session.getTimeSlotTemplate().getEndTime() != null ? session.getTimeSlotTemplate().getEndTime().toString() : null)
                        .build()
                    : null)
                .status(session.getStatus() != null ? session.getStatus().toString() : null)
                .teacher(teacher != null ?
                    StudentRequestDetailDTO.TeacherSummaryDTO.builder()
                        .id(teacher.getId())
                        .fullName(teacher.getUserAccount() != null ? teacher.getUserAccount().getFullName() : null)
                        .email(teacher.getUserAccount() != null ? teacher.getUserAccount().getEmail() : null)
                        .build()
                    : null)
                .build();
    }

    private StudentRequestDetailDTO.UserSummaryDTO mapToDetailUserSummary(UserAccount user) {
        if (user == null) return null;
        return StudentRequestDetailDTO.UserSummaryDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }

    private StudentRequestDetailDTO.StudentAbsenceStatsDTO calculateAbsenceStats(Long studentId, Long classId) {
        List<StudentSession> sessions = studentSessionRepository.findByStudentIdAndClassEntityId(studentId, classId);

        LocalDate today = LocalDate.now();
        List<StudentSession> pastSessions = sessions.stream()
                .filter(ss -> ss.getSession() != null
                        && ss.getSession().getStatus() != SessionStatus.CANCELLED
                        && !ss.getSession().getDate().isAfter(today))
                .toList();

        long totalSessions = pastSessions.size();
        long excusedAbsences = pastSessions.stream()
                .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.EXCUSED)
                .count();
        long unexcusedAbsences = pastSessions.stream()
                .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.ABSENT)
                .count();
        
        return StudentRequestDetailDTO.StudentAbsenceStatsDTO.builder()
                .totalAbsences((int) (excusedAbsences + unexcusedAbsences))
                .totalSessions((int) totalSessions)
                .absenceRate(totalSessions > 0 ? (double) unexcusedAbsences / totalSessions * 100 : 0.0)
                .excusedAbsences((int) excusedAbsences)
                .unexcusedAbsences((int) unexcusedAbsences)
                .build();
    }

    private StudentRequestDetailDTO.PreviousRequestsDTO calculatePreviousRequests(Long studentId) {
        List<StudentRequest> requests = studentRequestRepository.findByStudentId(studentId);

        long total = requests.size();
        long approved = requests.stream().filter(r -> r.getStatus() == RequestStatus.APPROVED).count();
        long rejected = requests.stream().filter(r -> r.getStatus() == RequestStatus.REJECTED).count();
        long cancelled = requests.stream().filter(r -> r.getStatus() == RequestStatus.CANCELLED).count();

        return StudentRequestDetailDTO.PreviousRequestsDTO.builder()
                .totalRequests((int) total)
                .approvedRequests((int) approved)
                .rejectedRequests((int) rejected)
                .cancelledRequests((int) cancelled)
                .build();
    }

    public boolean hasDuplicateRequest(Long studentId, Long sessionId, StudentRequestType requestType) {
        return studentRequestRepository.existsByStudentIdAndTargetSessionIdAndRequestTypeAndStatusIn(
                studentId, sessionId, requestType,
                List.of(RequestStatus.PENDING, RequestStatus.APPROVED));
    }

    public double calculateAbsenceRate(Long studentId, Long classId) {
        List<StudentSession> sessions = studentSessionRepository.findByStudentIdAndClassEntityId(studentId, classId);
        if (sessions.isEmpty()) {
            return 0.0;
        }

        // Only count sessions that have occurred (date <= today)
        LocalDate today = LocalDate.now();
        List<StudentSession> pastSessions = sessions.stream()
                .filter(ss -> ss.getSession() != null
                        && ss.getSession().getStatus() != SessionStatus.CANCELLED
                        && !ss.getSession().getDate().isAfter(today))
                .toList();

        if (pastSessions.isEmpty()) {
            return 0.0;
        }

        long unexcusedAbsences = pastSessions.stream()
                .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.ABSENT)
                .count();

        return (double) unexcusedAbsences / pastSessions.size() * 100;
    }

    private void markSessionAsExcused(Student student, Session session, String note) {
        StudentSession.StudentSessionId id = new StudentSession.StudentSessionId(student.getId(), session.getId());
        StudentSession ss = studentSessionRepository.findById(id)
                .orElseGet(() -> StudentSession.builder()
                        .id(id)
                        .student(student)
                        .session(session)
                        .build());

        ss.setAttendanceStatus(AttendanceStatus.EXCUSED);
        ss.setNote(note);
        ss.setRecordedAt(OffsetDateTime.now());

        studentSessionRepository.save(ss);
    }
}
