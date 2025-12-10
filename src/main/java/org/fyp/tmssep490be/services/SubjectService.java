package org.fyp.tmssep490be.services;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.subject.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final SubjectPhaseRepository subjectPhaseRepository;
    private final SubjectSessionRepository subjectSessionRepository;
    private final SubjectSessionCLOMappingRepository subjectSessionCLOMappingRepository;
    private final SubjectAssessmentRepository subjectAssessmentRepository;
    private final SubjectAssessmentCLOMappingRepository subjectAssessmentCLOMappingRepository;
    private final SubjectMaterialRepository subjectMaterialRepository;
    private final PLORepository ploRepository;
    private final PLOCLOMappingRepository ploCLOMappingRepository;
    private final UserAccountRepository userAccountRepository;
    private final EntityManager entityManager;

    @Transactional
    public SubjectDetailDTO createSubject(CreateSubjectRequestDTO request, Long userId) {
        log.info("Creating new subject: {}", request.getBasicInfo().getName());

        // 1. Validate and Create Subject Entity
        Curriculum curriculum = curriculumRepository.findById(request.getBasicInfo().getCurriculumId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương trình đào tạo"));
        Level level = levelRepository.findById(request.getBasicInfo().getLevelId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấp độ"));

        Subject subject = new Subject();
        subject.setCurriculum(curriculum);
        subject.setLevel(level);
        subject.setCode(request.getBasicInfo().getCode());
        subject.setName(request.getBasicInfo().getName());
        subject.setDescription(request.getBasicInfo().getDescription());
        subject.setScoreScale(request.getBasicInfo().getScoreScale());
        subject.setEffectiveDate(request.getBasicInfo().getEffectiveDate());

        if (request.getStatus() != null) {
            try {
                subject.setStatus(SubjectStatus.valueOf(request.getStatus()));
            } catch (IllegalArgumentException e) {
                subject.setStatus(SubjectStatus.DRAFT);
            }
        } else {
            subject.setStatus(SubjectStatus.DRAFT);
        }

        // Approval status: only set when submitted
        if (subject.getStatus() == SubjectStatus.SUBMITTED) {
            subject.setApprovalStatus(ApprovalStatus.PENDING);
        }

        if (userId != null) {
            UserAccount createdBy = userAccountRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
            subject.setCreatedBy(createdBy);
        }

        subject = subjectRepository.save(subject);
        log.info("Subject saved with ID: {}", subject.getId());

        // 2. Create CLOs
        Map<String, SubjectCLO> cloMap = createCLOs(subject, request.getClos());

        // 3. Create Structure (Phases & Sessions)
        createStructure(subject, request.getStructure(), cloMap);

        // 4. Create Assessments
        createAssessments(subject, request.getAssessments(), cloMap);

        // 5. Create Materials
        createMaterialsFromDTO(subject, request.getMaterials());

        log.info("Subject creation completed successfully");

        // Flush all changes and clear persistence context
        entityManager.flush();
        entityManager.clear();

        // Return full subject details
        return getSubjectDetails(subject.getId());
    }

    private Map<String, SubjectCLO> createCLOs(Subject subject, List<SubjectCLODTO> cloDTOs) {
        if (cloDTOs == null)
            return Map.of();

        List<SubjectCLO> clos = new ArrayList<>();
        for (SubjectCLODTO dto : cloDTOs) {
            SubjectCLO clo = new SubjectCLO();
            clo.setSubject(subject);
            clo.setCode(dto.getCode());
            clo.setDescription(dto.getDescription());
            clos.add(clo);
        }
        clos = subjectCLORepository.saveAll(clos);

        // Map PLOs
        for (SubjectCLODTO dto : cloDTOs) {
            if (dto.getMappedPLOs() != null && !dto.getMappedPLOs().isEmpty()) {
                SubjectCLO clo = clos.stream()
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
                    ploCLOMappingRepository.saveAll(mappings);
                }
            }
        }

        return clos.stream().collect(Collectors.toMap(SubjectCLO::getCode, Function.identity()));
    }

    private void createStructure(Subject subject, SubjectStructureDTO structureDTO, Map<String, SubjectCLO> cloMap) {
        if (structureDTO == null || structureDTO.getPhases() == null)
            return;

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
                                    : null)
                            .url(materialDTO.getUrl())
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
                    session.setSequenceNumber(sessionSeq++);
                    session.setSkills(parseSkillList(sessionDTO.getSkills()));

                    session = subjectSessionRepository.save(session);

                    // Map CLOs to Session
                    if (sessionDTO.getMappedCLOs() != null) {
                        List<SubjectSessionCLOMapping> mappings = new ArrayList<>();
                        for (String cloCode : sessionDTO.getMappedCLOs()) {
                            SubjectCLO clo = cloMap.get(cloCode);
                            if (clo != null) {
                                SubjectSessionCLOMapping mapping = new SubjectSessionCLOMapping();
                                mapping.setId(new SubjectSessionCLOMapping.SubjectSessionCLOMappingId());
                                mapping.setSession(session);
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
                                    .session(session)
                                    .title(materialDTO.getTitle())
                                    .materialType(materialDTO.getMaterialType() != null
                                            ? MaterialType.valueOf(materialDTO.getMaterialType())
                                            : null)
                                    .url(materialDTO.getUrl())
                                    .build();
                            subjectMaterialRepository.save(material);
                        }
                    }
                }
            }
        }
    }

    private void createAssessments(Subject subject, List<SubjectAssessmentDTO> assessmentDTOs,
            Map<String, SubjectCLO> cloMap) {
        if (assessmentDTOs == null)
            return;

        for (SubjectAssessmentDTO dto : assessmentDTOs) {
            SubjectAssessment assessment = new SubjectAssessment();
            assessment.setSubject(subject);
            assessment.setName(dto.getName());
            assessment.setAssessmentType(AssessmentType.valueOf(dto.getType()));
            assessment.setWeightPercent(dto.getWeight() != null ? dto.getWeight().doubleValue() : null);
            assessment.setDurationMinutes(dto.getDurationMinutes());
            assessment.setDescription(dto.getDescription());

            assessment = subjectAssessmentRepository.save(assessment);

            // Map CLOs to Assessment
            if (dto.getMappedCLOs() != null) {
                List<SubjectAssessmentCLOMapping> mappings = new ArrayList<>();
                for (String cloCode : dto.getMappedCLOs()) {
                    SubjectCLO clo = cloMap.get(cloCode);
                    if (clo != null) {
                        SubjectAssessmentCLOMapping mapping = new SubjectAssessmentCLOMapping();
                        mapping.setId(new SubjectAssessmentCLOMapping.SubjectAssessmentCLOMappingId());
                        mapping.setAssessment(assessment);
                        mapping.setClo(clo);
                        mapping.setStatus(MappingStatus.ACTIVE);
                        mappings.add(mapping);
                    }
                }
                subjectAssessmentCLOMappingRepository.saveAll(mappings);
            }
        }
    }

    private void createMaterialsFromDTO(Subject subject, List<SubjectMaterialDTO> materialDTOs) {
        if (materialDTOs == null)
            return;

        List<SubjectMaterial> materials = materialDTOs.stream().map(dto -> {
            SubjectMaterial.SubjectMaterialBuilder builder = SubjectMaterial.builder()
                    .subject(subject)
                    .title(dto.getTitle())
                    .materialType(dto.getMaterialType() != null ? MaterialType.valueOf(dto.getMaterialType()) : null)
                    .url(dto.getUrl());

            if (dto.getPhaseId() != null) {
                subject.getPhases().stream()
                        .filter(p -> p.getId() != null && p.getId().equals(dto.getPhaseId()))
                        .findFirst()
                        .ifPresent(builder::phase);
            }

            return builder.build();
        }).collect(Collectors.toList());

        subjectMaterialRepository.saveAll(materials);
    }

    private String parseSkillList(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return null;
        }
        return String.join(",", skills);
    }

    // Placeholder - will be implemented in FLOW_03
    public SubjectDetailDTO getSubjectDetails(Long id) {
        // TODO: Implement in FLOW_03_GET_DETAILS
        throw new UnsupportedOperationException("Will be implemented in FLOW_03_GET_DETAILS");
    }
}