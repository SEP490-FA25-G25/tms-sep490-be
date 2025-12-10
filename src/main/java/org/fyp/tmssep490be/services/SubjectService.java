package org.fyp.tmssep490be.services;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.subject.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final CurriculumRepository curriculumRepository;
    private final LevelRepository levelRepository;
    private final SubjectCLORepository subjectCLORepository;
    private final PLORepository ploRepository;
    private final PLOCLOMappingRepository ploCloMappingRepository;
    private final SubjectPhaseRepository subjectPhaseRepository;
    private final SubjectSessionRepository subjectSessionRepository;
    private final SubjectSessionCLOMappingRepository subjectSessionCLOMappingRepository;
    private final SubjectAssessmentRepository subjectAssessmentRepository;
    private final SubjectAssessmentCLOMappingRepository subjectAssessmentCLOMappingRepository;
    private final SubjectMaterialRepository subjectMaterialRepository;
    private final UserAccountRepository userAccountRepository;
    private final EntityManager entityManager;

    // ========== GET ALL SUBJECTS ==========
    public List<SubjectDTO> getAllSubjects(Long curriculumId, Long levelId) {
        log.debug("Getting all subjects with filters - curriculumId: {}, levelId: {}", curriculumId, levelId);

        List<Subject> subjects;

        if (curriculumId != null && levelId != null) {
            subjects = subjectRepository.findByCurriculumIdAndLevelIdOrderByUpdatedAtDesc(curriculumId, levelId);
        } else if (curriculumId != null) {
            subjects = subjectRepository.findByCurriculumIdOrderByUpdatedAtDesc(curriculumId);
        } else if (levelId != null) {
            subjects = subjectRepository.findByLevelIdOrderByUpdatedAtDesc(levelId);
        } else {
            subjects = subjectRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"));
        }

        return subjects.stream()
                .map(subject -> SubjectDTO.builder()
                        .id(subject.getId())
                        .code(subject.getCode())
                        .name(subject.getName())
                        .subjectName(subject.getCurriculum() != null ? subject.getCurriculum().getName() : null)
                        .levelName(subject.getLevel() != null ? subject.getLevel().getName() : null)
                        .status(subject.getStatus() != null ? subject.getStatus().name() : null)
                        .approvalStatus(subject.getApprovalStatus() != null ? subject.getApprovalStatus().name() : null)
                        .rejectionReason(subject.getRejectionReason())
                        .submittedAt(subject.getSubmittedAt())
                        .decidedAt(subject.getDecidedAt())
                        .effectiveDate(subject.getEffectiveDate())
                        .createdAt(subject.getCreatedAt())
                        .updatedAt(subject.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ========== CREATE SUBJECT ==========
    @Transactional
    public SubjectDetailDTO createSubject(CreateSubjectRequestDTO request, Long userId) {
        log.info("Creating new subject: {}", request.getBasicInfo().getName());

        // 1. Validate and Create Subject Entity
        Curriculum curriculum = curriculumRepository.findById(request.getBasicInfo().getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khung chương trình"));
        Level level = levelRepository.findById(request.getBasicInfo().getLevelId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấp độ"));

        Subject subject = new Subject();
        subject.setCurriculum(curriculum);
        subject.setLevel(level);
        subject.setCode(request.getBasicInfo().getCode());
        subject.setName(request.getBasicInfo().getName());
        subject.setDescription(request.getBasicInfo().getDescription());
        subject.setPrerequisites(request.getBasicInfo().getPrerequisites());
        subject.setTotalHours(request.getBasicInfo().getDurationHours());
        subject.setScoreScale(request.getBasicInfo().getScoreScale());
        subject.setTargetAudience(request.getBasicInfo().getTargetAudience());
        subject.setTeachingMethods(request.getBasicInfo().getTeachingMethods());
        subject.setEffectiveDate(request.getBasicInfo().getEffectiveDate());
        subject.setNumberOfSessions(request.getBasicInfo().getNumberOfSessions());
        subject.setHoursPerSession(request.getBasicInfo().getHoursPerSession());
        subject.setThumbnailUrl(request.getBasicInfo().getThumbnailUrl());

        // Calculate total hours if not provided but sessions and hours/session are
        if (subject.getTotalHours() == null && subject.getNumberOfSessions() != null
                && subject.getHoursPerSession() != null) {
            subject.setTotalHours(subject.getHoursPerSession()
                    .multiply(java.math.BigDecimal.valueOf(subject.getNumberOfSessions())).intValue());
        }

        // Set status
        if (request.getStatus() != null) {
            try {
                subject.setStatus(SubjectStatus.valueOf(request.getStatus()));
            } catch (IllegalArgumentException e) {
                subject.setStatus(SubjectStatus.DRAFT);
            }
        } else {
            subject.setStatus(SubjectStatus.DRAFT);
        }

        // Set approval status based on subject status
        if (subject.getStatus() == SubjectStatus.SUBMITTED) {
            subject.setApprovalStatus(ApprovalStatus.PENDING);
            subject.setSubmittedAt(OffsetDateTime.now());
        } else if (subject.getStatus() == SubjectStatus.DRAFT) {
            subject.setApprovalStatus(null);
            subject.setSubmittedAt(null);
        }

        // Set createdBy
        if (userId != null) {
            UserAccount createdBy = userAccountRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
            subject.setCreatedBy(createdBy);
        }

        subject = subjectRepository.save(subject);
        log.info("Subject saved with ID: {}", subject.getId());

        // 2. Create CLOs
        Map<String, CLO> cloMap = createCLOs(subject, request.getClos());

        // 3. Create Structure (Phases & Sessions)
        createStructure(subject, request.getStructure(), cloMap);

        // 4. Create Assessments
        createAssessments(subject, request.getAssessments(), cloMap);

        // 5. Create Materials
        createMaterials(subject, request.getMaterials());

        log.info("Subject creation completed successfully");

        // Flush all changes and clear persistence context to ensure fresh data load
        entityManager.flush();
        entityManager.clear();

        // Return full subject details
        return getSubjectDetails(subject.getId());
    }

    // ========== CREATE CLOs ==========
    private Map<String, CLO> createCLOs(Subject subject, List<SubjectCLODTO> cloDTOs) {
        if (cloDTOs == null) {
            return Map.of();
        }

        List<CLO> clos = new ArrayList<>();
        for (SubjectCLODTO dto : cloDTOs) {
            CLO clo = new CLO();
            clo.setSubject(subject);
            clo.setCode(dto.getCode());
            clo.setDescription(dto.getDescription());
            clos.add(clo);
        }
        clos = subjectCLORepository.saveAll(clos);

        // Map PLOs
        for (SubjectCLODTO dto : cloDTOs) {
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

    // ========== CREATE STRUCTURE (Phases & Sessions) ==========
    private void createStructure(Subject subject, SubjectStructureDTO structureDTO, Map<String, CLO> cloMap) {
        if (structureDTO == null || structureDTO.getPhases() == null) {
            return;
        }

        int phaseSeq = 1;
        for (SubjectPhaseDTO phaseDTO : structureDTO.getPhases()) {
            SubjectPhase phase = new SubjectPhase();
            phase.setSubject(subject);
            phase.setName(phaseDTO.getName());
            phase.setPhaseNumber(phaseSeq++);
            phase.setLearningFocus(phaseDTO.getDescription());
            phase = subjectPhaseRepository.save(phase);

            // Save Phase Materials
            if (phaseDTO.getMaterials() != null) {
                for (SubjectMaterialDTO materialDTO : phaseDTO.getMaterials()) {
                    SubjectMaterial material = SubjectMaterial.builder()
                            .subject(subject)
                            .phase(phase)
                            .title(materialDTO.getTitle())
                            .materialType(materialDTO.getMaterialType() != null
                                    ? MaterialType.valueOf(materialDTO.getMaterialType())
                                    : MaterialType.OTHER)
                            .url(materialDTO.getUrl() != null ? materialDTO.getUrl() : "")
                            .build();
                    subjectMaterialRepository.save(material);
                }
            }

            // Create Sessions for this Phase
            if (phaseDTO.getSessions() != null) {
                int sessionSeq = 1;
                for (SubjectSessionDTO sessionDTO : phaseDTO.getSessions()) {
                    SubjectSession session = new SubjectSession();
                    session.setPhase(phase);
                    session.setTopic(sessionDTO.getTopic());
                    session.setStudentTask(sessionDTO.getStudentTask());
                    session.setSequenceNo(sessionSeq++);
                    session.setSkills(parseSkillList(sessionDTO.getSkills()));

                    session = subjectSessionRepository.save(session);

                    // Map CLOs to Session
                    if (sessionDTO.getMappedCLOs() != null) {
                        List<SubjectSessionCLOMapping> mappings = new ArrayList<>();
                        for (String cloCode : sessionDTO.getMappedCLOs()) {
                            CLO clo = cloMap.get(cloCode);
                            if (clo != null) {
                                SubjectSessionCLOMapping mapping = new SubjectSessionCLOMapping();
                                mapping.setId(new SubjectSessionCLOMapping.SubjectSessionCLOMappingId());
                                mapping.setSubjectSession(session);
                                mapping.setClo(clo);
                                mapping.setStatus(MappingStatus.ACTIVE);
                                mappings.add(mapping);
                            }
                        }
                        subjectSessionCLOMappingRepository.saveAll(mappings);
                    }

                    // Save Session Materials
                    if (sessionDTO.getMaterials() != null) {
                        for (SubjectMaterialDTO materialDTO : sessionDTO.getMaterials()) {
                            SubjectMaterial material = SubjectMaterial.builder()
                                    .subject(subject)
                                    .subjectSession(session)
                                    .title(materialDTO.getTitle())
                                    .materialType(materialDTO.getMaterialType() != null
                                            ? MaterialType.valueOf(materialDTO.getMaterialType())
                                            : MaterialType.OTHER)
                                    .url(materialDTO.getUrl() != null ? materialDTO.getUrl() : "")
                                    .build();
                            subjectMaterialRepository.save(material);
                        }
                    }
                }
            }
        }
    }

    // ========== CREATE ASSESSMENTS ==========
    private void createAssessments(Subject subject, List<SubjectAssessmentDTO> assessmentDTOs,
            Map<String, CLO> cloMap) {
        if (assessmentDTOs == null) {
            return;
        }

        for (SubjectAssessmentDTO dto : assessmentDTOs) {
            SubjectAssessment assessment = new SubjectAssessment();
            assessment.setSubject(subject);
            assessment.setName(dto.getName());
            assessment.setKind(AssessmentKind.valueOf(dto.getType()));
            assessment.setMaxScore(dto.getMaxScore() != null ? dto.getMaxScore() : java.math.BigDecimal.ZERO);
            assessment.setDurationMinutes(dto.getDurationMinutes());
            assessment.setDescription(dto.getDescription());
            assessment.setNote(dto.getNote());
            assessment.setSkills(parseSkillList(dto.getSkills()));

            assessment = subjectAssessmentRepository.save(assessment);

            // Map CLOs to Assessment
            if (dto.getMappedCLOs() != null) {
                List<SubjectAssessmentCLOMapping> mappings = new ArrayList<>();
                for (String cloCode : dto.getMappedCLOs()) {
                    CLO clo = cloMap.get(cloCode);
                    if (clo != null) {
                        SubjectAssessmentCLOMapping mapping = new SubjectAssessmentCLOMapping();
                        mapping.setId(new SubjectAssessmentCLOMapping.SubjectAssessmentCLOMappingId());
                        mapping.setSubjectAssessment(assessment);
                        mapping.setClo(clo);
                        mapping.setStatus(MappingStatus.ACTIVE);
                        mappings.add(mapping);
                    }
                }
                subjectAssessmentCLOMappingRepository.saveAll(mappings);
            }
        }
    }

    // ========== CREATE MATERIALS ==========
    private void createMaterials(Subject subject, List<SubjectMaterialDTO> materialDTOs) {
        if (materialDTOs == null) {
            return;
        }

        for (SubjectMaterialDTO dto : materialDTOs) {
            SubjectMaterial material = SubjectMaterial.builder()
                    .subject(subject)
                    .title(dto.getTitle())
                    .materialType(dto.getMaterialType() != null
                            ? MaterialType.valueOf(dto.getMaterialType())
                            : MaterialType.OTHER)
                    .url(dto.getUrl() != null ? dto.getUrl() : "")
                    .build();
            subjectMaterialRepository.save(material);
        }
    }

    // ========== GET SUBJECT DETAILS ==========
    public SubjectDetailDTO getSubjectDetails(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));

        // Map Basic Info
        SubjectBasicInfoDTO basicInfo = SubjectBasicInfoDTO.builder()
                .subjectId(subject.getCurriculum() != null ? subject.getCurriculum().getId() : null)
                .levelId(subject.getLevel() != null ? subject.getLevel().getId() : null)
                .name(subject.getName())
                .code(subject.getCode())
                .description(subject.getDescription())
                .prerequisites(subject.getPrerequisites())
                .durationHours(subject.getTotalHours())
                .scoreScale(subject.getScoreScale())
                .targetAudience(subject.getTargetAudience())
                .teachingMethods(subject.getTeachingMethods())
                .effectiveDate(subject.getEffectiveDate())
                .numberOfSessions(subject.getNumberOfSessions())
                .hoursPerSession(subject.getHoursPerSession())
                .thumbnailUrl(subject.getThumbnailUrl())
                .build();

        // Map CLOs
        List<SubjectCLODTO> clos = subject.getClos().stream()
                .map(clo -> {
                    List<String> mappedPLOs = clo.getPloCloMappings().stream()
                            .map(mapping -> mapping.getPlo().getCode())
                            .collect(Collectors.toList());

                    return SubjectCLODTO.builder()
                            .code(clo.getCode())
                            .description(clo.getDescription())
                            .mappedPLOs(mappedPLOs)
                            .build();
                })
                .collect(Collectors.toList());

        // Map Phases
        List<SubjectPhaseDTO> phases = subject.getSubjectPhases().stream()
                .sorted(java.util.Comparator.comparing(SubjectPhase::getPhaseNumber))
                .map(phase -> {
                    List<SubjectSessionDTO> sessions = phase.getSubjectSessions().stream()
                            .sorted(java.util.Comparator.comparing(SubjectSession::getSequenceNo))
                            .map(session -> {
                                List<String> mappedCLOs = session.getSubjectSessionCLOMappings().stream()
                                        .map(mapping -> mapping.getClo().getCode())
                                        .collect(Collectors.toList());

                                List<SubjectMaterialDTO> sessionMaterials = subjectMaterialRepository
                                        .findBySubjectSessionId(session.getId()).stream()
                                        .map(material -> SubjectMaterialDTO.builder()
                                                .id(material.getId())
                                                .title(material.getTitle())
                                                .materialType(material.getMaterialType() != null
                                                        ? material.getMaterialType().name()
                                                        : null)
                                                .url(material.getUrl())
                                                .scope("SESSION")
                                                .sessionId(session.getId())
                                                .build())
                                        .collect(Collectors.toList());

                                return SubjectSessionDTO.builder()
                                        .id(session.getId())
                                        .sequenceNo(session.getSequenceNo())
                                        .topic(session.getTopic())
                                        .studentTask(session.getStudentTask())
                                        .skills(session.getSkills() != null
                                                ? session.getSkills().stream().map(Enum::name).toList()
                                                : new ArrayList<>())
                                        .mappedCLOs(mappedCLOs)
                                        .materials(sessionMaterials)
                                        .build();
                            })
                            .collect(Collectors.toList());

                    List<SubjectMaterialDTO> phaseMaterials = subjectMaterialRepository
                            .findByPhaseIdAndSubjectSessionIsNull(phase.getId()).stream()
                            .map(material -> SubjectMaterialDTO.builder()
                                    .id(material.getId())
                                    .title(material.getTitle())
                                    .materialType(material.getMaterialType() != null
                                            ? material.getMaterialType().name()
                                            : null)
                                    .url(material.getUrl())
                                    .scope("PHASE")
                                    .phaseId(phase.getId())
                                    .build())
                            .collect(Collectors.toList());

                    return SubjectPhaseDTO.builder()
                            .id(phase.getId())
                            .phaseNumber(phase.getPhaseNumber())
                            .name(phase.getName())
                            .description(phase.getLearningFocus())
                            .sessions(sessions)
                            .materials(phaseMaterials)
                            .build();
                })
                .collect(Collectors.toList());

        SubjectStructureDTO structure = SubjectStructureDTO.builder()
                .phases(phases)
                .build();

        // Map Assessments
        List<SubjectAssessmentDTO> assessments = subject.getSubjectAssessments().stream()
                .map(assessment -> {
                    List<String> mappedCLOs = assessment.getSubjectAssessmentCLOMappings().stream()
                            .map(mapping -> mapping.getClo().getCode())
                            .collect(Collectors.toList());

                    return SubjectAssessmentDTO.builder()
                            .id(assessment.getId())
                            .name(assessment.getName())
                            .type(assessment.getKind() != null ? assessment.getKind().name() : null)
                            .maxScore(assessment.getMaxScore() != null ? assessment.getMaxScore()
                                    : java.math.BigDecimal.ZERO)
                            .durationMinutes(assessment.getDurationMinutes())
                            .description(assessment.getDescription())
                            .note(assessment.getNote())
                            .skills(assessment.getSkills() != null
                                    ? assessment.getSkills().stream().map(Enum::name).toList()
                                    : new ArrayList<>())
                            .mappedCLOs(mappedCLOs)
                            .build();
                })
                .collect(Collectors.toList());

        // Map Subject-level Materials
        List<SubjectMaterialDTO> materials = subject.getSubjectMaterials().stream()
                .map(material -> SubjectMaterialDTO.builder()
                        .id(material.getId())
                        .title(material.getTitle())
                        .materialType(material.getMaterialType() != null ? material.getMaterialType().name() : null)
                        .url(material.getUrl())
                        .scope(material.getPhase() != null ? "PHASE"
                                : (material.getSubjectSession() != null ? "SESSION" : "SUBJECT"))
                        .phaseId(material.getPhase() != null ? material.getPhase().getId() : null)
                        .sessionId(material.getSubjectSession() != null ? material.getSubjectSession().getId() : null)
                        .build())
                .collect(Collectors.toList());

        return SubjectDetailDTO.builder()
                .id(subject.getId())
                .name(subject.getName())
                .code(subject.getCode())
                .description(subject.getDescription())
                .thumbnailUrl(subject.getThumbnailUrl())
                .subjectId(subject.getCurriculum() != null ? subject.getCurriculum().getId() : null)
                .subjectName(subject.getCurriculum() != null ? subject.getCurriculum().getName() : null)
                .levelId(subject.getLevel() != null ? subject.getLevel().getId() : null)
                .levelName(subject.getLevel() != null ? subject.getLevel().getName() : null)
                .basicInfo(basicInfo)
                .structure(structure)
                .status(subject.getStatus() != null ? subject.getStatus().name() : null)
                .approvalStatus(subject.getApprovalStatus() != null ? subject.getApprovalStatus().name() : null)
                .submittedAt(subject.getSubmittedAt())
                .decidedAt(subject.getDecidedAt())
                .createdAt(subject.getCreatedAt())
                .totalHours(subject.getTotalHours())
                .numberOfSessions(subject.getNumberOfSessions())
                .hoursPerSession(subject.getHoursPerSession())
                .scoreScale(subject.getScoreScale())
                .prerequisites(subject.getPrerequisites())
                .targetAudience(subject.getTargetAudience())
                .teachingMethods(subject.getTeachingMethods())
                .effectiveDate(subject.getEffectiveDate())
                .clos(clos)
                .phases(phases)
                .assessments(assessments)
                .materials(materials)
                .build();
    }

    // ========== GET SUBJECT SYLLABUS ==========
    public SubjectDetailDTO getSubjectSyllabus(Long subjectId) {
        return getSubjectDetails(subjectId);
    }

    // ========== HELPER METHODS ==========
    public Integer getNextVersionNumber(String subjectCode, String levelCode, Integer year) {
        String logicalSubjectCode = String.format("%s-%s-%d", subjectCode, levelCode, year);
        Integer maxVersion = subjectRepository.findMaxVersionByLogicalSubjectCode(logicalSubjectCode);
        return (maxVersion == null) ? 1 : maxVersion + 1;
    }

    // ========== GET SUBJECT MATERIALS ==========
    public MaterialHierarchyDTO getSubjectMaterials(Long subjectId, Long studentId) {
        log.debug("Getting materials hierarchy for subject {}, student {}", subjectId, studentId);

        // Get subject-level materials
        List<SubjectMaterialDTO> subjectLevelMaterials = subjectMaterialRepository
                .findSubjectLevelMaterials(subjectId)
                .stream()
                .map(this::convertToMaterialDTO)
                .collect(Collectors.toList());

        // Get phases with their materials
        List<PhaseMaterialDTO> phaseMaterials = subjectPhaseRepository
                .findBySubjectIdOrderByPhaseNumber(subjectId)
                .stream()
                .map(phase -> {
                    // Phase-level materials
                    List<SubjectMaterialDTO> pMaterials = subjectMaterialRepository
                            .findPhaseLevelMaterials(phase.getId())
                            .stream()
                            .map(this::convertToMaterialDTO)
                            .collect(Collectors.toList());

                    // Sessions with materials
                    List<SessionMaterialDTO> sessions = subjectSessionRepository
                            .findByPhaseIdOrderBySequenceNo(phase.getId())
                            .stream()
                            .map(session -> {
                                List<SubjectMaterialDTO> sMaterials = subjectMaterialRepository
                                        .findSessionLevelMaterials(session.getId())
                                        .stream()
                                        .map(this::convertToMaterialDTO)
                                        .collect(Collectors.toList());

                                return SessionMaterialDTO.builder()
                                        .id(session.getId())
                                        .sequenceNo(session.getSequenceNo())
                                        .topic(session.getTopic())
                                        .materials(sMaterials)
                                        .totalMaterials(sMaterials.size())
                                        .build();
                            })
                            .collect(Collectors.toList());

                    int totalPhaseMaterials = pMaterials.size() + sessions.stream()
                            .mapToInt(SessionMaterialDTO::getTotalMaterials)
                            .sum();

                    return PhaseMaterialDTO.builder()
                            .id(phase.getId())
                            .phaseNumber(phase.getPhaseNumber())
                            .name(phase.getName())
                            .materials(pMaterials)
                            .sessions(sessions)
                            .totalMaterials(totalPhaseMaterials)
                            .build();
                })
                .collect(Collectors.toList());

        int totalMaterials = subjectLevelMaterials.size() + phaseMaterials.stream()
                .mapToInt(PhaseMaterialDTO::getTotalMaterials)
                .sum();

        return MaterialHierarchyDTO.builder()
                .subjectLevel(subjectLevelMaterials)
                .phases(phaseMaterials)
                .totalMaterials(totalMaterials)
                .accessibleMaterials(totalMaterials) // Simplified for now
                .build();
    }

    private SubjectMaterialDTO convertToMaterialDTO(SubjectMaterial material) {
        String scope;
        Long phaseId = null;
        Long sessionId = null;

        if (material.getSubjectSession() != null) {
            scope = "SESSION";
            sessionId = material.getSubjectSession().getId();
            phaseId = material.getSubjectSession().getPhase().getId();
        } else if (material.getPhase() != null) {
            scope = "PHASE";
            phaseId = material.getPhase().getId();
        } else {
            scope = "SUBJECT";
        }

        return SubjectMaterialDTO.builder()
                .id(material.getId())
                .title(material.getTitle())
                .materialType(material.getMaterialType() != null ? material.getMaterialType().name() : null)
                .url(material.getUrl())
                .scope(scope)
                .phaseId(phaseId)
                .sessionId(sessionId)
                .build();
    }

    private List<Skill> parseSkillList(List<String> skillNames) {
        if (skillNames == null || skillNames.isEmpty()) {
            return new ArrayList<>();
        }
        return skillNames.stream()
                .map(name -> {
                    try {
                        return Skill.valueOf(name.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown skill: {}", name);
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }
}