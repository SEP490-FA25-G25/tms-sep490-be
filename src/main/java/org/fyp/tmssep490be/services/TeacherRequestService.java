package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.teacherrequest.MySessionDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestApproveDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestCreateDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestListDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestResponseDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.ModalityResourceSuggestionDTO;
import org.fyp.tmssep490be.entities.Resource;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.Teacher;
import org.fyp.tmssep490be.entities.TeacherRequest;
import org.fyp.tmssep490be.entities.TimeSlotTemplate;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.enums.Modality;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.entities.enums.TeacherRequestType;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.ResourceRepository;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.repositories.TeacherRepository;
import org.fyp.tmssep490be.repositories.TeacherRequestRepository;
import org.fyp.tmssep490be.repositories.TimeSlotTemplateRepository;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.fyp.tmssep490be.repositories.UserBranchesRepository;
import org.fyp.tmssep490be.repositories.SessionResourceRepository;
import org.fyp.tmssep490be.services.PolicyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherRequestService {

    private final TeacherRequestRepository teacherRequestRepository;
    private final TeacherRepository teacherRepository;
    private final SessionRepository sessionRepository;
    private final UserBranchesRepository userBranchesRepository;
    private final UserAccountRepository userAccountRepository;
    private final ResourceRepository resourceRepository;
    private final TimeSlotTemplateRepository timeSlotTemplateRepository;
    private final PolicyService policyService;
    private final SessionResourceRepository sessionResourceRepository;

    // Giáo viên tạo yêu cầu (hiện mới hỗ trợ MODALITY_CHANGE)
    @Transactional
    public TeacherRequestResponseDTO createRequest(TeacherRequestCreateDTO createDTO, Long userId) {
        log.info("Creating teacher request type {} for user {}", createDTO.getRequestType(), userId);

        Teacher teacher = teacherRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND, "Teacher profile not found for current user"));

        // Validate lý do tối thiểu theo policy
        int minReasonLength = policyService.getGlobalInt("teacher.request.reason_min_length", 15);
        String reason = createDTO.getReason() != null ? createDTO.getReason().trim() : "";
        if (reason.length() < minReasonLength) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Reason must be at least " + minReasonLength + " characters");
        }

        // Lấy session và kiểm tra quyền sở hữu
        Session session = sessionRepository.findById(createDTO.getSessionId())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT, "Session not found"));

        boolean isOwner = session.getTeachingSlots().stream()
                .anyMatch(slot -> slot.getTeacher() != null && slot.getTeacher().getId().equals(teacher.getId()));
        if (!isOwner) {
            throw new CustomException(ErrorCode.FORBIDDEN, "You are not assigned to this session");
        }

        // Chỉ cho phép tạo yêu cầu cho buổi PLANNED và còn đủ thời gian theo policy
        if (session.getStatus() != SessionStatus.PLANNED) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Session is not in PLANNED status");
        }
        int minDaysBeforeSession = policyService.getGlobalInt("teacher.request.min_days_before_session", 1);
        LocalDate minAllowedDate = LocalDate.now().plusDays(minDaysBeforeSession);
        if (session.getDate().isBefore(minAllowedDate)) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Session is too close to request (" + minDaysBeforeSession + " day(s) required)");
        }

        TeacherRequestType requestType = createDTO.getRequestType();
        if (requestType != TeacherRequestType.MODALITY_CHANGE) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Only MODALITY_CHANGE is supported at the moment");
        }

        TeacherRequest request = new TeacherRequest();
        request.setTeacher(teacher);
        request.setSession(session);
        request.setRequestType(requestType);
        request.setStatus(RequestStatus.PENDING);
        request.setSubmittedAt(OffsetDateTime.now());
        request.setSubmittedBy(teacher.getUserAccount());
        request.setRequestReason(reason);

        // MODALITY_CHANGE: yêu cầu resource (phòng) theo policy
        boolean requireResource = policyService.getGlobalBoolean("teacher.modality_change.require_resource", true);
        Long newResourceId = createDTO.getNewResourceId();
        if (requireResource && newResourceId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Resource is required for MODALITY_CHANGE requests");
        }
        if (newResourceId != null) {
            Resource resource = resourceRepository.findById(newResourceId)
                    .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT, "Resource not found"));
            request.setNewResource(resource);
        }

        request = teacherRequestRepository.save(request);
        log.info("Created teacher request {} for user {}", request.getId(), userId);

        return mapToResponseDTO(request);
    }

    // Gợi ý resource khả dụng cho yêu cầu đổi phương thức
    @Transactional(readOnly = true)
    public List<ModalityResourceSuggestionDTO> suggestModalityResources(Long sessionId, Long userId) {
        log.info("Suggest modality resources for session {} by user {}", sessionId, userId);

        Teacher teacher = teacherRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND, "Teacher profile not found for current user"));

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT, "Session not found"));

        // Quyền: giáo viên phải là người dạy buổi này
        boolean isOwner = session.getTeachingSlots().stream()
                .anyMatch(slot -> slot.getTeacher() != null && slot.getTeacher().getId().equals(teacher.getId()));
        if (!isOwner) {
            throw new CustomException(ErrorCode.FORBIDDEN, "You are not assigned to this session");
        }

        // Chỉ gợi ý cho session PLANNED
        if (session.getStatus() != SessionStatus.PLANNED) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Session is not in PLANNED status");
        }

        if (session.getClassEntity() == null || session.getClassEntity().getBranch() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Session branch is missing");
        }
        if (session.getTimeSlotTemplate() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Session time slot is missing");
        }

        Long branchId = session.getClassEntity().getBranch().getId();
        Long timeSlotId = session.getTimeSlotTemplate().getId();
        LocalDate sessionDate = session.getDate();

        // Resource hiện tại (nếu có)
        Long currentResourceId = session.getSessionResources().stream()
                .findFirst()
                .map(sr -> sr.getResource() != null ? sr.getResource().getId() : null)
                .orElse(null);

        List<Resource> resources = resourceRepository.findAvailableResourcesForSession(
                branchId, sessionDate, timeSlotId, sessionId);

        // Nếu buổi đang gán resource hiện tại, đảm bảo vẫn xuất hiện trong list
        if (currentResourceId != null) {
            boolean containsCurrent = resources.stream().anyMatch(r -> r.getId().equals(currentResourceId));
            if (!containsCurrent) {
                resourceRepository.findById(currentResourceId).ifPresent(resources::add);
            }
        }

        return resources.stream()
                .map(r -> ModalityResourceSuggestionDTO.builder()
                        .resourceId(r.getId())
                        .name(r.getName())
                        .resourceType(r.getResourceType() != null ? r.getResourceType().name() : null)
                        .capacity(r.getCapacity())
                        .branchId(r.getBranch() != null ? r.getBranch().getId() : null)
                        .currentResource(currentResourceId != null && currentResourceId.equals(r.getId()))
                        .build())
                .sorted((a, b) -> {
                    // current resource lên đầu, sau đó tên
                    if (a.isCurrentResource() && !b.isCurrentResource()) return -1;
                    if (!a.isCurrentResource() && b.isCurrentResource()) return 1;
                    if (a.getName() == null) return 1;
                    if (b.getName() == null) return -1;
                    return a.getName().compareToIgnoreCase(b.getName());
                })
                .collect(Collectors.toList());
    }

    //Lấy tất cả yêu cầu của giáo viên theo userId
    public List<TeacherRequestListDTO> getMyRequests(Long userId) {
        log.debug("Getting requests for user {}", userId);

        // Lấy teacher từ userAccountId
        Teacher teacher = teacherRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "Teacher profile not found for current user"));

        // Lấy tất cả yêu cầu: cả yêu cầu do giáo viên tạo và yêu cầu mà giáo viên là người thay thế
        List<TeacherRequest> requests = teacherRequestRepository
                .findByTeacherIdOrReplacementTeacherIdOrderBySubmittedAtDesc(teacher.getId());

        // Map sang DTO
        return requests.stream()
                .map(this::mapToListDTO)
                .collect(Collectors.toList());
    }

    //Chuyển đổi TeacherRequest entity sang TeacherRequestListDTO
    private TeacherRequestListDTO mapToListDTO(TeacherRequest request) {
        Session session = request.getSession();
        org.fyp.tmssep490be.entities.ClassEntity classEntity = session != null ? session.getClassEntity() : null;
        org.fyp.tmssep490be.entities.SubjectSession subjectSession = session != null ? session.getSubjectSession() : null;
        Teacher teacher = request.getTeacher();
        UserAccount teacherAccount = teacher != null ? teacher.getUserAccount() : null;
        TimeSlotTemplate timeSlot = session != null ? session.getTimeSlotTemplate() : null;

        Teacher replacementTeacher = request.getReplacementTeacher();
        UserAccount replacementAccount = replacementTeacher != null ? replacementTeacher.getUserAccount() : null;

        TimeSlotTemplate newTimeSlot = request.getNewTimeSlot();

        UserAccount decidedBy = request.getDecidedBy();

        // Xử lý modality cho MODALITY_CHANGE
        Modality currentModality = null;
        Modality newModality = null;
        if (request.getRequestType() == TeacherRequestType.MODALITY_CHANGE) {
            if (classEntity != null) {
                currentModality = classEntity.getModality();
            }
        }

        return TeacherRequestListDTO.builder()
                .id(request.getId())
                .requestType(request.getRequestType())
                .status(request.getStatus())
                .sessionId(session != null ? session.getId() : null)
                .sessionDate(session != null ? session.getDate() : null)
                .sessionStartTime(timeSlot != null ? timeSlot.getStartTime() : null)
                .sessionEndTime(timeSlot != null ? timeSlot.getEndTime() : null)
                .className(classEntity != null ? classEntity.getName() : null)
                .classCode(classEntity != null ? classEntity.getCode() : null)
                .sessionTopic(subjectSession != null ? subjectSession.getTopic() : null)
                .teacherId(teacher != null ? teacher.getId() : null)
                .teacherName(teacherAccount != null ? teacherAccount.getFullName() : null)
                .teacherEmail(teacherAccount != null ? teacherAccount.getEmail() : null)
                .replacementTeacherName(replacementAccount != null ? replacementAccount.getFullName() : null)
                .newSessionDate(request.getNewDate())
                .newSessionStartTime(newTimeSlot != null ? newTimeSlot.getStartTime() : null)
                .newSessionEndTime(newTimeSlot != null ? newTimeSlot.getEndTime() : null)
                .requestReason(request.getRequestReason())
                .submittedAt(request.getSubmittedAt())
                .decidedAt(request.getDecidedAt())
                .decidedById(decidedBy != null ? decidedBy.getId() : null)
                .decidedByName(decidedBy != null ? decidedBy.getFullName() : null)
                .decidedByEmail(decidedBy != null ? decidedBy.getEmail() : null)
                .currentModality(currentModality)
                .newModality(newModality)
                .build();
    }

    //Lấy danh sách yêu cầu giáo viên cho Academic Staff với optional status filter
    //Chỉ trả về requests của giáo viên trong cùng branch với academic staff
    @Transactional(readOnly = true)
    public List<TeacherRequestListDTO> getRequestsForStaff(RequestStatus status, Long academicStaffUserId) {
        log.info("Getting teacher requests for staff {} with status {}", academicStaffUserId, status);
        
        // Lấy branch IDs của academic staff
        List<Long> academicBranchIds = getBranchIdsForUser(academicStaffUserId);
        
        if (academicBranchIds.isEmpty()) {
            log.warn("Academic staff {} has no branches assigned", academicStaffUserId);
            return List.of();
        }
        
        // Lấy tất cả requests với status filter (nếu có)
        List<TeacherRequest> allRequests = status != null
                ? teacherRequestRepository.findByStatusOrderBySubmittedAtDesc(status)
                : teacherRequestRepository.findAllByOrderBySubmittedAtDesc();
        
        // Filter requests: chỉ lấy requests của giáo viên trong cùng branch với academic staff
        List<TeacherRequest> filteredRequests = allRequests.stream()
                .filter(request -> {
                    if (request.getTeacher() == null || request.getTeacher().getUserAccount() == null) {
                        return false;
                    }
                    Long teacherUserAccountId = request.getTeacher().getUserAccount().getId();
                    List<Long> teacherBranchIds = getBranchIdsForUser(teacherUserAccountId);
                    return teacherBranchIds.stream().anyMatch(academicBranchIds::contains);
                })
                .collect(Collectors.toList());
        
        // Map sang DTO
        return filteredRequests.stream()
                .map(this::mapToListDTO)
                .collect(Collectors.toList());
    }

    //Helper method để lấy branch IDs của user
    private List<Long> getBranchIdsForUser(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return userBranchesRepository.findBranchIdsByUserId(userId);
    }

    //Lấy chi tiết request theo ID cho teacher
    @Transactional(readOnly = true)
    public TeacherRequestResponseDTO getRequestById(Long requestId, Long userId) {
        log.info("Getting request {} for user {}", requestId, userId);

        TeacherRequest request = teacherRequestRepository.findByIdWithTeacherAndSession(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_REQUEST_NOT_FOUND, "Request not found"));

        // Force load relationships
        if (request.getNewResource() != null) {
            request.getNewResource().getName();
        }
        if (request.getNewTimeSlot() != null) {
            request.getNewTimeSlot().getName();
        }

        // Kiểm tra quyền truy cập
        Teacher teacher = teacherRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND, "Teacher not found"));

        boolean isRequestOwner = request.getTeacher() != null && request.getTeacher().getId().equals(teacher.getId());
        boolean isReplacementTeacher = request.getReplacementTeacher() != null &&
                request.getReplacementTeacher().getId().equals(teacher.getId());

        if (!isRequestOwner && !isReplacementTeacher) {
            throw new CustomException(ErrorCode.FORBIDDEN, "You don't have permission to view this request");
        }

        return mapToResponseDTO(request);
    }

    //Lấy chi tiết request theo ID cho academic staff (không cần kiểm tra authorization, đã filter theo branch ở list)
    @Transactional(readOnly = true)
    public TeacherRequestResponseDTO getRequestForStaff(Long requestId) {
        log.info("Getting request {} for staff", requestId);

        TeacherRequest request = teacherRequestRepository.findByIdWithTeacherAndSession(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_REQUEST_NOT_FOUND, "Request not found"));

        // Force load relationships
        if (request.getNewResource() != null) {
            request.getNewResource().getName();
        }
        if (request.getNewTimeSlot() != null) {
            request.getNewTimeSlot().getName();
        }

        return mapToResponseDTO(request);
    }

    //Map TeacherRequest entity sang TeacherRequestResponseDTO
    private TeacherRequestResponseDTO mapToResponseDTO(TeacherRequest request) {
        Session session = request.getSession();
        org.fyp.tmssep490be.entities.ClassEntity classEntity = session != null ? session.getClassEntity() : null;
        org.fyp.tmssep490be.entities.SubjectSession subjectSession = session != null ? session.getSubjectSession() : null;
        TimeSlotTemplate timeSlot = session != null ? session.getTimeSlotTemplate() : null;
        Teacher teacher = request.getTeacher();
        UserAccount teacherAccount = teacher != null ? teacher.getUserAccount() : null;
        Teacher replacementTeacher = request.getReplacementTeacher();
        UserAccount replacementTeacherAccount = replacementTeacher != null ? replacementTeacher.getUserAccount() : null;
        UserAccount decidedBy = request.getDecidedBy();
        Resource newResource = request.getNewResource();
        TimeSlotTemplate newTimeSlot = request.getNewTimeSlot();

        TeacherRequestResponseDTO.TeacherRequestResponseDTOBuilder builder = TeacherRequestResponseDTO.builder()
                .id(request.getId())
                .requestType(request.getRequestType())
                .status(request.getStatus())
                .sessionId(session != null ? session.getId() : null)
                .classCode(classEntity != null ? classEntity.getCode() : null)
                .sessionDate(session != null ? session.getDate() : null)
                .sessionStartTime(timeSlot != null ? timeSlot.getStartTime() : null)
                .sessionEndTime(timeSlot != null ? timeSlot.getEndTime() : null)
                .sessionTopic(subjectSession != null ? subjectSession.getTopic() : null)
                .teacherId(teacher != null ? teacher.getId() : null)
                .teacherName(teacherAccount != null ? teacherAccount.getFullName() : null)
                .teacherEmail(teacherAccount != null ? teacherAccount.getEmail() : null)
                .requestReason(request.getRequestReason())
                .note(request.getNote())
                .submittedAt(request.getSubmittedAt())
                .decidedAt(request.getDecidedAt())
                .decidedById(decidedBy != null ? decidedBy.getId() : null)
                .decidedByName(decidedBy != null ? decidedBy.getFullName() : null)
                .decidedByEmail(decidedBy != null ? decidedBy.getEmail() : null);

        // Hiển thị thông tin dựa trên yêu cầu
        switch (request.getRequestType()) {
            case MODALITY_CHANGE:
                builder.newResourceId(newResource != null ? newResource.getId() : null)
                        .newResourceName(newResource != null ? newResource.getName() : null)
                        .newDate(null)
                        .newTimeSlotStartTime(null)
                        .newTimeSlotEndTime(null)
                        .newTimeSlotName(null)
                        .replacementTeacherId(null)
                        .replacementTeacherName(null)
                        .replacementTeacherEmail(null)
                        .newSessionId(null);
                break;
            case RESCHEDULE:
                builder.newDate(request.getNewDate())
                        .newTimeSlotStartTime(newTimeSlot != null ? newTimeSlot.getStartTime() : null)
                        .newTimeSlotEndTime(newTimeSlot != null ? newTimeSlot.getEndTime() : null)
                        .newTimeSlotName(newTimeSlot != null ? newTimeSlot.getName() : null)
                        .newResourceId(newResource != null ? newResource.getId() : null)
                        .newResourceName(newResource != null ? newResource.getName() : null)
                        .newSessionId(request.getNewSession() != null ? request.getNewSession().getId() : null)
                        .replacementTeacherId(null)
                        .replacementTeacherName(null)
                        .replacementTeacherEmail(null);
                break;
            case REPLACEMENT:
                builder.replacementTeacherId(replacementTeacher != null ? replacementTeacher.getId() : null)
                        .replacementTeacherName(replacementTeacherAccount != null ? replacementTeacherAccount.getFullName() : null)
                        .replacementTeacherEmail(replacementTeacherAccount != null ? replacementTeacherAccount.getEmail() : null)
                        .newDate(null)
                        .newTimeSlotStartTime(null)
                        .newTimeSlotEndTime(null)
                        .newTimeSlotName(null)
                        .newResourceId(null)
                        .newResourceName(null)
                        .newSessionId(null);
                break;
            default:
                builder.newDate(null)
                        .newTimeSlotStartTime(null)
                        .newTimeSlotEndTime(null)
                        .newTimeSlotName(null)
                        .newResourceId(null)
                        .newResourceName(null)
                        .replacementTeacherId(null)
                        .replacementTeacherName(null)
                        .replacementTeacherEmail(null)
                        .newSessionId(null);
                break;
        }

        return builder.build();
    }

    //Lấy buổi học sắp tới của giáo viên theo teacher ID
    @Transactional(readOnly = true)
    public List<MySessionDTO> getFutureSessionsForTeacher(Long userId, Integer days, Long classId) {
        Teacher teacher = teacherRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND, "Teacher not found"));

        int windowDays = (days != null && days > 0) ? days : 14;
        LocalDate fromDate = LocalDate.now();
        LocalDate toDate = fromDate.plusDays(windowDays);

        List<Session> sessions = sessionRepository.findUpcomingSessionsForTeacher(
                teacher.getId(), fromDate, toDate, classId);

        return sessions.stream()
                .map(this::mapToMySessionDTO)
                .collect(Collectors.toList());
    }

    private MySessionDTO mapToMySessionDTO(Session session) {
        if (session == null) return null;
        TimeSlotTemplate timeSlot = session.getTimeSlotTemplate();
        org.fyp.tmssep490be.entities.ClassEntity classEntity = session.getClassEntity();
        org.fyp.tmssep490be.entities.Subject subject = classEntity != null ? classEntity.getSubject() : null;
        org.fyp.tmssep490be.entities.SubjectSession subjectSession = session.getSubjectSession();

        return MySessionDTO.builder()
                .sessionId(session.getId())
                .date(session.getDate())
                .startTime(timeSlot != null ? timeSlot.getStartTime() : null)
                .endTime(timeSlot != null ? timeSlot.getEndTime() : null)
                .className(classEntity != null ? classEntity.getName() : null)
                .subjectName(subject != null ? subject.getName() : null)
                .topic(subjectSession != null ? subjectSession.getTopic() : null)
                .hasPendingRequest(false)
                .build();
    }

    //Duyệt yêu cầu giáo viên (Academic Staff)
    //Tất cả thông tin bắt buộc phải được điền khi approve
    @Transactional
    public TeacherRequestResponseDTO approveRequest(Long requestId, TeacherRequestApproveDTO approveDTO, Long academicStaffUserId) {
        log.info("Approving request {} by academic staff {}", requestId, academicStaffUserId);

        // Lấy request với đầy đủ relationships
        TeacherRequest request = teacherRequestRepository.findByIdWithTeacherAndSession(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_REQUEST_NOT_FOUND, "Request not found"));

        // Kiểm tra status phải là PENDING
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Request is not in PENDING status");
        }

        // Kiểm tra quyền: academic staff phải có branch trùng với teacher
        List<Long> academicBranchIds = getBranchIdsForUser(academicStaffUserId);
        if (academicBranchIds.isEmpty()) {
            throw new CustomException(ErrorCode.FORBIDDEN, "Academic staff has no branches assigned");
        }
        Long teacherUserAccountId = request.getTeacher().getUserAccount().getId();
        List<Long> teacherBranchIds = getBranchIdsForUser(teacherUserAccountId);
        boolean hasAccess = teacherBranchIds.stream().anyMatch(academicBranchIds::contains);
        if (!hasAccess) {
            throw new CustomException(ErrorCode.FORBIDDEN, "You don't have permission to approve this request");
        }

        // Lấy user account của academic staff
        UserAccount academicStaffAccount = userAccountRepository.findById(academicStaffUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "Academic staff not found"));

        // Validate và xử lý theo request type
        switch (request.getRequestType()) {
            case REPLACEMENT:
                validateAndApproveReplacement(request, approveDTO);
                request.setStatus(RequestStatus.WAITING_CONFIRM); // Chờ replacement teacher confirm
                break;
            case MODALITY_CHANGE:
                validateAndApproveModalityChange(request, approveDTO);
                request.setStatus(RequestStatus.APPROVED);
                break;
            case RESCHEDULE:
                validateAndApproveReschedule(request, approveDTO);
                request.setStatus(RequestStatus.APPROVED);
                break;
            default:
                throw new CustomException(ErrorCode.INVALID_INPUT, "Invalid request type");
        }

        // Cập nhật thông tin quyết định
        request.setDecidedBy(academicStaffAccount);
        request.setDecidedAt(OffsetDateTime.now());
        if (approveDTO.getNote() != null && !approveDTO.getNote().trim().isEmpty()) {
            request.setNote(approveDTO.getNote().trim());
        }

        request = teacherRequestRepository.save(request);
        log.info("Request {} approved successfully by academic staff {}", requestId, academicStaffUserId);

        return mapToResponseDTO(request);
    }

    //Validate và approve REPLACEMENT request
    private void validateAndApproveReplacement(TeacherRequest request, TeacherRequestApproveDTO approveDTO) {
        // REPLACEMENT: replacementTeacherId bắt buộc
        if (approveDTO.getReplacementTeacherId() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Replacement teacher ID is required for REPLACEMENT requests");
        }

        // Kiểm tra replacement teacher tồn tại
        Teacher replacementTeacher = teacherRepository.findById(approveDTO.getReplacementTeacherId())
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND, "Replacement teacher not found"));

        // Không được chọn chính giáo viên tạo request
        if (replacementTeacher.getId().equals(request.getTeacher().getId())) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Replacement teacher cannot be the same as the requesting teacher");
        }

        // Cập nhật replacement teacher
        request.setReplacementTeacher(replacementTeacher);

    }

    //Validate và approve MODALITY_CHANGE request
    private void validateAndApproveModalityChange(TeacherRequest request, TeacherRequestApproveDTO approveDTO) {
        // MODALITY_CHANGE: newResourceId bắt buộc
        if (approveDTO.getNewResourceId() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Resource ID is required for MODALITY_CHANGE requests");
        }

        // Kiểm tra resource tồn tại
        Resource newResource = resourceRepository.findById(approveDTO.getNewResourceId())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT, "Resource not found"));

        // Cập nhật resource
        request.setNewResource(newResource);

    }

    //Validate và approve RESCHEDULE request
    private void validateAndApproveReschedule(TeacherRequest request, TeacherRequestApproveDTO approveDTO) {
        // RESCHEDULE: newResourceId bắt buộc
        if (approveDTO.getNewResourceId() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Resource ID is required for RESCHEDULE requests");
        }

        // Xác định newDate và newTimeSlotId
        // Priority: approveDTO > request (nếu teacher đã điền)
        LocalDate newDate = approveDTO.getNewDate() != null ? approveDTO.getNewDate() : request.getNewDate();
        Long newTimeSlotId = approveDTO.getNewTimeSlotId() != null ? approveDTO.getNewTimeSlotId() :
                (request.getNewTimeSlot() != null ? request.getNewTimeSlot().getId() : null);

        // Validate: nếu teacher chưa điền newDate hoặc newTimeSlotId, staff phải điền
        if (newDate == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "New date is required for RESCHEDULE requests");
        }
        if (newTimeSlotId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "New time slot ID is required for RESCHEDULE requests");
        }

        // Validate newDate không được trong quá khứ
        if (newDate.isBefore(LocalDate.now())) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "New date cannot be in the past");
        }

        // Kiểm tra resource tồn tại
        Resource newResource = resourceRepository.findById(approveDTO.getNewResourceId())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT, "Resource not found"));

        // Kiểm tra time slot tồn tại
        TimeSlotTemplate newTimeSlot = timeSlotTemplateRepository.findById(newTimeSlotId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT, "Time slot not found"));

        // Cập nhật thông tin
        request.setNewDate(newDate);
        request.setNewTimeSlot(newTimeSlot);
        request.setNewResource(newResource);

    }

    //Từ chối yêu cầu giáo viên
    @Transactional
    public TeacherRequestResponseDTO rejectRequest(Long requestId, String reason, Long academicStaffUserId) {
        log.info("Rejecting request {} by academic staff {}", requestId, academicStaffUserId);

        // Lấy request
        TeacherRequest request = teacherRequestRepository.findByIdWithTeacherAndSession(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_REQUEST_NOT_FOUND, "Request not found"));

        // Kiểm tra status phải là PENDING
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Request is not in PENDING status");
        }

        // Kiểm tra quyền: academic staff phải có branch trùng với teacher
        List<Long> academicBranchIds = getBranchIdsForUser(academicStaffUserId);
        if (academicBranchIds.isEmpty()) {
            throw new CustomException(ErrorCode.FORBIDDEN, "Academic staff has no branches assigned");
        }
        Long teacherUserAccountId = request.getTeacher().getUserAccount().getId();
        List<Long> teacherBranchIds = getBranchIdsForUser(teacherUserAccountId);
        boolean hasAccess = teacherBranchIds.stream().anyMatch(academicBranchIds::contains);
        if (!hasAccess) {
            throw new CustomException(ErrorCode.FORBIDDEN, "You don't have permission to reject this request");
        }

        // Validate lý do từ chối
        if (reason == null || reason.trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Rejection reason is required");
        }
        if (reason.trim().length() < 10) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Rejection reason must be at least 10 characters");
        }

        // Lấy user account của giáo vụ
        UserAccount academicStaffAccount = userAccountRepository.findById(academicStaffUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "Academic staff not found"));

        // Cập nhật status và thông tin quyết định
        request.setStatus(RequestStatus.REJECTED);
        request.setDecidedBy(academicStaffAccount);
        request.setDecidedAt(OffsetDateTime.now());
        request.setNote(reason.trim());

        request = teacherRequestRepository.save(request);
        log.info("Request {} rejected successfully by academic staff {}", requestId, academicStaffUserId);

        return mapToResponseDTO(request);
    }
}

