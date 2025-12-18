package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.teacherrequest.MySessionDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestApproveDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestCreateDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestListDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestResponseDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherListDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.ModalityResourceSuggestionDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.ReplacementCandidateDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.ReplacementCandidateSkillDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestConfigDTO;
import org.fyp.tmssep490be.dtos.schedule.TimeSlotDTO;
import org.fyp.tmssep490be.entities.Resource;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.SessionResource;
import org.fyp.tmssep490be.entities.Student;
import org.fyp.tmssep490be.entities.StudentSession;
import org.fyp.tmssep490be.entities.Teacher;
import org.fyp.tmssep490be.entities.TeacherRequest;
import org.fyp.tmssep490be.entities.TeachingSlot;
import org.fyp.tmssep490be.entities.TimeSlotTemplate;
import org.fyp.tmssep490be.entities.enums.TeachingSlotStatus;
import org.fyp.tmssep490be.entities.enums.ResourceType;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.enums.Modality;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.entities.enums.TeacherRequestType;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.entities.enums.NotificationType;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.ResourceRepository;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.repositories.SessionResourceRepository;
import org.fyp.tmssep490be.repositories.StudentRequestRepository;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.fyp.tmssep490be.repositories.TeacherRepository;
import org.fyp.tmssep490be.repositories.TeacherRequestRepository;
import org.fyp.tmssep490be.repositories.TeachingSlotRepository;
import org.fyp.tmssep490be.repositories.TimeSlotTemplateRepository;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.fyp.tmssep490be.repositories.UserBranchesRepository;
import org.fyp.tmssep490be.entities.enums.StudentRequestType;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.services.EmailService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherRequestService {

    private final TeacherRequestRepository teacherRequestRepository;
    private final TeacherRepository teacherRepository;
    private final SessionRepository sessionRepository;
    private final SessionResourceRepository sessionResourceRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final StudentRequestRepository studentRequestRepository;
    private final TeachingSlotRepository teachingSlotRepository;
    private final UserBranchesRepository userBranchesRepository;
    private final UserAccountRepository userAccountRepository;
    private final ResourceRepository resourceRepository;
    private final TimeSlotTemplateRepository timeSlotTemplateRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    private static final int MIN_REASON_LENGTH = 10;
    private static final boolean REQUIRE_RESOURCE_FOR_MODALITY_CHANGE = true;
    private static final boolean REQUIRE_RESOURCE_FOR_RESCHEDULE = true;
    private static final boolean REQUIRE_REPLACEMENT_TEACHER = true;
    private static final int TIME_WINDOW_DAYS = 14;

    // Giáo viên tạo yêu cầu
    @Transactional
    public TeacherRequestResponseDTO createRequest(TeacherRequestCreateDTO createDTO, Long userId) {
        log.info("Creating teacher request type {} for user {}", createDTO.getRequestType(), userId);

        Teacher teacher = teacherRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND, "Teacher profile not found for current user"));

        // Validate lý do tối thiểu
        String reason = createDTO.getReason() != null ? createDTO.getReason().trim() : "";
        if (reason.length() < MIN_REASON_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Reason must be at least " + MIN_REASON_LENGTH + " characters");
        }

        // Lấy session và kiểm tra quyền sở hữu
        Session session = sessionRepository.findById(createDTO.getSessionId())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT, "Session not found"));

        boolean isOwner = session.getTeachingSlots().stream()
                .anyMatch(slot -> slot.getTeacher() != null && slot.getTeacher().getId().equals(teacher.getId()));
        if (!isOwner) {
            throw new CustomException(ErrorCode.FORBIDDEN, "You are not assigned to this session");
        }

        // Chỉ cho phép tạo yêu cầu cho buổi PLANNED và còn đủ thời gian
        if (session.getStatus() != SessionStatus.PLANNED) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Session is not in PLANNED status");
        }
        LocalDate today = LocalDate.now();
        // Không cho phép chọn buổi trong quá khứ
        if (session.getDate().isBefore(today)) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Session date is in the past");
        }

        // Giới hạn tối đa 3 yêu cầu cho cùng một lớp (tính các trạng thái PENDING, WAITING_CONFIRM, APPROVED)
        if (session.getClassEntity() == null || session.getClassEntity().getId() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Session class is missing");
        }
        List<RequestStatus> countedStatuses = Arrays.asList(
                RequestStatus.PENDING,
                RequestStatus.WAITING_CONFIRM,
                RequestStatus.APPROVED
        );
        long activeRequestsForClass = teacherRequestRepository.countActiveRequestsByTeacherAndClass(
                teacher.getId(),
                session.getClassEntity().getId(),
                countedStatuses
        );
        if (activeRequestsForClass >= 3) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Bạn đã gửi 3 yêu cầu cho lớp này");
        }

        TeacherRequestType requestType = createDTO.getRequestType();
        TeacherRequest request = new TeacherRequest();
        request.setTeacher(teacher);
        request.setSession(session);
        request.setRequestType(requestType);
        request.setStatus(RequestStatus.PENDING);
        request.setSubmittedAt(OffsetDateTime.now());
        request.setSubmittedBy(teacher.getUserAccount());
        request.setRequestReason(reason);

        // Xử lý theo từng loại yêu cầu
        switch (requestType) {
            case MODALITY_CHANGE:
                handleModalityChangeRequest(request, createDTO);
                break;
            case RESCHEDULE:
                handleRescheduleRequest(request, createDTO);
                break;
            case REPLACEMENT:
                handleReplacementRequest(request, createDTO, teacher);
                break;
            default:
                throw new CustomException(ErrorCode.INVALID_INPUT, "Unsupported request type: " + requestType);
        }

        request = teacherRequestRepository.save(request);
        log.info("Created teacher request {} type {} for user {}", request.getId(), requestType, userId);

        // Gửi thông báo + email
        try {
            teacherRequestRepository.flush();
            // 1) Thông báo cho giáo vụ
            sendNotificationToAcademicStaffForNewRequest(request);
            // 2) Email cho giáo viên về việc đã tạo request
            sendEmailNotificationForCreatedRequest(request);
        } catch (Exception e) {
            log.error("Lỗi khi gửi notification/email về request {}: {}", request.getId(), e.getMessage(), e);
        }

        return mapToResponseDTO(request);
    }

    // Xử lý MODALITY_CHANGE request khi teacher tạo
    private void handleModalityChangeRequest(TeacherRequest request, TeacherRequestCreateDTO createDTO) {
        // MODALITY_CHANGE: yêu cầu resource (phòng) bắt buộc
        Long newResourceId = createDTO.getNewResourceId();
        if (REQUIRE_RESOURCE_FOR_MODALITY_CHANGE && newResourceId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Resource is required for MODALITY_CHANGE requests");
        }
        if (newResourceId != null) {
            Resource resource = resourceRepository.findById(newResourceId)
                    .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT, "Resource not found"));
            request.setNewResource(resource);
        }
    }

    // Xử lý RESCHEDULE request khi teacher tạo
    private void handleRescheduleRequest(TeacherRequest request, TeacherRequestCreateDTO createDTO) {
        // RESCHEDULE: newDate, newTimeSlotId và newResourceId là bắt buộc
        LocalDate newDate = createDTO.getNewDate();
        Long newTimeSlotId = createDTO.getNewTimeSlotId();
        Long newResourceId = createDTO.getNewResourceId();

        // Validate newDate bắt buộc
        if (newDate == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "New date is required for RESCHEDULE requests");
        }
        // Validate newDate không được trong quá khứ
        if (newDate.isBefore(LocalDate.now())) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "New date cannot be in the past");
        }
        request.setNewDate(newDate);

        // Validate newTimeSlotId bắt buộc
        if (newTimeSlotId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "New time slot ID is required for RESCHEDULE requests");
        }
        TimeSlotTemplate timeSlot = timeSlotTemplateRepository.findById(newTimeSlotId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT, "Time slot not found"));
        request.setNewTimeSlot(timeSlot);

        // Validate newResourceId bắt buộc nếu REQUIRE_RESOURCE_FOR_RESCHEDULE = true
        if (REQUIRE_RESOURCE_FOR_RESCHEDULE && newResourceId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "New resource ID is required for RESCHEDULE requests");
        }
        if (newResourceId != null) {
            Resource resource = resourceRepository.findById(newResourceId)
                    .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT, "Resource not found"));
            request.setNewResource(resource);
        }
    }

    // Xử lý REPLACEMENT request khi teacher tạo
    private void handleReplacementRequest(TeacherRequest request, TeacherRequestCreateDTO createDTO, Teacher requestingTeacher) {
        // REPLACEMENT: replacementTeacherId là bắt buộc nếu REQUIRE_REPLACEMENT_TEACHER = true
        Long replacementTeacherId = createDTO.getReplacementTeacherId();
        
        if (REQUIRE_REPLACEMENT_TEACHER && replacementTeacherId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Replacement teacher ID is required for REPLACEMENT requests");
        }

        // Kiểm tra replacement teacher tồn tại
        Teacher replacementTeacher = teacherRepository.findById(replacementTeacherId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND, "Replacement teacher not found"));

        // Không được chọn chính giáo viên tạo request
        if (replacementTeacher.getId().equals(requestingTeacher.getId())) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Replacement teacher cannot be the same as the requesting teacher");
        }

        // Set replacement teacher
        request.setReplacementTeacher(replacementTeacher);
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

        // Validate session status: chỉ gợi ý resource cho session PLANNED
        if (session.getStatus() != SessionStatus.PLANNED) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Session is not in PLANNED status");
        }

        // Validate session có đầy đủ thông tin cần thiết để query resource
        // Cần branch để filter resource theo chi nhánh (resource phải cùng branch với class)
        if (session.getClassEntity() == null || session.getClassEntity().getBranch() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Session branch is missing");
        }
        // Cần timeSlot để check resource availability tại khung giờ đó
        if (session.getTimeSlotTemplate() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Session time slot is missing");
        }

        Long branchId = session.getClassEntity().getBranch().getId();
        Long timeSlotId = session.getTimeSlotTemplate().getId();
        LocalDate sessionDate = session.getDate();

        return getModalityResourcesInternal(session, branchId, sessionDate, timeSlotId, true);
    }

    // Internal method để lấy resources với filter modality và capacity
    // isModalityChange: true = MODALITY_CHANGE (đổi modality), false = RESCHEDULE (giữ nguyên modality)
    private List<ModalityResourceSuggestionDTO> getModalityResourcesInternal(Session session, Long branchId, LocalDate sessionDate, Long timeSlotId, boolean isModalityChange) {
        Long sessionId = session.getId();

        // Resource hiện tại của session (nếu có)
        Resource currentResource = session.getSessionResources().stream()
                .findFirst()
                .map(sr -> sr.getResource())
                .filter(r -> r != null)
                .orElse(null);
        Long currentResourceId = currentResource != null ? currentResource.getId() : null;

        List<Resource> allResources = resourceRepository.findAvailableResourcesForSession(
                branchId, sessionDate, timeSlotId, sessionId);

        // Filter resources theo modality
        ResourceType requiredResourceType = null;
        if (isModalityChange) {
            // MODALITY_CHANGE: đổi modality của session
            // Dựa vào resource hiện tại của session, không phải classModality
            if (currentResource != null && currentResource.getResourceType() != null) {
                // Nếu session hiện tại có ROOM (OFFLINE) -> gợi ý VIRTUAL (ONLINE)
                // Nếu session hiện tại có VIRTUAL (ONLINE) -> gợi ý ROOM (OFFLINE)
                requiredResourceType = (currentResource.getResourceType() == ResourceType.ROOM) 
                        ? ResourceType.VIRTUAL 
                        : ResourceType.ROOM;
            } else {
                // Nếu session chưa có resource, dùng classModality làm mặc định
                Modality classModality = session.getClassEntity().getModality();
                if (classModality != null) {
                    // Class OFFLINE -> gợi ý VIRTUAL (đổi sang ONLINE)
                    // Class ONLINE -> gợi ý ROOM (đổi sang OFFLINE)
                    requiredResourceType = (classModality == Modality.OFFLINE) 
                            ? ResourceType.VIRTUAL 
                            : ResourceType.ROOM;
                }
            }
        } else {
            // RESCHEDULE: giữ nguyên modality -> lấy từ session resource, không phải class
            Modality sessionModality = getSessionModality(session);
            if (sessionModality != null) {
                // Giữ nguyên modality của session
                requiredResourceType = (sessionModality == Modality.OFFLINE) 
                        ? ResourceType.ROOM 
                        : ResourceType.VIRTUAL;
            } else {
                // Fallback về class modality nếu session chưa có resource
                Modality classModality = session.getClassEntity().getModality();
                if (classModality != null) {
                    requiredResourceType = (classModality == Modality.OFFLINE) 
                            ? ResourceType.ROOM 
                            : ResourceType.VIRTUAL;
                }
            }
        }
        
        final ResourceType finalRequiredResourceType = requiredResourceType;
        List<Resource> resources = allResources.stream()
                .filter(r -> finalRequiredResourceType == null || r.getResourceType() == finalRequiredResourceType)
                .collect(Collectors.toList());

        // Lấy số lượng học viên của lớp
        Integer studentCount = session.getClassEntity() != null 
                ? session.getClassEntity().getMaxCapacity() 
                : null;
        
        // Nếu không có maxCapacity, đếm số học viên đã đăng ký
        if (studentCount == null) {
            studentCount = (int) session.getStudentSessions().stream()
                    .filter(ss -> ss.getStudent() != null)
                    .count();
        }

        // Filter resources theo sức chứa: chỉ lấy các phòng có capacity >= số học viên
        final Integer finalStudentCount = studentCount;
        List<Resource> filteredResources = resources.stream()
                .filter(r -> {
                    // Nếu không có thông tin capacity hoặc studentCount, vẫn hiển thị
                    if (r.getCapacity() == null || finalStudentCount == null) {
                        return true;
                    }
                    // Chỉ lấy phòng có sức chứa đủ
                    return r.getCapacity() >= finalStudentCount;
                })
                .collect(Collectors.toList());

        // Nếu buổi đang gán resource hiện tại, đảm bảo vẫn xuất hiện trong list
        // (nhưng chỉ nếu resource đó phù hợp với modality và capacity)
        if (currentResourceId != null) {
            boolean containsCurrent = filteredResources.stream().anyMatch(r -> r.getId().equals(currentResourceId));
            if (!containsCurrent) {
                resourceRepository.findById(currentResourceId).ifPresent(resource -> {
                    // Chỉ thêm nếu resource phù hợp với modality và capacity
                    boolean modalityMatch = finalRequiredResourceType == null || 
                            resource.getResourceType() == finalRequiredResourceType;
                    boolean capacityMatch = resource.getCapacity() == null || finalStudentCount == null ||
                            resource.getCapacity() >= finalStudentCount;
                    if (modalityMatch && capacityMatch) {
                        filteredResources.add(resource);
                    }
                });
            }
        }

        // Sắp xếp theo: current resource trước, sau đó theo độ chênh lệch capacity (gần nhất trước)
        final Integer finalStudentCountForSort = studentCount;
        return filteredResources.stream()
                .map(r -> ModalityResourceSuggestionDTO.builder()
                        .resourceId(r.getId())
                        .name(r.getName())
                        .resourceType(r.getResourceType() != null ? r.getResourceType().name() : null)
                        .capacity(r.getCapacity())
                        .branchId(r.getBranch() != null ? r.getBranch().getId() : null)
                        .currentResource(currentResourceId != null && currentResourceId.equals(r.getId()))
                        .build())
                .sorted((a, b) -> {
                    // Current resource lên đầu
                    if (a.isCurrentResource() && !b.isCurrentResource()) return -1;
                    if (!a.isCurrentResource() && b.isCurrentResource()) return 1;
                    
                    // Sắp xếp theo độ chênh lệch capacity với số học viên (gần nhất trước)
                    if (finalStudentCountForSort != null && a.getCapacity() != null && b.getCapacity() != null) {
                        int diffA = Math.abs(a.getCapacity() - finalStudentCountForSort);
                        int diffB = Math.abs(b.getCapacity() - finalStudentCountForSort);
                        if (diffA != diffB) {
                            return Integer.compare(diffA, diffB); // Nhỏ hơn = gần hơn
                        }
                    }
                    
                    // Nếu cùng độ chênh lệch hoặc không có thông tin, sắp xếp theo tên
                    if (a.getName() == null) return 1;
                    if (b.getName() == null) return -1;
                    return a.getName().compareToIgnoreCase(b.getName());
                })
                .collect(Collectors.toList());
    }

    // Gợi ý resource cho giáo vụ dựa trên session ID và teacher ID (MODALITY_CHANGE) - khi tạo request mới
    @Transactional(readOnly = true)
    public List<ModalityResourceSuggestionDTO> suggestModalityResourcesForStaffBySession(
            Long sessionId, Long teacherId, Long academicStaffUserId) {
        log.info("Suggest modality resources for session {} and teacher {} by academic staff {}", 
                sessionId, teacherId, academicStaffUserId);

        // Kiểm tra quyền: academic staff phải có branch trùng với teacher
        List<Long> academicBranchIds = getBranchIdsForUser(academicStaffUserId);
        if (academicBranchIds.isEmpty()) {
            throw new CustomException(ErrorCode.FORBIDDEN, "Academic staff has no branches assigned");
        }

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND, "Teacher not found"));

        if (teacher.getUserAccount() == null) {
            throw new CustomException(ErrorCode.TEACHER_NOT_FOUND, "Teacher has no user account");
        }

        Long teacherUserAccountId = teacher.getUserAccount().getId();
        List<Long> teacherBranchIds = getBranchIdsForUser(teacherUserAccountId);
        boolean hasAccess = teacherBranchIds.stream().anyMatch(academicBranchIds::contains);
        if (!hasAccess) {
            throw new CustomException(ErrorCode.FORBIDDEN, "You don't have permission to view resources for this teacher");
        }

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT, "Session not found"));

        // Kiểm tra teacher có được assign vào session không
        boolean isOwner = session.getTeachingSlots().stream()
                .anyMatch(slot -> slot.getTeacher() != null && slot.getTeacher().getId().equals(teacher.getId()));
        if (!isOwner) {
            throw new CustomException(ErrorCode.FORBIDDEN, "Teacher is not assigned to this session");
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

        return getModalityResourcesInternal(session, branchId, sessionDate, timeSlotId, true);
    }

    // Gợi ý resource cho giáo vụ dựa trên request ID (đổi phương thức)
    @Transactional(readOnly = true)
    public List<ModalityResourceSuggestionDTO> suggestModalityResourcesForStaff(Long requestId, Long academicStaffUserId) {
        log.info("Suggest modality resources for staff {} on request {}", academicStaffUserId, requestId);

        TeacherRequest request = teacherRequestRepository.findByIdWithTeacherAndSession(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_REQUEST_NOT_FOUND, "Request not found"));

        // Kiểm tra quyền branch
        Long teacherUserAccountId = request.getTeacher() != null ? request.getTeacher().getUserAccount().getId() : null;
        List<Long> academicBranches = getBranchIdsForUser(academicStaffUserId);
        List<Long> teacherBranches = getBranchIdsForUser(teacherUserAccountId);
        boolean allowed = teacherBranches.isEmpty()
                || academicBranches.isEmpty()
                || academicBranches.stream().anyMatch(teacherBranches::contains);
        if (!allowed) {
            throw new CustomException(ErrorCode.FORBIDDEN, "You don't have permission to view resources for this request");
        }

        Session session = request.getSession();
        if (session == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Request has no session");
        }
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

        return getModalityResourcesInternal(session, branchId, sessionDate, timeSlotId, true);
    }

    // Lấy các time slots khả dụng cho RESCHEDULE (cho teacher)
    @Transactional(readOnly = true)
    public List<TimeSlotDTO> getRescheduleSlots(Long sessionId, String dateStr, Long userId) {
        log.info("Get reschedule slots for session {} on date {} by user {}", sessionId, dateStr, userId);

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

        return getRescheduleSlotsInternal(session, dateStr, teacher.getId());
    }

    // Lấy các time slots khả dụng cho RESCHEDULE (cho academic staff - từ sessionId)
    @Transactional(readOnly = true)
    public List<TimeSlotDTO> getRescheduleSlotsForStaff(Long sessionId, String dateStr, Long teacherId) {
        log.info("Get reschedule slots for session {} on date {} for teacher {} by staff", sessionId, dateStr, teacherId);

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT, "Session not found"));

        // Kiểm tra teacher tồn tại
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND, "Teacher not found"));

        // Kiểm tra teacher có phải là người dạy buổi này không
        boolean isOwner = session.getTeachingSlots().stream()
                .anyMatch(slot -> slot.getTeacher() != null && slot.getTeacher().getId().equals(teacher.getId()));
        if (!isOwner) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Teacher is not assigned to this session");
        }

        return getRescheduleSlotsInternal(session, dateStr, teacherId);
    }

    // Lấy các time slots khả dụng cho RESCHEDULE (cho academic staff - từ requestId)
    @Transactional(readOnly = true)
    public List<TimeSlotDTO> getRescheduleSlotsForStaffFromRequest(Long requestId) {
        log.info("Get reschedule slots from request {} by staff", requestId);

        TeacherRequest request = teacherRequestRepository.findById(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_REQUEST_NOT_FOUND, "Request not found"));

        if (request.getRequestType() != TeacherRequestType.RESCHEDULE) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Request is not a RESCHEDULE request");
        }

        if (request.getSession() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Request has no associated session");
        }

        if (request.getNewDate() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Request has no newDate set");
        }

        Session session = request.getSession();
        String dateStr = request.getNewDate().toString();
        Long teacherId = request.getTeacher().getId();

        return getRescheduleSlotsInternal(session, dateStr, teacherId);
    }

    // Internal method để lấy slots (shared logic)
    private List<TimeSlotDTO> getRescheduleSlotsInternal(Session session, String dateStr, Long teacherId) {
        Long sessionId = session.getId();

        // Chỉ gợi ý cho session PLANNED
        if (session.getStatus() != SessionStatus.PLANNED) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Session is not in PLANNED status");
        }

        LocalDate targetDate;
        try {
            targetDate = LocalDate.parse(dateStr);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Invalid date format");
        }

        // Lấy tất cả time slots từ branch của session
        if (session.getClassEntity() == null || session.getClassEntity().getBranch() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Session branch is missing");
        }

        Long branchId = session.getClassEntity().getBranch().getId();
        List<TimeSlotTemplate> availableSlots = timeSlotTemplateRepository.findByBranchIdOrderByStartTimeAsc(branchId);

        // Tính thời lượng của time slot hiện tại (duration)
        TimeSlotTemplate currentTimeSlot = session.getTimeSlotTemplate();
        if (currentTimeSlot == null || currentTimeSlot.getStartTime() == null || currentTimeSlot.getEndTime() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Session time slot is missing or invalid");
        }
        
        // Tính duration hiện tại (minutes)
        long currentDurationMinutes = java.time.Duration.between(
                currentTimeSlot.getStartTime(),
                currentTimeSlot.getEndTime()
        ).toMinutes();

        // Lấy sessions của teacher trong ngày target (trừ session hiện tại)
        List<Session> teacherSessions = sessionRepository.findSessionsForTeacherByDate(
                teacherId, targetDate, sessionId);

        // Lấy TẤT CẢ students có mặt trong session
        // Lấy từ studentSessionson
        List<Long> studentIds = session.getStudentSessions().stream()
                .filter(ss -> ss.getStudent() != null)
                .map(ss -> ss.getStudent().getId())
                .distinct()
                .collect(Collectors.toList());

        // Kiểm tra conflict cho từng time slot
        return availableSlots.stream()
                .filter(slot -> {
                    // Chỉ gợi ý time slots có cùng thời lượng
                    if (slot.getStartTime() == null || slot.getEndTime() == null) {
                        return false;
                    }
                    long slotDurationMinutes = java.time.Duration.between(
                            slot.getStartTime(),
                            slot.getEndTime()
                    ).toMinutes();
                    if (slotDurationMinutes != currentDurationMinutes) {
                        return false; // Filter out slots with different duration
                    }

                    // Kiểm tra conflict với teacher
                    boolean hasTeacherConflict = teacherSessions.stream()
                            .anyMatch(ts -> hasTimeOverlap(ts.getTimeSlotTemplate(), slot));

                    if (hasTeacherConflict) {
                        return false; // Filter out slots with teacher conflict
                    }

                    // Kiểm tra conflict với students
                    for (Long studentId : studentIds) {
                        List<Session> studentSessions = sessionRepository.findSessionsForStudentByDate(
                                studentId, targetDate);
                        boolean hasStudentConflict = studentSessions.stream()
                                .anyMatch(ss -> ss.getId().equals(sessionId) || // Exclude current session
                                        hasTimeOverlap(ss.getTimeSlotTemplate(), slot));
                        if (hasStudentConflict) {
                            return false; // Filter out slots with student conflict
                        }
                    }

                    return true; // Slot is available
                })
                .map(slot -> TimeSlotDTO.builder()
                        .timeSlotTemplateId(slot.getId())
                        .name(slot.getName())
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .build())
                .collect(Collectors.toList());
    }

    // Helper method to check time overlap between two time slots
    private boolean hasTimeOverlap(TimeSlotTemplate slot1, TimeSlotTemplate slot2) {
        if (slot1 == null || slot2 == null) return false;
        if (slot1.getStartTime() == null || slot1.getEndTime() == null ||
            slot2.getStartTime() == null || slot2.getEndTime() == null) {
            return false;
        }
        return !(slot1.getEndTime().isBefore(slot2.getStartTime()) ||
                 slot2.getEndTime().isBefore(slot1.getStartTime()));
    }

    // Kiểm tra xem teacher được gợi ý có conflict về thời gian với session không
    private boolean hasTeacherTimeConflict(Long teacherId, LocalDate sessionDate, TimeSlotTemplate sessionTimeSlot, Long excludeSessionId) {
        if (sessionDate == null || sessionTimeSlot == null) {
            return false; // Không có thông tin để kiểm tra
        }

        // Lấy các sessions của teacher trong ngày đó (trừ session hiện tại)
        List<Session> teacherSessions = sessionRepository.findSessionsForTeacherByDate(
                teacherId, sessionDate, excludeSessionId);

        // Kiểm tra xem có session nào overlap về thời gian không
        return teacherSessions.stream()
                .anyMatch(ts -> {
                    TimeSlotTemplate tsTimeSlot = ts.getTimeSlotTemplate();
                    return tsTimeSlot != null && hasTimeOverlap(tsTimeSlot, sessionTimeSlot);
                });
    }

    // Lấy các resources khả dụng cho RESCHEDULE (cho teacher)
    @Transactional(readOnly = true)
    public List<ModalityResourceSuggestionDTO> getRescheduleResources(Long sessionId, String dateStr, Long timeSlotId, Long userId) {
        log.info("Get reschedule resources for session {} on date {} with timeSlot {} by user {}", sessionId, dateStr, timeSlotId, userId);

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

        return getRescheduleResourcesInternal(session, dateStr, timeSlotId);
    }

    // Lấy các resources khả dụng cho RESCHEDULE (cho academic staff - từ sessionId)
    @Transactional(readOnly = true)
    public List<ModalityResourceSuggestionDTO> getRescheduleResourcesForStaff(Long sessionId, String dateStr, Long timeSlotId, Long teacherId) {
        log.info("Get reschedule resources for session {} on date {} with timeSlot {} for teacher {} by staff", sessionId, dateStr, timeSlotId, teacherId);

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT, "Session not found"));

        // Kiểm tra teacher tồn tại
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND, "Teacher not found"));

        // Kiểm tra teacher có phải là người dạy buổi này không
        boolean isOwner = session.getTeachingSlots().stream()
                .anyMatch(slot -> slot.getTeacher() != null && slot.getTeacher().getId().equals(teacher.getId()));
        if (!isOwner) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Teacher is not assigned to this session");
        }

        return getRescheduleResourcesInternal(session, dateStr, timeSlotId);
    }

    // Lấy các resources khả dụng cho RESCHEDULE (cho academic staff - từ requestId)
    @Transactional(readOnly = true)
    public List<ModalityResourceSuggestionDTO> getRescheduleResourcesForStaffFromRequest(Long requestId) {
        log.info("Get reschedule resources from request {} by staff", requestId);

        TeacherRequest request = teacherRequestRepository.findById(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_REQUEST_NOT_FOUND, "Request not found"));

        if (request.getRequestType() != TeacherRequestType.RESCHEDULE) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Request is not a RESCHEDULE request");
        }

        if (request.getSession() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Request has no associated session");
        }

        if (request.getNewDate() == null || request.getNewTimeSlot() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Request has no newDate or newTimeSlot set");
        }

        Session session = request.getSession();
        String dateStr = request.getNewDate().toString();
        Long timeSlotId = request.getNewTimeSlot().getId();

        return getRescheduleResourcesInternal(session, dateStr, timeSlotId);
    }

    // Internal method để lấy resources (shared logic)
    private List<ModalityResourceSuggestionDTO> getRescheduleResourcesInternal(Session session, String dateStr, Long timeSlotId) {
        // Chỉ gợi ý cho session PLANNED
        if (session.getStatus() != SessionStatus.PLANNED) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Session is not in PLANNED status");
        }

        LocalDate targetDate;
        try {
            targetDate = LocalDate.parse(dateStr);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Invalid date format");
        }

        if (session.getClassEntity() == null || session.getClassEntity().getBranch() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Session branch is missing");
        }

        Long branchId = session.getClassEntity().getBranch().getId();
        
        // RESCHEDULE: giữ nguyên modality -> isModalityChange = false
        return getModalityResourcesInternal(session, branchId, targetDate, timeSlotId, false);
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

        // Nếu chưa có decidedBy, fallback sang submittedBy để hiển thị người tạo (đặc biệt với WAITING_CONFIRM)
        UserAccount decidedBy = request.getDecidedBy();
        UserAccount submittedBy = request.getSubmittedBy();
        UserAccount handler = decidedBy != null ? decidedBy : submittedBy;
        OffsetDateTime decidedAt = request.getDecidedAt() != null ? request.getDecidedAt() : request.getSubmittedAt();

        // Xử lý modality cho MODALITY_CHANGE và RESCHEDULE
        Modality currentModality = null;
        Modality newModality = null;
        if (request.getRequestType() == TeacherRequestType.MODALITY_CHANGE || 
            request.getRequestType() == TeacherRequestType.RESCHEDULE) {
            // MODALITY_CHANGE và RESCHEDULE: lấy modality từ session (resource), không phải từ class
            if (session != null) {
                currentModality = getSessionModality(session);
                // Nếu session chưa có resource, fallback về class modality
                if (currentModality == null && classEntity != null) {
                    currentModality = classEntity.getModality();
                }
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
                .decidedAt(decidedAt)
                .decidedById(handler != null ? handler.getId() : null)
                .decidedByName(handler != null ? handler.getFullName() : null)
                .decidedByEmail(handler != null ? handler.getEmail() : null)
                .currentModality(currentModality)
                .newModality(newModality)
                .build();
    }

    //Lấy danh sách yêu cầu giáo viên cho Academic Staff với optional status filter
    //Chỉ trả về requests của giáo viên trong cùng branch với academic staff
    @Transactional(readOnly = true)
    public List<TeacherRequestListDTO> getRequestsForStaff(RequestStatus status, Long academicStaffUserId, Long branchId) {
        log.info("Getting teacher requests for staff {} with status {} and branch {}", academicStaffUserId, status, branchId);
        
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
                    boolean sameBranch = teacherBranchIds.stream().anyMatch(academicBranchIds::contains);
                    if (!sameBranch) {
                        return false;
                    }
                    // Nếu client truyền branchId (chi nhánh đang chọn) thì request phải thuộc chi nhánh đó
                    if (branchId != null) {
                        return teacherBranchIds.contains(branchId) && academicBranchIds.contains(branchId);
                    }
                    return true;
                })
                .collect(Collectors.toList());
        
        // Map sang DTO
        return filteredRequests.stream()
                .map(this::mapToListDTO)
                .collect(Collectors.toList());
    }

    // Helper method để lấy branch IDs của user
    
    // Lấy modality của session dựa vào resource được gán
    // ROOM -> OFFLINE, VIRTUAL -> ONLINE
    // Nếu session chưa có resource, trả về null
    private Modality getSessionModality(Session session) {
        if (session == null || session.getSessionResources() == null || session.getSessionResources().isEmpty()) {
            return null;
        }
        
        // Lấy resource đầu tiên của session
        SessionResource sessionResource = session.getSessionResources().iterator().next();
        if (sessionResource == null || sessionResource.getResource() == null) {
            return null;
        }
        
        ResourceType resourceType = sessionResource.getResource().getResourceType();
        if (resourceType == null) {
            return null;
        }
        
        // Chuyển đổi ResourceType -> Modality
        return (resourceType == ResourceType.ROOM) ? Modality.OFFLINE : Modality.ONLINE;
    }

    private List<Long> getBranchIdsForUser(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return userBranchesRepository.findBranchIdsByUserId(userId);
    }

    //Lấy danh sách teachers cho academic staff (filter theo branch)
    @Transactional(readOnly = true)
    public List<TeacherListDTO> getTeachersForStaff(Long academicStaffUserId, Long branchId) {
        log.info("Getting teachers list for academic staff {} with branch {}", academicStaffUserId, branchId);

        // Lấy branch IDs của academic staff
        List<Long> academicBranchIds = getBranchIdsForUser(academicStaffUserId);
        
        if (academicBranchIds.isEmpty()) {
            log.warn("Academic staff {} has no branches assigned", academicStaffUserId);
            return List.of();
        }

        // Lấy tất cả teachers
        List<Teacher> allTeachers = teacherRepository.findAll();

        // Filter teachers: chỉ lấy teachers trong cùng branch với academic staff (và branch đang chọn nếu có)
        return allTeachers.stream()
                .filter(teacher -> {
                    if (teacher.getUserAccount() == null) {
                        return false;
                    }
                    Long teacherUserAccountId = teacher.getUserAccount().getId();
                    List<Long> teacherBranchIds = getBranchIdsForUser(teacherUserAccountId);
                    boolean sameBranch = teacherBranchIds.stream().anyMatch(academicBranchIds::contains);
                    if (!sameBranch) {
                        return false;
                    }
                    // Nếu có branchId đang chọn, teacher phải thuộc branch đó và academic cũng phải có branch đó
                    if (branchId != null) {
                        return teacherBranchIds.contains(branchId) && academicBranchIds.contains(branchId);
                    }
                    return true;
                })
                .map(teacher -> {
                    UserAccount userAccount = teacher.getUserAccount();
                    return TeacherListDTO.builder()
                            .teacherId(teacher.getId())
                            .fullName(userAccount != null ? userAccount.getFullName() : null)
                            .email(userAccount != null ? userAccount.getEmail() : null)
                            .employeeCode(teacher.getEmployeeCode())
                            .build();
                })
                .filter(dto -> dto.getFullName() != null && dto.getEmail() != null) // Chỉ lấy teachers có đầy đủ thông tin
                .sorted((a, b) -> {
                    // Sắp xếp theo tên
                    String nameA = a.getFullName() != null ? a.getFullName() : "";
                    String nameB = b.getFullName() != null ? b.getFullName() : "";
                    return nameA.compareToIgnoreCase(nameB);
                })
                .collect(Collectors.toList());
    }

    //Lấy danh sách sessions của teacher cho academic staff (filter theo branch)
    @Transactional(readOnly = true)
    public List<MySessionDTO> getSessionsForTeacherByStaff(Long teacherId, Integer days, Long classId, Long academicStaffUserId, Long branchId) {
        log.info("Getting sessions for teacher {} by academic staff {} with branch {}", teacherId, academicStaffUserId, branchId);

        // Kiểm tra quyền: academic staff phải có branch trùng với teacher
        List<Long> academicBranchIds = getBranchIdsForUser(academicStaffUserId);
        if (academicBranchIds.isEmpty()) {
            throw new CustomException(ErrorCode.FORBIDDEN, "Academic staff has no branches assigned");
        }

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND, "Teacher not found"));

        if (teacher.getUserAccount() == null) {
            throw new CustomException(ErrorCode.TEACHER_NOT_FOUND, "Teacher has no user account");
        }

        Long teacherUserAccountId = teacher.getUserAccount().getId();
        List<Long> teacherBranchIds = getBranchIdsForUser(teacherUserAccountId);
        boolean hasAccess = teacherBranchIds.stream().anyMatch(academicBranchIds::contains);
        if (branchId != null) {
            hasAccess = hasAccess && teacherBranchIds.contains(branchId) && academicBranchIds.contains(branchId);
        }
        if (!hasAccess) {
            throw new CustomException(ErrorCode.FORBIDDEN, "You don't have permission to view sessions for this teacher");
        }

        // Lấy sessions của teacher
        int windowDays = (days != null && days > 0) ? days : 14;
        LocalDate fromDate = LocalDate.now();
        LocalDate toDate = fromDate.plusDays(windowDays);

        List<Session> sessions = sessionRepository.findUpcomingSessionsForTeacher(
                teacher.getId(), fromDate, toDate, classId);

        // Chỉ gợi ý session chưa diễn ra
        return sessions.stream()
                .filter(this::isUpcomingSession)
                .map(session -> {
                    MySessionDTO dto = mapToMySessionDTO(session);
                    if (dto != null) {
                        boolean hasPending = teacherRequestRepository.existsBySessionIdAndStatus(
                                session.getId(), RequestStatus.PENDING);
                        dto.setHasPendingRequest(hasPending);
                    }
                    return dto;
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    //Tạo request cho teacher bởi academic staff và tự động approve
    @Transactional
    public TeacherRequestResponseDTO createRequestForTeacherByStaff(TeacherRequestCreateDTO createDTO, Long academicStaffUserId) {
        log.info("Creating teacher request type {} for teacher {} by academic staff {}", 
                createDTO.getRequestType(), createDTO.getTeacherId(), academicStaffUserId);

        // Kiểm tra quyền: academic staff phải có branch trùng với teacher
        List<Long> academicBranchIds = getBranchIdsForUser(academicStaffUserId);
        if (academicBranchIds.isEmpty()) {
            throw new CustomException(ErrorCode.FORBIDDEN, "Academic staff has no branches assigned");
        }

        // Lấy teacher
        Teacher teacher = teacherRepository.findById(createDTO.getTeacherId())
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND, "Teacher not found"));

        if (teacher.getUserAccount() == null) {
            throw new CustomException(ErrorCode.TEACHER_NOT_FOUND, "Teacher has no user account");
        }

        Long teacherUserAccountId = teacher.getUserAccount().getId();
        List<Long> teacherBranchIds = getBranchIdsForUser(teacherUserAccountId);
        boolean hasAccess = teacherBranchIds.stream().anyMatch(academicBranchIds::contains);
        if (!hasAccess) {
            throw new CustomException(ErrorCode.FORBIDDEN, "You don't have permission to create request for this teacher");
        }

        // Validate lý do tối thiểu
        String reason = createDTO.getReason() != null ? createDTO.getReason().trim() : "";
        if (reason.length() < MIN_REASON_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Reason must be at least " + MIN_REASON_LENGTH + " characters");
        }

        // Lấy session và kiểm tra quyền sở hữu
        Session session = sessionRepository.findById(createDTO.getSessionId())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT, "Session not found"));

        boolean isOwner = session.getTeachingSlots().stream()
                .anyMatch(slot -> slot.getTeacher() != null && slot.getTeacher().getId().equals(teacher.getId()));
        if (!isOwner) {
            throw new CustomException(ErrorCode.FORBIDDEN, "Teacher is not assigned to this session");
        }

        LocalDate today = LocalDate.now();
        if (session.getDate().isBefore(today)) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Session date is in the past");
        }

        // Chỉ cho phép tạo yêu cầu cho buổi PLANNED
        if (session.getStatus() != SessionStatus.PLANNED) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Session is not in PLANNED status");
        }

        TeacherRequestType requestType = createDTO.getRequestType();
        TeacherRequest request = new TeacherRequest();
        request.setTeacher(teacher);
        request.setSession(session);
        request.setRequestType(requestType);
        request.setStatus(RequestStatus.PENDING); // Set PENDING để có thể gọi approveRequest()
        request.setSubmittedAt(OffsetDateTime.now());
        
        // Set submittedBy là academic staff
        UserAccount academicStaffAccount = userAccountRepository.findById(academicStaffUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "Academic staff account not found"));
        request.setSubmittedBy(academicStaffAccount);
        
        request.setRequestReason(reason);

        // Xử lý theo từng loại yêu cầu
        switch (requestType) {
            case MODALITY_CHANGE:
                handleModalityChangeRequest(request, createDTO);
                break;
            case RESCHEDULE:
                handleRescheduleRequest(request, createDTO);
                break;
            case REPLACEMENT:
                handleReplacementRequest(request, createDTO, teacher);
                break;
            default:
                throw new CustomException(ErrorCode.INVALID_INPUT, "Unsupported request type: " + requestType);
        }

        // Với REPLACEMENT request, không tự động approve mà chờ replacement teacher confirm
        if (requestType == TeacherRequestType.REPLACEMENT) {
            // Set status = WAITING_CONFIRM để chờ replacement teacher confirm
            request.setStatus(RequestStatus.WAITING_CONFIRM);
            request = teacherRequestRepository.save(request);
            log.info("Created REPLACEMENT request {} for teacher {} by academic staff {}. Status: WAITING_CONFIRM (waiting for replacement teacher confirmation)",
                    request.getId(), teacher.getId(), academicStaffUserId);
            
            // Gửi thông báo và email cho giáo viên dạy thay
            try {
                sendNotificationToReplacementTeacher(request);
                sendEmailNotificationForReplacementInvitation(request);
            } catch (Exception e) {
                log.error("Lỗi khi gửi thông báo cho replacement teacher: {}", e.getMessage());
            }
            
            return mapToResponseDTO(request);
        }

        // Với MODALITY_CHANGE và RESCHEDULE, tự động approve và update session
        request = teacherRequestRepository.save(request);
        log.info("Created teacher request {} type {} for teacher {} by academic staff {}", 
                request.getId(), requestType, teacher.getId(), academicStaffUserId);

        // Tự động approve và update session
        TeacherRequestApproveDTO approveDTO = new TeacherRequestApproveDTO();
        approveDTO.setNewResourceId(createDTO.getNewResourceId());
        approveDTO.setNewDate(createDTO.getNewDate());
        approveDTO.setNewTimeSlotId(createDTO.getNewTimeSlotId());
        approveDTO.setReplacementTeacherId(createDTO.getReplacementTeacherId());

        // Gọi logic approve để update session (sẽ set status = APPROVED và update session)
        TeacherRequestResponseDTO response = approveRequest(request.getId(), approveDTO, academicStaffUserId);
        
        // Gửi thông báo cho teacher (sau khi approve thành công)
        try {
            // Lấy lại request đã được approve để gửi notification
            TeacherRequest approvedRequest = teacherRequestRepository.findByIdWithTeacherAndSession(request.getId())
                    .orElse(null);
            if (approvedRequest != null) {
                sendNotificationToTeacherForStaffCreatedRequest(approvedRequest);
            }
        } catch (Exception e) {
            log.error("Lỗi khi gửi notification cho teacher về staff-created request {}: {}", request.getId(), e.getMessage());
            // Không throw exception - notification failure không nên block việc tạo request
        }
        
        return response;
    }

     // Gửi thông báo cho giáo vụ khi teacher tạo request mới
    private void sendNotificationToAcademicStaffForNewRequest(TeacherRequest request) {
        try {
            // Đảm bảo teacher và userAccount được load
            if (request.getTeacher() == null || request.getTeacher().getUserAccount() == null) {
                log.error("Request {} không có teacher hoặc userAccount - không thể gửi notification", request.getId());
                return;
            }
            
            // Lấy branch của teacher
            Long teacherUserAccountId = request.getTeacher().getUserAccount().getId();
            List<Long> teacherBranchIds = getBranchIdsForUser(teacherUserAccountId);

            log.info("Tìm academic staff cho teacher {} với branch IDs: {}", teacherUserAccountId, teacherBranchIds);

            // Luôn lấy tất cả academic staff trước để đảm bảo không bỏ sót
            List<UserAccount> allAcademicStaff = userAccountRepository.findUsersByRole("ACADEMIC_AFFAIR");
            log.info("Tìm thấy tổng cộng {} academic staff trong hệ thống", allAcademicStaff.size());
            
            // Log chi tiết từng academic staff để debug
            if (allAcademicStaff.isEmpty()) {
                log.error("Không tìm thấy academic staff nào trong hệ thống với role ACADEMIC_AFFAIR!");
            } else {
                allAcademicStaff.forEach(staff -> {
                    List<Long> staffBranches = getBranchIdsForUser(staff.getId());
                    log.info("Academic staff: ID={}, Name={}, Email={}, Branches={}", 
                            staff.getId(), staff.getFullName(), staff.getEmail(), staffBranches);
                });
            }

            List<UserAccount> academicStaffUsers;

            // Nếu teacher có branch, filter academic staff theo branch trong code
            // Lưu ý: Academic staff có thể thuộc nhiều branch, chỉ cần có 1 branch trùng là được
            if (!teacherBranchIds.isEmpty() && !allAcademicStaff.isEmpty()) {
                // Filter academic staff có ít nhất 1 branch trùng với teacher
                academicStaffUsers = allAcademicStaff.stream()
                        .filter(staff -> {
                            List<Long> staffBranches = getBranchIdsForUser(staff.getId());
                            // Kiểm tra xem có ít nhất 1 branch trùng không (anyMatch)
                            boolean hasMatchingBranch = !staffBranches.isEmpty() && 
                                    staffBranches.stream().anyMatch(teacherBranchIds::contains);
                            if (hasMatchingBranch) {
                                log.info("Academic staff {} (Name: {}, Branches: {}) có branch trùng với teacher branches {}", 
                                        staff.getId(), staff.getFullName(), staffBranches, teacherBranchIds);
                            } else {
                                log.debug("Academic staff {} (Name: {}, Branches: {}) không có branch trùng với teacher branches {}", 
                                        staff.getId(), staff.getFullName(), staffBranches, teacherBranchIds);
                            }
                            return hasMatchingBranch;
                        })
                        .collect(Collectors.toList());
                log.info("Tìm thấy {} academic staff có branch trùng với teacher (teacher branches: {})", 
                        academicStaffUsers.size(), teacherBranchIds);
            } else {
                log.warn("Teacher {} không có branch được gán hoặc không có academic staff, sẽ gửi notification cho tất cả academic staff", teacherUserAccountId);
                academicStaffUsers = allAcademicStaff;
            }

            // Fallback: Nếu không tìm thấy theo branch, gửi cho tất cả academic staff
            if (academicStaffUsers.isEmpty() && !allAcademicStaff.isEmpty()) {
                log.info("Không tìm thấy academic staff theo branch, gửi cho tất cả {} academic staff", allAcademicStaff.size());
                academicStaffUsers = allAcademicStaff;
            }

            if (academicStaffUsers.isEmpty()) {
                log.error("Không tìm thấy academic staff nào trong hệ thống - không thể gửi notification");
                return;
            }

            // Tạo title và message
            String requestTypeName = getRequestTypeName(request.getRequestType());
            Session session = request.getSession();
            String sessionInfo = session != null 
                ? String.format("%s - %s", session.getDate(), session.getTimeSlotTemplate() != null ? session.getTimeSlotTemplate().getName() : "")
                : "";

            String title = String.format("Yêu cầu mới: %s", requestTypeName);
            String message = String.format(
                "Giáo viên %s đã tạo yêu cầu %s cho buổi học %s. Vui lòng xem xét và xử lý.",
                request.getTeacher().getUserAccount().getFullName(),
                requestTypeName.toLowerCase(),
                sessionInfo
            );

            List<Long> recipientIds = academicStaffUsers.stream()
                    .map(UserAccount::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (recipientIds.isEmpty()) {
                log.error("Danh sách recipient IDs rỗng - không thể gửi notification");
                return;
            }

            log.info("Chuẩn bị gửi notification cho {} academic staff (IDs: {}) về request {}",
                    recipientIds.size(), recipientIds, request.getId());

            notificationService.sendBulkNotifications(
                    recipientIds,
                    NotificationType.REQUEST,
                    title,
                    message
            );

            log.info("Đã gửi notification thành công cho {} academic staff về request {} (type: {})",
                    recipientIds.size(), request.getId(), request.getRequestType());
        } catch (Exception e) {
            log.error("Lỗi khi gửi notification cho academic staff về request {}: {}", 
                    request.getId(), e.getMessage(), e);
            // Không throw exception - notification failure không nên block việc tạo request
        }
    }

    // Gửi thông báo cho teacher khi request được approve
    private void sendNotificationToTeacherForApproval(TeacherRequest request) {
        try {
            Long teacherUserAccountId = request.getTeacher().getUserAccount().getId();
            String requestTypeName = getRequestTypeName(request.getRequestType());
            Session session = request.getSession();
            String sessionInfo = session != null 
                ? String.format("%s - %s", session.getDate(), session.getTimeSlotTemplate() != null ? session.getTimeSlotTemplate().getName() : "")
                : "";

            String title = String.format("Yêu cầu %s đã được duyệt", requestTypeName);
            String message = String.format(
                "Yêu cầu %s của bạn cho buổi học %s đã được giáo vụ duyệt.",
                requestTypeName.toLowerCase(),
                sessionInfo
            );

            notificationService.createNotification(
                    teacherUserAccountId,
                    NotificationType.NOTIFICATION,
                    title,
                    message
            );

            log.info("Đã gửi notification cho teacher {} về approval của request {}",
                    teacherUserAccountId, request.getId());
        } catch (Exception e) {
            log.error("Lỗi khi gửi notification cho teacher về approval của request {}: {}", request.getId(), e.getMessage());
            throw e;
        }
    }

    // Gửi email cho teacher khi request được approve (MODALITY_CHANGE/RESCHEDULE)
    @Async("emailTaskExecutor")
    private void sendEmailNotificationForApproval(TeacherRequest request) {
        try {
            UserAccount teacherAccount = request.getTeacher() != null ? request.getTeacher().getUserAccount() : null;
            if (teacherAccount == null || teacherAccount.getEmail() == null || teacherAccount.getEmail().trim().isEmpty()) {
                log.warn("Teacher {} has no email, skip approval email for request {}", 
                        request.getTeacher() != null ? request.getTeacher().getId() : null,
                        request.getId());
                return;
            }

            String email = teacherAccount.getEmail();
            String teacherName = teacherAccount.getFullName();

            String requestTypeName = getRequestTypeName(request.getRequestType());
            Session session = request.getSession();
            String sessionInfo = session != null
                    ? String.format("%s (%s)", 
                        session.getDate(), 
                        session.getClassEntity() != null ? session.getClassEntity().getCode() : "N/A")
                    : "N/A";

            String subject = "Yêu cầu của bạn đã được phê duyệt";
            StringBuilder body = new StringBuilder();
            body.append("Xin chào ").append(teacherName).append(",<br/><br/>")
                .append("Yêu cầu ").append(requestTypeName.toLowerCase())
                .append(" của bạn cho buổi học ").append(sessionInfo)
                .append(" đã được giáo vụ phê duyệt.<br/><br/>")
                .append("Trân trọng,<br/>Hệ thống TMS");

            emailService.sendEmailAsync(email, subject, body.toString());
        } catch (Exception e) {
            log.error("Lỗi khi gửi approval email cho teacher về request {}: {}", request.getId(), e.getMessage());
        }
    }

    // Gửi thông báo cho replacement teacher khi request được approve và chờ confirm
    private void sendNotificationToReplacementTeacher(TeacherRequest request) {
        try {
            if (request.getReplacementTeacher() == null || request.getReplacementTeacher().getUserAccount() == null) {
                log.warn("Replacement teacher không có user account - không thể gửi notification cho request {}", request.getId());
                return;
            }

            Long replacementTeacherUserAccountId = request.getReplacementTeacher().getUserAccount().getId();
            Session session = request.getSession();
            String sessionInfo = session != null 
                ? String.format("%s - %s", session.getDate(), session.getTimeSlotTemplate() != null ? session.getTimeSlotTemplate().getName() : "")
                : "";
            String originalTeacherName = request.getTeacher().getUserAccount().getFullName();

            String title = "Yêu cầu dạy thay";
            String message = String.format(
                "Bạn được mời dạy thay cho giáo viên %s tại buổi học %s. Vui lòng xác nhận.",
                originalTeacherName,
                sessionInfo
            );

            notificationService.createNotification(
                    replacementTeacherUserAccountId,
                    NotificationType.REQUEST,
                    title,
                    message
            );

            log.info("Đã gửi notification cho replacement teacher {} về request {}",
                    replacementTeacherUserAccountId, request.getId());
        } catch (Exception e) {
            log.error("Lỗi khi gửi notification cho replacement teacher về request {}: {}", request.getId(), e.getMessage());
            throw e;
        }
    }

    // Gửi email mời giáo viên dạy thay khi request REPLACEMENT được approve (WAITING_CONFIRM)
    @Async("emailTaskExecutor")
    private void sendEmailNotificationForReplacementInvitation(TeacherRequest request) {
        try {
            if (request.getReplacementTeacher() == null || request.getReplacementTeacher().getUserAccount() == null) {
                log.warn("Replacement teacher has no user account - skip replacement invitation email for request {}", request.getId());
                return;
            }

            UserAccount replacementAccount = request.getReplacementTeacher().getUserAccount();
            if (replacementAccount.getEmail() == null || replacementAccount.getEmail().trim().isEmpty()) {
                log.warn("Replacement teacher {} has no email - skip replacement invitation email for request {}", 
                        replacementAccount.getId(), request.getId());
                return;
            }

            String email = replacementAccount.getEmail();
            String replacementName = replacementAccount.getFullName();
            String originalTeacherName = request.getTeacher() != null && request.getTeacher().getUserAccount() != null
                    ? request.getTeacher().getUserAccount().getFullName()
                    : "một giáo viên khác";

            Session session = request.getSession();
            String sessionInfo = session != null
                    ? String.format("%s (%s)", 
                        session.getDate(), 
                        session.getClassEntity() != null ? session.getClassEntity().getCode() : "N/A")
                    : "N/A";

            String subject = "Lời mời dạy thay từ hệ thống TMS";
            StringBuilder body = new StringBuilder();
            body.append("Xin chào ").append(replacementName).append(",<br/><br/>")
                .append("Bạn được mời dạy thay cho giáo viên ").append(originalTeacherName)
                .append(" tại buổi học ").append(sessionInfo).append(".<br/>")
                .append("Vui lòng vào hệ thống để xác nhận hoặc từ chối yêu cầu này.<br/><br/>")
                .append("Trân trọng,<br/>Hệ thống TMS");

            emailService.sendEmailAsync(email, subject, body.toString());
        } catch (Exception e) {
            log.error("Lỗi khi gửi replacement invitation email cho request {}: {}", request.getId(), e.getMessage());
        }
    }

    // Gửi thông báo cho teacher khi request bị reject
    private void sendNotificationToTeacherForRejection(TeacherRequest request, String reason) {
        try {
            Long teacherUserAccountId = request.getTeacher().getUserAccount().getId();
            String requestTypeName = getRequestTypeName(request.getRequestType());
            Session session = request.getSession();
            String sessionInfo = session != null 
                ? String.format("%s - %s", session.getDate(), session.getTimeSlotTemplate() != null ? session.getTimeSlotTemplate().getName() : "")
                : "";

            String title = String.format("Yêu cầu %s đã bị từ chối", requestTypeName);
            String message = String.format(
                "Yêu cầu %s của bạn cho buổi học %s đã bị giáo vụ từ chối. Lý do: %s",
                requestTypeName.toLowerCase(),
                sessionInfo,
                reason
            );

            notificationService.createNotification(
                    teacherUserAccountId,
                    NotificationType.NOTIFICATION,
                    title,
                    message
            );

            log.info("Đã gửi notification cho teacher {} về rejection của request {}",
                    teacherUserAccountId, request.getId());
        } catch (Exception e) {
            log.error("Lỗi khi gửi notification cho teacher về rejection của request {}: {}", request.getId(), e.getMessage());
            throw e;
        }
    }

    // Gửi email cho teacher khi request bị reject
    @Async("emailTaskExecutor")
    private void sendEmailNotificationForRejection(TeacherRequest request, String reason) {
        try {
            UserAccount teacherAccount = request.getTeacher() != null ? request.getTeacher().getUserAccount() : null;
            if (teacherAccount == null || teacherAccount.getEmail() == null || teacherAccount.getEmail().trim().isEmpty()) {
                log.warn("Teacher {} has no email, skip rejection email for request {}", 
                        request.getTeacher() != null ? request.getTeacher().getId() : null,
                        request.getId());
                return;
            }

            String email = teacherAccount.getEmail();
            String teacherName = teacherAccount.getFullName();

            String requestTypeName = getRequestTypeName(request.getRequestType());
            Session session = request.getSession();
            String sessionInfo = session != null
                    ? String.format("%s (%s)", 
                        session.getDate(), 
                        session.getClassEntity() != null ? session.getClassEntity().getCode() : "N/A")
                    : "N/A";

            String subject = "Yêu cầu của bạn đã bị từ chối";
            StringBuilder body = new StringBuilder();
            body.append("Xin chào ").append(teacherName).append(",<br/><br/>")
                .append("Yêu cầu ").append(requestTypeName.toLowerCase())
                .append(" của bạn cho buổi học ").append(sessionInfo)
                .append(" đã bị giáo vụ từ chối.<br/>")
                .append("Lý do: ").append(reason).append("<br/><br/>")
                .append("Trân trọng,<br/>Hệ thống TMS");

            emailService.sendEmailAsync(email, subject, body.toString());
        } catch (Exception e) {
            log.error("Lỗi khi gửi rejection email cho teacher về request {}: {}", request.getId(), e.getMessage());
        }
    }

    // Gửi thông báo cho giáo vụ khi replacement teacher confirm
    private void sendNotificationToAcademicStaffForReplacementConfirmation(TeacherRequest request) {
        try {
            // Lấy branch của teacher
            Long teacherUserAccountId = request.getTeacher().getUserAccount().getId();
            List<Long> teacherBranchIds = getBranchIdsForUser(teacherUserAccountId);

            if (teacherBranchIds.isEmpty()) {
                log.warn("Không tìm thấy branch cho teacher {} - không thể gửi notification", teacherUserAccountId);
                return;
            }

            // Lấy tất cả academic staff của các branch này
            List<UserAccount> academicStaffUsers = userAccountRepository.findByRoleCodeAndBranches(
                    "ACADEMIC_AFFAIR", teacherBranchIds);

            if (academicStaffUsers.isEmpty()) {
                log.warn("Không tìm thấy academic staff nào cho branch {} - không thể gửi notification", teacherBranchIds);
                return;
            }

            Session session = request.getSession();
            String sessionInfo = session != null 
                ? String.format("%s - %s", session.getDate(), session.getTimeSlotTemplate() != null ? session.getTimeSlotTemplate().getName() : "")
                : "";
            String replacementTeacherName = request.getReplacementTeacher() != null && request.getReplacementTeacher().getUserAccount() != null
                ? request.getReplacementTeacher().getUserAccount().getFullName()
                : "";

            String title = "Giáo viên dạy thay đã xác nhận";
            String message = String.format(
                "Giáo viên %s đã xác nhận đồng ý dạy thay cho buổi học %s.",
                replacementTeacherName,
                sessionInfo
            );

            List<Long> recipientIds = academicStaffUsers.stream()
                    .map(UserAccount::getId)
                    .collect(Collectors.toList());

            notificationService.sendBulkNotifications(
                    recipientIds,
                    NotificationType.NOTIFICATION,
                    title,
                    message
            );

            log.info("Đã gửi notification cho {} academic staff về replacement confirmation của request {}",
                    recipientIds.size(), request.getId());
        } catch (Exception e) {
            log.error("Lỗi khi gửi notification cho academic staff về replacement confirmation của request {}: {}", request.getId(), e.getMessage());
            throw e;
        }
    }

    // Gửi email cho teacher khi tự tạo request (teacher side)
    @Async("emailTaskExecutor")
    private void sendEmailNotificationForCreatedRequest(TeacherRequest request) {
        try {
            UserAccount teacherAccount = request.getTeacher() != null ? request.getTeacher().getUserAccount() : null;
            if (teacherAccount == null || teacherAccount.getEmail() == null || teacherAccount.getEmail().trim().isEmpty()) {
                log.warn("Teacher {} has no email, skip created email for request {}", 
                        request.getTeacher() != null ? request.getTeacher().getId() : null,
                        request.getId());
                return;
            }

            String email = teacherAccount.getEmail();
            String teacherName = teacherAccount.getFullName();

            String requestTypeName = getRequestTypeName(request.getRequestType());
            Session session = request.getSession();
            String sessionInfo = session != null
                    ? String.format("%s (%s)", 
                        session.getDate(), 
                        session.getClassEntity() != null ? session.getClassEntity().getCode() : "N/A")
                    : "N/A";

            String subject = "Bạn đã tạo yêu cầu mới trên hệ thống TMS";
            StringBuilder body = new StringBuilder();
            body.append("Xin chào ").append(teacherName).append(",<br/><br/>")
                .append("Bạn vừa tạo yêu cầu ").append(requestTypeName.toLowerCase())
                .append(" cho buổi học ").append(sessionInfo).append(".<br/>")
                .append("Hệ thống sẽ thông báo khi yêu cầu được xử lý.<br/><br/>")
                .append("Trân trọng,<br/>Hệ thống TMS");

            emailService.sendEmailAsync(email, subject, body.toString());
        } catch (Exception e) {
            log.error("Lỗi khi gửi created email cho teacher về request {}: {}", request.getId(), e.getMessage());
        }
    }

    // Gửi thông báo cho teacher gốc khi replacement teacher confirm
    private void sendNotificationToOriginalTeacherForReplacementConfirmation(TeacherRequest request) {
        try {
            Long teacherUserAccountId = request.getTeacher().getUserAccount().getId();
            Session session = request.getSession();
            String sessionInfo = session != null 
                ? String.format("%s - %s", session.getDate(), session.getTimeSlotTemplate() != null ? session.getTimeSlotTemplate().getName() : "")
                : "";
            String replacementTeacherName = request.getReplacementTeacher() != null && request.getReplacementTeacher().getUserAccount() != null
                ? request.getReplacementTeacher().getUserAccount().getFullName()
                : "";

            String title = "Yêu cầu dạy thay đã được xác nhận";
            String message = String.format(
                "Giáo viên %s đã xác nhận đồng ý dạy thay cho buổi học %s của bạn.",
                replacementTeacherName,
                sessionInfo
            );

            notificationService.createNotification(
                    teacherUserAccountId,
                    NotificationType.NOTIFICATION,
                    title,
                    message
            );

            log.info("Đã gửi notification cho teacher {} về replacement confirmation của request {}",
                    teacherUserAccountId, request.getId());
        } catch (Exception e) {
            log.error("Lỗi khi gửi notification cho teacher về replacement confirmation của request {}: {}", request.getId(), e.getMessage());
            throw e;
        }
    }

    // Gửi thông báo cho teacher khi giáo vụ tạo request hộ
    private void sendNotificationToTeacherForStaffCreatedRequest(TeacherRequest request) {
        try {
            Long teacherUserAccountId = request.getTeacher().getUserAccount().getId();
            String requestTypeName = getRequestTypeName(request.getRequestType());
            Session session = request.getSession();
            String sessionInfo = session != null 
                ? String.format("%s - %s", session.getDate(), session.getTimeSlotTemplate() != null ? session.getTimeSlotTemplate().getName() : "")
                : "";

            String title = String.format("Yêu cầu %s đã được tạo", requestTypeName);
            String message = String.format(
                "Giáo vụ đã tạo yêu cầu %s cho buổi học %s của bạn. Yêu cầu đã được tự động duyệt.",
                requestTypeName.toLowerCase(),
                sessionInfo
            );

            notificationService.createNotification(
                    teacherUserAccountId,
                    NotificationType.NOTIFICATION,
                    title,
                    message
            );

            log.info("Đã gửi notification cho teacher {} về staff-created request {}",
                    teacherUserAccountId, request.getId());
        } catch (Exception e) {
            log.error("Lỗi khi gửi notification cho teacher về staff-created request {}: {}", request.getId(), e.getMessage());
            throw e;
        }
    }

    // Lấy tên tiếng Việt của request type
    private String getRequestTypeName(TeacherRequestType requestType) {
        switch (requestType) {
            case MODALITY_CHANGE:
                return "Đổi phương tiện";
            case RESCHEDULE:
                return "Đổi lịch";
            case REPLACEMENT:
                return "Dạy thay";
            default:
                return "Yêu cầu";
        }
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
    public TeacherRequestResponseDTO getRequestForStaff(Long requestId, Long academicStaffUserId, Long branchId) {
        log.info("Getting request {} for staff {} with branch {}", requestId, academicStaffUserId, branchId);

        TeacherRequest request = teacherRequestRepository.findByIdWithTeacherAndSession(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_REQUEST_NOT_FOUND, "Request not found"));

        // Force load relationships
        if (request.getNewResource() != null) {
            request.getNewResource().getName();
        }
        if (request.getNewTimeSlot() != null) {
            request.getNewTimeSlot().getName();
        }

        // Ensure academic staff only sees requests of teachers in the same branch
        Long teacherUserAccountId = request.getTeacher() != null && request.getTeacher().getUserAccount() != null
                ? request.getTeacher().getUserAccount().getId()
                : null;
        if (teacherUserAccountId != null) {
            List<Long> teacherBranches = getBranchIdsForUser(teacherUserAccountId);
            List<Long> academicBranches = getBranchIdsForUser(academicStaffUserId);
            boolean allowed = teacherBranches.isEmpty()
                    || academicBranches.isEmpty()
                    || teacherBranches.stream().anyMatch(academicBranches::contains);
            if (branchId != null) {
                allowed = allowed && teacherBranches.contains(branchId) && academicBranches.contains(branchId);
            }
            if (!allowed) {
                throw new CustomException(ErrorCode.TEACHER_ACCESS_DENIED, "You don't have permission to view this request");
            }
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
        UserAccount submittedBy = request.getSubmittedBy();
        UserAccount handler = decidedBy != null ? decidedBy : submittedBy;
        OffsetDateTime decidedAt = request.getDecidedAt() != null ? request.getDecidedAt() : request.getSubmittedAt();
        Resource newResource = request.getNewResource();
        TimeSlotTemplate newTimeSlot = request.getNewTimeSlot();
        Session newSession = request.getNewSession();

        // Lấy thông tin buổi mới (nếu có)
        LocalDate newSessionDate = newSession != null ? newSession.getDate() : request.getNewDate();
        LocalTime newSessionStart = newSession != null && newSession.getTimeSlotTemplate() != null
                ? newSession.getTimeSlotTemplate().getStartTime()
                : (newTimeSlot != null ? newTimeSlot.getStartTime() : null);
        LocalTime newSessionEnd = newSession != null && newSession.getTimeSlotTemplate() != null
                ? newSession.getTimeSlotTemplate().getEndTime()
                : (newTimeSlot != null ? newTimeSlot.getEndTime() : null);
        String newSessionResourceName = null;
        if (newSession != null && newSession.getSessionResources() != null && !newSession.getSessionResources().isEmpty()) {
            newSessionResourceName = newSession.getSessionResources().iterator().next().getResource().getName();
        } else if (newResource != null) {
            newSessionResourceName = newResource.getName();
        }
        String newSessionClassCode = newSession != null && newSession.getClassEntity() != null
                ? newSession.getClassEntity().getCode()
                : null;

        // Current modality/resource
        // MODALITY_CHANGE và RESCHEDULE: lấy modality từ session (resource), không phải từ class
        String currentModality = null;
        if ((request.getRequestType() == TeacherRequestType.MODALITY_CHANGE || 
             request.getRequestType() == TeacherRequestType.RESCHEDULE) && session != null) {
            Modality sessionModality = getSessionModality(session);
            if (sessionModality != null) {
                currentModality = sessionModality.name();
            }
        }
        // Fallback về class modality nếu không lấy được từ session
        if (currentModality == null && classEntity != null && classEntity.getModality() != null) {
            currentModality = classEntity.getModality().name();
        }
        String currentResourceName = session != null && session.getSessionResources() != null && !session.getSessionResources().isEmpty()
                ? session.getSessionResources().iterator().next().getResource().getName()
                : null;

        // New modality/resource
        String newModality = newResource != null && newResource.getResourceType() != null
                ? newResource.getResourceType().name()
                : null;
        String newResourceType = newResource != null && newResource.getResourceType() != null
                ? newResource.getResourceType().name()
                : null;

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
                .currentModality(currentModality)
                .currentResourceName(currentResourceName)
                .newModality(newModality)
                .newResourceType(newResourceType)
                .requestReason(request.getRequestReason())
                .note(request.getNote())
                .submittedAt(request.getSubmittedAt())
                .decidedAt(decidedAt)
                .decidedById(handler != null ? handler.getId() : null)
                .decidedByName(handler != null ? handler.getFullName() : null)
                .decidedByEmail(handler != null ? handler.getEmail() : null)
                .newSessionId(newSession != null ? newSession.getId() : null)
                .newSessionDate(newSessionDate)
                .newSessionStartTime(newSessionStart)
                .newSessionEndTime(newSessionEnd)
                .newSessionResourceName(newSessionResourceName)
                .newSessionClassCode(newSessionClassCode);

        // Debug log to trace who is shown as handler in responses
        log.debug(
                "mapToResponseDTO id={} type={} status={} decidedBy={} submittedBy={} handler={}",
                request.getId(),
                request.getRequestType(),
                request.getStatus(),
                decidedBy != null ? decidedBy.getFullName() : null,
                submittedBy != null ? submittedBy.getFullName() : null,
                handler != null ? handler.getFullName() : null
        );

        // Hiển thị thông tin dựa trên yêu cầu
        switch (request.getRequestType()) {
            case MODALITY_CHANGE:
                builder.newResourceId(newResource != null ? newResource.getId() : null)
                        .newResourceName(newResource != null ? newResource.getName() : null)
                        .newResourceType(newResourceType)
                        .newModality(newModality)
                        .currentModality(currentModality)
                        .currentResourceName(currentResourceName)
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
                .filter(this::isUpcomingSession)
                .map(this::mapToMySessionDTO)
                .collect(Collectors.toList());
    }

    //Map Session entity sang MySessionDTO
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

    
      //Chỉ giữ các session chưa diễn ra:
      //- Ngày sau hôm nay, hoặc
      //- Cùng ngày nhưng startTime còn ở tương lai
    private boolean isUpcomingSession(Session session) {
        if (session == null || session.getDate() == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        if (session.getDate().isAfter(today)) {
            return true;
        }
        if (session.getDate().isEqual(today)) {
            TimeSlotTemplate slot = session.getTimeSlotTemplate();
            if (slot != null && slot.getStartTime() != null) {
                return slot.getStartTime().isAfter(java.time.LocalTime.now());
            }
            // Nếu không có startTime, không chắc trạng thái -> loại bỏ để tránh gợi ý buổi đã diễn ra
            return false;
        }
        return false;
    }

    //Gợi ý giáo viên dạy thay cho REPLACEMENT request (cho teacher)
    @Transactional(readOnly = true)
    public List<ReplacementCandidateDTO> suggestReplacementCandidates(Long sessionId, Long userId) {
        log.info("Suggesting replacement candidates for session {} by teacher {}", sessionId, userId);

        // Lấy teacher hiện tại
        Teacher requestingTeacher = teacherRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND, "Teacher not found"));

        // Lấy session và kiểm tra quyền
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT, "Session not found"));

        boolean isOwner = session.getTeachingSlots().stream()
                .anyMatch(slot -> slot.getTeacher() != null && slot.getTeacher().getId().equals(requestingTeacher.getId()));
        if (!isOwner) {
            throw new CustomException(ErrorCode.FORBIDDEN, "You are not assigned to this session");
        }

        // Lấy subject từ session
        org.fyp.tmssep490be.entities.ClassEntity classEntity = session.getClassEntity();
        if (classEntity == null || classEntity.getSubject() == null) {
            log.warn("Session {} has no subject information", sessionId);
            return List.of();
        }

        org.fyp.tmssep490be.entities.Subject subject = classEntity.getSubject();

        // Lấy branch của giáo viên tạo yêu cầu
        List<Long> requestingTeacherBranchIds = userBranchesRepository.findBranchIdsByUserId(requestingTeacher.getUserAccount().getId());
        
        // Lấy các giáo viên cùng branch với giáo viên tạo yêu cầu (trừ teacher hiện tại)
        List<Teacher> candidateTeachers;
        if (requestingTeacherBranchIds.isEmpty()) {
            candidateTeachers = List.of();
        } else {
            candidateTeachers = teacherRepository.findByBranchIds(requestingTeacherBranchIds).stream()
                    .filter(teacher -> !teacher.getId().equals(requestingTeacher.getId()))
                    .collect(Collectors.toList());
        }

        // Loại trừ các teacher đã từ chối request này (nếu có requestId)

        // Lấy skills yêu cầu của session từ SubjectSession
        Set<org.fyp.tmssep490be.entities.enums.Skill> sessionRequiredSkills = getSessionRequiredSkills(session);

        // Filter teachers có conflict về thời gian
        LocalDate sessionDate = session.getDate();
        TimeSlotTemplate sessionTimeSlot = session.getTimeSlotTemplate();
        List<Teacher> availableTeachers = candidateTeachers.stream()
                .filter(teacher -> !hasTeacherTimeConflict(teacher.getId(), sessionDate, sessionTimeSlot, session.getId()))
                .collect(Collectors.toList());

        // Map sang DTO và sắp xếp theo skill phù hợp, sau đó theo tên
        final Set<org.fyp.tmssep490be.entities.enums.Skill> finalSessionRequiredSkills = sessionRequiredSkills;
        return availableTeachers.stream()
                .map(teacher -> mapToReplacementCandidateDTO(teacher, subject, session))
                .filter(dto -> dto != null)
                .sorted((a, b) -> {
                    // 1. Ưu tiên teacher có skills phù hợp với skills yêu cầu của session
                    boolean aHasMatchingSkills = hasMatchingSkills(a, finalSessionRequiredSkills);
                    boolean bHasMatchingSkills = hasMatchingSkills(b, finalSessionRequiredSkills);
                    
                    if (aHasMatchingSkills && !bHasMatchingSkills) {
                        return -1; // a được ưu tiên
                    }
                    if (!aHasMatchingSkills && bHasMatchingSkills) {
                        return 1; // b được ưu tiên
                    }
                    
                    // 2. Nếu cả hai đều có hoặc không có matching skills, sắp xếp theo số lượng skills (nhiều hơn = phù hợp hơn)
                    int aSkillCount = a.getSkills() != null ? a.getSkills().size() : 0;
                    int bSkillCount = b.getSkills() != null ? b.getSkills().size() : 0;
                    if (aSkillCount != bSkillCount) {
                        return Integer.compare(bSkillCount, aSkillCount); // Giảm dần
                    }
                    
                    // 3. Cuối cùng sắp xếp theo tên tăng dần
                    String nameA = a.getFullName() != null ? a.getFullName() : "";
                    String nameB = b.getFullName() != null ? b.getFullName() : "";
                    return nameA.compareToIgnoreCase(nameB);
                })
                .collect(Collectors.toList());
    }

    //Map Teacher entity sang ReplacementCandidateDTO
    private ReplacementCandidateDTO mapToReplacementCandidateDTO(
            Teacher teacher, org.fyp.tmssep490be.entities.Subject subject, Session session) {
        UserAccount userAccount = teacher.getUserAccount();
        if (userAccount == null) {
            return null;
        }

        // Lấy skills
        List<ReplacementCandidateSkillDTO> skills = teacher.getTeacherSkills().stream()
                .map(ts -> ReplacementCandidateSkillDTO.builder()
                        .id(ts.getId() != null ? ts.getId().getSkill() != null ? (long) ts.getId().getSkill().ordinal() : null : null)
                        .name(ts.getId() != null && ts.getId().getSkill() != null ? ts.getId().getSkill().name() : null)
                        .skillName(ts.getId() != null && ts.getId().getSkill() != null ? ts.getId().getSkill().name() : null)
                        .level(ts.getLevel() != null ? ts.getLevel().toString() : null)
                        .skillLevel(ts.getLevel() != null ? ts.getLevel().toString() : null)
                        .description(ts.getSpecialization())
                        .build())
                .collect(Collectors.toList());

        // Tạo skill summary
        String skillSummary = teacher.getTeacherSkills().stream()
                .map(ts -> ts.getId() != null && ts.getId().getSkill() != null ? ts.getId().getSkill().name() : "")
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(", "));

        return ReplacementCandidateDTO.builder()
                .teacherId(teacher.getId())
                .fullName(userAccount.getFullName())
                .displayName(userAccount.getFullName())
                .email(userAccount.getEmail())
                .phone(userAccount.getPhone())
                .skills(skills)
                .skillSummary(skillSummary)
                .specialization(teacher.getTeacherSkills().stream()
                        .map(org.fyp.tmssep490be.entities.TeacherSkill::getSpecialization)
                        .filter(s -> s != null && !s.isEmpty())
                        .findFirst()
                        .orElse(null))
                .note(teacher.getNote())
                .build();
    }

    //Kiểm tra xem teacher có skills phù hợp với skills yêu cầu của session không
    private boolean hasMatchingSkills(ReplacementCandidateDTO candidate, Set<org.fyp.tmssep490be.entities.enums.Skill> sessionRequiredSkills) {
        if (sessionRequiredSkills.isEmpty() || candidate.getSkills() == null || candidate.getSkills().isEmpty()) {
            return false;
        }

        // Lấy danh sách skills của candidate
        Set<org.fyp.tmssep490be.entities.enums.Skill> candidateSkills = candidate.getSkills().stream()
                .filter(skill -> skill.getName() != null)
                .map(skill -> {
                    try {
                        return org.fyp.tmssep490be.entities.enums.Skill.valueOf(skill.getName());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(skill -> skill != null)
                .collect(Collectors.toSet());

        // Kiểm tra xem có skill nào trùng với skills yêu cầu của session không
        return candidateSkills.stream().anyMatch(sessionRequiredSkills::contains);
    }

    //Lấy danh sách skills yêu cầu của session từ SubjectSession
    private Set<org.fyp.tmssep490be.entities.enums.Skill> getSessionRequiredSkills(Session session) {
        Set<org.fyp.tmssep490be.entities.enums.Skill> requiredSkills = new HashSet<>();
        
        if (session == null || session.getSubjectSession() == null) {
            log.warn("Session {} has no SubjectSession", session != null ? session.getId() : "null");
            return requiredSkills;
        }
        
        org.fyp.tmssep490be.entities.SubjectSession subjectSession = session.getSubjectSession();
        List<org.fyp.tmssep490be.entities.enums.Skill> skills = subjectSession.getSkills();
        
        if (skills == null || skills.isEmpty()) {
            log.warn("SubjectSession {} has no skills requirement", subjectSession.getId());
            return requiredSkills;
        }
        
        // SubjectSession.skills đã là List<Skill>, chỉ cần chuyển sang Set
        requiredSkills.addAll(skills);
        
        return requiredSkills;
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

        // Gửi thông báo + email cho teacher
        try {
            if (request.getRequestType() == TeacherRequestType.REPLACEMENT && request.getStatus() == RequestStatus.WAITING_CONFIRM) {
                // REPLACEMENT: gửi thông báo cho replacement teacher
                sendNotificationToReplacementTeacher(request);
                // Email cho replacement teacher (mời dạy thay)
                sendEmailNotificationForReplacementInvitation(request);
            } else {
                // MODALITY_CHANGE và RESCHEDULE: gửi thông báo cho teacher gốc
                sendNotificationToTeacherForApproval(request);
                // Email cho teacher về việc request đã được duyệt
                sendEmailNotificationForApproval(request);
                
                // NEW: Gửi notification + email cho STUDENTS về schedule change
                if (request.getRequestType() == TeacherRequestType.RESCHEDULE || 
                    request.getRequestType() == TeacherRequestType.MODALITY_CHANGE) {
                    sendScheduleChangedNotificationToStudents(request);
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi gửi notification/email cho teacher về request {}: {}", requestId, e.getMessage());
            // Không throw exception - notification/email failure không nên block việc approve
        }

        return mapToResponseDTO(request);
    }

    //Validate và approve REPLACEMENT request
    private void validateAndApproveReplacement(TeacherRequest request, TeacherRequestApproveDTO approveDTO) {
        // REPLACEMENT: Ưu tiên replacement teacher do academic staff chọn trong approveDTO
        // Nếu academic staff không chọn, dùng replacement teacher mà teacher đã đề xuất khi tạo request
        Long chosenReplacementTeacherId = approveDTO.getReplacementTeacherId() != null
                ? approveDTO.getReplacementTeacherId()
                : (request.getReplacementTeacher() != null ? request.getReplacementTeacher().getId() : null);

        // Nếu cả academic staff và teacher đều không chọn, bắt buộc academic staff phải chọn
        if (chosenReplacementTeacherId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Replacement teacher ID is required. Please select a replacement teacher.");
        }

        // Kiểm tra replacement teacher tồn tại
        Teacher replacementTeacher = teacherRepository.findById(chosenReplacementTeacherId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND, "Replacement teacher not found"));

        // Không được chọn chính giáo viên tạo request
        if (replacementTeacher.getId().equals(request.getTeacher().getId())) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Replacement teacher cannot be the same as the requesting teacher");
        }

        // Validate conflict thời gian: kiểm tra overlap với các session đã được assign và các request đang pending
        Session session = request.getSession();
        if (session != null) {
            LocalDate sessionDate = session.getDate();
            TimeSlotTemplate sessionTimeSlot = session.getTimeSlotTemplate();
            
            if (sessionDate != null && sessionTimeSlot != null) {
                // Kiểm tra conflict với các session đã được assign (có TeachingSlot trong DB)
                boolean hasConflictWithAssignedSessions = hasTeacherTimeConflict(
                        chosenReplacementTeacherId, 
                        sessionDate, 
                        sessionTimeSlot, 
                        session.getId());
                
                if (hasConflictWithAssignedSessions) {
                    throw new CustomException(ErrorCode.INVALID_INPUT, 
                            "Replacement teacher already has another assigned session at overlapping time");
                }
                
                // Kiểm tra conflict với các pending replacement requests khác
                List<TeacherRequest> pendingRequests = teacherRequestRepository
                        .findByStatusOrderBySubmittedAtDesc(RequestStatus.PENDING);
                
                boolean hasConflictWithPendingRequests = pendingRequests.stream()
                        .filter(tr -> tr.getId() != null && !tr.getId().equals(request.getId())) // Loại trừ request hiện tại
                        .filter(tr -> tr.getRequestType() == TeacherRequestType.REPLACEMENT)
                        .anyMatch(tr -> {
                            // Kiểm tra xem pending request có cùng replacement teacher không
                            Teacher pendingReplacementTeacher = tr.getReplacementTeacher();
                            if (pendingReplacementTeacher == null 
                                    || !pendingReplacementTeacher.getId().equals(chosenReplacementTeacherId)) {
                                return false;
                            }
                            
                            // Kiểm tra xem session của pending request có cùng ngày và overlap thời gian không
                            Session pendingSession = tr.getSession();
                            if (pendingSession == null || pendingSession.getDate() == null 
                                    || !pendingSession.getDate().equals(sessionDate)) {
                                return false;
                            }
                            
                            TimeSlotTemplate pendingTimeSlot = pendingSession.getTimeSlotTemplate();
                            if (pendingTimeSlot == null) {
                                return false;
                            }
                            
                            // Kiểm tra overlap thời gian
                            return hasTimeOverlap(pendingTimeSlot, sessionTimeSlot);
                        });
                
                if (hasConflictWithPendingRequests) {
                    throw new CustomException(ErrorCode.INVALID_INPUT, 
                            "Replacement teacher is already selected by another pending replacement request at overlapping time");
                }
            }
        }

        // Cập nhật replacement teacher (ưu tiên lựa chọn của academic staff)
        request.setReplacementTeacher(replacementTeacher);
    }

    //Validate và approve MODALITY_CHANGE request
    private void validateAndApproveModalityChange(TeacherRequest request, TeacherRequestApproveDTO approveDTO) {
        // Ưu tiên resource do staff chọn; nếu staff không chọn, dùng resource teacher đã chọn
        Long chosenResourceId = approveDTO.getNewResourceId() != null
                ? approveDTO.getNewResourceId()
                : (request.getNewResource() != null ? request.getNewResource().getId() : null);

        if (chosenResourceId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Resource ID is required for MODALITY_CHANGE requests");
        }

        Resource newResource = resourceRepository.findById(chosenResourceId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT, "Resource not found"));

        request.setNewResource(newResource);

        // Cập nhật session với resource mới
        Session session = request.getSession();
        if (session == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Request has no associated session");
        }

        // Validate conflict resource: kiểm tra overlap với các session đã được approve và các request đang pending
        LocalDate sessionDate = session.getDate();
        TimeSlotTemplate sessionTimeSlot = session.getTimeSlotTemplate();
        
        if (sessionDate != null && sessionTimeSlot != null) {
            // Kiểm tra conflict với các session đã được approve (đã có SessionResource trong DB)
            List<SessionResource> existingSessionResources = sessionResourceRepository
                    .findByResourceIdAndSessionDate(chosenResourceId, sessionDate);
            
            boolean hasConflictWithApprovedSessions = existingSessionResources.stream()
                    .anyMatch(sr -> {
                        Session existingSession = sr.getSession();
                        // Bỏ qua các session đã bị hủy và session hiện tại
                        if (existingSession.getStatus() == SessionStatus.CANCELLED 
                                || existingSession.getId().equals(session.getId())) {
                            return false;
                        }
                        // Kiểm tra overlap thời gian
                        TimeSlotTemplate existingTimeSlot = existingSession.getTimeSlotTemplate();
                        return existingTimeSlot != null && hasTimeOverlap(existingTimeSlot, sessionTimeSlot);
                    });
            
            if (hasConflictWithApprovedSessions) {
                throw new CustomException(
                        ErrorCode.INVALID_INPUT,
                        "Phòng/phương tiện này đã được đặt cho một buổi học khác trong khung giờ trùng nhau"
                );
            }
            
            // Kiểm tra conflict với các request đang pending (MODALITY_CHANGE hoặc RESCHEDULE)
            List<TeacherRequest> pendingRequests = teacherRequestRepository
                    .findByStatusOrderBySubmittedAtDesc(RequestStatus.PENDING);
            
            boolean hasConflictWithPendingRequests = pendingRequests.stream()
                    .filter(tr -> tr.getId() != null && !tr.getId().equals(request.getId())) // Loại trừ request hiện tại
                    .filter(tr -> tr.getRequestType() == TeacherRequestType.MODALITY_CHANGE 
                            || tr.getRequestType() == TeacherRequestType.RESCHEDULE)
                    .anyMatch(tr -> {
                        // Kiểm tra xem pending request có cùng resource không
                        Resource pendingResource = tr.getNewResource();
                        if (pendingResource == null || !pendingResource.getId().equals(chosenResourceId)) {
                            return false;
                        }
                        
                        // Kiểm tra xem session của pending request có cùng ngày và overlap thời gian không
                        Session pendingSession = tr.getSession();
                        if (pendingSession == null || pendingSession.getDate() == null 
                                || !pendingSession.getDate().equals(sessionDate)) {
                            return false;
                        }
                        
                        TimeSlotTemplate pendingTimeSlot = pendingSession.getTimeSlotTemplate();
                        if (pendingTimeSlot == null) {
                            return false;
                        }
                        
                        // Kiểm tra overlap thời gian
                        return hasTimeOverlap(pendingTimeSlot, sessionTimeSlot);
                    });
            
            if (hasConflictWithPendingRequests) {
                throw new CustomException(ErrorCode.INVALID_INPUT, 
                        "Resource is already selected by another pending request at overlapping time");
            }
        }

        // Update session resources: xóa resources cũ và thêm resource mới
        // Xóa tất cả session resources hiện tại
        if (!session.getSessionResources().isEmpty()) {
            sessionResourceRepository.deleteAll(session.getSessionResources());
            session.getSessionResources().clear();
        }

        // Tạo session resource mới
        SessionResource newSessionResource = new SessionResource();
        SessionResource.SessionResourceId sessionResourceId = new SessionResource.SessionResourceId();
        sessionResourceId.setSessionId(session.getId());
        sessionResourceId.setResourceId(newResource.getId());
        newSessionResource.setId(sessionResourceId);
        newSessionResource.setSession(session);
        newSessionResource.setResource(newResource);

        // Thêm vào session
        session.getSessionResources().add(newSessionResource);

        // Lưu session (cascade sẽ lưu SessionResource)
        sessionRepository.save(session);

        log.info("Session {} updated for MODALITY_CHANGE: resource={}", 
                session.getId(), newResource.getId());
    }

    //Validate và approve RESCHEDULE request
    private void validateAndApproveReschedule(TeacherRequest request, TeacherRequestApproveDTO approveDTO) {
        // Lấy các giá trị mới (ưu tiên approveDTO)
        Long chosenResourceId = approveDTO.getNewResourceId() != null
                ? approveDTO.getNewResourceId()
                : (request.getNewResource() != null ? request.getNewResource().getId() : null);
        LocalDate newDate = approveDTO.getNewDate() != null ? approveDTO.getNewDate() : request.getNewDate();
        Long newTimeSlotId = approveDTO.getNewTimeSlotId() != null ? approveDTO.getNewTimeSlotId()
                : (request.getNewTimeSlot() != null ? request.getNewTimeSlot().getId() : null);

        // Validate tối thiểu
        if (chosenResourceId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Resource ID is required for RESCHEDULE requests");
        }
        if (newDate == null || newTimeSlotId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "New date and time slot are required for RESCHEDULE requests");
        }
        if (newDate.isBefore(LocalDate.now())) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "New date cannot be in the past");
        }

        Resource newResource = resourceRepository.findById(chosenResourceId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT, "Resource not found"));
        TimeSlotTemplate newTimeSlot = timeSlotTemplateRepository.findById(newTimeSlotId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT, "Time slot not found"));

        // Cập nhật thông tin mới vào request
        request.setNewDate(newDate);
        request.setNewTimeSlot(newTimeSlot);
        request.setNewResource(newResource);

        // Reschedule theo cách của bản deprecated: tạo session mới, copy, cancel session cũ
        Session oldSession = sessionRepository.findById(request.getSession().getId())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT, "Session not found"));

        // Tạo session mới
        OffsetDateTime now = OffsetDateTime.now();
        Session newSession = Session.builder()
                .classEntity(oldSession.getClassEntity())
                .subjectSession(oldSession.getSubjectSession())
                .timeSlotTemplate(newTimeSlot)
                .date(newDate)
                .type(oldSession.getType())
                .status(SessionStatus.PLANNED)
                .teacherNote(null)
                .createdAt(now)
                .updatedAt(now)
                .build();
        newSession = sessionRepository.save(newSession);

        // Copy teaching slot của giáo viên
        TeachingSlot newTeachingSlot = TeachingSlot.builder()
                .id(new TeachingSlot.TeachingSlotId(newSession.getId(), request.getTeacher().getId()))
                .session(newSession)
                .teacher(request.getTeacher())
                .status(TeachingSlotStatus.SCHEDULED)
                .build();
        teachingSlotRepository.save(newTeachingSlot);

        // Xử lý các trường hợp EXCUSED trước
        List<StudentSession> excusedStudentSessions = studentSessionRepository.findAll().stream()
                .filter(ss -> ss.getSession().getId().equals(oldSession.getId()))
                .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.EXCUSED)
                .collect(Collectors.toList());

        // Tìm tất cả makeup sessions trỏ đến session cũ (bất kể PRESENT/ABSENT/PLANNED)
        List<StudentSession> makeupSessions = studentSessionRepository.findMakeupSessionsByOriginalSessionIds(
                List.of(oldSession.getId()));

        // Map: studentId -> bất kỳ makeup session (giữ cái đầu tiên)
        Map<Long, StudentSession> makeupMap = makeupSessions.stream()
                .collect(Collectors.toMap(
                        ss -> ss.getStudent().getId(),
                        ss -> ss,
                        (existing, replacement) -> existing
                ));

        // Set để track học viên đã được xử lý
        Set<Long> processedStudentIds = new HashSet<>();

        // Logic đơn giản: chỉ phân biệt 2 trường hợp
        // 1) Có makeup session (request MAKEUP đã được APPROVED) -> giữ EXCUSED
        // 2) Không có makeup session (chỉ PENDING/REJECTED/không có request) -> reset về PLANNED
        for (StudentSession excusedSs : excusedStudentSessions) {
            Long studentId = excusedSs.getStudent().getId();
            processedStudentIds.add(studentId);

            StudentSession makeupSession = makeupMap.get(studentId);

            if (makeupSession != null) {
                // Trường hợp 1: Có makeup session đã được tạo (đã được APPROVED)
                // a) Cập nhật originalSession của makeup session trỏ đến session mới
                makeupSession.setOriginalSession(newSession);
                studentSessionRepository.save(makeupSession);
                log.info("Updated makeup session {} originalSession from {} to {} for student {}",
                        makeupSession.getId().getSessionId(), oldSession.getId(), newSession.getId(), studentId);

                // b) Cập nhật StudentRequest.targetSession cho các request MAKEUP đã APPROVED
                List<org.fyp.tmssep490be.entities.StudentRequest> makeupRequests = studentRequestRepository
                        .findByStudentIdAndTargetSessionIdAndRequestType(
                                studentId, oldSession.getId(), StudentRequestType.MAKEUP);
                for (org.fyp.tmssep490be.entities.StudentRequest makeupRequest : makeupRequests) {
                    if (makeupRequest.getStatus() == RequestStatus.APPROVED) {
                        makeupRequest.setTargetSession(newSession);
                        studentRequestRepository.save(makeupRequest);
                        log.info("Updated makeup request {} targetSession from {} to {}",
                                makeupRequest.getId(), oldSession.getId(), newSession.getId());
                    }
                }

                // c) Tạo StudentSession mới cho buổi được dời với status = EXCUSED (để hiển thị chấm xanh)
                StudentSession.StudentSessionId newSsId = new StudentSession.StudentSessionId();
                newSsId.setStudentId(studentId);
                newSsId.setSessionId(newSession.getId());

                StudentSession newSs = StudentSession.builder()
                        .id(newSsId)
                        .student(excusedSs.getStudent())
                        .session(newSession)
                        .attendanceStatus(AttendanceStatus.EXCUSED)
                        .isMakeup(false)
                        .homeworkStatus(null)
                        .note("Buổi học được đổi lịch từ session " + oldSession.getId())
                        .build();
                studentSessionRepository.save(newSs);
                log.info("Created new EXCUSED StudentSession for student {} in new session {} (has makeup session)",
                        studentId, newSession.getId());
            } else {
                // Trường hợp 2: Không có makeup session (chỉ PENDING/REJECTED/không có request)
                List<org.fyp.tmssep490be.entities.StudentRequest> pendingRequests = studentRequestRepository
                        .findByStudentIdAndTargetSessionIdAndRequestType(
                                studentId, oldSession.getId(), StudentRequestType.MAKEUP);

                // Hủy (CANCEL) các request PENDING vì buổi gốc đã được dời
                for (org.fyp.tmssep490be.entities.StudentRequest pendingRequest : pendingRequests) {
                    if (pendingRequest.getStatus() == RequestStatus.PENDING) {
                        pendingRequest.setStatus(RequestStatus.CANCELLED);
                        pendingRequest.setNote("Request cancelled due to session reschedule from " + oldSession.getId()
                                + " to " + newSession.getId());
                        studentRequestRepository.save(pendingRequest);
                        log.info("Cancelled makeup request {} for student {} due to session reschedule",
                                pendingRequest.getId(), studentId);
                    }
                }

                // Tạo StudentSession mới với status = PLANNED cho buổi được dời
                StudentSession.StudentSessionId newSsId = new StudentSession.StudentSessionId();
                newSsId.setStudentId(studentId);
                newSsId.setSessionId(newSession.getId());

                StudentSession newSs = StudentSession.builder()
                        .id(newSsId)
                        .student(excusedSs.getStudent())
                        .session(newSession)
                        .attendanceStatus(AttendanceStatus.PLANNED)
                        .isMakeup(false)
                        .homeworkStatus(null)
                        .note("Buổi học được đổi lịch từ session " + oldSession.getId())
                        .build();
                studentSessionRepository.save(newSs);
                log.info("Created new PLANNED StudentSession for student {} in new session {} (no makeup session)",
                        studentId, newSession.getId());
            }
        }

        // Copy student sessions (chỉ những bản ghi PLANNED và chưa được xử lý)
        List<StudentSession> oldStudentSessions = studentSessionRepository.findAll().stream()
                .filter(ss -> ss.getSession().getId().equals(oldSession.getId()))
                .filter(ss -> (ss.getAttendanceStatus() == null ||
                        ss.getAttendanceStatus() == AttendanceStatus.PLANNED))
                .filter(ss -> !processedStudentIds.contains(ss.getStudent().getId()))
                .collect(Collectors.toList());
        for (StudentSession oldSs : oldStudentSessions) {
            StudentSession.StudentSessionId newSsId = new StudentSession.StudentSessionId();
            newSsId.setStudentId(oldSs.getStudent().getId());
            newSsId.setSessionId(newSession.getId());

            StudentSession newSs = StudentSession.builder()
                    .id(newSsId)
                    .student(oldSs.getStudent())
                    .session(newSession)
                    .attendanceStatus(AttendanceStatus.PLANNED)
                    .isMakeup(false)
                    .homeworkStatus(null)
                    .note(null)
                    .build();
            studentSessionRepository.save(newSs);
        }

        // Tạo session resource cho session mới
        SessionResource newSessionResource = SessionResource.builder()
                .id(new SessionResource.SessionResourceId(newSession.getId(), newResource.getId()))
                .session(newSession)
                .resource(newResource)
                .build();
        sessionResourceRepository.save(newSessionResource);

        // Hủy session cũ
        oldSession.setStatus(SessionStatus.CANCELLED);
        sessionRepository.save(oldSession);

        // Lưu lại session mới vào request (để tracking)
        request.setNewSession(newSession);

        log.info("Reschedule: oldSession={} cancelled, newSession={} created date={} slot={} resource={}",
                oldSession.getId(), newSession.getId(), newDate, newTimeSlotId, newResource.getId());
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

        // Gửi thông báo + email cho teacher
        try {
            sendNotificationToTeacherForRejection(request, reason.trim());
            sendEmailNotificationForRejection(request, reason.trim());
        } catch (Exception e) {
            log.error("Lỗi khi gửi notification cho teacher về request {}: {}", requestId, e.getMessage());
            // Không throw exception - notification failure không nên block việc reject
        }

        return mapToResponseDTO(request);
    }

    //Giáo viên dạy thay xác nhận đồng ý dạy thay
    @Transactional
    public TeacherRequestResponseDTO confirmReplacementRequest(Long requestId, Long userId, String note) {
        log.info("Confirm replacement request {} by teacher {}", requestId, userId);

        // Lấy request với đầy đủ relationships
        TeacherRequest request = teacherRequestRepository.findByIdWithTeacherAndSession(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_REQUEST_NOT_FOUND, "Request not found"));

        // Kiểm tra request phải là REPLACEMENT và status là WAITING_CONFIRM
        if (request.getRequestType() != TeacherRequestType.REPLACEMENT) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Request is not a REPLACEMENT request");
        }
        if (request.getStatus() != RequestStatus.WAITING_CONFIRM) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Request is not in WAITING_CONFIRM status");
        }

        // Kiểm tra quyền: teacher phải là replacement teacher
        Teacher teacher = teacherRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND, "Teacher not found"));

        if (request.getReplacementTeacher() == null || !request.getReplacementTeacher().getId().equals(teacher.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN, "You are not the replacement teacher for this request");
        }

        // Cập nhật status thành APPROVED
        request.setStatus(RequestStatus.APPROVED);
        if (note != null && !note.trim().isEmpty()) {
            request.setNote(note.trim());
        }

        // Cập nhật session: thay teacher cũ bằng replacement teacher
        Session session = request.getSession();
        if (session == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Request has no associated session");
        }
        // - Original teacher slot → ON_LEAVE
        // - Replacement teacher slot → SUBSTITUTED
        Teacher originalTeacher = request.getTeacher();
        Teacher replacementTeacher = request.getReplacementTeacher();

        // Original teacher slot → ON_LEAVE
        TeachingSlot.TeachingSlotId originalSlotId = new TeachingSlot.TeachingSlotId();
        originalSlotId.setSessionId(session.getId());
        originalSlotId.setTeacherId(originalTeacher.getId());
        TeachingSlot originalSlot = teachingSlotRepository.findById(originalSlotId).orElse(null);
        
        if (originalSlot == null) {
            log.warn("No TeachingSlot found for original teacher {} in session {}", originalTeacher.getId(), session.getId());
        } else {
            originalSlot.setStatus(TeachingSlotStatus.ON_LEAVE);
            teachingSlotRepository.save(originalSlot);
        }

        // Replacement teacher slot → SUBSTITUTED
        TeachingSlot.TeachingSlotId replacementSlotId = new TeachingSlot.TeachingSlotId();
        replacementSlotId.setSessionId(session.getId());
        replacementSlotId.setTeacherId(replacementTeacher.getId());
        TeachingSlot replacementSlot = teachingSlotRepository.findById(replacementSlotId).orElse(null);
        
        if (replacementSlot == null) {
            // Tạo mới nếu chưa có
            replacementSlot = new TeachingSlot();
            replacementSlot.setId(replacementSlotId);
            replacementSlot.setSession(session);
            replacementSlot.setTeacher(replacementTeacher);
            replacementSlot.setStatus(TeachingSlotStatus.SUBSTITUTED);
        } else {
            replacementSlot.setStatus(TeachingSlotStatus.SUBSTITUTED);
        }
        teachingSlotRepository.save(replacementSlot);

        request = teacherRequestRepository.save(request);
        log.info("Replacement request {} confirmed successfully by teacher {}. Session {} updated: teacher {} -> {}", 
                requestId, userId, session.getId(), originalTeacher.getId(), request.getReplacementTeacher().getId());

        // Gửi thông báo cho giáo vụ, teacher gốc và STUDENTS
        try {
            sendNotificationToAcademicStaffForReplacementConfirmation(request);
            sendNotificationToOriginalTeacherForReplacementConfirmation(request);
            // NEW: Gửi notification + email cho students về việc teacher thay đổi
            sendSessionCancelledNotificationToStudents(request);
        } catch (Exception e) {
            log.error("Lỗi khi gửi notification về replacement confirmation cho request {}: {}", requestId, e.getMessage());
            // Không throw exception - notification failure không nên block việc confirm
        }

        return mapToResponseDTO(request);
    }

    //Giáo viên dạy thay từ chối dạy thay
    @Transactional
    public TeacherRequestResponseDTO declineReplacementRequest(Long requestId, String reason, Long userId) {
        log.info("Decline replacement request {} by teacher {}", requestId, userId);

        // Lấy request với đầy đủ relationships
        TeacherRequest request = teacherRequestRepository.findByIdWithTeacherAndSession(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_REQUEST_NOT_FOUND, "Request not found"));

        // Kiểm tra request phải là REPLACEMENT và status là WAITING_CONFIRM
        if (request.getRequestType() != TeacherRequestType.REPLACEMENT) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Request is not a REPLACEMENT request");
        }
        if (request.getStatus() != RequestStatus.WAITING_CONFIRM) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Request is not in WAITING_CONFIRM status");
        }

        // Kiểm tra quyền: teacher phải là replacement teacher
        Teacher teacher = teacherRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND, "Teacher not found"));

        if (request.getReplacementTeacher() == null || !request.getReplacementTeacher().getId().equals(teacher.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN, "You are not the replacement teacher for this request");
        }

        // Validate lý do từ chối
        if (reason == null || reason.trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Rejection reason is required");
        }
        if (reason.trim().length() < MIN_REASON_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Rejection reason must be at least " + MIN_REASON_LENGTH + " characters");
        }

        // Khi replacement teacher từ chối, request trở lại PENDING để giáo vụ có thể xử lý lại
        // Lưu teacher ID và tên đã từ chối vào note để track
        Long declinedTeacherId = teacher.getId();
        UserAccount teacherAccount = teacher.getUserAccount();
        String declinedTeacherName = teacherAccount != null ? teacherAccount.getFullName() : "Giáo viên ID " + declinedTeacherId;
        String declinedNote = String.format("DECLINED_BY_TEACHER:%d:%s:%s", declinedTeacherId, declinedTeacherName, reason.trim());
        
        // Nếu đã có note về teacher từ chối trước đó, append thêm
        String existingNote = request.getNote();
        if (existingNote != null && existingNote.contains("DECLINED_BY_TEACHER")) {
            request.setNote(existingNote + "\n" + declinedNote);
        } else {
            request.setNote(declinedNote);
        }
        
        // Clear replacement teacher và set status về PENDING
        request.setStatus(RequestStatus.PENDING);
        request.setReplacementTeacher(null); // Xóa replacement teacher để giáo vụ có thể chọn lại

        request = teacherRequestRepository.save(request);
        log.info("Replacement request {} declined by teacher {}, returning to PENDING status", requestId, userId);

        return mapToResponseDTO(request);
    }

    //Gợi ý giáo viên dạy thay cho REPLACEMENT request (cho academic staff)
    //Loại trừ các teacher đã từ chối request này
    @Transactional(readOnly = true)
    public List<ReplacementCandidateDTO> suggestReplacementCandidatesForStaff(Long requestId, Long academicStaffUserId) {
        log.info("Suggesting replacement candidates for request {} by academic staff {}", requestId, academicStaffUserId);

        // Lấy request
        TeacherRequest request = teacherRequestRepository.findByIdWithTeacherAndSession(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_REQUEST_NOT_FOUND, "Request not found"));

        // Kiểm tra request phải là REPLACEMENT
        if (request.getRequestType() != TeacherRequestType.REPLACEMENT) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Request is not a REPLACEMENT request");
        }

        // Kiểm tra quyền branch
        Long teacherUserAccountId = request.getTeacher() != null ? request.getTeacher().getUserAccount().getId() : null;
        List<Long> academicBranches = getBranchIdsForUser(academicStaffUserId);
        List<Long> teacherBranches = getBranchIdsForUser(teacherUserAccountId);
        boolean allowed = teacherBranches.isEmpty()
                || academicBranches.isEmpty()
                || academicBranches.stream().anyMatch(teacherBranches::contains);
        if (!allowed) {
            throw new CustomException(ErrorCode.FORBIDDEN, "You don't have permission to view candidates for this request");
        }

        Session session = request.getSession();
        if (session == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Request has no session");
        }

        // Lấy subject từ session
        org.fyp.tmssep490be.entities.ClassEntity classEntity = session.getClassEntity();
        if (classEntity == null || classEntity.getSubject() == null) {
            log.warn("Session {} has no subject information", session.getId());
            return List.of();
        }

        org.fyp.tmssep490be.entities.Subject subject = classEntity.getSubject();

        // Lấy skills yêu cầu của session từ SubjectSession
        Set<org.fyp.tmssep490be.entities.enums.Skill> sessionRequiredSkills = getSessionRequiredSkills(session);

        // Lấy branch của giáo viên được tạo yêu cầu hộ
        List<Long> requestTeacherBranchIds = teacherBranches;
        
        // Lấy các giáo viên cùng branch với giáo viên được tạo yêu cầu hộ
        List<Teacher> branchTeachers;
        if (requestTeacherBranchIds.isEmpty()) {
            branchTeachers = List.of();
        } else {
            branchTeachers = teacherRepository.findByBranchIds(requestTeacherBranchIds);
        }
        
        // Lấy danh sách teacher IDs đã từ chối request này
        // Parse note để lấy các teacher ID đã từ chối
        Set<Long> declinedTeacherIds = new java.util.HashSet<>();
        String note = request.getNote();
        if (note != null && (note.contains("DECLINED_BY_TEACHER") || note.contains("DECLINED_BY_TEACHER_ID"))) {
            // Parse note để lấy các teacher ID đã từ chối
            String[] lines = note.split("\n");
            for (String line : lines) {
                if (line.startsWith("DECLINED_BY_TEACHER:")) {
                    try {
                        // Format mới: DECLINED_BY_TEACHER:{teacherId}:{teacherName}:{reason}
                        String[] parts = line.split(":", 4);
                        if (parts.length >= 2) {
                            Long teacherId = Long.parseLong(parts[1]);
                            declinedTeacherIds.add(teacherId);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse declined teacher ID from note: {}", line);
                    }
                } else if (line.startsWith("DECLINED_BY_TEACHER_ID:")) {
                    try {
                        // Format cũ: DECLINED_BY_TEACHER_ID:{teacherId}:{reason}
                        String[] parts = line.split(":", 3);
                        if (parts.length >= 2) {
                            Long teacherId = Long.parseLong(parts[1]);
                            declinedTeacherIds.add(teacherId);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse declined teacher ID from note: {}", line);
                    }
                }
            }
        }

        // Loại trừ các teacher đã từ chối và teacher hiện tại
        Set<Long> excludedTeacherIds = new java.util.HashSet<>(declinedTeacherIds);
        if (request.getTeacher() != null) {
            excludedTeacherIds.add(request.getTeacher().getId());
        }
        if (request.getReplacementTeacher() != null) {
            excludedTeacherIds.add(request.getReplacementTeacher().getId());
        }

        // Filter teachers có conflict về thời gian
        LocalDate sessionDate = session.getDate();
        TimeSlotTemplate sessionTimeSlot = session.getTimeSlotTemplate();

        // Lọc candidates từ giáo viên cùng branch
        List<Teacher> candidateTeachers = branchTeachers.stream()
                .filter(teacher -> !excludedTeacherIds.contains(teacher.getId()))
                .filter(teacher -> !hasTeacherTimeConflict(teacher.getId(), sessionDate, sessionTimeSlot, session.getId()))
                .collect(Collectors.toList());

        // Map sang DTO và sắp xếp
        final Set<org.fyp.tmssep490be.entities.enums.Skill> finalSessionRequiredSkills = sessionRequiredSkills;
        return candidateTeachers.stream()
                .map(teacher -> mapToReplacementCandidateDTO(teacher, subject, session))
                .filter(dto -> dto != null)
                .sorted((a, b) -> {
                    // 1. Ưu tiên teacher có skills phù hợp với skills yêu cầu của session
                    boolean aHasMatchingSkills = hasMatchingSkills(a, finalSessionRequiredSkills);
                    boolean bHasMatchingSkills = hasMatchingSkills(b, finalSessionRequiredSkills);
                    
                    if (aHasMatchingSkills && !bHasMatchingSkills) {
                        return -1;
                    }
                    if (!aHasMatchingSkills && bHasMatchingSkills) {
                        return 1;
                    }
                    
                    // 2. Sắp xếp theo số lượng skills
                    int aSkillCount = a.getSkills() != null ? a.getSkills().size() : 0;
                    int bSkillCount = b.getSkills() != null ? b.getSkills().size() : 0;
                    if (aSkillCount != bSkillCount) {
                        return Integer.compare(bSkillCount, aSkillCount);
                    }
                    
                    // 3. Cuối cùng sắp xếp theo tên
                    String nameA = a.getFullName() != null ? a.getFullName() : "";
                    String nameB = b.getFullName() != null ? b.getFullName() : "";
                    return nameA.compareToIgnoreCase(nameB);
                })
                .collect(Collectors.toList());
    }

    //Gợi ý giáo viên dạy thay cho REPLACEMENT request (cho academic staff - từ sessionId khi tạo request mới)
    @Transactional(readOnly = true)
    public List<ReplacementCandidateDTO> suggestReplacementCandidatesForStaffBySession(
            Long sessionId, Long teacherId, Long academicStaffUserId) {
        log.info("Suggesting replacement candidates for session {} and teacher {} by academic staff {}",
                sessionId, teacherId, academicStaffUserId);

        // Kiểm tra quyền: academic staff phải có branch trùng với session branch
        List<Long> academicBranchIds = getBranchIdsForUser(academicStaffUserId);
        if (academicBranchIds.isEmpty()) {
            throw new CustomException(ErrorCode.FORBIDDEN, "Academic staff has no branches assigned");
        }

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT, "Session not found"));

        // Kiểm tra session có branch không
        if (session.getClassEntity() == null || session.getClassEntity().getBranch() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Session branch is missing");
        }

        Long sessionBranchId = session.getClassEntity().getBranch().getId();
        boolean hasAccess = academicBranchIds.contains(sessionBranchId);
        if (!hasAccess) {
            throw new CustomException(ErrorCode.FORBIDDEN, "You don't have permission to view candidates for this session");
        }

        // Kiểm tra teacher có được assign vào session không
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND, "Teacher not found"));

        boolean isOwner = session.getTeachingSlots().stream()
                .anyMatch(slot -> slot.getTeacher() != null && slot.getTeacher().getId().equals(teacher.getId()));
        if (!isOwner) {
            throw new CustomException(ErrorCode.FORBIDDEN, "Teacher is not assigned to this session");
        }

        // Lấy subject từ session
        org.fyp.tmssep490be.entities.ClassEntity classEntity = session.getClassEntity();
        if (classEntity == null || classEntity.getSubject() == null) {
            log.warn("Session {} has no subject information", sessionId);
            return List.of();
        }

        org.fyp.tmssep490be.entities.Subject subject = classEntity.getSubject();

        // Lấy skills yêu cầu của session từ SubjectSession
        Set<org.fyp.tmssep490be.entities.enums.Skill> sessionRequiredSkills = getSessionRequiredSkills(session);

        // Lấy branch của giáo viên được tạo yêu cầu hộ
        List<Long> teacherBranchIds = userBranchesRepository.findBranchIdsByUserId(teacher.getUserAccount().getId());
        
        // Lấy các giáo viên cùng branch với giáo viên được tạo yêu cầu hộ (trừ teacher hiện tại)
        List<Teacher> branchTeachers;
        if (teacherBranchIds.isEmpty()) {
            branchTeachers = List.of();
        } else {
            branchTeachers = teacherRepository.findByBranchIds(teacherBranchIds);
        }

        // Loại trừ teacher hiện tại
        Set<Long> excludedTeacherIds = new java.util.HashSet<>();
        excludedTeacherIds.add(teacher.getId());

        // Filter teachers có conflict về thời gian
        LocalDate sessionDate = session.getDate();
        TimeSlotTemplate sessionTimeSlot = session.getTimeSlotTemplate();

        // Lọc candidates
        List<Teacher> candidateTeachers = branchTeachers.stream()
                .filter(t -> !excludedTeacherIds.contains(t.getId()))
                .filter(t -> !hasTeacherTimeConflict(t.getId(), sessionDate, sessionTimeSlot, session.getId()))
                .collect(Collectors.toList());

        // Map sang DTO và sắp xếp
        final Set<org.fyp.tmssep490be.entities.enums.Skill> finalSessionRequiredSkills = sessionRequiredSkills;
        return candidateTeachers.stream()
                .map(t -> mapToReplacementCandidateDTO(t, subject, session))
                .filter(dto -> dto != null)
                .sorted((a, b) -> {
                    // 1. Ưu tiên teacher có skills phù hợp với skills yêu cầu của session
                    boolean aHasMatchingSkills = hasMatchingSkills(a, finalSessionRequiredSkills);
                    boolean bHasMatchingSkills = hasMatchingSkills(b, finalSessionRequiredSkills);

                    if (aHasMatchingSkills && !bHasMatchingSkills) {
                        return -1;
                    }
                    if (!aHasMatchingSkills && bHasMatchingSkills) {
                        return 1;
                    }

                    // 2. Sắp xếp theo số lượng skills
                    int aSkillCount = a.getSkills() != null ? a.getSkills().size() : 0;
                    int bSkillCount = b.getSkills() != null ? b.getSkills().size() : 0;
                    if (aSkillCount != bSkillCount) {
                        return Integer.compare(bSkillCount, aSkillCount);
                    }

                    // 3. Cuối cùng sắp xếp theo tên
                    String nameA = a.getFullName() != null ? a.getFullName() : "";
                    String nameB = b.getFullName() != null ? b.getFullName() : "";
                    return nameA.compareToIgnoreCase(nameB);
                })
                .collect(Collectors.toList());
    }

    //Academic staff chọn lại replacement teacher khi request ở PENDING (sau khi bị từ chối)
    @Transactional
    public TeacherRequestResponseDTO updateReplacementTeacher(Long requestId, Long replacementTeacherId, Long academicStaffUserId) {
        log.info("Updating replacement teacher for request {} to teacher {} by academic staff {}",
                requestId, replacementTeacherId, academicStaffUserId);

        // Lấy request với đầy đủ relationships
        TeacherRequest request = teacherRequestRepository.findByIdWithTeacherAndSession(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_REQUEST_NOT_FOUND, "Request not found"));

        // Kiểm tra request phải là REPLACEMENT
        if (request.getRequestType() != TeacherRequestType.REPLACEMENT) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Request is not a REPLACEMENT request");
        }

        // Kiểm tra status phải là PENDING (sau khi replacement teacher từ chối)
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Request must be in PENDING status to update replacement teacher");
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
            throw new CustomException(ErrorCode.FORBIDDEN, "You don't have permission to update this request");
        }

        // Kiểm tra replacement teacher tồn tại
        Teacher replacementTeacher = teacherRepository.findById(replacementTeacherId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND, "Replacement teacher not found"));

        // Không được chọn chính giáo viên tạo request
        if (replacementTeacher.getId().equals(request.getTeacher().getId())) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Replacement teacher cannot be the same as the requesting teacher");
        }

        // Kiểm tra replacement teacher đã từ chối request này chưa
        String existingNote = request.getNote();
        if (existingNote != null && existingNote.contains("DECLINED_BY_TEACHER")) {
            String[] declinedParts = existingNote.split("\n");
            for (String part : declinedParts) {
                if (part.startsWith("DECLINED_BY_TEACHER:")) {
                    String[] tokens = part.split(":");
                    if (tokens.length >= 2) {
                        try {
                            Long declinedTeacherId = Long.parseLong(tokens[1]);
                            if (declinedTeacherId.equals(replacementTeacherId)) {
                                throw new CustomException(ErrorCode.INVALID_INPUT,
                                        "This teacher has already declined this request. Please select a different teacher.");
                            }
                        } catch (NumberFormatException e) {
                            log.warn("Failed to parse declined teacher ID from note: {}", part);
                        }
                    }
                }
            }
        }

        // Kiểm tra conflict thời gian cho replacement teacher
        Session session = request.getSession();
        if (session == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Request has no associated session");
        }

        // Kiểm tra replacement teacher có conflict không
        boolean hasConflict = hasTeacherTimeConflict(replacementTeacher.getId(), session.getDate(),
                session.getTimeSlotTemplate(), session.getId());
        if (hasConflict) {
            throw new CustomException(ErrorCode.INVALID_INPUT,
                    "Replacement teacher has a time conflict on this date and time slot");
        }

        // Cập nhật replacement teacher và chuyển status sang WAITING_CONFIRM
        request.setReplacementTeacher(replacementTeacher);
        request.setStatus(RequestStatus.WAITING_CONFIRM);

        request = teacherRequestRepository.save(request);
        log.info("Replacement teacher updated for request {} to teacher {} by academic staff {}. Status changed to WAITING_CONFIRM",
                requestId, replacementTeacherId, academicStaffUserId);

        return mapToResponseDTO(request);
    }

    // Helper: Gửi notification + email cho students khi session bị cancel (teacher replacement)
    @Async("emailTaskExecutor")
    private void sendSessionCancelledNotificationToStudents(TeacherRequest request) {
        try {
            Session session = request.getSession();
            if (session == null) {
                return;
            }

            // Lấy danh sách students enrolled trong session
            List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(session.getId());
            if (studentSessions.isEmpty()) {
                log.info("No students found in session {} - skip notification", session.getId());
                return;
            }

            // Prepare notification data
            String className = session.getClassEntity().getName();
            String subjectName = session.getClassEntity().getSubject().getName();
            String sessionDate = session.getDate().toString();
            String sessionTime = session.getTimeSlotTemplate() != null 
                ? String.format("%s - %s", 
                    session.getTimeSlotTemplate().getStartTime(), 
                    session.getTimeSlotTemplate().getEndTime())
                : "N/A";
            String originalTeacherName = request.getTeacher() != null && request.getTeacher().getUserAccount() != null
                ? request.getTeacher().getUserAccount().getFullName()
                : "Giáo viên";
            String replacementTeacherName = request.getReplacementTeacher() != null
                    && request.getReplacementTeacher().getUserAccount() != null
                ? request.getReplacementTeacher().getUserAccount().getFullName()
                : "giáo viên dạy thay";
            String reason = request.getRequestReason() != null ? request.getRequestReason() : "Giáo viên có việc đột xuất";

            // Internal notification title & message
            String notificationTitle = "Thay đổi giáo viên";
            String notificationMessage = String.format(
                "Giáo viên %s sẽ không dạy buổi %s (%s). Giáo viên dạy thay là %s.",
                originalTeacherName, sessionDate, className, replacementTeacherName
            );

            // Send bulk internal notifications
            List<Long> studentUserIds = studentSessions.stream()
                .map(ss -> ss.getStudent().getUserAccount().getId())
                .toList();

            notificationService.sendBulkNotifications(
                studentUserIds,
                NotificationType.NOTIFICATION,
                notificationTitle,
                notificationMessage
            );

            // Send individual emails
            for (StudentSession ss : studentSessions) {
                Student student = ss.getStudent();
                if (student.getUserAccount() != null && student.getUserAccount().getEmail() != null) {
                    emailService.sendSessionCancelledAsync(
                        student.getUserAccount().getEmail(),
                        student.getUserAccount().getFullName(),
                        className,
                        subjectName,
                        sessionDate,
                        sessionTime,
                        originalTeacherName,
                        replacementTeacherName,
                        reason
                    );
                }
            }

            log.info("Sent session cancelled notifications to {} students for session {}", studentSessions.size(), session.getId());
        } catch (Exception e) {
            log.error("Error sending session cancelled notifications for request {}: {}", request.getId(), e.getMessage());
            throw e;
        }
    }

    // Helper: Gửi notification + email cho students khi schedule thay đổi (RESCHEDULE/MODALITY_CHANGE)
    @Async("emailTaskExecutor")
    private void sendScheduleChangedNotificationToStudents(TeacherRequest request) {
        try {
            Session oldSession = request.getSession();
            Session newSession = request.getNewSession();
            
            if (oldSession == null) {
                return;
            }

            // Lấy danh sách students
            List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(oldSession.getId());
            if (studentSessions.isEmpty()) {
                log.info("No students found in session {} - skip schedule change notification", oldSession.getId());
                return;
            }

            // Prepare notification data
            String className = oldSession.getClassEntity().getName();
            String subjectName = oldSession.getClassEntity().getSubject().getName();
            
            // Old schedule info
            String oldDate = oldSession.getDate().toString();
            String oldTime = oldSession.getTimeSlotTemplate() != null
                ? String.format("%s - %s", 
                    oldSession.getTimeSlotTemplate().getStartTime(),
                    oldSession.getTimeSlotTemplate().getEndTime())
                : "N/A";
            String oldRoom = "N/A";
            String oldModality = "N/A";
            if (!oldSession.getSessionResources().isEmpty()) {
                Resource oldResource = oldSession.getSessionResources().iterator().next().getResource();
                oldRoom = oldResource.getName();
                // Modality không có trong Resource entity - skip
            }

            // New schedule info
            String newDate = oldDate; // default to old if not changed
            String newTime = oldTime;
            String newRoom = oldRoom;
            String newModality = oldModality;
            
            if (request.getRequestType() == TeacherRequestType.RESCHEDULE && newSession != null) {
                newDate = newSession.getDate().toString();
                newTime = newSession.getTimeSlotTemplate() != null
                    ? String.format("%s - %s",
                        newSession.getTimeSlotTemplate().getStartTime(),
                        newSession.getTimeSlotTemplate().getEndTime())
                    : oldTime;
                if (!newSession.getSessionResources().isEmpty()) {
                    Resource newResource = newSession.getSessionResources().iterator().next().getResource();
                    newRoom = newResource.getName();
                    // Modality không có trong Resource entity - skip
                }
            } else if (request.getRequestType() == TeacherRequestType.MODALITY_CHANGE && request.getNewResource() != null) {
                // MODALITY_CHANGE: only resource changes, date/time stay same
                newRoom = request.getNewResource().getName();
                // Modality không có trong Resource entity - skip
            }

            String reason = request.getRequestReason() != null ? request.getRequestReason() : "Thay đổi lịch học";

            // Internal notification
            String notificationTitle;
            String notificationMessage;
            if (request.getRequestType() == TeacherRequestType.MODALITY_CHANGE) {
                // Thông báo rõ thay đổi phòng/hình thức học
                notificationTitle = "Thay đổi hình thức/phòng học";
                notificationMessage = String.format(
                    "Lịch học lớp %s ngày %s %s đã thay đổi phòng/hình thức học. Từ %s sang %s.",
                    className, oldDate, oldTime, oldRoom, newRoom
                );
            } else {
                // RESCHEDULE: nhấn mạnh thay đổi ngày/giờ
                notificationTitle = "Thay đổi lịch học";
                notificationMessage = String.format(
                    "Lịch học lớp %s đã thay đổi. Từ %s %s thành %s %s.",
                    className, oldDate, oldTime, newDate, newTime
                );
            }

            // Send bulk internal notifications
            List<Long> studentUserIds = studentSessions.stream()
                .map(ss -> ss.getStudent().getUserAccount().getId())
                .toList();

            notificationService.sendBulkNotifications(
                studentUserIds,
                NotificationType.NOTIFICATION,
                notificationTitle,
                notificationMessage
            );

            // Send individual emails
            for (StudentSession ss : studentSessions) {
                Student student = ss.getStudent();
                if (student.getUserAccount() != null && student.getUserAccount().getEmail() != null) {
                    emailService.sendScheduleChangedAsync(
                        student.getUserAccount().getEmail(),
                        student.getUserAccount().getFullName(),
                        className,
                        subjectName,
                        oldDate,
                        oldTime,
                        oldRoom,
                        oldModality,
                        newDate,
                        newTime,
                        newRoom,
                        newModality,
                        reason
                    );
                }
            }

            log.info("Sent schedule changed notifications to {} students for session {}", studentSessions.size(), oldSession.getId());
        } catch (Exception e) {
            log.error("Error sending schedule changed notifications for request {}: {}", request.getId(), e.getMessage());
            throw e;
        }
    }

    // Lấy config cho teacher request
    public TeacherRequestConfigDTO getTeacherRequestConfig() {
        return TeacherRequestConfigDTO.builder()
                .requireResourceAtRescheduleCreate(REQUIRE_RESOURCE_FOR_RESCHEDULE)
                .requireResourceAtModalityChangeCreate(REQUIRE_RESOURCE_FOR_MODALITY_CHANGE)
                .reasonMinLength(MIN_REASON_LENGTH)
                .timeWindowDays(TIME_WINDOW_DAYS)
                .build();
    }
}

