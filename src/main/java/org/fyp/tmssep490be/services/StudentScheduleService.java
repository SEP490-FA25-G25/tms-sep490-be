package org.fyp.tmssep490be.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.schedule.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;
import org.fyp.tmssep490be.entities.enums.Skill;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.EnrollmentRepository;
import org.fyp.tmssep490be.repositories.StudentRepository;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.fyp.tmssep490be.repositories.TimeSlotTemplateRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StudentScheduleService {

    private final StudentRepository studentRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final TimeSlotTemplateRepository timeSlotTemplateRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ObjectMapper objectMapper;

    public LocalDate getCurrentWeekStart() {
        LocalDate today = LocalDate.now();
        return today.minusDays(today.getDayOfWeek().getValue() - 1);
    }

    public WeeklyScheduleResponseDTO getWeeklySchedule(Long studentId, LocalDate weekStart) {
        return getWeeklySchedule(studentId, weekStart, null);
    }

    public WeeklyScheduleResponseDTO getWeeklySchedule(Long studentId, LocalDate weekStart, Long classId) {
        log.info("Getting weekly schedule for student: {}, week: {}, class: {}", studentId, weekStart, classId);

        // 1. Validate weekStart is Monday
        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // 2. Calculate week range
        LocalDate weekEnd = weekStart.plusDays(6);

        // 3. Fetch student
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDENT_NOT_FOUND));

        // 4. Fetch all sessions for this week (with JOIN FETCH), optionally filtered by classId
        List<StudentSession> studentSessions = studentSessionRepository
                .findWeeklyScheduleByStudentId(studentId, weekStart, weekEnd, classId);

        log.debug("Found {} sessions for week {} to {} (classId: {})", 
                studentSessions.size(), weekStart, weekEnd, classId);

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

        log.info("Schedule map for student {}: {} total sessions across {} days with data", 
                studentId, 
                scheduleMap.values().stream().mapToInt(List::size).sum(),
                scheduleMap.values().stream().filter(list -> !list.isEmpty()).count());

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

    public SessionDetailDTO getSessionDetail(Long studentId, Long sessionId) {
        log.info("Getting session detail for student: {}, session: {}", studentId, sessionId);

        // 1. Fetch with authorization check
        StudentSession studentSession = studentSessionRepository
                .findByStudentIdAndSessionId(studentId, sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDENT_SESSION_NOT_FOUND));

        return mapToSessionDetailDTO(studentSession);
    }

    private SessionDetailDTO mapToSessionDetailDTO(StudentSession studentSession) {
        Session session = studentSession.getSession();
        TimeSlotTemplate timeSlot = session.getTimeSlotTemplate();
        ClassEntity classEntity = session.getClassEntity();
        SubjectSession subjectSession = session.getSubjectSession();

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

        ClassInfoDTO classInfo = ClassInfoDTO.builder()
                .classId(classEntity.getId())
                .classCode(classEntity.getCode())
                .className(classEntity.getName())
                .subjectId(classEntity.getSubject().getId())
                .subjectName(classEntity.getSubject().getName())
                .teacherId(teacherId)
                .teacherName(teacherName)
                .branchId(classEntity.getBranch().getId())
                .branchName(classEntity.getBranch().getName())
                .branchAddress(classEntity.getBranch().getAddress())
                .modality(classEntity.getModality())
                .build();

        SessionInfoDTO sessionInfo = SessionInfoDTO.builder()
                .topic(subjectSession != null ? subjectSession.getTopic() : null)
                .description(subjectSession != null ? subjectSession.getStudentTask() : null)
                .sessionType(session.getType())
                .sessionStatus(session.getStatus())
                .location(determineLocation(classEntity, session))
                .onlineLink(determineOnlineLink(session))
                .skills(parseSkills(subjectSession))
                .sequenceNo(subjectSession != null ? subjectSession.getSequenceNo() : null)
                .build();

        StudentStatusDTO studentStatus = StudentStatusDTO.builder()
                .attendanceStatus(resolveDisplayStatus(studentSession))
                .homeworkStatus(studentSession.getHomeworkStatus())
                .note(studentSession.getNote())
                .build();

        List<MaterialDTO> materials = new ArrayList<>();
        if (subjectSession != null && !subjectSession.getSubjectMaterials().isEmpty()) {
            materials = subjectSession.getSubjectMaterials().stream()
                    .map(this::mapToMaterialDTO)
                    .collect(Collectors.toList());
        }

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

    private List<String> parseSkills(SubjectSession subjectSession) {
        if (subjectSession == null || subjectSession.getSkills() == null) {
            return new ArrayList<>();
        }
        
        // SubjectSession.skills đã là List<Skill>, chỉ cần map sang tên
        return subjectSession.getSkills().stream()
                .map(Skill::name)
                .toList();
    }

    private String determineOnlineLink(Session session) {
        return session.getSessionResources().stream()
                .filter(sr -> sr.getResource().getResourceType() == org.fyp.tmssep490be.entities.enums.ResourceType.VIRTUAL)
                .map(sr -> sr.getResource().getName())
                .findFirst()
                .orElse(null);
    }

    private MaterialDTO mapToMaterialDTO(SubjectMaterial subjectMaterial) {
        return MaterialDTO.builder()
                .materialId(subjectMaterial.getId())
                .title(subjectMaterial.getTitle())
                .description(subjectMaterial.getDescription())
                .fileName(subjectMaterial.getTitle())
                .materialType(subjectMaterial.getMaterialType() != null ? subjectMaterial.getMaterialType().name() : null)
                .fileUrl(subjectMaterial.getUrl())
                .uploadedAt(subjectMaterial.getCreatedAt() != null ?
                        subjectMaterial.getCreatedAt().toLocalDateTime() : null)
                .build();
    }

    private ResourceDTO mapToResourceDTO(SessionResource sessionResource) {
        Resource resource = sessionResource.getResource();
        return ResourceDTO.builder()
                .resourceId(resource.getId())
                .resourceCode(resource.getCode())
                .resourceName(resource.getName())
                .resourceType(resource.getResourceType())
                .capacity(resource.getCapacity())
                .location(determineResourceLocation(resource))
                .onlineLink(determineResourceOnlineLink(resource))
                .build();
    }

    private List<TimeSlotDTO> getAllTimeSlotsForStudent(Long studentId) {
        log.debug("Getting all time slots for student: {}", studentId);

        // 1. Get all active enrollments for student
        List<Enrollment> activeEnrollments = enrollmentRepository
                .findByStudentIdAndStatus(studentId, EnrollmentStatus.ENROLLED);

        if (activeEnrollments.isEmpty()) {
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
                .toList();

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

    private SessionSummaryDTO mapToSessionSummaryDTO(StudentSession studentSession) {
        Session session = studentSession.getSession();
        ClassEntity classEntity = session.getClassEntity();
        SubjectSession subjectSession = session.getSubjectSession();
        TimeSlotTemplate timeSlot = session.getTimeSlotTemplate();

        MakeupInfoDTO makeupInfo = null;
        if (studentSession.getIsMakeup() != null && studentSession.getIsMakeup()) {
            makeupInfo = buildMakeupInfo(studentSession);
        }

        // Extract resource information
        String resourceName = null;
        org.fyp.tmssep490be.entities.enums.ResourceType resourceType = null;
        String onlineLink = null;

        if (!session.getSessionResources().isEmpty()) {
            SessionResource sessionResource = session.getSessionResources().iterator().next();
            Resource resource = sessionResource.getResource();
            resourceName = resource.getName();
            resourceType = resource.getResourceType();
            
            // If virtual resource, the name is typically the zoom link or label
            if (resourceType == org.fyp.tmssep490be.entities.enums.ResourceType.VIRTUAL) {
                onlineLink = resource.getName(); // Store zoom link
            }
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
                .subjectId(classEntity.getSubject().getId())
                .subjectName(classEntity.getSubject().getName())
                .topic(subjectSession != null ? subjectSession.getTopic() : null)
                .sessionType(session.getType())
                .sessionStatus(session.getStatus())
                .modality(classEntity.getModality())
                .location(determineLocationForSummary(classEntity, session))
                .branchName(classEntity.getBranch().getName())
                .attendanceStatus(resolveDisplayStatus(studentSession))
                .isMakeup(studentSession.getIsMakeup() != null ? studentSession.getIsMakeup() : false)
                .makeupInfo(makeupInfo)
                .resourceName(resourceName)
                .resourceType(resourceType)
                .onlineLink(onlineLink)
                .build();
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

    private MakeupInfoDTO buildMakeupInfo(StudentSession studentSession) {
        Session originalSession = studentSession.getOriginalSession();
        if (originalSession == null) {
            return null;
        }

        TimeSlotTemplate originalTimeSlot = originalSession.getTimeSlotTemplate();
        return MakeupInfoDTO.builder()
                .isMakeup(true)
                .originalSessionId(originalSession.getId())
                .originalDate(originalSession.getDate())
                .originalStartTime(originalTimeSlot != null ? originalTimeSlot.getStartTime().toString() : null)
                .originalEndTime(originalTimeSlot != null ? originalTimeSlot.getEndTime().toString() : null)
                .originalStatus(originalSession.getStatus())
                .reason("Session rescheduled")
                .makeupDate(studentSession.getSession().getDate())
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

    private org.fyp.tmssep490be.entities.enums.AttendanceStatus resolveDisplayStatus(StudentSession studentSession) {
        Session session = studentSession.getSession();
        if (session == null) {
            return studentSession.getAttendanceStatus();
        }
        org.fyp.tmssep490be.entities.enums.AttendanceStatus status = studentSession.getAttendanceStatus();
        LocalDate today = LocalDate.now();

        if (session.getDate() != null
                && session.getDate().isBefore(today)
                && (status == null || status == org.fyp.tmssep490be.entities.enums.AttendanceStatus.PLANNED)) {
            return org.fyp.tmssep490be.entities.enums.AttendanceStatus.ABSENT;
        }
        return status;
    }

    @lombok.Value
    private static class TimeRange {
        java.time.LocalTime startTime;
        java.time.LocalTime endTime;
    }

}

