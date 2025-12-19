package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.teacherregistration.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.NotificationType;
import org.fyp.tmssep490be.entities.enums.RegistrationStatus;
import org.fyp.tmssep490be.entities.enums.RegistrationWindowStatus;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final NotificationService notificationService;
    private final TeacherSkillRepository teacherSkillRepository;

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

        // Lấy các lớp có registration dates được set (includes PENDING_OPEN và OPEN)
        List<ClassEntity> availableClasses = classRepository.findAvailableForTeacherRegistration(
                branchIds, ApprovalStatus.APPROVED, ClassStatus.SCHEDULED);

        // Lấy teacher skills để matching
        List<TeacherSkill> teacherSkills = teacherSkillRepository.findByTeacherId(teacher.getId());
        Set<String> teacherSpecializations = teacherSkills.stream()
                .map(TeacherSkill::getSpecialization)
                .filter(s -> s != null && !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        Set<String> teacherLanguages = teacherSkills.stream()
                .map(TeacherSkill::getLanguage)
                .filter(l -> l != null && !l.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        OffsetDateTime now = OffsetDateTime.now();

        return availableClasses.stream()
                .map(c -> mapToAvailableClassDTO(c, teacher, teacherSpecializations, teacherLanguages, now))
                // Filter: only show classes that are PENDING_OPEN or OPEN
                .filter(dto -> dto.getRegistrationStatus() != RegistrationWindowStatus.CLOSED)
                // Filter: only show classes that match teacher's language AND specialization
                .filter(dto -> Boolean.TRUE.equals(dto.getIsMatch()))
                // Filter: hide classes with schedule conflicts
                .filter(dto -> !Boolean.TRUE.equals(dto.getHasScheduleConflict()))
                // Sort: OPEN first, then by close date
                .sorted(Comparator
                        .comparing(
                                (AvailableClassDTO dto) -> dto.getRegistrationStatus() != RegistrationWindowStatus.OPEN)
                        .thenComparing(AvailableClassDTO::getRegistrationCloseDate))
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

        List<TeacherClassRegistration> registrations = registrationRepository
                .findByTeacherIdOrderByRegisteredAtDesc(teacher.getId());

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

    // Kiểm tra xung đột lịch khi giáo viên đăng ký dạy lớp
    @Transactional(readOnly = true)
    public ScheduleConflictDTO checkScheduleConflict(Long classId, Long userId) {
        Teacher teacher = getTeacherByUserId(userId);
        getClassById(classId); // Validate class exists

        // Lấy sessions của lớp muốn đăng ký
        List<Session> targetSessions = sessionRepository.findByClassEntityIdOrderByDateAsc(classId);
        if (targetSessions.isEmpty()) {
            return ScheduleConflictDTO.builder()
                    .hasConflict(false)
                    .conflicts(List.of())
                    .build();
        }

        List<ScheduleConflictDTO.ConflictDetail> conflicts = new ArrayList<>();

        // 1. Check với các lớp mà teacher đang được assign
        LocalDate today = LocalDate.now();
        List<ClassEntity> assignedClasses = classRepository.findActiveClassesByAssignedTeacher(
                teacher.getId(), today);

        for (ClassEntity assignedClass : assignedClasses) {
            List<Session> assignedSessions = sessionRepository.findByClassEntityIdOrderByDateAsc(
                    assignedClass.getId());

            for (Session targetSession : targetSessions) {
                for (Session assignedSession : assignedSessions) {
                    if (isSessionConflict(targetSession, assignedSession)) {
                        conflicts.add(buildConflictDetail(assignedClass, assignedSession, "Đang dạy"));
                    }
                }
            }
        }

        // 2. Check với các lớp mà teacher đã đăng ký (PENDING) nhưng chưa được duyệt
        List<TeacherClassRegistration> pendingRegistrations = registrationRepository
                .findByTeacherIdAndStatusOrderByRegisteredAtDesc(teacher.getId(), RegistrationStatus.PENDING);

        for (TeacherClassRegistration registration : pendingRegistrations) {
            ClassEntity registeredClass = registration.getClassEntity();
            // Skip lớp đang check (tránh check với chính nó)
            if (registeredClass.getId().equals(classId))
                continue;

            List<Session> registeredSessions = sessionRepository.findByClassEntityIdOrderByDateAsc(
                    registeredClass.getId());

            for (Session targetSession : targetSessions) {
                for (Session registeredSession : registeredSessions) {
                    if (isSessionConflict(targetSession, registeredSession)) {
                        conflicts.add(buildConflictDetail(registeredClass, registeredSession, "Đã đăng ký"));
                    }
                }
            }
        }

        return ScheduleConflictDTO.builder()
                .hasConflict(!conflicts.isEmpty())
                .conflicts(conflicts)
                .build();
    }

    // Kiểm tra 2 session có xung đột không (cùng ngày + khung giờ overlap)
    private boolean isSessionConflict(Session session1, Session session2) {
        // Phải cùng ngày
        if (!session1.getDate().equals(session2.getDate())) {
            return false;
        }

        // Check time overlap
        TimeSlotTemplate slot1 = session1.getTimeSlotTemplate();
        TimeSlotTemplate slot2 = session2.getTimeSlotTemplate();

        if (slot1 == null || slot2 == null) {
            return false;
        }

        LocalTime start1 = slot1.getStartTime();
        LocalTime end1 = slot1.getEndTime();
        LocalTime start2 = slot2.getStartTime();
        LocalTime end2 = slot2.getEndTime();

        // Overlap if: start1 < end2 AND start2 < end1
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    private ScheduleConflictDTO.ConflictDetail buildConflictDetail(ClassEntity classEntity, Session session,
            String conflictType) {
        String dayOfWeek = getDayOfWeekVietnamese(session.getDate().getDayOfWeek().getValue());
        String timeSlot = "";
        if (session.getTimeSlotTemplate() != null) {
            TimeSlotTemplate slot = session.getTimeSlotTemplate();
            timeSlot = slot.getStartTime().toString() + " - " + slot.getEndTime().toString();
        }

        return ScheduleConflictDTO.ConflictDetail.builder()
                .conflictingClassId(classEntity.getId())
                .conflictingClassName(classEntity.getName())
                .conflictingClassCode(classEntity.getCode())
                .conflictDate(session.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .conflictDayOfWeek(dayOfWeek)
                .conflictTimeSlot(timeSlot)
                .conflictType(conflictType)
                .build();
    }

    private String getDayOfWeekVietnamese(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> "Thứ 2";
            case 2 -> "Thứ 3";
            case 3 -> "Thứ 4";
            case 4 -> "Thứ 5";
            case 5 -> "Thứ 6";
            case 6 -> "Thứ 7";
            case 7 -> "Chủ nhật";
            default -> "";
        };
    }

    // ==================== ACADEMIC AFFAIRS APIs ====================

    // AA mở đăng ký cho lớp
    @Transactional
    public void openRegistration(OpenRegistrationRequest request, Long userId) {
        ClassEntity classEntity = getClassById(request.getClassId());
        getUserById(userId); // Validate user exists

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

        // Validate close date must be at least 2 days before class start date
        if (classEntity.getStartDate() != null) {
            OffsetDateTime classStartDateTime = classEntity.getStartDate()
                    .atStartOfDay(java.time.ZoneId.systemDefault()).toOffsetDateTime();
            OffsetDateTime latestCloseDate = classStartDateTime.minusDays(2);
            if (request.getRegistrationCloseDate().isAfter(latestCloseDate)) {
                throw new CustomException(ErrorCode.INVALID_INPUT,
                        "Ngày đóng đăng ký phải trước ngày bắt đầu lớp ít nhất 2 ngày");
            }
        }

        classEntity.setRegistrationOpenDate(request.getRegistrationOpenDate());
        classEntity.setRegistrationCloseDate(request.getRegistrationCloseDate());
        classRepository.save(classEntity);

        log.info("AA {} opened registration for class {} from {} to {}",
                userId, classEntity.getId(), request.getRegistrationOpenDate(), request.getRegistrationCloseDate());

        // Send notification to all teachers in the class's branch
        sendRegistrationOpenedNotification(classEntity);
    }

    // Helper method to send notifications to qualified teachers when registration
    // opens
    private void sendRegistrationOpenedNotification(ClassEntity classEntity) {
        try {
            Long branchId = classEntity.getBranch().getId();
            String className = classEntity.getName();
            String classCode = classEntity.getCode();
            String subjectName = classEntity.getSubject().getName();

            // Get curriculum info for matching
            String curriculumCode = null;
            String curriculumLanguage = null;
            if (classEntity.getSubject() != null && classEntity.getSubject().getCurriculum() != null) {
                curriculumCode = classEntity.getSubject().getCurriculum().getCode();
                curriculumLanguage = classEntity.getSubject().getCurriculum().getLanguage();
            }

            // Get all teachers in the class's branch
            List<Teacher> teachers = teacherRepository.findByBranchIds(List.of(branchId));

            if (teachers.isEmpty()) {
                return;
            }

            // Get teacher IDs
            List<Long> teacherIds = teachers.stream().map(Teacher::getId).collect(Collectors.toList());

            // Get skill details including language
            List<Object[]> skillDetails = teacherSkillRepository.findSkillDetailsByTeacherIds(teacherIds);

            // Build teacher skills and languages map
            java.util.Map<Long, Set<String>> teacherLanguagesMap = new java.util.HashMap<>();
            java.util.Map<Long, Set<String>> teacherSpecializationsMap = new java.util.HashMap<>();

            for (Object[] row : skillDetails) {
                Long teacherId = (Long) row[0];
                String specialization = (String) row[2];
                String language = (String) row[4];

                if (language != null) {
                    teacherLanguagesMap
                            .computeIfAbsent(teacherId, k -> new java.util.HashSet<>())
                            .add(language.toLowerCase());
                }
                if (specialization != null) {
                    teacherSpecializationsMap
                            .computeIfAbsent(teacherId, k -> new java.util.HashSet<>())
                            .add(specialization.toLowerCase());
                }
            }

            // Get class sessions for conflict detection
            List<Session> targetSessions = sessionRepository.findByClassEntityId(classEntity.getId());
            log.info("Notification check: class {} has {} sessions", classCode, targetSessions.size());
            for (Session s : targetSessions) {
                log.info("  Target session: date={}, time={}-{}",
                        s.getDate(),
                        s.getTimeSlotTemplate().getStartTime(),
                        s.getTimeSlotTemplate().getEndTime());
            }

            final String finalCurriculumCode = curriculumCode;
            final String finalCurriculumLanguage = curriculumLanguage;

            // Filter teachers by: language match, specialization match, no schedule
            // conflict
            List<Long> qualifiedTeacherUserIds = teachers.stream()
                    .filter(teacher -> {
                        // Check language match
                        if (finalCurriculumLanguage != null) {
                            Set<String> langs = teacherLanguagesMap.getOrDefault(teacher.getId(), Set.of());
                            if (!langs.contains(finalCurriculumLanguage.toLowerCase())) {
                                return false;
                            }
                        }

                        // Check specialization match
                        if (finalCurriculumCode != null) {
                            Set<String> specs = teacherSpecializationsMap.getOrDefault(teacher.getId(), Set.of());
                            if (!specs.contains(finalCurriculumCode.toLowerCase())) {
                                return false;
                            }
                        }

                        // Check schedule conflict
                        log.info("Checking conflicts for teacher {} (ID={}), has {} assigned classes",
                                teacher.getEmployeeCode(), teacher.getId(), teacher.getAssignedClasses().size());
                        for (ClassEntity assignedClass : teacher.getAssignedClasses()) {
                            log.info("  Assigned class: {} (ID={}, status={})",
                                    assignedClass.getName(), assignedClass.getId(), assignedClass.getStatus());
                            if (assignedClass.getStatus() == ClassStatus.COMPLETED ||
                                    assignedClass.getStatus() == ClassStatus.CANCELLED) {
                                continue;
                            }

                            List<Session> assignedSessions = sessionRepository
                                    .findByClassEntityId(assignedClass.getId());
                            log.info("    Assigned class has {} sessions", assignedSessions.size());
                            for (Session targetSession : targetSessions) {
                                for (Session assignedSession : assignedSessions) {
                                    if (targetSession.getDate().equals(assignedSession.getDate())) {
                                        LocalTime targetStart = targetSession.getTimeSlotTemplate().getStartTime();
                                        LocalTime targetEnd = targetSession.getTimeSlotTemplate().getEndTime();
                                        LocalTime assignedStart = assignedSession.getTimeSlotTemplate().getStartTime();
                                        LocalTime assignedEnd = assignedSession.getTimeSlotTemplate().getEndTime();

                                        if (targetStart.isBefore(assignedEnd) && assignedStart.isBefore(targetEnd)) {
                                            return false; // Has conflict
                                        }
                                    }
                                }
                            }
                        }

                        return true; // Qualified
                    })
                    .map(t -> t.getUserAccount().getId())
                    .collect(Collectors.toList());

            if (qualifiedTeacherUserIds.isEmpty()) {
                log.info("No qualified teachers found for class {} notification", classCode);
                return;
            }

            // Format dates for notification - use Vietnam timezone
            java.time.ZoneId vietnamZone = java.time.ZoneId.of("Asia/Ho_Chi_Minh");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String openDate = classEntity.getRegistrationOpenDate().atZoneSameInstant(vietnamZone).format(formatter);
            String closeDate = classEntity.getRegistrationCloseDate().atZoneSameInstant(vietnamZone).format(formatter);

            // Check if registration is already open or pending
            OffsetDateTime now = OffsetDateTime.now();
            boolean isAlreadyOpen = !now.isBefore(classEntity.getRegistrationOpenDate());

            String title = "Mở đăng ký dạy lớp: " + className;
            String message;
            if (isAlreadyOpen) {
                message = String.format(
                        "Lớp %s (%s) - Môn %s đang mở đăng ký dạy. Hạn đăng ký: %s. " +
                                "Vào mục 'Đăng ký dạy' để đăng ký ngay!",
                        className, classCode, subjectName, closeDate);
            } else {
                message = String.format(
                        "Lớp %s (%s) - Môn %s sẽ mở đăng ký dạy từ %s đến %s. " +
                                "Vào mục 'Đăng ký dạy' để theo dõi!",
                        className, classCode, subjectName, openDate, closeDate);
            }

            notificationService.sendBulkNotifications(
                    qualifiedTeacherUserIds,
                    NotificationType.NOTIFICATION,
                    title,
                    message);

            log.info("Sent registration notification to {} qualified teachers for class {}",
                    qualifiedTeacherUserIds.size(), classCode);
        } catch (Exception e) {
            log.error("Failed to send registration notifications: {}", e.getMessage());
        }
    }

    // AA xem danh sách lớp cần review đăng ký
    @Transactional(readOnly = true)
    public List<ClassRegistrationSummaryDTO> getClassesNeedingReview(Long userId, Long requestedBranchId) {
        List<Long> branchIds = userBranchesRepository.findBranchIdsByUserId(userId);

        if (branchIds.isEmpty()) {
            return List.of();
        }

        // Validate and use requestedBranchId if provided
        if (requestedBranchId != null && !branchIds.contains(requestedBranchId)) {
            throw new CustomException(ErrorCode.BRANCH_ACCESS_DENIED,
                    "Access denied to branch ID: " + requestedBranchId);
        }

        Long branchId = requestedBranchId != null ? requestedBranchId : branchIds.get(0);

        // Lấy các lớp có đăng ký pending
        List<Long> pendingClassIds = registrationRepository.findClassIdsWithPendingRegistrationsByBranchId(branchId);

        // Lấy các lớp đã được gán giáo viên
        List<Long> assignedClassIds = registrationRepository.findClassIdsWithAssignedTeacherByBranchId(branchId);

        // Gộp và loại bỏ trùng lặp
        Set<Long> allClassIds = new HashSet<>();
        allClassIds.addAll(pendingClassIds);
        allClassIds.addAll(assignedClassIds);

        return allClassIds.stream()
                .map(this::getClassRegistrationSummary)
                .collect(Collectors.toList());
    }

    // AA xem danh sách lớp cần gán giáo viên (APPROVED, chưa có teacher)
    @Transactional(readOnly = true)
    public List<ClassNeedingTeacherDTO> getClassesNeedingTeacher(Long userId, Long requestedBranchId) {
        List<Long> branchIds = userBranchesRepository.findBranchIdsByUserId(userId);

        if (branchIds.isEmpty()) {
            return List.of();
        }

        // Validate and use requestedBranchId if provided
        if (requestedBranchId != null && !branchIds.contains(requestedBranchId)) {
            throw new CustomException(ErrorCode.BRANCH_ACCESS_DENIED,
                    "Access denied to branch ID: " + requestedBranchId);
        }

        List<Long> targetBranchIds = requestedBranchId != null ? List.of(requestedBranchId) : branchIds;

        // Lấy các lớp APPROVED + SCHEDULED chưa có teacher
        List<ClassEntity> classes = classRepository.findClassesNeedingTeacher(targetBranchIds);

        return classes.stream()
                .map(this::mapToClassNeedingTeacherDTO)
                .collect(Collectors.toList());
    }

    // AA xem chi tiết đăng ký của 1 lớp
    @Transactional(readOnly = true)
    public ClassRegistrationSummaryDTO getClassRegistrationSummary(Long classId) {
        ClassEntity classEntity = getClassById(classId);

        List<TeacherClassRegistration> registrations = registrationRepository
                .findPendingRegistrationsWithTeacherDetailsForClass(classId);

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
                .assignedTeacherId(
                        classEntity.getAssignedTeacher() != null ? classEntity.getAssignedTeacher().getId() : null)
                .assignedTeacherName(classEntity.getAssignedTeacher() != null
                        ? classEntity.getAssignedTeacher().getUserAccount().getFullName()
                        : null)
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
        List<TeacherClassRegistration> otherRegistrations = registrationRepository
                .findByClassEntityIdAndStatusOrderByRegisteredAtAsc(
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

        // Gửi thông báo cho giáo viên được chọn
        try {
            String subjectName = classEntity.getSubject() != null ? classEntity.getSubject().getName() : "N/A";
            String startDate = classEntity.getStartDate() != null
                    ? classEntity.getStartDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    : "N/A";

            notificationService.createNotification(
                    teacher.getUserAccount().getId(),
                    NotificationType.NOTIFICATION,
                    "Đăng ký dạy lớp đã được duyệt",
                    String.format(
                            "Chúc mừng! Đăng ký dạy lớp %s (%s) - Môn %s, bắt đầu từ %s đã được duyệt. " +
                                    "Vào mục 'Lịch dạy' để xem chi tiết!",
                            classEntity.getName(),
                            classEntity.getCode(),
                            subjectName,
                            startDate));
            log.info("Sent approval notification to teacher {}", teacher.getId());

            // Gửi thông báo cho các giáo viên bị từ chối
            for (TeacherClassRegistration other : otherRegistrations) {
                notificationService.createNotification(
                        other.getTeacher().getUserAccount().getId(),
                        NotificationType.NOTIFICATION,
                        "Đăng ký dạy lớp không được chọn",
                        String.format(
                                "Đăng ký dạy lớp %s (%s) của bạn không được chọn vì đã có giáo viên khác được gán. " +
                                        "Cảm ơn bạn đã quan tâm đến lớp học này!",
                                classEntity.getName(),
                                classEntity.getCode()));
            }
            log.info("Sent rejection notifications to {} teachers", otherRegistrations.size());
        } catch (Exception e) {
            log.error("Failed to send registration approval notifications: {}", e.getMessage());
        }

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

        // Validate registration deadline - AA chỉ được gán trực tiếp sau khi hết hạn
        // đăng ký
        // HOẶC khi lớp ở trạng thái khẩn cấp (gần/quá ngày bắt đầu, không còn thời gian
        // mở đăng ký bình thường)
        OffsetDateTime now = OffsetDateTime.now();
        boolean isEmergency = isEmergencyAssignAllowed(classEntity);

        if (!isEmergency) {
            // Normal flow: require registration to be opened and expired
            if (classEntity.getRegistrationCloseDate() == null) {
                throw new CustomException(ErrorCode.INVALID_INPUT,
                        "Lớp chưa được mở đăng ký. Vui lòng mở đăng ký trước khi gán trực tiếp giáo viên.");
            }
            if (now.isBefore(classEntity.getRegistrationCloseDate())) {
                throw new CustomException(ErrorCode.INVALID_INPUT,
                        "Chưa hết hạn đăng ký. Chỉ được gán trực tiếp giáo viên sau khi hết hạn đăng ký (" +
                                classEntity.getRegistrationCloseDate()
                                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                                + ").");
            }
        }
        // If isEmergency = true, skip registration check - allow direct assign for
        // urgent cases

        // Validate schedule conflict (check thời gian overlap)
        List<Session> targetSessions = sessionRepository.findByClassEntityId(classEntity.getId());

        for (ClassEntity assignedClass : teacher.getAssignedClasses()) {
            if (assignedClass.getStatus() == ClassStatus.COMPLETED ||
                    assignedClass.getStatus() == ClassStatus.CANCELLED) {
                continue;
            }

            List<Session> assignedSessions = sessionRepository.findByClassEntityId(assignedClass.getId());

            for (Session targetSession : targetSessions) {
                if (targetSession.getDate() == null || targetSession.getTimeSlotTemplate() == null) {
                    continue;
                }

                LocalTime targetStart = targetSession.getTimeSlotTemplate().getStartTime();
                LocalTime targetEnd = targetSession.getTimeSlotTemplate().getEndTime();

                for (Session assignedSession : assignedSessions) {
                    if (assignedSession.getDate() == null || assignedSession.getTimeSlotTemplate() == null) {
                        continue;
                    }

                    // Check same date
                    if (!targetSession.getDate().equals(assignedSession.getDate())) {
                        continue;
                    }

                    LocalTime assignedStart = assignedSession.getTimeSlotTemplate().getStartTime();
                    LocalTime assignedEnd = assignedSession.getTimeSlotTemplate().getEndTime();

                    // Check time overlap: (start1 < end2) AND (start2 < end1)
                    boolean hasOverlap = targetStart.isBefore(assignedEnd) && assignedStart.isBefore(targetEnd);

                    if (hasOverlap) {
                        throw new CustomException(ErrorCode.INVALID_INPUT,
                                String.format("Giáo viên có lịch trùng với lớp %s: Ngày %s - %s~%s overlap với %s~%s",
                                        assignedClass.getName(),
                                        targetSession.getDate(),
                                        targetStart, targetEnd,
                                        assignedStart, assignedEnd));
                    }
                }
            }
        }

        // Gán teacher vào class
        classEntity.setAssignedTeacher(teacher);
        classEntity.setTeacherAssignedAt(now);
        classEntity.setTeacherAssignedBy(assigner);
        classEntity.setDirectAssignReason(request.getReason());
        classRepository.save(classEntity);

        // Từ chối tất cả đăng ký pending nếu có
        List<TeacherClassRegistration> pendingRegistrations = registrationRepository
                .findByClassEntityIdAndStatusOrderByRegisteredAtAsc(
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

        // Gửi thông báo cho giáo viên được gán
        try {
            String subjectName = classEntity.getSubject() != null ? classEntity.getSubject().getName() : "N/A";
            String startDate = classEntity.getStartDate() != null
                    ? classEntity.getStartDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    : "N/A";

            notificationService.createNotification(
                    teacher.getUserAccount().getId(),
                    NotificationType.NOTIFICATION,
                    "Bạn đã được gán dạy lớp mới",
                    String.format(
                            "Bạn đã được gán dạy lớp %s (%s) - Môn %s, bắt đầu từ %s. " +
                                    "Lý do: %s. Vào mục 'Lịch dạy' để xem chi tiết!",
                            classEntity.getName(),
                            classEntity.getCode(),
                            subjectName,
                            startDate,
                            request.getReason()));
            log.info("Sent direct assignment notification to teacher {}", teacher.getId());
        } catch (Exception e) {
            log.error("Failed to send direct assignment notification: {}", e.getMessage());
        }

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

    /**
     * Check if emergency direct assign is allowed for a class.
     * Emergency assign is allowed when:
     * - Class start date is within 2 days or already passed
     * - This means normal registration flow is no longer possible
     * (because registrationCloseDate must be at least 2 days before startDate)
     */
    private boolean isEmergencyAssignAllowed(ClassEntity classEntity) {
        if (classEntity.getStartDate() == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        // Latest possible close date is startDate - 2 days
        LocalDate latestCloseDate = classEntity.getStartDate().minusDays(2);
        // If today >= latestCloseDate, it's too late for normal registration
        return !today.isBefore(latestCloseDate);
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

        // Kiểm tra schedule conflict với các lớp đang dạy (check thời gian overlap)
        List<Session> targetSessions = sessionRepository.findByClassEntityId(classEntity.getId());

        for (ClassEntity assignedClass : teacher.getAssignedClasses()) {
            if (assignedClass.getStatus() == ClassStatus.COMPLETED ||
                    assignedClass.getStatus() == ClassStatus.CANCELLED) {
                continue;
            }

            List<Session> assignedSessions = sessionRepository.findByClassEntityId(assignedClass.getId());

            for (Session targetSession : targetSessions) {
                if (targetSession.getDate() == null || targetSession.getTimeSlotTemplate() == null) {
                    continue;
                }

                LocalTime targetStart = targetSession.getTimeSlotTemplate().getStartTime();
                LocalTime targetEnd = targetSession.getTimeSlotTemplate().getEndTime();

                for (Session assignedSession : assignedSessions) {
                    if (assignedSession.getDate() == null || assignedSession.getTimeSlotTemplate() == null) {
                        continue;
                    }

                    // Check same date
                    if (!targetSession.getDate().equals(assignedSession.getDate())) {
                        continue;
                    }

                    LocalTime assignedStart = assignedSession.getTimeSlotTemplate().getStartTime();
                    LocalTime assignedEnd = assignedSession.getTimeSlotTemplate().getEndTime();

                    // Check time overlap: (start1 < end2) AND (start2 < end1)
                    boolean hasOverlap = targetStart.isBefore(assignedEnd) && assignedStart.isBefore(targetEnd);

                    if (hasOverlap) {
                        throw new CustomException(ErrorCode.INVALID_INPUT,
                                String.format("Lịch dạy trùng với lớp %s: Ngày %s - %s~%s overlap với %s~%s",
                                        assignedClass.getName(),
                                        targetSession.getDate(),
                                        targetStart, targetEnd,
                                        assignedStart, assignedEnd));
                    }
                }
            }
        }
    }

    private void createTeachingSlotsForClass(ClassEntity classEntity, Teacher teacher) {
        List<Session> sessions = sessionRepository.findByClassEntityId(classEntity.getId());

        for (Session session : sessions) {
            // Kiểm tra đã có teaching slot chưa
            boolean hasSlot = teachingSlotRepository.existsBySessionId(session.getId());
            if (!hasSlot) {
                // Create composite key explicitly
                TeachingSlot.TeachingSlotId slotId = new TeachingSlot.TeachingSlotId(
                        session.getId(),
                        teacher.getId());

                TeachingSlot slot = TeachingSlot.builder()
                        .id(slotId)
                        .session(session)
                        .teacher(teacher)
                        .status(org.fyp.tmssep490be.entities.enums.TeachingSlotStatus.SCHEDULED)
                        .build();
                teachingSlotRepository.save(slot);
            }
        }
    }

    // ==================== MAPPING METHODS ====================

    private AvailableClassDTO mapToAvailableClassDTO(ClassEntity c, Teacher teacher,
            Set<String> teacherSpecializations, Set<String> teacherLanguages, OffsetDateTime now) {
        Long teacherId = teacher.getId();
        int totalRegistrations = (int) registrationRepository.countByClassEntityIdAndStatus(
                c.getId(), RegistrationStatus.PENDING);
        boolean alreadyRegistered = registrationRepository.existsByTeacherIdAndClassEntityId(teacherId, c.getId());

        // Calculate registration status (real-time, not stored in DB)
        RegistrationWindowStatus registrationStatus = calculateRegistrationStatus(c, now);

        // Get curriculum info for matching
        // Use curriculum.code for matching (e.g. 'IELTS', 'TOEIC') as it matches
        // teacher skill specialization
        String curriculumCode = null;
        String curriculumLanguage = null;
        if (c.getSubject() != null && c.getSubject().getCurriculum() != null) {
            curriculumCode = c.getSubject().getCurriculum().getCode();
            curriculumLanguage = c.getSubject().getCurriculum().getLanguage();
        }

        // Calculate matching
        boolean isMatch = false;
        String matchReason = null;

        if (curriculumCode != null && curriculumLanguage != null) {
            boolean specializationMatch = teacherSpecializations.contains(curriculumCode.toLowerCase());
            boolean languageMatch = teacherLanguages.contains(curriculumLanguage.toLowerCase());

            if (specializationMatch && languageMatch) {
                isMatch = true;
                matchReason = "Phù hợp: " + curriculumCode + " - " + curriculumLanguage;
            } else if (specializationMatch) {
                isMatch = true;
                matchReason = "Phù hợp chuyên ngành: " + curriculumCode;
            } else if (languageMatch) {
                // Language-only match is weaker, but still show as partial match
                isMatch = false; // Only specialization match counts
                matchReason = null;
            }
        }

        // Get sessions for this class
        List<Session> sessions = sessionRepository.findByClassEntityId(c.getId());

        // Get time slot from first session (legacy - for backward compatibility)
        String timeSlotStart = null;
        String timeSlotEnd = null;
        if (!sessions.isEmpty() && sessions.get(0).getTimeSlotTemplate() != null) {
            TimeSlotTemplate slot = sessions.get(0).getTimeSlotTemplate();
            timeSlotStart = slot.getStartTime() != null ? slot.getStartTime().toString() : null;
            timeSlotEnd = slot.getEndTime() != null ? slot.getEndTime().toString() : null;
        }

        // Calculate time slots grouped by day of week
        Map<String, String> timeSlotsByDay = calculateTimeSlotsByDay(sessions);

        // Check schedule conflict with teacher's assigned classes (time overlap check)
        boolean hasConflict = false;
        StringBuilder conflictBuilder = new StringBuilder();

        for (ClassEntity assignedClass : teacher.getAssignedClasses()) {
            // Skip completed/cancelled classes
            if (assignedClass.getStatus() == ClassStatus.COMPLETED ||
                    assignedClass.getStatus() == ClassStatus.CANCELLED) {
                continue;
            }

            // Get sessions of assigned class
            List<Session> assignedSessions = sessionRepository.findByClassEntityId(assignedClass.getId());
            boolean foundConflictWithThisClass = false;

            for (Session targetSession : sessions) {
                if (targetSession.getDate() == null || targetSession.getTimeSlotTemplate() == null) {
                    continue;
                }

                LocalTime targetStart = targetSession.getTimeSlotTemplate().getStartTime();
                LocalTime targetEnd = targetSession.getTimeSlotTemplate().getEndTime();

                for (Session assignedSession : assignedSessions) {
                    if (assignedSession.getDate() == null || assignedSession.getTimeSlotTemplate() == null) {
                        continue;
                    }

                    // Check same date
                    if (!targetSession.getDate().equals(assignedSession.getDate())) {
                        continue;
                    }

                    LocalTime assignedStart = assignedSession.getTimeSlotTemplate().getStartTime();
                    LocalTime assignedEnd = assignedSession.getTimeSlotTemplate().getEndTime();

                    // Check time overlap: (start1 < end2) AND (start2 < end1)
                    boolean hasOverlap = targetStart.isBefore(assignedEnd) && assignedStart.isBefore(targetEnd);

                    if (hasOverlap) {
                        hasConflict = true;
                        foundConflictWithThisClass = true;
                        if (conflictBuilder.length() == 0) {
                            conflictBuilder.append("Trùng lịch với: ");
                        }
                        conflictBuilder.append(assignedClass.getName())
                                .append(" (").append(targetSession.getDate())
                                .append(" ").append(targetStart).append("-").append(targetEnd)
                                .append(" vs ").append(assignedStart).append("-").append(assignedEnd)
                                .append("); ");
                        break;
                    }
                }

                if (foundConflictWithThisClass) {
                    break; // Found conflict with this class, move to next class
                }
            }
        }

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
                .registrationStatus(registrationStatus)
                .isMatch(isMatch)
                .matchReason(matchReason)
                .curriculumName(curriculumCode)
                .curriculumLanguage(curriculumLanguage)
                .timeSlotStart(timeSlotStart)
                .timeSlotEnd(timeSlotEnd)
                .timeSlotsByDay(timeSlotsByDay)
                .hasScheduleConflict(hasConflict)
                .conflictDetails(hasConflict ? conflictBuilder.toString() : null)
                .build();
    }

    /**
     * Calculate time slots grouped by day of week.
     * Groups days that have the same time slot together.
     * 
     * Example output:
     * - If T2, T4 both have 07:00-08:30 and T6 has 08:00-10:30:
     * {"T2, T4": "07:00 - 08:30", "T6": "08:00 - 10:30"}
     * - If all days have same time slot:
     * {"T2, T4, T6": "07:00 - 08:30"}
     */
    private Map<String, String> calculateTimeSlotsByDay(List<Session> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return Map.of();
        }

        // Map: timeSlotKey ("07:00-08:30") -> Set of day numbers (1, 3, 5)
        Map<String, Set<Integer>> timeSlotToDays = new HashMap<>();
        // Map: timeSlotKey -> formatted time string ("07:00 - 08:30")
        Map<String, String> timeSlotFormatted = new HashMap<>();

        for (Session session : sessions) {
            if (session.getTimeSlotTemplate() == null || session.getDate() == null) {
                continue;
            }

            TimeSlotTemplate slot = session.getTimeSlotTemplate();
            if (slot.getStartTime() == null || slot.getEndTime() == null) {
                continue;
            }

            // Create a key for this time slot
            String timeKey = slot.getStartTime().toString() + "-" + slot.getEndTime().toString();
            String timeFormatted = slot.getStartTime().toString() + " - " + slot.getEndTime().toString();

            // Get day of week (1 = Monday, 7 = Sunday)
            int dayOfWeek = session.getDate().getDayOfWeek().getValue();

            timeSlotToDays.computeIfAbsent(timeKey, k -> new HashSet<>()).add(dayOfWeek);
            timeSlotFormatted.putIfAbsent(timeKey, timeFormatted);
        }

        if (timeSlotToDays.isEmpty()) {
            return Map.of();
        }

        // Now invert: group days by their time slot
        // Result: "T2, T4" -> "07:00 - 08:30", "T6" -> "08:00 - 10:30"
        Map<String, String> result = new LinkedHashMap<>();

        // Sort time slots by start time for consistent ordering
        List<String> sortedTimeKeys = new ArrayList<>(timeSlotToDays.keySet());
        sortedTimeKeys.sort(Comparator.naturalOrder());

        for (String timeKey : sortedTimeKeys) {
            Set<Integer> days = timeSlotToDays.get(timeKey);
            String dayLabel = formatDayLabels(days);
            String timeValue = timeSlotFormatted.get(timeKey);
            result.put(dayLabel, timeValue);
        }

        return result;
    }

    /**
     * Format a set of day numbers into Vietnamese day labels.
     * Days are sorted and joined with ", ".
     */
    private String formatDayLabels(Set<Integer> days) {
        return days.stream()
                .sorted()
                .map(this::getDayOfWeekVietnamese)
                .collect(Collectors.joining(", "));
    }

    /**
     * Calculate registration window status based on current time.
     * This is computed in real-time and NOT stored in the database.
     */
    private RegistrationWindowStatus calculateRegistrationStatus(ClassEntity c, OffsetDateTime now) {
        if (c.getRegistrationOpenDate() == null) {
            return RegistrationWindowStatus.NOT_OPENED;
        }

        if (now.isBefore(c.getRegistrationOpenDate())) {
            return RegistrationWindowStatus.PENDING_OPEN;
        }

        if (c.getRegistrationCloseDate() != null && now.isAfter(c.getRegistrationCloseDate())) {
            return RegistrationWindowStatus.CLOSED;
        }

        return RegistrationWindowStatus.OPEN;
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

        // Get time slot from first session
        String timeSlotStart = null;
        String timeSlotEnd = null;
        List<Session> sessions = sessionRepository.findByClassEntityIdOrderByDateAsc(c.getId());
        if (!sessions.isEmpty() && sessions.get(0).getTimeSlotTemplate() != null) {
            TimeSlotTemplate slot = sessions.get(0).getTimeSlotTemplate();
            timeSlotStart = slot.getStartTime().toString();
            timeSlotEnd = slot.getEndTime().toString();
        }

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
                .timeSlotStart(timeSlotStart)
                .timeSlotEnd(timeSlotEnd)
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

    private ClassNeedingTeacherDTO mapToClassNeedingTeacherDTO(ClassEntity c) {
        int pendingRegistrations = (int) registrationRepository.countByClassEntityIdAndStatus(
                c.getId(), RegistrationStatus.PENDING);

        // Convert Short[] to int[]
        int[] scheduleDaysArray = null;
        if (c.getScheduleDays() != null) {
            scheduleDaysArray = new int[c.getScheduleDays().length];
            for (int i = 0; i < c.getScheduleDays().length; i++) {
                scheduleDaysArray[i] = c.getScheduleDays()[i];
            }
        }

        return ClassNeedingTeacherDTO.builder()
                .classId(c.getId())
                .classCode(c.getCode())
                .className(c.getName())
                .subjectName(c.getSubject() != null ? c.getSubject().getName() : null)
                .branchName(c.getBranch() != null ? c.getBranch().getName() : null)
                .modality(c.getModality() != null ? c.getModality().name() : null)
                .startDate(c.getStartDate())
                .plannedEndDate(c.getPlannedEndDate())
                .scheduleDays(scheduleDaysArray)
                .maxCapacity(c.getMaxCapacity())
                .registrationOpenDate(c.getRegistrationOpenDate())
                .registrationCloseDate(c.getRegistrationCloseDate())
                .registrationOpened(c.getRegistrationOpenDate() != null)
                .pendingRegistrations(pendingRegistrations)
                .build();
    }

    // AA lấy danh sách giáo viên phù hợp để gán trực tiếp cho lớp
    @Transactional(readOnly = true)
    public List<QualifiedTeacherDTO> getQualifiedTeachersForClass(Long classId, Long userId) {
        // Validate class exists
        ClassEntity classEntity = getClassById(classId);

        // Get AA's accessible branches
        List<Long> branchIds = userBranchesRepository.findBranchIdsByUserId(userId);
        if (branchIds.isEmpty()) {
            return List.of();
        }

        // Get class branch ID
        Long classBranchId = classEntity.getBranch() != null ? classEntity.getBranch().getId() : null;
        if (classBranchId == null || !branchIds.contains(classBranchId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "Không có quyền truy cập lớp này");
        }

        // Get curriculum info for matching
        String curriculumCode = null;
        String curriculumLanguage = null;
        if (classEntity.getSubject() != null && classEntity.getSubject().getCurriculum() != null) {
            curriculumCode = classEntity.getSubject().getCurriculum().getCode(); // e.g., "IELTS"
            curriculumLanguage = classEntity.getSubject().getCurriculum().getLanguage(); // e.g., "English"
        }

        // Get all teachers in this branch
        List<Teacher> teachers = teacherRepository.findByBranchIds(List.of(classBranchId));

        if (teachers.isEmpty()) {
            return List.of();
        }

        // Get teacher IDs
        List<Long> teacherIds = teachers.stream().map(Teacher::getId).collect(Collectors.toList());

        // Get all skills for these teachers (now includes language)
        List<Object[]> skillDetails = teacherSkillRepository.findSkillDetailsByTeacherIds(teacherIds);

        // Group skills by teacher ID and track languages taught by each teacher
        java.util.Map<Long, List<QualifiedTeacherDTO.TeacherSkillInfo>> teacherSkillsMap = new java.util.HashMap<>();
        java.util.Map<Long, Set<String>> teacherLanguagesMap = new java.util.HashMap<>();

        for (Object[] row : skillDetails) {
            Long teacherId = (Long) row[0];
            String skill = row[1] != null ? row[1].toString() : null;
            String specialization = (String) row[2];
            Short level = row[3] != null ? ((Number) row[3]).shortValue() : null;
            String language = (String) row[4];

            teacherSkillsMap
                    .computeIfAbsent(teacherId, k -> new java.util.ArrayList<>())
                    .add(QualifiedTeacherDTO.TeacherSkillInfo.builder()
                            .skill(skill)
                            .specialization(specialization)
                            .level(level)
                            .language(language)
                            .build());

            // Track languages
            if (language != null) {
                teacherLanguagesMap
                        .computeIfAbsent(teacherId, k -> new java.util.HashSet<>())
                        .add(language.toLowerCase());
            }
        }

        final String finalCurriculumCode = curriculumCode;
        final String finalCurriculumLanguage = curriculumLanguage;

        // Get target class sessions for schedule conflict detection
        List<Session> targetClassSessions = sessionRepository.findByClassEntityId(classId);

        // Map to DTOs with match scoring, workload, and schedule conflict
        // FILTER: Only include teachers who teach the curriculum language
        List<QualifiedTeacherDTO> result = teachers.stream()
                .filter(teacher -> {
                    // If no curriculum language defined, show all teachers
                    if (finalCurriculumLanguage == null)
                        return true;

                    // Only show teachers who have skills in matching language
                    Set<String> teacherLanguages = teacherLanguagesMap.getOrDefault(teacher.getId(), Set.of());
                    return teacherLanguages.contains(finalCurriculumLanguage.toLowerCase());
                })
                .map(teacher -> {
                    UserAccount user = teacher.getUserAccount();
                    List<QualifiedTeacherDTO.TeacherSkillInfo> skills = teacherSkillsMap.getOrDefault(teacher.getId(),
                            List.of());

                    // Calculate match score: requires BOTH specialization AND language match for
                    // "Phù hợp"
                    boolean hasMatchingSpecialization = skills.stream()
                            .anyMatch(s -> s.getSpecialization() != null &&
                                    s.getSpecialization().equalsIgnoreCase(finalCurriculumCode) &&
                                    s.getLanguage() != null &&
                                    s.getLanguage().equalsIgnoreCase(finalCurriculumLanguage));

                    int matchScore = 0;
                    String matchReason = null;

                    if (finalCurriculumCode != null && hasMatchingSpecialization) {
                        matchScore = 100;
                        matchReason = "Phù hợp chuyên môn " + finalCurriculumCode;
                    } else if (!skills.isEmpty()) {
                        // Has language match but different specialization
                        matchScore = 50;
                        matchReason = "Khác chuyên môn";
                    } else {
                        matchScore = 0;
                        matchReason = "Chưa có thông tin kỹ năng";
                    }

                    // Calculate workload (SCHEDULED + ONGOING classes)
                    int workload = teacherRepository.countAssignedClassesByTeacherIdAndStatuses(
                            teacher.getId(),
                            List.of(ClassStatus.SCHEDULED, ClassStatus.ONGOING));

                    // Check schedule conflict with assigned classes (time overlap check)
                    boolean hasConflict = false;
                    StringBuilder conflictBuilder = new StringBuilder();

                    // Debug logging
                    Set<ClassEntity> assignedClasses = teacher.getAssignedClasses();
                    log.info("Teacher {} (ID={}) has {} assigned classes, workload count={}",
                            teacher.getEmployeeCode(), teacher.getId(),
                            assignedClasses != null ? assignedClasses.size() : "null", workload);

                    for (ClassEntity assignedClass : assignedClasses) {
                        log.info("  - Assigned class: {} (ID={}, status={})",
                                assignedClass.getName(), assignedClass.getId(), assignedClass.getStatus());
                        // Skip completed/cancelled classes
                        if (assignedClass.getStatus() == ClassStatus.COMPLETED ||
                                assignedClass.getStatus() == ClassStatus.CANCELLED) {
                            continue;
                        }

                        // Get sessions of assigned class
                        List<Session> assignedSessions = sessionRepository.findByClassEntityId(assignedClass.getId());
                        boolean foundConflictWithThisClass = false;

                        for (Session targetSession : targetClassSessions) {
                            if (targetSession.getDate() == null || targetSession.getTimeSlotTemplate() == null) {
                                continue;
                            }

                            LocalTime targetStart = targetSession.getTimeSlotTemplate().getStartTime();
                            LocalTime targetEnd = targetSession.getTimeSlotTemplate().getEndTime();

                            for (Session assignedSession : assignedSessions) {
                                if (assignedSession.getDate() == null
                                        || assignedSession.getTimeSlotTemplate() == null) {
                                    continue;
                                }

                                // Check same date
                                if (!targetSession.getDate().equals(assignedSession.getDate())) {
                                    continue;
                                }

                                LocalTime assignedStart = assignedSession.getTimeSlotTemplate().getStartTime();
                                LocalTime assignedEnd = assignedSession.getTimeSlotTemplate().getEndTime();

                                // Check time overlap: (start1 < end2) AND (start2 < end1)
                                boolean hasOverlap = targetStart.isBefore(assignedEnd)
                                        && assignedStart.isBefore(targetEnd);

                                if (hasOverlap) {
                                    hasConflict = true;
                                    foundConflictWithThisClass = true;
                                    if (conflictBuilder.length() == 0) {
                                        conflictBuilder.append("Trùng lịch với: ");
                                    }
                                    conflictBuilder.append(assignedClass.getName()).append("; ");
                                    break;
                                }
                            }

                            if (foundConflictWithThisClass) {
                                break;
                            }
                        }
                    }

                    return QualifiedTeacherDTO.builder()
                            .teacherId(teacher.getId())
                            .fullName(user.getFullName())
                            .email(user.getEmail())
                            .phone(user.getPhone())
                            .avatarUrl(user.getAvatarUrl())
                            .employeeCode(teacher.getEmployeeCode())
                            .contractType(teacher.getContractType())
                            .skills(skills)
                            .totalSkills(skills.size())
                            .isMatch(matchScore >= 100)
                            .matchReason(matchReason)
                            .matchScore(matchScore)
                            .hasScheduleConflict(hasConflict)
                            .conflictDetails(hasConflict ? conflictBuilder.toString() : null)
                            .currentWorkload(workload)
                            .build();
                })
                // FILTER: Only show teachers who are matched AND have no schedule conflict
                .filter(dto -> dto.getIsMatch() && !dto.getHasScheduleConflict())
                // Sort by: workload ascending, then name
                .sorted(Comparator
                        .comparing(QualifiedTeacherDTO::getCurrentWorkload)
                        .thenComparing(QualifiedTeacherDTO::getFullName))
                .collect(Collectors.toList());

        return result;
    }
}
