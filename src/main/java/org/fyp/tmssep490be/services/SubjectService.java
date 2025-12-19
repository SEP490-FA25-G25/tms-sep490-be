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
import org.springframework.transaction.annotation.Propagation;

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
    private final SubjectCLORepository cloRepository;
    private final SubjectAssessmentRepository subjectAssessmentRepository;
    private final SubjectAssessmentCLOMappingRepository subjectAssessmentCLOMappingRepository;
    private final SubjectMaterialRepository subjectMaterialRepository;
    private final UserAccountRepository userAccountRepository;
    private final ClassRepository classRepository;
    private final NotificationService notificationService;
    private final EntityManager entityManager;

    // ========== GET ALL SUBJECTS ==========
    public List<SubjectDTO> getAllSubjects(Long curriculumId, Long levelId, Boolean forClassCreation) {
        log.debug("Getting all subjects with filters - curriculumId: {}, levelId: {}, forClassCreation: {}",
                curriculumId, levelId, forClassCreation);

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

        // Filter for class creation: only ACTIVE or PENDING_ACTIVATION subjects
        if (Boolean.TRUE.equals(forClassCreation)) {
            subjects = subjects.stream()
                    .filter(subject -> subject.getStatus() == SubjectStatus.ACTIVE
                            || subject.getStatus() == SubjectStatus.PENDING_ACTIVATION)
                    .collect(Collectors.toList());
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
                        .requesterName(subject.getCreatedBy() != null ? subject.getCreatedBy().getFullName() : null)
                        .rejectionReason(subject.getRejectionReason())
                        .submittedAt(subject.getSubmittedAt())
                        .decidedAt(subject.getDecidedAt())
                        .effectiveDate(subject.getEffectiveDate())
                        .createdAt(subject.getCreatedAt())
                        .updatedAt(subject.getUpdatedAt())
                        .numberOfSessions(subject.getNumberOfSessions())
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

    // ========== UPDATE SUBJECT ==========
    @Transactional
    public SubjectDetailDTO updateSubject(Long id, CreateSubjectRequestDTO request, Long userId) {
        log.info("Updating subject ID: {}", id);

        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));

        // Status validation: Block editing if ACTIVE
        if (subject.getStatus() == SubjectStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Không thể chỉnh sửa môn học đang ở trạng thái HOẠT ĐỘNG. Hãy tạo phiên bản mới.");
        }

        // If PENDING_ACTIVATION, reset to DRAFT and clear approval (requires
        // re-approval)
        if (subject.getStatus() == SubjectStatus.PENDING_ACTIVATION) {
            log.info("Subject {} is PENDING_ACTIVATION, resetting to DRAFT for re-approval", id);
            subject.setStatus(SubjectStatus.DRAFT);
            subject.setApprovalStatus(null);
            subject.setSubmittedAt(null);
            subject.setDecidedAt(null);
            subject.setDecidedByManager(null);
        }

        // Update Basic Info
        SubjectBasicInfoDTO basicInfo = request.getBasicInfo();
        subject.setName(basicInfo.getName());
        subject.setCode(basicInfo.getCode());
        subject.setDescription(basicInfo.getDescription());
        subject.setPrerequisites(basicInfo.getPrerequisites());
        subject.setTotalHours(basicInfo.getDurationHours());
        subject.setScoreScale(basicInfo.getScoreScale());
        subject.setTargetAudience(basicInfo.getTargetAudience());
        subject.setTeachingMethods(basicInfo.getTeachingMethods());
        subject.setEffectiveDate(basicInfo.getEffectiveDate());
        subject.setNumberOfSessions(basicInfo.getNumberOfSessions());
        subject.setHoursPerSession(basicInfo.getHoursPerSession());
        subject.setThumbnailUrl(basicInfo.getThumbnailUrl());

        // Recalculate total hours
        if (subject.getNumberOfSessions() != null && subject.getHoursPerSession() != null) {
            subject.setTotalHours(subject.getHoursPerSession()
                    .multiply(java.math.BigDecimal.valueOf(subject.getNumberOfSessions())).intValue());
        }

        // Update Curriculum
        if (basicInfo.getSubjectId() != null) {
            Curriculum curriculum = curriculumRepository.findById(basicInfo.getSubjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khung chương trình"));
            subject.setCurriculum(curriculum);
        }

        // Update Level
        if (basicInfo.getLevelId() != null) {
            Level level = levelRepository.findById(basicInfo.getLevelId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấp độ"));
            subject.setLevel(level);
        }

        // Update status
        if (request.getStatus() != null) {
            try {
                subject.setStatus(SubjectStatus.valueOf(request.getStatus()));
            } catch (IllegalArgumentException e) {
                // Ignore invalid status
            }
        }

        // Keep approval status consistent
        if (subject.getStatus() == SubjectStatus.SUBMITTED) {
            subject.setApprovalStatus(ApprovalStatus.PENDING);
            if (subject.getSubmittedAt() == null) {
                subject.setSubmittedAt(OffsetDateTime.now());
            }
        } else if (subject.getStatus() == SubjectStatus.DRAFT) {
            subject.setApprovalStatus(null);
            subject.setSubmittedAt(null);
        }

        subject = subjectRepository.save(subject);

        // Clear and recreate child entities (simplified approach)
        // 1. Clear existing CLOs (cascade will delete mappings)
        subjectCLORepository.deleteBySubjectId(id);

        // 2. Clear existing Phases (cascade will delete sessions and materials)
        subjectPhaseRepository.deleteBySubjectId(id);

        // 3. Clear existing Assessments
        subjectAssessmentRepository.deleteBySubjectId(id);

        // 4. Clear existing Materials (subject-level only)
        subjectMaterialRepository.deleteBySubjectIdAndPhaseIsNullAndSubjectSessionIsNull(id);

        entityManager.flush();
        entityManager.clear();

        // Reload subject to avoid stale state
        subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));

        // Recreate child entities
        Map<String, CLO> cloMap = createCLOs(subject, request.getClos());
        createStructure(subject, request.getStructure(), cloMap);
        createAssessments(subject, request.getAssessments(), cloMap);
        createMaterials(subject, request.getMaterials());

        log.info("Subject update completed successfully");

        entityManager.flush();
        entityManager.clear();

        return getSubjectDetails(id);
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
                            .title(materialDTO.getName())
                            .materialType(materialDTO.getType() != null
                                    ? MaterialType.valueOf(materialDTO.getType())
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
                                    .title(materialDTO.getName())
                                    .materialType(materialDTO.getType() != null
                                            ? MaterialType.valueOf(materialDTO.getType())
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
                    .title(dto.getName()) // Frontend sends 'name', entity uses 'title'
                    .materialType(dto.getType() != null
                            ? MaterialType.valueOf(dto.getType())
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
                .thumbnailUrl(subject.getThumbnailUrl())
                .durationHours(subject.getTotalHours())
                .numberOfSessions(subject.getNumberOfSessions())
                .hoursPerSession(subject.getHoursPerSession())
                .scoreScale(subject.getScoreScale())
                .prerequisites(subject.getPrerequisites())
                .targetAudience(subject.getTargetAudience())
                .teachingMethods(subject.getTeachingMethods())
                .effectiveDate(subject.getEffectiveDate())
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
                                                .name(material.getTitle())
                                                .type(material.getMaterialType() != null
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
                                    .name(material.getTitle())
                                    .type(material.getMaterialType() != null
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
                            .totalSessions(sessions.size())
                            .totalMaterials(phaseMaterials.size())
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
                        .name(material.getTitle())
                        .type(material.getMaterialType() != null ? material.getMaterialType().name() : null)
                        .url(material.getUrl())
                        .scope(material.getPhase() != null ? "PHASE"
                                : (material.getSubjectSession() != null ? "SESSION" : "COURSE"))
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
                .name(material.getTitle())
                .type(material.getMaterialType() != null ? material.getMaterialType().name() : null)
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

    // ========== APPROVAL WORKFLOW ==========

    /**
     * Submit a subject for approval
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void submitSubject(Long id) {
        log.info("Submitting subject for approval: {}", id);
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));

        log.info("Current subject status: {}, approvalStatus: {}", subject.getStatus(), subject.getApprovalStatus());

        // Only allow submission if status is DRAFT or REJECTED
        if (subject.getStatus() != SubjectStatus.DRAFT && subject.getStatus() != SubjectStatus.REJECTED) {
            throw new IllegalStateException("Chỉ có thể gửi môn học ở trạng thái NHÁP hoặc BỊ TỪ CHỐI");
        }

        // Validate effectiveDate is not in the past
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate effectiveDate = subject.getEffectiveDate();
        if (effectiveDate != null && effectiveDate.isBefore(today)) {
            throw new IllegalStateException(
                    "Ngày hiệu lực không được là ngày trong quá khứ. Vui lòng quay lại Bước 1 để cập nhật ngày hiệu lực.");
        }

        // Set updatedAt only when re-submitting after rejection
        if (subject.getApprovalStatus() == ApprovalStatus.REJECTED) {
            subject.setUpdatedAt(OffsetDateTime.now());
            log.info("Subject {} is being re-submitted after rejection", id);
        }

        subject.setStatus(SubjectStatus.SUBMITTED);
        subject.setApprovalStatus(ApprovalStatus.PENDING);
        subject.setSubmittedAt(OffsetDateTime.now());

        log.info("About to save subject with status: {}", subject.getStatus());
        subjectRepository.save(subject);
        log.info("Subject saved successfully");

        // Send notification to Managers - temporarily disabled for debugging
        // TODO: Re-enable once issue is fixed
        try {
            sendNotificationToManagers(subject);
        } catch (Exception e) {
            log.error("Notification failed but continuing: {}", e.getMessage());
            // Don't rethrow - let the submit succeed even if notification fails
        }

        log.info("Subject {} submitted successfully", id);
    }

    /**
     * Approve a subject for publication
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void approveSubject(Long id, Long managerId) {
        log.info("Approving subject with ID: {} by manager: {}", id, managerId);
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));

        if (subject.getStatus() != SubjectStatus.SUBMITTED) {
            throw new IllegalStateException("Chỉ có thể phê duyệt môn học ở trạng thái ĐÃ GỬI");
        }

        // Set approval status
        subject.setApprovalStatus(ApprovalStatus.APPROVED);
        subject.setDecidedAt(OffsetDateTime.now());

        // Check if effectiveDate <= today -> ACTIVE immediately, else
        // PENDING_ACTIVATION
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate effectiveDate = subject.getEffectiveDate();

        if (effectiveDate != null && !effectiveDate.isAfter(today)) {
            // effectiveDate <= today -> Active immediately
            subject.setStatus(SubjectStatus.ACTIVE);
            log.info("Subject {} approved and activated immediately (effectiveDate: {} <= today: {})",
                    id, effectiveDate, today);
        } else {
            // effectiveDate > today -> Wait for scheduler
            subject.setStatus(SubjectStatus.PENDING_ACTIVATION);
            log.info("Subject {} approved. Will be activated on effective date: {}",
                    id, effectiveDate);
        }

        // Set decided by manager
        if (managerId != null) {
            UserAccount manager = userAccountRepository.findById(managerId).orElse(null);
            subject.setDecidedByManager(manager);
        }

        subjectRepository.save(subject);

        // Cascade ACTIVE status to Level and Curriculum if subject is activated
        // immediately
        activateLevelAndCurriculumIfNeeded(subject);

        // Send notification to Subject Leader (creator)
        sendApprovalNotificationToCreator(subject, true, null);
    }

    /**
     * Reject a subject
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rejectSubject(Long id, Long managerId, String reason) {
        log.info("Rejecting subject with ID: {}", id);
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));

        if (subject.getStatus() != SubjectStatus.SUBMITTED) {
            throw new IllegalStateException("Chỉ có thể từ chối môn học ở trạng thái ĐÃ GỬI");
        }

        subject.setStatus(SubjectStatus.DRAFT);
        subject.setApprovalStatus(ApprovalStatus.REJECTED);
        subject.setRejectionReason(reason);
        subject.setDecidedAt(OffsetDateTime.now());

        // Set decided by manager
        if (managerId != null) {
            UserAccount manager = userAccountRepository.findById(managerId).orElse(null);
            subject.setDecidedByManager(manager);
        }

        subjectRepository.save(subject);

        // Send notification to Subject Leader (creator) with rejection reason
        sendApprovalNotificationToCreator(subject, false, reason);

        log.info("Subject {} rejected. Reason: {}", id, reason);
    }

    /**
     * Send notification to all Managers when a subject is submitted for approval
     */
    private void sendNotificationToManagers(Subject subject) {
        try {
            List<UserAccount> managers = userAccountRepository.findUsersByRole("MANAGER");

            if (!managers.isEmpty()) {
                String title = "Môn học mới cần phê duyệt";
                String message = String.format(
                        "Môn học '%s' (%s) cần được phê duyệt.",
                        subject.getName(),
                        subject.getCode());

                List<Long> managerIds = managers.stream()
                        .map(UserAccount::getId)
                        .collect(Collectors.toList());

                notificationService.sendBulkNotifications(
                        managerIds,
                        NotificationType.REQUEST,
                        title,
                        message);

                log.info("Sent notification to {} managers about subject {} submission",
                        managers.size(), subject.getId());
            } else {
                log.warn("No managers found to notify about subject {} submission", subject.getId());
            }
        } catch (Exception e) {
            log.error("Error sending notification to managers for subject {}: {}",
                    subject.getId(), e.getMessage(), e);
            // Don't throw exception - notification failure shouldn't block submission
        }
    }

    /**
     * Send notification to Subject Leader (creator) when subject is approved or
     * rejected
     */
    private void sendApprovalNotificationToCreator(Subject subject, boolean isApproved, String rejectionReason) {
        try {
            UserAccount creator = subject.getCreatedBy();
            if (creator == null) {
                log.warn("No creator found for subject {} - cannot send approval notification", subject.getId());
                return;
            }

            String title;
            String message;
            NotificationType notificationType;

            if (isApproved) {
                title = "Môn học đã được phê duyệt";
                message = String.format("Môn học '%s' (%s) của bạn đã được phê duyệt.",
                        subject.getName(), subject.getCode());
                notificationType = NotificationType.NOTIFICATION;
            } else {
                title = "Môn học bị từ chối";
                message = String.format("Môn học '%s' (%s) của bạn bị từ chối. Lý do: %s",
                        subject.getName(), subject.getCode(),
                        rejectionReason != null ? rejectionReason : "Không có lý do cụ thể");
                notificationType = NotificationType.NOTIFICATION;
            }

            notificationService.createNotification(
                    creator.getId(),
                    notificationType,
                    title,
                    message);

            log.info("Sent {} notification to creator (user {}) for subject {}",
                    isApproved ? "approval" : "rejection", creator.getId(), subject.getId());
        } catch (Exception e) {
            log.error("Error sending approval notification for subject {}: {}",
                    subject.getId(), e.getMessage(), e);
            // Don't throw exception - notification failure shouldn't block
            // approval/rejection
        }
    }

    // ========== DELETE SUBJECT ==========
    @Transactional
    public void deleteSubject(Long id) {
        log.info("Deleting subject with ID: {}", id);
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học với ID: " + id));

        // Validation: Only allow delete if status is DRAFT and never submitted
        if (subject.getStatus() != SubjectStatus.DRAFT || subject.getApprovalStatus() != null) {
            throw new IllegalStateException(
                    "Không thể xóa môn học. Môn học phải ở trạng thái NHÁP và chưa được gửi phê duyệt.");
        }

        subjectRepository.delete(subject);
        log.info("Subject deleted successfully: {}", id);
    }

    // ========== DEACTIVATE SUBJECT ==========
    @Transactional
    public void deactivateSubject(Long id) {
        log.info("Deactivating subject with ID: {}", id);
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học với ID: " + id));

        // Only allow deactivating ACTIVE subjects
        if (subject.getStatus() != SubjectStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Chỉ có thể vô hiệu hóa môn học đang ở trạng thái HOẠT ĐỘNG");
        }

        // Check if any classes are using this subject with SCHEDULED or ONGOING status
        boolean hasActiveClasses = classRepository.existsBySubjectIdAndStatusIn(
                id,
                java.util.List.of(ClassStatus.SCHEDULED, ClassStatus.ONGOING));

        if (hasActiveClasses) {
            throw new IllegalStateException(
                    "Không thể vô hiệu hóa môn học vì đang có lớp học đã lên lịch hoặc đang diễn ra");
        }

        subject.setStatus(SubjectStatus.INACTIVE);
        subjectRepository.save(subject);
        log.info("Subject {} deactivated successfully", id);
    }

    // ========== REACTIVATE SUBJECT ==========
    @Transactional
    public void reactivateSubject(Long id) {
        log.info("Reactivating subject with ID: {}", id);
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học với ID: " + id));

        if (subject.getStatus() != SubjectStatus.INACTIVE) {
            throw new IllegalStateException("Chỉ có thể kích hoạt lại môn học đang ở trạng thái KHÔNG HOẠT ĐỘNG");
        }

        // Check if subject has enough data to be active
        boolean hasCLOs = subject.getClos() != null && !subject.getClos().isEmpty();
        boolean hasPhases = subject.getSubjectPhases() != null && !subject.getSubjectPhases().isEmpty();

        if (hasCLOs && hasPhases) {
            subject.setStatus(SubjectStatus.ACTIVE);
        } else {
            subject.setStatus(SubjectStatus.DRAFT);
            log.info("Subject {} reactivated as DRAFT because it lacks CLOs or Phases", id);
        }
        subjectRepository.save(subject);
        log.info("Subject {} reactivated with status: {}", id, subject.getStatus());
    }

    // ========== CLONE SUBJECT ==========
    @Transactional
    public SubjectDTO cloneSubject(Long id, Long userId) {
        log.info("Cloning subject with ID: {} by user: {}", id, userId);

        Subject originalSubject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học với ID: " + id));

        // Get the original code and calculate next version
        String originalCode = originalSubject.getCode();

        // Extract base code (remove existing version suffix if present, e.g., -V2, -V3)
        String baseCode = originalCode.replaceAll("-V\\d+$", "");

        // Find next available version number for this base code
        int nextVersion = 2; // Start from V2
        String newCode = baseCode + "-V" + nextVersion;
        while (subjectRepository.existsByCode(newCode)) {
            nextVersion++;
            newCode = baseCode + "-V" + nextVersion;
        }

        // Get creator
        UserAccount creator = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        // Create new subject
        Subject newSubject = new Subject();
        newSubject.setCurriculum(originalSubject.getCurriculum());
        newSubject.setLevel(originalSubject.getLevel());
        newSubject.setLogicalSubjectCode(baseCode);
        newSubject.setVersion(nextVersion);
        newSubject.setCode(newCode);
        newSubject.setName(originalSubject.getName() + " (Bản sao)");
        newSubject.setDescription(originalSubject.getDescription());
        newSubject.setScoreScale(originalSubject.getScoreScale());
        newSubject.setTotalHours(originalSubject.getTotalHours());
        newSubject.setNumberOfSessions(originalSubject.getNumberOfSessions());
        newSubject.setHoursPerSession(originalSubject.getHoursPerSession());
        newSubject.setPrerequisites(originalSubject.getPrerequisites());
        newSubject.setTargetAudience(originalSubject.getTargetAudience());
        newSubject.setTeachingMethods(originalSubject.getTeachingMethods());
        newSubject.setEffectiveDate(originalSubject.getEffectiveDate());
        newSubject.setThumbnailUrl(originalSubject.getThumbnailUrl());
        newSubject.setStatus(SubjectStatus.DRAFT);
        newSubject.setApprovalStatus(null);
        newSubject.setCreatedBy(creator);
        newSubject.setCreatedAt(OffsetDateTime.now());
        newSubject.setUpdatedAt(OffsetDateTime.now());

        // Save new subject first
        newSubject = subjectRepository.save(newSubject);
        final Subject savedNewSubject = newSubject;

        // Clone CLOs and track mapping
        java.util.Map<Long, CLO> oldToNewCloMap = new java.util.HashMap<>();
        if (originalSubject.getClos() != null) {
            for (CLO oldClo : originalSubject.getClos()) {
                CLO newClo = new CLO();
                newClo.setSubject(savedNewSubject);
                newClo.setCode(oldClo.getCode());
                newClo.setDescription(oldClo.getDescription());
                newClo = subjectCLORepository.save(newClo);
                oldToNewCloMap.put(oldClo.getId(), newClo);

                // Clone PLO-CLO mappings
                List<PLOCLOMapping> oldMappings = ploCloMappingRepository.findByCloId(oldClo.getId());
                for (PLOCLOMapping oldMapping : oldMappings) {
                    PLOCLOMapping newMapping = new PLOCLOMapping();
                    // Must initialize EmbeddedId before setting entities with @MapsId
                    newMapping.setId(new PLOCLOMapping.PLOCLOMappingId(
                            oldMapping.getPlo().getId(), newClo.getId()));
                    newMapping.setClo(newClo);
                    newMapping.setPlo(oldMapping.getPlo());
                    newMapping.setStatus(oldMapping.getStatus());
                    ploCloMappingRepository.save(newMapping);
                }
            }
        }

        // Clone Phases and Sessions
        if (originalSubject.getSubjectPhases() != null) {
            for (SubjectPhase oldPhase : originalSubject.getSubjectPhases()) {
                SubjectPhase newPhase = new SubjectPhase();
                newPhase.setSubject(savedNewSubject);
                newPhase.setName(oldPhase.getName());
                newPhase.setDescription(oldPhase.getDescription());
                newPhase.setPhaseNumber(oldPhase.getPhaseNumber());
                newPhase = subjectPhaseRepository.save(newPhase);
                final SubjectPhase savedNewPhase = newPhase;

                // Clone Sessions
                if (oldPhase.getSubjectSessions() != null) {
                    for (SubjectSession oldSession : oldPhase.getSubjectSessions()) {
                        SubjectSession newSession = new SubjectSession();
                        newSession.setPhase(savedNewPhase);
                        newSession.setTopic(oldSession.getTopic());
                        newSession.setStudentTask(oldSession.getStudentTask());
                        newSession.setSkills(oldSession.getSkills() != null
                                ? new ArrayList<>(oldSession.getSkills())
                                : new ArrayList<>());
                        newSession.setSequenceNo(oldSession.getSequenceNo());
                        newSession = subjectSessionRepository.save(newSession);
                        final SubjectSession savedNewSession = newSession;

                        // Clone Session-CLO mappings
                        List<SubjectSessionCLOMapping> oldSessionMappings = subjectSessionCLOMappingRepository
                                .findBySubjectSessionId(oldSession.getId());
                        for (SubjectSessionCLOMapping oldMapping : oldSessionMappings) {
                            CLO newClo = oldToNewCloMap.get(oldMapping.getClo().getId());
                            if (newClo != null) {
                                SubjectSessionCLOMapping newMapping = new SubjectSessionCLOMapping();
                                // Must initialize EmbeddedId before setting entities with @MapsId
                                newMapping.setId(new SubjectSessionCLOMapping.SubjectSessionCLOMappingId(
                                        savedNewSession.getId(), newClo.getId()));
                                newMapping.setSubjectSession(savedNewSession);
                                newMapping.setClo(newClo);
                                newMapping.setStatus(oldMapping.getStatus());
                                subjectSessionCLOMappingRepository.save(newMapping);
                            }
                        }

                        // Clone Session Materials
                        List<SubjectMaterial> oldSessionMaterials = subjectMaterialRepository
                                .findBySubjectSessionId(oldSession.getId());
                        for (SubjectMaterial oldMaterial : oldSessionMaterials) {
                            SubjectMaterial newMaterial = new SubjectMaterial();
                            newMaterial.setSubject(savedNewSubject);
                            newMaterial.setPhase(savedNewPhase);
                            newMaterial.setSubjectSession(savedNewSession);
                            newMaterial.setTitle(oldMaterial.getTitle());
                            newMaterial.setMaterialType(oldMaterial.getMaterialType());
                            newMaterial.setUrl(oldMaterial.getUrl());
                            subjectMaterialRepository.save(newMaterial);
                        }
                    }
                }

                // Clone Phase Materials
                List<SubjectMaterial> oldPhaseMaterials = subjectMaterialRepository
                        .findByPhaseIdAndSubjectSessionIsNull(oldPhase.getId());
                for (SubjectMaterial oldMaterial : oldPhaseMaterials) {
                    SubjectMaterial newMaterial = new SubjectMaterial();
                    newMaterial.setSubject(savedNewSubject);
                    newMaterial.setPhase(savedNewPhase);
                    newMaterial.setSubjectSession(null);
                    newMaterial.setTitle(oldMaterial.getTitle());
                    newMaterial.setMaterialType(oldMaterial.getMaterialType());
                    newMaterial.setUrl(oldMaterial.getUrl());
                    subjectMaterialRepository.save(newMaterial);
                }
            }
        }

        // Clone Subject-level Materials
        List<SubjectMaterial> oldSubjectMaterials = subjectMaterialRepository
                .findBySubjectIdAndPhaseIsNullAndSubjectSessionIsNull(originalSubject.getId());
        for (SubjectMaterial oldMaterial : oldSubjectMaterials) {
            SubjectMaterial newMaterial = new SubjectMaterial();
            newMaterial.setSubject(savedNewSubject);
            newMaterial.setPhase(null);
            newMaterial.setSubjectSession(null);
            newMaterial.setTitle(oldMaterial.getTitle());
            newMaterial.setMaterialType(oldMaterial.getMaterialType());
            newMaterial.setUrl(oldMaterial.getUrl());
            subjectMaterialRepository.save(newMaterial);
        }

        // Clone Assessments
        if (originalSubject.getSubjectAssessments() != null) {
            for (SubjectAssessment oldAssessment : originalSubject.getSubjectAssessments()) {
                SubjectAssessment newAssessment = new SubjectAssessment();
                newAssessment.setSubject(savedNewSubject);
                newAssessment.setName(oldAssessment.getName());
                newAssessment.setKind(oldAssessment.getKind());
                newAssessment.setMaxScore(oldAssessment.getMaxScore());
                newAssessment.setDurationMinutes(oldAssessment.getDurationMinutes());
                newAssessment.setDescription(oldAssessment.getDescription());
                newAssessment.setNote(oldAssessment.getNote());
                newAssessment.setSkills(
                        oldAssessment.getSkills() != null ? new java.util.ArrayList<>(oldAssessment.getSkills())
                                : new java.util.ArrayList<>());
                newAssessment = subjectAssessmentRepository.save(newAssessment);
                final SubjectAssessment savedNewAssessment = newAssessment;

                // Clone Assessment-CLO mappings
                List<SubjectAssessmentCLOMapping> oldAssessmentMappings = subjectAssessmentCLOMappingRepository
                        .findBySubjectAssessmentId(oldAssessment.getId());
                for (SubjectAssessmentCLOMapping oldMapping : oldAssessmentMappings) {
                    CLO newClo = oldToNewCloMap.get(oldMapping.getClo().getId());
                    if (newClo != null) {
                        SubjectAssessmentCLOMapping.SubjectAssessmentCLOMappingId newId = new SubjectAssessmentCLOMapping.SubjectAssessmentCLOMappingId(
                                savedNewAssessment.getId(), newClo.getId());
                        SubjectAssessmentCLOMapping newMapping = SubjectAssessmentCLOMapping.builder()
                                .id(newId)
                                .subjectAssessment(savedNewAssessment)
                                .clo(newClo)
                                .status(oldMapping.getStatus())
                                .build();
                        subjectAssessmentCLOMappingRepository.save(newMapping);
                    }
                }
            }
        }

        log.info("Subject {} cloned successfully as {} with ID: {}", id, newCode, savedNewSubject.getId());

        // Return simple DTO
        return SubjectDTO.builder()
                .id(savedNewSubject.getId())
                .code(savedNewSubject.getCode())
                .name(savedNewSubject.getName())
                .subjectName(savedNewSubject.getCurriculum() != null ? savedNewSubject.getCurriculum().getName() : null)
                .subjectId(savedNewSubject.getCurriculum() != null ? savedNewSubject.getCurriculum().getId() : null)
                .levelName(savedNewSubject.getLevel() != null ? savedNewSubject.getLevel().getName() : null)
                .levelId(savedNewSubject.getLevel() != null ? savedNewSubject.getLevel().getId() : null)
                .status(savedNewSubject.getStatus().name())
                .effectiveDate(savedNewSubject.getEffectiveDate())
                .createdAt(savedNewSubject.getCreatedAt())
                .build();
    }

    // ========== CASCADE STATUS UPDATES ==========
    /**
     * Activates Level and Curriculum when Subject becomes ACTIVE.
     * - Level becomes ACTIVE if at least one Subject is ACTIVE
     * - Curriculum becomes ACTIVE if at least one Level is ACTIVE
     * 
     * @param subject The subject that was activated
     */
    public void activateLevelAndCurriculumIfNeeded(Subject subject) {
        if (subject.getStatus() != SubjectStatus.ACTIVE) {
            return; // Only cascade if subject is ACTIVE
        }

        // 1. Activate Level if needed
        Level level = subject.getLevel();
        if (level != null && level.getStatus() != LevelStatus.ACTIVE) {
            level.setStatus(LevelStatus.ACTIVE);
            levelRepository.save(level);
            log.info("Level {} ('{}') activated because Subject {} ('{}') is ACTIVE",
                    level.getId(), level.getName(), subject.getId(), subject.getName());

            // 2. Activate Curriculum if needed
            Curriculum curriculum = level.getCurriculum();
            if (curriculum != null && curriculum.getStatus() != CurriculumStatus.ACTIVE) {
                curriculum.setStatus(CurriculumStatus.ACTIVE);
                curriculumRepository.save(curriculum);
                log.info("Curriculum {} ('{}') activated because Level {} ('{}') is ACTIVE",
                        curriculum.getId(), curriculum.getName(), level.getId(), level.getName());
            }
        }
    }
}
