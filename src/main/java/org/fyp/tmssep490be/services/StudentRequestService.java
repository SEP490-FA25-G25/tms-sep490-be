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

        StudentRequestType requestType = null;
        if (filter.getRequestType() != null) {
            try {
                requestType = StudentRequestType.valueOf(filter.getRequestType());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid request type: {}", filter.getRequestType());
            }
        }

        RequestStatus status = null;
        if (filter.getStatus() != null) {
            try {
                status = RequestStatus.valueOf(filter.getStatus());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status: {}", filter.getStatus());
            }
        }

        String searchTerm = (filter.getSearch() != null && !filter.getSearch().trim().isEmpty()) 
            ? filter.getSearch().trim() : null;

        // Simple sort
        Sort sort = Sort.by(Sort.Direction.DESC, "submittedAt");
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        // Convert to List for repository query
        List<StudentRequestType> requestTypes = requestType != null ? List.of(requestType) : null;
        List<RequestStatus> statuses = status != null ? List.of(status) : null;

        Page<StudentRequest> requests = studentRequestRepository.findStudentRequestsWithFilters(
            student.getId(), searchTerm, requestTypes, statuses, pageable);

        return requests.map(this::mapToStudentRequestResponseDTO);
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

        // Simple sort
        Sort sort = Sort.by(Sort.Direction.DESC, "submittedAt");
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        Page<StudentRequest> requests = studentRequestRepository.findPendingRequestsByBranches(
                RequestStatus.PENDING, targetBranchIds, pageable);
        
        // Apply additional filters in memory (Note: this may cause pagination count mismatch)
        List<StudentRequest> filteredRequests = requests.getContent().stream()
                .filter(request -> applyAdditionalFilters(request, filter))
                .toList();

        return new PageImpl<>(
                filteredRequests.stream().map(this::mapToAAResponseDTO).toList(),
                pageable,
                requests.getTotalElements()
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

        // Simple sort
        Sort sort = Sort.by(Sort.Direction.DESC, "submittedAt");
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        Page<StudentRequest> requests;
        if (filter.getDecidedBy() != null) {
            requests = studentRequestRepository.findAllRequestsByBranchesAndDecidedBy(
                    targetBranchIds, filter.getDecidedBy(), pageable);
        } else {
            requests = studentRequestRepository.findAllRequestsByBranches(
                    targetBranchIds, pageable);
        }

        // Apply additional filters in memory (Note: this may cause pagination count mismatch)
        List<StudentRequest> filteredRequests = requests.getContent().stream()
                .filter(request -> applyAdditionalFilters(request, filter))
                .toList();

        return new PageImpl<>(
                filteredRequests.stream().map(this::mapToAAResponseDTO).toList(),
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

        // Handle ABSENCE requests
        if (request.getRequestType().equals(StudentRequestType.ABSENCE) && request.getTargetSession() != null) {
            markSessionAsExcused(
                    request.getStudent(),
                    request.getTargetSession(),
                    String.format("Vắng có phép được duyệt lúc %s. Request ID: %d",
                            OffsetDateTime.now(), requestId)
            );
        }

        // Handle MAKEUP requests - validate capacity and create student session
        if (request.getRequestType().equals(StudentRequestType.MAKEUP)) {
            executeMakeupApproval(request);
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

    @Transactional
    public StudentRequestResponseDTO submitMakeupRequest(Long userId, MakeupRequestDTO dto) {
        log.info("Submitting makeup request for user {} - target: {}, makeup: {}",
                userId, dto.getTargetSessionId(), dto.getMakeupSessionId());

        Student student = studentRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user ID: " + userId));

        UserAccount submittedBy = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return submitMakeupRequestInternal(student.getId(), dto, submittedBy, false);
    }

    @Transactional
    public StudentRequestResponseDTO submitMakeupRequestOnBehalf(Long decidedById, MakeupRequestDTO dto) {
        log.info("AA user {} submitting makeup request on-behalf for student {} - target: {}, makeup: {}",
                decidedById, dto.getStudentId(), dto.getTargetSessionId(), dto.getMakeupSessionId());

        if (dto.getStudentId() == null) {
            throw new BusinessRuleException("MISSING_STUDENT_ID", "Student ID is required for on-behalf requests");
        }

        UserAccount submittedBy = userAccountRepository.findById(decidedById)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return submitMakeupRequestInternal(dto.getStudentId(), dto, submittedBy, true);
    }

    private StudentRequestResponseDTO submitMakeupRequestInternal(Long studentId, MakeupRequestDTO dto,
                                                                   UserAccount submittedBy, boolean autoApprove) {
        // 1. Validate target session exists and has ABSENT or EXCUSED attendance
        StudentSession.StudentSessionId targetSsId = new StudentSession.StudentSessionId(studentId, dto.getTargetSessionId());
        StudentSession targetStudentSession = studentSessionRepository.findById(targetSsId)
                .orElseThrow(() -> new ResourceNotFoundException("Target session not found for this student"));

        AttendanceStatus attendanceStatus = targetStudentSession.getAttendanceStatus();
        if (attendanceStatus != AttendanceStatus.ABSENT && attendanceStatus != AttendanceStatus.EXCUSED) {
            throw new BusinessRuleException("NOT_ABSENT", "Can only makeup absent or excused sessions");
        }

        Session targetSession = targetStudentSession.getSession();

        // 2. Check eligible timeframe (4 weeks limit)
        int makeupWeeksLimit = 4;
        long weeksAgo = ChronoUnit.WEEKS.between(targetSession.getDate(), LocalDate.now());
        if (weeksAgo > makeupWeeksLimit) {
            throw new BusinessRuleException("SESSION_TOO_OLD",
                    String.format("Session too old for makeup (limit: %d weeks)", makeupWeeksLimit));
        }

        // 3. Validate makeup session
        Session makeupSession = sessionRepository.findById(dto.getMakeupSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Makeup session not found"));

        if (makeupSession.getStatus() != SessionStatus.PLANNED) {
            throw new BusinessRuleException("INVALID_MAKEUP_STATUS", "Makeup session must be PLANNED");
        }

        if (makeupSession.getDate().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("PAST_SESSION", "Makeup session must be in the future");
        }

        // 4. CRITICAL: Validate subject session match (same content)
        if (targetSession.getSubjectSession() == null || makeupSession.getSubjectSession() == null) {
            throw new BusinessRuleException("MISSING_SUBJECT_SESSION", "Both sessions must have subject session defined");
        }

        if (!targetSession.getSubjectSession().getId().equals(makeupSession.getSubjectSession().getId())) {
            throw new BusinessRuleException("SUBJECT_SESSION_MISMATCH",
                    "Makeup session must have same content (subjectSessionId)");
        }

        // 5. Check capacity (for informational purposes only)
        // Note: We allow requests even if session is full - AA will decide whether to approve
        ClassEntity makeupClass = makeupSession.getClassEntity();
        long enrolledCount = studentSessionRepository.countBySessionId(makeupSession.getId());
        boolean isOverCapacity = enrolledCount >= makeupClass.getMaxCapacity();
        
        if (isOverCapacity) {
            if (autoApprove) {
                // AA on-behalf: Log but allow (AA consciously overriding capacity)
                log.warn("AA override: Creating makeup request for session {} at/over capacity ({}/{})",
                        makeupSession.getId(), enrolledCount, makeupClass.getMaxCapacity());
            } else {
                // Student self-service: Log but allow (AA will review and decide)
                log.info("Student requesting makeup for session {} at/over capacity ({}/{}) - AA will review",
                        makeupSession.getId(), enrolledCount, makeupClass.getMaxCapacity());
            }
        }

        // 6. Check schedule conflict
        List<Session> studentSessions = sessionRepository.findSessionsForStudentByDate(studentId, makeupSession.getDate());
        for (Session existing : studentSessions) {
            if (hasTimeOverlap(existing.getTimeSlotTemplate(), makeupSession.getTimeSlotTemplate())) {
                throw new BusinessRuleException("SCHEDULE_CONFLICT", "Schedule conflict with other classes");
            }
        }

        // 7. Check duplicate request
        boolean hasDuplicate = studentRequestRepository.existsByStudentIdAndTargetSessionIdAndRequestTypeAndStatusIn(
                studentId, dto.getTargetSessionId(), StudentRequestType.MAKEUP,
                List.of(RequestStatus.PENDING, RequestStatus.APPROVED));

        if (hasDuplicate) {
            throw new BusinessRuleException("DUPLICATE_REQUEST", "Duplicate makeup request for this session");
        }

        // 8. Create request
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        ClassEntity currentClass = classRepository.findById(dto.getCurrentClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        StudentRequest request = StudentRequest.builder()
                .student(student)
                .requestType(StudentRequestType.MAKEUP)
                .currentClass(currentClass)
                .targetSession(targetSession)
                .makeupSession(makeupSession)
                .requestReason(dto.getRequestReason())
                .note(dto.getNote())
                .status(autoApprove ? RequestStatus.APPROVED : RequestStatus.PENDING)
                .submittedBy(submittedBy)
                .submittedAt(OffsetDateTime.now())
                .build();

        // 9. If auto-approve (AA on-behalf)
        if (autoApprove) {
            request.setDecidedBy(submittedBy);
            request.setDecidedAt(OffsetDateTime.now());
        }

        request = studentRequestRepository.save(request);

        // 10. If auto-approved, create makeup student session immediately
        if (autoApprove) {
            executeMakeupApproval(request);
        }

        log.info("Makeup request created with ID: {} - Status: {}", request.getId(), request.getStatus());
        return mapToStudentRequestResponseDTO(request);
    }

    private void executeMakeupApproval(StudentRequest request) {
        log.info("Executing makeup approval for request {}", request.getId());

        // 1. Check capacity (informational only - AA has already seen the warning and decided to approve)
        long currentEnrolled = studentSessionRepository.countBySessionId(request.getMakeupSession().getId());
        int maxCapacity = request.getMakeupSession().getClassEntity().getMaxCapacity();
        
        if (currentEnrolled >= maxCapacity) {
            log.warn("CAPACITY OVERRIDE: AA approved makeup request {} for session {} which is at/over capacity ({}/{}). " +
                    "Student {} will be added despite full capacity.",
                    request.getId(), 
                    request.getMakeupSession().getId(), 
                    currentEnrolled, 
                    maxCapacity,
                    request.getStudent().getStudentCode());
        }

        // 2. Create StudentSession for the makeup session
        StudentSession.StudentSessionId makeupSsId = new StudentSession.StudentSessionId(
                request.getStudent().getId(),
                request.getMakeupSession().getId()
        );

        StudentSession makeupStudentSession = studentSessionRepository.findById(makeupSsId)
                .orElseGet(() -> StudentSession.builder()
                        .id(makeupSsId)
                        .student(request.getStudent())
                        .session(request.getMakeupSession())
                        .build());

        // 3. Mark as makeup session linking to original
        makeupStudentSession.setIsMakeup(true);
        makeupStudentSession.setOriginalSession(request.getTargetSession());
        makeupStudentSession.setAttendanceStatus(AttendanceStatus.PLANNED);
        makeupStudentSession.setNote("Makeup for session " + request.getTargetSession().getId());

        studentSessionRepository.save(makeupStudentSession);
        log.info("Created makeup StudentSession for student {} session {}", 
                request.getStudent().getId(), request.getMakeupSession().getId());
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
        
        // Get capacity information
        long enrolledCount = studentSessionRepository.countBySessionId(session.getId());
        Integer maxCapacity = session.getClassEntity() != null ? session.getClassEntity().getMaxCapacity() : null;
        
        // Get class information
        ClassEntity classEntity = session.getClassEntity();
        StudentRequestDetailDTO.ClassInfoDTO classInfo = null;
        if (classEntity != null) {
            classInfo = StudentRequestDetailDTO.ClassInfoDTO.builder()
                    .classId(classEntity.getId())
                    .classCode(classEntity.getCode())
                    .branchName(classEntity.getBranch() != null ? classEntity.getBranch().getName() : null)
                    .build();
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
                .enrolledCount(enrolledCount)
                .maxCapacity(maxCapacity)
                .classInfo(classInfo)
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

    public StudentRequestConfigDTO getStudentRequestConfig() {
        return StudentRequestConfigDTO.builder()
                .makeupLookbackWeeks(4)
                .makeupWeeksLimit(4)
                .maxTransfersPerCourse(2)
                .absenceLeadTimeDays(LEAD_TIME_DAYS)
                .reasonMinLength(10)
                .build();
    }

     public MissedSessionsResponseDTO getMissedSessions(Long userId, Integer weeksBack, Boolean excludeRequested) {
        Student student = studentRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user ID: " + userId));
        return getMissedSessionsForStudent(student.getId(), weeksBack, excludeRequested);
    }

    public MissedSessionsResponseDTO getMissedSessionsForStudent(Long studentId, Integer weeksBack, Boolean excludeRequested) {
        int lookbackWeeks = weeksBack != null ? weeksBack : 4;
        boolean excludeRequestedSessions = excludeRequested != null ? excludeRequested : true;

        LocalDate cutoffDate = LocalDate.now().minusWeeks(lookbackWeeks);
        LocalDate today = LocalDate.now();

        List<StudentSession> allSessions = studentSessionRepository.findAllByStudentId(studentId);
        
        List<MissedSessionDTO> missedSessions = allSessions.stream()
                .filter(ss -> ss.getSession() != null)
                .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.ABSENT 
                           || ss.getAttendanceStatus() == AttendanceStatus.EXCUSED)
                .filter(ss -> {
                    LocalDate sessionDate = ss.getSession().getDate();
                    return !sessionDate.isBefore(cutoffDate) && !sessionDate.isAfter(today);
                })
                .filter(ss -> ss.getSession().getStatus() != SessionStatus.CANCELLED)
                .map(ss -> mapToMissedSessionDTO(ss, excludeRequestedSessions))
                .filter(dto -> !excludeRequestedSessions || !dto.getHasExistingMakeupRequest())
                .sorted((a, b) -> b.getDate().compareTo(a.getDate())) // 
                .collect(Collectors.toList());

        return MissedSessionsResponseDTO.builder()
                .studentId(studentId)
                .totalCount(missedSessions.size())
                .missedSessions(missedSessions)
                .build();
    }

    public MakeupOptionsResponseDTO getMakeupOptions(Long userId, Long targetSessionId) {
        Student student = studentRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user ID: " + userId));

        return getMakeupOptionsForStudent(student.getId(), targetSessionId);
    }

    public MakeupOptionsResponseDTO getMakeupOptionsForStudent(Long studentId, Long targetSessionId) {
        Session targetSession = sessionRepository.findById(targetSessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Target session not found"));

        if (targetSession.getSubjectSession() == null) {
            throw new BusinessRuleException("INVALID_SESSION", "Target session must have subject session defined");
        }

        // Find makeup options with same subject session (same content)
        // Business Rule: Same branch → allow all modalities; Different branch → ONLINE only
        List<Session> makeupOptions = sessionRepository.findMakeupSessionOptions(
                targetSession.getSubjectSession().getId(),
                targetSessionId,
                targetSession.getClassEntity().getBranch().getId()
        );

        // Apply smart ranking and filtering
        List<MakeupOptionDTO> rankedOptions = makeupOptions.stream()
                .map(session -> mapToMakeupOptionDTO(session, targetSession, studentId))
                .filter(option -> option != null) // Filter out sessions with conflicts
                .sorted((a, b) -> {
                    // Primary: Sort by total score (higher is better)
                    int scoreCompare = b.getMatchScore().getTotalScore().compareTo(a.getMatchScore().getTotalScore());
                    if (scoreCompare != 0) return scoreCompare;
                    // Secondary: If scores are equal, prefer earlier date (closer to today)
                    return a.getDate().compareTo(b.getDate());
                })
                .collect(Collectors.toList());

        // Build target session info
        MakeupOptionsResponseDTO.TargetSessionInfo targetInfo = MakeupOptionsResponseDTO.TargetSessionInfo.builder()
                .sessionId(targetSession.getId())
                .subjectSessionId(targetSession.getSubjectSession().getId())
                .classId(targetSession.getClassEntity().getId())
                .classCode(targetSession.getClassEntity().getCode())
                .branchId(targetSession.getClassEntity().getBranch().getId())
                .branchName(targetSession.getClassEntity().getBranch().getName())
                .modality(targetSession.getClassEntity().getModality().name())
                .build();

        return MakeupOptionsResponseDTO.builder()
                .targetSessionId(targetSessionId)
                .targetSession(targetInfo)
                .makeupOptions(rankedOptions)
                .totalOptions(rankedOptions.size())
                .build();
    }

    private MissedSessionDTO mapToMissedSessionDTO(StudentSession studentSession, boolean checkExistingRequest) {
        Session session = studentSession.getSession();
        SubjectSession subjectSession = session.getSubjectSession();
        ClassEntity classEntity = session.getClassEntity();
        TimeSlotTemplate timeSlot = session.getTimeSlotTemplate();

        // Check if there's an existing absence request
        List<StudentRequest> absenceRequests = studentRequestRepository
                .findByStudentIdAndTargetSessionIdAndRequestType(
                        studentSession.getStudent().getId(),
                        session.getId(),
                        StudentRequestType.ABSENCE
                );

        StudentRequest absenceRequest = absenceRequests.stream()
                .filter(r -> r.getStatus() != RequestStatus.CANCELLED && r.getStatus() != RequestStatus.REJECTED)
                .findFirst()
                .orElse(null);

        // Check if there's an existing makeup request
        boolean hasExistingMakeupRequest = checkExistingRequest && studentRequestRepository
                .existsByStudentIdAndTargetSessionIdAndRequestTypeAndStatusIn(
                        studentSession.getStudent().getId(),
                        session.getId(),
                        StudentRequestType.MAKEUP,
                        List.of(RequestStatus.PENDING, RequestStatus.APPROVED)
                );

        int daysAgo = (int) ChronoUnit.DAYS.between(session.getDate(), LocalDate.now());

        return MissedSessionDTO.builder()
                .sessionId(session.getId())
                .date(session.getDate())
                .daysAgo(daysAgo)
                .subjectSessionNumber(subjectSession != null ? subjectSession.getSequenceNo() : null)
                .subjectSessionTitle(subjectSession != null ? subjectSession.getTopic() : null)
                .subjectSessionId(subjectSession != null ? subjectSession.getId() : null)
                .classInfo(MissedSessionDTO.ClassInfo.builder()
                        .classId(classEntity.getId())
                        .classCode(classEntity.getCode())
                        .className(classEntity.getName())
                        .branchId(classEntity.getBranch().getId())
                        .branchName(classEntity.getBranch().getName())
                        .modality(classEntity.getModality().name())
                        .build())
                .timeSlotInfo(MissedSessionDTO.TimeSlotInfo.builder()
                        .startTime(timeSlot != null ? timeSlot.getStartTime().toString() : null)
                        .endTime(timeSlot != null ? timeSlot.getEndTime().toString() : null)
                        .build())
                .attendanceStatus(studentSession.getAttendanceStatus().name())
                .hasExistingMakeupRequest(hasExistingMakeupRequest)
                .isExcusedAbsence(studentSession.getAttendanceStatus() == AttendanceStatus.EXCUSED)
                .absenceRequestId(absenceRequest != null ? absenceRequest.getId() : null)
                .absenceRequestStatus(absenceRequest != null ? absenceRequest.getStatus().name() : null)
                .build();
    }

    private MakeupOptionDTO mapToMakeupOptionDTO(Session session, Session targetSession, Long studentId) {
        // Check schedule conflict
        List<Session> conflicts = sessionRepository.findSessionsForStudentByDate(studentId, session.getDate());
        for (Session conflict : conflicts) {
            if (hasTimeOverlap(conflict.getTimeSlotTemplate(), session.getTimeSlotTemplate())) {
                return null; // Filter out conflicting sessions
            }
        }

        // Calculate match score
        boolean branchMatch = session.getClassEntity().getBranch().getId()
                .equals(targetSession.getClassEntity().getBranch().getId());
        boolean modalityMatch = session.getClassEntity().getModality()
                .equals(targetSession.getClassEntity().getModality());

        int score = 0;
        //Cùng chi nhánh thì + 10, khác thì không +
        if (branchMatch) score += 10;
        //Cùng hình thức học thì + 5, khác thì không cộng
        if (modalityMatch) score += 5;

        // Date proximity bonus
        long weeksUntil = ChronoUnit.WEEKS.between(LocalDate.now(), session.getDate());
        int dateProximityScore = (int) Math.max(0, 3 - weeksUntil); // +3 this week, +2 next, +1 in 2 weeks
        //weeksUntil = 0 -> 3-0 =3 -> điểm cao
        // tương tự như vậy weeksUntil = 3 -> 3-3 = 0 điểm vì quá xa
        score += dateProximityScore;

        // Capacity bonus
        long enrolled = studentSessionRepository.countBySessionId(session.getId());
        int availableSlots = session.getClassEntity().getMaxCapacity() - (int) enrolled;
        boolean capacityOk = availableSlots > 0;
        score += Math.min(1, availableSlots / 5); // +1 per 5 slots

        String priority = score >= 15 ? "HIGH" : (score >= 8 ? "MEDIUM" : "LOW");

        SubjectSession subjectSession = session.getSubjectSession();
        TimeSlotTemplate timeSlot = session.getTimeSlotTemplate();

        return MakeupOptionDTO.builder()
                .sessionId(session.getId())
                .date(session.getDate())
                .dayOfWeek(session.getDate().getDayOfWeek().name())
                .subjectSessionNumber(subjectSession != null ? subjectSession.getSequenceNo() : null)
                .subjectSessionTitle(subjectSession != null ? subjectSession.getTopic() : null)
                .subjectSessionId(subjectSession != null ? subjectSession.getId() : null)
                .classInfo(MakeupOptionDTO.ClassInfo.builder()
                        .classId(session.getClassEntity().getId())
                        .classCode(session.getClassEntity().getCode())
                        .className(session.getClassEntity().getName())
                        .branchId(session.getClassEntity().getBranch().getId())
                        .branchName(session.getClassEntity().getBranch().getName())
                        .modality(session.getClassEntity().getModality().name())
                        .availableSlots(availableSlots)
                        .maxCapacity(session.getClassEntity().getMaxCapacity())
                        .build())
                .timeSlotInfo(MakeupOptionDTO.TimeSlotInfo.builder()
                        .startTime(timeSlot != null ? timeSlot.getStartTime().toString() : null)
                        .endTime(timeSlot != null ? timeSlot.getEndTime().toString() : null)
                        .slotId(timeSlot != null ? timeSlot.getId() : null)
                        .slotName(timeSlot != null ? timeSlot.getName() : null)
                        .build())
                .availableSlots(availableSlots)
                .maxCapacity(session.getClassEntity().getMaxCapacity())
                .matchScore(MakeupOptionDTO.MatchScore.builder()
                        .branchMatch(branchMatch)
                        .modalityMatch(modalityMatch)
                        .capacityOk(capacityOk)
                        .dateProximityScore(dateProximityScore)
                        .totalScore(score)
                        .priority(priority)
                        .build())
                .build();
    }

    private boolean hasTimeOverlap(TimeSlotTemplate slot1, TimeSlotTemplate slot2) {
        if (slot1 == null || slot2 == null) return false;
        return !(slot1.getEndTime().isBefore(slot2.getStartTime()) || 
                 slot2.getEndTime().isBefore(slot1.getStartTime()));
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
