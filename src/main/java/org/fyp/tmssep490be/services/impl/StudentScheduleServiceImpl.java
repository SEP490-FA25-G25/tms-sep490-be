package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.schedule.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.repositories.StudentRepository;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.fyp.tmssep490be.services.StudentScheduleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

        // 5. Extract unique timeslots
        List<TimeSlotTemplate> timeSlots = studentSessions.stream()
                .map(ss -> ss.getSession().getTimeSlotTemplate())
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparing(TimeSlotTemplate::getStartTime))
                .toList();

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
                .timeSlots(timeSlots.stream().map(this::mapToTimeSlotDTO).toList())
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

        // 5. Extract unique timeslots
        List<TimeSlotTemplate> timeSlots = studentSessions.stream()
                .map(ss -> ss.getSession().getTimeSlotTemplate())
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparing(TimeSlotTemplate::getStartTime))
                .toList();

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
                .timeSlots(timeSlots.stream().map(this::mapToTimeSlotDTO).toList())
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
                .attendanceStatus(studentSession.getAttendanceStatus())
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
                .attendanceStatus(studentSession.getAttendanceStatus())
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
}