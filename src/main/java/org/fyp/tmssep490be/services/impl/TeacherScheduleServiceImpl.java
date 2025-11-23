package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.schedule.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.fyp.tmssep490be.repositories.TeacherRepository;
import org.fyp.tmssep490be.repositories.TeachingSlotRepository;
import org.fyp.tmssep490be.services.TeacherScheduleService;
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
public class TeacherScheduleServiceImpl implements TeacherScheduleService {

    private final TeacherRepository teacherRepository;
    private final TeachingSlotRepository teachingSlotRepository;
    private final StudentSessionRepository studentSessionRepository;

    @Override
    @Transactional(readOnly = true)
    public TeacherWeeklyScheduleResponseDTO getWeeklySchedule(Long teacherId, LocalDate weekStart) {
        log.info("Getting weekly schedule for teacher: {}, week: {}", teacherId, weekStart);

        // 1. Validate weekStart is Monday
        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        // 3. Calculate week range
        LocalDate weekEnd = weekStart.plusDays(6);

        // 4. Fetch teacher
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        // 5. Fetch all teaching slots for this week (with JOIN FETCH)
        List<TeachingSlot> teachingSlots = teachingSlotRepository
                .findWeeklyScheduleByTeacherId(teacherId, weekStart, weekEnd);

        log.debug("Found {} teaching slots for week {} to {}", teachingSlots.size(), weekStart, weekEnd);

        // 5. Extract unique timeslots
        List<TimeSlotTemplate> timeSlots = teachingSlots.stream()
                .map(ts -> ts.getSession().getTimeSlotTemplate())
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparing(TimeSlotTemplate::getStartTime))
                .toList();

        // 6. Group by day of week and map to DTOs
        Map<DayOfWeek, List<TeacherSessionSummaryDTO>> scheduleMap = teachingSlots.stream()
                .collect(Collectors.groupingBy(
                        ts -> ts.getSession().getDate().getDayOfWeek(),
                        Collectors.mapping(this::mapToTeacherSessionSummaryDTO, Collectors.toList())
                ));

        // 7. Ensure all days exist in map (even if empty)
        for (DayOfWeek day : DayOfWeek.values()) {
            scheduleMap.putIfAbsent(day, new ArrayList<>());
        }

        // 8. Build response
        return TeacherWeeklyScheduleResponseDTO.builder()
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .teacherId(teacher.getId())
                .teacherName(teacher.getUserAccount().getFullName())
                .timeSlots(timeSlots.stream().map(this::mapToTimeSlotDTO).toList())
                .schedule(scheduleMap)
                .build();
    }

    @Override
    @Transactional
    public TeacherWeeklyScheduleResponseDTO getWeeklyScheduleByClass(Long teacherId, Long classId, LocalDate weekStart) {
        log.info("Getting weekly schedule for teacher: {}, class: {}, week: {}", teacherId, classId, weekStart);

        // 1. Validate weekStart is Monday
        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        // 3. Calculate week range
        LocalDate weekEnd = weekStart.plusDays(6);

        // 4. Fetch teacher
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        // 5. Fetch all teaching slots for this week filtered by class (with JOIN FETCH)
        List<TeachingSlot> teachingSlots = teachingSlotRepository
                .findWeeklyScheduleByTeacherIdAndClassId(teacherId, classId, weekStart, weekEnd);

        log.debug("Found {} teaching slots for teacher {} in class {} for week {} to {}",
                teachingSlots.size(), teacherId, classId, weekStart, weekEnd);

        // 5. Extract unique timeslots
        List<TimeSlotTemplate> timeSlots = teachingSlots.stream()
                .map(ts -> ts.getSession().getTimeSlotTemplate())
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparing(TimeSlotTemplate::getStartTime))
                .toList();

        // 6. Group by day of week
        Map<DayOfWeek, List<TeacherSessionSummaryDTO>> scheduleMap = teachingSlots.stream()
                .collect(Collectors.groupingBy(
                        ts -> ts.getSession().getDate().getDayOfWeek(),
                        Collectors.mapping(this::mapToTeacherSessionSummaryDTO, Collectors.toList())
                ));

        // 7. Ensure all days exist in map (even if empty)
        for (DayOfWeek day : DayOfWeek.values()) {
            scheduleMap.putIfAbsent(day, new ArrayList<>());
        }

        // 8. Build response
        return TeacherWeeklyScheduleResponseDTO.builder()
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .teacherId(teacher.getId())
                .teacherName(teacher.getUserAccount().getFullName())
                .timeSlots(timeSlots.stream().map(this::mapToTimeSlotDTO).toList())
                .schedule(scheduleMap)
                .build();
    }

    @Override
    @Transactional
    public TeacherSessionDetailDTO getSessionDetail(Long teacherId, Long sessionId) {
        log.info("Getting session detail for teacher: {}, session: {}", teacherId, sessionId);

        // 1. Fetch with authorization check
        TeachingSlot teachingSlot = teachingSlotRepository
                .findByTeacherIdAndSessionId(teacherId, sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        return mapToTeacherSessionDetailDTO(teachingSlot);
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDate getCurrentWeekStart() {
        LocalDate today = LocalDate.now();
        return today.minusDays(today.getDayOfWeek().getValue() - 1);
    }

    private TeacherSessionSummaryDTO mapToTeacherSessionSummaryDTO(TeachingSlot teachingSlot) {
        Session session = teachingSlot.getSession();
        ClassEntity classEntity = session.getClassEntity();
        CourseSession courseSession = session.getCourseSession();
        TimeSlotTemplate timeSlot = session.getTimeSlotTemplate();

        // Calculate attendance statistics
        Long sessionId = session.getId();
        long totalStudents = studentSessionRepository.countBySessionId(sessionId);
        long presentCount = studentSessionRepository.countBySessionIdAndAttendanceStatus(sessionId, AttendanceStatus.PRESENT);
        long absentCount = studentSessionRepository.countBySessionIdAndAttendanceStatus(sessionId, AttendanceStatus.ABSENT);
        
        // Check if attendance has been submitted (session status is DONE or has any attendance records)
        boolean attendanceSubmitted = session.getStatus() == org.fyp.tmssep490be.entities.enums.SessionStatus.DONE
                || presentCount > 0 || absentCount > 0;

        // Check if this is a makeup session (check if any student session has isMakeup = true)
        boolean isMakeup = studentSessionRepository.existsBySessionIdAndIsMakeup(sessionId, true);

        MakeupInfoDTO makeupInfo = null;
        if (isMakeup) {
            makeupInfo = buildMakeupInfo(session);
        }

        return TeacherSessionSummaryDTO.builder()
                .sessionId(session.getId())
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
                .totalStudents((int) totalStudents)
                .presentCount((int) presentCount)
                .absentCount((int) absentCount)
                .attendanceSubmitted(attendanceSubmitted)
                .isMakeup(isMakeup)
                .makeupInfo(makeupInfo)
                .build();
    }

    private TeacherSessionDetailDTO mapToTeacherSessionDetailDTO(TeachingSlot teachingSlot) {
        Session session = teachingSlot.getSession();
        TimeSlotTemplate timeSlot = session.getTimeSlotTemplate();
        ClassEntity classEntity = session.getClassEntity();
        CourseSession courseSession = session.getCourseSession();

        // Get teacher from teaching slot
        String teacherName = teachingSlot.getTeacher().getUserAccount().getFullName();
        Long teacherId = teachingSlot.getTeacher().getId();

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
                .description(courseSession != null ? courseSession.getStudentTask() : null)
                .sessionType(session.getType())
                .sessionStatus(session.getStatus())
                .location(determineLocation(classEntity, session))
                .onlineLink(null) // Will be determined by session resources
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
        boolean isMakeup = studentSessionRepository.existsBySessionIdAndIsMakeup(session.getId(), true);
        if (isMakeup) {
            makeupInfo = buildMakeupInfo(session);
        }

        // Calculate attendance summary
        Long sessionId = session.getId();
        long totalStudents = studentSessionRepository.countBySessionId(sessionId);
        long presentCount = studentSessionRepository.countBySessionIdAndAttendanceStatus(sessionId, AttendanceStatus.PRESENT);
        long absentCount = studentSessionRepository.countBySessionIdAndAttendanceStatus(sessionId, AttendanceStatus.ABSENT);
        boolean attendanceSubmitted = session.getStatus() == org.fyp.tmssep490be.entities.enums.SessionStatus.DONE
                || presentCount > 0 || absentCount > 0;

        AttendanceSummaryDTO attendanceSummary = AttendanceSummaryDTO.builder()
                .totalStudents((int) totalStudents)
                .presentCount((int) presentCount)
                .absentCount((int) absentCount)
                .lateCount(0) // Not available in current schema
                .excusedCount(0) // Not available in current schema
                .attendanceSubmitted(attendanceSubmitted)
                .build();

        return TeacherSessionDetailDTO.builder()
                .sessionId(session.getId())
                .date(session.getDate())
                .dayOfWeek(session.getDate().getDayOfWeek())
                .startTime(timeSlot != null ? timeSlot.getStartTime() : null)
                .endTime(timeSlot != null ? timeSlot.getEndTime() : null)
                .timeSlotName(timeSlot != null ? timeSlot.getName() : null)
                .classInfo(classInfo)
                .sessionInfo(sessionInfo)
                .materials(materials)
                .classroomResource(classroomResource)
                .makeupInfo(makeupInfo)
                .attendanceSummary(attendanceSummary)
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
                .fileName(courseMaterial.getTitle())
                .fileUrl(courseMaterial.getUrl())
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
            return "Online";
        }
    }

    private String determineResourceOnlineLink(Resource resource) {
        if (resource.getResourceType() == org.fyp.tmssep490be.entities.enums.ResourceType.VIRTUAL) {
            return resource.getName();
        }
        return null;
    }

    private String determineLocation(ClassEntity classEntity, Session session) {
        if (classEntity.getModality() == org.fyp.tmssep490be.entities.enums.Modality.OFFLINE) {
            return classEntity.getBranch().getName();
        } else if (classEntity.getModality() == org.fyp.tmssep490be.entities.enums.Modality.ONLINE) {
            return "Online";
        } else {
            return "Hybrid";
        }
    }

    private String determineLocationForSummary(ClassEntity classEntity, Session session) {
        if (!session.getSessionResources().isEmpty()) {
            Resource resource = session.getSessionResources().iterator().next().getResource();
            if (resource.getResourceType() == org.fyp.tmssep490be.entities.enums.ResourceType.ROOM) {
                return resource.getName();
            } else if (resource.getResourceType() == org.fyp.tmssep490be.entities.enums.ResourceType.VIRTUAL) {
                return "Online";
            }
        }
        return determineLocation(classEntity, session);
    }

    private MakeupInfoDTO buildMakeupInfo(Session session) {
        // Find original session from student sessions
        Optional<StudentSession> studentSessionWithMakeup = studentSessionRepository.findAll().stream()
                .filter(ss -> ss.getSession().getId().equals(session.getId()) && 
                             ss.getIsMakeup() != null && ss.getIsMakeup() &&
                             ss.getOriginalSession() != null)
                .findFirst();

        if (studentSessionWithMakeup.isEmpty()) {
            return null;
        }

        Session originalSession = studentSessionWithMakeup.get().getOriginalSession();
        TimeSlotTemplate originalTimeSlot = originalSession.getTimeSlotTemplate();
        return MakeupInfoDTO.builder()
                .isMakeup(true)
                .originalSessionId(originalSession.getId())
                .originalDate(originalSession.getDate())
                .originalStartTime(originalTimeSlot != null ? originalTimeSlot.getStartTime().toString() : null)
                .originalEndTime(originalTimeSlot != null ? originalTimeSlot.getEndTime().toString() : null)
                .originalStatus(originalSession.getStatus())
                .reason("Session rescheduled")
                .makeupDate(session.getDate())
                .build();
    }

}

