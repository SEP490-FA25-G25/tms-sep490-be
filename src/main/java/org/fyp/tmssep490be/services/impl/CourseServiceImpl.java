package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.CourseDTO;
import org.fyp.tmssep490be.dtos.course.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.CourseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.LocalDate;

/**
 * Implementation of CourseService
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;
    private final LevelRepository levelRepository;
    private final CLORepository cloRepository;
    private final PLORepository ploRepository;
    private final PLOCLOMappingRepository ploCloMappingRepository;
    private final CoursePhaseRepository coursePhaseRepository;
    private final CourseSessionRepository courseSessionRepository;
    private final CourseSessionCLOMappingRepository courseSessionCLOMappingRepository;
    private final CourseAssessmentRepository courseAssessmentRepository;
    private final CourseAssessmentCLOMappingRepository courseAssessmentCLOMappingRepository;
    private final CourseMaterialRepository courseMaterialRepository;

    // Main branch dependencies
    private final EnrollmentRepository enrollmentRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final ClassRepository classRepository;
    private final CenterRepository centerRepository;
    private final StudentRepository studentRepository;

    @Override
    public List<CourseDTO> getAllCourses(Long subjectId, Long levelId) {
        log.debug("Getting all courses for dropdown with filters - subjectId: {}, levelId: {}", subjectId, levelId);

        List<Course> courses;

        if (subjectId != null && levelId != null) {
            courses = courseRepository.findBySubjectIdAndLevelId(subjectId, levelId);
        } else if (subjectId != null) {
            courses = courseRepository.findBySubjectId(subjectId);
        } else if (levelId != null) {
            courses = courseRepository.findByLevelId(levelId);
        } else {
            courses = courseRepository.findAll();
        }

        return courses.stream()
                .map(course -> CourseDTO.builder()
                        .id(course.getId())
                        .name(course.getName())
                        .code(course.getCode())
                        .status(course.getStatus().name())
                        .build())
                .collect(Collectors.toList());
    }

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

        List<Enrollment> enrollments = enrollmentRepository.findByStudentIdAndStatus(studentId,
                EnrollmentStatus.ENROLLED);

        return enrollments.stream()
                .map(this::convertToStudentCourseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CourseDTO createCourse(CreateCourseRequestDTO request) {
        log.info("Creating new course: {}", request.getBasicInfo().getName());

        // 1. Validate and Create Course Entity
        Subject subject = subjectRepository.findById(request.getBasicInfo().getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        Level level = levelRepository.findById(request.getBasicInfo().getLevelId())
                .orElseThrow(() -> new ResourceNotFoundException("Level not found"));

        Course course = new Course();
        course.setSubject(subject);
        course.setLevel(level);
        course.setCode(request.getBasicInfo().getCode());
        course.setName(request.getBasicInfo().getName());
        course.setDescription(request.getBasicInfo().getDescription());
        course.setPrerequisites(request.getBasicInfo().getPrerequisites());
        course.setTotalHours(request.getBasicInfo().getDurationHours());
        course.setDurationWeeks(request.getBasicInfo().getDurationWeeks());
        course.setScoreScale(request.getBasicInfo().getScoreScale());
        course.setTargetAudience(request.getBasicInfo().getTargetAudience());
        course.setTeachingMethods(request.getBasicInfo().getTeachingMethods());
        course.setEffectiveDate(request.getBasicInfo().getEffectiveDate());
        course.setSessionPerWeek(request.getBasicInfo().getSessionPerWeek());
        course.setHoursPerSession(request.getBasicInfo().getHoursPerSession());

        if (request.getStatus() != null) {
            try {
                course.setStatus(CourseStatus.valueOf(request.getStatus()));
            } catch (IllegalArgumentException e) {
                course.setStatus(CourseStatus.DRAFT);
            }
        } else {
            course.setStatus(CourseStatus.DRAFT);
        }

        course = courseRepository.save(course);
        log.info("Course saved with ID: {}", course.getId());

        // 2. Create CLOs
        Map<String, CLO> cloMap = createCLOs(course, request.getClos());

        // 3. Create Structure (Phases & Sessions)
        createStructure(course, request.getStructure(), cloMap);

        // 4. Create Assessments
        createAssessments(course, request.getAssessments(), cloMap);

        // 5. Create Materials
        createMaterialsFromDTO(course, request.getMaterials());

        log.info("Course creation completed successfully");

        return CourseDTO.builder()
                .id(course.getId())
                .name(course.getName())
                .code(course.getCode())
                .build();
    }

    private Map<String, CLO> createCLOs(Course course, List<CourseCLODTO> cloDTOs) {
        if (cloDTOs == null)
            return Map.of();

        List<CLO> clos = new ArrayList<>();
        for (CourseCLODTO dto : cloDTOs) {
            CLO clo = new CLO();
            clo.setCourse(course);
            clo.setCode(dto.getCode());
            clo.setDescription(dto.getDescription());
            clos.add(clo);
        }
        clos = cloRepository.saveAll(clos);

        // Map PLOs if needed (Skipping complex PLO mapping for now, assuming PLOs
        // exist)

        return clos.stream().collect(Collectors.toMap(CLO::getCode, Function.identity()));
    }

    private void createStructure(Course course, CourseStructureDTO structureDTO, Map<String, CLO> cloMap) {
        if (structureDTO == null || structureDTO.getPhases() == null)
            return;

        for (CoursePhaseDTO phaseDTO : structureDTO.getPhases()) {
            CoursePhase phase = new CoursePhase();
            phase.setCourse(course);
            phase.setName(phaseDTO.getName());
            phase = coursePhaseRepository.save(phase);

            if (phaseDTO.getSessions() != null) {
                for (CourseSessionDTO sessionDTO : phaseDTO.getSessions()) {
                    CourseSession session = new CourseSession();
                    session.setPhase(phase);
                    session.setTopic(sessionDTO.getTopic());
                    session.setStudentTask(sessionDTO.getStudentTask());
                    session.setSequenceNo(1); // Default sequence for now
                    if (sessionDTO.getSkillSets() != null) {
                        session.setSkillSet(sessionDTO.getSkillSets().stream()
                                .map(skillName -> {
                                    try {
                                        return org.fyp.tmssep490be.entities.enums.Skill.valueOf(skillName);
                                    } catch (IllegalArgumentException e) {
                                        return null;
                                    }
                                })
                                .filter(java.util.Objects::nonNull)
                                .collect(java.util.stream.Collectors.toSet()));
                    }
                    session = courseSessionRepository.save(session);

                    // Map CLOs to Session
                    if (sessionDTO.getMappedCLOs() != null) {
                        List<CourseSessionCLOMapping> mappings = new ArrayList<>();
                        for (String cloCode : sessionDTO.getMappedCLOs()) {
                            CLO clo = cloMap.get(cloCode);
                            if (clo != null) {
                                CourseSessionCLOMapping mapping = new CourseSessionCLOMapping();
                                mapping.setCourseSession(session);
                                mapping.setClo(clo);
                                mappings.add(mapping);
                            }
                        }
                        courseSessionCLOMappingRepository.saveAll(mappings);
                    }
                }
            }
        }
    }

    private void createAssessments(Course course, List<CourseAssessmentDTO> assessmentDTOs, Map<String, CLO> cloMap) {
        if (assessmentDTOs == null)
            return;

        for (CourseAssessmentDTO dto : assessmentDTOs) {
            CourseAssessment assessment = new CourseAssessment();
            assessment.setCourse(course);
            assessment.setName(dto.getName());
            assessment.setKind(AssessmentKind.valueOf(dto.getType()));
            assessment.setMaxScore(java.math.BigDecimal.valueOf(10)); // Default max score
            assessment.setDurationMinutes(dto.getDurationMinutes());
            // assessment.setSkills(new org.fyp.tmssep490be.entities.enums.Skill[] {}); //
            // Removed as per lint error, assuming setSkills takes Set or List
            assessment = courseAssessmentRepository.save(assessment);

            // Map CLOs to Assessment
            if (dto.getMappedCLOs() != null) {
                List<CourseAssessmentCLOMapping> mappings = new ArrayList<>();
                for (String cloCode : dto.getMappedCLOs()) {
                    CLO clo = cloMap.get(cloCode);
                    if (clo != null) {
                        CourseAssessmentCLOMapping mapping = new CourseAssessmentCLOMapping();
                        mapping.setCourseAssessment(assessment);
                        mapping.setClo(clo);
                        mappings.add(mapping);
                    }
                }
                courseAssessmentCLOMappingRepository.saveAll(mappings);
            }
        }
    }

    private void createMaterialsFromDTO(Course course, List<CourseMaterialDTO> materialDTOs) {
        if (materialDTOs == null)
            return;

        List<CourseMaterial> materials = materialDTOs.stream().map(dto -> {
            CourseMaterial material = new CourseMaterial();
            material.setCourse(course);
            material.setTitle(dto.getName());
            material.setMaterialType(MaterialType.valueOf(dto.getType()));
            material.setUrl(dto.getUrl());
            return material;
        }).collect(Collectors.toList());

        courseMaterialRepository.saveAll(materials);
    }

    @Override
    public CourseDetailDTO getCourseDetail(Long courseId) {
        return getCourseDetails(courseId);
    }

    @Override
    public CourseDetailDTO getCourseDetails(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        // Map Basic Info
        CourseBasicInfoDTO basicInfo = CourseBasicInfoDTO.builder()
                .subjectId(course.getSubject().getId())
                .levelId(course.getLevel().getId())
                .name(course.getName())
                .code(course.getCode())
                .description(course.getDescription())
                .prerequisites(course.getPrerequisites())
                .durationHours(course.getTotalHours())
                .durationWeeks(course.getDurationWeeks())
                .scoreScale(course.getScoreScale())
                .targetAudience(course.getTargetAudience())
                .teachingMethods(course.getTeachingMethods())
                .effectiveDate(course.getEffectiveDate())
                .sessionPerWeek(course.getSessionPerWeek())
                .hoursPerSession(course.getHoursPerSession())
                .build();

        // Map CLOs
        List<CourseCLODTO> clos = course.getClos().stream()
                .map(clo -> {
                    List<String> mappedPLOs = clo.getPloCloMappings().stream()
                            .map(mapping -> mapping.getPlo().getCode())
                            .collect(Collectors.toList());

                    return CourseCLODTO.builder()
                            .code(clo.getCode())
                            .description(clo.getDescription())
                            .mappedPLOs(mappedPLOs)
                            .build();
                })
                .collect(Collectors.toList());

        // Map Structure
        List<CoursePhaseDTO> phases = course.getCoursePhases().stream()
                .map(phase -> {
                    List<CourseSessionDTO> sessions = phase.getCourseSessions().stream()
                            .map(session -> {
                                List<String> mappedCLOs = session.getCourseSessionCLOMappings().stream()
                                        .map(mapping -> mapping.getClo().getCode())
                                        .collect(Collectors.toList());

                                return CourseSessionDTO.builder()
                                        .topic(session.getTopic())
                                        .studentTask(session.getStudentTask())
                                        .mappedCLOs(mappedCLOs)
                                        .build();
                            })
                            .collect(Collectors.toList());

                    return CoursePhaseDTO.builder()
                            .name(phase.getName())
                            .sessions(sessions)
                            .build();
                })
                .collect(Collectors.toList());

        CourseStructureDTO structure = CourseStructureDTO.builder()
                .phases(phases)
                .build();

        // Map Assessments
        List<CourseAssessmentDTO> assessments = course.getCourseAssessments().stream()
                .map(assessment -> {
                    List<String> mappedCLOs = assessment.getCourseAssessmentCLOMappings().stream()
                            .map(mapping -> mapping.getClo().getCode())
                            .collect(Collectors.toList());

                    return CourseAssessmentDTO.builder()
                            .name(assessment.getName())
                            .type(assessment.getKind().name())
                            .weight(assessment.getMaxScore() != null ? assessment.getMaxScore()
                                    : java.math.BigDecimal.ZERO)
                            .durationMinutes(assessment.getDurationMinutes())
                            .mappedCLOs(mappedCLOs)
                            .build();
                })
                .collect(Collectors.toList());

        // Map Materials
        List<CourseMaterialDTO> materials = course.getCourseMaterials().stream()
                .map(material -> CourseMaterialDTO.builder()
                        .name(material.getTitle())
                        .type(material.getMaterialType().name())
                        .url(material.getUrl())
                        .scope(material.getPhase() != null ? "PHASE"
                                : (material.getCourseSession() != null ? "SESSION" : "COURSE"))
                        .build())
                .collect(Collectors.toList());

        return CourseDetailDTO.builder()
                .id(course.getId())
                .basicInfo(basicInfo)
                .clos(clos)
                .structure(structure)
                .assessments(assessments)
                .materials(materials)
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
                }).collect(Collectors.toList());

        int totalMaterials = courseLevelMaterials.size() + phaseMaterials.stream()
                .mapToInt(pm -> pm.getTotalMaterials())
                .sum();

        // accessibleMaterials count is no longer used in frontend, setting to
        // totalMaterials
        int accessibleMaterials = totalMaterials;

        return MaterialHierarchyDTO.builder()
                .courseLevel(courseLevelMaterials)
                .phases(phaseMaterials)
                .totalMaterials(totalMaterials)
                .accessibleMaterials(accessibleMaterials)
                .build();
    }

    @Override
    @Transactional
    public void updateCourse(Long id, CreateCourseRequestDTO request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        // Update Basic Info
        CourseBasicInfoDTO basicInfo = request.getBasicInfo();
        course.setName(basicInfo.getName());
        course.setCode(basicInfo.getCode());
        course.setDescription(basicInfo.getDescription());
        course.setPrerequisites(basicInfo.getPrerequisites());
        course.setTotalHours(basicInfo.getDurationHours());
        course.setDurationWeeks(basicInfo.getDurationWeeks());
        course.setScoreScale(basicInfo.getScoreScale());
        course.setTargetAudience(basicInfo.getTargetAudience());
        course.setTeachingMethods(basicInfo.getTeachingMethods());
        course.setEffectiveDate(basicInfo.getEffectiveDate());
        course.setSessionPerWeek(basicInfo.getSessionPerWeek());
        course.setHoursPerSession(basicInfo.getHoursPerSession());

        if (basicInfo.getSubjectId() != null) {
            Subject subject = subjectRepository.findById(basicInfo.getSubjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
            course.setSubject(subject);
        }

        if (basicInfo.getLevelId() != null) {
            Level level = levelRepository.findById(basicInfo.getLevelId())
                    .orElseThrow(() -> new ResourceNotFoundException("Level not found"));
            course.setLevel(level);
        }

        if (request.getStatus() != null) {
            try {
                course.setStatus(CourseStatus.valueOf(request.getStatus()));
            } catch (IllegalArgumentException e) {
                // Ignore invalid status or handle error
            }
        }

        // Clear existing data (simplified update strategy: delete and recreate
        // children)
        // In a real app, you might want to update existing records to preserve
        // IDs/history
        course.getClos().clear();
        course.getCoursePhases().clear();
        course.getCourseAssessments().clear();
        course.getCourseMaterials().clear();

        // Save to clear children first if needed, or just proceed to add new ones
        // JPA orphanRemoval=true should handle deletion
        courseRepository.save(course);

        // Re-add CLOs
        if (request.getClos() != null) {
            List<CLO> clos = request.getClos().stream()
                    .map(dto -> CLO.builder()
                            .course(course)
                            .code(dto.getCode())
                            .description(dto.getDescription())
                            .build())
                    .collect(Collectors.toList());
            course.getClos().addAll(clos);
        }

        // Re-add Structure (Phases & Sessions)
        if (request.getStructure() != null && request.getStructure().getPhases() != null) {
            int phaseSeq = 1;
            for (CoursePhaseDTO phaseDTO : request.getStructure().getPhases()) {
                CoursePhase phase = CoursePhase.builder()
                        .course(course)
                        .name(phaseDTO.getName())
                        .phaseNumber(phaseSeq++) // Changed from sequenceNo to phaseNumber
                        .build();

                course.getCoursePhases().add(phase);

                if (phaseDTO.getSessions() != null) {
                    int sessionSeq = 1;
                    for (CourseSessionDTO sessionDTO : phaseDTO.getSessions()) {
                        CourseSession session = CourseSession.builder()
                                .phase(phase)
                                .topic(sessionDTO.getTopic())
                                .studentTask(sessionDTO.getStudentTask())
                                .sequenceNo(sessionSeq++)
                                .sequenceNo(sessionSeq++)
                                .skillSet(sessionDTO.getSkillSets() != null ? sessionDTO.getSkillSets().stream()
                                        .map(skillName -> {
                                            try {
                                                return org.fyp.tmssep490be.entities.enums.Skill.valueOf(skillName);
                                            } catch (IllegalArgumentException e) {
                                                return null;
                                            }
                                        })
                                        .filter(java.util.Objects::nonNull)
                                        .collect(java.util.stream.Collectors.toSet()) : new java.util.HashSet<>())
                                .build();

                        phase.getCourseSessions().add(session);

                        // CLO Mappings for Session
                        if (sessionDTO.getMappedCLOs() != null) {
                            for (String cloCode : sessionDTO.getMappedCLOs()) {
                                CLO clo = course.getClos().stream()
                                        .filter(c -> c.getCode().equals(cloCode))
                                        .findFirst()
                                        .orElse(null);
                                if (clo != null) {
                                    CourseSessionCLOMapping mapping = CourseSessionCLOMapping.builder()
                                            .courseSession(session)
                                            .clo(clo)
                                            .build();
                                    session.getCourseSessionCLOMappings().add(mapping);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Re-add Assessments
        if (request.getAssessments() != null) {
            for (CourseAssessmentDTO assessmentDTO : request.getAssessments()) {
                CourseAssessment assessment = CourseAssessment.builder()
                        .course(course)
                        .name(assessmentDTO.getName())
                        .kind(AssessmentKind.valueOf(assessmentDTO.getType()))
                        .durationMinutes(assessmentDTO.getDurationMinutes())
                        // .maxScore(assessmentDTO.getWeight()) // Assuming weight maps to maxScore for
                        // now or need to add weight
                        .maxScore(assessmentDTO.getWeight() != null ? assessmentDTO.getWeight()
                                : java.math.BigDecimal.ZERO)
                        .skills(java.util.Collections.emptySet()) // Default empty skills
                        .build();

                course.getCourseAssessments().add(assessment);

                // CLO Mappings for Assessment
                if (assessmentDTO.getMappedCLOs() != null) {
                    for (String cloCode : assessmentDTO.getMappedCLOs()) {
                        CLO clo = course.getClos().stream()
                                .filter(c -> c.getCode().equals(cloCode))
                                .findFirst()
                                .orElse(null);
                        if (clo != null) {
                            CourseAssessmentCLOMapping mapping = CourseAssessmentCLOMapping.builder()
                                    .courseAssessment(assessment)
                                    .clo(clo)
                                    .build();
                            assessment.getCourseAssessmentCLOMappings().add(mapping);
                        }
                    }
                }
            }
        }

        // Re-add Materials
        if (request.getMaterials() != null) {
            List<CourseMaterial> materials = request.getMaterials().stream()
                    .map(dto -> CourseMaterial.builder()
                            .course(course)
                            .title(dto.getName())
                            .materialType(MaterialType.valueOf(dto.getType()))
                            .url(dto.getUrl())
                            .build())
                    .collect(Collectors.toList());
            course.getCourseMaterials().addAll(materials);
        }

        courseRepository.save(course);
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
                .flatMap(clo -> ploCloMappingRepository.findByCloId(clo.getId()).stream())
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
                enrollment.getClassId());
        int totalSessions = (int) courseSessionRepository.countByCourseId(course.getId());
        int completedSessions = (int) studentSessions.stream()
                .filter(ss -> AttendanceStatus.PRESENT.equals(ss.getAttendanceStatus()))
                .count();

        double progressPercentage = totalSessions > 0 ? (double) completedSessions / totalSessions * 100 : 0.0;

        // Calculate attendance rate
        double attendanceRate = studentSessions.size() > 0 ? (double) completedSessions / studentSessions.size() * 100
                : 0.0;

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
                .classEndDate(classEntity.getActualEndDate() != null ? classEntity.getActualEndDate()
                        : classEntity.getPlannedEndDate())
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
        List<String> skillSets = session.getSkillSet() != null ? session.getSkillSet().stream()
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
                .findByStudentIdAndCourseIdAndStatus(studentId, material.getCourse().getId(),
                        EnrollmentStatus.ENROLLED);

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
        List<CoursePLODTO> relatedPLOs = ploCloMappingRepository.findByCloId(clo.getId())
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
                .programName(plo.getSubject() != null ? plo.getSubject().getName() : null) // Use subject name instead
                                                                                           // of programName
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
                .duration(assessment.getDurationMinutes() != null ? assessment.getDurationMinutes().toString() : null) // Convert
                                                                                                                       // Integer
                                                                                                                       // to
                                                                                                                       // String
                .cloMappings(cloMappings)
                .build();
    }
}
