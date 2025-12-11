package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.classcreation.CreateClassRequest;
import org.fyp.tmssep490be.dtos.classcreation.CreateClassResponse;
import org.fyp.tmssep490be.dtos.classcreation.SessionListResponse;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.TeachingSlotStatus;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassCreationService {

    private final ClassRepository classRepository;
    private final BranchRepository branchRepository;
    private final SubjectRepository subjectRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserBranchesRepository userBranchesRepository;
    private final SessionRepository sessionRepository;
    private final SessionResourceRepository sessionResourceRepository;
    private final TeachingSlotRepository teachingSlotRepository;
    private final ClassCodeGeneratorService classCodeGeneratorService;
    private final SessionGenerationService sessionGenerationService;

    // Step 1: Tạo lớp và sinh sessions tự động
    @Transactional
    public CreateClassResponse createClass(CreateClassRequest request, Long userId) {
        log.info("Tạo lớp mới cho user {}", userId);

        // Validate đầu vào
        validateRequest(request);

        // Lấy entities
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new CustomException(ErrorCode.BRANCH_NOT_FOUND));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new CustomException(ErrorCode.SUBJECT_NOT_FOUND));

        UserAccount createdBy = userAccountRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Validate business rules
        validateBusinessRules(request, branch, subject, userId);

        // Sinh mã lớp tự động nếu không có
        String classCode = request.getCode();
        if (classCode == null || classCode.isBlank()) {
            classCode = classCodeGeneratorService.generateClassCode(
                    branch.getId(),
                    branch.getCode(),
                    subject.getCode(),
                    request.getStartDate());
            log.info("Đã sinh mã lớp: {}", classCode);
        }

        OffsetDateTime now = OffsetDateTime.now();

        // Tạo entity
        ClassEntity classEntity = ClassEntity.builder()
                .branch(branch)
                .subject(subject)
                .code(classCode)
                .name(request.getName())
                .modality(request.getModality())
                .startDate(request.getStartDate())
                .scheduleDays(request.getScheduleDays().toArray(new Short[0]))
                .maxCapacity(request.getMaxCapacity())
                .status(ClassStatus.DRAFT)
                .approvalStatus(null) // Chỉ set PENDING khi submit
                .createdBy(createdBy)
                .createdAt(now)
                .updatedAt(now)
                .build();

        classEntity = classRepository.save(classEntity);
        log.info("Đã tạo lớp ID: {}", classEntity.getId());

        // Sinh sessions tự động
        List<Session> sessions = sessionGenerationService.generateSessionsForClass(classEntity, subject);
        List<Session> savedSessions = sessionRepository.saveAll(sessions);

        // Tính ngày kết thúc
        LocalDate endDate = sessionGenerationService.calculateEndDate(savedSessions);
        classEntity.setPlannedEndDate(endDate);
        classEntity = classRepository.save(classEntity);

        log.info("Đã sinh {} sessions cho lớp {}", savedSessions.size(), classEntity.getCode());

        // Trả về response
        return buildResponse(classEntity, subject, savedSessions, endDate);
    }

    // Validate đầu vào cơ bản
    private void validateRequest(CreateClassRequest request) {
        if (request == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // Check lịch học trùng
        if (request.getScheduleDays() != null) {
            long uniqueDays = request.getScheduleDays().stream().distinct().count();
            if (uniqueDays != request.getScheduleDays().size()) {
                throw new CustomException(ErrorCode.INVALID_SCHEDULE_DAYS);
            }
        }

        // Check ngày bắt đầu phải nằm trong lịch học
        if (request.getStartDate() != null && request.getScheduleDays() != null) {
            DayOfWeek dayOfWeek = request.getStartDate().getDayOfWeek();
            short dow = (short) (dayOfWeek.getValue() % 7); // 0=Sunday, 1=Monday,...
            if (!request.getScheduleDays().contains(dow)) {
                throw new CustomException(ErrorCode.START_DATE_NOT_IN_SCHEDULE_DAYS);
            }
        }
    }

    // Validate business rules
    private void validateBusinessRules(CreateClassRequest request, Branch branch, Subject subject, Long userId) {
        // Kiểm tra quyền truy cập branch
        List<Long> userBranchIds = userBranchesRepository.findBranchIdsByUserId(userId);
        if (!userBranchIds.contains(branch.getId())) {
            throw new CustomException(ErrorCode.BRANCH_ACCESS_DENIED);
        }

        // Kiểm tra subject đã được phê duyệt
        if (subject.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new CustomException(ErrorCode.SUBJECT_NOT_APPROVED);
        }

        // Kiểm tra mã lớp trùng
        if (request.getCode() != null && !request.getCode().isBlank()) {
            if (classRepository.findByBranchIdAndCode(branch.getId(), request.getCode()).isPresent()) {
                throw new CustomException(ErrorCode.CLASS_CODE_DUPLICATE);
            }
        }

        // Kiểm tra ngày bắt đầu lớp phải sau ngày hiệu lực của môn học
        if (subject.getEffectiveDate() != null && request.getStartDate() != null) {
            if (!request.getStartDate().isAfter(subject.getEffectiveDate())) {
                throw new CustomException(ErrorCode.START_DATE_BEFORE_EFFECTIVE_DATE);
            }
        }

        // Kiểm tra số ngày học không vượt quá số buổi học của môn học
        if (subject.getNumberOfSessions() != null && request.getScheduleDays() != null) {
            if (request.getScheduleDays().size() > subject.getNumberOfSessions()) {
                throw new CustomException(ErrorCode.SCHEDULE_DAYS_EXCEEDS_SESSIONS);
            }
        }
    }

    // Build response
    private CreateClassResponse buildResponse(ClassEntity classEntity, Subject subject,
            List<Session> sessions, LocalDate endDate) {
        CreateClassResponse.SessionGenerationSummary sessionSummary = CreateClassResponse.SessionGenerationSummary
                .builder()
                .sessionsGenerated(sessions.size())
                .totalSessionsInSubject(sessions.size())
                .subjectCode(subject.getCode())
                .subjectName(subject.getName())
                .startDate(classEntity.getStartDate())
                .endDate(endDate)
                .scheduleDays(classEntity.getScheduleDays())
                .build();

        return CreateClassResponse.builder()
                .classId(classEntity.getId())
                .code(classEntity.getCode())
                .name(classEntity.getName())
                .status(classEntity.getStatus())
                .approvalStatus(classEntity.getApprovalStatus())
                .createdAt(classEntity.getCreatedAt())
                .sessionSummary(sessionSummary)
                .build();
    }

    // Cập nhật lớp (chỉ cho DRAFT hoặc REJECTED)
    @Transactional
    public CreateClassResponse updateClass(Long classId, CreateClassRequest request, Long userId) {
        log.info("Cập nhật lớp {} bởi user {}", classId, userId);

        ClassEntity existingClass = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        // Chỉ cho phép update DRAFT hoặc REJECTED
        if (existingClass.getStatus() != ClassStatus.DRAFT) {
            throw new CustomException(ErrorCode.INVALID_CLASS_STATUS);
        }

        // Validate đầu vào
        validateRequest(request);

        // Lấy entities
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new CustomException(ErrorCode.BRANCH_NOT_FOUND));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new CustomException(ErrorCode.SUBJECT_NOT_FOUND));

        // Kiểm tra quyền truy cập
        List<Long> userBranchIds = userBranchesRepository.findBranchIdsByUserId(userId);
        if (!userBranchIds.contains(branch.getId())) {
            throw new CustomException(ErrorCode.BRANCH_ACCESS_DENIED);
        }

        // Kiểm tra subject đã được phê duyệt
        if (subject.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new CustomException(ErrorCode.SUBJECT_NOT_APPROVED);
        }

        // Kiểm tra tên lớp trùng (trừ lớp hiện tại)
        if (classRepository.existsByBranchIdAndNameIgnoreCaseAndIdNot(
                branch.getId(), request.getName(), classId)) {
            throw new CustomException(ErrorCode.CLASS_NAME_DUPLICATE);
        }

        boolean subjectChanged = !existingClass.getSubject().getId().equals(request.getSubjectId());
        boolean scheduleDaysChanged = !Arrays.equals(
                existingClass.getScheduleDays(),
                request.getScheduleDays().toArray(new Short[0]));
        boolean startDateChanged = !existingClass.getStartDate().equals(request.getStartDate());

        // Nếu subject, schedule hoặc startDate thay đổi thì cần xóa sessions cũ và sinh
        // lại
        if (subjectChanged || scheduleDaysChanged || startDateChanged) {
            log.info("Subject/Schedule/StartDate thay đổi, xóa sessions cũ và sinh lại");
            sessionRepository.deleteByClassEntityId(classId);
        }

        // Sinh mã lớp mới nếu branch hoặc subject thay đổi
        String classCode = existingClass.getCode();
        boolean branchChanged = !existingClass.getBranch().getId().equals(request.getBranchId());
        if (branchChanged || subjectChanged) {
            classCode = classCodeGeneratorService.generateClassCode(
                    branch.getId(),
                    branch.getCode(),
                    subject.getCode(),
                    request.getStartDate());
            log.info("Đã sinh mã lớp mới: {}", classCode);
        }

        // Cập nhật entity
        existingClass.setBranch(branch);
        existingClass.setSubject(subject);
        existingClass.setCode(classCode);
        existingClass.setName(request.getName());
        existingClass.setModality(request.getModality());
        existingClass.setStartDate(request.getStartDate());
        existingClass.setScheduleDays(request.getScheduleDays().toArray(new Short[0]));
        existingClass.setMaxCapacity(request.getMaxCapacity());
        existingClass.setUpdatedAt(OffsetDateTime.now());

        existingClass = classRepository.save(existingClass);

        // Sinh sessions mới nếu cần
        List<Session> sessions;
        if (subjectChanged || scheduleDaysChanged || startDateChanged) {
            sessions = sessionGenerationService.generateSessionsForClass(existingClass, subject);
            sessions = sessionRepository.saveAll(sessions);
        } else {
            sessions = sessionRepository.findByClassEntityIdOrderByDateAsc(classId);
        }

        // Cập nhật ngày kết thúc
        LocalDate endDate = sessionGenerationService.calculateEndDate(sessions);
        existingClass.setPlannedEndDate(endDate);
        existingClass = classRepository.save(existingClass);

        log.info("Đã cập nhật lớp {}", classId);

        return buildResponse(existingClass, subject, sessions, endDate);
    }

    // Preview mã lớp
    @Transactional(readOnly = true)
    public String previewClassCode(Long branchId, Long subjectId, LocalDate startDate) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new CustomException(ErrorCode.BRANCH_NOT_FOUND));

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new CustomException(ErrorCode.SUBJECT_NOT_FOUND));

        return classCodeGeneratorService.previewClassCode(
                branchId,
                branch.getCode(),
                subject.getCode(),
                startDate);
    }

    // Kiểm tra tên lớp trùng
    @Transactional(readOnly = true)
    public boolean checkClassNameExists(Long branchId, String name, Long excludeId) {
        if (excludeId != null) {
            return classRepository.existsByBranchIdAndNameIgnoreCaseAndIdNot(branchId, name, excludeId);
        }
        return classRepository.existsByBranchIdAndNameIgnoreCase(branchId, name);
    }

    // Lấy thông tin lớp để edit
    @Transactional(readOnly = true)
    public CreateClassResponse getClassForEdit(Long classId, Long userId) {
        log.info("Lấy lớp {} để edit bởi user {}", classId, userId);

        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        // Kiểm tra quyền truy cập
        List<Long> userBranchIds = userBranchesRepository.findBranchIdsByUserId(userId);
        if (!userBranchIds.contains(classEntity.getBranch().getId())) {
            throw new CustomException(ErrorCode.BRANCH_ACCESS_DENIED);
        }

        List<Session> sessions = sessionRepository.findByClassEntityIdOrderByDateAsc(classId);
        LocalDate endDate = sessions.isEmpty() ? null : sessions.get(sessions.size() - 1).getDate();

        return buildResponseWithEditInfo(classEntity, sessions, endDate);
    }

    // Build response với thông tin edit
    private CreateClassResponse buildResponseWithEditInfo(ClassEntity classEntity,
            List<Session> sessions, LocalDate endDate) {
        Subject subject = classEntity.getSubject();

        CreateClassResponse.SessionGenerationSummary sessionSummary = CreateClassResponse.SessionGenerationSummary
                .builder()
                .sessionsGenerated(sessions.size())
                .totalSessionsInSubject(sessions.size())
                .subjectCode(subject.getCode())
                .subjectName(subject.getName())
                .startDate(classEntity.getStartDate())
                .endDate(endDate)
                .scheduleDays(classEntity.getScheduleDays())
                .build();

        return CreateClassResponse.builder()
                .classId(classEntity.getId())
                .code(classEntity.getCode())
                .name(classEntity.getName())
                .branchId(classEntity.getBranch().getId())
                .branchName(classEntity.getBranch().getName())
                .subjectId(subject.getId())
                .subjectName(subject.getName())
                .modality(classEntity.getModality())
                .startDate(classEntity.getStartDate())
                .plannedEndDate(classEntity.getPlannedEndDate())
                .scheduleDays(Arrays.asList(classEntity.getScheduleDays()))
                .maxCapacity(classEntity.getMaxCapacity())
                .status(classEntity.getStatus())
                .approvalStatus(classEntity.getApprovalStatus())
                .createdAt(classEntity.getCreatedAt())
                .sessionSummary(sessionSummary)
                .build();
    }

    // Step 2: Lấy danh sách sessions cho review
    @Transactional(readOnly = true)
    public SessionListResponse listSessions(Long classId, Long userId) {
        log.info("Lấy sessions cho lớp {} bởi user {}", classId, userId);

        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        // Kiểm tra quyền truy cập
        if (userId != null) {
            List<Long> userBranchIds = userBranchesRepository.findBranchIdsByUserId(userId);
            if (!userBranchIds.contains(classEntity.getBranch().getId())) {
                throw new CustomException(ErrorCode.BRANCH_ACCESS_DENIED);
            }
        }

        List<Session> sessions = sessionRepository.findByClassEntityIdOrderByDateAsc(classId);

        if (sessions.isEmpty()) {
            return SessionListResponse.builder()
                    .classId(classId)
                    .classCode(classEntity.getCode())
                    .totalSessions(0)
                    .sessions(List.of())
                    .groupedByWeek(List.of())
                    .warnings(List.of())
                    .build();
        }

        LocalDate startDate = sessions.get(0).getDate();
        LocalDate endDate = sessions.get(sessions.size() - 1).getDate();

        // Build session DTOs
        List<SessionListResponse.SessionDTO> sessionDTOs = new ArrayList<>();
        int seq = 1;

        for (Session session : sessions) {
            boolean hasTimeSlot = session.getTimeSlotTemplate() != null;
            boolean hasResource = sessionResourceRepository.existsBySessionId(session.getId());
            boolean hasTeacher = teachingSlotRepository.existsBySessionId(session.getId());

            SessionListResponse.TimeSlotInfoDTO timeSlotInfo = null;
            Long timeSlotTemplateId = null;
            if (hasTimeSlot) {
                TimeSlotTemplate ts = session.getTimeSlotTemplate();
                timeSlotTemplateId = ts.getId();
                if (ts.getStartTime() != null && ts.getEndTime() != null) {
                    timeSlotInfo = SessionListResponse.TimeSlotInfoDTO.builder()
                            .startTime(ts.getStartTime().toString())
                            .endTime(ts.getEndTime().toString())
                            .displayName(ts.getStartTime() + " - " + ts.getEndTime())
                            .build();
                }
            }

            // Lấy resource
            String resourceName = null;
            Long resourceId = null;
            if (hasResource) {
                List<SessionResource> resources = sessionResourceRepository.findBySessionId(session.getId());
                if (!resources.isEmpty() && resources.get(0).getResource() != null) {
                    resourceName = resources.get(0).getResource().getName();
                    resourceId = resources.get(0).getResource().getId();
                }
            }

            // Lấy teachers
            List<SessionListResponse.TeacherInfoDTO> teacherInfos = new ArrayList<>();
            String teacherName = null;
            if (hasTeacher) {
                List<TeachingSlot> slots = teachingSlotRepository.findBySessionIdAndStatus(
                        session.getId(), TeachingSlotStatus.SCHEDULED);
                teacherInfos = slots.stream()
                        .filter(slot -> slot.getTeacher() != null)
                        .map(slot -> SessionListResponse.TeacherInfoDTO.builder()
                                .teacherId(slot.getTeacher().getId())
                                .fullName(slot.getTeacher().getUserAccount().getFullName())
                                .employeeCode(slot.getTeacher().getEmployeeCode())
                                .build())
                        .collect(Collectors.toList());
                if (!teacherInfos.isEmpty()) {
                    teacherName = teacherInfos.stream()
                            .map(SessionListResponse.TeacherInfoDTO::getFullName)
                            .collect(Collectors.joining(", "));
                }
            }

            String dayOfWeekName = session.getDate().getDayOfWeek()
                    .getDisplayName(TextStyle.FULL, Locale.forLanguageTag("vi"));
            Short dayOfWeekNumber = (short) session.getDate().getDayOfWeek().getValue();

            String subjectSessionName = session.getSubjectSession() != null
                    ? session.getSubjectSession().getTopic()
                    : null;

            sessionDTOs.add(SessionListResponse.SessionDTO.builder()
                    .sessionId(session.getId())
                    .sequenceNumber(seq++)
                    .date(session.getDate())
                    .dayOfWeek(dayOfWeekName)
                    .dayOfWeekNumber(dayOfWeekNumber)
                    .subjectSessionName(subjectSessionName)
                    .status(session.getStatus().name())
                    .hasTimeSlot(hasTimeSlot)
                    .hasResource(hasResource)
                    .hasTeacher(hasTeacher)
                    .timeSlotTemplateId(timeSlotTemplateId)
                    .resourceId(resourceId)
                    .timeSlotInfo(timeSlotInfo)
                    .resourceName(resourceName)
                    .teacherName(teacherName)
                    .teachers(teacherInfos)
                    .build());
        }

        // Group by week
        List<SessionListResponse.WeekGroupDTO> weekGroups = groupSessionsByWeek(sessionDTOs, startDate);

        return SessionListResponse.builder()
                .classId(classId)
                .classCode(classEntity.getCode())
                .totalSessions(sessions.size())
                .dateRange(SessionListResponse.DateRangeDTO.builder()
                        .startDate(startDate)
                        .endDate(endDate)
                        .build())
                .sessions(sessionDTOs)
                .groupedByWeek(weekGroups)
                .warnings(List.of())
                .build();
    }

    // Group sessions theo tuần
    private List<SessionListResponse.WeekGroupDTO> groupSessionsByWeek(
            List<SessionListResponse.SessionDTO> sessions, LocalDate startDate) {
        Map<Integer, List<SessionListResponse.SessionDTO>> weekMap = new LinkedHashMap<>();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());

        for (SessionListResponse.SessionDTO session : sessions) {
            int weekNum = session.getDate().get(weekFields.weekOfWeekBasedYear());
            weekMap.computeIfAbsent(weekNum, k -> new ArrayList<>()).add(session);
        }

        List<SessionListResponse.WeekGroupDTO> result = new ArrayList<>();
        int weekIndex = 1;
        for (Map.Entry<Integer, List<SessionListResponse.SessionDTO>> entry : weekMap.entrySet()) {
            List<SessionListResponse.SessionDTO> weekSessions = entry.getValue();
            LocalDate weekStart = weekSessions.get(0).getDate();
            LocalDate weekEnd = weekSessions.get(weekSessions.size() - 1).getDate();

            result.add(SessionListResponse.WeekGroupDTO.builder()
                    .weekNumber(weekIndex++)
                    .weekRange(weekStart + " - " + weekEnd)
                    .sessionCount(weekSessions.size())
                    .sessionIds(weekSessions.stream()
                            .map(SessionListResponse.SessionDTO::getSessionId)
                            .collect(Collectors.toList()))
                    .build());
        }
        return result;
    }
}
