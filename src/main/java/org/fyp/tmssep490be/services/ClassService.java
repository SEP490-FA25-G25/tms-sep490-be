package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.classmanagement.AvailableStudentDTO;
import org.fyp.tmssep490be.dtos.classmanagement.ClassDetailDTO;
import org.fyp.tmssep490be.dtos.classmanagement.ClassListItemDTO;
import org.fyp.tmssep490be.dtos.classmanagement.ClassStudentDTO;
import org.fyp.tmssep490be.dtos.classmanagement.TeacherSummaryDTO;
import org.fyp.tmssep490be.dtos.classcreation.CreateClassRequest;
import org.fyp.tmssep490be.dtos.classcreation.CreateClassResponse;
import org.fyp.tmssep490be.dtos.qa.QASessionListResponse;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.utils.ScheduleUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClassService {

        private final ClassRepository classRepository;
        private final SessionRepository sessionRepository;
        private final TeachingSlotRepository teachingSlotRepository;
        private final EnrollmentRepository enrollmentRepository;
        private final UserBranchesRepository userBranchesRepository;
        private final UserAccountRepository userAccountRepository;
        private final TeacherRepository teacherRepository;
        private final StudentRepository studentRepository;
        private final ReplacementSkillAssessmentRepository skillAssessmentRepository;
        private final StudentSessionRepository studentSessionRepository;
        private final BranchRepository branchRepository;
        private final SubjectRepository subjectRepository;
        private final SubjectSessionRepository subjectSessionRepository;
        private final TimeSlotTemplateRepository timeSlotTemplateRepository;
        private final ResourceRepository resourceRepository;
        private final SessionResourceRepository sessionResourceRepository;
        private final ApprovalService approvalService;
        private final VietnamHolidayService vietnamHolidayService;

        public Page<ClassListItemDTO> getClasses(
                        List<Long> branchIds,
                        Long subjectId,
                        ClassStatus status,
                        ApprovalStatus approvalStatus,
                        Modality modality,
                        String search,
                        Pageable pageable,
                        Long userId) {
                log.debug(
                                "Getting classes for user {} with filters: branchIds={}, subjectId={}, status={}, approvalStatus={}, modality={}, search={}",
                                userId, branchIds, subjectId, status, approvalStatus, modality, search);

                List<Long> accessibleBranchIds = getUserAccessibleBranches(userId);

                // Filter by provided branch IDs if any
                List<Long> finalBranchIds = branchIds != null ? branchIds : accessibleBranchIds;
                if (finalBranchIds.isEmpty()) {
                        throw new CustomException(ErrorCode.CLASS_NO_BRANCH_ACCESS);
                }

                // Query classes with filters (null status/approvalStatus = all)
                Page<ClassEntity> classes = classRepository.findClassesForAcademicAffairs(
                                finalBranchIds,
                                approvalStatus, // null = all approval statuses
                                status, // null = all class statuses
                                subjectId,
                                modality,
                                search,
                                pageable);

                // Batch query session counts for all classes in page
                List<Long> classIds = classes.getContent().stream()
                                .map(ClassEntity::getId)
                                .toList();
                Map<Long, int[]> sessionCountsMap = getSessionCountsForClasses(classIds);

                return classes.map(classEntity -> convertToClassListItemDTO(classEntity, sessionCountsMap));
        }

        private Map<Long, int[]> getSessionCountsForClasses(List<Long> classIds) {
                if (classIds.isEmpty()) {
                        return java.util.Collections.emptyMap();
                }
                List<Object[]> results = sessionRepository.countSessionsByClassIds(classIds);
                Map<Long, int[]> map = new java.util.HashMap<>();
                for (Object[] row : results) {
                        Long classId = ((Number) row[0]).longValue();
                        int completed = ((Number) row[1]).intValue();
                        int total = ((Number) row[2]).intValue();
                        map.put(classId, new int[] { completed, total });
                }
                return map;
        }

        private ClassListItemDTO convertToClassListItemDTO(ClassEntity classEntity, Map<Long, int[]> sessionCountsMap) {
                // Lấy ra số lượng học viên đang ghi danh trong lớp này
                Integer currentEnrolled = enrollmentRepository.countByClassIdAndStatus(
                                classEntity.getId(), EnrollmentStatus.ENROLLED);

                Integer maxCapacity = classEntity.getMaxCapacity();
                Integer availableSlots = maxCapacity - currentEnrolled;
                Double utilizationRate = maxCapacity > 0 ? (double) currentEnrolled / maxCapacity * 100 : 0.0;

                // Get all teachers teaching this class
                List<TeacherSummaryDTO> teachers = getTeachersForClass(classEntity.getId());

                // Determine if enrollment is possible
                boolean canEnroll = availableSlots > 0
                                && (classEntity.getStatus() == ClassStatus.SCHEDULED
                                                || classEntity.getStatus() == ClassStatus.ONGOING)
                                && classEntity.getApprovalStatus() == ApprovalStatus.APPROVED;

                // Get session progress from batch query result
                int[] sessionCounts = sessionCountsMap.getOrDefault(classEntity.getId(), new int[] { 0, 0 });
                int completedSessions = sessionCounts[0];
                int totalSessions = sessionCounts[1];

                return ClassListItemDTO.builder()
                                .id(classEntity.getId())
                                .code(classEntity.getCode())
                                .name(classEntity.getName())
                                .subjectName(classEntity.getSubject().getName())
                                .subjectCode(classEntity.getSubject().getCode())
                                .branchName(classEntity.getBranch().getName())
                                .branchCode(classEntity.getBranch().getCode())
                                .modality(classEntity.getModality())
                                .startDate(classEntity.getStartDate())
                                .plannedEndDate(classEntity.getPlannedEndDate())
                                .status(classEntity.getStatus())
                                .approvalStatus(classEntity.getApprovalStatus())
                                .maxCapacity(maxCapacity)
                                .currentEnrolled(currentEnrolled)
                                .availableSlots(availableSlots)
                                .utilizationRate(utilizationRate)
                                .teachers(teachers)
                                .scheduleSummary(formatScheduleSummary(classEntity.getId()))
                                .scheduleDays(classEntity.getScheduleDays() != null
                                                ? Arrays.stream(classEntity.getScheduleDays())
                                                                .map(s -> s != null ? s.intValue() : null)
                                                                .toArray(Integer[]::new)
                                                : null)
                                .completedSessions(completedSessions)
                                .totalSessions(totalSessions)
                                .canEnrollStudents(canEnroll)
                                .enrollmentRestrictionReason(canEnroll ? null
                                                : availableSlots <= 0 ? "Class is full"
                                                                : classEntity.getStatus() == ClassStatus.COMPLETED
                                                                                ? "Class has completed"
                                                                                : classEntity.getStatus() == ClassStatus.CANCELLED
                                                                                                ? "Class was cancelled"
                                                                                                : "Class not available for enrollment")
                                .submittedAt(classEntity.getSubmittedAt())
                                .decidedAt(classEntity.getDecidedAt())
                                .rejectionReason(classEntity.getRejectionReason())
                                .build();
        }

        private String formatScheduleSummary(Long classId) {
                List<Session> sessions = sessionRepository.findByClassEntityIdOrderByDateAsc(classId);
                if (sessions.isEmpty()) {
                        return "Not specified";
                }
                return ScheduleUtils.generateScheduleSummary(sessions);
        }

        private List<ClassDetailDTO.ScheduleDetailDTO> generateScheduleDetailsForClass(Long classId) {
                List<Session> sessions = sessionRepository.findByClassEntityIdOrderByDateAsc(classId);
                if (sessions.isEmpty()) {
                        return List.of();
                }

                Map<Integer, String> dayTimeSlots = ScheduleUtils.extractScheduleFromSessions(sessions);

                return dayTimeSlots.entrySet().stream()
                                .sorted(Map.Entry.comparingByKey())
                                .map(entry -> {
                                        String dayName = ScheduleUtils.getDayNameVietnamese(entry.getKey());
                                        String[] times = ScheduleUtils.parseTimeSlot(entry.getValue());

                                        return ClassDetailDTO.ScheduleDetailDTO.builder()
                                                        .day(dayName)
                                                        .startTime(times[0])
                                                        .endTime(times[1])
                                                        .build();
                                })
                                .collect(Collectors.toList());
        }

        private List<TeacherSummaryDTO> getTeachersForClass(Long classId) {
                List<TeachingSlot> teachingSlots = teachingSlotRepository
                                .findByClassEntityId(classId);

                // Chỉ lấy các slot có status SCHEDULED (giáo viên dạy đúng lịch)
                // Loại bỏ SUBSTITUTED (giáo viên dạy thay) và ON_LEAVE (giáo viên nghỉ)
                Map<Teacher, Long> teacherSessionCounts = teachingSlots.stream()
                                .filter(slot -> slot.getTeacher() != null)
                                .filter(slot -> slot.getStatus() == TeachingSlotStatus.SCHEDULED)
                                .collect(Collectors.groupingBy(
                                                TeachingSlot::getTeacher,
                                                Collectors.counting()));

                return teacherSessionCounts.entrySet().stream()
                                .map(entry -> {
                                        Teacher teacher = entry.getKey();
                                        UserAccount userAccount = teacher.getUserAccount();
                                        return TeacherSummaryDTO.builder()
                                                        .id(userAccount.getId())
                                                        .teacherId(teacher.getId())
                                                        .fullName(userAccount.getFullName())
                                                        .email(userAccount.getEmail())
                                                        .phone(userAccount.getPhone())
                                                        .employeeCode(teacher.getEmployeeCode())
                                                        .sessionCount(entry.getValue().intValue())
                                                        .build();
                                })
                                .sorted(Comparator.comparing(TeacherSummaryDTO::getSessionCount).reversed())
                                .collect(Collectors.toList());
        }

        private List<Long> getUserAccessibleBranches(Long userId) {
                return userBranchesRepository.findBranchIdsByUserId(userId);
        }

        public ClassDetailDTO getClassDetail(Long classId, Long userId) {
                log.debug("Getting class detail for class {} by user {}", classId, userId);

                ClassEntity classEntity = classRepository.findById(classId)
                                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

                validateClassBranchAccess(classEntity, userId);

                // Get enrollment summary
                Integer currentEnrolled = enrollmentRepository.countByClassIdAndStatus(classId,
                                EnrollmentStatus.ENROLLED);
                ClassDetailDTO.EnrollmentSummary enrollmentSummary = calculateEnrollmentSummary(
                                currentEnrolled, classEntity.getMaxCapacity(), classEntity.getStatus(),
                                classEntity.getApprovalStatus());

                // Get all teachers teaching this class
                List<TeacherSummaryDTO> teachers = getTeachersForClass(classId);

                // Get session summary
                ClassDetailDTO.SessionSummary sessionSummary = calculateSessionSummary(classId);

                // Get performance metrics
                ClassDetailDTO.PerformanceMetrics performanceMetrics = calculatePerformanceMetrics(classId);

                return ClassDetailDTO.builder()
                                .id(classEntity.getId())
                                .code(classEntity.getCode())
                                .name(classEntity.getName())
                                .subject(convertToSubjectDTO(classEntity.getSubject()))
                                .branch(convertToBranchDTO(classEntity.getBranch()))
                                .modality(classEntity.getModality())
                                .startDate(classEntity.getStartDate())
                                .plannedEndDate(classEntity.getPlannedEndDate())
                                .actualEndDate(classEntity.getActualEndDate())
                                .scheduleDays(classEntity.getScheduleDays())
                                .maxCapacity(classEntity.getMaxCapacity())
                                .status(classEntity.getStatus())
                                .approvalStatus(classEntity.getApprovalStatus())
                                .rejectionReason(classEntity.getRejectionReason())
                                .submittedAt(classEntity.getSubmittedAt() != null
                                                ? classEntity.getSubmittedAt().toLocalDate()
                                                : null)
                                .decidedAt(classEntity.getDecidedAt() != null ? classEntity.getDecidedAt().toLocalDate()
                                                : null)
                                .decidedByName(classEntity.getDecidedBy() != null
                                                ? classEntity.getDecidedBy().getFullName()
                                                : null)
                                .createdByName(classEntity.getCreatedBy() != null
                                                ? classEntity.getCreatedBy().getFullName()
                                                : null)
                                .createdAt(classEntity.getCreatedAt())
                                .updatedAt(classEntity.getUpdatedAt())
                                .teachers(teachers)
                                .scheduleSummary(formatScheduleSummary(classEntity.getId()))
                                .scheduleDetails(generateScheduleDetailsForClass(classEntity.getId()))
                                .enrollmentSummary(enrollmentSummary)
                                .sessionSummary(sessionSummary)
                                .performanceMetrics(performanceMetrics)
                                .build();
        }

        private void validateClassBranchAccess(ClassEntity classEntity, Long userId) {
                // Managers are allowed to view all classes for monitoring purposes
                if (userId != null) {
                        UserAccount user = userAccountRepository.findById(userId).orElse(null);
                        if (user != null && user.getUserRoles() != null) {
                                boolean isManager = user.getUserRoles().stream()
                                                .anyMatch(ur -> ur.getRole() != null
                                                                && "MANAGER".equals(ur.getRole().getCode()));
                                if (isManager) {
                                        return;
                                }
                        }
                }

                List<Long> accessibleBranchIds = getUserAccessibleBranches(userId);
                boolean hasBranchAccess = accessibleBranchIds.contains(classEntity.getBranch().getId());
                boolean hasTeachingAccess = hasTeacherAssignment(userId, classEntity.getId());

                if (!hasBranchAccess && !hasTeachingAccess) {
                        log.warn("Access denied for user {} on class {} - branchAccess={}, teachingAccess={}",
                                        userId, classEntity.getId(), hasBranchAccess, hasTeachingAccess);
                        throw new CustomException(ErrorCode.CLASS_NOT_FOUND); // Use existing error code
                }
        }

        private boolean hasTeacherAssignment(Long userId, Long classId) {
                if (userId == null) {
                        return false;
                }

                // Check if user has any teaching slots for this class
                List<TeachingSlot> slots = teachingSlotRepository.findByClassEntityIdAndStatus(classId,
                                TeachingSlotStatus.SCHEDULED);
                boolean hasUserAssignment = slots.stream()
                                .anyMatch(slot -> slot.getTeacher() != null &&
                                                slot.getTeacher().getUserAccount().getId().equals(userId));

                if (hasUserAssignment) {
                        return true;
                }

                // Check by teacher entity
                return teacherRepository.findByUserAccountId(userId)
                                .map(teacher -> slots.stream()
                                                .anyMatch(slot -> slot.getTeacher() != null
                                                                && slot.getTeacher().getId().equals(teacher.getId())))
                                .orElse(false);
        }

        private ClassDetailDTO.SubjectDTO convertToSubjectDTO(Subject subject) {
                ClassDetailDTO.LevelDTO levelDTO = null;

                if (subject.getLevel() != null) {
                        Level level = subject.getLevel();
                        ClassDetailDTO.CurriculumDTO curriculumDTO = null;

                        if (level.getCurriculum() != null) {
                                Curriculum curriculum = level.getCurriculum();
                                curriculumDTO = ClassDetailDTO.CurriculumDTO.builder()
                                                .id(curriculum.getId())
                                                .code(curriculum.getCode())
                                                .name(curriculum.getName())
                                                .build();
                        }

                        levelDTO = ClassDetailDTO.LevelDTO.builder()
                                        .id(level.getId())
                                        .code(level.getCode())
                                        .name(level.getName())
                                        .curriculum(curriculumDTO)
                                        .build();
                }

                return ClassDetailDTO.SubjectDTO.builder()
                                .id(subject.getId())
                                .code(subject.getCode())
                                .name(subject.getName())
                                .description(subject.getDescription())
                                .totalHours(subject.getTotalHours())
                                .numberOfSessions(subject.getNumberOfSessions())
                                .hoursPerSession(subject.getHoursPerSession())
                                .prerequisites(subject.getPrerequisites())
                                .targetAudience(subject.getTargetAudience())
                                .teachingMethods(subject.getTeachingMethods())
                                .level(levelDTO)
                                .build();
        }

        private ClassDetailDTO.BranchDTO convertToBranchDTO(Branch branch) {
                return ClassDetailDTO.BranchDTO.builder()
                                .id(branch.getId())
                                .code(branch.getCode())
                                .name(branch.getName())
                                .address(branch.getAddress())
                                .phone(branch.getPhone())
                                .email(branch.getEmail())
                                .district(branch.getDistrict())
                                .city(branch.getCity())
                                .build();
        }

        private ClassDetailDTO.SessionSummary calculateSessionSummary(Long classId) {
                List<Session> sessions = sessionRepository.findByClassEntityIdOrderByDateAsc(classId);

                int totalSessions = sessions.size();
                int completedSessions = (int) sessions.stream()
                                .filter(s -> s.getStatus() == SessionStatus.DONE)
                                .count();
                int cancelledSessions = (int) sessions.stream()
                                .filter(s -> s.getStatus() == SessionStatus.CANCELLED)
                                .count();
                int upcomingSessions = (int) sessions.stream()
                                .filter(s -> s.getStatus() == SessionStatus.PLANNED)
                                .count();

                return ClassDetailDTO.SessionSummary.builder()
                                .totalSessions(totalSessions)
                                .completedSessions(completedSessions)
                                .upcomingSessions(upcomingSessions)
                                .cancelledSessions(cancelledSessions)
                                .build();
        }

        private ClassDetailDTO.PerformanceMetrics calculatePerformanceMetrics(Long classId) {
                // Get all student sessions for this class
                List<StudentSession> studentSessions = studentSessionRepository
                                .findByClassIdWithSessionAndStudent(classId);

                if (studentSessions.isEmpty()) {
                        return ClassDetailDTO.PerformanceMetrics.builder()
                                        .attendanceRate(0.0)
                                        .homeworkCompletionRate(0.0)
                                        .build();
                }

                // Only count sessions that are DONE (completed)
                List<StudentSession> completedStudentSessions = studentSessions.stream()
                                .filter(ss -> ss.getSession().getStatus() == SessionStatus.DONE)
                                .filter(ss -> !Boolean.TRUE.equals(ss.getIsMakeup())) // Bỏ qua học bù
                                .toList();

                if (completedStudentSessions.isEmpty()) {
                        return ClassDetailDTO.PerformanceMetrics.builder()
                                        .attendanceRate(0.0)
                                        .homeworkCompletionRate(0.0)
                                        .build();
                }

                // Map thông tin học bù
                List<Long> sessionIds = completedStudentSessions.stream()
                                .map(ss -> ss.getSession().getId())
                                .distinct()
                                .toList();
                Map<Long, Map<Long, Boolean>> makeupCompletedMap = new HashMap<>();
                if (!sessionIds.isEmpty()) {
                        studentSessionRepository.findMakeupSessionsByOriginalSessionIds(sessionIds)
                                        .forEach(ss -> {
                                                Session originalSession = ss.getOriginalSession();
                                                if (originalSession == null || ss.getStudent() == null) {
                                                        return;
                                                }
                                                Long originalSessionId = originalSession.getId();
                                                Long studentId = ss.getStudent().getId();
                                                if (originalSessionId == null || studentId == null) {
                                                        return;
                                                }

                                                makeupCompletedMap
                                                                .computeIfAbsent(originalSessionId,
                                                                                k -> new HashMap<>())
                                                                .merge(
                                                                                studentId,
                                                                                ss.getAttendanceStatus() == AttendanceStatus.PRESENT,
                                                                                (oldVal, newVal) -> oldVal || newVal);
                                        });
                }

                // Calculate attendance rate với logic EXCUSED mới
                long totalRecorded = 0;
                long presentCount = 0;
                LocalDateTime now = LocalDateTime.now();

                for (StudentSession ss : completedStudentSessions) {
                        AttendanceStatus status = ss.getAttendanceStatus();
                        Session session = ss.getSession();

                        if (status == AttendanceStatus.PRESENT) {
                                presentCount++;
                                totalRecorded++;
                        } else if (status == AttendanceStatus.ABSENT) {
                                totalRecorded++;
                        } else if (status == AttendanceStatus.EXCUSED) {
                                // Kiểm tra xem đã có buổi học bù PRESENT hay chưa
                                boolean hasMakeupCompleted = makeupCompletedMap
                                                .getOrDefault(session.getId(), Map.of())
                                                .getOrDefault(ss.getStudent().getId(), false);

                                if (hasMakeupCompleted) {
                                        // EXCUSED có học bù (chấm xanh) → tính như PRESENT
                                        presentCount++;
                                        totalRecorded++;
                                } else {
                                        // Kiểm tra xem đã qua giờ kết thúc buổi gốc chưa
                                        LocalDate sessionDate = session.getDate();
                                        LocalDateTime sessionEndDateTime;
                                        if (session.getTimeSlotTemplate() != null
                                                        && session.getTimeSlotTemplate().getEndTime() != null) {
                                                LocalTime endTime = session.getTimeSlotTemplate().getEndTime();
                                                sessionEndDateTime = LocalDateTime.of(sessionDate, endTime);
                                        } else {
                                                sessionEndDateTime = LocalDateTime.of(sessionDate, LocalTime.MAX);
                                        }

                                        boolean isAfterSessionEnd = now.isAfter(sessionEndDateTime);
                                        if (isAfterSessionEnd) {
                                                // EXCUSED không học bù và đã qua giờ kết thúc (chấm đỏ) → tính như
                                                // ABSENT
                                                totalRecorded++;
                                        }
                                        // Nếu chưa qua giờ kết thúc → bỏ qua (không tính vào tỷ lệ)
                                }
                        }
                }

                double attendanceRate = totalRecorded > 0 ? (double) presentCount / totalRecorded * 100 : 0.0;

                // Calculate homework completion rate
                long totalWithHomework = completedStudentSessions.stream()
                                .filter(ss -> ss.getHomeworkStatus() != null)
                                .count();
                long completedHomework = completedStudentSessions.stream()
                                .filter(ss -> ss.getHomeworkStatus() == HomeworkStatus.COMPLETED)
                                .count();
                double homeworkRate = totalWithHomework > 0 ? (double) completedHomework / totalWithHomework * 100
                                : 0.0;

                return ClassDetailDTO.PerformanceMetrics.builder()
                                .attendanceRate(Math.round(attendanceRate * 10.0) / 10.0)
                                .homeworkCompletionRate(Math.round(homeworkRate * 10.0) / 10.0)
                                .build();
        }

        private ClassDetailDTO.EnrollmentSummary calculateEnrollmentSummary(
                        Integer currentEnrolled,
                        Integer maxCapacity,
                        ClassStatus status,
                        ApprovalStatus approvalStatus) {
                Integer availableSlots = maxCapacity - currentEnrolled;
                Double utilizationRate = maxCapacity > 0 ? (double) currentEnrolled / maxCapacity * 100 : 0.0;

                boolean canEnroll = (status == ClassStatus.SCHEDULED || status == ClassStatus.ONGOING)
                                && approvalStatus == ApprovalStatus.APPROVED
                                && availableSlots > 0;

                String restrictionReason = null;
                if (!canEnroll) {
                        if (status == ClassStatus.COMPLETED) {
                                restrictionReason = "Class has completed";
                        } else if (status == ClassStatus.CANCELLED) {
                                restrictionReason = "Class was cancelled";
                        } else if (status != ClassStatus.SCHEDULED && status != ClassStatus.ONGOING) {
                                restrictionReason = "Class is not available for enrollment";
                        } else if (approvalStatus != ApprovalStatus.APPROVED) {
                                restrictionReason = "Class is not approved";
                        } else if (availableSlots <= 0) {
                                restrictionReason = "Class is at full capacity";
                        }
                }

                return ClassDetailDTO.EnrollmentSummary.builder()
                                .currentEnrolled(currentEnrolled)
                                .maxCapacity(maxCapacity)
                                .availableSlots(availableSlots)
                                .utilizationRate(utilizationRate)
                                .canEnrollStudents(canEnroll)
                                .enrollmentRestrictionReason(restrictionReason)
                                .build();
        }

        // Allowed skills for matching
        private static final Set<Skill> ALLOWED_SKILLS = Set.of(
                        Skill.GENERAL, Skill.READING, Skill.WRITING, Skill.SPEAKING, Skill.LISTENING);

        public Page<ClassStudentDTO> getClassStudents(
                        Long classId,
                        String search,
                        Pageable pageable,
                        Long userId) {
                log.debug("Getting students for class {} by user {} with search: {}", classId, userId, search);

                ClassEntity classEntity = classRepository.findById(classId)
                                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

                // Validate user has access to this class
                validateClassBranchAccess(classEntity, userId);

                // Prepare search parameter with wildcards for LIKE query
                String searchPattern = (search != null && !search.isBlank())
                                ? "%" + search + "%"
                                : null;

                // Get enrolled students
                Page<Enrollment> enrollments = enrollmentRepository.findEnrolledStudentsByClass(
                                classId, EnrollmentStatus.ENROLLED, searchPattern, pageable);

                return enrollments.map(this::convertToClassStudentDTO);
        }

        public Page<AvailableStudentDTO> getAvailableStudentsForClass(
                        Long classId,
                        String search,
                        Pageable pageable,
                        Long userId) {
                log.debug("Getting available students for class {} by user {} with search: {}", classId, userId,
                                search);

                // Validate class exists and user has access
                ClassEntity classEntity = classRepository.findById(classId)
                                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));
                validateClassBranchAccess(classEntity, userId);

                // Get class details for skill assessment matching
                Long branchId = classEntity.getBranch().getId();
                final Long classCurriculumId;
                final Long classLevelId;

                if (classEntity.getSubject() != null && classEntity.getSubject().getLevel() != null) {
                        classLevelId = classEntity.getSubject().getLevel().getId();
                        if (classEntity.getSubject().getLevel().getCurriculum() != null) {
                                classCurriculumId = classEntity.getSubject().getLevel().getCurriculum().getId();
                        } else {
                                classCurriculumId = null;
                        }
                } else {
                        classCurriculumId = null;
                        classLevelId = null;
                }

                // Get ALL available students (no pagination for proper sorting)
                List<Student> allAvailableStudents = studentRepository.findAllAvailableStudentsForClass(
                                classId, branchId, search);

                // Batch fetch ALL skill assessments for all students
                List<Long> studentIds = allAvailableStudents.stream()
                                .map(Student::getId)
                                .collect(Collectors.toList());

                List<ReplacementSkillAssessment> allAssessments = studentIds.isEmpty() ? List.of()
                                : skillAssessmentRepository.findByStudentIdIn(studentIds);

                // Nhóm toàn bộ bài kiểm tra của một student vào một key trong map bằng
                // studentId
                // 1L: [assessment1_of_student1, assessment2_of_student1],
                // 2L: [assessment1_of_student2, assessment2_of_student2],
                // 3L: [assessment1_of_student3, assessment2_of_student3]
                Map<Long, List<ReplacementSkillAssessment>> assessmentsByStudent = allAssessments.stream()
                                .collect(Collectors.groupingBy(assessment -> assessment.getStudent().getId()));

                // Sort by matchPriority (ascending: 1, 2, 3)
                List<AvailableStudentDTO> allDtos = allAvailableStudents.stream()
                                .map(student -> convertToAvailableStudentDTO(
                                                student,
                                                assessmentsByStudent.get(student.getId()),
                                                classCurriculumId,
                                                classLevelId))
                                .sorted(Comparator.comparingInt(
                                                (AvailableStudentDTO dto) -> dto.getClassMatchInfo().getMatchPriority())
                                                .thenComparing(AvailableStudentDTO::getFullName))
                                .collect(Collectors.toList());

                // Apply pagination manually on sorted list
                int start = pageable.getPageNumber() * pageable.getPageSize();
                int end = Math.min(start + pageable.getPageSize(), allDtos.size());
                List<AvailableStudentDTO> paginatedDtos = start < allDtos.size() ? allDtos.subList(start, end)
                                : List.of();

                return new PageImpl<>(paginatedDtos, pageable, allDtos.size());
        }

        private AvailableStudentDTO convertToAvailableStudentDTO(
                        Student student,
                        List<ReplacementSkillAssessment> assessments,
                        Long classCurriculumId,
                        Long classLevelId) {
                UserAccount userAccount = student.getUserAccount();
                List<ReplacementSkillAssessment> filteredAssessments = filterAllowedAssessments(assessments);

                // Convert all assessments to DTOs
                List<AvailableStudentDTO.SkillAssessmentDTO> assessmentDTOs = !filteredAssessments.isEmpty()
                                ? filteredAssessments.stream()
                                                .map(this::convertToSkillAssessmentDTO)
                                                .sorted((a1, a2) -> a2.getAssessmentDate()
                                                                .compareTo(a1.getAssessmentDate()))
                                                .collect(Collectors.toList())
                                : List.of();

                // Calculate class match info
                AvailableStudentDTO.ClassMatchInfoDTO classMatchInfo = calculateClassMatchInfo(
                                filteredAssessments, classCurriculumId, classLevelId);

                // Get active enrollments count
                int activeEnrollments = enrollmentRepository.countByStudentIdAndStatus(
                                student.getId(), EnrollmentStatus.ENROLLED);

                return AvailableStudentDTO.builder()
                                .id(student.getId())
                                .studentCode(student.getStudentCode())
                                .fullName(userAccount.getFullName())
                                .email(userAccount.getEmail())
                                .phone(userAccount.getPhone())
                                .address(userAccount.getAddress())
                                .avatarUrl(userAccount.getAvatarUrl())
                                .replacementSkillAssessments(assessmentDTOs)
                                .classMatchInfo(classMatchInfo)
                                .activeEnrollments(activeEnrollments)
                                .build();
        }

        private List<ReplacementSkillAssessment> filterAllowedAssessments(
                        List<ReplacementSkillAssessment> assessments) {
                if (assessments == null || assessments.isEmpty()) {
                        return List.of();
                }
                return assessments.stream()
                                .filter(a -> a.getSkill() != null && ALLOWED_SKILLS.contains(a.getSkill()))
                                .collect(Collectors.toList());
        }

        private AvailableStudentDTO.ClassMatchInfoDTO calculateClassMatchInfo(
                        List<ReplacementSkillAssessment> assessments,
                        Long classCurriculumId,
                        Long classLevelId) {
                // Default: No match
                Integer matchPriority = 3;
                String matchingSkill = null;
                AvailableStudentDTO.LevelInfoDTO matchingLevel = null;

                if (assessments == null || assessments.isEmpty() || classCurriculumId == null) {
                        return buildClassMatchInfo(matchPriority, matchingSkill, matchingLevel);
                }

                // Find assessments matching the class curriculum
                List<ReplacementSkillAssessment> curriculumMatches = assessments.stream()
                                .filter(a -> a.getLevel() != null &&
                                                a.getLevel().getCurriculum() != null &&
                                                a.getLevel().getCurriculum().getId().equals(classCurriculumId))
                                .toList();

                if (curriculumMatches.isEmpty()) {
                        return buildClassMatchInfo(matchPriority, matchingSkill, matchingLevel);
                }

                // Try to find perfect match (curriculum + level)
                ReplacementSkillAssessment matchingAssessment = null;
                if (classLevelId != null) {
                        matchingAssessment = curriculumMatches.stream()
                                        .filter(a -> a.getLevel().getId().equals(classLevelId))
                                        .findFirst()
                                        .orElse(null);

                        if (matchingAssessment != null) {
                                matchPriority = 1; // Perfect match (curriculum + level)
                        }
                }

                // If no perfect match, take first curriculum match
                if (matchingAssessment == null) {
                        matchingAssessment = curriculumMatches.get(0);
                        matchPriority = 2; // Partial match (curriculum only)
                }

                // Build result with matched assessment info
                matchingSkill = matchingAssessment.getSkill().name();
                matchingLevel = convertToLevelInfoDTO(matchingAssessment.getLevel());

                return buildClassMatchInfo(matchPriority, matchingSkill, matchingLevel);
        }

        private AvailableStudentDTO.ClassMatchInfoDTO buildClassMatchInfo(
                        Integer priority,
                        String skill,
                        AvailableStudentDTO.LevelInfoDTO level) {
                return AvailableStudentDTO.ClassMatchInfoDTO.builder()
                                .matchPriority(priority)
                                .matchingSkill(skill)
                                .matchingLevel(level)
                                .build();
        }

        private AvailableStudentDTO.SkillAssessmentDTO convertToSkillAssessmentDTO(
                        ReplacementSkillAssessment assessment) {
                return AvailableStudentDTO.SkillAssessmentDTO.builder()
                                .id(assessment.getId())
                                .skill(assessment.getSkill().name())
                                .level(convertToLevelInfoDTO(assessment.getLevel()))
                                .score(assessment.getScore())
                                .assessmentDate(assessment.getAssessmentDate())
                                .assessmentType(assessment.getAssessmentType())
                                .note(assessment.getNote())
                                .assessedBy(convertToAssessorDTO(assessment.getAssessedBy()))
                                .build();
        }

        private AvailableStudentDTO.LevelInfoDTO convertToLevelInfoDTO(Level level) {
                if (level == null) {
                        return null;
                }

                return AvailableStudentDTO.LevelInfoDTO.builder()
                                .id(level.getId())
                                .code(level.getCode())
                                .name(level.getName())
                                .subject(convertToCurriculumInfoDTO(level.getCurriculum()))
                                .description(level.getDescription())
                                .build();
        }

        private AvailableStudentDTO.SubjectInfoDTO convertToCurriculumInfoDTO(Curriculum curriculum) {
                if (curriculum == null) {
                        return null;
                }

                return AvailableStudentDTO.SubjectInfoDTO.builder()
                                .id(curriculum.getId())
                                .name(curriculum.getName())
                                .build();
        }

        private AvailableStudentDTO.AssessorDTO convertToAssessorDTO(UserAccount assessor) {
                if (assessor == null) {
                        return null;
                }

                return AvailableStudentDTO.AssessorDTO.builder()
                                .id(assessor.getId())
                                .fullName(assessor.getFullName())
                                .build();
        }

        public QASessionListResponse getSessionsWithMetrics(Long classId, Long userId) {
                log.info("Getting sessions with metrics for class ID {} by user {}", classId, userId);

                // Get class and validate access
                ClassEntity classEntity = classRepository.findById(classId)
                                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));
                validateClassBranchAccess(classEntity, userId);

                // Get all sessions ordered by date
                List<Session> sessions = sessionRepository.findByClassEntityIdOrderByDateAsc(classId);

                // Map to QASessionItemDTO with real metrics
                List<QASessionListResponse.QASessionItemDTO> sessionItems = sessions.stream()
                                .map(s -> {
                                        // Get real student session data for this session
                                        List<StudentSession> studentSessions = studentSessionRepository
                                                        .findBySessionId(s.getId());

                                        // Calculate real attendance metrics
                                        long presentCount = studentSessions.stream()
                                                        .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.PRESENT)
                                                        .count();

                                        long absentCount = studentSessions.stream()
                                                        .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.ABSENT)
                                                        .count();

                                        long totalStudents = studentSessions.size();

                                        long homeworkCompletedCount = studentSessions.stream()
                                                        .filter(ss -> ss.getHomeworkStatus() == HomeworkStatus.COMPLETED)
                                                        .count();

                                        boolean hasHomework = studentSessions.stream()
                                                        .anyMatch(ss -> ss.getHomeworkStatus() != null
                                                                        && ss.getHomeworkStatus() != HomeworkStatus.NO_HOMEWORK);

                                        // Get QA report count
                                        int qaReportCount = s.getQaReports() != null ? s.getQaReports().size() : 0;

                                        // Get session info from related entities
                                        Integer sequenceNumber = s.getSubjectSession() != null
                                                        ? s.getSubjectSession().getSequenceNo()
                                                        : null;
                                        String timeSlot = s.getTimeSlotTemplate() != null
                                                        ? s.getTimeSlotTemplate().getName()
                                                        : "Chưa xếp lịch";
                                        LocalTime startTime = s.getTimeSlotTemplate() != null
                                                        ? s.getTimeSlotTemplate().getStartTime()
                                                        : null;
                                        LocalTime endTime = s.getTimeSlotTemplate() != null
                                                        ? s.getTimeSlotTemplate().getEndTime()
                                                        : null;
                                        String topic = s.getSubjectSession() != null ? s.getSubjectSession().getTopic()
                                                        : "N/A";

                                        // Get teacher from teaching slots
                                        String teacherName = "Chưa phân công";
                                        if (s.getTeachingSlots() != null && !s.getTeachingSlots().isEmpty()) {
                                                teacherName = s.getTeachingSlots().stream()
                                                                .findFirst()
                                                                .map(ts -> ts.getTeacher() != null && ts.getTeacher()
                                                                                .getUserAccount() != null
                                                                                                ? ts.getTeacher()
                                                                                                                .getUserAccount()
                                                                                                                .getFullName()
                                                                                                : "Chưa phân công")
                                                                .orElse("Chưa phân công");
                                        }

                                        return QASessionListResponse.QASessionItemDTO.builder()
                                                        .sessionId(s.getId())
                                                        .sequenceNumber(sequenceNumber)
                                                        .date(s.getDate())
                                                        .dayOfWeek(s.getDate() != null
                                                                        ? s.getDate().getDayOfWeek().name()
                                                                        : null)
                                                        .timeSlot(timeSlot)
                                                        .startTime(startTime)
                                                        .endTime(endTime)
                                                        .topic(topic)
                                                        .status(s.getStatus() != null ? s.getStatus().name() : null)
                                                        .teacherName(teacherName)
                                                        .totalStudents((int) totalStudents)
                                                        .presentCount((int) presentCount)
                                                        .absentCount((int) absentCount)
                                                        .homeworkCompletedCount((int) homeworkCompletedCount)
                                                        .hasHomework(hasHomework)
                                                        .hasQAReport(qaReportCount > 0)
                                                        .qaReportCount(qaReportCount)
                                                        .build();
                                })
                                .collect(Collectors.toList());

                return QASessionListResponse.builder()
                                .classId(classEntity.getId())
                                .classCode(classEntity.getCode() != null ? classEntity.getCode() : "N/A")
                                .totalSessions(sessions.size())
                                .sessions(sessionItems)
                                .build();
        }

        private ClassStudentDTO convertToClassStudentDTO(Enrollment enrollment) {
                Student student = enrollment.getStudent();
                UserAccount userAccount = student.getUserAccount();

                // Get branch name from student's user branches
                String branchName = null;
                if (userAccount.getUserBranches() != null && !userAccount.getUserBranches().isEmpty()) {
                        branchName = userAccount.getUserBranches().iterator().next().getBranch().getName();
                }

                return ClassStudentDTO.builder()
                                .id(enrollment.getId())
                                .studentId(student.getId())
                                .studentCode(student.getStudentCode())
                                .fullName(userAccount.getFullName())
                                .email(userAccount.getEmail())
                                .phone(userAccount.getPhone())
                                .avatarUrl(userAccount.getAvatarUrl())
                                .address(userAccount.getAddress())
                                .branchName(branchName)
                                .enrolledAt(enrollment.getEnrolledAt())
                                .enrolledBy(enrollment.getEnrolledByUser() != null
                                                ? enrollment.getEnrolledByUser().getFullName()
                                                : "System")
                                .enrolledById(enrollment.getEnrolledBy())
                                .status(enrollment.getStatus())
                                .joinSessionId(enrollment.getJoinSessionId())
                                .joinSessionDate(
                                                enrollment.getJoinSession() != null
                                                                ? enrollment.getJoinSession().getDate().toString()
                                                                : null)
                                .capacityOverride(enrollment.getCapacityOverride())
                                .overrideReason(enrollment.getOverrideReason())
                                .build();
        }

        // ==================== CREATE CLASS (STEP 1) ====================

        @Transactional
        public CreateClassResponse createClass(CreateClassRequest request, Long userId) {
                log.info("Creating new class with code: {} for user ID: {}", request.getCode(), userId);

                // Validate user has access to branch
                List<Long> userBranchIds = userBranchesRepository.findBranchIdsByUserId(userId);
                if (!userBranchIds.contains(request.getBranchId())) {
                        throw new CustomException(ErrorCode.BRANCH_ACCESS_DENIED);
                }

                // Get entities
                Branch branch = branchRepository.findById(request.getBranchId())
                                .orElseThrow(() -> new CustomException(ErrorCode.BRANCH_NOT_FOUND));

                Subject subject = subjectRepository.findById(request.getSubjectId())
                                .orElseThrow(() -> new CustomException(ErrorCode.SUBJECT_NOT_FOUND));

                UserAccount createdBy = userAccountRepository.findById(userId)
                                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                // Validate class name uniqueness in branch
                if (classRepository.existsByBranchIdAndNameIgnoreCase(request.getBranchId(), request.getName())) {
                        throw new CustomException(ErrorCode.CLASS_NAME_DUPLICATE);
                }

                // Validate schedule days vs number of sessions
                if (request.getScheduleDays().size() > subject.getNumberOfSessions()) {
                        throw new CustomException(ErrorCode.INVALID_SCHEDULE_DAYS);
                }

                // Validate start date must fall on one of the schedule days
                int startDayOfWeek = request.getStartDate().getDayOfWeek().getValue() % 7; // Convert to 0=Sunday,
                                                                                           // 1=Monday...6=Saturday
                boolean startDateInSchedule = request.getScheduleDays().stream()
                                .anyMatch(day -> day.intValue() == startDayOfWeek);
                if (!startDateInSchedule) {
                        throw new CustomException(ErrorCode.START_DATE_NOT_IN_SCHEDULE_DAYS);
                }

                // Auto-generate class code if not provided
                String classCode = request.getCode();
                if (classCode == null || classCode.trim().isEmpty()) {
                        log.info("Class code not provided - auto-generating");
                        classCode = generateClassCode(branch, subject, request.getStartDate());
                        log.info("Auto-generated class code: {}", classCode);
                }

                // Create class entity
                OffsetDateTime now = OffsetDateTime.now();
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
                                .approvalStatus(null)
                                .createdBy(createdBy)
                                .createdAt(now)
                                .updatedAt(now)
                                .build();

                // Save class first to get ID
                classEntity = classRepository.save(classEntity);
                log.info("Created class entity with ID: {}", classEntity.getId());

                // Generate sessions
                List<Session> sessions = generateSessionsForClass(classEntity, subject);
                List<Session> savedSessions = sessionRepository.saveAll(sessions);

                // Calculate end date
                LocalDate endDate = calculateEndDate(savedSessions);
                classEntity.setPlannedEndDate(endDate);
                classEntity = classRepository.save(classEntity);

                log.info("Generated and saved {} sessions for class {}", savedSessions.size(), classEntity.getCode());

                // Build response
                CreateClassResponse.SessionGenerationSummary sessionSummary = CreateClassResponse.SessionGenerationSummary
                                .builder()
                                .sessionsGenerated(savedSessions.size())
                                .totalSessionsInSubject(subject.getNumberOfSessions())
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

        /**
         * Update an existing class (only for DRAFT or REJECTED status)
         * Does not regenerate sessions, only updates basic info
         */
        @Transactional
        public CreateClassResponse updateClass(Long classId, CreateClassRequest request, Long userId) {
                log.info("Updating class ID: {} for user ID: {}", classId, userId);

                // Find class
                ClassEntity classEntity = classRepository.findById(classId)
                                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

                // Check if class can be edited (only DRAFT or REJECTED)
                if (classEntity.getStatus() != ClassStatus.DRAFT) {
                        if (classEntity.getApprovalStatus() != ApprovalStatus.REJECTED) {
                                throw new CustomException(ErrorCode.CLASS_NOT_EDITABLE);
                        }
                }

                // Validate user has access to branch
                List<Long> userBranchIds = userBranchesRepository.findBranchIdsByUserId(userId);
                if (!userBranchIds.contains(request.getBranchId())) {
                        throw new CustomException(ErrorCode.BRANCH_ACCESS_DENIED);
                }

                // Validate class name uniqueness in branch (exclude current class)
                boolean nameExists = classRepository.existsByBranchIdAndNameIgnoreCaseAndIdNot(
                                request.getBranchId(), request.getName(), classId);
                if (nameExists) {
                        throw new CustomException(ErrorCode.CLASS_NAME_DUPLICATE);
                }

                // Get entities if branch or subject changed
                Branch branch = classEntity.getBranch();
                Subject subject = classEntity.getSubject();

                if (!request.getBranchId().equals(branch.getId())) {
                        branch = branchRepository.findById(request.getBranchId())
                                        .orElseThrow(() -> new CustomException(ErrorCode.BRANCH_NOT_FOUND));
                }

                if (!request.getSubjectId().equals(subject.getId())) {
                        subject = subjectRepository.findById(request.getSubjectId())
                                        .orElseThrow(() -> new CustomException(ErrorCode.SUBJECT_NOT_FOUND));
                }

                // Validate schedule days vs number of sessions
                if (request.getScheduleDays().size() > subject.getNumberOfSessions()) {
                        throw new CustomException(ErrorCode.INVALID_SCHEDULE_DAYS);
                }

                // Validate start date must fall on one of the schedule days
                int startDayOfWeek = request.getStartDate().getDayOfWeek().getValue() % 7; // Convert to 0=Sunday,
                                                                                           // 1=Monday...6=Saturday
                boolean startDateInSchedule = request.getScheduleDays().stream()
                                .anyMatch(day -> day.intValue() == startDayOfWeek);
                if (!startDateInSchedule) {
                        throw new CustomException(ErrorCode.START_DATE_NOT_IN_SCHEDULE_DAYS);
                }

                // Check if schedule-affecting fields changed (need to regenerate sessions)
                boolean scheduleDaysChanged = !java.util.Arrays.equals(
                                classEntity.getScheduleDays(),
                                request.getScheduleDays().toArray(new Short[0]));
                boolean startDateChanged = !classEntity.getStartDate().equals(request.getStartDate());
                boolean subjectChanged = !subject.getId().equals(classEntity.getSubject().getId());
                boolean needRegenerateSessions = scheduleDaysChanged || startDateChanged || subjectChanged;

                // Update basic info
                classEntity.setBranch(branch);
                classEntity.setSubject(subject);
                classEntity.setName(request.getName());
                classEntity.setModality(request.getModality());
                classEntity.setMaxCapacity(request.getMaxCapacity());
                classEntity.setScheduleDays(request.getScheduleDays().toArray(new Short[0]));
                classEntity.setStartDate(request.getStartDate());
                classEntity.setUpdatedAt(OffsetDateTime.now());

                // If class was rejected, reset to DRAFT for re-submission
                if (classEntity.getApprovalStatus() == ApprovalStatus.REJECTED) {
                        classEntity.setApprovalStatus(null);
                        classEntity.setRejectionReason(null);
                        classEntity.setSubmittedAt(null);
                        classEntity.setDecidedAt(null);
                        classEntity.setDecidedBy(null);
                }

                classEntity = classRepository.save(classEntity);
                log.info("Updated class entity with ID: {}", classEntity.getId());

                // Regenerate sessions if schedule changed
                List<Session> sessions;
                if (needRegenerateSessions) {
                        log.info("Schedule changed - regenerating sessions for class {}", classEntity.getId());

                        // Delete old sessions
                        List<Session> oldSessions = sessionRepository.findByClassEntityIdOrderByDateAsc(classId);
                        sessionRepository.deleteAll(oldSessions);
                        log.info("Deleted {} old sessions", oldSessions.size());

                        // Generate new sessions
                        sessions = generateSessionsForClass(classEntity, subject);
                        sessions = sessionRepository.saveAll(sessions);
                        log.info("Generated {} new sessions", sessions.size());

                        // Update end date
                        LocalDate endDate = calculateEndDate(sessions);
                        classEntity.setPlannedEndDate(endDate);
                        classEntity = classRepository.save(classEntity);
                } else {
                        sessions = sessionRepository.findByClassEntityIdOrderByDateAsc(classId);
                }

                // Build response
                CreateClassResponse.SessionGenerationSummary sessionSummary = CreateClassResponse.SessionGenerationSummary
                                .builder()
                                .sessionsGenerated(sessions.size())
                                .totalSessionsInSubject(subject.getNumberOfSessions())
                                .subjectCode(subject.getCode())
                                .subjectName(subject.getName())
                                .startDate(classEntity.getStartDate())
                                .endDate(classEntity.getPlannedEndDate())
                                .scheduleDays(classEntity.getScheduleDays())
                                .build();

                return CreateClassResponse.builder().classId(classEntity.getId()).code(classEntity.getCode())
                                .name(classEntity.getName()).status(classEntity.getStatus())
                                .approvalStatus(classEntity.getApprovalStatus()).createdAt(classEntity.getCreatedAt())
                                .sessionSummary(sessionSummary).build();
        }

        private String generateClassCode(Branch branch, Subject subject, LocalDate startDate) {
                String normalizedSubjectCode = normalizeSubjectCode(subject.getCode());
                int year = startDate.getYear() % 100;
                String prefix = String.format("%s-%s-%02d", normalizedSubjectCode, branch.getCode(), year);
                int nextSequence = findNextSequence(branch.getId(), prefix);
                return String.format("%s-%03d", prefix, nextSequence);
        }

        private String normalizeSubjectCode(String subjectCode) {
                if (subjectCode == null || subjectCode.trim().isEmpty()) {
                        return "UNKNOWN";
                }
                String normalized = subjectCode.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
                Pattern yearPattern = Pattern.compile("(\\d{4})");
                Matcher matcher = yearPattern.matcher(normalized);
                if (matcher.find()) {
                        normalized = normalized.substring(0, matcher.start());
                }
                return normalized.isEmpty() ? subjectCode.toUpperCase() : normalized;
        }

        private int findNextSequence(Long branchId, String prefix) {
                String likePattern = prefix + "-%";
                String highestCode = classRepository.findHighestCodeByPrefixReadOnly(branchId, likePattern);
                if (highestCode == null || highestCode.isEmpty()) {
                        return 1;
                }
                try {
                        String sequencePart = highestCode.substring(highestCode.lastIndexOf('-') + 1);
                        return Integer.parseInt(sequencePart) + 1;
                } catch (Exception e) {
                        log.warn("Could not extract sequence from code: {}", highestCode);
                        return 1;
                }
        }

        private List<Session> generateSessionsForClass(ClassEntity classEntity, Subject subject) {
                log.info("Generating sessions for class {} based on subject {}",
                                classEntity.getCode(), subject.getCode());

                // Get subject sessions ordered by phase ASC, sequence ASC
                List<SubjectSession> subjectSessions = subjectSessionRepository
                                .findBySubjectIdOrderByPhaseAndSequence(subject.getId());

                if (subjectSessions.isEmpty()) {
                        log.warn("No subject sessions found for subject {} (ID: {})", subject.getCode(),
                                        subject.getId());
                        return new ArrayList<>();
                }

                LocalDate startDate = classEntity.getStartDate();
                Short[] scheduleDays = classEntity.getScheduleDays();
                List<Session> sessions = new ArrayList<>();
                OffsetDateTime now = OffsetDateTime.now();

                // Convert startDate's day of week to frontend format (0=Sunday,
                // 1=Monday...6=Saturday)
                int startDayOfWeek = startDate.getDayOfWeek().getValue() % 7; // 1-7 -> 1-6,0

                // Sort scheduleDays to find rotation starting point
                Short[] sortedDays = java.util.Arrays.copyOf(scheduleDays, scheduleDays.length);
                java.util.Arrays.sort(sortedDays);

                // Find where startDate's day is in sortedDays and rotate from there
                int rotationIndex = 0;
                for (int i = 0; i < sortedDays.length; i++) {
                        if (sortedDays[i].intValue() == startDayOfWeek) {
                                rotationIndex = i;
                                break;
                        }
                        // If startDate is before this day, sessions start from this day
                        if (sortedDays[i].intValue() > startDayOfWeek) {
                                rotationIndex = i;
                                break;
                        }
                }

                // Create rotated schedule: start from the correct day
                Short[] rotatedDays = new Short[sortedDays.length];
                for (int i = 0; i < sortedDays.length; i++) {
                        rotatedDays[i] = sortedDays[(rotationIndex + i) % sortedDays.length];
                }

                LocalDate currentDate = startDate;
                for (int sessionIndex = 0; sessionIndex < subjectSessions.size(); sessionIndex++) {
                        SubjectSession subjectSession = subjectSessions.get(sessionIndex);
                        Short targetDayOfWeek = rotatedDays[sessionIndex % rotatedDays.length];
                        LocalDate sessionDate = findNextDateForDayOfWeek(currentDate, targetDayOfWeek);

                        // Skip holidays - find next available date with same day of week
                        while (vietnamHolidayService.isHoliday(sessionDate)) {
                                log.info("Session date {} is a holiday ({}), moving to next week",
                                                sessionDate, vietnamHolidayService.getHolidayName(sessionDate));
                                sessionDate = sessionDate.plusWeeks(1); // Move to same day next week
                        }

                        Session session = Session.builder()
                                        .classEntity(classEntity)
                                        .subjectSession(subjectSession)
                                        .date(sessionDate)
                                        .type(SessionType.CLASS)
                                        .status(SessionStatus.PLANNED)
                                        .createdAt(now)
                                        .updatedAt(now)
                                        .build();

                        sessions.add(session);
                        currentDate = sessionDate.plusDays(1);
                }

                log.info("Generated {} sessions for class {}", sessions.size(), classEntity.getCode());
                return sessions;
        }

        private LocalDate findNextDateForDayOfWeek(LocalDate fromDate, Short targetDayOfWeek) {
                // Frontend format: 0=Sunday, 1=Monday...6=Saturday
                // Java DayOfWeek: 1=Monday...7=Sunday
                // Convert: if targetDayOfWeek == 0 (Sunday) -> 7, else keep as is
                int javaDayOfWeek = targetDayOfWeek == 0 ? 7 : targetDayOfWeek.intValue();
                DayOfWeek target = DayOfWeek.of(javaDayOfWeek);
                LocalDate current = fromDate;
                while (current.getDayOfWeek() != target) {
                        current = current.plusDays(1);
                }
                return current;
        }

        private LocalDate calculateEndDate(List<Session> sessions) {
                if (sessions == null || sessions.isEmpty()) {
                        return null;
                }
                return sessions.stream()
                                .map(Session::getDate)
                                .max(LocalDate::compareTo)
                                .orElse(null);
        }

        // ==================== GET CLASS SESSIONS (STEP 2) ====================

        public org.fyp.tmssep490be.dtos.classcreation.ClassSessionsOverviewDTO getClassSessions(Long classId,
                        Long userId) {
                log.info("Getting sessions for class {} by user {}", classId, userId);

                ClassEntity classEntity = classRepository.findById(classId)
                                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

                validateClassBranchAccess(classEntity, userId);

                List<Session> sessions = sessionRepository.findByClassEntityIdOrderByDateAsc(classId);

                // Calculate week number for each session based on class schedule
                // Each week = N sessions where N = scheduleDays.length
                LocalDate classStartDate = classEntity.getStartDate();
                if (classStartDate == null && !sessions.isEmpty()) {
                        classStartDate = sessions.get(0).getDate();
                }
                final LocalDate startDate = classStartDate;

                // Get sessions per week from scheduleDays
                int sessionsPerWeek = classEntity.getScheduleDays() != null
                                ? classEntity.getScheduleDays().length
                                : 3; // Default to 3 if not set

                // Group sessions by class week (every N sessions = 1 week)
                Map<Integer, List<Session>> sessionsByWeekNum = new LinkedHashMap<>();
                for (int i = 0; i < sessions.size(); i++) {
                        int weekNum = (i / sessionsPerWeek) + 1;
                        sessionsByWeekNum.computeIfAbsent(weekNum, k -> new ArrayList<>()).add(sessions.get(i));
                }

                // Calculate week ranges
                Map<Integer, String> weekRanges = new HashMap<>();
                for (Map.Entry<Integer, List<Session>> entry : sessionsByWeekNum.entrySet()) {
                        List<Session> weekSessions = entry.getValue();
                        LocalDate firstDate = weekSessions.stream().map(Session::getDate).min(LocalDate::compareTo)
                                        .orElse(null);
                        LocalDate lastDate = weekSessions.stream().map(Session::getDate).max(LocalDate::compareTo)
                                        .orElse(null);
                        weekRanges.put(entry.getKey(), firstDate + " - " + lastDate);
                }

                // Build session DTOs with week info
                List<org.fyp.tmssep490be.dtos.classcreation.ClassSessionsOverviewDTO.SessionDTO> sessionDTOs = new ArrayList<>();
                int sequenceNumber = 1;
                for (Session session : sessions) {
                        boolean hasTimeSlot = session.getTimeSlotTemplate() != null;
                        boolean hasResource = session.getSessionResources() != null
                                        && !session.getSessionResources().isEmpty();
                        boolean hasTeacher = session.getTeachingSlots() != null
                                        && !session.getTeachingSlots().isEmpty();

                        org.fyp.tmssep490be.dtos.classcreation.ClassSessionsOverviewDTO.TimeSlotInfoDTO timeSlotInfo = null;
                        Long timeSlotTemplateId = null;
                        String timeSlotName = null;
                        String timeSlotLabel = null;

                        if (hasTimeSlot) {
                                timeSlotTemplateId = session.getTimeSlotTemplate().getId();
                                timeSlotName = session.getTimeSlotTemplate().getName();
                                timeSlotLabel = session.getTimeSlotTemplate().getStartTime() + " - "
                                                + session.getTimeSlotTemplate().getEndTime();
                                timeSlotInfo = org.fyp.tmssep490be.dtos.classcreation.ClassSessionsOverviewDTO.TimeSlotInfoDTO
                                                .builder()
                                                .id(timeSlotTemplateId)
                                                .name(timeSlotName)
                                                .startTime(session.getTimeSlotTemplate().getStartTime().toString())
                                                .endTime(session.getTimeSlotTemplate().getEndTime().toString())
                                                .build();
                        }

                        // Get resource info
                        Long resourceId = null;
                        String resourceName = null;
                        String resourceDisplayName = null;
                        if (hasResource) {
                                var firstResource = session.getSessionResources().iterator().next();
                                if (firstResource != null && firstResource.getResource() != null) {
                                        resourceId = firstResource.getResource().getId();
                                        resourceName = firstResource.getResource().getName();
                                        resourceDisplayName = firstResource.getResource().getCode() + " - "
                                                        + firstResource.getResource().getName();
                                }
                        }

                        String subjectSessionName = session.getSubjectSession() != null
                                        ? session.getSubjectSession().getTopic()
                                        : "Session " + sequenceNumber;

                        DayOfWeek dayOfWeek = session.getDate().getDayOfWeek();
                        String dayOfWeekVi = switch (dayOfWeek) {
                                case MONDAY -> "Thứ 2";
                                case TUESDAY -> "Thứ 3";
                                case WEDNESDAY -> "Thứ 4";
                                case THURSDAY -> "Thứ 5";
                                case FRIDAY -> "Thứ 6";
                                case SATURDAY -> "Thứ 7";
                                case SUNDAY -> "Chủ nhật";
                        };

                        // Calculate week number for this session (based on session index, not calendar)
                        int weekNum = (sequenceNumber - 1) / sessionsPerWeek + 1;

                        sessionDTOs.add(org.fyp.tmssep490be.dtos.classcreation.ClassSessionsOverviewDTO.SessionDTO
                                        .builder()
                                        .sessionId(session.getId())
                                        .sequenceNumber(sequenceNumber)
                                        .date(session.getDate())
                                        .dayOfWeek(dayOfWeekVi)
                                        .dayOfWeekNumber(dayOfWeek.getValue())
                                        .subjectSessionName(subjectSessionName)
                                        .status(session.getStatus() != null ? session.getStatus().name() : null)
                                        .hasTimeSlot(hasTimeSlot)
                                        .hasResource(hasResource)
                                        .hasTeacher(hasTeacher)
                                        .timeSlotInfo(timeSlotInfo)
                                        .timeSlotTemplateId(timeSlotTemplateId)
                                        .timeSlotName(timeSlotName)
                                        .timeSlotLabel(timeSlotLabel)
                                        .resourceId(resourceId)
                                        .resourceName(resourceName)
                                        .resourceDisplayName(resourceDisplayName)
                                        .weekNumber(weekNum)
                                        .weekRange(weekRanges.get(weekNum))
                                        .build());
                        sequenceNumber++;
                }

                // Build week groups from sessionsByWeekNum (already grouped correctly)
                List<org.fyp.tmssep490be.dtos.classcreation.ClassSessionsOverviewDTO.WeekGroupDTO> weekGroups = new ArrayList<>();
                for (Map.Entry<Integer, List<Session>> entry : sessionsByWeekNum.entrySet()) {
                        int weekNum = entry.getKey();
                        List<Session> weekSessions = entry.getValue();
                        LocalDate firstDate = weekSessions.stream().map(Session::getDate).min(LocalDate::compareTo)
                                        .orElse(null);
                        LocalDate lastDate = weekSessions.stream().map(Session::getDate).max(LocalDate::compareTo)
                                        .orElse(null);
                        String weekRange = firstDate + " - " + lastDate;

                        weekGroups.add(org.fyp.tmssep490be.dtos.classcreation.ClassSessionsOverviewDTO.WeekGroupDTO
                                        .builder()
                                        .weekNumber(weekNum)
                                        .weekRange(weekRange)
                                        .sessionCount(weekSessions.size())
                                        .sessionIds(weekSessions.stream().map(Session::getId).toList())
                                        .build());
                }

                // Build date range
                LocalDate dateRangeStart = sessions.isEmpty() ? classEntity.getStartDate()
                                : sessions.stream().map(Session::getDate).min(LocalDate::compareTo).orElse(null);
                LocalDate dateRangeEnd = sessions.isEmpty() ? classEntity.getPlannedEndDate()
                                : sessions.stream().map(Session::getDate).max(LocalDate::compareTo).orElse(null);

                return org.fyp.tmssep490be.dtos.classcreation.ClassSessionsOverviewDTO.builder()
                                .classId(classEntity.getId())
                                .classCode(classEntity.getCode())
                                .totalSessions(sessions.size())
                                .dateRange(org.fyp.tmssep490be.dtos.classcreation.ClassSessionsOverviewDTO.DateRangeDTO
                                                .builder()
                                                .startDate(dateRangeStart)
                                                .endDate(dateRangeEnd)
                                                .build())
                                .sessions(sessionDTOs)
                                .groupedByWeek(weekGroups)
                                .build();
        }

        // ==================== ASSIGN TIME SLOTS (STEP 3) ====================

        @Transactional
        public org.fyp.tmssep490be.dtos.classcreation.AssignTimeSlotsResponse assignTimeSlots(
                        Long classId,
                        org.fyp.tmssep490be.dtos.classcreation.AssignTimeSlotsRequest request,
                        Long userId) {
                log.info("Assigning time slots for class ID {} by user {}", classId, userId);

                // Get class and validate access
                ClassEntity classEntity = classRepository.findById(classId)
                                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

                validateClassBranchAccess(classEntity, userId);

                // Get total sessions count
                Long totalSessionsLong = sessionRepository.countByClassEntityId(classId);
                int totalSessions = totalSessionsLong != null ? totalSessionsLong.intValue() : 0;
                if (totalSessions == 0) {
                        throw new CustomException(ErrorCode.NO_SESSIONS_FOUND_FOR_CLASS);
                }

                List<org.fyp.tmssep490be.dtos.classcreation.AssignTimeSlotsResponse.AssignmentDetail> assignmentDetails = new ArrayList<>();
                int totalSessionsUpdated = 0;

                // Process each time slot assignment
                for (org.fyp.tmssep490be.dtos.classcreation.AssignTimeSlotsRequest.TimeSlotAssignment assignment : request
                                .getAssignments()) {
                        try {
                                // Validate time slot exists and belongs to class's branch
                                org.fyp.tmssep490be.entities.TimeSlotTemplate timeSlot = timeSlotTemplateRepository
                                                .findById(assignment.getTimeSlotTemplateId())
                                                .orElseThrow(() -> new CustomException(ErrorCode.TIME_SLOT_NOT_FOUND));

                                if (!timeSlot.getBranch().getId().equals(classEntity.getBranch().getId())) {
                                        throw new CustomException(ErrorCode.TIME_SLOT_NOT_IN_BRANCH);
                                }

                                // Update sessions for this day of week
                                int sessionsUpdated = sessionRepository.updateTimeSlotByDayOfWeek(
                                                classId,
                                                assignment.getDayOfWeek().intValue(),
                                                assignment.getTimeSlotTemplateId());

                                // Create assignment detail
                                org.fyp.tmssep490be.dtos.classcreation.AssignTimeSlotsResponse.AssignmentDetail detail = org.fyp.tmssep490be.dtos.classcreation.AssignTimeSlotsResponse.AssignmentDetail
                                                .builder()
                                                .dayOfWeek(assignment.getDayOfWeek())
                                                .dayName(getDayNameVi(assignment.getDayOfWeek()))
                                                .timeSlotTemplateId(assignment.getTimeSlotTemplateId())
                                                .timeSlotName(timeSlot.getName())
                                                .startTime(timeSlot.getStartTime().toString())
                                                .endTime(timeSlot.getEndTime().toString())
                                                .sessionsAffected(sessionsUpdated)
                                                .successful(true)
                                                .build();

                                assignmentDetails.add(detail);
                                totalSessionsUpdated += sessionsUpdated;

                                log.debug("Updated {} sessions for day {} with time slot {} in class {}",
                                                sessionsUpdated, assignment.getDayOfWeek(), timeSlot.getName(),
                                                classEntity.getCode());

                        } catch (CustomException e) {
                                // Create failed assignment detail
                                org.fyp.tmssep490be.dtos.classcreation.AssignTimeSlotsResponse.AssignmentDetail detail = org.fyp.tmssep490be.dtos.classcreation.AssignTimeSlotsResponse.AssignmentDetail
                                                .builder()
                                                .dayOfWeek(assignment.getDayOfWeek())
                                                .dayName(getDayNameVi(assignment.getDayOfWeek()))
                                                .timeSlotTemplateId(assignment.getTimeSlotTemplateId())
                                                .successful(false)
                                                .errorMessage(e.getMessage())
                                                .build();

                                assignmentDetails.add(detail);
                                log.warn("Failed to assign time slot for day {} in class {}: {}",
                                                assignment.getDayOfWeek(), classEntity.getCode(), e.getMessage());
                        }
                }

                // Build response
                return org.fyp.tmssep490be.dtos.classcreation.AssignTimeSlotsResponse.builder()
                                .success(totalSessionsUpdated > 0)
                                .message(String.format("Đã gán khung giờ thành công. %d/%d buổi đã được cập nhật.",
                                                totalSessionsUpdated, totalSessions))
                                .classId(classId)
                                .classCode(classEntity.getCode())
                                .totalSessions(totalSessions)
                                .sessionsUpdated(totalSessionsUpdated)
                                .updatedAt(OffsetDateTime.now())
                                .assignmentDetails(assignmentDetails)
                                .build();
        }

        private String getDayNameVi(Short dayOfWeek) {
                return switch (dayOfWeek.intValue()) {
                        case 0 -> "Chủ nhật";
                        case 1 -> "Thứ 2";
                        case 2 -> "Thứ 3";
                        case 3 -> "Thứ 4";
                        case 4 -> "Thứ 5";
                        case 5 -> "Thứ 6";
                        case 6 -> "Thứ 7";
                        default -> "Unknown";
                };
        }

        // ==================== STEP 4: RESOURCES ====================

        public List<org.fyp.tmssep490be.dtos.classcreation.AvailableResourceDTO> getAvailableResources(
                        Long classId, Long timeSlotId, Short dayOfWeek, Long userId) {
                log.info("Getting available resources for class {} timeSlot {} dayOfWeek {} by user {}",
                                classId, timeSlotId, dayOfWeek, userId);

                ClassEntity classEntity = classRepository.findById(classId)
                                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

                validateClassBranchAccess(classEntity, userId);

                // Determine required resource type based on class modality
                org.fyp.tmssep490be.entities.enums.ResourceType requiredType = classEntity
                                .getModality() == org.fyp.tmssep490be.entities.enums.Modality.ONLINE
                                                ? org.fyp.tmssep490be.entities.enums.ResourceType.VIRTUAL
                                                : org.fyp.tmssep490be.entities.enums.ResourceType.ROOM;

                // Get resources for the branch and filter by resource type AND capacity
                Integer requiredCapacity = classEntity.getMaxCapacity() != null ? classEntity.getMaxCapacity() : 0;
                List<org.fyp.tmssep490be.entities.Resource> resources = resourceRepository
                                .findByBranchIdOrderByNameAsc(classEntity.getBranch().getId())
                                .stream()
                                .filter(r -> r.getResourceType() == requiredType)
                                .filter(r -> r.getCapacity() != null && r.getCapacity() >= requiredCapacity)
                                .toList();

                log.info("Found {} resources of type {} for class modality {}",
                                resources.size(), requiredType, classEntity.getModality());

                // Get sessions for this class and dayOfWeek to calculate conflicts
                List<org.fyp.tmssep490be.entities.Session> classSessions = sessionRepository
                                .findByClassEntityId(classId)
                                .stream()
                                .filter(s -> s.getDate() != null &&
                                                (s.getDate().getDayOfWeek().getValue() % 7) == dayOfWeek.intValue())
                                .filter(s -> s.getTimeSlotTemplate() != null)
                                .toList();

                int totalSessions = classSessions.size();
                Map<Long, Integer> conflictMap = new HashMap<>();

                if (totalSessions > 0) {
                        List<Long> resourceIds = resources.stream().map(org.fyp.tmssep490be.entities.Resource::getId)
                                        .toList();
                        List<LocalDate> dates = classSessions.stream()
                                        .map(org.fyp.tmssep490be.entities.Session::getDate)
                                        .distinct().toList();
                        List<Long> timeSlotIds = classSessions.stream()
                                        .map(s -> s.getTimeSlotTemplate().getId())
                                        .distinct()
                                        .toList();

                        List<Object[]> conflictResults = sessionResourceRepository
                                        .batchCountConflictsByResourcesAcrossAllClasses(
                                                        resourceIds, dates, timeSlotIds, classId);

                        for (Object[] result : conflictResults) {
                                Long rId = ((Number) result[0]).longValue();
                                Integer count = ((Number) result[1]).intValue();
                                conflictMap.put(rId, count);
                        }
                }

                log.info("Calculated conflicts for {} resources across {} sessions", resources.size(), totalSessions);

                // Filter and sort resources
                final int sessionsCount = totalSessions;
                return resources.stream()
                                // Filter out resources with 100% conflict (all sessions are conflicting)
                                .filter(r -> conflictMap.getOrDefault(r.getId(), 0) < sessionsCount)
                                // Sort by: 1) capacity closest to class capacity, 2) name
                                .sorted((r1, r2) -> {
                                        // Sort by capacity difference (ascending) - closest to class capacity first
                                        int diff1 = Math.abs((r1.getCapacity() != null ? r1.getCapacity() : 0)
                                                        - requiredCapacity);
                                        int diff2 = Math.abs((r2.getCapacity() != null ? r2.getCapacity() : 0)
                                                        - requiredCapacity);
                                        int capacityCompare = Integer.compare(diff1, diff2);
                                        if (capacityCompare != 0)
                                                return capacityCompare;
                                        // Then by name
                                        return r1.getName().compareToIgnoreCase(r2.getName());
                                })
                                .map(r -> org.fyp.tmssep490be.dtos.classcreation.AvailableResourceDTO.fromEntity(
                                                r,
                                                conflictMap.getOrDefault(r.getId(), 0),
                                                sessionsCount))
                                .toList();
        }

        /**
         * Assign a specific resource to a single session (used for conflict resolution)
         */
        @Transactional
        public void assignResourceToSession(Long classId, Long sessionId, Long resourceId, Long userId) {
                log.info("Assigning resource {} to session {} of class {} by user {}",
                                resourceId, sessionId, classId, userId);

                ClassEntity classEntity = classRepository.findById(classId)
                                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

                validateClassBranchAccess(classEntity, userId);

                org.fyp.tmssep490be.entities.Session session = sessionRepository.findById(sessionId)
                                .orElseThrow(() -> new CustomException(ErrorCode.NO_SESSIONS_FOUND_FOR_CLASS));

                // Verify session belongs to this class
                if (!session.getClassEntity().getId().equals(classId)) {
                        throw new CustomException(ErrorCode.INVALID_REQUEST);
                }

                org.fyp.tmssep490be.entities.Resource resource = resourceRepository.findById(resourceId)
                                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

                // Verify resource belongs to same branch
                if (!resource.getBranch().getId().equals(classEntity.getBranch().getId())) {
                        throw new CustomException(ErrorCode.RESOURCE_NOT_IN_BRANCH);
                }

                // Delete existing resource assignment for this session
                session.getSessionResources().clear();

                // Create new assignment
                org.fyp.tmssep490be.entities.SessionResource sessionResource = org.fyp.tmssep490be.entities.SessionResource
                                .builder()
                                .id(new org.fyp.tmssep490be.entities.SessionResource.SessionResourceId(sessionId,
                                                resourceId))
                                .session(session)
                                .resource(resource)
                                .build();

                session.getSessionResources().add(sessionResource);
                sessionRepository.save(session);
                log.info("Successfully assigned resource {} to session {}", resourceId, sessionId);
        }

        @Transactional
        public org.fyp.tmssep490be.dtos.classcreation.AssignResourcesResponse assignResources(
                        Long classId,
                        org.fyp.tmssep490be.dtos.classcreation.AssignResourcesRequest request,
                        Long userId) {
                long startTime = System.currentTimeMillis();
                log.info("Starting HYBRID resource assignment for class ID {} by user {}", classId, userId);

                ClassEntity classEntity = classRepository.findById(classId)
                                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

                validateClassBranchAccess(classEntity, userId);

                Long totalSessionsLong = sessionRepository.countByClassEntityId(classId);
                int totalSessions = totalSessionsLong != null ? totalSessionsLong.intValue() : 0;
                if (totalSessions == 0) {
                        throw new CustomException(ErrorCode.NO_SESSIONS_FOUND_FOR_CLASS);
                }

                List<org.fyp.tmssep490be.dtos.classcreation.AssignResourcesResponse.ResourceConflictDetail> conflicts = new ArrayList<>();

                // Validate all resources exist and belong to same branch
                for (org.fyp.tmssep490be.dtos.classcreation.AssignResourcesRequest.ResourceAssignment assignment : request
                                .getPattern()) {
                        org.fyp.tmssep490be.entities.Resource resource = resourceRepository
                                        .findById(assignment.getResourceId())
                                        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

                        if (!resource.getBranch().getId().equals(classEntity.getBranch().getId())) {
                                throw new CustomException(ErrorCode.RESOURCE_NOT_IN_BRANCH);
                        }
                }

                // PHASE 1: Delete existing + SQL Bulk Insert (Replace resources)
                int totalSuccessCount = 0;
                for (org.fyp.tmssep490be.dtos.classcreation.AssignResourcesRequest.ResourceAssignment assignment : request
                                .getPattern()) {
                        // First, delete existing resources for this day of week
                        int deletedCount = sessionResourceRepository.deleteResourcesForDayOfWeek(
                                        classId,
                                        assignment.getDayOfWeek().intValue());
                        log.debug("Phase 1 - Day {}: Deleted {} existing resource assignments",
                                        assignment.getDayOfWeek(), deletedCount);

                        // Then, insert the new resource
                        int assignedCount = sessionResourceRepository.bulkInsertResourcesForDayOfWeek(
                                        classId,
                                        assignment.getDayOfWeek().intValue(),
                                        assignment.getResourceId());
                        totalSuccessCount += assignedCount;

                        log.debug("Phase 1 - Day {}: Assigned {} sessions to Resource ID: {}",
                                        assignment.getDayOfWeek(), assignedCount, assignment.getResourceId());
                }

                log.info("Phase 1 complete: {}/{} sessions assigned successfully", totalSuccessCount, totalSessions);

                // PHASE 2: Java Conflict Analysis (Detailed Path - find sessions that couldn't
                // be assigned)
                for (org.fyp.tmssep490be.dtos.classcreation.AssignResourcesRequest.ResourceAssignment assignment : request
                                .getPattern()) {
                        org.fyp.tmssep490be.entities.Resource resource = resourceRepository
                                        .findById(assignment.getResourceId()).get();

                        // Find sessions with resource conflicts for this day of week
                        List<Object[]> conflictingSessions = sessionResourceRepository.findSessionsWithResourceConflict(
                                        classId,
                                        assignment.getDayOfWeek().intValue(),
                                        assignment.getResourceId());

                        log.debug("Phase 2 - Day {}: Found {} sessions with conflicts",
                                        assignment.getDayOfWeek(), conflictingSessions.size());

                        // Analyze each conflicting session and build conflict details
                        for (Object[] conflictData : conflictingSessions) {
                                Long sessionId = ((Number) conflictData[0]).longValue();
                                LocalDate sessionDate = conflictData[1] instanceof java.sql.Date
                                                ? ((java.sql.Date) conflictData[1]).toLocalDate()
                                                : (LocalDate) conflictData[1];
                                Long timeSlotId = conflictData[2] != null ? ((Number) conflictData[2]).longValue()
                                                : null;
                                Long conflictingClassId = conflictData[4] != null
                                                ? ((Number) conflictData[4]).longValue()
                                                : null;

                                // Get detailed conflict information
                                Object[] conflictDetails = sessionResourceRepository.findConflictingSessionDetails(
                                                sessionId, assignment.getResourceId());

                                String conflictingClassName = null;
                                java.time.LocalTime timeStart = null;
                                java.time.LocalTime timeEnd = null;

                                if (conflictDetails != null && conflictDetails.length >= 3) {
                                        conflictingClassName = (String) conflictDetails[2];
                                        if (conflictDetails.length > 4 && conflictDetails[4] != null) {
                                                timeStart = (java.time.LocalTime) conflictDetails[4];
                                        }
                                        if (conflictDetails.length > 5 && conflictDetails[5] != null) {
                                                timeEnd = (java.time.LocalTime) conflictDetails[5];
                                        }
                                }

                                String conflictReason = String.format(
                                                "Tài nguyên '%s' đã được sử dụng bởi lớp '%s' vào ngày %s lúc %s-%s",
                                                resource.getName(),
                                                conflictingClassName != null ? conflictingClassName : "khác",
                                                sessionDate,
                                                timeStart != null ? timeStart : "N/A",
                                                timeEnd != null ? timeEnd : "N/A");

                                log.warn("CONFLICT DETECTED! Resource {} is already used by class {} on {} at time slot {}",
                                                resource.getId(), conflictingClassName, sessionDate, timeSlotId);

                                conflicts.add(org.fyp.tmssep490be.dtos.classcreation.AssignResourcesResponse.ResourceConflictDetail
                                                .builder()
                                                .sessionId(sessionId)
                                                .date(sessionDate)
                                                .dayOfWeek(assignment.getDayOfWeek())
                                                .timeSlotTemplateId(timeSlotId)
                                                .timeSlotStart(timeStart)
                                                .timeSlotEnd(timeEnd)
                                                .requestedResourceId(resource.getId())
                                                .requestedResourceName(resource.getName())
                                                .conflictType(org.fyp.tmssep490be.dtos.classcreation.AssignResourcesResponse.ConflictType.CLASS_BOOKING)
                                                .conflictReason(conflictReason)
                                                .conflictingClassId(conflictingClassId)
                                                .conflictingClassName(conflictingClassName)
                                                .build());
                        }
                }

                log.info("Phase 2 complete: {} conflicts detected", conflicts.size());

                long processingTime = System.currentTimeMillis() - startTime;
                log.info("HYBRID resource assignment completed in {}ms", processingTime);

                return org.fyp.tmssep490be.dtos.classcreation.AssignResourcesResponse.builder()
                                .classId(classId)
                                .totalSessions(totalSessions)
                                .successCount(totalSuccessCount)
                                .conflictCount(conflicts.size())
                                .conflicts(conflicts)
                                .processingTimeMs(processingTime)
                                .build();
        }

        // ==================== STEP 5: VALIDATE & SUBMIT ====================

        public org.fyp.tmssep490be.dtos.classcreation.ValidateClassResponse validateClass(Long classId, Long userId) {
                log.info("Validating class ID: {} by user ID: {}", classId, userId);

                ClassEntity classEntity = classRepository.findById(classId)
                                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

                validateClassBranchAccess(classEntity, userId);

                // Get session counts
                Long totalSessions = sessionRepository.countByClassEntityId(classId);
                if (totalSessions == null || totalSessions == 0) {
                        return org.fyp.tmssep490be.dtos.classcreation.ValidateClassResponse.builder()
                                        .valid(false)
                                        .canSubmit(false)
                                        .classId(classId)
                                        .message("Không có buổi học nào được khởi tạo")
                                        .errors(List.of("Lớp học chưa có buổi học"))
                                        .build();
                }

                // Count sessions with time slots (TimeSlotTemplate != null)
                List<Session> allSessions = sessionRepository.findByClassEntityIdOrderByDateAsc(classId);
                long sessionsWithTimeSlots = allSessions.stream()
                                .filter(s -> s.getTimeSlotTemplate() != null)
                                .count();
                long sessionsWithResources = allSessions.stream()
                                .filter(s -> s.getSessionResources() != null && !s.getSessionResources().isEmpty())
                                .count();

                long sessionsWithoutTimeSlots = totalSessions - sessionsWithTimeSlots;
                long sessionsWithoutResources = totalSessions - sessionsWithResources;

                boolean allSessionsHaveTimeSlots = sessionsWithoutTimeSlots == 0;
                boolean allSessionsHaveResources = sessionsWithoutResources == 0;

                // Calculate completion percentage (only time slots and resources)
                long completedChecks = (allSessionsHaveTimeSlots ? 1 : 0) +
                                (allSessionsHaveResources ? 1 : 0);
                int completionPercentage = (int) (completedChecks * 100 / 2);

                // Only check time slots and resources (no teacher check)
                boolean valid = allSessionsHaveTimeSlots && allSessionsHaveResources;
                boolean canSubmit = valid;

                List<String> errors = new ArrayList<>();
                if (!allSessionsHaveTimeSlots) {
                        errors.add(String.format("Còn %d buổi chưa có khung giờ", sessionsWithoutTimeSlots));
                }
                if (!allSessionsHaveResources) {
                        errors.add(String.format("Còn %d buổi chưa có tài nguyên", sessionsWithoutResources));
                }

                String message = valid ? "Lớp học đã sẵn sàng để gửi duyệt"
                                : "Lớp học chưa hoàn tất các cấu hình cần thiết";

                return org.fyp.tmssep490be.dtos.classcreation.ValidateClassResponse.builder()
                                .valid(valid)
                                .canSubmit(canSubmit)
                                .classId(classId)
                                .message(message)
                                .checks(org.fyp.tmssep490be.dtos.classcreation.ValidateClassResponse.ValidationChecks
                                                .builder()
                                                .totalSessions(totalSessions)
                                                .sessionsWithTimeSlots(sessionsWithTimeSlots)
                                                .sessionsWithResources(sessionsWithResources)
                                                .sessionsWithoutTimeSlots(sessionsWithoutTimeSlots)
                                                .sessionsWithoutResources(sessionsWithoutResources)
                                                .completionPercentage(completionPercentage)
                                                .allSessionsHaveTimeSlots(allSessionsHaveTimeSlots)
                                                .allSessionsHaveResources(allSessionsHaveResources)
                                                .build())
                                .errors(errors)
                                .warnings(List.of())
                                .build();
        }

        @Transactional
        public org.fyp.tmssep490be.dtos.classcreation.SubmitClassResponse submitClass(Long classId, Long userId) {
                log.info("Submitting class ID: {} for approval by user: {}", classId, userId);

                ClassEntity classEntity = classRepository.findById(classId)
                                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

                validateClassBranchAccess(classEntity, userId);

                // Delegate to ApprovalService (which handles validation, status update, and
                // notifications)
                return approvalService.submitForApproval(classId, userId);
        }

        // ==================== WIZARD RESET ====================

        /**
         * Reset wizard step data when user navigates back and makes changes.
         * This clears time slots (Step 3) and resources (Step 4) assignments.
         *
         * @param classId  the class ID
         * @param fromStep the step user is resetting from (e.g., 3 = clear Step 3+4, 4
         *                 = clear Step 4 only)
         * @param userId   the user ID for access validation
         * @return summary of reset operations
         */
        @Transactional
        public Map<String, Object> resetWizardSteps(Long classId, Integer fromStep, Long userId) {
                log.info("Resetting wizard steps from step {} for class {} by user {}", fromStep, classId, userId);

                ClassEntity classEntity = classRepository.findById(classId)
                                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

                validateClassBranchAccess(classEntity, userId);

                // Only DRAFT classes can be reset
                if (classEntity.getStatus() != ClassStatus.DRAFT) {
                        throw new CustomException(ErrorCode.CLASS_NOT_EDITABLE);
                }

                int timeSlotsCleared = 0;
                int resourcesCleared = 0;

                // Reset from Step 3 or earlier: clear time slots and resources
                if (fromStep != null && fromStep <= 3) {
                        timeSlotsCleared = sessionRepository.clearTimeSlotsForClass(classId);
                        resourcesCleared = sessionResourceRepository.deleteAllByClassId(classId);
                        log.info("Reset from Step 3: cleared {} time slots and {} resources for class {}",
                                        timeSlotsCleared, resourcesCleared, classId);
                }
                // Reset from Step 4: clear only resources
                else if (fromStep != null && fromStep == 4) {
                        resourcesCleared = sessionResourceRepository.deleteAllByClassId(classId);
                        log.info("Reset from Step 4: cleared {} resources for class {}", resourcesCleared, classId);
                }

                return Map.of(
                                "classId", classId,
                                "fromStep", fromStep,
                                "timeSlotsCleared", timeSlotsCleared,
                                "resourcesCleared", resourcesCleared,
                                "message", "Wizard steps reset successfully");
        }

}
