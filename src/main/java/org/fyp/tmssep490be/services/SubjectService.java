package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.subject.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.MappingStatus;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final CurriculumRepository curriculumRepository;
    private final LevelRepository levelRepository;
    private final UserAccountRepository userAccountRepository;
    private final NotificationService notificationService;
    private final SubjectPhaseRepository subjectPhaseRepository;
    private final SubjectSessionRepository subjectSessionRepository;
    private final CLORepository cloRepository;
    private final SubjectAssessmentRepository subjectAssessmentRepository;
    private final SubjectMaterialRepository subjectMaterialRepository;

    public List<SubjectDTO> getAllSubjects(Long curriculumId, Long levelId) {
        log.debug("Fetching subjects with filters - curriculumId: {}, levelId: {}", curriculumId, levelId);

        List<Subject> subjects;
        if (curriculumId != null && levelId != null) {
            subjects = subjectRepository.findByCurriculumIdAndLevelIdOrderByUpdatedAtDesc(curriculumId, levelId);
        } else if (curriculumId != null) {
            subjects = subjectRepository.findByCurriculumIdOrderByUpdatedAtDesc(curriculumId);
        } else if (levelId != null) {
            subjects = subjectRepository.findByLevelIdOrderByUpdatedAtDesc(levelId);
        } else {
            subjects = subjectRepository.findAll();
        }

        return subjects.stream()
                .map(this::toSubjectDTO)
                .collect(Collectors.toList());
    }

    private SubjectDTO toSubjectDTO(Subject subject) {
        return SubjectDTO.builder()
                .id(subject.getId())
                .name(subject.getName())
                .code(subject.getCode())
                .status(subject.getStatus() != null ? subject.getStatus().name() : null)
                .build();
    }

     // Lấy giáo trình (syllabus) của subject
    public SubjectDetailDTO getSubjectSyllabus(Long subjectId) {
        log.debug("Getting subject syllabus for subject {}", subjectId);

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> {
                    log.error("Subject not found: {}", subjectId);
                    return new ResourceNotFoundException("Không tìm thấy môn học: " + subjectId);
                });

        List<SubjectPhaseDTO> phases = getSubjectPhasesWithFullDetails(subjectId);
        List<SubjectCLODTO> clos = getSubjectCLOsList(subjectId);
        List<SubjectAssessmentDTO> assessments = getSubjectAssessments(subjectId);

        // Tính tổng số tuần từ phases
        Integer totalDurationWeeks = phases.stream()
                .map(SubjectPhaseDTO::getDurationWeeks)
                .filter(java.util.Objects::nonNull)
                .reduce(0, Integer::sum);

        // Tính tổng số session từ phases
        Integer calculatedTotalSessions = phases.stream()
                .flatMap(phase -> phase.getSessions() != null ? phase.getSessions().stream()
                        : java.util.stream.Stream.empty())
                .mapToInt(session -> 1)
                .sum();

        log.debug("Subject {} - Entity numberOfSessions: {}, calculated from phases: {}",
                subjectId, subject.getNumberOfSessions(), calculatedTotalSessions);

        // Ưu tiên số session từ phases nếu có, nếu không thì sử dụng số session từ entity
        Integer effectiveNumberOfSessions = calculatedTotalSessions > 0
                ? calculatedTotalSessions
                : (subject.getNumberOfSessions() != null ? subject.getNumberOfSessions() : 0);

        // Tính tổng số giờ từ sessions * hoursPerSession nếu có sessions
        Integer effectiveTotalHours;
        if (calculatedTotalSessions > 0 && subject.getHoursPerSession() != null) {
            // Tính tổng số giờ từ số session tính toán
            effectiveTotalHours = subject.getHoursPerSession()
                    .multiply(java.math.BigDecimal.valueOf(calculatedTotalSessions))
                    .intValue();
        } else {
            // Sử dụng số giờ từ entity
            effectiveTotalHours = subject.getTotalHours();
        }

        log.debug("Subject {} - effectiveTotalHours: {}, effectiveNumberOfSessions: {}",
                subjectId, effectiveTotalHours, effectiveNumberOfSessions);

        return SubjectDetailDTO.builder()
                .id(subject.getId())
                .code(subject.getCode())
                .name(subject.getName())
                .description(subject.getDescription())
                .thumbnailUrl(subject.getThumbnailUrl())
                .curriculumId(subject.getCurriculum() != null ? subject.getCurriculum().getId() : null)
                .curriculumName(subject.getCurriculum() != null ? subject.getCurriculum().getName() : null)
                .levelId(subject.getLevel() != null ? subject.getLevel().getId() : null)
                .levelName(subject.getLevel() != null ? subject.getLevel().getName() : null)
                .logicalSubjectCode(subject.getLogicalSubjectCode())
                .version(subject.getVersion())
                .totalHours(effectiveTotalHours)
                .numberOfSessions(effectiveNumberOfSessions)
                .hoursPerSession(subject.getHoursPerSession())
                .scoreScale(subject.getScoreScale())
                .prerequisites(subject.getPrerequisites())
                .targetAudience(subject.getTargetAudience())
                .teachingMethods(subject.getTeachingMethods())
                .effectiveDate(subject.getEffectiveDate())
                .status(subject.getStatus() != null ? subject.getStatus().toString() : null)
                .approvalStatus(subject.getApprovalStatus() != null ? subject.getApprovalStatus().toString() : null)
                .submittedAt(subject.getSubmittedAt())
                .decidedAt(subject.getDecidedAt())
                .createdAt(subject.getCreatedAt())
                .phases(phases)
                .clos(clos)
                .assessments(assessments)
                .build();
    }

    // Lấy danh sách phases với đầy đủ thông tin (sessions, materials)
    private List<SubjectPhaseDTO> getSubjectPhasesWithFullDetails(Long subjectId) {
        return subjectPhaseRepository.findBySubjectIdOrderByPhaseNumberAsc(subjectId)
                .stream()
                .map(phase -> {
                    List<SubjectSessionDTO> sessions = subjectSessionRepository
                            .findByPhaseIdOrderBySequenceNoAsc(phase.getId())
                            .stream()
                            .map(this::convertToSessionDTOWithCLOs)
                            .collect(Collectors.toList());

                    // Lấy materials cho phase này
                    List<SubjectMaterialDTO> phaseMaterials = subjectMaterialRepository
                            .findPhaseLevelMaterials(phase.getId())
                            .stream()
                            .map(this::convertToSimpleMaterialDTO)
                            .collect(Collectors.toList());

                    return SubjectPhaseDTO.builder()
                            .id(phase.getId())
                            .phaseNumber(phase.getPhaseNumber())
                            .name(phase.getName())
                            .description(phase.getDescription())
                            .durationWeeks(phase.getDurationWeeks())
                            .sessions(sessions)
                            .materials(phaseMaterials)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // Chuyển đổi SubjectSession sang DTO kèm theo CLOs đã map
    private SubjectSessionDTO convertToSessionDTOWithCLOs(SubjectSession session) {
        // Chuyển đổi List<Skill> sang List<String>
        List<String> skills = session.getSkills() != null
                ? session.getSkills().stream().map(Enum::name).collect(Collectors.toList())
                : new ArrayList<>();

        // Lấy mã CLO đã map từ SubjectSessionCLOMapping
        List<String> mappedCLOs = session.getSubjectSessionCLOMappings().stream()
                .filter(mapping -> mapping.getStatus() == MappingStatus.ACTIVE)
                .map(mapping -> mapping.getClo().getCode())
                .sorted()
                .collect(Collectors.toList());

        // Lấy materials cho session này
        List<SubjectMaterialDTO> sessionMaterials = subjectMaterialRepository
                .findSessionLevelMaterials(session.getId())
                .stream()
                .map(this::convertToSimpleMaterialDTO)
                .collect(Collectors.toList());

        return SubjectSessionDTO.builder()
                .id(session.getId())
                .sequenceNo(session.getSequenceNo())
                .topic(session.getTopic())
                .studentTask(session.getStudentTask())
                .skills(skills)
                .mappedCLOs(mappedCLOs)
                .materials(sessionMaterials)
                .build();
    }

    // Chuyển đổi material sang DTO đơn giản
    private SubjectMaterialDTO convertToSimpleMaterialDTO(SubjectMaterial material) {
        String scope;
        if (material.getSubjectSession() != null) {
            scope = "SESSION";
        } else if (material.getPhase() != null) {
            scope = "PHASE";
        } else {
            scope = "SUBJECT";
        }

        return SubjectMaterialDTO.builder()
                .id(material.getId())
                .title(material.getTitle())
                .materialType(material.getMaterialType() != null ? material.getMaterialType().name() : null)
                .url(material.getUrl())
                .scope(scope)
                .phaseId(material.getPhase() != null ? material.getPhase().getId() : null)
                .sessionId(material.getSubjectSession() != null ? material.getSubjectSession().getId() : null)
                .build();
    }

    // Lấy danh sách CLOs của subject
    private List<SubjectCLODTO> getSubjectCLOsList(Long subjectId) {
        return cloRepository.findBySubjectId(subjectId)
                .stream()
                .map(this::convertToCLODTO)
                .collect(Collectors.toList());
    }

    // Chuyển đổi CLO sang DTO
    private SubjectCLODTO convertToCLODTO(CLO clo) {
        return SubjectCLODTO.builder()
                .code(clo.getCode())
                .description(clo.getDescription())
                .mappedPLOs(new ArrayList<>()) // TODO: Thêm PLO mapping nếu cần
                .build();
    }

    // Lấy danh sách assessments của subject
    private List<SubjectAssessmentDTO> getSubjectAssessments(Long subjectId) {
        return subjectAssessmentRepository.findBySubjectIdOrderByIdAsc(subjectId)
                .stream()
                .map(this::convertToAssessmentDTO)
                .collect(Collectors.toList());
    }

    // Chuyển đổi SubjectAssessment sang DTO
    private SubjectAssessmentDTO convertToAssessmentDTO(SubjectAssessment assessment) {
        // Trích xuất mã CLO từ mappings
        List<String> cloMappings = assessment.getSubjectAssessmentCLOMappings().stream()
                .map(mapping -> mapping.getClo().getCode())
                .sorted()
                .collect(Collectors.toList());

        return SubjectAssessmentDTO.builder()
                .id(assessment.getId())
                .name(assessment.getName())
                .type(assessment.getKind() != null ? assessment.getKind().name() : null)
                .maxScore(assessment.getMaxScore())
                .durationMinutes(assessment.getDurationMinutes())
                .description(assessment.getDescription())
                .note(assessment.getNote())
                .skills(assessment.getSkills() != null
                        ? assessment.getSkills().stream().map(Enum::name).collect(Collectors.toList())
                        : new ArrayList<>())
                .mappedCLOs(cloMappings)
                .build();
    }

    // Lấy danh sách materials của subject theo hierarchy (subject level, phase level, session level)
    public MaterialHierarchyDTO getSubjectMaterials(Long subjectId, Long studentId) {
        log.debug("Getting materials hierarchy for subject {}, student {}", subjectId, studentId);

        // Lấy materials ở cấp subject (không thuộc phase hay session nào)
        List<SubjectMaterialDTO> subjectLevelMaterials = subjectMaterialRepository
                .findSubjectLevelMaterials(subjectId)
                .stream()
                .map(this::convertToMaterialDTO)
                .collect(Collectors.toList());

        // Lấy phases với materials của chúng
        List<PhaseMaterialDTO> phaseMaterials = subjectPhaseRepository
                .findBySubjectIdOrderByPhaseNumberAsc(subjectId)
                .stream()
                .map(phase -> {
                    // Lấy materials ở cấp phase
                    List<SubjectMaterialDTO> phaseMaterialsList = subjectMaterialRepository
                            .findPhaseLevelMaterials(phase.getId())
                            .stream()
                            .map(this::convertToMaterialDTO)
                            .collect(Collectors.toList());

                    // Lấy sessions với materials của chúng
                    List<SessionMaterialDTO> sessionMaterials = subjectSessionRepository
                            .findByPhaseIdOrderBySequenceNoAsc(phase.getId())
                            .stream()
                            .map(session -> {
                                List<SubjectMaterialDTO> sessionMaterialsList = subjectMaterialRepository
                                        .findSessionLevelMaterials(session.getId())
                                        .stream()
                                        .map(this::convertToMaterialDTO)
                                        .collect(Collectors.toList());

                                return SessionMaterialDTO.builder()
                                        .id(session.getId())
                                        .sequenceNo(session.getSequenceNo())
                                        .topic(session.getTopic())
                                        .materials(sessionMaterialsList)
                                        .skills(session.getSkills() != null
                                                ? session.getSkills().stream().map(Enum::name).collect(Collectors.toList())
                                                : new ArrayList<>())
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

        // Tính tổng số materials
        int totalMaterials = subjectLevelMaterials.size() + phaseMaterials.stream()
                .mapToInt(pm -> pm.getTotalMaterials())
                .sum();

        // accessibleMaterials count (tạm thời set bằng totalMaterials, có thể thêm logic check access sau)
        int accessibleMaterials = totalMaterials;

        return MaterialHierarchyDTO.builder()
                .subjectLevel(subjectLevelMaterials)
                .phases(phaseMaterials)
                .totalMaterials(totalMaterials)
                .accessibleMaterials(accessibleMaterials)
                .build();
    }

    // Chuyển đổi SubjectMaterial sang DTO (có thể thêm logic check access cho student sau)
    private SubjectMaterialDTO convertToMaterialDTO(SubjectMaterial material) {
        String scope;
        if (material.getSubjectSession() != null) {
            scope = "SESSION";
        } else if (material.getPhase() != null) {
            scope = "PHASE";
        } else {
            scope = "SUBJECT";
        }

        return SubjectMaterialDTO.builder()
                .id(material.getId())
                .title(material.getTitle())
                .materialType(material.getMaterialType() != null ? material.getMaterialType().name() : null)
                .url(material.getUrl())
                .scope(scope)
                .phaseId(material.getPhase() != null ? material.getPhase().getId() : null)
                .sessionId(material.getSubjectSession() != null ? material.getSubjectSession().getId() : null)
                .build();
    }
}
