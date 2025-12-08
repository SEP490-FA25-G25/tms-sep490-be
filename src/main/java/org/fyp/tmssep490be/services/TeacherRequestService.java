package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestListDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestResponseDTO;
import org.fyp.tmssep490be.entities.Resource;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.Teacher;
import org.fyp.tmssep490be.entities.TeacherRequest;
import org.fyp.tmssep490be.entities.TimeSlotTemplate;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.enums.Modality;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.entities.enums.TeacherRequestType;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.TeacherRepository;
import org.fyp.tmssep490be.repositories.TeacherRequestRepository;
import org.fyp.tmssep490be.repositories.UserBranchesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherRequestService {

    private final TeacherRequestRepository teacherRequestRepository;
    private final TeacherRepository teacherRepository;
    private final UserBranchesRepository userBranchesRepository;

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
}

