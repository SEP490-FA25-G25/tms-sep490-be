package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.qa.QAClassListItemDTO;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.HomeworkStatus;
import org.fyp.tmssep490be.exceptions.InvalidRequestException;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.repositories.QAReportRepository;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.fyp.tmssep490be.repositories.UserBranchesRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QAService {

    private final ClassRepository classRepository;
    private final QAReportRepository qaReportRepository;
    private final SessionRepository sessionRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final UserBranchesRepository userBranchesRepository;

    @Transactional(readOnly = true)
    public Page<QAClassListItemDTO> getQAClasses(
            List<Long> branchIds,
            String status,
            String search,
            Pageable pageable,
            Long userId
    ) {
        log.info("Getting QA classes list: branchIds={}, status={}, search={}", branchIds, status, search);

        if (branchIds == null || branchIds.isEmpty()) {
            branchIds = getUserAccessibleBranches(userId);
            if (branchIds.isEmpty()) {
                throw new InvalidRequestException("Bạn không được phân công chi nhánh nào");
            }
        }

        ClassStatus classStatus = null;
        if (status != null && !status.isEmpty() && !status.equalsIgnoreCase("all")) {
            try {
                classStatus = ClassStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid class status: {}. Ignoring filter.", status);
            }
        }

        Page<ClassEntity> classes = classRepository.findClassesForAcademicAffairs(
                branchIds, null, classStatus, null, null, search, pageable
        );

        List<Long> classIds = classes.getContent().stream()
                .map(ClassEntity::getId)
                .collect(Collectors.toList());

        Map<Long, Double> attendanceRates = calculateAttendanceRatesForClasses(classIds);
        Map<Long, Double> homeworkRates = calculateHomeworkRatesForClasses(classIds);

        return classes.map(c -> {
            long totalSessions = sessionRepository.countByClassEntityId(c.getId());
            long completedSessions = sessionRepository.countByClassEntityIdExcludingCancelled(c.getId());
            long qaReportCount = qaReportRepository.countByClassEntityId(c.getId());

            double attendanceRate = attendanceRates.getOrDefault(c.getId(), 0.0);
            double homeworkRate = homeworkRates.getOrDefault(c.getId(), 0.0);

            return QAClassListItemDTO.builder()
                    .classId(c.getId())
                    .classCode(c.getCode() != null ? c.getCode() : "N/A")
                    .className(c.getName() != null ? c.getName() : "N/A")
                    .courseId(c.getSubject() != null ? c.getSubject().getId() : null)
                    .courseName(c.getSubject() != null ? c.getSubject().getName() : "N/A")
                    .branchName(c.getBranch() != null ? c.getBranch().getName() : "N/A")
                    .modality(c.getModality() != null ? c.getModality().name() : null)
                    .status(c.getStatus() != null ? c.getStatus().name() : null)
                    .startDate(c.getStartDate())
                    .totalSessions((int) totalSessions)
                    .completedSessions((int) completedSessions)
                    .attendanceRate(attendanceRate)
                    .homeworkCompletionRate(homeworkRate)
                    .qaReportCount((int) qaReportCount)
                    .build();
        });
    }

    private List<Long> getUserAccessibleBranches(Long userId) {
        return userBranchesRepository.findBranchIdsByUserId(userId);
    }

    private Map<Long, Double> calculateAttendanceRatesForClasses(List<Long> classIds) {
        Map<Long, Double> rates = new HashMap<>();
        if (classIds.isEmpty()) {
            return rates;
        }

        try {
            List<Object[]> data = studentSessionRepository.getAttendanceSummaryByClassIds(classIds);
            Map<Long, Long> presentCounts = new HashMap<>();
            Map<Long, Long> totalCounts = new HashMap<>();

            for (Object[] row : data) {
                Long classId = (Long) row[0];
                AttendanceStatus status = (AttendanceStatus) row[1];
                Long count = (Long) row[2];

                totalCounts.put(classId, totalCounts.getOrDefault(classId, 0L) + count);
                if (status == AttendanceStatus.PRESENT) {
                    presentCounts.put(classId, presentCounts.getOrDefault(classId, 0L) + count);
                }
            }

            for (Long classId : classIds) {
                long presentCount = presentCounts.getOrDefault(classId, 0L);
                long totalCount = totalCounts.getOrDefault(classId, 0L);
                rates.put(classId, totalCount > 0 ? (presentCount * 100.0) / totalCount : 0.0);
            }
        } catch (Exception e) {
            log.error("Error calculating attendance rates for classes {}: {}", classIds, e.getMessage());
            classIds.forEach(classId -> rates.put(classId, 0.0));
        }

        return rates;
    }

    private Map<Long, Double> calculateHomeworkRatesForClasses(List<Long> classIds) {
        Map<Long, Double> rates = new HashMap<>();
        if (classIds.isEmpty()) {
            return rates;
        }

        try {
            List<Object[]> data = studentSessionRepository.getHomeworkSummaryByClassIds(classIds);
            Map<Long, Long> completedCounts = new HashMap<>();
            Map<Long, Long> totalCounts = new HashMap<>();

            for (Object[] row : data) {
                Long classId = (Long) row[0];
                HomeworkStatus status = (HomeworkStatus) row[1];
                Long count = (Long) row[2];

                totalCounts.put(classId, totalCounts.getOrDefault(classId, 0L) + count);
                if (status == HomeworkStatus.COMPLETED) {
                    completedCounts.put(classId, completedCounts.getOrDefault(classId, 0L) + count);
                }
            }

            for (Long classId : classIds) {
                long completedCount = completedCounts.getOrDefault(classId, 0L);
                long totalCount = totalCounts.getOrDefault(classId, 0L);
                rates.put(classId, totalCount > 0 ? (completedCount * 100.0) / totalCount : 0.0);
            }
        } catch (Exception e) {
            log.error("Error calculating homework rates for classes {}: {}", classIds, e.getMessage());
            classIds.forEach(classId -> rates.put(classId, 0.0));
        }

        return rates;
    }
}
