package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.CourseDTO;
import org.fyp.tmssep490be.dtos.course.*;
import org.fyp.tmssep490be.dtos.course.CourseDetailDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.CourseStatus;
import org.fyp.tmssep490be.entities.enums.AssessmentKind;
import org.fyp.tmssep490be.entities.enums.MaterialType;
import org.fyp.tmssep490be.entities.enums.Skill;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.CourseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final PLORepository ploRepository;
    private final PLOCLOMappingRepository ploCloMappingRepository;
    private final CoursePhaseRepository coursePhaseRepository;
    private final CourseSessionRepository courseSessionRepository;
    private final CourseSessionCLOMappingRepository courseSessionCLOMappingRepository;
    private final CourseAssessmentRepository courseAssessmentRepository;
    private final CourseAssessmentCLOMappingRepository courseAssessmentCLOMappingRepository;
    private final CourseMaterialRepository courseMaterialRepository;

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
        createMaterials(course, request.getMaterials());

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
            assessment.setSkills(new org.fyp.tmssep490be.entities.enums.Skill[] {}); // Empty skills for now
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

    private void createMaterials(Course course, List<CourseMaterialDTO> materialDTOs) {
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
                            .weight(assessment.getMaxScore() != null ? assessment.getMaxScore().doubleValue() : 0.0)
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
                                : (material.getCourseSession() != null ? "SESSION" : "COURSE")) // Infer scope
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
                        .maxScore(new java.math.BigDecimal(
                                assessmentDTO.getWeight() != null ? assessmentDTO.getWeight() : 0))
                        .skills(new Skill[] {}) // Default empty skills
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
}
