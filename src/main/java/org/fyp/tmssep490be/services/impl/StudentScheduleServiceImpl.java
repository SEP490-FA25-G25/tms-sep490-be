package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.schedule.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;
import org.fyp.tmssep490be.repositories.EnrollmentRepository;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.repositories.StudentRepository;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.fyp.tmssep490be.repositories.TimeSlotTemplateRepository;
import org.fyp.tmssep490be.services.StudentScheduleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudentScheduleServiceImpl implements StudentScheduleService {

    private final StudentRepository studentRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final SessionRepository sessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TimeSlotTemplateRepository timeSlotTemplateRepository;

    @Override
    public WeeklyScheduleResponseDTO getWeeklySchedule(Long studentId, LocalDate weekStart) {
        log.info("Getting weekly schedule for student: {}, week: {}", studentId, weekStart);

        // 1. Validate weekStart is Monday
        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        // 2. Calculate week range
        LocalDate weekEnd = weekStart.plusDays(6);

        // 3. Fetch student
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDENT_NOT_FOUND));

        // 4. Fetch all sessions for this week (with JOIN FETCH)
        List<StudentSession> studentSessions = studentSessionRepository
                .findWeeklyScheduleByStudentId(studentId, weekStart, weekEnd);

        log.debug("Found {} sessions for week {} to {}", studentSessions.size(), weekStart, weekEnd);

        // 5. Get all time slots from all branches with active enrollments (Union + Merge)
        List<TimeSlotDTO> timeSlots = getAllTimeSlotsForStudent(studentId);

        // 6. Group by day of week
        Map<DayOfWeek, List<SessionSummaryDTO>> scheduleMap = studentSessions.stream()
                .collect(Collectors.groupingBy(
                        ss -> ss.getSession().getDate().getDayOfWeek(),
                        Collectors.mapping(this::mapToSessionSummaryDTO, Collectors.toList())
                ));

        // 7. Ensure all days exist in map (even if empty)
        for (DayOfWeek day : DayOfWeek.values()) {
            scheduleMap.putIfAbsent(day, new ArrayList<>());
        }

        // 8. Build response
        return WeeklyScheduleResponseDTO.builder()
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .studentId(student.getId())
                .studentName(student.getUserAccount().getFullName())
                .timeSlots(timeSlots)
                .schedule(scheduleMap)
                .build();
    }

    @Override
    public WeeklyScheduleResponseDTO getWeeklyScheduleByClass(Long studentId, Long classId, LocalDate weekStart) {
        log.info("Getting weekly schedule for student: {}, class: {}, week: {}", studentId, classId, weekStart);

        // 1. Validate weekStart is Monday
        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        // 2. Calculate week range
        LocalDate weekEnd = weekStart.plusDays(6);

        // 3. Fetch student
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDENT_NOT_FOUND));

        // 4. Fetch all sessions for this week filtered by class (with JOIN FETCH)
        List<StudentSession> studentSessions = studentSessionRepository
                .findWeeklyScheduleByStudentIdAndClassId(studentId, classId, weekStart, weekEnd);

        log.debug("Found {} sessions for student {} in class {} for week {} to {}",
                studentSessions.size(), studentId, classId, weekStart, weekEnd);

        // 5. Get all time slots from all branches with active enrollments (Union + Merge)
        List<TimeSlotDTO> timeSlots = getAllTimeSlotsForStudent(studentId);

        // 6. Group by day of week
        Map<DayOfWeek, List<SessionSummaryDTO>> scheduleMap = studentSessions.stream()
                .collect(Collectors.groupingBy(
                        ss -> ss.getSession().getDate().getDayOfWeek(),
                        Collectors.mapping(this::mapToSessionSummaryDTO, Collectors.toList())
                ));

        // 7. Ensure all days exist in map (even if empty)
        for (DayOfWeek day : DayOfWeek.values()) {
            scheduleMap.putIfAbsent(day, new ArrayList<>());
        }

        // 8. Build response
        return WeeklyScheduleResponseDTO.builder()
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .studentId(student.getId())
                .studentName(student.getUserAccount().getFullName())
                .timeSlots(timeSlots)
                .schedule(scheduleMap)
                .build();
    }

    @Override
    public SessionDetailDTO getSessionDetail(Long studentId, Long sessionId) {
        log.info("Getting session detail for student: {}, session: {}", studentId, sessionId);

        // 1. Fetch with authorization check
        StudentSession studentSession = studentSessionRepository
                .findByStudentIdAndSessionId(studentId, sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDENT_SESSION_NOT_FOUND));

        return mapToSessionDetailDTO(studentSession);
    }

    @Override
    public LocalDate getCurrentWeekStart() {
        LocalDate today = LocalDate.now();
        return today.minusDays(today.getDayOfWeek().getValue() - 1);
    }

    /**
     * Get all time slots for a student by unioning time slots from all branches
     * where the student has active enrollments, then merging duplicates by time range
     */
    private List<TimeSlotDTO> getAllTimeSlotsForStudent(Long studentId) {
        log.debug("Getting all time slots for student: {}", studentId);

        // 1. Get all active enrollments for student
        List<Enrollment> activeEnrollments = enrollmentRepository
                .findByStudentIdAndStatus(studentId, EnrollmentStatus.ENROLLED);

        if (activeEnrollments.isEmpty()) {
            // Instead of failing the whole API, just return an empty list.
            // This allows the frontend to display a friendly "no classes" state
            // for students who have not been enrolled in any class yet.
            log.warn("Student {} has no active enrollments. Returning empty time slot list.", studentId);
            return Collections.emptyList();
        }

        // 2. Extract unique branch IDs from enrollments
        Set<Long> branchIds = activeEnrollments.stream()
                .map(e -> e.getClassEntity().getBranch().getId())
                .collect(Collectors.toSet());

        log.debug("Student {} has active enrollments in {} branches", studentId, branchIds.size());

        // 3. Union time slots from all branches
        List<TimeSlotTemplate> allTimeSlots = branchIds.stream()
                .flatMap(branchId -> timeSlotTemplateRepository
                        .findByBranchIdOrderByStartTimeAsc(branchId).stream())
                .collect(Collectors.toList());

        // 4. Group by TimeRange (startTime, endTime) to merge duplicates
        Map<TimeRange, List<TimeSlotTemplate>> groupedByTimeRange = allTimeSlots.stream()
                .collect(Collectors.groupingBy(
                        ts -> new TimeRange(ts.getStartTime(), ts.getEndTime())
                ));

        // 5. Merge duplicates and build DTOs
        List<TimeSlotDTO> mergedTimeSlots = groupedByTimeRange.entrySet().stream()
                .map(entry -> {
                    TimeRange timeRange = entry.getKey();
                    List<TimeSlotTemplate> slots = entry.getValue();

                    // If multiple time slots with same time range, merge their names
                    String mergedName;
                    Long timeSlotTemplateId;
                    if (slots.size() == 1) {
                        TimeSlotTemplate slot = slots.get(0);
                        mergedName = slot.getName();
                        timeSlotTemplateId = slot.getId();
                    } else {
                        // Merge names from different branches (e.g., "HN Morning 1 / SG Morning 1")
                        mergedName = slots.stream()
                                .map(TimeSlotTemplate::getName)
                                .distinct()
                                .collect(Collectors.joining(" / "));
                        timeSlotTemplateId = slots.get(0).getId(); // Use first one as representative
                    }

                    return TimeSlotDTO.builder()
                            .timeSlotTemplateId(timeSlotTemplateId)
                            .name(mergedName)
                            .startTime(timeRange.getStartTime())
                            .endTime(timeRange.getEndTime())
                            .build();
                })
                .sorted(Comparator.comparing(TimeSlotDTO::getStartTime))
                .collect(Collectors.toList());

        log.debug("Merged {} unique time slots for student {}", mergedTimeSlots.size(), studentId);
        return mergedTimeSlots;
    }

    /**
     * Helper class to group time slots by time range (startTime, endTime)
     */
    @lombok.Value
    private static class TimeRange {
        LocalTime startTime;
        LocalTime endTime;
    }

    private SessionSummaryDTO mapToSessionSummaryDTO(StudentSession studentSession) {
        Session session = studentSession.getSession();
        ClassEntity classEntity = session.getClassEntity();
        CourseSession courseSession = session.getCourseSession();
        TimeSlotTemplate timeSlot = session.getTimeSlotTemplate();

        MakeupInfoDTO makeupInfo = null;
        if (studentSession.getIsMakeup() != null && studentSession.getIsMakeup()) {
            makeupInfo = buildMakeupInfo(studentSession);
        }

        return SessionSummaryDTO.builder()
                .sessionId(session.getId())
                .studentSessionId(studentSession.getId().getSessionId())
                .classId(classEntity.getId())
                .date(session.getDate())
                .dayOfWeek(session.getDate().getDayOfWeek())
                .timeSlotTemplateId(timeSlot != null ? timeSlot.getId() : null)
                .startTime(timeSlot != null ? timeSlot.getStartTime() : null)
                .endTime(timeSlot != null ? timeSlot.getEndTime() : null)
                .classCode(classEntity.getCode())
                .className(classEntity.getName())
                .courseId(classEntity.getCourse().getId())
                .courseName(classEntity.getCourse().getName())
                .topic(courseSession != null ? courseSession.getTopic() : null)
                .sessionType(session.getType())
                .sessionStatus(session.getStatus())
                .modality(classEntity.getModality())
                .location(determineLocationForSummary(classEntity, session))
                .branchName(classEntity.getBranch().getName())
                .attendanceStatus(resolveDisplayStatus(studentSession))
                .isMakeup(studentSession.getIsMakeup() != null ? studentSession.getIsMakeup() : false)
                .makeupInfo(makeupInfo)
                .build();
    }

    private SessionDetailDTO mapToSessionDetailDTO(StudentSession studentSession) {
        Session session = studentSession.getSession();
        TimeSlotTemplate timeSlot = session.getTimeSlotTemplate();
        ClassEntity classEntity = session.getClassEntity();
        CourseSession courseSession = session.getCourseSession();

        // Get teacher from teaching slots
        String teacherName = session.getTeachingSlots().stream()
                .filter(ts -> ts.getTeacher() != null)
                .map(ts -> ts.getTeacher().getUserAccount().getFullName())
                .findFirst()
                .orElse(null);

        Long teacherId = session.getTeachingSlots().stream()
                .filter(ts -> ts.getTeacher() != null)
                .map(ts -> ts.getTeacher().getId())
                .findFirst()
                .orElse(null);

        // Build ClassInfoDTO
        ClassInfoDTO classInfo = ClassInfoDTO.builder()
                .classId(classEntity.getId())
                .classCode(classEntity.getCode())
                .className(classEntity.getName())
                .courseId(classEntity.getCourse().getId())
                .courseName(classEntity.getCourse().getName())
                .teacherId(teacherId)
                .teacherName(teacherName)
                .branchId(classEntity.getBranch().getId())
                .branchName(classEntity.getBranch().getName())
                .modality(classEntity.getModality())
                .build();

        // Build SessionInfoDTO
        SessionInfoDTO sessionInfo = SessionInfoDTO.builder()
                .topic(courseSession != null ? courseSession.getTopic() : null)
                .description(courseSession != null ? courseSession.getStudentTask() : null) // Use studentTask as description
                .sessionType(session.getType())
                .sessionStatus(session.getStatus())
                .location(determineLocation(classEntity, session))
                .onlineLink(null) // Will be determined by session resources or class details
                .build();

        // Build StudentStatusDTO
        StudentStatusDTO studentStatus = StudentStatusDTO.builder()
                .attendanceStatus(resolveDisplayStatus(studentSession))
                .homeworkStatus(studentSession.getHomeworkStatus())
                .homeworkDueDate(null) // Not available in current schema
                .homeworkDescription(studentSession.getNote()) // Use student session note
                .build();

        // Extract learning materials from course materials
        List<MaterialDTO> materials = new ArrayList<>();
        if (courseSession != null && !courseSession.getCourseMaterials().isEmpty()) {
            materials = courseSession.getCourseMaterials().stream()
                    .map(this::mapToMaterialDTO)
                    .collect(Collectors.toList());
        }

        // Extract classroom resource (room, zoom, etc.)
        ResourceDTO classroomResource = null;
        if (!session.getSessionResources().isEmpty()) {
            classroomResource = session.getSessionResources().stream()
                    .findFirst()
                    .map(this::mapToResourceDTO)
                    .orElse(null);
        }

        // Build MakeupInfoDTO if applicable
        MakeupInfoDTO makeupInfo = null;
        if (studentSession.getIsMakeup() != null && studentSession.getIsMakeup()) {
            makeupInfo = buildMakeupInfo(studentSession);
        }

        return SessionDetailDTO.builder()
                .sessionId(session.getId())
                .studentSessionId(studentSession.getId().getSessionId())
                .date(session.getDate())
                .dayOfWeek(session.getDate().getDayOfWeek())
                .startTime(timeSlot != null ? timeSlot.getStartTime() : null)
                .endTime(timeSlot != null ? timeSlot.getEndTime() : null)
                .timeSlotName(timeSlot != null ? timeSlot.getName() : null)
                .classInfo(classInfo)
                .sessionInfo(sessionInfo)
                .studentStatus(studentStatus)
                .materials(materials)
                .classroomResource(classroomResource)
                .makeupInfo(makeupInfo)
                .build();
    }

    private TimeSlotDTO mapToTimeSlotDTO(TimeSlotTemplate timeSlot) {
        return TimeSlotDTO.builder()
                .timeSlotTemplateId(timeSlot.getId())
                .name(timeSlot.getName())
                .startTime(timeSlot.getStartTime())
                .endTime(timeSlot.getEndTime())
                .build();
    }

    private MaterialDTO mapToMaterialDTO(CourseMaterial courseMaterial) {
        return MaterialDTO.builder()
                .materialId(courseMaterial.getId())
                .fileName(courseMaterial.getTitle())  // Use course material title instead of resource name
                .fileUrl(courseMaterial.getUrl())      // Use course material URL
                .uploadedAt(courseMaterial.getCreatedAt() != null ?
                        courseMaterial.getCreatedAt().toLocalDateTime() : LocalDateTime.now())
                .build();
    }

    private ResourceDTO mapToResourceDTO(SessionResource sessionResource) {
        return ResourceDTO.builder()
                .resourceId(sessionResource.getResource().getId())
                .resourceCode(sessionResource.getResource().getCode())
                .resourceName(sessionResource.getResource().getName())
                .resourceType(sessionResource.getResource().getResourceType())
                .capacity(sessionResource.getResource().getCapacity())
                .location(determineResourceLocation(sessionResource.getResource()))
                .onlineLink(determineResourceOnlineLink(sessionResource.getResource()))
                .build();
    }

    private String determineResourceLocation(Resource resource) {
        if (resource.getResourceType() == org.fyp.tmssep490be.entities.enums.ResourceType.ROOM) {
            return resource.getName() + ", " + resource.getBranch().getName();
        } else {
            return "Online"; // For virtual resources
        }
    }

    private String determineResourceOnlineLink(Resource resource) {
        if (resource.getResourceType() == org.fyp.tmssep490be.entities.enums.ResourceType.VIRTUAL) {
            return resource.getName(); // For zoom links, name contains the URL
        }
        return null;
    }

    private String determineLocation(ClassEntity classEntity, Session session) {
        if (classEntity.getModality() == org.fyp.tmssep490be.entities.enums.Modality.OFFLINE) {
            return classEntity.getBranch().getName();
        } else if (classEntity.getModality() == org.fyp.tmssep490be.entities.enums.Modality.ONLINE) {
            return "Online"; // Since ClassEntity doesn't have onlineLink field, just return "Online"
        } else {
            return "Hybrid";
        }
    }

    private String determineLocationForSummary(ClassEntity classEntity, Session session) {
        // Get specific resource information for better location display in weekly view
        if (!session.getSessionResources().isEmpty()) {
            Resource resource = session.getSessionResources().iterator().next().getResource();
            if (resource.getResourceType() == org.fyp.tmssep490be.entities.enums.ResourceType.ROOM) {
                return resource.getName(); // Show room name like "Room 101"
            } else if (resource.getResourceType() == org.fyp.tmssep490be.entities.enums.ResourceType.VIRTUAL) {
                return "Online";
            }
        }
        return determineLocation(classEntity, session);
    }

    private MakeupInfoDTO buildMakeupInfo(StudentSession studentSession) {
        Session originalSession = studentSession.getOriginalSession();
        if (originalSession == null) {
            return null;
        }

        return MakeupInfoDTO.builder()
                .isMakeup(true)
                .originalSessionId(originalSession.getId())
                .originalDate(originalSession.getDate())
                .originalStatus(originalSession.getStatus())
                .reason("Session rescheduled")
                .makeupDate(studentSession.getSession().getDate())
                .build();
    }

    /**
     * Hiển thị trạng thái cho học viên trong lịch/chi tiết:
     * - Nếu ngày session < hôm nay và status là null hoặc PLANNED → ABSENT (vắng không phép)
     * - Ngược lại dùng status thực tế (PRESENT / ABSENT / EXCUSED / PLANNED / LATE)
     */
    private AttendanceStatus resolveDisplayStatus(StudentSession studentSession) {
        Session session = studentSession.getSession();
        if (session == null) {
            return studentSession.getAttendanceStatus();
        }
        AttendanceStatus status = studentSession.getAttendanceStatus();
        LocalDate today = LocalDate.now();

        if (session.getDate() != null
                && session.getDate().isBefore(today)
                && (status == null || status == AttendanceStatus.PLANNED)) {
            return AttendanceStatus.ABSENT;
        }
        return status;
    }
}
