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
import org.fyp.tmssep490be.services.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.Set;
import java.util.HashSet;

import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final StorageService storageService;

    private final StudentRepository studentRepository;
    private final PLORepository ploRepository;
    private final UserAccountRepository userAccountRepository;

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
                        .requesterName(course.getCreatedBy() != null ? course.getCreatedBy().getFullName() : null)
                        .approvalStatus(course.getApprovalStatus() != null ? course.getApprovalStatus().name() : null)
                        .rejectionReason(course.getRejectionReason())
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
    public CourseDTO createCourse(CreateCourseRequestDTO request, Long userId) {
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
        course.setScoreScale(request.getBasicInfo().getScoreScale());
        course.setTargetAudience(request.getBasicInfo().getTargetAudience());
        course.setTeachingMethods(request.getBasicInfo().getTeachingMethods());
        course.setEffectiveDate(request.getBasicInfo().getEffectiveDate());
        course.setNumberOfSessions(request.getBasicInfo().getNumberOfSessions());
        course.setHoursPerSession(request.getBasicInfo().getHoursPerSession());

        // Calculate total hours if not provided but sessions and hours/session are
        if (course.getTotalHours() == null && course.getNumberOfSessions() != null
                && course.getHoursPerSession() != null) {
            course.setTotalHours(course.getHoursPerSession()
                    .multiply(java.math.BigDecimal.valueOf(course.getNumberOfSessions())).intValue());
        }

        if (request.getStatus() != null) {
            try {
                course.setStatus(CourseStatus.valueOf(request.getStatus()));
            } catch (IllegalArgumentException e) {
                course.setStatus(CourseStatus.DRAFT);
            }
        } else {
            course.setStatus(CourseStatus.DRAFT);
        }

        if (userId != null) {
            UserAccount createdBy = userAccountRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            course.setCreatedBy(createdBy);
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

        // Map PLOs
        for (CourseCLODTO dto : cloDTOs) {
            if (dto.getMappedPLOs() != null && !dto.getMappedPLOs().isEmpty()) {
                CLO clo = clos.stream()
                        .filter(c -> c.getCode().equals(dto.getCode()))
                        .findFirst()
                        .orElse(null);

                if (clo != null) {
                    List<PLO> plos = ploRepository.findByCodeIn(dto.getMappedPLOs());
                    List<PLOCLOMapping> mappings = new ArrayList<>();

                    for (PLO plo : plos) {
                        PLOCLOMapping mapping = PLOCLOMapping.builder()
                                .id(new PLOCLOMapping.PLOCLOMappingId())
                                .plo(plo)
                                .clo(clo)
                                .status(MappingStatus.ACTIVE)
                                .build();
                        mappings.add(mapping);
                    }
                    ploCloMappingRepository.saveAll(mappings);
                }
            }
        }

        return clos.stream().collect(Collectors.toMap(CLO::getCode, Function.identity()));
    }

    private void createStructure(Course course, CourseStructureDTO structureDTO, Map<String, CLO> cloMap) {
        if (structureDTO == null || structureDTO.getPhases() == null)
            return;

        int phaseSeq = 1;
        for (CoursePhaseDTO phaseDTO : structureDTO.getPhases()) {
            CoursePhase phase = new CoursePhase();
            phase.setCourse(course);
            phase.setName(phaseDTO.getName());
            phase.setPhaseNumber(phaseSeq++);
            phase.setDescription(phaseDTO.getDescription());
            phase = coursePhaseRepository.save(phase);

            // Save Phase Materials
            if (phaseDTO.getMaterials() != null) {
                for (CourseMaterialDTO materialDTO : phaseDTO.getMaterials()) {
                    CourseMaterial material = CourseMaterial.builder()
                            .course(course)
                            .phase(phase)
                            .title(materialDTO.getName())
                            .materialType(MaterialType.valueOf(materialDTO.getType()))
                            .url(materialDTO.getUrl())
                            .build();
                    courseMaterialRepository.save(material);
                }
            }

            // Create Sessions for this Phase
            if (phaseDTO.getSessions() != null) {
                int sessionSeq = 1;
                for (CourseSessionDTO sessionDTO : phaseDTO.getSessions()) {
                    CourseSession session = new CourseSession();
                    session.setPhase(phase);
                    session.setTopic(sessionDTO.getTopic());
                    session.setStudentTask(sessionDTO.getStudentTask());
                    session.setSequenceNo(sessionSeq++);
                    if (sessionDTO.getSkillSets() != null) {
                        session.setSkillSet(new ArrayList<>(sessionDTO.getSkillSets().stream()
                                .map(skillName -> {
                                    try {
                                        org.fyp.tmssep490be.entities.enums.Skill.valueOf(skillName);
                                        return skillName;
                                    } catch (IllegalArgumentException e) {
                                        return null;
                                    }
                                })
                                .filter(java.util.Objects::nonNull)
                                .toList()));
                    } else {
                        session.setSkillSet(new ArrayList<>());
                    }

                    session = courseSessionRepository.save(session); // Save session first

                    // Map CLOs to Session
                    if (sessionDTO.getMappedCLOs() != null) {
                        List<CourseSessionCLOMapping> mappings = new ArrayList<>();
                        for (String cloCode : sessionDTO.getMappedCLOs()) {
                            CLO clo = cloMap.get(cloCode);
                            if (clo != null) {
                                CourseSessionCLOMapping mapping = new CourseSessionCLOMapping();
                                mapping.setId(new CourseSessionCLOMapping.CourseSessionCLOMappingId());
                                mapping.setCourseSession(session);
                                mapping.setClo(clo);
                                mapping.setStatus(org.fyp.tmssep490be.entities.enums.MappingStatus.ACTIVE);
                                mappings.add(mapping);
                            }
                        }
                        courseSessionCLOMappingRepository.saveAll(mappings);
                    }

                    // Save Session Materials
                    if (sessionDTO.getMaterials() != null) {
                        for (CourseMaterialDTO materialDTO : sessionDTO.getMaterials()) {
                            CourseMaterial material = CourseMaterial.builder()
                                    .course(course)
                                    .courseSession(session)
                                    .title(materialDTO.getName())
                                    .materialType(MaterialType.valueOf(materialDTO.getType()))
                                    .url(materialDTO.getUrl())
                                    .build();
                            courseMaterialRepository.save(material);
                        }
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
            assessment.setMaxScore(dto.getWeight() != null ? dto.getWeight() : java.math.BigDecimal.ZERO);
            assessment.setDurationMinutes(dto.getDurationMinutes());
            assessment.setDescription(dto.getDescription());
            assessment.setNote(dto.getNote());
            assessment.setSkills(dto.getSkills() != null ? dto.getSkills() : new ArrayList<>());

            assessment = courseAssessmentRepository.save(assessment);

            // Map CLOs to Assessment
            if (dto.getMappedCLOs() != null) {
                List<CourseAssessmentCLOMapping> mappings = new ArrayList<>();
                for (String cloCode : dto.getMappedCLOs()) {
                    CLO clo = cloMap.get(cloCode);
                    if (clo != null) {
                        CourseAssessmentCLOMapping mapping = new CourseAssessmentCLOMapping();
                        mapping.setId(new CourseAssessmentCLOMapping.CourseAssessmentCLOMappingId());
                        mapping.setCourseAssessment(assessment);
                        mapping.setClo(clo);
                        mapping.setStatus(org.fyp.tmssep490be.entities.enums.MappingStatus.ACTIVE);
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
            CourseMaterial.CourseMaterialBuilder builder = CourseMaterial.builder()
                    .course(course)
                    .title(dto.getName())
                    .materialType(MaterialType.valueOf(dto.getType()))
                    .url(dto.getUrl());

            if (dto.getPhaseId() != null) {
                course.getCoursePhases().stream()
                        .filter(p -> p.getId() != null && p.getId().equals(dto.getPhaseId()))
                        .findFirst()
                        .ifPresent(builder::phase);
            }

            return builder.build();
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
                .scoreScale(course.getScoreScale())
                .targetAudience(course.getTargetAudience())
                .teachingMethods(course.getTeachingMethods())
                .effectiveDate(course.getEffectiveDate())
                .numberOfSessions(course.getNumberOfSessions())
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
                .sorted(java.util.Comparator.comparing(CoursePhase::getPhaseNumber))
                .map(phase -> {
                    List<CourseSessionDTO> sessions = phase.getCourseSessions().stream()
                            .sorted(java.util.Comparator.comparing(CourseSession::getSequenceNo))
                            .map(session -> {
                                List<String> mappedCLOs = session.getCourseSessionCLOMappings().stream()
                                        .map(mapping -> mapping.getClo().getCode())
                                        .collect(Collectors.toList());

                                return CourseSessionDTO.builder()
                                        .id(session.getId()) // Ensure ID is returned
                                        .sequenceNo(session.getSequenceNo()) // Ensure sequenceNo is returned
                                        .topic(session.getTopic())
                                        .studentTask(session.getStudentTask())
                                        .skillSets(
                                                session.getSkillSet() != null ? new ArrayList<>(session.getSkillSet())
                                                        : new ArrayList<>())
                                        .mappedCLOs(mappedCLOs)
                                        .build();
                            })
                            .collect(Collectors.toList());

                    // Map Phase Materials
                    List<CourseMaterialDTO> phaseMaterials = phase.getCourseMaterials().stream()
                            .filter(material -> material.getCourseSession() == null) // Filter out session materials
                            .map(material -> CourseMaterialDTO.builder()
                                    .id(material.getId())
                                    .name(material.getTitle())
                                    .type(material.getMaterialType().name())
                                    .url(material.getUrl())
                                    .scope("PHASE")
                                    .phaseId(material.getPhase() != null ? material.getPhase().getId() : null)
                                    .build())
                            .collect(Collectors.toList());

                    return CoursePhaseDTO.builder()
                            .id(phase.getId())
                            .phaseNumber(phase.getPhaseNumber())
                            .name(phase.getName())
                            .description(phase.getDescription())
                            .sessions(sessions)
                            .materials(phaseMaterials)
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
                            .id(assessment.getId())
                            .name(assessment.getName())
                            .type(assessment.getKind().name())
                            .maxScore(assessment.getMaxScore() != null ? assessment.getMaxScore()
                                    : java.math.BigDecimal.ZERO)
                            .durationMinutes(assessment.getDurationMinutes())
                            .description(assessment.getDescription())
                            .note(assessment.getNote())
                            .skills(assessment.getSkills())
                            .mappedCLOs(mappedCLOs)
                            .build();
                })
                .collect(Collectors.toList());

        // Map Materials
        List<CourseMaterialDTO> materials = course.getCourseMaterials().stream()
                .map(material -> CourseMaterialDTO.builder()
                        .id(material.getId())
                        .name(material.getTitle())
                        .type(material.getMaterialType().name())
                        .url(storageService.generatePresignedUrl(material.getUrl()))
                        .scope(material.getPhase() != null ? "PHASE"
                                : (material.getCourseSession() != null ? "SESSION" : "COURSE"))
                        .phaseId(material.getPhase() != null ? material.getPhase().getId()
                                : (material.getCourseSession() != null ? material.getCourseSession().getPhase().getId()
                                        : null))
                        .sessionId(material.getCourseSession() != null ? material.getCourseSession().getId() : null)
                        .build())
                .collect(Collectors.toList());

        return CourseDetailDTO.builder()
                .id(course.getId())
                .name(course.getName())
                .code(course.getCode())
                .description(course.getDescription())
                .subjectId(course.getSubject() != null ? course.getSubject().getId() : null)
                .subjectName(course.getSubject() != null ? course.getSubject().getName() : null)
                .levelId(course.getLevel() != null ? course.getLevel().getId() : null)
                .levelName(course.getLevel() != null ? course.getLevel().getName() : null)
                .basicInfo(basicInfo)
                .status(course.getStatus() != null ? course.getStatus().name() : null)
                .approvalStatus(course.getApprovalStatus() != null ? course.getApprovalStatus().name() : null)
                .totalHours(course.getTotalHours())
                .hoursPerSession(course.getHoursPerSession())
                .scoreScale(course.getScoreScale())
                .prerequisites(course.getPrerequisites())
                .targetAudience(course.getTargetAudience())
                .teachingMethods(course.getTeachingMethods())
                .clos(clos)
                .structure(structure)
                .phases(phases)
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
                .subjectId(course.getSubject() != null ? course.getSubject().getId() : null)
                .subjectName(course.getSubject() != null ? course.getSubject().getName() : null)
                .levelId(course.getLevel() != null ? course.getLevel().getId() : null)
                .levelName(course.getLevel() != null ? course.getLevel().getName() : null)
                .logicalCourseCode(course.getLogicalCourseCode())
                .version(course.getVersion())
                .totalHours(course.getTotalHours())
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
                                        .skillSet(new ArrayList<>(session.getSkillSet()))
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

        return MaterialHierarchyDTO.builder().courseLevel(courseLevelMaterials).phases(phaseMaterials)
                .totalMaterials(totalMaterials).accessibleMaterials(accessibleMaterials).build();
    }

    @Override
    @Transactional
    public CourseDetailDTO updateCourse(Long id, CreateCourseRequestDTO request, Long userId) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        // Update Basic Info
        CourseBasicInfoDTO basicInfo = request.getBasicInfo();
        course.setName(basicInfo.getName());
        course.setCode(basicInfo.getCode());
        course.setDescription(basicInfo.getDescription());
        course.setPrerequisites(basicInfo.getPrerequisites());
        course.setTotalHours(basicInfo.getDurationHours());
        course.setScoreScale(basicInfo.getScoreScale());
        course.setTargetAudience(basicInfo.getTargetAudience());
        course.setTeachingMethods(basicInfo.getTeachingMethods());
        course.setEffectiveDate(basicInfo.getEffectiveDate());
        course.setNumberOfSessions(basicInfo.getNumberOfSessions());
        course.setHoursPerSession(basicInfo.getHoursPerSession());

        // Recalculate total hours
        if (course.getNumberOfSessions() != null && course.getHoursPerSession() != null) {
            course.setTotalHours(course.getHoursPerSession()
                    .multiply(java.math.BigDecimal.valueOf(course.getNumberOfSessions())).intValue());
        }

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

        // Ensure createdBy is set (for legacy courses or if missed during creation)
        if (course.getCreatedBy() == null && userId != null) {
            UserAccount createdBy = userAccountRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            course.setCreatedBy(createdBy);
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
        // Update CLOs (Smart Update to avoid unique constraint violation)
        if (request.getClos() != null) {
            Map<String, CLO> existingClosMap = course.getClos().stream()
                    .collect(Collectors.toMap(CLO::getCode, clo -> clo));

            Set<String> incomingCodes = new HashSet<>();

            for (var cloDTO : request.getClos()) {
                incomingCodes.add(cloDTO.getCode());
                CLO existingClo = existingClosMap.get(cloDTO.getCode());
                CLO cloToProcess;

                if (existingClo != null) {
                    // Update existing CLO
                    existingClo.setDescription(cloDTO.getDescription());
                    cloToProcess = existingClo;
                } else {
                    // Add new CLO
                    CLO newClo = CLO.builder()
                            .course(course)
                            .code(cloDTO.getCode())
                            .description(cloDTO.getDescription())
                            .build();
                    course.getClos().add(newClo);
                    cloToProcess = newClo;
                }

                // Update PLO Mappings for this CLO
                if (cloDTO.getMappedPLOs() != null) {
                    List<PLO> plos = ploRepository.findByCodeIn(cloDTO.getMappedPLOs());

                    // Create a set of PLO IDs to keep
                    Set<Long> ploIdsToKeep = plos.stream().map(PLO::getId).collect(Collectors.toSet());

                    // Remove mappings not in the list
                    cloToProcess.getPloCloMappings().removeIf(m -> !ploIdsToKeep.contains(m.getPlo().getId()));

                    // Add new mappings
                    Set<Long> existingPloIds = cloToProcess.getPloCloMappings().stream()
                            .map(m -> m.getPlo().getId())
                            .collect(Collectors.toSet());

                    for (PLO plo : plos) {
                        if (!existingPloIds.contains(plo.getId())) {
                            PLOCLOMapping mapping = PLOCLOMapping.builder()
                                    .id(new PLOCLOMapping.PLOCLOMappingId())
                                    .plo(plo)
                                    .clo(cloToProcess)
                                    .status(org.fyp.tmssep490be.entities.enums.MappingStatus.ACTIVE)
                                    .build();
                            cloToProcess.getPloCloMappings().add(mapping);
                        }
                    }
                }
            }

            // Remove CLOs that are not in the request
            course.getClos().removeIf(clo -> !incomingCodes.contains(clo.getCode()));
        } else {
            course.getClos().clear();
        }

        // Update Structure (Phases & Sessions)
        if (request.getStructure() != null && request.getStructure().getPhases() != null) {
            // Temporary: Set phase numbers to negative to avoid unique constraint violation
            // during reordering
            if (!course.getCoursePhases().isEmpty()) {
                for (CoursePhase phase : course.getCoursePhases()) {
                    phase.setPhaseNumber(-phase.getPhaseNumber() - 1000); // Ensure unique negative
                }
                coursePhaseRepository.saveAll(course.getCoursePhases());
                coursePhaseRepository.flush();
            }

            Map<Long, CoursePhase> existingPhasesMap = course.getCoursePhases().stream()
                    .collect(Collectors.toMap(CoursePhase::getId, phase -> phase));

            Set<Long> keptPhaseIds = new HashSet<>();
            int phaseSeq = 1;

            for (CoursePhaseDTO phaseDTO : request.getStructure().getPhases()) {
                CoursePhase phase;
                if (phaseDTO.getId() != null && existingPhasesMap.containsKey(phaseDTO.getId())) {
                    phase = existingPhasesMap.get(phaseDTO.getId());
                    phase.setName(phaseDTO.getName());
                    phase.setDescription(phaseDTO.getDescription());
                    phase.setPhaseNumber(phaseSeq++);
                    keptPhaseIds.add(phase.getId());

                    // Update Phase Materials
                    if (phaseDTO.getMaterials() != null) {
                        Map<Long, CourseMaterial> existingPhaseMaterialsMap = phase.getCourseMaterials().stream()
                                .filter(m -> m.getCourseSession() == null) // Only phase-level materials
                                .collect(Collectors.toMap(CourseMaterial::getId, Function.identity()));

                        Set<Long> incomingPhaseMaterialIds = phaseDTO.getMaterials().stream()
                                .map(CourseMaterialDTO::getId)
                                .filter(matId -> matId != null)
                                .collect(Collectors.toSet());

                        // Remove deleted materials
                        phase.getCourseMaterials().removeIf(m -> {
                            if (m.getCourseSession() == null && !incomingPhaseMaterialIds.contains(m.getId())) {
                                if (m.getMaterialType() != MaterialType.LINK && m.getUrl() != null) {
                                    storageService.deleteFile(m.getUrl());
                                }
                                return true;
                            }
                            return false;
                        });

                        // Add/Update materials
                        for (CourseMaterialDTO materialDTO : phaseDTO.getMaterials()) {
                            if (materialDTO.getId() != null
                                    && existingPhaseMaterialsMap.containsKey(materialDTO.getId())) {
                                CourseMaterial existingMaterial = existingPhaseMaterialsMap.get(materialDTO.getId());
                                existingMaterial.setTitle(materialDTO.getName());
                                existingMaterial.setMaterialType(MaterialType.valueOf(materialDTO.getType()));
                                existingMaterial.setUrl(storageService.extractKeyFromUrl(materialDTO.getUrl()));
                            } else {
                                CourseMaterial newMaterial = CourseMaterial.builder()
                                        .course(course)
                                        .phase(phase)
                                        .title(materialDTO.getName())
                                        .materialType(MaterialType.valueOf(materialDTO.getType()))
                                        .url(storageService.extractKeyFromUrl(materialDTO.getUrl()))
                                        .build();
                                phase.getCourseMaterials().add(newMaterial);
                            }
                        }
                    } else {
                        // If no materials are provided in DTO, remove all existing phase materials
                        phase.getCourseMaterials().removeIf(m -> {
                            if (m.getCourseSession() == null) {
                                if (m.getMaterialType() != MaterialType.LINK && m.getUrl() != null) {
                                    storageService.deleteFile(m.getUrl());
                                }
                                return true;
                            }
                            return false;
                        });
                    }

                } else {
                    phase = CoursePhase.builder()
                            .course(course)
                            .name(phaseDTO.getName())
                            .description(phaseDTO.getDescription())
                            .phaseNumber(phaseSeq++)
                            .build();
                    course.getCoursePhases().add(phase);
                }

                // Update Sessions for this Phase
                if (phaseDTO.getSessions() != null) {
                    Map<Long, CourseSession> existingSessionsMap = phase.getCourseSessions().stream()
                            .collect(Collectors.toMap(CourseSession::getId, session -> session));
                    Set<Long> keptSessionIds = new HashSet<>();
                    int sessionSeq = 1;

                    for (CourseSessionDTO sessionDTO : phaseDTO.getSessions()) {
                        CourseSession session;
                        if (sessionDTO.getId() != null && existingSessionsMap.containsKey(sessionDTO.getId())) {
                            session = existingSessionsMap.get(sessionDTO.getId());
                            session.setTopic(sessionDTO.getTopic());
                            session.setStudentTask(sessionDTO.getStudentTask());
                            session.setSequenceNo(sessionSeq++);
                            keptSessionIds.add(session.getId());
                        } else {
                            session = CourseSession.builder()
                                    .phase(phase)
                                    .topic(sessionDTO.getTopic())
                                    .studentTask(sessionDTO.getStudentTask())
                                    .sequenceNo(sessionSeq++)
                                    .build();
                            phase.getCourseSessions().add(session);
                        }

                        // Update SkillSets
                        if (sessionDTO.getSkillSets() != null) {
                            session.setSkillSet(new java.util.ArrayList<>(sessionDTO.getSkillSets().stream()
                                    .map(skillName -> {
                                        try {
                                            org.fyp.tmssep490be.entities.enums.Skill.valueOf(skillName);
                                            return skillName;
                                        } catch (IllegalArgumentException e) {
                                            return null;
                                        }
                                    })
                                    .filter(java.util.Objects::nonNull)
                                    .toList()));
                        } else {
                            session.setSkillSet(new java.util.ArrayList<>());
                        }

                        // Update CLO Mappings for Session
                        if (sessionDTO.getMappedCLOs() != null) {
                            Set<String> incomingCloCodes = new HashSet<>(sessionDTO.getMappedCLOs());

                            // Remove mappings not in the request
                            session.getCourseSessionCLOMappings()
                                    .removeIf(mapping -> !incomingCloCodes.contains(mapping.getClo().getCode()));

                            // Add new mappings
                            Set<String> existingCloCodes = session.getCourseSessionCLOMappings().stream()
                                    .map(mapping -> mapping.getClo().getCode())
                                    .collect(Collectors.toSet());

                            for (String cloCode : incomingCloCodes) {
                                if (!existingCloCodes.contains(cloCode)) {
                                    CLO clo = course.getClos().stream()
                                            .filter(c -> c.getCode().equals(cloCode))
                                            .findFirst()
                                            .orElse(null);
                                    if (clo != null) {
                                        CourseSessionCLOMapping mapping = CourseSessionCLOMapping.builder()
                                                .id(new CourseSessionCLOMapping.CourseSessionCLOMappingId())
                                                .courseSession(session)
                                                .clo(clo)
                                                .status(MappingStatus.ACTIVE)
                                                .build();
                                        session.getCourseSessionCLOMappings().add(mapping);
                                    }
                                }
                            }
                        } else {
                            session.getCourseSessionCLOMappings().clear();
                        }

                        // Update Session Materials
                        if (sessionDTO.getMaterials() != null) {
                            Set<Long> incomingMaterialIds = sessionDTO.getMaterials().stream()
                                    .map(CourseMaterialDTO::getId)
                                    .filter(java.util.Objects::nonNull)
                                    .collect(Collectors.toSet());

                            // Remove materials not in the request
                            session.getCourseMaterials()
                                    .removeIf(m -> m.getId() != null && !incomingMaterialIds.contains(m.getId()));

                            for (CourseMaterialDTO materialDTO : sessionDTO.getMaterials()) {
                                CourseMaterial material;
                                if (materialDTO.getId() != null) {
                                    material = session.getCourseMaterials().stream()
                                            .filter(m -> m.getId().equals(materialDTO.getId()))
                                            .findFirst()
                                            .orElse(null);
                                    if (material != null) {
                                        // Update existing
                                        material.setTitle(materialDTO.getName());
                                        material.setMaterialType(MaterialType.valueOf(materialDTO.getType()));
                                        material.setUrl(storageService.extractKeyFromUrl(materialDTO.getUrl()));
                                    } else {
                                        // Should not happen if ID is valid, but treat as new if not found
                                        material = CourseMaterial.builder()
                                                .course(course)
                                                .courseSession(session)
                                                .title(materialDTO.getName())
                                                .materialType(MaterialType.valueOf(materialDTO.getType()))
                                                .url(storageService.extractKeyFromUrl(materialDTO.getUrl()))
                                                .build();
                                        session.getCourseMaterials().add(material);
                                    }
                                } else {
                                    // Create new
                                    material = CourseMaterial.builder()
                                            .course(course)
                                            .courseSession(session)
                                            .title(materialDTO.getName())
                                            .materialType(MaterialType.valueOf(materialDTO.getType()))
                                            .url(storageService.extractKeyFromUrl(materialDTO.getUrl()))
                                            .build();
                                    session.getCourseMaterials().add(material);
                                }
                            }
                        } else {
                            session.getCourseMaterials().clear();
                        }
                    }
                    phase.getCourseSessions().removeIf(s -> s.getId() != null && !keptSessionIds.contains(s.getId()));
                } else {
                    phase.getCourseSessions().clear();
                }
            }
            course.getCoursePhases().removeIf(p -> p.getId() != null && !keptPhaseIds.contains(p.getId()));
        } else {
            course.getCoursePhases().clear();
        }

        // Update Assessments
        if (request.getAssessments() != null) {
            Map<Long, CourseAssessment> existingAssessmentsMap = course.getCourseAssessments().stream()
                    .collect(Collectors.toMap(CourseAssessment::getId, a -> a));
            Set<Long> keptAssessmentIds = new HashSet<>();

            for (CourseAssessmentDTO assessmentDTO : request.getAssessments()) {
                CourseAssessment assessment;
                if (assessmentDTO.getId() != null && existingAssessmentsMap.containsKey(assessmentDTO.getId())) {
                    assessment = existingAssessmentsMap.get(assessmentDTO.getId());
                    assessment.setName(assessmentDTO.getName());
                    assessment.setKind(AssessmentKind.valueOf(assessmentDTO.getType()));
                    assessment.setDurationMinutes(assessmentDTO.getDurationMinutes());
                    assessment.setMaxScore(
                            assessmentDTO.getWeight() != null ? assessmentDTO.getWeight() : java.math.BigDecimal.ZERO);
                    assessment.setDescription(assessmentDTO.getDescription());
                    assessment.setNote(assessmentDTO.getNote());
                    assessment.setSkills(assessmentDTO.getSkills() != null ? assessmentDTO.getSkills()
                            : new java.util.ArrayList<>());
                    keptAssessmentIds.add(assessment.getId());
                } else {
                    assessment = CourseAssessment.builder()
                            .course(course)
                            .name(assessmentDTO.getName())
                            .kind(AssessmentKind.valueOf(assessmentDTO.getType()))
                            .durationMinutes(assessmentDTO.getDurationMinutes())
                            .maxScore(assessmentDTO.getWeight() != null ? assessmentDTO.getWeight()
                                    : java.math.BigDecimal.ZERO)
                            .description(assessmentDTO.getDescription())
                            .note(assessmentDTO.getNote())
                            .skills(assessmentDTO.getSkills() != null ? assessmentDTO.getSkills()
                                    : new java.util.ArrayList<>())
                            .build();
                    course.getCourseAssessments().add(assessment);
                }
                // CLO Mappings for Assessment
                if (assessmentDTO.getMappedCLOs() != null) {
                    Set<String> incomingCloCodes = new HashSet<>(assessmentDTO.getMappedCLOs());

                    // Remove mappings not in the request
                    assessment.getCourseAssessmentCLOMappings()
                            .removeIf(mapping -> !incomingCloCodes.contains(mapping.getClo().getCode()));

                    // Add new mappings
                    Set<String> existingCloCodes = assessment.getCourseAssessmentCLOMappings().stream()
                            .map(mapping -> mapping.getClo().getCode())
                            .collect(Collectors.toSet());

                    for (String cloCode : incomingCloCodes) {
                        if (!existingCloCodes.contains(cloCode)) {
                            CLO clo = course.getClos().stream()
                                    .filter(c -> c.getCode().equals(cloCode))
                                    .findFirst()
                                    .orElse(null);
                            if (clo != null) {
                                CourseAssessmentCLOMapping mapping = CourseAssessmentCLOMapping.builder()
                                        .id(new CourseAssessmentCLOMapping.CourseAssessmentCLOMappingId())
                                        .courseAssessment(assessment)
                                        .clo(clo)
                                        .status(MappingStatus.ACTIVE)
                                        .build();
                                assessment.getCourseAssessmentCLOMappings().add(mapping);
                            }
                        }
                    }
                } else {
                    assessment.getCourseAssessmentCLOMappings().clear();
                }
            }
        }

        // Update Materials
        if (request.getMaterials() != null) {
            // Get existing materials map
            Map<Long, CourseMaterial> existingMaterialsMap = course.getCourseMaterials().stream()
                    .collect(Collectors.toMap(CourseMaterial::getId, Function.identity()));

            // Identify materials to remove
            Set<Long> incomingMaterialIds = request.getMaterials().stream()
                    .map(CourseMaterialDTO::getId)
                    .filter(materialId -> materialId != null)
                    .collect(Collectors.toSet());

            List<CourseMaterial> materialsToRemove = course.getCourseMaterials().stream()
                    .filter(m -> !incomingMaterialIds.contains(m.getId()))
                    .filter(m -> m.getCourseSession() == null) // Only delete if NOT session-scoped
                    .filter(m -> m.getPhase() == null) // Only delete if NOT phase-scoped
                    .collect(Collectors.toList());

            // Delete files from S3 for removed materials (if not LINK type)
            for (CourseMaterial material : materialsToRemove) {
                if (material.getMaterialType() != MaterialType.LINK && material.getUrl() != null) {
                    storageService.deleteFile(material.getUrl());
                }
            }

            // Remove from collection (orphanRemoval will delete from DB)
            course.getCourseMaterials().removeAll(materialsToRemove);

            // Add/Update materials
            List<CourseMaterial> materials = request.getMaterials().stream().map(dto -> {
                CourseMaterial.CourseMaterialBuilder builder;

                if (dto.getId() != null && existingMaterialsMap.containsKey(dto.getId())) {
                    // Update existing
                    CourseMaterial existing = existingMaterialsMap.get(dto.getId());

                    existing.setTitle(dto.getName());
                    existing.setMaterialType(MaterialType.valueOf(dto.getType()));
                    existing.setUrl(dto.getUrl());

                    // Reset relations
                    existing.setPhase(null);
                    existing.setCourseSession(null);

                    builder = null; // Mark as processed
                } else {
                    // Create new
                    builder = CourseMaterial.builder()
                            .course(course)
                            .title(dto.getName())
                            .materialType(MaterialType.valueOf(dto.getType()))
                            .url(dto.getUrl());
                }

                CourseMaterial material = (builder != null) ? builder.build() : existingMaterialsMap.get(dto.getId());

                // Link Phase
                if (dto.getPhaseId() != null) {
                    course.getCoursePhases().stream()
                            .filter(p -> p.getId() != null && p.getId().equals(dto.getPhaseId()))
                            .findFirst()
                            .ifPresent(material::setPhase);
                }

                // Link Session
                if (dto.getSessionId() != null) {
                    course.getCoursePhases().stream()
                            .flatMap(p -> p.getCourseSessions().stream())
                            .filter(s -> s.getId() != null && s.getId().equals(dto.getSessionId()))
                            .findFirst()
                            .ifPresent(material::setCourseSession);
                }

                return material;
            }).collect(Collectors.toList());

            // Add new materials (updates were done in-place)
            List<CourseMaterial> newMaterials = materials.stream()
                    .filter(m -> m.getId() == null)
                    .collect(Collectors.toList());

            course.getCourseMaterials().addAll(newMaterials);
        } else {
            // If materials list is null (meaning clear all?), or just not provided?
            // Usually if not provided we might skip. But if provided as empty list, we
            // clear.
            // Let's assume null means "no change" or "clear"?
            // In createCourse it was "createMaterialsFromDTO".
            // Here, if request.getMaterials() is null, we might skip update.
            // But if user wants to delete all, they send empty list.
            // So if null, do nothing.
        }

        Course savedCourse = courseRepository.save(course);

        return getCourseDetails(savedCourse.getId());
    }

    @Override
    @Transactional
    public void submitCourse(Long id) {
        log.info("Submitting course for approval: {}", id);
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        // Only allow submission if status is DRAFT or REJECTED
        if (course.getStatus() != CourseStatus.DRAFT && course.getStatus() != CourseStatus.REJECTED) {
            throw new IllegalStateException("Only DRAFT or REJECTED courses can be submitted");
        }

        course.setStatus(CourseStatus.SUBMITTED);
        course.setApprovalStatus(ApprovalStatus.PENDING);
        courseRepository.save(course);
        log.info("Course {} submitted successfully", id);
    }

    @Override
    @Transactional
    public void approveCourse(Long id) {
        log.info("Approving course with ID: {}", id);
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        if (course.getStatus() != CourseStatus.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED courses can be approved");
        }

        course.setStatus(CourseStatus.ACTIVE);
        course.setApprovalStatus(ApprovalStatus.APPROVED);
        courseRepository.save(course);
        log.info("Course {} approved successfully", id);
    }

    @Override
    @Transactional
    public void rejectCourse(Long id, String reason) {
        log.info("Rejecting course with ID: {}", id);
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        if (course.getStatus() != CourseStatus.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED courses can be rejected");
        }

        course.setStatus(CourseStatus.DRAFT);
        course.setApprovalStatus(ApprovalStatus.REJECTED);
        course.setRejectionReason(reason);

        log.info("Course {} rejected. Reason: {}", id, reason);

        courseRepository.save(course);
    }

    @Override
    @Transactional
    public void deactivateCourse(Long id) {
        log.info("Deactivating course with ID: {}", id);
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        course.setStatus(CourseStatus.INACTIVE);
        courseRepository.save(course);
    }

    @Override
    @Transactional
    public void reactivateCourse(Long id) {
        log.info("Reactivating course with ID: {}", id);
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        // Check if course is valid enough to be active
        if (course.getClos() != null && !course.getClos().isEmpty() &&
                course.getCoursePhases() != null && !course.getCoursePhases().isEmpty()) {
            course.setStatus(CourseStatus.ACTIVE);
        } else {
            course.setStatus(CourseStatus.DRAFT);
        }
        courseRepository.save(course);
    }

    @Override
    public List<CoursePLODTO> getCoursePLOs(Long courseId) {
        log.debug("Getting PLOs for course {}", courseId);

        if (!courseRepository.existsById(courseId)) {
            log.error("Course not found: {}", courseId);
            throw new IllegalArgumentException("Course not found: " + courseId);
        }

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
        List<String> skillSets = session.getSkillSet() != null ? new ArrayList<>(session.getSkillSet()) : List.of();

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

        return CourseMaterialDTO.builder()
                .id(material.getId())
                .name(material.getTitle())
                .type(material.getMaterialType() != null ? material.getMaterialType().toString() : null)
                .url(material.getUrl())
                .scope(level)
                .build();

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
