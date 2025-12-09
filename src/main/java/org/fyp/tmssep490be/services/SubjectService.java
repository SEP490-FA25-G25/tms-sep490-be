package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.subject.*;
import org.fyp.tmssep490be.entities.Subject;
import org.fyp.tmssep490be.entities.SubjectPhase;
import org.fyp.tmssep490be.entities.SubjectSession;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.SubjectRepository;
import org.springframework.data.domain.Sort;
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

    public List<SubjectDTO> getAllSubjects(Long curriculumId, Long levelId) {
        log.debug("Getting all subjects for dropdown with filters - curriculumId: {}, levelId: {}", curriculumId, levelId);

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
                        .name(subject.getName())
                        .code(subject.getCode())
                        .status(subject.getStatus().name())
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

    public SubjectDetailDTO getSubjectDetails(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));

        // Map Basic Info
        SubjectBasicInfoDTO basicInfo = SubjectBasicInfoDTO.builder()
                .curriculumId(subject.getCurriculum().getId())
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

        // Map Structure (Phases & Sessions)
        List<SubjectPhaseDTO> phases = subject.getSubjectPhases().stream()
                .sorted(java.util.Comparator.comparing(SubjectPhase::getPhaseNumber))
                .map(phase -> {
                    List<SubjectSessionDTO> sessions = phase.getSubjectSessions().stream()
                            .sorted(java.util.Comparator.comparing(SubjectSession::getSequenceNo))
                            .map(session -> {
                                List<String> mappedCLOs = session.getSubjectSessionCLOMappings().stream()
                                        .map(mapping -> mapping.getClo().getCode())
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
                                        .build();
                            })
                            .collect(Collectors.toList());

                    List<SubjectMaterialDTO> phaseMaterials = phase.getSubjectMaterials().stream()
                            .filter(material -> material.getSubjectSession() == null)
                            .map(material -> SubjectMaterialDTO.builder()
                                    .id(material.getId())
                                    .title(material.getTitle())
                                    .materialType(material.getMaterialType() != null ? material.getMaterialType().name() : null)
                                    .url(material.getUrl())
                                    .scope("PHASE")
                                    .phaseId(material.getPhase() != null ? material.getPhase().getId() : null)
                                    .build())
                            .collect(Collectors.toList());

                    return SubjectPhaseDTO.builder()
                            .id(phase.getId())
                            .phaseNumber(phase.getPhaseNumber())
                            .name(phase.getName())
                            .description(phase.getDescription())
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
                            .type(assessment.getKind().name())
                            .maxScore(assessment.getMaxScore() != null ? assessment.getMaxScore() : java.math.BigDecimal.ZERO)
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

        // Map Materials
        List<SubjectMaterialDTO> materials = subject.getSubjectMaterials().stream()
                .map(material -> SubjectMaterialDTO.builder()
                        .id(material.getId())
                        .title(material.getTitle())
                        .materialType(material.getMaterialType() != null ? material.getMaterialType().name() : null)
                        .url(material.getUrl())
                        .scope(material.getPhase() != null ? "PHASE"
                                : (material.getSubjectSession() != null ? "SESSION" : "SUBJECT"))
                        .phaseId(material.getPhase() != null ? material.getPhase().getId()
                                : (material.getSubjectSession() != null ? material.getSubjectSession().getPhase().getId() : null))
                        .sessionId(material.getSubjectSession() != null ? material.getSubjectSession().getId() : null)
                        .build())
                .collect(Collectors.toList());

        return SubjectDetailDTO.builder()
                .id(subject.getId())
                .name(subject.getName())
                .code(subject.getCode())
                .description(subject.getDescription())
                .thumbnailUrl(subject.getThumbnailUrl())
                .curriculumId(subject.getCurriculum() != null ? subject.getCurriculum().getId() : null)
                .curriculumName(subject.getCurriculum() != null ? subject.getCurriculum().getName() : null)
                .levelId(subject.getLevel() != null ? subject.getLevel().getId() : null)
                .levelName(subject.getLevel() != null ? subject.getLevel().getName() : null)
                .basicInfo(basicInfo)
                .status(subject.getStatus() != null ? subject.getStatus().name() : null)
                .approvalStatus(subject.getApprovalStatus() != null ? subject.getApprovalStatus().name() : null)
                .submittedAt(subject.getSubmittedAt())
                .decidedAt(subject.getDecidedAt())
                .createdAt(subject.getCreatedAt())
                .totalHours(subject.getTotalHours())
                .hoursPerSession(subject.getHoursPerSession())
                .scoreScale(subject.getScoreScale())
                .prerequisites(subject.getPrerequisites())
                .targetAudience(subject.getTargetAudience())
                .teachingMethods(subject.getTeachingMethods())
                .effectiveDate(subject.getEffectiveDate())
                .clos(clos)
                .structure(structure)
                .phases(phases)
                .assessments(assessments)
                .materials(materials)
                .build();
    }
}
