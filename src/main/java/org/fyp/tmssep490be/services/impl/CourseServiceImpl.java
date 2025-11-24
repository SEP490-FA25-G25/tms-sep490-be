package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.course.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.CourseService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final CoursePhaseRepository coursePhaseRepository;
    private final CourseSessionRepository courseSessionRepository;
    private final CourseMaterialRepository courseMaterialRepository;
    private final CLORepository cloRepository;
    private final CourseAssessmentRepository courseAssessmentRepository;
    private final PLORepository ploRepository;
    private final PLOCLOMappingRepository ploCLOMappingRepository;
    private final ClassRepository classRepository;
    private final CenterRepository centerRepository;
    private final StudentRepository studentRepository;

    @Override
    public List<StudentCourseDTO> getStudentCoursesByUserId(Long userId) {
        log.debug("Getting courses for user {}", userId);

        // Find student by user ID
        Student student = studentRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found for user ID: " + userId));

        return getStudentCourses(student.getId());
    }

    @Override
    public List<StudentCourseDTO> getStudentCourses(Long studentId) {
        log.debug("Getting courses for student {}", studentId);

        List<Enrollment> enrollments = enrollmentRepository.findByStudentIdAndStatus(studentId, EnrollmentStatus.ENROLLED);

        return enrollments.stream()
                .map(this::convertToStudentCourseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CourseDetailDTO getCourseDetail(Long courseId) {
        log.debug("Getting course details for course {}", courseId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> {
                    log.error("Course not found: {}", courseId);
                    return new IllegalArgumentException("Course not found: " + courseId);
                });

        List<CoursePhaseDTO> phases = getCoursePhases(courseId);
        List<CourseMaterialDTO> materials = getCourseMaterials(courseId);
        List<CourseCLODTO> clos = getCourseCLOsList(courseId);
        List<CourseAssessmentDTO> assessments = getCourseAssessments(courseId);

        long totalSessionsLong = courseSessionRepository.countByCourseId(courseId);
        int totalSessions = (int) totalSessionsLong;
        long totalMaterialsLong = courseMaterialRepository.countByCourseId(courseId);
        int totalMaterials = (int) totalMaterialsLong;

        return CourseDetailDTO.builder()
                .id(course.getId())
                .code(course.getCode())
                .name(course.getName())
                .description(course.getDescription())
                .subjectName(course.getSubject() != null ? course.getSubject().getName() : null)
                .levelName(course.getLevel() != null ? course.getLevel().getName() : null)
                .logicalCourseCode(course.getLogicalCourseCode())
                .version(course.getVersion())
                .totalHours(course.getTotalHours())
                .durationWeeks(course.getDurationWeeks())
                .sessionPerWeek(course.getSessionPerWeek())
                .hoursPerSession(course.getHoursPerSession())
                .scoreScale(course.getScoreScale())
                .prerequisites(course.getPrerequisites())
                .targetAudience(course.getTargetAudience())
                .teachingMethods(course.getTeachingMethods())
                .effectiveDate(course.getEffectiveDate())
                .status(course.getStatus().toString())
                .approvalStatus(course.getApprovalStatus().toString())
                .phases(phases)
                .materials(materials)
                .clos(clos)
                .assessments(assessments)
                .totalSessions(totalSessions)
                .totalMaterials(totalMaterials)
                .build();
    }

    @Override
    public CourseDetailDTO getCourseSyllabus(Long courseId) {
        log.debug("Getting course syllabus for course {}", courseId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> {
                    log.error("Course not found: {}", courseId);
                    return new IllegalArgumentException("Course not found: " + courseId);
                });

        List<CoursePhaseDTO> phases = getCoursePhases(courseId);
        List<CourseCLODTO> clos = getCourseCLOsList(courseId);
        List<CourseAssessmentDTO> assessments = getCourseAssessments(courseId);

        return CourseDetailDTO.builder()
                .id(course.getId())
                .code(course.getCode())
                .name(course.getName())
                .description(course.getDescription())
                .subjectName(course.getSubject() != null ? course.getSubject().getName() : null)
                .levelName(course.getLevel() != null ? course.getLevel().getName() : null)
                .logicalCourseCode(course.getLogicalCourseCode())
                .version(course.getVersion())
                .totalHours(course.getTotalHours())
                .durationWeeks(course.getDurationWeeks())
                .sessionPerWeek(course.getSessionPerWeek())
                .hoursPerSession(course.getHoursPerSession())
                .scoreScale(course.getScoreScale())
                .prerequisites(course.getPrerequisites())
                .targetAudience(course.getTargetAudience())
                .teachingMethods(course.getTeachingMethods())
                .effectiveDate(course.getEffectiveDate())
                .status(course.getStatus() != null ? course.getStatus().toString() : null)
                .approvalStatus(course.getApprovalStatus() != null ? course.getApprovalStatus().toString() : null)
                .phases(phases)
                .clos(clos)
                .assessments(assessments)
                .build();
    }

    @Override
    public MaterialHierarchyDTO getCourseMaterials(Long courseId, Long studentId) {
        log.debug("Getting materials hierarchy for course {}, student {}", courseId, studentId);

        // Get course-level materials
        List<CourseMaterialDTO> courseLevelMaterials = courseMaterialRepository
                .findCourseLevelMaterials(courseId)
                .stream()
                .map(material -> convertToMaterialDTO(material, studentId))
                .collect(Collectors.toList());

        // Get phases with their materials
        List<PhaseMaterialDTO> phaseMaterials = coursePhaseRepository
                .findByCourseIdOrderByPhaseNumber(courseId)
                .stream()
                .map(phase -> {
                    List<CourseMaterialDTO> phaseMaterialsList = courseMaterialRepository
                            .findPhaseLevelMaterials(phase.getId())
                            .stream()
                            .map(material -> convertToMaterialDTO(material, studentId))
                            .collect(Collectors.toList());

                    List<SessionMaterialDTO> sessionMaterials = courseSessionRepository
                            .findByPhaseIdOrderBySequenceNo(phase.getId())
                            .stream()
                            .map(session -> {
                                List<CourseMaterialDTO> sessionMaterialsList = courseMaterialRepository
                                        .findSessionLevelMaterials(session.getId())
                                        .stream()
                                        .map(material -> convertToMaterialDTO(material, studentId))
                                        .collect(Collectors.toList());

                                return SessionMaterialDTO.builder()
                                        .id(session.getId())
                                        .sequenceNo(session.getSequenceNo())
                                        .topic(session.getTopic())
                                        .materials(sessionMaterialsList)
                                        .totalMaterials(sessionMaterialsList.size())
                                        .build();
                            })
                            .collect(Collectors.toList());

                    return PhaseMaterialDTO.builder()
                            .id(phase.getId())
                            .phaseNumber(phase.getPhaseNumber())
                            .name(phase.getName())
                            .materials(phaseMaterialsList)
                            .sessions(sessionMaterials)
                            .totalMaterials(phaseMaterialsList.size() + sessionMaterials.stream()
                                    .mapToInt(sm -> sm.getTotalMaterials())
                                    .sum())
                            .build();
                })
                .collect(Collectors.toList());

        int totalMaterials = courseLevelMaterials.size() + phaseMaterials.stream()
                .mapToInt(pm -> pm.getTotalMaterials())
                .sum();

        // accessibleMaterials count is no longer used in frontend, setting to totalMaterials
        int accessibleMaterials = totalMaterials;

        return MaterialHierarchyDTO.builder()
                .courseLevel(courseLevelMaterials)
                .phases(phaseMaterials)
                .totalMaterials(totalMaterials)
                .accessibleMaterials(accessibleMaterials)
                .build();
    }

    @Override
    public List<CoursePLODTO> getCoursePLOs(Long courseId) {
        log.debug("Getting PLOs for course {}", courseId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> {
                    log.error("Course not found: {}", courseId);
                    return new IllegalArgumentException("Course not found: " + courseId);
                });

        List<CLO> clos = cloRepository.findByCourseId(courseId);

        Map<Long, PLO> uniquePlos = clos.stream()
                .flatMap(clo -> ploCLOMappingRepository.findByCloId(clo.getId()).stream())
                .map(mapping -> mapping.getPlo())
                .distinct()
                .collect(Collectors.toMap(PLO::getId, plo -> plo));

        return uniquePlos.values().stream()
                .map(this::convertToPLODTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseCLODTO> getCourseCLOs(Long courseId) {
        log.debug("Getting CLOs for course {}", courseId);

        return getCourseCLOsList(courseId);
    }

    private StudentCourseDTO convertToStudentCourseDTO(Enrollment enrollment) {
        Course course = enrollment.getClassEntity().getCourse();
        ClassEntity classEntity = enrollment.getClassEntity();

        // Calculate progress
        List<StudentSession> studentSessions = studentSessionRepository.findByStudentIdAndClassId(
                enrollment.getStudentId(),
                enrollment.getClassId()
        );
        int totalSessions = courseSessionRepository.countByCourseId(course.getId());
        int completedSessions = (int) studentSessions.stream()
                .filter(ss -> AttendanceStatus.PRESENT.equals(ss.getAttendanceStatus()))
                .count();

        double progressPercentage = totalSessions > 0 ?
                (double) completedSessions / totalSessions * 100 : 0.0;

        // Calculate attendance rate
        double attendanceRate = studentSessions.size() > 0 ?
                (double) completedSessions / studentSessions.size() * 100 : 0.0;

        return StudentCourseDTO.builder()
                .id(course.getId())
                .code(course.getCode())
                .name(course.getName())
                .description(course.getDescription())
                .subjectName(course.getSubject() != null ? course.getSubject().getName() : null)
                .levelName(course.getLevel() != null ? course.getLevel().getName() : null)
                .logicalCourseCode(course.getLogicalCourseCode())
                .totalHours(course.getTotalHours())
                .durationWeeks(course.getDurationWeeks())
                .sessionPerWeek(course.getSessionPerWeek())
                .targetAudience(course.getTargetAudience())
                .teachingMethods(course.getTeachingMethods())
                .effectiveDate(course.getEffectiveDate())
                .status(course.getStatus().toString())
                .approvalStatus(course.getApprovalStatus().toString())
                .classId(classEntity.getId())
                .classCode(classEntity.getCode())
                .centerName(classEntity.getBranch() != null ? classEntity.getBranch().getName() : null)
                .roomName(null) // Room not available in ClassEntity
                .modality(classEntity.getModality() != null ? classEntity.getModality().toString() : null)
                .classStartDate(classEntity.getStartDate())
                .classEndDate(classEntity.getActualEndDate() != null ? classEntity.getActualEndDate() : classEntity.getPlannedEndDate())
                .teacherName(null) // Teacher not directly available in ClassEntity
                .enrollmentStatus(enrollment.getStatus().toString())
                .enrolledAt(enrollment.getCreatedAt() != null ? enrollment.getCreatedAt().toLocalDate() : null)
                .progressPercentage(progressPercentage)
                .completedSessions(completedSessions)
                .totalSessions(totalSessions)
                .attendanceRate(String.format("%.1f%%", attendanceRate))
                .build();
    }

    private List<CoursePhaseDTO> getCoursePhases(Long courseId) {
        return coursePhaseRepository.findByCourseIdOrderByPhaseNumber(courseId)
                .stream()
                .map(phase -> {
                    List<CourseSessionDTO> sessions = courseSessionRepository
                            .findByPhaseIdOrderBySequenceNo(phase.getId())
                            .stream()
                            .map(this::convertToSessionDTO)
                            .collect(Collectors.toList());

                    return CoursePhaseDTO.builder()
                            .id(phase.getId())
                            .phaseNumber(phase.getPhaseNumber())
                            .name(phase.getName())
                            .description(phase.getLearningFocus()) // Use learningFocus instead of description
                            .sequenceNo(phase.getPhaseNumber()) // Use phaseNumber instead of sequenceNo
                            .sessions(sessions)
                            .totalSessions(sessions.size())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private CourseSessionDTO convertToSessionDTO(CourseSession session) {
        // Handle skillSet array properly
        List<String> skillSets = session.getSkillSet() != null ?
                java.util.Arrays.stream(session.getSkillSet())
                        .map(Enum::name)
                        .collect(java.util.stream.Collectors.toList()) : List.of();

        return CourseSessionDTO.builder()
                .id(session.getId())
                .sequenceNo(session.getSequenceNo())
                .topic(session.getTopic())
                .description(session.getStudentTask()) // Use studentTask instead of description
                .objectives(null) // objectives not available in CourseSession
                .skillSets(skillSets)
                .totalMaterials((int) courseMaterialRepository.countByCourseSessionId(session.getId()))
                .build();
    }

    private CourseMaterialDTO convertToMaterialDTO(CourseMaterial material, Long studentId) {
        String level;
        if (material.getPhase() == null && material.getCourseSession() == null) {
            level = "COURSE";
        } else if (material.getCourseSession() == null) {
            level = "PHASE";
        } else {
            level = "SESSION";
        }

        boolean isAccessible = studentId == null || isMaterialAccessible(studentId, material);

        return CourseMaterialDTO.builder()
                .id(material.getId())
                .title(material.getTitle())
                .description(material.getDescription())
                .materialType(material.getMaterialType() != null ? material.getMaterialType().toString() : null)
                .fileName(null) // fileName not available in CourseMaterial
                .filePath(null) // filePath not available in CourseMaterial
                .fileUrl(material.getUrl()) // Use url instead of fileUrl
                .fileSize(null) // fileSize not available in CourseMaterial
                .level(level)
                .phaseId(material.getPhase() != null ? material.getPhase().getId() : null)
                .sessionId(material.getCourseSession() != null ? material.getCourseSession().getId() : null)
                .sequenceNo(null) // sequenceNo not available in CourseMaterial
                .isAccessible(isAccessible)
                .createdAt(material.getCreatedAt())
                .updatedAt(material.getUpdatedAt())
                .build();
    }

    private boolean isMaterialAccessible(Long studentId, CourseMaterial material) {
        // Course-level materials are always accessible
        if (material.getPhase() == null && material.getCourseSession() == null) {
            return true;
        }

        // Check if student is enrolled
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseIdAndStatus(studentId, material.getCourse().getId(), EnrollmentStatus.ENROLLED);

        if (enrollment == null) {
            return false;
        }

        // Phase-level materials are accessible when phase starts
        if (material.getPhase() != null && material.getCourseSession() == null) {
            return isPhaseAccessible(enrollment, material.getPhase());
        }

        // Session-level materials are accessible after session completes
        if (material.getCourseSession() != null) {
            return isSessionAccessible(enrollment, material.getCourseSession());
        }

        return false;
    }

    private boolean isPhaseAccessible(Enrollment enrollment, CoursePhase phase) {
        // Phase is accessible if student is enrolled and class has started
        // Don't require session completion for phase access
        if (enrollment == null || enrollment.getClassEntity() == null) {
            return false;
        }

        LocalDate classStartDate = enrollment.getClassEntity().getStartDate();
        if (classStartDate == null) {
            return false;
        }

        // Phase becomes accessible when class starts (simplified logic)
        // In a real implementation, this could be based on specific phase start dates
        LocalDate currentDate = LocalDate.now();
        return currentDate.isAfter(classStartDate) || currentDate.isEqual(classStartDate);
    }

    private boolean isSessionAccessible(Enrollment enrollment, CourseSession session) {
        Optional<StudentSession> studentSession = studentSessionRepository
                .findByStudentIdAndCourseSessionId(enrollment.getStudentId(), session.getId());

        return studentSession.isPresent() &&
            AttendanceStatus.PRESENT.equals(studentSession.get().getAttendanceStatus());
    }

    private List<CourseMaterialDTO> getCourseMaterials(Long courseId) {
        return courseMaterialRepository.findByCourseId(courseId)
                .stream()
                .map(material -> convertToMaterialDTO(material, null))
                .collect(Collectors.toList());
    }

   private List<CourseCLODTO> getCourseCLOsList(Long courseId) {
        return cloRepository.findByCourseId(courseId)
                .stream()
                .map(this::convertToCLODTO)
                .collect(Collectors.toList());
    }

    private CourseCLODTO convertToCLODTO(CLO clo) {
        List<CoursePLODTO> relatedPLOs = ploCLOMappingRepository.findByCloId(clo.getId())
                .stream()
                .map(mapping -> convertToPLODTO(mapping.getPlo()))
                .collect(Collectors.toList());

        return CourseCLODTO.builder()
                .id(clo.getId())
                .code(clo.getCode())
                .description(clo.getDescription())
                .competencyLevel(null) // competencyLevel not available in CLO
                .relatedPLOs(relatedPLOs)
                .build();
    }

    private CoursePLODTO convertToPLODTO(PLO plo) {
        return CoursePLODTO.builder()
                .id(plo.getId())
                .code(plo.getCode())
                .description(plo.getDescription())
                .programName(plo.getSubject() != null ? plo.getSubject().getName() : null) // Use subject name instead of programName
                .build();
    }

    private List<CourseAssessmentDTO> getCourseAssessments(Long courseId) {
        return courseAssessmentRepository.findByCourseId(courseId)
                .stream()
                .map(this::convertToAssessmentDTO)
                .collect(Collectors.toList());
    }

    private CourseAssessmentDTO convertToAssessmentDTO(CourseAssessment assessment) {
        // Extract CLO codes from mappings
        List<String> cloMappings = assessment.getCourseAssessmentCLOMappings().stream()
                .map(mapping -> mapping.getClo().getCode())
                .sorted()
                .collect(Collectors.toList());

        return CourseAssessmentDTO.builder()
                .id(assessment.getId())
                .name(assessment.getName())
                .description(assessment.getDescription())
                .assessmentType(assessment.getKind().toString()) // Use getKind() instead of getAssessmentType()
                .weight(null) // weight not available in CourseAssessment
                .maxScore(assessment.getMaxScore())
                .duration(assessment.getDurationMinutes() != null ? assessment.getDurationMinutes().toString() : null) // Convert Integer to String
                .cloMappings(cloMappings)
                .build();
    }
}
