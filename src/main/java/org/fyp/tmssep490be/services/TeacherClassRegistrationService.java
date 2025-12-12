package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.teacherregistration.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.RegistrationStatus;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherClassRegistrationService {

    private final TeacherClassRegistrationRepository registrationRepository;
    private final ClassRepository classRepository;
    private final TeacherRepository teacherRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserBranchesRepository userBranchesRepository;
    private final TeachingSlotRepository teachingSlotRepository;
    private final SessionRepository sessionRepository;

    // ==================== TEACHER APIs ====================

    // Giáo viên xem danh sách lớp có thể đăng ký
    @Transactional(readOnly = true)
    public List<AvailableClassDTO> getAvailableClasses(Long userId) {
        Teacher teacher = getTeacherByUserId(userId);
        
        // Lấy các branch của teacher
        List<Long> branchIds = userBranchesRepository.findBranchIdsByUserId(userId);

        if (branchIds.isEmpty()) {
            return List.of();
        }

        OffsetDateTime now = OffsetDateTime.now();

        // Lấy các lớp đang mở đăng ký
        List<ClassEntity> availableClasses = classRepository.findAvailableForTeacherRegistration(
                branchIds, now, ApprovalStatus.APPROVED, ClassStatus.SCHEDULED);

        return availableClasses.stream()
                .map(c -> mapToAvailableClassDTO(c, teacher.getId()))
                .collect(Collectors.toList());
    }

    // Giáo viên đăng ký dạy lớp
    @Transactional
    public TeacherRegistrationResponse registerForClass(TeacherRegistrationRequest request, Long userId) {
        Teacher teacher = getTeacherByUserId(userId);
        ClassEntity classEntity = getClassById(request.getClassId());

        // Validate
        validateRegistration(classEntity, teacher, userId);

        // Tạo đăng ký mới
        TeacherClassRegistration registration = TeacherClassRegistration.builder()
                .teacher(teacher)
                .classEntity(classEntity)
                .status(RegistrationStatus.PENDING)
                .note(request.getNote())
                .registeredAt(OffsetDateTime.now())
                .build();

        registration = registrationRepository.save(registration);
        log.info("Teacher {} registered for class {}", teacher.getId(), classEntity.getId());

        return mapToResponse(registration);
    }

    // Giáo viên xem danh sách đăng ký của mình
    @Transactional(readOnly = true)
    public List<MyRegistrationDTO> getMyRegistrations(Long userId) {
        Teacher teacher = getTeacherByUserId(userId);
        
        List<TeacherClassRegistration> registrations = 
                registrationRepository.findByTeacherIdOrderByRegisteredAtDesc(teacher.getId());

        OffsetDateTime now = OffsetDateTime.now();
        return registrations.stream()
                .map(r -> mapToMyRegistrationDTO(r, now))
                .collect(Collectors.toList());
    }

    // Giáo viên hủy đăng ký
    @Transactional
    public void cancelRegistration(Long registrationId, Long userId) {
        Teacher teacher = getTeacherByUserId(userId);
        
        TeacherClassRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT, "Không tìm thấy đăng ký"));

        // Kiểm tra quyền
        if (!registration.getTeacher().getId().equals(teacher.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN, "Bạn không có quyền hủy đăng ký này");
        }

        // Kiểm tra status
        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Chỉ có thể hủy đăng ký đang chờ duyệt");
        }

        // Kiểm tra còn trong thời gian hủy không
        ClassEntity classEntity = registration.getClassEntity();
        if (classEntity.getRegistrationCloseDate() != null 
                && OffsetDateTime.now().isAfter(classEntity.getRegistrationCloseDate())) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Đã hết hạn đăng ký, không thể hủy");
        }

        registration.setStatus(RegistrationStatus.CANCELLED);
        registration.setCancelledAt(OffsetDateTime.now());
        registrationRepository.save(registration);

        log.info("Teacher {} cancelled registration {} for class {}", 
                teacher.getId(), registrationId, classEntity.getId());
    }

    // ==================== ACADEMIC AFFAIRS APIs ====================

    // AA mở đăng ký cho lớp
    @Transactional
    public void openRegistration(OpenRegistrationRequest request, Long userId) {
        ClassEntity classEntity = getClassById(request.getClassId());
        UserAccount user = getUserById(userId);

        // Validate quyền truy cập branch
        validateBranchAccess(userId, classEntity.getBranch().getId());

        // Validate class status
        if (classEntity.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Lớp chưa được duyệt");
        }
        if (classEntity.getAssignedTeacher() != null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Lớp đã có giáo viên được gán");
        }

        // Validate dates
        OffsetDateTime now = OffsetDateTime.now();
        if (request.getRegistrationOpenDate().isBefore(now)) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Ngày mở đăng ký phải sau thời điểm hiện tại");
        }
        if (request.getRegistrationCloseDate().isBefore(request.getRegistrationOpenDate())) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Ngày đóng đăng ký phải sau ngày mở");
        }

        classEntity.setRegistrationOpenDate(request.getRegistrationOpenDate());
        classEntity.setRegistrationCloseDate(request.getRegistrationCloseDate());
        classRepository.save(classEntity);

        log.info("AA {} opened registration for class {} from {} to {}", 
                userId, classEntity.getId(), request.getRegistrationOpenDate(), request.getRegistrationCloseDate());
    }

    // AA xem danh sách lớp cần review đăng ký
    @Transactional(readOnly = true)
    public List<ClassRegistrationSummaryDTO> getClassesNeedingReview(Long userId) {
        List<Long> branchIds = userBranchesRepository.findBranchIdsByUserId(userId);

        if (branchIds.isEmpty()) {
            return List.of();
        }

        // Lấy các lớp có đăng ký pending
        List<Long> classIds = registrationRepository.findClassIdsWithPendingRegistrationsByBranchId(branchIds.get(0));
        
        return classIds.stream()
                .map(this::getClassRegistrationSummary)
                .collect(Collectors.toList());
    }

    // AA xem chi tiết đăng ký của 1 lớp
    @Transactional(readOnly = true)
    public ClassRegistrationSummaryDTO getClassRegistrationSummary(Long classId) {
        ClassEntity classEntity = getClassById(classId);
        
        List<TeacherClassRegistration> registrations = 
                registrationRepository.findPendingRegistrationsWithTeacherDetailsForClass(classId);

        List<RegistrationDetailDTO> details = registrations.stream()
                .map(this::mapToRegistrationDetailDTO)
                .collect(Collectors.toList());

        return ClassRegistrationSummaryDTO.builder()
                .classId(classEntity.getId())
                .classCode(classEntity.getCode())
                .className(classEntity.getName())
                .subjectName(classEntity.getSubject() != null ? classEntity.getSubject().getName() : null)
                .modality(classEntity.getModality() != null ? classEntity.getModality().name() : null)
                .startDate(classEntity.getStartDate())
                .scheduleDays(classEntity.getScheduleDays())
                .registrationCloseDate(classEntity.getRegistrationCloseDate())
                .pendingCount(details.size())
                .registrations(details)
                .assignedTeacherId(classEntity.getAssignedTeacher() != null ? classEntity.getAssignedTeacher().getId() : null)
                .assignedTeacherName(classEntity.getAssignedTeacher() != null ? 
                        classEntity.getAssignedTeacher().getUserAccount().getFullName() : null)
                .build();
    }

    // AA duyệt chọn giáo viên
    @Transactional
    public void approveRegistration(ApproveRegistrationRequest request, Long userId) {
        UserAccount reviewer = getUserById(userId);
        
        TeacherClassRegistration registration = registrationRepository.findById(request.getRegistrationId())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT, "Không tìm thấy đăng ký"));

        ClassEntity classEntity = registration.getClassEntity();
        
        // Validate quyền
        validateBranchAccess(userId, classEntity.getBranch().getId());

        // Validate status
        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Đăng ký không ở trạng thái chờ duyệt");
        }
        if (classEntity.getAssignedTeacher() != null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Lớp đã có giáo viên được gán");
        }

        Teacher teacher = registration.getTeacher();
        OffsetDateTime now = OffsetDateTime.now();

        // Cập nhật registration được chọn
        registration.setStatus(RegistrationStatus.APPROVED);
        registration.setReviewedAt(now);
        registration.setReviewedBy(reviewer);
        registrationRepository.save(registration);

        // Gán teacher vào class
        classEntity.setAssignedTeacher(teacher);
        classEntity.setTeacherAssignedAt(now);
        classEntity.setTeacherAssignedBy(reviewer);
        classRepository.save(classEntity);

        // Từ chối các đăng ký khác
        List<TeacherClassRegistration> otherRegistrations = 
                registrationRepository.findByClassEntityIdAndStatusOrderByRegisteredAtAsc(
                        classEntity.getId(), RegistrationStatus.PENDING);
        
        for (TeacherClassRegistration other : otherRegistrations) {
            other.setStatus(RegistrationStatus.REJECTED);
            other.setReviewedAt(now);
            other.setReviewedBy(reviewer);
            other.setRejectionReason("Đã chọn giáo viên khác");
        }
        registrationRepository.saveAll(otherRegistrations);

        // Tạo teaching slots cho tất cả sessions
        createTeachingSlotsForClass(classEntity, teacher);

        log.info("AA {} approved registration {} for class {}, teacher {}", 
                userId, registration.getId(), classEntity.getId(), teacher.getId());
    }

    // AA gán trực tiếp giáo viên (không qua đăng ký)
    @Transactional
    public void directAssignTeacher(DirectAssignRequest request, Long userId) {
        UserAccount assigner = getUserById(userId);
        ClassEntity classEntity = getClassById(request.getClassId());
        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND));

        // Validate quyền
        validateBranchAccess(userId, classEntity.getBranch().getId());

        // Validate class
        if (classEntity.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Lớp chưa được duyệt");
        }
        if (classEntity.getAssignedTeacher() != null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Lớp đã có giáo viên được gán");
        }

        OffsetDateTime now = OffsetDateTime.now();

        // Gán teacher vào class
        classEntity.setAssignedTeacher(teacher);
        classEntity.setTeacherAssignedAt(now);
        classEntity.setTeacherAssignedBy(assigner);
        classEntity.setDirectAssignReason(request.getReason());
        classRepository.save(classEntity);

        // Từ chối tất cả đăng ký pending nếu có
        List<TeacherClassRegistration> pendingRegistrations = 
                registrationRepository.findByClassEntityIdAndStatusOrderByRegisteredAtAsc(
                        classEntity.getId(), RegistrationStatus.PENDING);
        
        for (TeacherClassRegistration reg : pendingRegistrations) {
            reg.setStatus(RegistrationStatus.REJECTED);
            reg.setReviewedAt(now);
            reg.setReviewedBy(assigner);
            reg.setRejectionReason("AA đã gán trực tiếp giáo viên khác");
        }
        registrationRepository.saveAll(pendingRegistrations);

        // Tạo teaching slots
        createTeachingSlotsForClass(classEntity, teacher);

        log.info("AA {} directly assigned teacher {} to class {} with reason: {}", 
                userId, teacher.getId(), classEntity.getId(), request.getReason());
    }

    // ==================== HELPER METHODS ====================

    private Teacher getTeacherByUserId(Long userId) {
        return teacherRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND, 
                        "Không tìm thấy hồ sơ giáo viên cho tài khoản này"));
    }

    private ClassEntity getClassById(Long classId) {
        return classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));
    }

    private UserAccount getUserById(Long userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateBranchAccess(Long userId, Long branchId) {
        boolean hasAccess = userBranchesRepository.existsByUserAccountIdAndBranchId(userId, branchId);

        if (!hasAccess) {
            throw new CustomException(ErrorCode.BRANCH_ACCESS_DENIED);
        }
    }

    private void validateRegistration(ClassEntity classEntity, Teacher teacher, Long userId) {
        // Kiểm tra lớp đã được duyệt
        if (classEntity.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Lớp chưa được duyệt");
        }

        // Kiểm tra lớp đã có teacher chưa
        if (classEntity.getAssignedTeacher() != null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Lớp đã có giáo viên được gán");
        }

        // Kiểm tra đang trong thời gian đăng ký
        OffsetDateTime now = OffsetDateTime.now();
        if (classEntity.getRegistrationOpenDate() == null || classEntity.getRegistrationCloseDate() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Lớp chưa mở đăng ký");
        }
        if (now.isBefore(classEntity.getRegistrationOpenDate())) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Chưa đến thời gian đăng ký");
        }
        if (now.isAfter(classEntity.getRegistrationCloseDate())) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Đã hết thời gian đăng ký");
        }

        // Kiểm tra teacher thuộc branch của class
        validateBranchAccess(userId, classEntity.getBranch().getId());

        // Kiểm tra đã đăng ký chưa
        if (registrationRepository.existsByTeacherIdAndClassEntityId(teacher.getId(), classEntity.getId())) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Bạn đã đăng ký lớp này rồi");
        }
    }

    private void createTeachingSlotsForClass(ClassEntity classEntity, Teacher teacher) {
        List<Session> sessions = sessionRepository.findByClassEntityId(classEntity.getId());
        
        for (Session session : sessions) {
            // Kiểm tra đã có teaching slot chưa
            boolean hasSlot = teachingSlotRepository.existsBySessionId(session.getId());
            if (!hasSlot) {
                TeachingSlot slot = TeachingSlot.builder()
                        .session(session)
                        .teacher(teacher)
                        .status(org.fyp.tmssep490be.entities.enums.TeachingSlotStatus.SCHEDULED)
                        .build();
                teachingSlotRepository.save(slot);
            }
        }
    }

    // ==================== MAPPING METHODS ====================

    private AvailableClassDTO mapToAvailableClassDTO(ClassEntity c, Long teacherId) {
        int totalRegistrations = (int) registrationRepository.countByClassEntityIdAndStatus(
                c.getId(), RegistrationStatus.PENDING);
        boolean alreadyRegistered = registrationRepository.existsByTeacherIdAndClassEntityId(teacherId, c.getId());

        return AvailableClassDTO.builder()
                .classId(c.getId())
                .classCode(c.getCode())
                .className(c.getName())
                .subjectName(c.getSubject() != null ? c.getSubject().getName() : null)
                .branchName(c.getBranch() != null ? c.getBranch().getName() : null)
                .modality(c.getModality() != null ? c.getModality().name() : null)
                .startDate(c.getStartDate())
                .plannedEndDate(c.getPlannedEndDate())
                .scheduleDays(c.getScheduleDays())
                .maxCapacity(c.getMaxCapacity())
                .registrationOpenDate(c.getRegistrationOpenDate())
                .registrationCloseDate(c.getRegistrationCloseDate())
                .totalRegistrations(totalRegistrations)
                .alreadyRegistered(alreadyRegistered)
                .build();
    }

    private TeacherRegistrationResponse mapToResponse(TeacherClassRegistration reg) {
        ClassEntity c = reg.getClassEntity();
        return TeacherRegistrationResponse.builder()
                .id(reg.getId())
                .classId(c.getId())
                .classCode(c.getCode())
                .className(c.getName())
                .status(reg.getStatus().name())
                .note(reg.getNote())
                .registeredAt(reg.getRegisteredAt())
                .build();
    }

    private MyRegistrationDTO mapToMyRegistrationDTO(TeacherClassRegistration reg, OffsetDateTime now) {
        ClassEntity c = reg.getClassEntity();
        boolean canCancel = reg.getStatus() == RegistrationStatus.PENDING
                && c.getRegistrationCloseDate() != null
                && now.isBefore(c.getRegistrationCloseDate());

        return MyRegistrationDTO.builder()
                .id(reg.getId())
                .classId(c.getId())
                .classCode(c.getCode())
                .className(c.getName())
                .subjectName(c.getSubject() != null ? c.getSubject().getName() : null)
                .branchName(c.getBranch() != null ? c.getBranch().getName() : null)
                .modality(c.getModality() != null ? c.getModality().name() : null)
                .startDate(c.getStartDate())
                .plannedEndDate(c.getPlannedEndDate())
                .scheduleDays(c.getScheduleDays())
                .status(reg.getStatus().name())
                .note(reg.getNote())
                .registeredAt(reg.getRegisteredAt())
                .registrationCloseDate(c.getRegistrationCloseDate())
                .rejectionReason(reg.getRejectionReason())
                .canCancel(canCancel)
                .build();
    }

    private RegistrationDetailDTO mapToRegistrationDetailDTO(TeacherClassRegistration reg) {
        Teacher t = reg.getTeacher();
        UserAccount ua = t.getUserAccount();

        // Đếm số lớp đang dạy
        int currentClassCount = t.getAssignedClasses() != null ? t.getAssignedClasses().size() : 0;

        // Map skills
        List<TeacherSkillDTO> skills = t.getTeacherSkills().stream()
                .map(s -> TeacherSkillDTO.builder()
                        .skill(s.getId() != null && s.getId().getSkill() != null ? s.getId().getSkill().name() : null)
                        .specialization(s.getSpecialization())
                        .language(s.getLanguage())
                        .level(s.getLevel() != null ? s.getLevel().doubleValue() : null)
                        .build())
                .collect(Collectors.toList());

        return RegistrationDetailDTO.builder()
                .registrationId(reg.getId())
                .teacherId(t.getId())
                .teacherName(ua.getFullName())
                .teacherEmail(ua.getEmail())
                .employeeCode(t.getEmployeeCode())
                .contractType(t.getContractType())
                .note(reg.getNote())
                .registeredAt(reg.getRegisteredAt())
                .status(reg.getStatus().name())
                .currentClassCount(currentClassCount)
                .skills(skills)
                .build();
    }
}
