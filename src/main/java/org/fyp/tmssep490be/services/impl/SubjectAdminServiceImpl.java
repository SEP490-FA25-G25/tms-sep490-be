package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.subject.CourseSummaryDTO;
import org.fyp.tmssep490be.dtos.subject.LevelSummaryDTO;
import org.fyp.tmssep490be.dtos.subject.SubjectDetailDTO;
import org.fyp.tmssep490be.dtos.subject.SubjectSummaryDTO;
import org.fyp.tmssep490be.dtos.subject.projections.SubjectSummaryProjection;
import org.fyp.tmssep490be.entities.Course;
import org.fyp.tmssep490be.entities.Level;
import org.fyp.tmssep490be.entities.enums.SubjectStatus;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.CourseRepository;
import org.fyp.tmssep490be.repositories.LevelRepository;
import org.fyp.tmssep490be.repositories.SubjectRepository;
import org.fyp.tmssep490be.services.SubjectAdminService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubjectAdminServiceImpl implements SubjectAdminService {

    private final SubjectRepository subjectRepository;
    private final LevelRepository levelRepository;
    private final CourseRepository courseRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SubjectSummaryDTO> getSubjectSummaries(SubjectStatus status, String search) {
        String normalizedSearch = null;
        if (search != null && !search.isBlank()) {
            normalizedSearch = "%" + search.trim().toLowerCase() + "%";
        }

        List<SubjectSummaryProjection> projections = subjectRepository.findSubjectSummaries(
                status,
                normalizedSearch
        );

        return projections.stream()
                .map(this::mapProjectionToSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectDetailDTO getSubjectDetail(Long subjectId) {
        SubjectSummaryProjection projection = subjectRepository.findSubjectSummaryById(subjectId)
                .orElseThrow(() -> new CustomException(ErrorCode.SUBJECT_NOT_FOUND));

        SubjectSummaryDTO summaryDTO = mapProjectionToSummary(projection);

        List<Level> levels = levelRepository.findBySubjectIdOrderBySortOrderAsc(subjectId);
        List<Course> courses = courseRepository.findBySubjectId(subjectId);

        Map<Long, Long> levelCourseCounts = courses.stream()
                .filter(course -> course.getLevel() != null)
                .collect(Collectors.groupingBy(course -> course.getLevel().getId(), Collectors.counting()));

        List<LevelSummaryDTO> levelSummaries = levels.stream()
                .map(level -> LevelSummaryDTO.builder()
                        .id(level.getId())
                        .code(level.getCode())
                        .name(level.getName())
                        .description(level.getDescription())
                        .expectedDurationHours(level.getExpectedDurationHours())
                        .sortOrder(level.getSortOrder())
                        .courseCount(levelCourseCounts.getOrDefault(level.getId(), 0L))
                        .build())
                .collect(Collectors.toList());

        List<CourseSummaryDTO> courseSummaries = courses.stream()
                .sorted(Comparator.comparing(Course::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(course -> CourseSummaryDTO.builder()
                        .id(course.getId())
                        .code(course.getCode())
                        .name(course.getName())
                        .status(course.getStatus())
                        .approvalStatus(course.getApprovalStatus())
                        .levelName(course.getLevel() != null ? course.getLevel().getName() : null)
                        .scoreScale(course.getScoreScale())
                        .createdAt(course.getCreatedAt())
                        .decidedAt(course.getDecidedAt())
                        .build())
                .collect(Collectors.toList());

        return SubjectDetailDTO.builder()
                .summary(summaryDTO)
                .levels(levelSummaries)
                .courses(courseSummaries)
                .build();
    }

    private SubjectSummaryDTO mapProjectionToSummary(SubjectSummaryProjection projection) {
        long pending = projection.getPendingCourseCount() != null ? projection.getPendingCourseCount() : 0L;
        long approved = projection.getApprovedCourseCount() != null ? projection.getApprovedCourseCount() : 0L;
        long draft = projection.getDraftCourseCount() != null ? projection.getDraftCourseCount() : 0L;

        return SubjectSummaryDTO.builder()
                .id(projection.getId())
                .code(projection.getCode())
                .name(projection.getName())
                .description(projection.getDescription())
                .status(projection.getStatus())
                .createdAt(projection.getCreatedAt())
                .updatedAt(projection.getUpdatedAt())
                .ownerName(projection.getOwnerName())
                .levelCount(projection.getLevelCount() != null ? projection.getLevelCount() : 0L)
                .courseCount(projection.getCourseCount() != null ? projection.getCourseCount() : 0L)
                .pendingCourseCount(pending)
                .approvedCourseCount(approved)
                .draftCourseCount(draft)
                .build();
    }
}

