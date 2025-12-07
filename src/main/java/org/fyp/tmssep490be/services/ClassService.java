package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.classmanagement.ClassListItemDTO;
import org.fyp.tmssep490be.dtos.classmanagement.TeacherSummaryDTO;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.Teacher;
import org.fyp.tmssep490be.entities.TeachingSlot;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

    public Page<ClassListItemDTO> getClasses(
            List<Long> branchIds,
            Long courseId,
            ClassStatus status,
            ApprovalStatus approvalStatus,
            Modality modality,
            String search,
            Pageable pageable,
            Long userId) {
        log.debug(
                "Getting classes for user {} with filters: branchIds={}, courseId={}, status={}, approvalStatus={}, modality={}, search={}",
                userId, branchIds, courseId, status, approvalStatus, modality, search);

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
                courseId,
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
                .courseName(classEntity.getSubject().getName())
                .courseCode(classEntity.getSubject().getCode())
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
                .scheduleSummary(formatScheduleSummary(classEntity.getScheduleDays()))
                .completedSessions(completedSessions)
                .totalSessions(totalSessions)
                .canEnrollStudents(canEnroll)
                .enrollmentRestrictionReason(canEnroll ? null
                        : availableSlots <= 0 ? "Class is full"
                        : classEntity.getStatus() == ClassStatus.COMPLETED ? "Class has completed"
                        : classEntity.getStatus() == ClassStatus.CANCELLED ? "Class was cancelled"
                        : "Class not available for enrollment")
                .build();
    }

    private String formatScheduleSummary(Short[] scheduleDays) {
        if (scheduleDays == null || scheduleDays.length == 0) {
            return "Not specified";
        }

        String[] dayNames = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
        return Arrays.stream(scheduleDays)
                .filter(day -> day != null && day >= 1 && day <= 7)
                .map(day -> dayNames[day - 1])
                .collect(Collectors.joining(", "));
    }

    private List<TeacherSummaryDTO> getTeachersForClass(Long classId) {
        List<TeachingSlot> teachingSlots = teachingSlotRepository
                .findByClassEntityIdAndStatus(classId, TeachingSlotStatus.SCHEDULED);

        // Group by teacher and count sessions
        Map<Teacher, Long> teacherSessionCounts = teachingSlots.stream()
                .filter(slot -> slot.getTeacher() != null)
                .collect(Collectors.groupingBy(
                        TeachingSlot::getTeacher,
                        Collectors.counting()));

        // Convert to DTOs sorted by session count (descending)
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

}
