package org.fyp.tmssep490be.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.schedule.*;
import org.fyp.tmssep490be.dtos.schedule.AttendanceSummaryDTO;
import org.fyp.tmssep490be.dtos.schedule.TeacherSessionDetailDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.ResourceType;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TeacherScheduleService {

    private final TeacherRepository teacherRepository;
    private final SessionRepository sessionRepository;
    private final TimeSlotTemplateRepository timeSlotTemplateRepository;
    private final ObjectMapper objectMapper;

    public LocalDate getCurrentWeekStart() {
        LocalDate today = LocalDate.now();
        return today.minusDays(today.getDayOfWeek().getValue() - 1);
    }

    @Transactional(readOnly = true)
    public WeeklyScheduleResponseDTO getWeeklySchedule(Long teacherId, LocalDate weekStart, Long classId) {
        log.info("Getting weekly schedule for teacher: {}, week: {}, class: {}", teacherId, weekStart, classId);

        // 1. Validate weekStart is Monday
        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "weekStart must be a Monday");
        }

        // 2. Calculate week range
        LocalDate weekEnd = weekStart.plusDays(6);

        // 3. Fetch teacher
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND));

        // 4. Fetch all sessions for this week (all statuses)
        List<Session> sessions = sessionRepository.findWeeklySessionsForTeacher(
                teacherId, weekStart, weekEnd, classId);

        log.debug("Found {} sessions for week {} to {}", sessions.size(), weekStart, weekEnd);

        // 5. Get all time slots from branches where teacher has classes
        List<TimeSlotDTO> timeSlots = getAllTimeSlotsForTeacher(teacherId);

        // 6. Group by day of week
        Map<DayOfWeek, List<SessionSummaryDTO>> scheduleMap = sessions.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getDate().getDayOfWeek(),
                        Collectors.mapping(this::mapToSessionSummaryDTO, Collectors.toList())
                ));

        // 7. Ensure all days exist in map (even if empty)
        for (DayOfWeek day : DayOfWeek.values()) {
            scheduleMap.putIfAbsent(day, new ArrayList<>());
        }

        log.info("Schedule map for teacher {}: {} total sessions across {} days with data",
                teacherId,
                scheduleMap.values().stream().mapToInt(List::size).sum(),
                scheduleMap.values().stream().filter(list -> !list.isEmpty()).count());

        // 8. Build response
        return WeeklyScheduleResponseDTO.builder()
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .studentId(teacher.getId()) // Reuse field for teacherId
                .studentName(teacher.getUserAccount().getFullName()) // Reuse field for teacherName
                .timeSlots(timeSlots)
                .schedule(scheduleMap)
                .build();
    }

    @Transactional(readOnly = true)
    public TeacherSessionDetailDTO getSessionDetail(Long teacherId, Long sessionId) {
        log.info("Getting session detail for teacher: {}, session: {}", teacherId, sessionId);

        // 1. Fetch session with authorization check
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT, "Session not found"));

        // 2. Verify teacher is assigned to this session
        boolean isAssigned = session.getTeachingSlots().stream()
                .anyMatch(ts -> ts.getTeacher() != null && ts.getTeacher().getId().equals(teacherId));

        if (!isAssigned) {
            throw new CustomException(ErrorCode.FORBIDDEN, "Teacher is not assigned to this session");
        }

        return mapToTeacherSessionDetailDTO(session);
    }

    private TeacherSessionDetailDTO mapToTeacherSessionDetailDTO(Session session) {
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

        // Note: TeacherSessionDetailDTO doesn't need StudentStatusDTO

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

        // Build MakeupInfoDTO if applicable (check if session has makeup info)
        MakeupInfoDTO makeupInfo = null;
        // Note: Makeup info for teacher sessions might be stored differently
        // For now, we'll leave it null unless we find makeup-related data

        // Get attendance summary
        long totalStudents = session.getStudentSessions().size();
        long presentCount = session.getStudentSessions().stream()
                .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.PRESENT)
                .count();
        long absentCount = session.getStudentSessions().stream()
                .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.ABSENT)
                .count();
        // Note: AttendanceStatus enum doesn't have LATE status
        // Set to 0 for now
        long lateCount = 0;
        long excusedCount = session.getStudentSessions().stream()
                .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.EXCUSED)
                .count();
        
        // Check if attendance has been submitted (any student has non-PLANNED status)
        boolean attendanceSubmitted = session.getStudentSessions().stream()
                .anyMatch(ss -> ss.getAttendanceStatus() != null && 
                               ss.getAttendanceStatus() != AttendanceStatus.PLANNED);

        // Build attendance summary
        AttendanceSummaryDTO attendanceSummary = AttendanceSummaryDTO.builder()
                .totalStudents(totalStudents)
                .presentCount(presentCount)
                .absentCount(absentCount)
                .lateCount(lateCount)
                .excusedCount(excusedCount)
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

    private List<String> parseSkills(SubjectSession subjectSession) {
        if (subjectSession == null || subjectSession.getSkills() == null) {
            return new ArrayList<>();
        }

        Object skillsRaw = subjectSession.getSkills();

        // Case 1: String (JSON array hoặc text đơn)
        if (skillsRaw instanceof String) {
            String skillsText = ((String) skillsRaw).trim();

            if (skillsText.isEmpty()) {
                return new ArrayList<>();
            }

            if (skillsText.startsWith("[")) {
                try {
                    return objectMapper.readValue(skillsText, new TypeReference<List<String>>() {});
                } catch (JsonProcessingException e) {
                    log.warn("Failed to parse skills JSON array: {}", skillsText, e);
                    return new ArrayList<>();
                }
            }

            return List.of(skillsText);
        }

        // Case 2: List<?> (enum Skill hoặc String)
        if (skillsRaw instanceof List<?>) {
            List<?> skillList = (List<?>) skillsRaw;
            return skillList.stream()
                    .map(item -> {
                        if (item == null) return null;
                        if (item instanceof String) {
                            String txt = ((String) item).trim();
                            return txt.isEmpty() ? null : txt;
                        }
                        // enum Skill
                        String txt = item.toString();
                        return txt != null && !txt.trim().isEmpty() ? txt.trim() : null;
                    })
                    .filter(s -> s != null && !s.isEmpty())
                    .collect(Collectors.toList());
        }

        // Fallback
        return new ArrayList<>();
    }

    private String determineOnlineLink(Session session) {
        return session.getSessionResources().stream()
                .filter(sr -> sr.getResource().getResourceType() == ResourceType.VIRTUAL)
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

    private List<TimeSlotDTO> getAllTimeSlotsForTeacher(Long teacherId) {
        log.debug("Getting all time slots for teacher: {}", teacherId);

        // 1. Get all branches where teacher has classes
        // Query sessions in a wide date range to get all branches
        LocalDate fromDate = LocalDate.now().minusYears(1);
        LocalDate toDate = LocalDate.now().plusYears(1);
        List<Session> allSessions = sessionRepository.findWeeklySessionsForTeacher(
                teacherId, fromDate, toDate, null);
        
        Set<Long> branchIds = allSessions.stream()
                .map(s -> s.getClassEntity().getBranch().getId())
                .collect(Collectors.toSet());

        if (branchIds.isEmpty()) {
            log.warn("Teacher {} has no sessions. Returning empty time slot list.", teacherId);
            return Collections.emptyList();
        }

        log.debug("Teacher {} has sessions in {} branches", teacherId, branchIds.size());

        // 2. Union time slots from all branches
        List<TimeSlotTemplate> allTimeSlots = branchIds.stream()
                .flatMap(branchId -> timeSlotTemplateRepository
                        .findByBranchIdOrderByStartTimeAsc(branchId).stream())
                .toList();

        // 3. Group by TimeRange (startTime, endTime) to merge duplicates
        Map<TimeRange, List<TimeSlotTemplate>> groupedByTimeRange = allTimeSlots.stream()
                .collect(Collectors.groupingBy(
                        ts -> new TimeRange(ts.getStartTime(), ts.getEndTime())
                ));

        // 4. Merge duplicates and build DTOs
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
                        // Merge names from different branches
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

        log.debug("Merged {} unique time slots for teacher {}", mergedTimeSlots.size(), teacherId);
        return mergedTimeSlots;
    }

    private SessionSummaryDTO mapToSessionSummaryDTO(Session session) {
        ClassEntity classEntity = session.getClassEntity();
        SubjectSession subjectSession = session.getSubjectSession();
        TimeSlotTemplate timeSlot = session.getTimeSlotTemplate();

        // Extract resource information
        String resourceName = null;
        ResourceType resourceType = null;
        String onlineLink = null;

        if (!session.getSessionResources().isEmpty()) {
            SessionResource sessionResource = session.getSessionResources().iterator().next();
            Resource resource = sessionResource.getResource();
            resourceName = resource.getName();
            resourceType = resource.getResourceType();

            // If virtual resource, the name is typically the zoom link or label
            if (resourceType == ResourceType.VIRTUAL) {
                onlineLink = resource.getName(); // Store zoom link
            }
        }

        // Note: Attendance summary is not included in SessionSummaryDTO
        // It's only available in TeacherSessionDetailDTO

        return SessionSummaryDTO.builder()
                .sessionId(session.getId())
                .studentSessionId(null) // Not applicable for teacher
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
                .attendanceStatus(null) // Not applicable for teacher
                .isMakeup(false) // TODO: Check if session is makeup
                .makeupInfo(null) // TODO: Build makeup info if applicable
                .resourceName(resourceName)
                .resourceType(resourceType)
                .onlineLink(onlineLink)
                .build();
    }

    private String determineLocationForSummary(ClassEntity classEntity, Session session) {
        if (!session.getSessionResources().isEmpty()) {
            Resource resource = session.getSessionResources().iterator().next().getResource();
            if (resource.getResourceType() == ResourceType.ROOM) {
                return resource.getName();
            } else if (resource.getResourceType() == ResourceType.VIRTUAL) {
                return "Online";
            }
        }
        return determineLocation(classEntity, session);
    }

    private String determineResourceLocation(Resource resource) {
        if (resource.getResourceType() == ResourceType.ROOM) {
            return resource.getName() + ", " + resource.getBranch().getName();
        } else {
            return "Online";
        }
    }

    private String determineResourceOnlineLink(Resource resource) {
        if (resource.getResourceType() == ResourceType.VIRTUAL) {
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

    @lombok.Value
    private static class TimeRange {
        LocalTime startTime;
        LocalTime endTime;
    }
}

