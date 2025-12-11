package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.dtos.subject.SubjectPhaseDTO;
import org.fyp.tmssep490be.entities.SubjectPhase;
import org.fyp.tmssep490be.repositories.SubjectPhaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubjectPhaseService {

    private final SubjectPhaseRepository subjectPhaseRepository;

    @Transactional(readOnly = true)
    public List<SubjectPhaseDTO> getPhasesBySubjectId(Long subjectId) {
        List<SubjectPhase> phases = subjectPhaseRepository.findBySubjectIdOrderByPhaseNumber(subjectId);
        return phases.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubjectPhaseDTO> getAllPhases() {
        List<SubjectPhase> phases = subjectPhaseRepository.findAll();
        return phases.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private SubjectPhaseDTO convertToDTO(SubjectPhase phase) {
        return SubjectPhaseDTO.builder()
                .id(phase.getId())
                .subjectId(phase.getSubject() != null ? phase.getSubject().getId() : null)
                .subjectName(phase.getSubject() != null ? phase.getSubject().getName() : null)
                .phaseNumber(phase.getPhaseNumber())
                .name(phase.getName())
                .durationWeeks(phase.getDurationWeeks())
                .learningFocus(phase.getLearningFocus())
                .description(phase.getDescription())
                .build();
    }
}
