package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.classes.ClassSearchCriteria;
import org.fyp.tmssep490be.dtos.schedule.WeeklyScheduleResponseDTO;
import org.fyp.tmssep490be.dtos.studentrequest.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.BusinessRuleException;
import org.fyp.tmssep490be.exceptions.DuplicateRequestException;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.StudentRequestService;
import org.fyp.tmssep490be.services.StudentScheduleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.LinkedHashSet;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentRequestServiceImpl implements StudentRequestService {

    private final StudentRequestRepository studentRequestRepository;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final SessionRepository sessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final UserAccountRepository userAccountRepository;
    private final StudentScheduleService studentScheduleService;

    // Configuration values (in real implementation, these would come from properties)
    private static final int LEAD_TIME_DAYS = 1;
    private static final double ABSENCE_THRESHOLD_PERCENT = 20.0;
    private static final int REASON_MIN_LENGTH = 10;

    @Override
    public Page<StudentRequestResponseDTO> getMyRequests(Long userId, RequestFilterDTO filter) {
        // Lookup actual student ID from user_account ID
        Student student = studentRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user ID: " + userId));

        List<RequestStatus> statuses = filter.getStatus() != null ?
                List.of(RequestStatus.valueOf(filter.getStatus())) :
                List.of(RequestStatus.values());

        Sort sort = Sort.by(Sort.Direction.fromString(filter.getSort().split(",")[1]),
                filter.getSort().split(",")[0]);
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        Page<StudentRequest> requests = studentRequestRepository.findByStudentIdAndStatusIn(
                student.getId(), statuses, pageable);

        return requests.map(this::mapToStudentResponseDTO);
    }

    @Override
    public StudentRequestDetailDTO getRequestById(Long requestId, Long userId) {
        // Lookup actual student ID from user_account ID
        Student student = studentRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user ID: " + userId));

        StudentRequest request = studentRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + requestId));

        if (!request.getStudent().getId().equals(student.getId())) {
            throw new BusinessRuleException("ACCESS_DENIED", "You can only view your own requests");
        }

        return mapToDetailDTO(request);
    }

    @Override
    @Transactional
    public StudentRequestResponseDTO submitAbsenceRequest(Long userId, AbsenceRequestDTO dto) {
        log.info("Submitting absence request for user {} and session {}", userId, dto.getTargetSessionId());

        // 0. Lookup actual student ID from user_account ID
        Student student = studentRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user ID: " + userId));

        log.info("Resolved user ID {} to student ID {}", userId, student.getId());

        // 1. Validate session exists and is future
        Session session = sessionRepository.findById(dto.getTargetSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        if (session.getDate().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("PAST_SESSION", "Cannot request absence for past sessions");
        }

        if (!session.getStatus().equals(SessionStatus.PLANNED)) {
            throw new BusinessRuleException("INVALID_SESSION_STATUS", "Session must be in PLANNED status");
        }

        // 2. Validate student enrollment
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndClassIdAndStatus(student.getId(), dto.getCurrentClassId(), EnrollmentStatus.ENROLLED);

        if (enrollment == null) {
            throw new BusinessRuleException("NOT_ENROLLED", "You are not enrolled in this class");
        }

        // 3. Check duplicate request
        if (hasDuplicateRequest(student.getId(), dto.getTargetSessionId(), StudentRequestType.ABSENCE)) {
            throw new DuplicateRequestException("Duplicate absence request for this session");
        }

        // 4. Check lead time (warning only)
        long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), session.getDate());
        if (daysUntil < LEAD_TIME_DAYS) {
            log.warn("Absence request submitted with insufficient lead time: {} days", daysUntil);
        }

        // 5. Check absence threshold (warning only)
        double absenceRate = calculateAbsenceRate(student.getId(), dto.getCurrentClassId());
        if (absenceRate > ABSENCE_THRESHOLD_PERCENT) {
            log.warn("Student absence rate {}% exceeds threshold", absenceRate);
        }

        // 6. Get entities
        ClassEntity classEntity = classRepository.findById(dto.getCurrentClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        UserAccount submittedBy = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 7. Create request
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

        // TODO: Send notification to Academic Affairs

        return mapToStudentResponseDTO(request);
    }

    @Override
    @Transactional
    public StudentRequestResponseDTO cancelRequest(Long requestId, Long userId) {
        // Lookup actual student ID from user_account ID
        Student student = studentRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user ID: " + userId));

        StudentRequest request = studentRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (!request.getStudent().getId().equals(student.getId())) {
            throw new BusinessRuleException("ACCESS_DENIED", "You can only cancel your own requests");
        }

        if (!request.getStatus().equals(RequestStatus.PENDING)) {
            throw new BusinessRuleException("INVALID_STATUS", "Only pending requests can be cancelled");
        }

        request.setStatus(RequestStatus.CANCELLED);
        request = studentRequestRepository.save(request);

        log.info("Request {} cancelled by student {} (user {})", requestId, student.getId(), userId);
        return mapToStudentResponseDTO(request);
    }

    @Override
    public List<SessionAvailabilityDTO> getAvailableSessionsForDate(Long userId, LocalDate date, StudentRequestType requestType) {
        // Lookup actual student ID from user_account ID
        Student student = studentRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user ID: " + userId));

        // Find all sessions for the student's classes on the given date
        List<Session> sessions = sessionRepository.findSessionsForStudentByDate(student.getId(), date);

        return sessions.stream()
                .collect(Collectors.groupingBy(session -> session.getClassEntity()))
                .entrySet().stream()
                .map(entry -> {
                    ClassEntity classEntity = entry.getKey();
                    List<Session> classSessions = entry.getValue();

                    return SessionAvailabilityDTO.builder()
                            .classId(classEntity.getId())
                            .classCode(classEntity.getCode())
                            .className(classEntity.getName())
                            .courseId(classEntity.getCourse().getId())
                            .courseName(classEntity.getCourse().getName())
                            .branchId(classEntity.getBranch().getId())
                            .branchName(classEntity.getBranch().getName())
                            .modality(classEntity.getModality().toString())
                            .sessionCount(classSessions.size())
                            .sessions(classSessions.stream()
                                    .map(this::mapToSessionDTO)
                                    .collect(Collectors.toList()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public Page<AARequestResponseDTO> getPendingRequests(AARequestFilterDTO filter) {
        Sort sort = Sort.by(Sort.Direction.fromString(filter.getSort().split(",")[1]),
                filter.getSort().split(",")[0]);

        // Fetch all pending requests without pagination for filtering
        List<StudentRequest> allRequests = studentRequestRepository.findByStatus(RequestStatus.PENDING, sort);

        // Apply filtering
        List<StudentRequest> filteredRequests = allRequests.stream()
                .filter(request -> {
                    // Filter by request type
                    if (filter.getRequestType() != null) {
                        StudentRequestType requestType = StudentRequestType.valueOf(filter.getRequestType());
                        if (!request.getRequestType().equals(requestType)) {
                            return false;
                        }
                    }

                    // Filter by branch
                    if (filter.getBranchId() != null && request.getCurrentClass() != null) {
                        if (!request.getCurrentClass().getBranch().getId().equals(filter.getBranchId())) {
                            return false;
                        }
                    }

                    // Filter by student name
                    if (filter.getStudentName() != null && !filter.getStudentName().trim().isEmpty()) {
                        String searchLower = filter.getStudentName().toLowerCase();
                        boolean matchesName = (request.getStudent().getUserAccount().getFullName() != null &&
                                request.getStudent().getUserAccount().getFullName().toLowerCase().contains(searchLower)) ||
                                (request.getStudent().getStudentCode() != null &&
                                request.getStudent().getStudentCode().toLowerCase().contains(searchLower));
                        if (!matchesName) {
                            return false;
                        }
                    }

                    // Filter by class code
                    if (filter.getClassCode() != null && !filter.getClassCode().trim().isEmpty()) {
                        if (request.getCurrentClass() == null || request.getCurrentClass().getCode() == null ||
                                !request.getCurrentClass().getCode().toLowerCase().contains(filter.getClassCode().toLowerCase())) {
                            return false;
                        }
                    }

                    // Filter by session date
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

                    return true;
                })
                .collect(Collectors.toList());

        // Calculate pagination manually
        int start = Math.min(filter.getPage() * filter.getSize(), filteredRequests.size());
        int end = Math.min(start + filter.getSize(), filteredRequests.size());
        List<StudentRequest> paginatedRequests = filteredRequests.subList(start, end);

        // Create properly paginated result
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);
        return new org.springframework.data.domain.PageImpl<>(
                paginatedRequests.stream().map(this::mapToAAResponseDTO).collect(Collectors.toList()),
                pageable,
                filteredRequests.size() // Correct total count
        );
    }

    @Override
    public Page<AARequestResponseDTO> getAllRequests(AARequestFilterDTO filter) {
        Sort sort = Sort.by(Sort.Direction.fromString(filter.getSort().split(",")[1]),
                filter.getSort().split(",")[0]);

        // Fetch all requests without pagination for proper filtering
        List<StudentRequest> allRequests;
        RequestStatus status = filter.getStatus() != null ?
                RequestStatus.valueOf(filter.getStatus()) : null;

        if (status != null) {
            allRequests = studentRequestRepository.findByStatus(status, sort);
        } else {
            allRequests = studentRequestRepository.findAll(sort);
        }

        // Apply additional filtering in service layer
        List<StudentRequest> filteredRequests = allRequests.stream()
                .filter(request -> {
                    // Filter by request type
                    if (filter.getRequestType() != null) {
                        StudentRequestType requestType = StudentRequestType.valueOf(filter.getRequestType());
                        if (!request.getRequestType().equals(requestType)) {
                            return false;
                        }
                    }

                    // Filter by branch
                    if (filter.getBranchId() != null && request.getCurrentClass() != null) {
                        if (!request.getCurrentClass().getBranch().getId().equals(filter.getBranchId())) {
                            return false;
                        }
                    }

                    // Filter by student name
                    if (filter.getStudentName() != null && !filter.getStudentName().trim().isEmpty()) {
                        String searchLower = filter.getStudentName().toLowerCase();
                        boolean matchesName = (request.getStudent().getUserAccount().getFullName() != null &&
                                request.getStudent().getUserAccount().getFullName().toLowerCase().contains(searchLower)) ||
                                (request.getStudent().getStudentCode() != null &&
                                request.getStudent().getStudentCode().toLowerCase().contains(searchLower));
                        if (!matchesName) {
                            return false;
                        }
                    }

                    // Filter by class code
                    if (filter.getClassCode() != null && !filter.getClassCode().trim().isEmpty()) {
                        if (request.getCurrentClass() == null || request.getCurrentClass().getCode() == null ||
                                !request.getCurrentClass().getCode().toLowerCase().contains(filter.getClassCode().toLowerCase())) {
                            return false;
                        }
                    }

                    // Filter by decided by
                    if (filter.getDecidedBy() != null && request.getDecidedBy() != null) {
                        if (!request.getDecidedBy().getId().equals(filter.getDecidedBy())) {
                            return false;
                        }
                    }

                    // Filter by session date
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

                    // Filter by submitted date
                    if (filter.getSubmittedDateFrom() != null) {
                        LocalDate submittedDateFrom = LocalDate.parse(filter.getSubmittedDateFrom());
                        if (request.getSubmittedAt().toLocalDate().isBefore(submittedDateFrom)) {
                            return false;
                        }
                    }

                    if (filter.getSubmittedDateTo() != null) {
                        LocalDate submittedDateTo = LocalDate.parse(filter.getSubmittedDateTo());
                        if (request.getSubmittedAt().toLocalDate().isAfter(submittedDateTo)) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());

        // Calculate pagination manually
        int start = Math.min(filter.getPage() * filter.getSize(), filteredRequests.size());
        int end = Math.min(start + filter.getSize(), filteredRequests.size());
        List<StudentRequest> paginatedRequests = filteredRequests.subList(start, end);

        // Create properly paginated result with correct total count
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);
        return new org.springframework.data.domain.PageImpl<>(
                paginatedRequests.stream().map(this::mapToAAResponseDTO).collect(Collectors.toList()),
                pageable,
                filteredRequests.size() // Correct total count
        );
    }

    @Override
    public StudentRequestDetailDTO getRequestDetailsForAA(Long requestId) {
        StudentRequest request = studentRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + requestId));

        StudentRequestDetailDTO detailDTO = mapToDetailDTO(request);

        // Add additional info for AA
        if (request.getTargetSession() != null) {
            long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), request.getTargetSession().getDate());
            double absenceRate = calculateAbsenceRate(request.getStudent().getId(), request.getCurrentClass().getId());

            StudentRequestDetailDTO.AdditionalInfoDTO additionalInfo = StudentRequestDetailDTO.AdditionalInfoDTO.builder()
                    .daysUntilSession(daysUntil)
                    .studentAbsenceStats(calculateAbsenceStats(request.getStudent().getId(), request.getCurrentClass().getId()))
                    .previousRequests(calculatePreviousRequests(request.getStudent().getId()))
                    .build();

            detailDTO.setAdditionalInfo(additionalInfo);
        }

        return detailDTO;
    }

    @Override
    @Transactional
    public StudentRequestResponseDTO approveRequest(Long requestId, Long decidedById, ApprovalDTO dto) {
        StudentRequest request = studentRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (!request.getStatus().equals(RequestStatus.PENDING)) {
            throw new BusinessRuleException("INVALID_STATUS", "Only pending requests can be approved");
        }

        // Update request status
        UserAccount decidedBy = userAccountRepository.findById(decidedById)
                .orElseThrow(() -> new ResourceNotFoundException("Deciding user not found"));

        request.setStatus(RequestStatus.APPROVED);
        request.setDecidedBy(decidedBy);
        request.setDecidedAt(OffsetDateTime.now());
        request.setNote(dto.getNote());
        request = studentRequestRepository.save(request);

        // If it's an absence request, update student_session attendance
        if (request.getRequestType().equals(StudentRequestType.ABSENCE) && request.getTargetSession() != null) {
            Optional<StudentSession> studentSession = studentSessionRepository
                    .findById(new StudentSession.StudentSessionId(request.getStudent().getId(), request.getTargetSession().getId()));

            if (studentSession.isPresent()) {
                StudentSession ss = studentSession.get();
                ss.setAttendanceStatus(AttendanceStatus.ABSENT);
                ss.setNote(String.format("Excused absence approved on %s. Request ID: %d",
                        OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), requestId));
                studentSessionRepository.save(ss);
            }
        }

        // If it's a makeup request, execute makeup approval logic
        if (request.getRequestType().equals(StudentRequestType.MAKEUP)) {
            executeMakeupApproval(request);
        }

        // If it's a transfer request, execute transfer approval logic
        if (request.getRequestType().equals(StudentRequestType.TRANSFER)) {
            executeTransfer(request);
        }

        log.info("Request {} approved by user {}", requestId, decidedById);
        // TODO: Send notification to student

        return mapToStudentResponseDTO(request);
    }

    @Override
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
        request.setNote(dto.getRejectionReason());
        request = studentRequestRepository.save(request);

        log.info("Request {} rejected by user {}", requestId, decidedById);
        // TODO: Send notification to student

        return mapToStudentResponseDTO(request);
    }

    @Override
    public RequestSummaryDTO getRequestSummary(AARequestFilterDTO filter) {
        long totalPending = studentRequestRepository.countByStatus(RequestStatus.PENDING);

        // Count urgent requests (sessions in next 2 days)
        LocalDate twoDaysFromNow = LocalDate.now().plusDays(2);
        long urgentCount = studentRequestRepository.countByStatus(RequestStatus.PENDING); // Simplified, would need date filtering

        long absenceRequests = studentRequestRepository.countByRequestTypeAndStatus(
                StudentRequestType.ABSENCE, RequestStatus.PENDING);
        long makeupRequests = studentRequestRepository.countByRequestTypeAndStatus(
                StudentRequestType.MAKEUP, RequestStatus.PENDING);
        long transferRequests = studentRequestRepository.countByRequestTypeAndStatus(
                StudentRequestType.TRANSFER, RequestStatus.PENDING);

        return RequestSummaryDTO.builder()
                .totalPending((int) totalPending)
                .needsUrgentReview((int) urgentCount)
                .absenceRequests((int) absenceRequests)
                .makeupRequests((int) makeupRequests)
                .transferRequests((int) transferRequests)
                .build();
    }

    @Override
    public boolean hasDuplicateRequest(Long studentId, Long sessionId, StudentRequestType requestType) {
        return studentRequestRepository.existsByStudentIdAndTargetSessionIdAndRequestTypeAndStatusIn(
                studentId, sessionId, requestType,
                List.of(RequestStatus.PENDING, RequestStatus.APPROVED));
    }

    @Override
    public double calculateAbsenceRate(Long studentId, Long classId) {
        // Simplified calculation - in real implementation, would count actual absences
        List<StudentSession> sessions = studentSessionRepository.findByStudentIdAndClassEntityId(studentId, classId);
        if (sessions.isEmpty()) {
            return 0.0;
        }

        long absenceCount = sessions.stream()
                .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.ABSENT)
                .count();

        return (double) absenceCount / sessions.size() * 100;
    }

    // Helper methods for mapping entities to DTOs
    private StudentRequestResponseDTO mapToStudentResponseDTO(StudentRequest request) {
        // When rejected, the note contains the rejection reason. When approved, it contains approval note.
        String rejectionReason = request.getStatus() == RequestStatus.REJECTED ? request.getNote() : null;

        // For TRANSFER requests, map effectiveSession to targetSession, and include targetClass + effectiveDate
        Session sessionToMap = request.getRequestType() == StudentRequestType.TRANSFER
                ? request.getEffectiveSession()
                : request.getTargetSession();

        String effectiveDateStr = request.getEffectiveDate() != null
                ? request.getEffectiveDate().format(DateTimeFormatter.ISO_DATE)
                : null;

        return StudentRequestResponseDTO.builder()
                .id(request.getId())
                .requestType(request.getRequestType().toString())
                .status(request.getStatus().toString())
                .currentClass(mapToClassSummaryDTO(request.getCurrentClass()))
                .targetClass(mapToClassSummaryDTO(request.getTargetClass())) // For TRANSFER only
                .targetSession(mapToSessionSummaryDTO(sessionToMap))
                .makeupSession(mapToSessionSummaryDTO(request.getMakeupSession())) // For MAKEUP only
                .effectiveDate(effectiveDateStr) // For TRANSFER only
                .requestReason(request.getRequestReason())
                .note(request.getNote())
                .submittedAt(request.getSubmittedAt())
                .submittedBy(mapToUserSummaryDTO(request.getSubmittedBy()))
                .decidedAt(request.getDecidedAt())
                .decidedBy(mapToUserSummaryDTO(request.getDecidedBy()))
                .rejectionReason(rejectionReason)
                .build();
    }

    private AARequestResponseDTO mapToAAResponseDTO(StudentRequest request) {
        String rejectionReason = request.getStatus() == RequestStatus.REJECTED ? request.getNote() : null;

        // For TRANSFER requests, use effectiveSession instead of targetSession
        Session sessionToMap = request.getRequestType() == StudentRequestType.TRANSFER
                ? request.getEffectiveSession()
                : request.getTargetSession();

        String effectiveDateStr = request.getEffectiveDate() != null
                ? request.getEffectiveDate().format(DateTimeFormatter.ISO_DATE)
                : null;

        return AARequestResponseDTO.builder()
                .id(request.getId())
                .requestType(request.getRequestType().toString())
                .status(request.getStatus().toString())
                .student(mapToStudentSummaryDTO(request.getStudent()))
                .currentClass(mapToAAClassSummaryDTO(request.getCurrentClass()))
                .targetClass(mapToAAClassSummaryDTO(request.getTargetClass())) // For TRANSFER only
                .targetSession(mapToAASessionSummaryDTO(sessionToMap))
                .makeupSession(mapToAASessionSummaryDTO(request.getMakeupSession())) // For MAKEUP only
                .effectiveDate(effectiveDateStr) // For TRANSFER only
                .requestReason(request.getRequestReason())
                .note(request.getNote())
                .submittedAt(request.getSubmittedAt())
                .submittedBy(mapToAAUserSummaryDTO(request.getSubmittedBy()))
                .decidedAt(request.getDecidedAt())
                .decidedBy(mapToAAUserSummaryDTO(request.getDecidedBy()))
                .rejectionReason(rejectionReason)
                .daysUntilSession(sessionToMap != null ?
                        ChronoUnit.DAYS.between(LocalDate.now(), sessionToMap.getDate()) : null)
                .studentAbsenceRate(request.getCurrentClass() != null ?
                        calculateAbsenceRate(request.getStudent().getId(), request.getCurrentClass().getId()) : null)
                .build();
    }

    private StudentRequestDetailDTO mapToDetailDTO(StudentRequest request) {
        String rejectionReason = request.getStatus() == RequestStatus.REJECTED ? request.getNote() : null;

        // For TRANSFER requests, use effectiveSession instead of targetSession
        Session sessionToMap = request.getRequestType() == StudentRequestType.TRANSFER
                ? request.getEffectiveSession()
                : request.getTargetSession();

        String effectiveDateStr = request.getEffectiveDate() != null
                ? request.getEffectiveDate().format(DateTimeFormatter.ISO_DATE)
                : null;

        return StudentRequestDetailDTO.builder()
                .id(request.getId())
                .requestType(request.getRequestType().toString())
                .status(request.getStatus().toString())
                .student(mapToDetailStudentSummaryDTO(request.getStudent()))
                .currentClass(mapToDetailClassDTO(request.getCurrentClass()))
                .targetClass(mapToDetailClassDTO(request.getTargetClass())) // For TRANSFER only
                .targetSession(mapToDetailSessionDTO(sessionToMap))
                .makeupSession(mapToDetailSessionDTO(request.getMakeupSession())) // For MAKEUP only
                .effectiveDate(effectiveDateStr) // For TRANSFER only
                .requestReason(request.getRequestReason())
                .note(request.getNote())
                .submittedAt(request.getSubmittedAt())
                .submittedBy(mapToDetailUserSummaryDTO(request.getSubmittedBy()))
                .decidedAt(request.getDecidedAt())
                .decidedBy(mapToDetailUserSummaryDTO(request.getDecidedBy()))
                .rejectionReason(rejectionReason)
                .build();
    }

    // Various mapping helper methods
    private StudentRequestResponseDTO.ClassSummaryDTO mapToClassSummaryDTO(ClassEntity classEntity) {
        if (classEntity == null) return null;
        return StudentRequestResponseDTO.ClassSummaryDTO.builder()
                .id(classEntity.getId())
                .code(classEntity.getCode())
                .name(classEntity.getName())
                .build();
    }

    private StudentRequestResponseDTO.SessionSummaryDTO mapToSessionSummaryDTO(Session session) {
        if (session == null) return null;
        return StudentRequestResponseDTO.SessionSummaryDTO.builder()
                .id(session.getId())
                .date(session.getDate().format(DateTimeFormatter.ISO_DATE))
                .courseSessionNumber(session.getCourseSession() != null ? session.getCourseSession().getSequenceNo() : null)
                .courseSessionTitle(session.getCourseSession() != null ? session.getCourseSession().getTopic() : null)
                .timeSlot(mapToTimeSlotSummaryDTO(session.getTimeSlotTemplate()))
                .build();
    }

    private StudentRequestResponseDTO.TimeSlotSummaryDTO mapToTimeSlotSummaryDTO(TimeSlotTemplate timeSlot) {
        if (timeSlot == null) return null;
        return StudentRequestResponseDTO.TimeSlotSummaryDTO.builder()
                .startTime(timeSlot.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .endTime(timeSlot.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .build();
    }

    private AARequestResponseDTO.TimeSlotSummaryDTO mapToAATimeSlotSummaryDTO(TimeSlotTemplate timeSlot) {
        if (timeSlot == null) return null;
        return AARequestResponseDTO.TimeSlotSummaryDTO.builder()
                .startTime(timeSlot.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .endTime(timeSlot.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .build();
    }

    private StudentRequestResponseDTO.UserSummaryDTO mapToUserSummaryDTO(UserAccount user) {
        if (user == null) return null;
        return StudentRequestResponseDTO.UserSummaryDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }

    private AARequestResponseDTO.UserSummaryDTO mapToAAUserSummaryDTO(UserAccount user) {
        if (user == null) return null;
        return AARequestResponseDTO.UserSummaryDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }

    private AARequestResponseDTO.StudentSummaryDTO mapToStudentSummaryDTO(Student student) {
        if (student == null) return null;
        return AARequestResponseDTO.StudentSummaryDTO.builder()
                .id(student.getId())
                .studentCode(student.getStudentCode())
                .fullName(student.getUserAccount().getFullName())
                .email(student.getUserAccount().getEmail())
                .phone(student.getUserAccount().getPhone())
                .build();
    }

    private AARequestResponseDTO.ClassSummaryDTO mapToAAClassSummaryDTO(ClassEntity classEntity) {
        if (classEntity == null) return null;
        return AARequestResponseDTO.ClassSummaryDTO.builder()
                .id(classEntity.getId())
                .code(classEntity.getCode())
                .name(classEntity.getName())
                .branch(mapToBranchSummaryDTO(classEntity.getBranch()))
                .build();
    }

    private AARequestResponseDTO.BranchSummaryDTO mapToBranchSummaryDTO(Branch branch) {
        if (branch == null) return null;
        return AARequestResponseDTO.BranchSummaryDTO.builder()
                .id(branch.getId())
                .name(branch.getName())
                .build();
    }

    private AARequestResponseDTO.SessionSummaryDTO mapToAASessionSummaryDTO(Session session) {
        if (session == null) return null;
        return AARequestResponseDTO.SessionSummaryDTO.builder()
                .id(session.getId())
                .date(session.getDate().format(DateTimeFormatter.ISO_DATE))
                .dayOfWeek(session.getDate().getDayOfWeek().toString())
                .courseSessionNumber(session.getCourseSession() != null ? session.getCourseSession().getSequenceNo() : null)
                .courseSessionTitle(session.getCourseSession() != null ? session.getCourseSession().getTopic() : null)
                .timeSlot(mapToAATimeSlotSummaryDTO(session.getTimeSlotTemplate()))
                .status(session.getStatus().toString())
                .teacher(null) // Teacher mapping to be implemented via TeachingSlot relationship
                .build();
    }

    private AARequestResponseDTO.TeacherSummaryDTO mapToTeacherSummaryDTO(Teacher teacher) {
        if (teacher == null) return null;
        return AARequestResponseDTO.TeacherSummaryDTO.builder()
                .id(teacher.getId())
                .fullName(teacher.getUserAccount().getFullName())
                .email(teacher.getUserAccount().getEmail())
                .build();
    }

    private StudentRequestDetailDTO.StudentSummaryDTO mapToDetailStudentSummaryDTO(Student student) {
        if (student == null) return null;
        return StudentRequestDetailDTO.StudentSummaryDTO.builder()
                .id(student.getId())
                .studentCode(student.getStudentCode())
                .fullName(student.getUserAccount().getFullName())
                .email(student.getUserAccount().getEmail())
                .phone(student.getUserAccount().getPhone())
                .build();
    }

    private StudentRequestDetailDTO.ClassDetailDTO mapToDetailClassDTO(ClassEntity classEntity) {
        if (classEntity == null) return null;
        return StudentRequestDetailDTO.ClassDetailDTO.builder()
                .id(classEntity.getId())
                .code(classEntity.getCode())
                .name(classEntity.getName())
                .branch(mapToDetailBranchSummaryDTO(classEntity.getBranch()))
                .teacher(null) // Teacher mapping to be implemented
                .build();
    }

    private StudentRequestDetailDTO.BranchSummaryDTO mapToDetailBranchSummaryDTO(Branch branch) {
        if (branch == null) return null;
        return StudentRequestDetailDTO.BranchSummaryDTO.builder()
                .id(branch.getId())
                .name(branch.getName())
                .build();
    }

    private StudentRequestDetailDTO.TeacherSummaryDTO mapToDetailTeacherSummaryDTO(Teacher teacher) {
        if (teacher == null) return null;
        return StudentRequestDetailDTO.TeacherSummaryDTO.builder()
                .id(teacher.getId())
                .fullName(teacher.getUserAccount().getFullName())
                .email(teacher.getUserAccount().getEmail())
                .build();
    }

    private StudentRequestDetailDTO.SessionDetailDTO mapToDetailSessionDTO(Session session) {
        if (session == null) return null;
        return StudentRequestDetailDTO.SessionDetailDTO.builder()
                .id(session.getId())
                .date(session.getDate().format(DateTimeFormatter.ISO_DATE))
                .dayOfWeek(session.getDate().getDayOfWeek().toString())
                .courseSessionNumber(session.getCourseSession() != null ? session.getCourseSession().getSequenceNo() : null)
                .courseSessionTitle(session.getCourseSession() != null ? session.getCourseSession().getTopic() : null)
                .timeSlot(mapToDetailTimeSlotDTO(session.getTimeSlotTemplate()))
                .status(session.getStatus().toString())
                .teacher(null) // Teacher mapping to be implemented via TeachingSlot relationship
                .build();
    }

    private StudentRequestDetailDTO.TimeSlotDTO mapToDetailTimeSlotDTO(TimeSlotTemplate timeSlot) {
        if (timeSlot == null) return null;
        return StudentRequestDetailDTO.TimeSlotDTO.builder()
                .startTime(timeSlot.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .endTime(timeSlot.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .build();
    }

    private StudentRequestDetailDTO.UserSummaryDTO mapToDetailUserSummaryDTO(UserAccount user) {
        if (user == null) return null;
        return StudentRequestDetailDTO.UserSummaryDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }

    private SessionAvailabilityDTO.SessionDTO mapToSessionDTO(Session session) {
        return SessionAvailabilityDTO.SessionDTO.builder()
                .sessionId(session.getId())
                .date(session.getDate().format(DateTimeFormatter.ISO_DATE))
                .courseSessionNumber(session.getCourseSession() != null ? session.getCourseSession().getSequenceNo() : null)
                .courseSessionTitle(session.getCourseSession() != null ? session.getCourseSession().getTopic() : null)
                .timeSlot(mapToSessionTimeSlotDTO(session.getTimeSlotTemplate()))
                .status(session.getStatus().toString())
                .type(session.getType().toString())
                .teacher(null) // Teacher mapping to be implemented via TeachingSlot relationship
                .build();
    }

    private SessionAvailabilityDTO.TimeSlotDTO mapToSessionTimeSlotDTO(TimeSlotTemplate timeSlot) {
        if (timeSlot == null) return null;
        return SessionAvailabilityDTO.TimeSlotDTO.builder()
                .startTime(timeSlot.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .endTime(timeSlot.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .build();
    }

    private SessionAvailabilityDTO.TeacherDTO mapToSessionTeacherDTO(Teacher teacher) {
        if (teacher == null) return null;
        return SessionAvailabilityDTO.TeacherDTO.builder()
                .id(teacher.getId())
                .fullName(teacher.getUserAccount().getFullName())
                .build();
    }

    private StudentRequestDetailDTO.StudentAbsenceStatsDTO calculateAbsenceStats(Long studentId, Long classId) {
        List<StudentSession> sessions = studentSessionRepository.findByStudentIdAndClassEntityId(studentId, classId);

        long totalSessions = sessions.size();
        long absences = sessions.stream().filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.ABSENT).count();

        // For now, assuming all absences are unexcused (would need more complex logic for excused vs unexcused)
        return StudentRequestDetailDTO.StudentAbsenceStatsDTO.builder()
                .totalAbsences((int) absences)
                .totalSessions((int) totalSessions)
                .absenceRate(totalSessions > 0 ? (double) absences / totalSessions * 100 : 0.0)
                .excusedAbsences(0)
                .unexcusedAbsences((int) absences)
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

    // ==================== MAKEUP REQUEST METHODS ====================

    @Override
    public MissedSessionsResponseDTO getMissedSessions(Long userId, Integer weeksBack, Boolean excludeRequested) {
        // Resolve student from user
        Student student = studentRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user ID: " + userId));

        return getMissedSessionsForStudent(student.getId(), weeksBack);
    }

    @Override
    public MissedSessionsResponseDTO getMissedSessionsForStudent(Long studentId, Integer weeksBack) {
        LocalDate fromDate = LocalDate.now().minusWeeks(weeksBack != null ? weeksBack : 4);
        LocalDate toDate = LocalDate.now();

        List<Session> missedSessions = sessionRepository.findMissedSessionsForStudent(studentId, fromDate, toDate);

        List<MissedSessionDTO> sessionDTOs = missedSessions.stream()
                .map(session -> mapToMissedSessionDTO(session, studentId))
                .collect(Collectors.toList());

        return MissedSessionsResponseDTO.builder()
                .totalCount(sessionDTOs.size())
                .sessions(sessionDTOs)
                .build();
    }

    @Override
    public MakeupOptionsResponseDTO getMakeupOptions(Long targetSessionId, Long userId) {
        // Resolve student from user
        Student student = studentRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user ID: " + userId));

        return getMakeupOptionsForStudent(targetSessionId, student.getId());
    }

    @Override
    public MakeupOptionsResponseDTO getMakeupOptionsForStudent(Long targetSessionId, Long studentId) {
        // Get target session
        Session targetSession = sessionRepository.findById(targetSessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Target session not found"));

        if (targetSession.getCourseSession() == null) {
            throw new BusinessRuleException("INVALID_SESSION", "Target session must have course session defined");
        }

        // Find makeup options with same course session
        // For OFFLINE: filter by same branch; For ONLINE/HYBRID: allow different branches
        List<Session> makeupOptions = sessionRepository.findMakeupSessionOptions(
                targetSession.getCourseSession().getId(),
                targetSessionId,
                targetSession.getClassEntity().getBranch().getId(),
                targetSession.getClassEntity().getModality().name()
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

        // Build response with target session context
        MakeupOptionsResponseDTO.TargetSessionInfo targetInfo = MakeupOptionsResponseDTO.TargetSessionInfo.builder()
                .sessionId(targetSession.getId())
                .courseSessionId(targetSession.getCourseSession().getId())
                .classId(targetSession.getClassEntity().getId())
                .classCode(targetSession.getClassEntity().getCode())
                .branchId(targetSession.getClassEntity().getBranch().getId())
                .modality(targetSession.getClassEntity().getModality().name())
                .build();

        return MakeupOptionsResponseDTO.builder()
                .targetSession(targetInfo)
                .makeupOptions(rankedOptions)
                .build();
    }

    @Override
    @Transactional
    public StudentRequestResponseDTO submitMakeupRequest(Long userId, MakeupRequestDTO dto) {
        // Resolve student from user
        Student student = studentRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user ID: " + userId));

        UserAccount submittedBy = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return submitMakeupRequestInternal(student.getId(), dto, submittedBy, false);
    }

    @Override
    @Transactional
    public StudentRequestResponseDTO submitAbsenceRequestOnBehalf(Long decidedById, AbsenceRequestDTO dto) {
        log.info("AA user {} submitting absence request on-behalf for student {} session {}",
                decidedById, dto.getStudentId(), dto.getTargetSessionId());

        // 1. Validate studentId is provided (required for on-behalf)
        if (dto.getStudentId() == null) {
            throw new BusinessRuleException("MISSING_STUDENT_ID", "Student ID is required for on-behalf requests");
        }

        // 2. Validate student exists
        Student student = studentRepository.findById(dto.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + dto.getStudentId()));

        // 3. Validate session exists
        Session session = sessionRepository.findById(dto.getTargetSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        // 4. Validate enrollment
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndClassIdAndStatus(student.getId(), dto.getCurrentClassId(), EnrollmentStatus.ENROLLED);

        if (enrollment == null) {
            throw new BusinessRuleException("NOT_ENROLLED", "Student is not enrolled in this class");
        }

        // 5. Check duplicate request
        if (hasDuplicateRequest(student.getId(), dto.getTargetSessionId(), StudentRequestType.ABSENCE)) {
            throw new DuplicateRequestException("Duplicate absence request for this session");
        }

        // 6. Get entities
        ClassEntity classEntity = classRepository.findById(dto.getCurrentClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        UserAccount submittedBy = userAccountRepository.findById(decidedById)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 7. Create request with auto-approval
        String finalNote = dto.getNote() != null
            ? dto.getNote() + " [Auto-approved by Academic Affairs]"
            : "Auto-approved by Academic Affairs";

        StudentRequest request = StudentRequest.builder()
                .student(student)
                .requestType(StudentRequestType.ABSENCE)
                .currentClass(classEntity)
                .targetSession(session)
                .requestReason(dto.getRequestReason())
                .note(finalNote)
                .status(RequestStatus.APPROVED)  // Auto-approved for AA on-behalf
                .submittedBy(submittedBy)
                .submittedAt(OffsetDateTime.now())
                .decidedBy(submittedBy)  // Same AA user
                .decidedAt(OffsetDateTime.now())
                .build();

        request = studentRequestRepository.save(request);
        log.info("Absence request created and auto-approved with id: {}", request.getId());

        // 8. Mark student session as ABSENT (approved absence request)
        StudentSession.StudentSessionId ssId = new StudentSession.StudentSessionId(student.getId(), session.getId());
        StudentSession ss = studentSessionRepository.findById(ssId)
                .orElseThrow(() -> new ResourceNotFoundException("Student session not found"));

        ss.setAttendanceStatus(AttendanceStatus.ABSENT);
        studentSessionRepository.save(ss);

        log.info("Marked student session as ABSENT (approved absence) for student {} session {}", student.getId(), session.getId());

        return mapToStudentResponseDTO(request);
    }

    @Override
    @Transactional
    public StudentRequestResponseDTO submitMakeupRequestOnBehalf(Long decidedById, MakeupRequestDTO dto) {
        if (dto.getStudentId() == null) {
            throw new BusinessRuleException("MISSING_STUDENT_ID", "Student ID is required for on-behalf requests");
        }

        UserAccount submittedBy = userAccountRepository.findById(decidedById)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return submitMakeupRequestInternal(dto.getStudentId(), dto, submittedBy, true);
    }

    private StudentRequestResponseDTO submitMakeupRequestInternal(Long studentId, MakeupRequestDTO dto,
                                                                   UserAccount submittedBy, boolean autoApprove) {
        log.info("Submitting makeup request for student {} - target: {}, makeup: {}",
                studentId, dto.getTargetSessionId(), dto.getMakeupSessionId());

        // 1. Validate target session exists and has ABSENT attendance
        Optional<StudentSession> targetStudentSession = studentSessionRepository.findById(
                new StudentSession.StudentSessionId(studentId, dto.getTargetSessionId()));

        if (targetStudentSession.isEmpty()) {
            throw new ResourceNotFoundException("Target session not found for this student");
        }

        if (!targetStudentSession.get().getAttendanceStatus().equals(AttendanceStatus.ABSENT)) {
            throw new BusinessRuleException("NOT_ABSENT", "Can only makeup absent sessions");
        }

        Session targetSession = targetStudentSession.get().getSession();

        // 2. Check eligible timeframe (within 4 weeks)
        long weeksAgo = ChronoUnit.WEEKS.between(targetSession.getDate(), LocalDate.now());
        if (weeksAgo > 4) {
            throw new BusinessRuleException("SESSION_TOO_OLD", "Session too old for makeup (limit: 4 weeks)");
        }

        // 3. Validate makeup session
        Session makeupSession = sessionRepository.findById(dto.getMakeupSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Makeup session not found"));

        if (!makeupSession.getStatus().equals(SessionStatus.PLANNED)) {
            throw new BusinessRuleException("INVALID_MAKEUP_STATUS", "Makeup session must be PLANNED");
        }

        if (makeupSession.getDate().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("PAST_SESSION", "Makeup session must be in the future");
        }

        // 4. CRITICAL: Validate course session match
        if (!targetSession.getCourseSession().getId().equals(makeupSession.getCourseSession().getId())) {
            throw new BusinessRuleException("COURSE_SESSION_MISMATCH",
                    "Makeup session must have same content (courseSessionId)");
        }

        // 5. Check capacity
        long enrolledCount = studentSessionRepository.countBySessionId(makeupSession.getId());
        if (enrolledCount >= makeupSession.getClassEntity().getMaxCapacity()) {
            throw new BusinessRuleException("SESSION_FULL", "Makeup session is full");
        }

        // 6. Check schedule conflict
        List<Session> studentSessions = sessionRepository.findSessionsForStudentByDate(
                studentId, makeupSession.getDate());

        for (Session existing : studentSessions) {
            if (hasTimeOverlap(existing.getTimeSlotTemplate(), makeupSession.getTimeSlotTemplate())) {
                throw new BusinessRuleException("SCHEDULE_CONFLICT",
                        "Schedule conflict with other classes");
            }
        }

        // 7. Check duplicate request
        boolean hasDuplicate = studentRequestRepository.existsByStudentIdAndTargetSessionIdAndRequestTypeAndStatusIn(
                studentId, dto.getTargetSessionId(), StudentRequestType.MAKEUP,
                List.of(RequestStatus.PENDING, RequestStatus.APPROVED));

        if (hasDuplicate) {
            throw new DuplicateRequestException("Duplicate makeup request for this session");
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

        // 10. If auto-approved, execute approval logic
        if (autoApprove) {
            executeMakeupApproval(request);
        }

        log.info("Makeup request created with ID: {} - Status: {}", request.getId(), request.getStatus());
        return mapToStudentResponseDTO(request);
    }

    private boolean hasTimeOverlap(TimeSlotTemplate slot1, TimeSlotTemplate slot2) {
        if (slot1 == null || slot2 == null) return false;
        return !slot1.getEndTime().isBefore(slot2.getStartTime()) &&
               !slot2.getEndTime().isBefore(slot1.getStartTime());
    }

    private void executeMakeupApproval(StudentRequest request) {
        log.info("Executing makeup approval for request {}", request.getId());

        // 1. Re-validate capacity (race condition check)
        long currentEnrolled = studentSessionRepository.countBySessionId(request.getMakeupSession().getId());
        if (currentEnrolled >= request.getMakeupSession().getClassEntity().getMaxCapacity()) {
            throw new BusinessRuleException("SESSION_FULL", "Makeup session became full");
        }

        // 2. Update original student_session
        Optional<StudentSession> originalStudentSession = studentSessionRepository.findById(
                new StudentSession.StudentSessionId(request.getStudent().getId(), request.getTargetSession().getId()));

        if (originalStudentSession.isPresent()) {
            StudentSession original = originalStudentSession.get();
            original.setMakeupSession(request.getMakeupSession());
            original.setNote(String.format("Makeup approved: Session %d on %s. Request ID: %d",
                    request.getMakeupSession().getCourseSession().getId(),
                    request.getMakeupSession().getDate(),
                    request.getId()));
            studentSessionRepository.save(original);
        }

        // 3. Create NEW student_session for makeup
        StudentSession.StudentSessionId makeupId = new StudentSession.StudentSessionId(
                request.getStudent().getId(),
                request.getMakeupSession().getId()
        );

        StudentSession makeupStudentSession = StudentSession.builder()
                .id(makeupId)
                .student(request.getStudent())
                .session(request.getMakeupSession())
                .attendanceStatus(AttendanceStatus.PLANNED)
                .isMakeup(true)
                .makeupSession(request.getMakeupSession())
                .originalSession(request.getTargetSession())
                .note("Makeup student from " + request.getCurrentClass().getCode())
                .build();

        studentSessionRepository.save(makeupStudentSession);

        log.info("Makeup approval executed: original session updated, new makeup session created");
    }

    private MissedSessionDTO mapToMissedSessionDTO(Session session, Long studentId) {
        long daysAgo = ChronoUnit.DAYS.between(session.getDate(), LocalDate.now());

        // Check if has existing makeup request
        boolean hasExistingMakeup = studentRequestRepository.existsByStudentIdAndTargetSessionIdAndRequestTypeAndStatusIn(
                studentId, session.getId(), StudentRequestType.MAKEUP,
                List.of(RequestStatus.PENDING, RequestStatus.APPROVED));

        // Check if has approved absence request
        boolean isExcused = studentRequestRepository.existsByStudentIdAndTargetSessionIdAndRequestTypeAndStatusIn(
                studentId, session.getId(), StudentRequestType.ABSENCE,
                List.of(RequestStatus.APPROVED));

        return MissedSessionDTO.builder()
                .sessionId(session.getId())
                .date(session.getDate())
                .daysAgo((int) daysAgo)
                .courseSessionNumber(session.getCourseSession() != null ? session.getCourseSession().getSequenceNo() : null)
                .courseSessionTitle(session.getCourseSession() != null ? session.getCourseSession().getTopic() : null)
                .courseSessionId(session.getCourseSession() != null ? session.getCourseSession().getId() : null)
                .classInfo(MissedSessionDTO.ClassInfo.builder()
                        .id(session.getClassEntity().getId())
                        .code(session.getClassEntity().getCode())
                        .name(session.getClassEntity().getName())
                        .build())
                .timeSlotInfo(MissedSessionDTO.TimeSlotInfo.builder()
                        .startTime(session.getTimeSlotTemplate().getStartTime())
                        .endTime(session.getTimeSlotTemplate().getEndTime())
                        .build())
                .attendanceStatus(AttendanceStatus.ABSENT.name())
                .hasExistingMakeupRequest(hasExistingMakeup)
                .isExcusedAbsence(isExcused)
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
        if (branchMatch) score += 10;
        if (modalityMatch) score += 5;

        // Date proximity bonus
        long weeksUntil = ChronoUnit.WEEKS.between(LocalDate.now(), session.getDate());
        score += Math.max(0, 3 - weeksUntil); // +3 for this week, +2 next week, +1 in 2 weeks

        // Capacity bonus
        long enrolled = studentSessionRepository.countBySessionId(session.getId());
        int availableSlots = session.getClassEntity().getMaxCapacity() - (int) enrolled;
        score += Math.min(1, availableSlots / 5); // +1 per 5 slots

        String priority = score >= 15 ? "HIGH" : (score >= 8 ? "MEDIUM" : "LOW");

        return MakeupOptionDTO.builder()
                .sessionId(session.getId())
                .date(session.getDate())
                .courseSessionId(session.getCourseSession().getId())
                .courseSessionTitle(session.getCourseSession().getTopic())
                .courseSessionNumber(session.getCourseSession().getSequenceNo())
                .classInfo(MakeupOptionDTO.ClassInfo.builder()
                        .id(session.getClassEntity().getId())
                        .code(session.getClassEntity().getCode())
                        .name(session.getClassEntity().getName())
                        .branchId(session.getClassEntity().getBranch().getId())
                        .branchName(session.getClassEntity().getBranch().getName())
                        .modality(session.getClassEntity().getModality().name())
                        .availableSlots(availableSlots)
                        .maxCapacity(session.getClassEntity().getMaxCapacity())
                        .build())
                .timeSlotInfo(MakeupOptionDTO.TimeSlotInfo.builder()
                        .startTime(session.getTimeSlotTemplate().getStartTime())
                        .endTime(session.getTimeSlotTemplate().getEndTime())
                        .build())
                .matchScore(MakeupOptionDTO.MatchScore.builder()
                        .branchMatch(branchMatch)
                        .modalityMatch(modalityMatch)
                        .totalScore(score)
                        .priority(priority)
                        .build())
                .build();
    }

    // ============== TRANSFER REQUEST IMPLEMENTATIONS ==============

    @Override
    @Transactional
    public StudentRequestResponseDTO submitTransferRequest(Long userId, TransferRequestDTO dto) {
        log.info("Submitting transfer request for user {} from class {} to class {}",
                userId, dto.getCurrentClassId(), dto.getTargetClassId());

        // Resolve student from user
        Student student = studentRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user ID: " + userId));

        UserAccount submittedBy = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return submitTransferRequestInternal(student.getId(), dto, submittedBy, false);
    }

    @Override
    @Transactional
    public StudentRequestResponseDTO submitTransferRequestOnBehalf(Long decidedById, TransferRequestDTO dto) {
        // Validate that this is an AA on-behalf request
        if (dto.getStudentId() == null) {
            throw new BusinessRuleException("MISSING_STUDENT_ID", "Student ID is required for on-behalf transfer requests");
        }

        if (dto.getNote() == null || !dto.getNote().contains("AA")) {
            // This is a safeguard - on-behalf requests should have AA context
            dto.setNote((dto.getNote() != null ? dto.getNote() + " - " : "") + "Submitted by AA on behalf");
        }

        UserAccount submittedBy = userAccountRepository.findById(decidedById)
                .orElseThrow(() -> new ResourceNotFoundException("AA user not found"));

        return submitTransferRequestInternal(dto.getStudentId(), dto, submittedBy, true);
    }

    @Override
    public TransferEligibilityDTO getTransferEligibility(Long userId) {
        Student student = studentRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user ID: " + userId));

        // Get current enrollments
        List<Enrollment> enrollments = enrollmentRepository.findByStudentIdAndStatusIn(
                student.getId(), List.of(EnrollmentStatus.ENROLLED));

        List<TransferEligibilityDTO.CurrentClassInfo> currentClasses = enrollments.stream()
                .map(enrollment -> mapToCurrentClassInfoWithQuota(enrollment, student.getId()))
                .collect(Collectors.toList());

        // Check transfer policy compliance (global summary)
        int totalUsedTransfers = calculateUsedTransfers(student.getId());
        int totalRemainingTransfers = currentClasses.stream()
                .mapToInt(c -> c.getTransferQuota().getRemaining())
                .sum();

        TransferEligibilityDTO.TransferPolicyInfo policyInfo = TransferEligibilityDTO.TransferPolicyInfo.builder()
                .maxTransfersPerCourse(1) // Business rule: 1 transfer per course
                .usedTransfers(totalUsedTransfers)
                .remainingTransfers(totalRemainingTransfers)
                .requiresAAApproval(false) // Tier 1 transfers are auto-approved for same branch/mode
                .policyDescription("Maximum 1 transfer per course. Same branch & mode changes are auto-approved.")
                .build();

        boolean eligible = !currentClasses.isEmpty() && totalRemainingTransfers > 0;

        return TransferEligibilityDTO.builder()
                .eligibleForTransfer(eligible)
                .ineligibilityReason(eligible ? null : "No eligible enrollments or transfer limit exceeded")
                .currentClasses(currentClasses)
                .policyInfo(policyInfo)
                .build();
    }

    @Override
    public List<TransferOptionDTO> getTransferOptions(Long userId, Long currentClassId) {
        ClassEntity currentClass = classRepository.findByIdWithCourse(currentClassId)
                .orElseThrow(() -> new ResourceNotFoundException("Current class not found"));

        // If userId is provided (student request), verify enrollment
        if (userId != null) {
            Student student = studentRepository.findByUserAccountId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found for user ID: " + userId));

            boolean isEnrolled = enrollmentRepository.existsByStudentIdAndClassIdAndStatusIn(
                    student.getId(), currentClassId, List.of(EnrollmentStatus.ENROLLED));

            if (!isEnrolled) {
                throw new BusinessRuleException("NOT_ENROLLED", "Student is not enrolled in this class");
            }
        } else {
            // For AA view without studentId, return empty list as this method requires student context
            return List.of();
        }

        // Find eligible target classes (same course, different schedules/branches)
        List<ClassEntity> targetClasses = classRepository.findByCourseIdAndStatusIn(
                currentClass.getCourse().getId(), List.of(ClassStatus.SCHEDULED, ClassStatus.ONGOING));

        // Filter out current class and check capacity
        List<TransferOptionDTO> options = targetClasses.stream()
                .filter(cls -> !cls.getId().equals(currentClassId))
                .filter(cls -> {
                    // Calculate current enrollment using repository method to avoid lazy loading
                    Integer currentEnrollmentCount = classRepository.countEnrolledStudents(cls.getId());
                    int currentEnrollment = currentEnrollmentCount != null ? currentEnrollmentCount : 0;
                    return cls.getMaxCapacity() != null && currentEnrollment < cls.getMaxCapacity();
                })
                .map(cls -> mapToTransferOptionDTO(cls, currentClass))
                .sorted((a, b) -> Boolean.compare(b.isCanTransfer(), a.isCanTransfer()))
                .collect(Collectors.toList());

        return options;
    }

    /**
     * Get flexible transfer options for AA (Academic Affairs)
     * Supports filtering by branch, modality, and schedule changes
     *
     * @param currentClassId Current class ID (required)
     * @param targetBranchId Target branch ID (optional - for branch change)
     * @param targetModality Target modality (optional - for modality change)
     * @param scheduleOnly Schedule-only change flag (optional - for schedule change within same branch/modality)
     * @return TransferOptionsResponseDTO with current class info, criteria, and available classes
     */
    @Override
    public TransferOptionsResponseDTO getTransferOptionsFlexible(
            Long currentClassId, Long targetBranchId, String targetModality, Boolean scheduleOnly) {

        // Load current class with all related entities
        ClassEntity currentClass = classRepository.findByIdWithCourse(currentClassId)
                .orElseThrow(() -> new ResourceNotFoundException("Current class not found with ID: " + currentClassId));

        // Build flexible filter criteria
        ClassSearchCriteria criteria = ClassSearchCriteria.builder()
                .courseId(currentClass.getCourse().getId())
                .excludeClassId(currentClassId)
                .statuses(List.of(ClassStatus.SCHEDULED, ClassStatus.ONGOING))
                .hasCapacity(true)
                .build();

        // Apply optional filters based on parameters
        if (Boolean.TRUE.equals(scheduleOnly)) {
            // Schedule-only: same branch + same modality
            criteria.setBranchId(currentClass.getBranch().getId());
            criteria.setModality(currentClass.getModality());
            // Note: scheduleOnly filtering by excluding same schedule days is handled in query
        } else {
            // Flexible filtering
            if (targetBranchId != null) {
                criteria.setBranchId(targetBranchId);
            }
            if (targetModality != null) {
                try {
                    criteria.setModality(Modality.valueOf(targetModality));
                } catch (IllegalArgumentException e) {
                    throw new BusinessRuleException("INVALID_MODALITY",
                        "Invalid modality: " + targetModality + ". Must be OFFLINE, ONLINE, or HYBRID");
                }
            }
        }

        // Fetch classes using flexible criteria
        List<ClassEntity> targetClasses = classRepository.findByFlexibleCriteria(criteria);

        // Filter by capacity and map to DTOs with changes summary
        List<TransferOptionDTO> availableClasses = targetClasses.stream()
                .filter(cls -> {
                    Integer currentEnrollmentCount = classRepository.countEnrolledStudents(cls.getId());
                    int currentEnrollment = currentEnrollmentCount != null ? currentEnrollmentCount : 0;
                    return cls.getMaxCapacity() != null && currentEnrollment < cls.getMaxCapacity();
                })
                .map(cls -> mapToTransferOptionDTOWithChanges(cls, currentClass))
                .sorted(this::compareByCompatibility)
                .collect(Collectors.toList());

        // Build current class info
        TransferOptionsResponseDTO.CurrentClassInfo currentClassInfo = buildCurrentClassInfo(currentClass);

        // Build transfer criteria summary
        TransferOptionsResponseDTO.TransferCriteria transferCriteria =
            TransferOptionsResponseDTO.TransferCriteria.builder()
                .branchChange(targetBranchId != null && !targetBranchId.equals(currentClass.getBranch().getId()))
                .modalityChange(targetModality != null && !targetModality.equals(currentClass.getModality().name()))
                .scheduleChange(Boolean.TRUE.equals(scheduleOnly))
                .build();

        return TransferOptionsResponseDTO.builder()
                .currentClass(currentClassInfo)
                .transferCriteria(transferCriteria)
                .availableClasses(availableClasses)
                .build();
    }

    /**
     * Map ClassEntity to TransferOptionDTO with changes summary
     * Shows what will change: branch, modality, schedule
     */
    private TransferOptionDTO mapToTransferOptionDTOWithChanges(ClassEntity targetClass, ClassEntity currentClass) {
        TransferOptionDTO dto = mapToTransferOptionDTO(targetClass, currentClass);

        // Build changes summary
        TransferOptionDTO.Changes changes = TransferOptionDTO.Changes.builder()
                .branch(buildChangeText(currentClass.getBranch().getName(), targetClass.getBranch().getName()))
                .modality(buildChangeText(currentClass.getModality().name(), targetClass.getModality().name()))
                .schedule(buildScheduleChangeText(currentClass, targetClass))
                .build();

        dto.setChanges(changes);
        return dto;
    }

    /**
     * Build change text for branch/modality
     * Returns "X → Y" if changed, "No change" if same
     */
    private String buildChangeText(String currentValue, String targetValue) {
        if (currentValue == null || targetValue == null) {
            return "Unknown";
        }
        return currentValue.equals(targetValue) ? "No change" : currentValue + " → " + targetValue;
    }

    /**
     * Build schedule change text
     * Shows time slot changes if available
     */
    private String buildScheduleChangeText(ClassEntity currentClass, ClassEntity targetClass) {
        String currentSchedule = formatSchedule(currentClass);
        String targetSchedule = formatSchedule(targetClass);

        if (currentSchedule.equals(targetSchedule)) {
            return "No change";
        }
        return currentSchedule + " → " + targetSchedule;
    }

    /**
     * Format schedule info from class
     * Uses date range and schedule days since time slots vary per session
     */
    private String formatSchedule(ClassEntity classEntity) {
        StringBuilder schedule = new StringBuilder();

        // Format schedule days (e.g., "Mon, Wed, Fri")
        if (classEntity.getScheduleDays() != null && classEntity.getScheduleDays().length > 0) {
            String days = Arrays.stream(classEntity.getScheduleDays())
                    .map(this::dayOfWeekToString)
                    .collect(Collectors.joining(", "));
            schedule.append(days);
        }

        // Add date range
        if (classEntity.getStartDate() != null) {
            if (schedule.length() > 0) schedule.append(" - ");
            schedule.append(classEntity.getStartDate());
            if (classEntity.getPlannedEndDate() != null) {
                schedule.append(" to ").append(classEntity.getPlannedEndDate());
            }
        }

        return schedule.length() > 0 ? schedule.toString() : "Schedule TBD";
    }

    /**
     * Convert day of week number to string
     * 1=Monday, 2=Tuesday, ..., 7=Sunday
     */
    private String dayOfWeekToString(Short dayNum) {
        return switch (dayNum) {
            case 1 -> "Mon";
            case 2 -> "Tue";
            case 3 -> "Wed";
            case 4 -> "Thu";
            case 5 -> "Fri";
            case 6 -> "Sat";
            case 7 -> "Sun";
            default -> String.valueOf(dayNum);
        };
    }

    /**
     * Compare classes by compatibility (fewer changes = higher priority)
     */
    private int compareByCompatibility(TransferOptionDTO a, TransferOptionDTO b) {
        int changesA = countChanges(a.getChanges());
        int changesB = countChanges(b.getChanges());

        if (changesA != changesB) {
            return Integer.compare(changesA, changesB);
        }

        // Secondary sort by content gap severity
        String severityA = a.getContentGapAnalysis() != null ? a.getContentGapAnalysis().getGapLevel() : "NONE";
        String severityB = b.getContentGapAnalysis() != null ? b.getContentGapAnalysis().getGapLevel() : "NONE";
        return compareSeverity(severityA, severityB);
    }

    /**
     * Count number of changes in Changes object
     */
    private int countChanges(TransferOptionDTO.Changes changes) {
        if (changes == null) return 0;

        int count = 0;
        if (changes.getBranch() != null && !changes.getBranch().equals("No change")) count++;
        if (changes.getModality() != null && !changes.getModality().equals("No change")) count++;
        if (changes.getSchedule() != null && !changes.getSchedule().equals("No change")) count++;
        return count;
    }

    /**
     * Compare severity levels: NONE < MINOR < MODERATE < MAJOR
     */
    private int compareSeverity(String severityA, String severityB) {
        Map<String, Integer> severityOrder = Map.of(
            "NONE", 0, "MINOR", 1, "MODERATE", 2, "MAJOR", 3
        );
        return Integer.compare(
            severityOrder.getOrDefault(severityA, 0),
            severityOrder.getOrDefault(severityB, 0)
        );
    }

    /**
     * Build current class info for response
     */
    private TransferOptionsResponseDTO.CurrentClassInfo buildCurrentClassInfo(ClassEntity currentClass) {
        // Get current session count (completed sessions)
        List<Session> completedSessions = sessionRepository.findByClassIdAndStatusIn(
            currentClass.getId(),
            List.of(SessionStatus.DONE, SessionStatus.CANCELLED)
        );
        int currentSessionCount = completedSessions != null ? completedSessions.size() : 0;

        return TransferOptionsResponseDTO.CurrentClassInfo.builder()
                .id(currentClass.getId())
                .code(currentClass.getCode())
                .name(currentClass.getName())
                .branchName(currentClass.getBranch().getName())
                .modality(currentClass.getModality().name())
                .scheduleDays(formatSchedule(currentClass))
                .scheduleTime("Varies by session") // Time slots vary per session
                .currentSession(currentSessionCount)
                .build();
    }


    @Override
    public boolean hasExceededTransferLimit(Long studentId, Long courseId) {
        long approvedTransfers = studentRequestRepository.countByStudentIdAndRequestTypeAndStatusAndTargetClassCourseId(
                studentId, StudentRequestType.TRANSFER, RequestStatus.APPROVED, courseId);

        return approvedTransfers >= 1; // Business rule: max 1 transfer per course
    }

    @Override
    public boolean isValidTransfer(Long currentClassId, Long targetClassId) {
        ClassEntity currentClass = classRepository.findByIdWithCourse(currentClassId)
                .orElseThrow(() -> new ResourceNotFoundException("Current class not found"));
        ClassEntity targetClass = classRepository.findByIdWithCourse(targetClassId)
                .orElseThrow(() -> new ResourceNotFoundException("Target class not found"));

        // Same course validation
        if (!currentClass.getCourse().getId().equals(targetClass.getCourse().getId())) {
            return false;
        }

        // Target class status validation
        if (!List.of(ClassStatus.SCHEDULED, ClassStatus.ONGOING).contains(targetClass.getStatus())) {
            return false;
        }

        // Capacity validation - use repository method to avoid lazy loading
        Integer currentEnrollment = classRepository.countEnrolledStudents(targetClass.getId());
        if (currentEnrollment != null && targetClass.getMaxCapacity() != null && 
            currentEnrollment >= targetClass.getMaxCapacity()) {
            return false;
        }

        return true;
    }

    @Override
    @Transactional
    public void executeTransfer(StudentRequest request) {
        log.info("Executing transfer for request {} - student: {} from class {} to class {}",
                request.getId(), request.getStudent().getId(),
                request.getCurrentClass().getId(), request.getTargetClass().getId());

        // 1. Re-validate transfer validity (race condition check)
        if (!isValidTransfer(request.getCurrentClass().getId(), request.getTargetClass().getId())) {
            throw new BusinessRuleException("TRANSFER_NO_LONGER_VALID", "Transfer is no longer valid");
        }

        // 2. Update old enrollment to TRANSFERRED status
        Enrollment oldEnrollment = enrollmentRepository.findByStudentIdAndClassId(
                request.getStudent().getId(), request.getCurrentClass().getId());

        if (oldEnrollment == null) {
            throw new ResourceNotFoundException("Current enrollment not found");
        }

        oldEnrollment.setStatus(EnrollmentStatus.TRANSFERRED);
        oldEnrollment.setLeftSessionId(request.getEffectiveSession().getId());
        oldEnrollment.setLeftAt(OffsetDateTime.now());
        oldEnrollment.setOverrideReason(String.format("Transferred to class %s on %s. Request ID: %d",
                request.getTargetClass().getCode(),
                OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                request.getId()));
        enrollmentRepository.save(oldEnrollment);

        // 3. Create new enrollment in target class
        Enrollment newEnrollment = Enrollment.builder()
                .studentId(request.getStudent().getId())
                .classId(request.getTargetClass().getId())
                .status(EnrollmentStatus.ENROLLED)
                .joinSessionId(request.getEffectiveSession().getId())
                .enrolledAt(OffsetDateTime.now())
                .enrolledBy(request.getDecidedBy() != null ? request.getDecidedBy().getId() : null)
                .capacityOverride(false)
                .overrideReason(String.format("Transferred from class %s. Request ID: %d",
                        request.getCurrentClass().getCode(), request.getId()))
                .build();

        enrollmentRepository.save(newEnrollment);

        // 4. Update student sessions in old class from effective date onwards to ABSENT
        // Use effectiveDate.minusDays(1) to include the effective date itself (session on 18/11 should be marked ABSENT)
        List<StudentSession> futureOldSessions = studentSessionRepository
                .findByStudentIdAndClassEntityIdAndSessionDateAfter(
                        request.getStudent().getId(),
                        request.getCurrentClass().getId(),
                        request.getEffectiveDate().minusDays(1));

        for (StudentSession session : futureOldSessions) {
            session.setAttendanceStatus(AttendanceStatus.ABSENT);
            session.setIsTransferredOut(true);
            session.setNote(String.format("Student transferred out. Request ID: %d", request.getId()));
        }
        studentSessionRepository.saveAll(futureOldSessions);

        // 5. Create student sessions for new class from effective date onwards
        // Use effectiveDate.minusDays(1) to include the effective date itself (session on 18/11 should be created)
        List<Session> futureNewSessions = sessionRepository
                .findByClassEntityIdAndDateAfter(request.getTargetClass().getId(), request.getEffectiveDate().minusDays(1));

        for (Session session : futureNewSessions) {
            StudentSession.StudentSessionId sessionId = new StudentSession.StudentSessionId(
                    request.getStudent().getId(), session.getId());

            StudentSession newStudentSession = StudentSession.builder()
                    .id(sessionId)
                    .student(request.getStudent())
                    .session(session)
                    .attendanceStatus(AttendanceStatus.PLANNED)
                    .note(String.format("Transferred in from class %s. Request ID: %d",
                            request.getCurrentClass().getCode(), request.getId()))
                    .build();

            studentSessionRepository.save(newStudentSession);
        }

        log.info("Transfer execution completed for request {}", request.getId());
        // TODO: Send notifications to student, old instructor, new instructor
    }

    // ============== HELPER METHODS FOR TRANSFER ==============

    private StudentRequestResponseDTO submitTransferRequestInternal(Long studentId, TransferRequestDTO dto,
                                                                   UserAccount submittedBy, boolean autoApprove) {
        log.info("Submitting transfer request for student {} - from: {} to: {}, effective: {}",
                studentId, dto.getCurrentClassId(), dto.getTargetClassId(), dto.getEffectiveDate());

        // 1. Validate current class exists and student is enrolled
        ClassEntity currentClass = classRepository.findByIdWithCourse(dto.getCurrentClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Current class not found"));

        Enrollment currentEnrollment = enrollmentRepository.findByStudentIdAndClassId(
                studentId, dto.getCurrentClassId());

        if (currentEnrollment == null || currentEnrollment.getStatus() != EnrollmentStatus.ENROLLED) {
            throw new BusinessRuleException("NOT_ENROLLED", "Student is not enrolled in current class");
        }

        // 2. Validate target class
        ClassEntity targetClass = classRepository.findByIdWithCourse(dto.getTargetClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Target class not found"));

        // 3. Business rule validations
        if (!isValidTransfer(dto.getCurrentClassId(), dto.getTargetClassId())) {
            throw new BusinessRuleException("INVALID_TRANSFER", "Transfer is not valid");
        }

        if (hasExceededTransferLimit(studentId, currentClass.getCourse().getId())) {
            throw new BusinessRuleException("TRANSFER_LIMIT_EXCEEDED", "Transfer limit exceeded for this course");
        }

        // 4. Session-aware validation: Verify session exists and hasn't started yet
        Session effectiveSession = sessionRepository.findById(dto.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with ID: " + dto.getSessionId()));

        // Verify session belongs to target class
        if (!effectiveSession.getClassEntity().getId().equals(dto.getTargetClassId())) {
            throw new BusinessRuleException("SESSION_CLASS_MISMATCH",
                    "Selected session does not belong to the target class");
        }

        // Validate session start time is in the future
        if (effectiveSession.getTimeSlotTemplate() != null) {
            LocalDate sessionDate = effectiveSession.getDate();
            LocalTime sessionStartTime = effectiveSession.getTimeSlotTemplate().getStartTime();

            // Combine date and time for comparison
            LocalDate today = LocalDate.now();
            LocalTime now = java.time.LocalTime.now();

            // Check if session is in the past
            if (sessionDate.isBefore(today) ||
                (sessionDate.equals(today) && sessionStartTime.isBefore(now))) {
                throw new BusinessRuleException("SESSION_ALREADY_STARTED",
                        "Cannot transfer to a session that has already started. Please select a future session.");
            }
        } else {
            // Fallback to date-only validation if time slot is not available
            if (effectiveSession.getDate().isBefore(LocalDate.now())) {
                throw new BusinessRuleException("PAST_EFFECTIVE_DATE",
                        "Effective date cannot be in the past");
            }
        }

        // Set effectiveDate from the session's date
        dto.setEffectiveDate(effectiveSession.getDate());

        // 5. Check duplicate pending transfer
        boolean hasDuplicate = studentRequestRepository.existsByStudentIdAndCurrentClassIdAndTargetClassIdAndRequestTypeAndStatusIn(
                studentId, dto.getCurrentClassId(), dto.getTargetClassId(), StudentRequestType.TRANSFER,
                List.of(RequestStatus.PENDING, RequestStatus.APPROVED));

        if (hasDuplicate) {
            throw new DuplicateRequestException("Duplicate transfer request for these classes");
        }

        // 6. Tier-based validation
        boolean requiresAAApproval = requiresAAApproval(currentClass, targetClass);
        if (requiresAAApproval && !autoApprove) {
            throw new BusinessRuleException("REQUIRES_AA_APPROVAL",
                    "This transfer requires Academic Affairs approval");
        }

        // 7. Create request
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        StudentRequest request = StudentRequest.builder()
                .student(student)
                .requestType(StudentRequestType.TRANSFER)
                .currentClass(currentClass)
                .targetClass(targetClass)
                .effectiveDate(dto.getEffectiveDate())
                .effectiveSession(effectiveSession)
                .requestReason(dto.getRequestReason())
                .note(dto.getNote())
                .status(autoApprove ? RequestStatus.APPROVED : RequestStatus.PENDING)
                .submittedBy(submittedBy)
                .submittedAt(OffsetDateTime.now())
                .build();

        // 8. If auto-approve, set decision info
        if (autoApprove) {
            request.setDecidedBy(submittedBy);
            request.setDecidedAt(OffsetDateTime.now());
        }

        request = studentRequestRepository.save(request);

        // 9. If auto-approved, execute transfer
        if (autoApprove) {
            executeTransfer(request);
        }

        log.info("Transfer request created with ID: {} - Status: {}", request.getId(), request.getStatus());
        return mapToStudentResponseDTO(request);
    }

    private boolean requiresAAApproval(ClassEntity currentClass, ClassEntity targetClass) {
        // Tier 1: Same branch AND same learning mode = auto-approve (student self-service)
        // Tier 2: Different branch OR different learning mode = requires AA approval
        
        // Add null safety checks - if branch info is missing, require AA approval
        if (currentClass.getBranch() == null || targetClass.getBranch() == null) {
            log.warn("Branch information missing for classes {} or {} - requiring AA approval", 
                     currentClass.getId(), targetClass.getId());
            return true;
        }
        
        // Check branch change
        boolean sameBranch = currentClass.getBranch().getId().equals(targetClass.getBranch().getId());
        
        // Check modality compatibility (HYBRID = OFFLINE for transfer purposes)
        boolean sameModality = isSameModalityForTransfer(currentClass.getModality(), targetClass.getModality());
        
        // Tier 1 (student self-service): same branch AND same modality
        // Tier 2 (requires AA): different branch OR different modality
        return !sameBranch || !sameModality;
    }
    
    /**
     * Check if two modalities are considered "same" for Tier 1 transfer purposes
     * HYBRID = OFFLINE (both are location-based learning)
     * ONLINE is distinct (remote learning)
     */
    private boolean isSameModalityForTransfer(Modality current, Modality target) {
        // ONLINE is distinct - must match exactly
        if (current == Modality.ONLINE || target == Modality.ONLINE) {
            return current == target;
        }
        
        // OFFLINE and HYBRID are treated as equivalent (both location-based)
        return (current == Modality.OFFLINE || current == Modality.HYBRID) &&
               (target == Modality.OFFLINE || target == Modality.HYBRID);
    }

    private int calculateUsedTransfers(Long studentId) {
        return (int) studentRequestRepository.countByStudentIdAndRequestTypeAndStatus(
                studentId, StudentRequestType.TRANSFER, RequestStatus.APPROVED);
    }

    private TransferEligibilityDTO.CurrentClassInfo mapToCurrentClassInfoWithQuota(Enrollment enrollment, Long studentId) {
        ClassEntity cls = enrollment.getClassEntity();

        // Safe navigation with null checks
        String courseName = "Unknown Course";
        Long courseId = null;
        if (cls.getCourse() != null) {
            courseId = cls.getCourse().getId();
            String subjectName = cls.getCourse().getSubject() != null ?
                cls.getCourse().getSubject().getName() : "Unknown Subject";
            String levelName = cls.getCourse().getLevel() != null ?
                cls.getCourse().getLevel().getName() : "Unknown Level";
            courseName = subjectName + " - " + levelName;
        }

        String branchName = cls.getBranch() != null ? cls.getBranch().getName() : "Unknown Branch";
        String scheduleInfo = cls.getStartDate() + " to " + cls.getPlannedEndDate();
        LocalDate enrollmentDate = enrollment.getEnrolledAt() != null ?
            enrollment.getEnrolledAt().toLocalDate() : LocalDate.now();

        // Calculate per-course quota
        int usedTransfersForCourse = 0;
        if (courseId != null) {
            usedTransfersForCourse = (int) studentRequestRepository.countByStudentIdAndRequestTypeAndStatusAndTargetClassCourseId(
                    studentId, StudentRequestType.TRANSFER, RequestStatus.APPROVED, courseId);
        }
        int maxTransfersPerCourse = 1; // Business rule
        int remainingTransfers = Math.max(0, maxTransfersPerCourse - usedTransfersForCourse);

        TransferEligibilityDTO.TransferQuotaInfo quotaInfo = TransferEligibilityDTO.TransferQuotaInfo.builder()
                .used(usedTransfersForCourse)
                .limit(maxTransfersPerCourse)
                .remaining(remainingTransfers)
                .build();

        // Check for pending transfer from this class
        boolean hasPendingTransfer = studentRequestRepository.existsPendingTransferFromClass(
                studentId, cls.getId(), StudentRequestType.TRANSFER, RequestStatus.PENDING);

        // Determine if transfer is allowed
        boolean canTransfer = remainingTransfers > 0 && !hasPendingTransfer;

        return TransferEligibilityDTO.CurrentClassInfo.builder()
                .enrollmentId(enrollment.getId())
                .classId(cls.getId())
                .classCode(cls.getCode())
                .className(cls.getName())
                .courseId(courseId)
                .courseName(courseName)
                .branchName(branchName)
                .learningMode(cls.getModality().name())
                .scheduleInfo(scheduleInfo)
                .enrollmentDate(enrollmentDate)
                .canTransfer(canTransfer)
                .hasPendingTransfer(hasPendingTransfer)
                .transferQuota(quotaInfo)
                .build();
    }

    private TransferOptionDTO mapToTransferOptionDTO(ClassEntity targetClass, ClassEntity currentClass) {
        // Safe navigation for transfer tier determination
        boolean sameBranch = targetClass.getBranch() != null && currentClass.getBranch() != null &&
                targetClass.getBranch().getId().equals(currentClass.getBranch().getId());
        boolean sameMode = targetClass.getModality() != null && currentClass.getModality() != null &&
                targetClass.getModality().equals(currentClass.getModality());
        boolean tier1Transfer = sameBranch && sameMode;

        // Analyze content gap with full algorithm
        TransferOptionDTO.ContentGapAnalysis contentGap = analyzeContentGap(currentClass.getId(), targetClass.getId());

        // Calculate current enrollment using repository method to avoid lazy loading
        Integer currentEnrollmentCount = classRepository.countEnrolledStudents(targetClass.getId());
        int currentEnrollment = currentEnrollmentCount != null ? currentEnrollmentCount : 0;
        int availableSlots = targetClass.getMaxCapacity() != null ?
                targetClass.getMaxCapacity() - currentEnrollment : 0;

        // Safe course name construction (now includes subject and level from enhanced query)
        String courseName = "Unknown Course";
        if (targetClass.getCourse() != null) {
            String subjectName = targetClass.getCourse().getSubject() != null ?
                targetClass.getCourse().getSubject().getName() : "Unknown Subject";
            String levelName = targetClass.getCourse().getLevel() != null ?
                targetClass.getCourse().getLevel().getName() : "Unknown Level";
            courseName = subjectName + " - " + levelName;
        }

        String branchName = targetClass.getBranch() != null ? targetClass.getBranch().getName() : "Unknown Branch";
        String scheduleInfo = targetClass.getStartDate() + " to " + targetClass.getPlannedEndDate();

        // Get upcoming sessions for effective date selection
        List<TransferOptionDTO.UpcomingSessionInfo> upcomingSessions = getUpcomingSessionsForTransfer(targetClass.getId());

        return TransferOptionDTO.builder()
                .classId(targetClass.getId())
                .classCode(targetClass.getCode())
                .className(targetClass.getName())
                .courseName(courseName)
                .branchName(branchName)
                .learningMode(targetClass.getModality().name())
                .scheduleInfo(scheduleInfo)
                .instructorName("TBD") // Placeholder - would need to implement proper instructor logic
                .currentEnrollment(currentEnrollment)
                .maxCapacity(targetClass.getMaxCapacity())
                .availableSlots(availableSlots)
                .startDate(targetClass.getStartDate())
                .endDate(targetClass.getPlannedEndDate())
                .status(targetClass.getStatus().name())
                .contentGapAnalysis(contentGap)
                .canTransfer(availableSlots > 0 && targetClass.getStatus() != null &&
                        List.of(ClassStatus.SCHEDULED, ClassStatus.ONGOING).contains(targetClass.getStatus()))
                .upcomingSessions(upcomingSessions)
                .build();
    }

    /**
     * Analyze content gap between current class and target class
     * Implements the algorithm from transfer-request.md lines 580-618
     *
     * @param currentClassId Current class ID
     * @param targetClassId Target class ID
     * @return ContentGapAnalysis with gap level, missed sessions, and recommendations
     */
    private TransferOptionDTO.ContentGapAnalysis analyzeContentGap(Long currentClassId, Long targetClassId) {
        // Step 1: Get completed course sessions in current class
        List<Session> completedSessions = sessionRepository.findByClassIdAndStatusIn(
                currentClassId,
                List.of(SessionStatus.DONE, SessionStatus.CANCELLED)
        );

        // Extract completed course session IDs
        Set<Long> completedCourseSessionIds = completedSessions.stream()
                .filter(s -> s.getCourseSession() != null)
                .map(s -> s.getCourseSession().getId())
                .collect(Collectors.toSet());

        // Step 2: Get target class's past sessions (already happened)
        List<Session> targetPastSessions = sessionRepository.findByClassIdAndDateBefore(
                targetClassId,
                LocalDate.now()
        );

        // Step 3: Find gap: sessions target class covered but current class hasn't
        List<Session> gapSessions = targetPastSessions.stream()
                .filter(s -> s.getCourseSession() != null)
                .filter(s -> !completedCourseSessionIds.contains(s.getCourseSession().getId()))
                .collect(Collectors.toList());

        // Step 4: Calculate severity
        int gapCount = gapSessions.size();
        String gapLevel = determineGapLevel(gapCount);

        // Step 5: Map to GapSessionInfo
        List<TransferOptionDTO.GapSessionInfo> gapSessionInfos = gapSessions.stream()
                .map(session -> TransferOptionDTO.GapSessionInfo.builder()
                        .courseSessionNumber(session.getCourseSession().getSequenceNo())
                        .courseSessionTitle(session.getCourseSession().getTopic())
                        .scheduledDate(session.getDate())
                        .build())
                .collect(Collectors.toList());

        // Step 6: Calculate total sessions (from course)
        int totalSessions = completedSessions.size() + gapCount;

        // Step 7: Generate recommendations
        List<String> recommendations = generateRecommendations(gapLevel, gapCount);

        // Step 8: Generate impact description
        String impactDescription = generateImpactDescription(gapLevel, gapCount);

        return TransferOptionDTO.ContentGapAnalysis.builder()
                .gapLevel(gapLevel)
                .missedSessions(gapCount)
                .totalSessions(totalSessions)
                .gapSessions(gapSessionInfos)
                .recommendedActions(recommendations)
                .impactDescription(impactDescription)
                .build();
    }

    /**
     * Determine gap level based on missed sessions count
     * As per spec: NONE (0), MINOR (1-2), MODERATE (3-5), MAJOR (>5)
     */
    private String determineGapLevel(int gapCount) {
        if (gapCount == 0) return "NONE";
        if (gapCount <= 2) return "MINOR";
        if (gapCount <= 5) return "MODERATE";
        return "MAJOR";
    }

    /**
     * Generate recommendations based on gap level
     */
    private List<String> generateRecommendations(String gapLevel, int gapCount) {
        return switch (gapLevel) {
            case "NONE" -> List.of("No action needed. You're on the same pace.");
            case "MINOR" -> List.of(
                    "Review materials for missed topics",
                    "Consider requesting makeup sessions if available"
            );
            case "MODERATE" -> List.of(
                    "Review course materials carefully",
                    "Request makeup sessions for critical topics",
                    "Consult with instructor about catch-up plan"
            );
            case "MAJOR" -> List.of(
                    "Significant content gap detected",
                    "Strongly recommend consultation with Academic Affairs",
                    "May need extensive catch-up or consider different class timing"
            );
            default -> List.of();
        };
    }

    /**
     * Generate impact description based on gap level
     */
    private String generateImpactDescription(String gapLevel, int gapCount) {
        return switch (gapLevel) {
            case "NONE" -> "No content gap. Both classes are at the same pace.";
            case "MINOR" -> String.format("Minor gap: You will miss %d session(s). Review materials recommended.", gapCount);
            case "MODERATE" -> String.format("Moderate gap: %d sessions behind. Catch-up required.", gapCount);
            case "MAJOR" -> String.format("Major gap: %d sessions behind. Consider alternative options.", gapCount);
            default -> "Content gap analysis unavailable.";
        };
    }

    /**
     * Get upcoming sessions for transfer effective date selection
     * Returns future PLANNED sessions (not CANCELLED or DONE)
     *
     * @param targetClassId Target class ID
     * @return List of UpcomingSessionInfo for date picker
     */
    private List<TransferOptionDTO.UpcomingSessionInfo> getUpcomingSessionsForTransfer(Long targetClassId) {
        // Get all future PLANNED sessions (date >= today)
        List<Session> upcomingSessions = sessionRepository
                .findByClassEntityIdAndDateGreaterThanEqualAndStatusOrderByDateAsc(
                        targetClassId,
                        LocalDate.now(),
                        SessionStatus.PLANNED
                );

        // Map to UpcomingSessionInfo DTO
        return upcomingSessions.stream()
                .map(session -> {
                    // Format time slot if available
                    String timeSlot = "TBD";
                    if (session.getTimeSlotTemplate() != null) {
                        timeSlot = session.getTimeSlotTemplate().getStartTime() + " - " + 
                                   session.getTimeSlotTemplate().getEndTime();
                    }

                    return TransferOptionDTO.UpcomingSessionInfo.builder()
                            .sessionId(session.getId())
                            .date(session.getDate())
                            .courseSessionNumber(session.getCourseSession() != null ?
                                    session.getCourseSession().getSequenceNo() : null)
                            .courseSessionTitle(session.getCourseSession() != null ?
                                    session.getCourseSession().getTopic() : "Session")
                            .timeSlot(timeSlot)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Map Enrollment to StudentClassDTO with full class details
     */
    private StudentClassDTO mapToStudentClassDTO(Enrollment enrollment) {
        ClassEntity classEntity = enrollment.getClassEntity();
        Course course = classEntity.getCourse();
        Branch branch = classEntity.getBranch();

        // Get schedule summary
        String scheduleSummary = buildScheduleSummary(classEntity);

        // Get all instructors with SCHEDULED status
        List<String> instructors = getInstructors(classEntity.getId());

        // Calculate session statistics using existing queries
        List<Session> allSessions = sessionRepository.findByClassIdAndStatusIn(
                classEntity.getId(),
                List.of(SessionStatus.PLANNED, SessionStatus.DONE, SessionStatus.CANCELLED));
        int totalSessions = allSessions.size();

        int completedSessions = (int) allSessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.DONE)
                .count();

        // Get attendance statistics
        Long studentId = enrollment.getStudent().getId();
        List<StudentSession> studentSessions = studentSessionRepository
                .findByStudentIdAndClassEntityId(studentId, classEntity.getId());

        int attendedSessions = (int) studentSessions.stream()
                .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.PRESENT)
                .count();
        double attendanceRate = totalSessions > 0 ? (attendedSessions * 100.0) / totalSessions : 0.0;

        return StudentClassDTO.builder()
                .classId(classEntity.getId())
                .classCode(classEntity.getCode())
                .className(classEntity.getName())
                .courseId(course.getId())
                .courseName(course.getName())
                .courseCode(course.getCode())
                .branchId(branch.getId())
                .branchName(branch.getName())
                .modality(classEntity.getModality().name())
                .status(classEntity.getStatus().name())
                .startDate(classEntity.getStartDate())
                .endDate(classEntity.getPlannedEndDate())
                .scheduleSummary(scheduleSummary)
                .enrollmentId(enrollment.getId())
                .enrollmentDate(enrollment.getEnrolledAt().toLocalDate())
                .enrollmentStatus(enrollment.getStatus().name())
                .totalSessions(totalSessions)
                .completedSessions(completedSessions)
                .attendedSessions(attendedSessions)
                .attendanceRate(Math.round(attendanceRate * 100.0) / 100.0)
                .instructorNames(instructors)
                .build();
    }

    /**
     * Get all instructors with SCHEDULED status (no duplicates)
     */
    private List<String> getInstructors(Long classId) {
        // Find all sessions for the class
        List<Session> sessions = sessionRepository.findByClassIdAndStatusIn(
                classId,
                List.of(SessionStatus.PLANNED, SessionStatus.DONE, SessionStatus.CANCELLED));

        Set<String> instructorNames = new LinkedHashSet<>();

        for (Session session : sessions) {
            if (session.getTeachingSlots() != null) {
                for (TeachingSlot slot : session.getTeachingSlots()) {
                    // Only get teachers with SCHEDULED status
                    if (slot.getStatus() == TeachingSlotStatus.SCHEDULED &&
                        slot.getTeacher() != null && slot.getTeacher().getUserAccount() != null) {
                        String teacherName = slot.getTeacher().getUserAccount().getFullName();
                        instructorNames.add(teacherName);
                    }
                }
            }
        }

        return new ArrayList<>(instructorNames);
    }

    @Override
    public List<StudentClassDTO> getMyClassesForStudent(Long studentId) {
        // Verify student exists
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + studentId));

        // Get active enrollments for the student
        List<Enrollment> enrollments = enrollmentRepository.findByStudentIdAndStatusIn(
                studentId, List.of(EnrollmentStatus.ENROLLED));

        // Map to DTOs using the same logic as getMyClasses
        return enrollments.stream()
                .map(this::mapToStudentClassDTO)
                .collect(Collectors.toList());
    }

    /**
     * Build schedule summary string (e.g., "Mon, Wed, Fri | 09:00-11:00")
     */
    private String buildScheduleSummary(ClassEntity classEntity) {
        if (classEntity.getScheduleDays() == null || classEntity.getScheduleDays().length == 0) {
            return "Schedule not available";
        }

        // Convert schedule days to day names
        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        List<String> scheduleDayNames = Arrays.stream(classEntity.getScheduleDays())
                .map(day -> dayNames[day])
                .collect(Collectors.toList());

        // Get time slot from first session
        List<Session> sessions = sessionRepository.findByClassIdAndStatusIn(
                classEntity.getId(),
                List.of(SessionStatus.PLANNED, SessionStatus.DONE));

        if (sessions.isEmpty()) {
            return String.join(", ", scheduleDayNames);
        }

        Session firstSession = sessions.get(0);
        if (firstSession.getTimeSlotTemplate() != null) {
            TimeSlotTemplate template = firstSession.getTimeSlotTemplate();
            return String.format("%s | %s-%s",
                    String.join(", ", scheduleDayNames),
                    template.getStartTime(),
                    template.getEndTime());
        }

        return String.join(", ", scheduleDayNames);
    }

    // ==================== STUDENT SCHEDULE METHODS FOR AA ====================

    @Override
    public WeeklyScheduleResponseDTO getWeeklySchedule(Long studentId, LocalDate weekStart) {
        log.info("AA getting weekly schedule for student: {}, week: {}", studentId, weekStart);
        return studentScheduleService.getWeeklySchedule(studentId, weekStart);
    }

    @Override
    public WeeklyScheduleResponseDTO getWeeklyScheduleByClass(Long studentId, Long classId, LocalDate weekStart) {
        log.info("AA getting weekly schedule for student: {}, class: {}, week: {}", studentId, classId, weekStart);
        return studentScheduleService.getWeeklyScheduleByClass(studentId, classId, weekStart);
    }

    @Override
    public LocalDate getCurrentWeekStart() {
        return studentScheduleService.getCurrentWeekStart();
    }
}
